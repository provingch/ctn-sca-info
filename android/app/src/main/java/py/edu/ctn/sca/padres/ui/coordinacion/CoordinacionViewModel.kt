package py.edu.ctn.sca.padres.ui.coordinacion

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.data.coordinacion.AnotChip
import py.edu.ctn.sca.padres.data.coordinacion.AnotacionDto
import py.edu.ctn.sca.padres.data.coordinacion.CoordinacionRepository
import py.edu.ctn.sca.padres.data.coordinacion.QuejaDto
import py.edu.ctn.sca.padres.data.coordinacion.QuejaEstado
import py.edu.ctn.sca.padres.data.coordinacion.QuejaFiltro
import py.edu.ctn.sca.padres.data.coordinacion.estadoEfectivo
import py.edu.ctn.sca.padres.data.coordinacion.ultimosDias
import java.io.IOException

data class QuejasState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<QuejaDto> = emptyList(),
    val nombres: Map<Long, String> = emptyMap(),
    val filtro: QuejaFiltro = QuejaFiltro.PENDIENTES,
) {
    val pendientes get() = items.count { it.estadoEfectivo() == QuejaEstado.PENDIENTE }
    fun creador(id: Long) = nombres[id]?.takeIf { it.isNotBlank() } ?: "Usuario #$id"
}

data class AnotacionesState(
    val loading: Boolean = true,
    val error: String? = null,
    /** Ya limitadas a los últimos 7 días. */
    val items: List<AnotacionDto> = emptyList(),
    val texto: String = "",
    val chip: AnotChip = AnotChip.TODAS,
)

class CoordinacionViewModel(private val repo: CoordinacionRepository) : ViewModel() {
    private val _quejas = MutableStateFlow(QuejasState())
    val quejas: StateFlow<QuejasState> = _quejas.asStateFlow()
    private val _anot = MutableStateFlow(AnotacionesState())
    val anotaciones: StateFlow<AnotacionesState> = _anot.asStateFlow()
    private val fotos = LruCache<String, ImageBitmap>(30)

    init {
        cargarQuejas()
        cargarAnotaciones()
    }

    private fun mensaje(e: Exception, que: String) =
        if (e is IOException) "Sin conexión. Verificá tu internet e intentá de nuevo." else "No se pudieron cargar $que."

    fun cargarQuejas() {
        _quejas.update { it.copy(loading = it.items.isEmpty(), error = null) }
        viewModelScope.launch {
            try {
                val items = repo.quejas()
                _quejas.update { it.copy(loading = false, items = items) }
                if (_quejas.value.nombres.isEmpty()) {
                    val n = repo.nombres()
                    _quejas.update { it.copy(nombres = n) }
                }
            } catch (e: Exception) {
                _quejas.update { it.copy(loading = false, error = mensaje(e, "las quejas")) }
            }
        }
    }

    fun setFiltro(f: QuejaFiltro) = _quejas.update { it.copy(filtro = f) }

    /** null = ok (y recarga la lista); si no, mensaje de error. */
    suspend fun aceptar(id: Long): String? = repo.aceptar(id).also { if (it == null) cargarQuejas() }

    suspend fun rechazar(id: Long, motivo: String): String? = repo.rechazar(id, motivo).also { if (it == null) cargarQuejas() }

    fun cargarAnotaciones() {
        _anot.update { it.copy(loading = it.items.isEmpty(), error = null) }
        viewModelScope.launch {
            try {
                val items = ultimosDias(repo.anotaciones())
                _anot.update { it.copy(loading = false, items = items) }
            } catch (e: Exception) {
                _anot.update { it.copy(loading = false, error = mensaje(e, "las anotaciones")) }
            }
        }
    }

    fun setTexto(t: String) = _anot.update { it.copy(texto = t) }
    fun setChip(c: AnotChip) = _anot.update { it.copy(chip = c) }

    suspend fun foto(id: Int, maxPx: Int): ImageBitmap? {
        val key = "$id:$maxPx"
        fotos.get(key)?.let { return it }
        return repo.foto(id, maxPx)?.asImageBitmap()?.also { fotos.put(key, it) }
    }
}
