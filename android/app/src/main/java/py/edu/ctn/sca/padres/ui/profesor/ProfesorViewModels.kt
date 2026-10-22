package py.edu.ctn.sca.padres.ui.profesor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import py.edu.ctn.sca.padres.data.profesor.AlumnoDto
import py.edu.ctn.sca.padres.data.profesor.AlumnoRiesgoDto
import py.edu.ctn.sca.padres.data.profesor.BloqueHoyDto
import py.edu.ctn.sca.padres.data.profesor.ClaseDadaDto
import py.edu.ctn.sca.padres.data.profesor.ClaseDetalleDto
import py.edu.ctn.sca.padres.data.profesor.CodigoConductaDto
import py.edu.ctn.sca.padres.data.profesor.CrearClaseRequest
import py.edu.ctn.sca.padres.data.profesor.InstrumentoDto
import py.edu.ctn.sca.padres.data.profesor.PlanHistorialDto
import py.edu.ctn.sca.padres.data.profesor.PlanillaDetalleDto
import py.edu.ctn.sca.padres.data.profesor.PlanillaResumenDto
import py.edu.ctn.sca.padres.data.profesor.ProfesorRepository
import py.edu.ctn.sca.padres.data.profesor.TareaDto

const val MSG_ATRASO = "Se requiere justificar el atraso para este tema."

// ---------------------------------------------------------------- Hoy

data class HoyUi(
    val loading: Boolean = true,
    val error: String? = null,
    val bloques: List<BloqueHoyDto>? = null,
    val planes: List<PlanHistorialDto> = emptyList(),
    val riesgo: List<AlumnoRiesgoDto> = emptyList(),
)

class HoyViewModel(private val repo: ProfesorRepository) : ViewModel() {
    private val _ui = MutableStateFlow(HoyUi())
    val ui: StateFlow<HoyUi> = _ui.asStateFlow()

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val planes = async { repo.misPlanes() }
            val riesgo = async { repo.alumnosRiesgo() }
            val horario = repo.horarioHoy()
            _ui.update {
                it.copy(
                    loading = false,
                    error = horario.exceptionOrNull()?.message,
                    bloques = horario.getOrNull() ?: it.bloques,
                    planes = planes.await().getOrNull()?.let(::planesRechazados) ?: emptyList(),
                    riesgo = riesgo.await().getOrNull() ?: emptyList(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Registro de clase

data class RegistroUi(
    val loading: Boolean = true,
    val error: String? = null,
    val bloque: BloqueHoyDto? = null,
    val alumnos: List<AlumnoDto> = emptyList(),
    val instrumentos: List<InstrumentoDto> = emptyList(),
    val codigos: List<CodigoConductaDto> = emptyList(),
    val temas: List<String> = emptyList(),
    val etapa: Int = 1,
    val modalidad: String = "Presencial",
    val instrumentoId: Int? = null,
    val tema: String = "",
    val ausentes: Set<Int> = emptySet(),
    val codigosPorAlumno: Map<Int, Set<String>> = emptyMap(),
    val saving: Boolean = false,
    val saveError: String? = null,
    val pedirJustificacion: Boolean = false,
    val guardada: Boolean = false,
) {
    val resumen get() = resumenAsistencia(alumnos.size, ausentes, codigosPorAlumno)
    val puedeGuardar get() = !saving && bloque != null && !bloque.registrada && alumnos.isNotEmpty() &&
        instrumentoId != null && tema.isNotBlank()
}

val MODALIDADES = listOf("Presencial", "Virtual")

class RegistroViewModel(
    private val repo: ProfesorRepository,
    private val asignacionId: Int,
    private val horaInicio: String,
) : ViewModel() {
    private val _ui = MutableStateFlow(RegistroUi())
    val ui: StateFlow<RegistroUi> = _ui.asStateFlow()

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val bloque = repo.horarioHoy().getOrElse { e -> return@launch fail(e.message) }
                .firstOrNull { it.asignacionId == asignacionId && it.horaInicio == horaInicio }
                ?: return@launch fail("No se encontró esa clase en tu horario de hoy.")
            val codigos = async { repo.codigosConducta() }
            val home = repo.home(cursoId = bloque.cursoId).getOrElse { e -> return@launch fail(e.message) }
            val cods = codigos.await().getOrElse { e -> return@launch fail(e.message) }
            val temas = repo.miPlan(bloque.asignacionId, home.selEtapa).getOrNull()?.let { temasSugeridos(it.temas) } ?: emptyList()
            _ui.update {
                it.copy(
                    loading = false, bloque = bloque, alumnos = home.rasgoAlumnosValidos, instrumentos = home.instrumentos,
                    codigos = cods, temas = temas, etapa = home.selEtapa,
                )
            }
        }
    }

    private fun fail(msg: String?) = _ui.update { it.copy(loading = false, error = msg ?: "No se pudo cargar la clase.") }

    fun setModalidad(v: String) = _ui.update { it.copy(modalidad = v) }
    fun setInstrumento(id: Int) = _ui.update { it.copy(instrumentoId = id) }
    fun setTema(v: String) = _ui.update { it.copy(tema = v.take(150)) }

    fun toggleAusente(alumnoId: Int) = _ui.update {
        it.copy(ausentes = if (alumnoId in it.ausentes) it.ausentes - alumnoId else it.ausentes + alumnoId)
    }

    fun setCodigos(alumnoId: Int, codigos: Set<String>) = _ui.update { it.copy(codigosPorAlumno = it.codigosPorAlumno + (alumnoId to codigos)) }

    fun cancelarJustificacion() = _ui.update { it.copy(pedirJustificacion = false) }

    /** Un único POST create-rasgo-planilla, como el web: ausentes y códigos viajan en el mismo request. */
    fun guardar(justificacion: String? = null) {
        val s = _ui.value
        val b = s.bloque ?: return
        val instrumento = s.instrumentoId ?: return
        if (!s.puedeGuardar) return
        _ui.update { it.copy(saving = true, saveError = null, pedirJustificacion = false) }
        viewModelScope.launch {
            val req = CrearClaseRequest(
                cursoId = b.cursoId, asignacionId = b.asignacionId, instrumentoId = instrumento,
                horaInicio = b.horaInicio, horasCatedra = b.horasCatedra, modalidad = s.modalidad,
                observaciones = "", tema = s.tema.trim(), justificacionAtraso = justificacion,
                alumnosAusentes = s.ausentes.toList(),
                codigosPorAlumno = s.codigosPorAlumno.filterValues { it.isNotEmpty() }.mapValues { it.value.toList() },
            )
            repo.crearClase(req)
                .onSuccess { _ui.update { it.copy(saving = false, guardada = true) } }
                .onFailure { e ->
                    _ui.update {
                        if (e.message == MSG_ATRASO) it.copy(saving = false, pedirJustificacion = true)
                        else it.copy(saving = false, saveError = e.message)
                    }
                }
        }
    }
}

// ---------------------------------------------------------------- Clases

data class ClasesUi(val loading: Boolean = true, val error: String? = null, val clases: List<ClaseDadaDto>? = null)

class ClasesViewModel(private val repo: ProfesorRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ClasesUi())
    val ui: StateFlow<ClasesUi> = _ui.asStateFlow()

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val r = repo.misClases()
            _ui.update { it.copy(loading = false, error = r.exceptionOrNull()?.message, clases = r.getOrNull() ?: it.clases) }
        }
    }
}

data class ClaseDetalleUi(val loading: Boolean = true, val error: String? = null, val clase: ClaseDetalleDto? = null)

class ClaseDetalleViewModel(private val repo: ProfesorRepository, private val id: Int) : ViewModel() {
    private val _ui = MutableStateFlow(ClaseDetalleUi())
    val ui: StateFlow<ClaseDetalleUi> = _ui.asStateFlow()

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val r = repo.miClase(id)
            _ui.update { it.copy(loading = false, error = r.exceptionOrNull()?.message, clase = r.getOrNull()) }
        }
    }
}

// ---------------------------------------------------------------- Planillas / tareas

data class PlanillasUi(
    val loading: Boolean = true,
    val error: String? = null,
    val etapa: Int = 1,
    val planillas: List<PlanillaResumenDto>? = null,
)

class PlanillasViewModel(private val repo: ProfesorRepository) : ViewModel() {
    private val _ui = MutableStateFlow(PlanillasUi())
    val ui: StateFlow<PlanillasUi> = _ui.asStateFlow()

    init { load(1) }

    fun load(etapa: Int = _ui.value.etapa) {
        _ui.update { it.copy(loading = true, error = null, etapa = etapa) }
        viewModelScope.launch {
            val r = repo.home(etapa = etapa)
            _ui.update {
                it.copy(loading = false, error = r.exceptionOrNull()?.message, planillas = r.getOrNull()?.planillasResumen ?: it.planillas)
            }
        }
    }
}

data class TareasUi(
    val loading: Boolean = true,
    val error: String? = null,
    val tareas: List<TareaDto>? = null,
    val detalle: PlanillaDetalleDto? = null,
)

class TareasViewModel(private val repo: ProfesorRepository, private val planillaId: Int) : ViewModel() {
    private val _ui = MutableStateFlow(TareasUi())
    val ui: StateFlow<TareasUi> = _ui.asStateFlow()

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val detalle = async { repo.planilla(planillaId) }
            val tareas = repo.tareas(planillaId)
            val d = detalle.await()
            _ui.update {
                it.copy(
                    loading = false, error = (tareas.exceptionOrNull() ?: d.exceptionOrNull())?.message,
                    tareas = tareas.getOrNull() ?: it.tareas, detalle = d.getOrNull() ?: it.detalle,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- TP corrección

data class CorreccionUi(
    val loading: Boolean = true,
    val error: String? = null,
    val detalle: PlanillaDetalleDto? = null,
    val tarea: TareaDto? = null,
    /** alumnoId → puntos (null = sin nota). */
    val notas: Map<Int, Int?> = emptyMap(),
    val ordenSinNotaPrimero: Boolean = false,
    val saveError: String? = null,
    val focusAlumnoId: Int? = null,
) {
    val rows get() = detalle?.rows.orEmpty().let { r ->
        if (ordenSinNotaPrimero) r.sortedBy { notas[it.alumnoId] != null } else r
    }
    val cargadas get() = notas.values.count { it != null }
}

class CorreccionViewModel(
    private val repo: ProfesorRepository,
    private val planillaId: Int,
    private val tareaId: Int,
) : ViewModel() {
    private val _ui = MutableStateFlow(CorreccionUi())
    val ui: StateFlow<CorreccionUi> = _ui.asStateFlow()
    private val jobs = HashMap<Int, Job>()
    /** Notas tipeadas en los últimos 700 ms: se envían igual aunque el profesor salga de la pantalla. */
    private val pendientes = HashMap<Int, Int>()

    init { load() }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val r = repo.planilla(planillaId)
            _ui.update {
                val d = r.getOrNull()
                val t = d?.tareas?.firstOrNull { x -> x.id == tareaId }
                when {
                    r.isFailure -> it.copy(loading = false, error = r.exceptionOrNull()?.message)
                    t == null -> it.copy(loading = false, error = "No se encontró la tarea.")
                    else -> it.copy(
                        loading = false, detalle = d, tarea = t,
                        notas = d.rows.associate { row -> row.alumnoId to row.grades.firstOrNull { g -> g.tareaId == tareaId }?.puntos },
                    )
                }
            }
        }
    }

    fun toggleOrden() = _ui.update { it.copy(ordenSinNotaPrimero = !it.ordenSinNotaPrimero) }

    fun siguienteSinNota() {
        val s = _ui.value
        val ids = s.rows.map { it.alumnoId }
        val actual = s.focusAlumnoId?.let(ids::indexOf) ?: -1
        val i = siguienteSinNota(ids.map { s.notas[it] }, actual)
        _ui.update { it.copy(focusAlumnoId = i?.let(ids::get)) }
    }

    /** Autoguardado con debounce; vaciar el campo no envía nada (el backend lo tomaría como 0, igual que el web). */
    fun setNota(alumnoId: Int, texto: String) {
        val total = _ui.value.tarea?.total ?: return
        val puntos = parsePuntaje(texto, total)
        _ui.update { it.copy(notas = it.notas + (alumnoId to puntos), focusAlumnoId = null) }
        jobs.remove(alumnoId)?.cancel()
        pendientes.remove(alumnoId)
        if (puntos == null) return
        pendientes[alumnoId] = puntos
        jobs[alumnoId] = viewModelScope.launch {
            delay(700)
            pendientes.remove(alumnoId)
            repo.guardarNota(planillaId, alumnoId, tareaId, puntos)
                .onSuccess { _ui.update { it.copy(saveError = null) } }
                .onFailure { e -> _ui.update { it.copy(saveError = e.message) } }
        }
    }

    override fun onCleared() {
        if (pendientes.isEmpty()) return
        val porGuardar = pendientes.toMap()
        pendientes.clear()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            porGuardar.forEach { (alumnoId, puntos) -> repo.guardarNota(planillaId, alumnoId, tareaId, puntos) }
        }
    }
}
