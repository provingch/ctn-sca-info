package py.edu.ctn.sca.padres.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.data.AuthRepository
import py.edu.ctn.sca.padres.data.ParentRepository
import py.edu.ctn.sca.padres.data.ParentResponse
import py.edu.ctn.sca.padres.data.ParentResult
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
) {
    val subjectsForStage: List<SubjectDto>
        get() = data?.materias?.filter { Stage.from(it.etapa) == stage } ?: emptyList()

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
                is ParentResult.Ok -> _ui.update { prev ->
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
                    )
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

    fun selectStage(stage: Stage) = _ui.update { it.copy(stage = stage, selectedPlanillaId = null) }

    fun selectSubject(planillaId: Int) = _ui.update { it.copy(selectedPlanillaId = planillaId) }

    fun refresh() = load(alumnoId = _ui.value.data?.selectedAlumnoId, isRefresh = true)

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
