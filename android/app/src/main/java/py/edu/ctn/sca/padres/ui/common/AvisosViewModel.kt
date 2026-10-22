package py.edu.ctn.sca.padres.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.core.Session
import py.edu.ctn.sca.padres.data.ProfileLoad
import py.edu.ctn.sca.padres.data.ProfileRepository
import py.edu.ctn.sca.padres.data.common.NotificacionDto
import py.edu.ctn.sca.padres.data.common.NotificacionesRepository
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class AvisosSeccion(val titulo: String) { HOY("Hoy"), ESTA_SEMANA("Esta semana"), ANTES("Antes") }

data class AvisosUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<NotificacionDto> = emptyList(),
    val soloSinLeer: Boolean = true,
    /** Contador para el badge de la pestaña (se refresca sin abrir la pantalla). */
    val unread: Int = 0,
    val userName: String = "",
) {
    val visibles get() = if (soloSinLeer) items.filter { !it.leida } else items
}

/** Agrupa por fecha de creación; dentro de cada sección, más nuevas primero (como las manda el backend). */
fun agruparAvisos(items: List<NotificacionDto>, hoy: LocalDate = LocalDate.now()): List<Pair<AvisosSeccion, List<NotificacionDto>>> =
    items.groupBy { n ->
        val d = n.creadaEn()?.toLocalDate()
        when {
            d == null -> AvisosSeccion.ANTES
            !d.isBefore(hoy) -> AvisosSeccion.HOY
            !d.isBefore(hoy.minusDays(7)) -> AvisosSeccion.ESTA_SEMANA
            else -> AvisosSeccion.ANTES
        }
    }.toList().sortedBy { it.first.ordinal }

private val Hora = DateTimeFormatter.ofPattern("HH:mm")
private val Dia = DateTimeFormatter.ofPattern("dd/MM")

fun cuandoAviso(dt: LocalDateTime?, hoy: LocalDate = LocalDate.now()): String {
    if (dt == null) return ""
    val d = dt.toLocalDate()
    return when {
        !d.isBefore(hoy) -> dt.format(Hora)
        !d.isBefore(hoy.minusDays(7)) -> d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
        else -> dt.format(Dia)
    }
}

/**
 * Compartido por las apps de todos los roles (scope de Activity): alimenta la pantalla Avisos
 * y el badge de la barra inferior.
 */
class AvisosViewModel(
    private val repo: NotificacionesRepository,
    private val profile: ProfileRepository,
    session: Session,
) : ViewModel() {

    private val _ui = MutableStateFlow(AvisosUiState())
    val ui: StateFlow<AvisosUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            session.state.collect { if (it == Session.AuthState.LOGGED_OUT) _ui.value = AvisosUiState() }
        }
    }

    /** Solo el contador del badge; barato, se llama al cambiar de pestaña. */
    fun refreshCount() {
        viewModelScope.launch {
            runCatching { repo.contador() }.onSuccess { c -> _ui.update { it.copy(unread = c) } }
        }
    }

    fun loadUserName() {
        if (_ui.value.userName.isNotEmpty()) return
        viewModelScope.launch {
            (profile.load() as? ProfileLoad.Ok)?.let { ok ->
                val o = ok.data.profileOwner
                val name = o.fullName ?: listOfNotNull(o.nombre, o.apellido).joinToString(" ")
                _ui.update { it.copy(userName = name) }
            }
        }
    }

    fun load() {
        _ui.update { it.copy(loading = it.items.isEmpty(), error = null) }
        viewModelScope.launch {
            try {
                val items = repo.listar()
                _ui.update { it.copy(loading = false, items = items, unread = items.count { n -> !n.leida }) }
            } catch (e: Exception) {
                val msg = if (e is IOException) "Sin conexión. Verificá tu internet e intentá de nuevo."
                else "No se pudieron cargar los avisos."
                _ui.update { it.copy(loading = false, error = msg) }
            }
        }
    }

    fun setSoloSinLeer(value: Boolean) = _ui.update { it.copy(soloSinLeer = value) }

    fun marcarLeida(n: NotificacionDto) {
        if (n.leida) return
        viewModelScope.launch {
            // Los recordatorios no descartables responden 400: se quedan sin leer.
            if (repo.leer(n.id)) {
                _ui.update { s ->
                    val items = s.items.map { if (it.id == n.id) it.copy(leida = true) else it }
                    s.copy(items = items, unread = items.count { !it.leida })
                }
            }
        }
    }

    fun marcarTodas() {
        viewModelScope.launch {
            repo.leerTodas()
            load()
        }
    }
}
