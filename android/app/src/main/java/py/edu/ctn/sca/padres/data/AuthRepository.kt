package py.edu.ctn.sca.padres.data

import kotlinx.serialization.json.Json
import py.edu.ctn.sca.padres.core.PersistentCookieJar
import py.edu.ctn.sca.padres.core.Session
import retrofit2.Response

sealed interface LoginStep {
    data object Authenticated : LoginStep
    data class NeedsTwoFactor(val tempToken: String) : LoginStep
    data class Failed(val message: String, val locked: Boolean = false, val retryAfterSeconds: Long? = null) : LoginStep
}

/** Solo las cuentas Padre (nivel 4) pueden usar esta app. */
private const val PARENT_LEVEL = 4
private const val NOT_PARENT_MESSAGE =
    "Esta app es solo para cuentas de padres. Iniciá sesión desde la web con tu usuario."

class AuthRepository(
    private val authApi: AuthApi,
    private val session: Session,
    private val cookieJar: PersistentCookieJar,
    private val json: Json,
) {

    suspend fun login(username: String, password: String, rememberMe: Boolean): LoginStep =
        runCatching { authApi.login(LoginRequest(username.trim(), password, rememberMe)) }
            .fold(onSuccess = { handle(it) }, onFailure = { networkError(it) })

    suspend fun verifyTwoFactor(tempToken: String, code: String, rememberMe: Boolean): LoginStep =
        runCatching { authApi.verify2fa(Verify2faRequest(tempToken, code.trim(), rememberMe)) }
            .fold(onSuccess = { handle(it) }, onFailure = { networkError(it) })

    /**
     * Cold-start: try to turn a stored cookie into a live session. Any outcome
     * other than "authenticated" ends in [Session.onLoggedOut] so the UI leaves
     * the splash and shows the login screen — a network error must not strand
     * the app on the spinner.
     */
    suspend fun restore(): Boolean {
        val resp = runCatching { authApi.refresh() }.getOrNull()
        val body = resp?.body()
        return if (resp != null && resp.isSuccessful && body != null && body.level == PARENT_LEVEL) {
            session.onAuthenticated(body.accessToken, body.level)
            true
        } else {
            session.onLoggedOut()
            false
        }
    }

    suspend fun logout() {
        runCatching { authApi.logout() }
        cookieJar.clear()
        session.onLoggedOut()
    }

    private suspend fun handle(resp: Response<LoginResponse>): LoginStep {
        if (!resp.isSuccessful) {
            val err = resp.errorBody()?.string()?.let {
                runCatching { json.decodeFromString<AuthErrorResponse>(it) }.getOrNull()
            }
            val locked = resp.code() == 429 || err?.code == "AUTH_LOCKED"
            return LoginStep.Failed(
                message = err?.message ?: defaultError(resp.code()),
                locked = locked,
                retryAfterSeconds = err?.retryAfterSeconds
                    ?: resp.headers()["Retry-After"]?.toLongOrNull(),
            )
        }
        val body = resp.body() ?: return LoginStep.Failed("Respuesta vacía del servidor.")
        return when {
            body.requiere2fa && !body.tempToken.isNullOrBlank() ->
                LoginStep.NeedsTwoFactor(body.tempToken)
            !body.accessToken.isNullOrBlank() -> {
                // La app es solo para padres: si el login server-side es de otro rol,
                // se cierra la sesión recién creada (cookie + refresh token) sin llegar
                // a marcar la sesión como autenticada, así la UI nunca muestra el dashboard.
                if (body.level != PARENT_LEVEL) {
                    logout()
                    LoginStep.Failed(NOT_PARENT_MESSAGE)
                } else {
                    session.onAuthenticated(body.accessToken, body.level)
                    LoginStep.Authenticated
                }
            }
            else -> LoginStep.Failed("No se pudo iniciar sesión.")
        }
    }

    private fun networkError(t: Throwable): LoginStep.Failed {
        val msg = if (t is java.io.IOException)
            "Sin conexión. Verificá tu internet e intentá de nuevo."
        else
            "No se pudo iniciar sesión. Intentá de nuevo."
        return LoginStep.Failed(msg)
    }

    private fun defaultError(code: Int) = when (code) {
        401 -> "Usuario o contraseña incorrectos."
        429 -> "Demasiados intentos. Probá más tarde."
        in 500..599 -> "El servidor no está disponible. Intentá de nuevo."
        else -> "No se pudo iniciar sesión (error $code)."
    }
}
