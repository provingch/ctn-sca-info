package py.edu.ctn.sca.padres.ui.evaluador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.data.evaluador.EvaluadorRepository
import py.edu.ctn.sca.padres.data.evaluador.IncumplimientoDto
import py.edu.ctn.sca.padres.data.evaluador.PlanDetalleDto
import py.edu.ctn.sca.padres.data.evaluador.PlanResumenDto
import py.edu.ctn.sca.padres.data.evaluador.ResolucionDto
import java.io.File

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Error(val message: String) : Load<Nothing>
    data class Ok<T>(val data: T) : Load<T>
}

data class EvaluadorUi(
    val planes: Load<List<PlanResumenDto>> = Load.Loading,
    val incumplimientos: Load<List<IncumplimientoDto>> = Load.Loading,
    val detalle: Load<PlanDetalleDto> = Load.Loading,
    val busy: Boolean = false,
    /** Mensaje efímero para mostrar como Toast; la UI llama [EvaluadorViewModel.messageShown]. */
    val message: String? = null,
)

class EvaluadorViewModel(private val repo: EvaluadorRepository) : ViewModel() {
    private val _ui = MutableStateFlow(EvaluadorUi())
    val ui: StateFlow<EvaluadorUi> = _ui.asStateFlow()

    init { refresh() }

    fun refresh() {
        loadPlanes()
        loadIncumplimientos()
    }

    fun loadPlanes() {
        _ui.update { it.copy(planes = Load.Loading) }
        viewModelScope.launch {
            val r = repo.pendientes()
            _ui.update { it.copy(planes = r.fold({ d -> Load.Ok(d) }, { e -> Load.Error(e.message.orEmpty()) })) }
        }
    }

    fun loadIncumplimientos() {
        _ui.update { it.copy(incumplimientos = Load.Loading) }
        viewModelScope.launch {
            val r = repo.incumplimientos()
            _ui.update { it.copy(incumplimientos = r.fold({ d -> Load.Ok(d) }, { e -> Load.Error(e.message.orEmpty()) })) }
        }
    }

    fun loadDetalle(id: Int) {
        _ui.update { it.copy(detalle = Load.Loading) }
        viewModelScope.launch {
            val r = repo.plan(id)
            _ui.update { it.copy(detalle = r.fold({ d -> Load.Ok(d) }, { e -> Load.Error(e.message.orEmpty()) })) }
        }
    }

    fun aprobar(id: Int, onDone: () -> Unit) = decidir(id, "Plan aprobado.", onDone) { repo.aprobar(id) }

    fun rechazar(id: Int, observacion: String, onDone: () -> Unit) =
        decidir(id, "Plan rechazado.", onDone) { repo.rechazar(id, observacion.trim()) }

    private fun decidir(id: Int, ok: String, onDone: () -> Unit, call: suspend () -> Result<Unit>) {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            val r = call()
            _ui.update { it.copy(busy = false, message = r.fold({ ok }, { e -> e.message })) }
            if (r.isSuccess) {
                loadPlanes()
                onDone()
            }
        }
    }

    fun resolver(item: IncumplimientoDto, resolucion: ResolucionDto) {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            val r = repo.resolver(item.id, resolucion)
            _ui.update {
                it.copy(
                    busy = false,
                    message = r.fold(
                        { res ->
                            if (res.bloqueoGenerado) "Con este rechazo se alcanzó el umbral: Iniciar clase quedó bloqueado."
                            else "Caso resuelto y profesor notificado."
                        },
                        { e -> e.message },
                    ),
                )
            }
            if (r.isSuccess) loadIncumplimientos()
        }
    }

    /** Descarga el documento; devuelve el archivo por [onFile] o el error como mensaje. */
    fun abrirDocumento(id: Int, onFile: (File) -> Unit) {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            val r = repo.documento(id)
            _ui.update { it.copy(busy = false, message = r.exceptionOrNull()?.message) }
            r.getOrNull()?.let(onFile)
        }
    }

    fun messageShown() = _ui.update { it.copy(message = null) }
}
