package py.edu.ctn.sca.padres.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory auth state. The access token is deliberately not persisted; on a
 * cold start the app calls `/api/auth/refresh` with the stored cookie to get a
 * fresh one (see [py.edu.ctn.sca.padres.data.AuthRepository]).
 */
class Session {

    @Volatile
    var accessToken: String? = null
        private set

    @Volatile
    var level: Int? = null
        private set

    private val _state = MutableStateFlow(AuthState.UNKNOWN)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun onAuthenticated(token: String, level: Int?) {
        this.accessToken = token
        this.level = level
        _state.value = AuthState.AUTHENTICATED
    }

    fun onLoggedOut() {
        accessToken = null
        level = null
        _state.value = AuthState.LOGGED_OUT
    }

    enum class AuthState { UNKNOWN, AUTHENTICATED, LOGGED_OUT }
}
