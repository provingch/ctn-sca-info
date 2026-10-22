package py.edu.ctn.sca.padres.ui.padre

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.data.ChildDto
import py.edu.ctn.sca.padres.data.ConductaResult
import py.edu.ctn.sca.padres.data.ParentRepository
import py.edu.ctn.sca.padres.data.ParentResponse
import py.edu.ctn.sca.padres.data.ParentResult
import py.edu.ctn.sca.padres.data.SubjectDto
import py.edu.ctn.sca.padres.data.padre.AnotacionDto
import py.edu.ctn.sca.padres.data.padre.PadreRepository
import py.edu.ctn.sca.padres.data.padre.PadreResult
import py.edu.ctn.sca.padres.ui.parent.Stage

data class PadreUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val hijos: List<ChildDto> = emptyList(),
    val selectedId: Int? = null,
    val summary: ParentResponse? = null,
    val stage: Stage = Stage.current(),
    val planillaId: Int? = null,
    val conductaLoading: Boolean = false,
    val conductaError: String? = null,
    val codigos: List<py.edu.ctn.sca.padres.data.RasgoConductaDto> = emptyList(),
    val anotaciones: List<AnotacionDto> = emptyList(),
    /** Curso ("Informática · 1° A") de cada hijo, sale de sus anotaciones (el resumen solo trae la especialidad). */
    val cursos: Map<Int, String> = emptyMap(),
    val conductaFiltro: ConductaFiltro = ConductaFiltro.TODO,
) {
    val hijo: ChildDto? get() = hijos.firstOrNull { it.id == selectedId }
    val hijoNombre: String get() = hijo?.let { "${it.nombre} ${it.apellido}".trim() } ?: ""

    /** "Informática 1° A", o solo la especialidad si no se conoce el curso. */
    fun cursoDe(h: ChildDto): String? = cursoLegible(cursos[h.id]) ?: h.especialidad

    val subjectsForStage: List<SubjectDto> get() = summary?.materias?.filter { Stage.from(it.etapa) == stage } ?: emptyList()
    val selectedSubject: SubjectDto?
        get() = subjectsForStage.firstOrNull { it.planillaId == planillaId } ?: subjectsForStage.firstOrNull()
    val tareasSinEntregar: Int get() = subjectsForStage.flatMap { it.tareas }.count { it.estado == "NO_ENTREGADA" }

    val items: List<ConductaItem> get() = mezclarConducta(codigos, anotaciones)
    val ultimaAnotacion: AnotacionDto?
        get() = items.filterIsInstance<ConductaItem.Anotacion>().firstOrNull()?.dto
}

class PadreViewModel(
    private val parents: ParentRepository,
    private val repo: PadreRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(PadreUiState())
    val ui: StateFlow<PadreUiState> = _ui.asStateFlow()

    init {
        load(null)
    }

    fun load(alumnoId: Int? = _ui.value.selectedId) {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = parents.summary(alumnoId)) {
                is ParentResult.Ok -> {
                    val sel = r.data.selectedAlumnoId?.takeIf { it > 0 }
                    _ui.update { prev ->
                        val first = prev.summary == null
                        val available = r.data.materias.map { Stage.from(it.etapa) }.toSet()
                        prev.copy(
                            loading = false,
                            hijos = r.data.hijos,
                            selectedId = sel,
                            summary = r.data,
                            planillaId = null,
                            stage = if (first && Stage.current() !in available) {
                                r.data.materias.firstOrNull()?.let { Stage.from(it.etapa) } ?: prev.stage
                            } else prev.stage,
                        )
                    }
                    if (sel != null) loadConducta(sel)
                }
                is ParentResult.Error -> _ui.update { it.copy(loading = false, error = r.message) }
            }
        }
    }

    fun selectChild(id: Int) {
        if (id == _ui.value.selectedId) return
        _ui.update { it.copy(conductaFiltro = ConductaFiltro.TODO, codigos = emptyList(), anotaciones = emptyList()) }
        load(id)
    }

    fun selectStage(stage: Stage) = _ui.update { it.copy(stage = stage, planillaId = null) }
    fun selectSubject(id: Int) = _ui.update { it.copy(planillaId = id) }
    fun setConductaFiltro(f: ConductaFiltro) = _ui.update { it.copy(conductaFiltro = f) }
    fun retryConducta() { _ui.value.selectedId?.let(::loadConducta) }

    private fun loadConducta(alumnoId: Int) {
        _ui.update { it.copy(conductaLoading = true, conductaError = null) }
        viewModelScope.launch {
            // Como la web: al abrir al hijo se cierran sus novedades pendientes, sin bloquear la UI.
            launch { repo.visto(alumnoId) }
            val cod = async { parents.conducta(alumnoId) }
            val ano = async { repo.anotaciones(alumnoId) }
            val c = cod.await()
            val a = ano.await()
            if (_ui.value.selectedId != alumnoId) return@launch // cambió de hijo mientras cargaba
            val error = (c as? ConductaResult.Error)?.message ?: (a as? PadreResult.Error)?.message
            _ui.update { s ->
                val anotaciones = (a as? PadreResult.Ok)?.data ?: emptyList()
                s.copy(
                    conductaLoading = false,
                    conductaError = error,
                    codigos = (c as? ConductaResult.Ok)?.data ?: emptyList(),
                    anotaciones = anotaciones,
                    cursos = anotaciones.firstNotNullOfOrNull { it.curso }?.let { s.cursos + (alumnoId to it) } ?: s.cursos,
                )
            }
        }
    }

    suspend fun foto(alumnoId: Int, anotacionId: Int): ImageBitmap? = repo.foto(alumnoId, anotacionId)
}
