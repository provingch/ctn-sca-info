package py.edu.ctn.sca.padres.data

import kotlinx.serialization.json.Json
import py.edu.ctn.sca.padres.core.PersistentCookieJar
import py.edu.ctn.sca.padres.core.Role
import py.edu.ctn.sca.padres.core.Session
import retrofit2.Response

sealed interface LoginStep {
    data object Authenticated : LoginStep
    data class NeedsTwoFactor(val tempToken: String) : LoginStep
    data class Failed(val message: String, val locked: Boolean = false, val retryAfterSeconds: Long? = null) : LoginStep
}

/** Niveles 1, 2, 4 y 5 usan la app; el 3 (Administración) se usa solo desde la web. */
private const val ADMIN_MESSAGE = "Las cuentas de Administración se usan desde la computadora, en la web del SCA."

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
        return if (resp != null && resp.isSuccessful && body != null && Role.isSupported(body.level)) {
            session.onAuthenticated(body.accessToken, body.level)
            true
        } else {
            if (body?.level == Role.ADMIN.level) logout() else session.onLoggedOut()
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
                // Administración no tiene app: se cierra la sesión recién creada (cookie +
                // refresh token) sin marcarla como autenticada.
                if (!Role.isSupported(body.level)) {
                    logout()
                    LoginStep.Failed(ADMIN_MESSAGE)
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
