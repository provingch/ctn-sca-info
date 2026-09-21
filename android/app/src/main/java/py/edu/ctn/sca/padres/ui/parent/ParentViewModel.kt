package py.edu.ctn.sca.padres.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.data.AuthRepository
import py.edu.ctn.sca.padres.data.ConductaResult
import py.edu.ctn.sca.padres.data.ParentRepository
import py.edu.ctn.sca.padres.data.ParentResponse
import py.edu.ctn.sca.padres.data.ParentResult
import py.edu.ctn.sca.padres.data.RasgoConductaDto
import py.edu.ctn.sca.padres.data.SubjectDto
import java.util.Calendar

enum class Stage(val apiValue: String, val label: String) {
    PRIMERA("primera", "Primera etapa"),
    SEGUNDA("segunda", "Segunda etapa");

    companion object {
        fun current(): Stage {
            val c = Calendar.getInstance()
            val month = c.get(Calendar.MONTH) // 0-based
            val day = c.get(Calendar.DAY_OF_MONTH)
            return if (month > 6 || (month == 6 && day >= 15)) SEGUNDA else PRIMERA
        }

        fun from(value: String): Stage = entries.firstOrNull { it.apiValue == value } ?: PRIMERA
    }
}

data class ParentUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val data: ParentResponse? = null,
    val stage: Stage = Stage.current(),
    val selectedPlanillaId: Int? = null,
    val conducta: List<RasgoConductaDto> = emptyList(),
    val conductaError: String? = null,
    val conductaLoading: Boolean = false,
    val materiaSearch: String = "",
    val taskMonthFilter: String = "",
    val conductaFiltro: FiltroConducta = FiltroConducta(),
) {
    val subjectsForStage: List<SubjectDto>
        get() = data?.materias?.filter { Stage.from(it.etapa) == stage } ?: emptyList()

    /** Lo que se muestra en la grilla de materias: `subjectsForStage` filtrado por el buscador. */
    val subjectsVisible: List<SubjectDto>
        get() = filtrarMaterias(subjectsForStage, materiaSearch)

    /** Con pocas materias el buscador sobra (igual que `MATERIAS_PARA_BUSCADOR` en la web). */
    val mostrarBuscadorMateria: Boolean
        get() = subjectsForStage.size >= MATERIAS_PARA_BUSCADOR

    val selectedSubject: SubjectDto?
        get() = subjectsForStage.firstOrNull { it.planillaId == selectedPlanillaId }
            ?: subjectsForStage.firstOrNull()

    val stagePercent: Int
        get() {
            val pts = subjectsForStage.sumOf { it.puntos }
            val tot = subjectsForStage.sumOf { it.total }
            return if (tot > 0) Math.round(pts * 100.0 / tot).toInt() else 0
        }

    val pendingTasks: Int
        get() = subjectsForStage.flatMap { it.tareas }.count { it.estado != "CALIFICADA" }

    val missingTasks: Int
        get() = subjectsForStage.flatMap { it.tareas }.count { it.estado == "NO_ENTREGADA" }

    val conductaVisible: List<RasgoConductaDto>
        get() = filtrarConducta(conducta, conductaFiltro)

    val conductaMateriasDisponibles: List<String>
        get() = materiasDeConducta(conducta)
}

class ParentViewModel(
    private val parentRepository: ParentRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(ParentUiState())
    val ui: StateFlow<ParentUiState> = _ui.asStateFlow()

    init {
        load()
    }

    fun load(alumnoId: Int? = null, isRefresh: Boolean = false) {
        _ui.update {
            it.copy(
                loading = it.data == null && !isRefresh,
                refreshing = isRefresh,
                error = null,
            )
        }
        viewModelScope.launch {
            when (val result = parentRepository.summary(alumnoId)) {
                is ParentResult.Ok -> {
                    _ui.update { prev ->
                        val available = result.data.materias.map { Stage.from(it.etapa) }.toSet()
                        val preferred = when {
                            available.contains(Stage.current()) -> Stage.current()
                            else -> result.data.materias.firstOrNull()?.let { Stage.from(it.etapa) } ?: Stage.current()
                        }
                        prev.copy(
                            loading = false,
                            refreshing = false,
                            error = null,
                            data = result.data,
                            stage = if (alumnoId == null) preferred else prev.stage,
                            selectedPlanillaId = null,
                            conducta = emptyList(),
                            conductaError = null,
                            // El mes va atado a la materia elegida (que se reinicia siempre). Buscador y filtros de
                            // conducta sólo se limpian al cambiar de hijo, no al refrescar con el gesto de arrastre.
                            taskMonthFilter = "",
                            materiaSearch = if (isRefresh) prev.materiaSearch else "",
                            conductaFiltro = if (isRefresh) prev.conductaFiltro else FiltroConducta(),
                        )
                    }
                    result.data.selectedAlumnoId?.takeIf { it > 0 }?.let { loadConducta(it) }
                }
                is ParentResult.Error -> _ui.update {
                    it.copy(loading = false, refreshing = false, error = result.message)
                }
            }
        }
    }

    fun selectChild(alumnoId: Int) {
        if (alumnoId == _ui.value.data?.selectedAlumnoId) return
        load(alumnoId = alumnoId)
    }

    fun selectStage(stage: Stage) = _ui.update { it.copy(stage = stage, selectedPlanillaId = null, taskMonthFilter = "") }

    fun selectSubject(planillaId: Int) = _ui.update { it.copy(selectedPlanillaId = planillaId, taskMonthFilter = "") }

    fun setMateriaSearch(value: String) = _ui.update { it.copy(materiaSearch = value) }

    fun setTaskMonthFilter(value: String) = _ui.update { it.copy(taskMonthFilter = value) }

    fun setConductaFiltro(update: (FiltroConducta) -> FiltroConducta) =
        _ui.update { it.copy(conductaFiltro = update(it.conductaFiltro)) }

    fun clearConductaFiltro() = _ui.update { it.copy(conductaFiltro = FiltroConducta()) }

    fun refresh() = load(alumnoId = _ui.value.data?.selectedAlumnoId, isRefresh = true)

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    private fun loadConducta(alumnoId: Int) {
        _ui.update { it.copy(conductaLoading = true, conductaError = null) }
        viewModelScope.launch {
            when (val res = parentRepository.conducta(alumnoId)) {
                is ConductaResult.Ok -> _ui.update {
                    it.copy(conductaLoading = false, conducta = res.data, conductaError = null)
                }
                is ConductaResult.Error -> _ui.update {
                    it.copy(conductaLoading = false, conducta = emptyList(), conductaError = res.message)
                }
            }
        }
    }
}
