package py.edu.ctn.sca.padres.core

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import py.edu.ctn.sca.padres.BuildConfig
import py.edu.ctn.sca.padres.data.AuthApi
import py.edu.ctn.sca.padres.data.ParentApi
import py.edu.ctn.sca.padres.data.PushApi
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Wires OkHttp + Retrofit. One shared [PersistentCookieJar] so the rotating
 * refresh cookie is kept in sync across every call. Two client instances:
 *  - [bare]     : cookie jar only. Used for `/auth/refresh` and `/auth/login`
 *                 so the token refresh path can never recurse into itself.
 *  - [authed]   : adds the `Authorization: Bearer` header and a 401 -> refresh
 *                 -> retry [TokenAuthenticator].
 */
class Network(
    private val session: Session,
    private val cookieJar: PersistentCookieJar,
    private val onSessionExpired: () -> Unit,
) {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val converter = json.asConverterFactory("application/json".toMediaType())

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }

    // Independent dispatcher so a blocking /auth/refresh fired from the
    // TokenAuthenticator can never starve the pool serving the request that
    // triggered it.
    private val bare: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .dispatcher(okhttp3.Dispatcher())
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    /** Retrofit over [bare]; only auth endpoints live here. */
    private val bareRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.SCA_BASE_URL)
        .client(bare)
        .addConverterFactory(converter)
        .build()

    val authApi: AuthApi = bareRetrofit.create(AuthApi::class.java)

    private val bearerInterceptor = Interceptor { chain ->
        val token = session.accessToken
        val request = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val authed: OkHttpClient = bare.newBuilder()
        .dispatcher(okhttp3.Dispatcher())
        .addInterceptor(bearerInterceptor)
        .authenticator(TokenAuthenticator(session, authApi, onSessionExpired))
        .build()

    private val authedRetrofit: Retrofit = bareRetrofit.newBuilder()
        .client(authed)
        .build()

    val parentApi: ParentApi = authedRetrofit.create(ParentApi::class.java)
    val pushApi: PushApi = authedRetrofit.create(PushApi::class.java)
}

/**
 * On a 401, tries `/api/auth/refresh` once (blocking, on OkHttp's thread) and
 * replays the request with the new bearer token. If refresh fails, signals a
 * hard logout and gives up.
 */
private class TokenAuthenticator(
    private val session: Session,
    private val authApi: AuthApi,
    private val onSessionExpired: () -> Unit,
) : okhttp3.Authenticator {

    @Synchronized
    override fun authenticate(route: okhttp3.Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null // already retried once

        val failedToken = response.request.header("Authorization")
        // Another thread may have refreshed already; if so, just retry with the current token.
        session.accessToken?.let { current ->
            if ("Bearer $current" != failedToken) {
                return response.request.newBuilder().header("Authorization", "Bearer $current").build()
            }
        }

        val refreshed = runCatching {
            kotlinx.coroutines.runBlocking { authApi.refresh() }
        }.getOrNull()

        val newToken = refreshed?.takeIf { it.isSuccessful }?.body()?.accessToken
        if (newToken == null) {
            session.onLoggedOut()
            onSessionExpired()
            return null
        }
        session.onAuthenticated(newToken, refreshed.body()?.level ?: session.level)
        return response.request.newBuilder().header("Authorization", "Bearer $newToken").build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
