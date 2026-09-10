package py.edu.ctn.sca.padres.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.data.ProfileLoad
import py.edu.ctn.sca.padres.data.ProfileRepository
import py.edu.ctn.sca.padres.data.ProfileSave
import py.edu.ctn.sca.padres.data.SaveProfileRequest

data class ProfileUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val nombre: String = "",
    val apellido: String = "",
    val correo: String = "",
    val telefono: String = "",
    val usuario: String = "",
    /** Nombre/apellido solo los edita un admin; para el padre suele venir en false. */
    val canEditIdentity: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false,
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
                            correo = owner.correo.orEmpty(),
                            telefono = owner.telefono.orEmpty(),
                            usuario = owner.usuario.orEmpty(),
                            canEditIdentity = result.data.canEditAdminOnlyProfileFields,
                            saved = false,
                            saveError = null,
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
    fun onCorreo(v: String) = _ui.update { it.copy(correo = v, saved = false, saveError = null) }
    fun onTelefono(v: String) = _ui.update { it.copy(telefono = v, saved = false, saveError = null) }

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
                correo = s.correo.trim().ifBlank { null },
                telefono = s.telefono.trim().ifBlank { null },
            )
            when (val result = profileRepository.save(request)) {
                is ProfileSave.Ok -> _ui.update {
                    it.copy(
                        saving = false,
                        saved = true,
                        saveError = null,
                        // Reflejar lo que efectivamente se envió (ya recortado).
                        nombre = request.nombre.orEmpty(),
                        apellido = request.apellido.orEmpty(),
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
}
