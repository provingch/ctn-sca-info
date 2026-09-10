package py.edu.ctn.sca.padres.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.data.AuthRepository
import py.edu.ctn.sca.padres.data.LoginStep
import py.edu.ctn.sca.padres.data.PushRepository

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    val code: String = "",
    val stage: Stage = Stage.CREDENTIALS,
    val loading: Boolean = false,
    val error: String? = null,
    val lockedSeconds: Long? = null,
) {
    enum class Stage { CREDENTIALS, TWO_FACTOR }
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val pushRepository: PushRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    private var tempToken: String? = null

    fun onUsername(v: String) = _ui.update { it.copy(username = v, error = null) }
    fun onPassword(v: String) = _ui.update { it.copy(password = v, error = null) }
    fun onCode(v: String) = _ui.update { it.copy(code = v.filter(Char::isDigit).take(6), error = null) }
    fun onRememberMe(v: Boolean) = _ui.update { it.copy(rememberMe = v) }

    fun backToCredentials() {
        tempToken = null
        _ui.update { it.copy(stage = AuthUiState.Stage.CREDENTIALS, code = "", error = null) }
    }

    fun submitCredentials() {
        val s = _ui.value
        if (s.username.isBlank() || s.password.isBlank() || s.loading) return
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val step = authRepository.login(s.username, s.password, s.rememberMe)
            applyStep(step)
        }
    }

    fun submitCode() {
        val s = _ui.value
        val token = tempToken ?: return
        if (s.code.length < 6 || s.loading) return
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val step = authRepository.verifyTwoFactor(token, s.code, s.rememberMe)
            applyStep(step)
        }
    }

    private suspend fun applyStep(step: LoginStep) {
        when (step) {
            is LoginStep.Authenticated -> {
                _ui.update { it.copy(loading = false, error = null) }
                runCatching { pushRepository.syncToken(force = true) }
            }
            is LoginStep.NeedsTwoFactor -> {
                tempToken = step.tempToken
                _ui.update {
                    it.copy(loading = false, stage = AuthUiState.Stage.TWO_FACTOR, error = null)
                }
            }
            is LoginStep.Failed -> _ui.update {
                it.copy(loading = false, error = step.message, lockedSeconds = step.retryAfterSeconds)
            }
        }
    }
}
