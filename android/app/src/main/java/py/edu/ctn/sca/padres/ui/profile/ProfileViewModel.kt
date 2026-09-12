package py.edu.ctn.sca.padres.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.data.ChangePasswordRequest
import py.edu.ctn.sca.padres.data.PasswordChange
import py.edu.ctn.sca.padres.data.ProfileLoad
import py.edu.ctn.sca.padres.data.ProfileRepository
import py.edu.ctn.sca.padres.data.ProfileSave
import py.edu.ctn.sca.padres.data.SaveProfileRequest

data class ProfileUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val nombre: String = "",
    val apellido: String = "",
    val ci: String = "",
    val correo: String = "",
    val telefono: String = "",
    val usuario: String = "",
    val roleLabel: String = "",
    /** Nombre/apellido/CI solo los edita un admin; para el padre suele venir en false. */
    val canEditIdentity: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false,
    val activityLog: List<String> = emptyList(),
    val showFotoPanel: Boolean = false,
    val fotoPerfil: String? = null,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val changingPassword: Boolean = false,
    val passwordError: String? = null,
    val passwordChanged: Boolean = false,
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ProfileUiState())
    val ui: StateFlow<ProfileUiState> = _ui.asStateFlow()

    init {
        load()
    }

    fun load() {
        _ui.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = profileRepository.load()) {
                is ProfileLoad.Ok -> {
                    val owner = result.data.profileOwner
                    _ui.update {
                        it.copy(
                            loading = false,
                            loadError = null,
                            nombre = owner.nombre.orEmpty(),
                            apellido = owner.apellido.orEmpty(),
                            ci = owner.ci?.toString().orEmpty(),
                            correo = owner.correo.orEmpty(),
                            telefono = owner.telefono.orEmpty(),
                            usuario = owner.usuario.orEmpty(),
                            roleLabel = result.data.profileRoleLabel.orEmpty(),
                            canEditIdentity = result.data.canEditAdminOnlyProfileFields,
                            saved = false,
                            saveError = null,
                            activityLog = result.data.activityLog,
                            showFotoPanel = result.data.showFotoPanel,
                            fotoPerfil = owner.fotoPerfil,
                        )
                    }
                }
                is ProfileLoad.Error -> _ui.update {
                    it.copy(loading = false, loadError = result.message)
                }
            }
        }
    }

    fun onNombre(v: String) = _ui.update { it.copy(nombre = v, saved = false, saveError = null) }
    fun onApellido(v: String) = _ui.update { it.copy(apellido = v, saved = false, saveError = null) }
    fun onCi(v: String) = _ui.update { it.copy(ci = v.filter { c -> c.isDigit() }, saved = false, saveError = null) }
    fun onCorreo(v: String) = _ui.update { it.copy(correo = v, saved = false, saveError = null) }
    fun onTelefono(v: String) = _ui.update { it.copy(telefono = v, saved = false, saveError = null) }

    fun onFotoPerfil(base64WithPrefix: String?) = _ui.update {
        it.copy(fotoPerfil = base64WithPrefix, saved = false, saveError = null)
    }

    fun save() {
        val s = _ui.value
        if (s.saving || s.loading) return
        if (s.usuario.isBlank()) {
            _ui.update { it.copy(saveError = "No se pudo determinar tu nombre de usuario. Recargá el perfil.") }
            return
        }
        _ui.update { it.copy(saving = true, saveError = null, saved = false) }
        viewModelScope.launch {
            val request = SaveProfileRequest(
                usuario = s.usuario.trim(),
                nombre = s.nombre.trim().ifBlank { null },
                apellido = s.apellido.trim().ifBlank { null },
                ci = s.ci.trim().toLongOrNull()?.let { if (it in 1..99_999_999) it.toInt() else null },
                correo = s.correo.trim().ifBlank { null },
                telefono = s.telefono.trim().ifBlank { null },
                fotoPerfil = if (s.showFotoPanel) s.fotoPerfil.orEmpty() else null,
            )
            when (val result = profileRepository.save(request)) {
                is ProfileSave.Ok -> _ui.update {
                    it.copy(
                        saving = false,
                        saved = true,
                        saveError = null,
                        nombre = request.nombre.orEmpty(),
                        apellido = request.apellido.orEmpty(),
                        ci = request.ci?.toString().orEmpty(),
                        correo = request.correo.orEmpty(),
                        telefono = request.telefono.orEmpty(),
                    )
                }
                is ProfileSave.Error -> _ui.update {
                    it.copy(saving = false, saveError = result.message)
                }
            }
        }
    }

    fun onCurrentPassword(v: String) = _ui.update {
        it.copy(currentPassword = v, passwordChanged = false, passwordError = null)
    }
    fun onNewPassword(v: String) = _ui.update {
        it.copy(newPassword = v, passwordChanged = false, passwordError = null)
    }
    fun onConfirmPassword(v: String) = _ui.update {
        it.copy(confirmPassword = v, passwordChanged = false, passwordError = null)
    }

    fun changePassword() {
        val s = _ui.value
        if (s.changingPassword) return
        if (s.currentPassword.isBlank() || s.newPassword.isBlank() || s.confirmPassword.isBlank()) {
            _ui.update { it.copy(passwordError = "Completá los tres campos de contraseña.") }
            return
        }
        if (s.newPassword.length < 6) {
            _ui.update { it.copy(passwordError = "La nueva contraseña debe tener al menos 6 caracteres.") }
            return
        }
        if (s.newPassword != s.confirmPassword) {
            _ui.update { it.copy(passwordError = "Las contraseñas no coinciden.") }
            return
        }
        _ui.update { it.copy(changingPassword = true, passwordError = null, passwordChanged = false) }
        viewModelScope.launch {
            val req = ChangePasswordRequest(
                currentPassword = s.currentPassword,
                newPassword = s.newPassword,
                confirmPassword = s.confirmPassword,
            )
            when (val result = profileRepository.changePassword(req)) {
                is PasswordChange.Ok -> _ui.update {
                    it.copy(
                        changingPassword = false,
                        passwordChanged = true,
                        passwordError = null,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                    )
                }
                is PasswordChange.Error -> _ui.update {
                    it.copy(changingPassword = false, passwordError = result.message)
                }
            }
        }
    }
}
