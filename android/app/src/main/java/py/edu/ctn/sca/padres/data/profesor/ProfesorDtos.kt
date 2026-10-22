package py.edu.ctn.sca.padres.data.profesor

import kotlinx.serialization.Serializable

@Serializable
data class BloqueHoyDto(
    val asignacionId: Int = 0,
    val cursoId: Int = 0,
    val materiaNombre: String = "",
    val cursoDescripcion: String = "",
    val salaNombre: String? = null,
    val horaInicio: String = "",
    val horaFin: String = "",
    val horasCatedra: Int = 0,
    val registrada: Boolean = false,
)

@Serializable
data class PlanHistorialDto(
    val id: Int = 0,
    val estado: String = "",
    val observacionesEvaluador: String? = null,
    val etapa: String = "",
    val anio: Int = 0,
    val materiaNombre: String = "",
    val cursoOrdinal: String = "",
    val seccion: String = "",
    val asignacionId: Int = 0,
)

@Serializable
data class AlumnosRiesgoDto(val alumnos: List<AlumnoRiesgoDto> = emptyList())

@Serializable
data class AlumnoRiesgoDto(
    val alumnoId: Int = 0,
    val nombreCompleto: String = "",
    val cursoId: Int = 0,
    val cursoNombre: String = "",
    val especialidadNombre: String = "",
    val seccion: String = "",
    val tareasNoEntregadas: Int = 0,
    val notasConductuales: Int = 0,
    val motivos: List<String> = emptyList(),
)

@Serializable
data class PlanillaResumenDto(
    val id: Int = 0,
    val materiaNombre: String = "",
    val cursoOrdinal: String = "",
    val seccion: String = "",
    val especialidadNombre: String = "",
    val etapa: Int = 1,
)

@Serializable
data class AlumnoDto(val id: Int = 0, val nombre: String = "", val apellido: String = "") {
    val nombreCompleto get() = if (apellido.isBlank()) nombre else "$apellido, $nombre"
}

@Serializable
data class InstrumentoDto(val id: Int = 0, val nombre: String = "")

/** Subconjunto de HomeResponse: planillas (sin cursoId) y, con cursoId, alumnos + instrumentos de la clase. */
@Serializable
data class HomeDto(
    val selEtapa: Int = 1,
    val planillasResumen: List<PlanillaResumenDto> = emptyList(),
    val rasgoAlumnosValidos: List<AlumnoDto> = emptyList(),
    val instrumentos: List<InstrumentoDto> = emptyList(),
)

@Serializable
data class CodigoConductaDto(val id: Int = 0, val codigo: String = "", val descripcion: String = "")

@Serializable
data class TemaPlanDto(
    val ordenMes: Int = 0,
    val bloque: Int = 0,
    val temasContenidos: String = "",
    val estadoCobertura: String? = null,
)

@Serializable
data class PlanEstadoDto(val estado: String = "", val temas: List<TemaPlanDto> = emptyList())

/** Mismos campos y nombres que CreateRasgoPlanillaRequest (sin defaults para que siempre se envíen). */
@Serializable
data class CrearClaseRequest(
    val cursoId: Int,
    val asignacionId: Int,
    val instrumentoId: Int,
    val horaInicio: String,
    val horasCatedra: Int,
    val modalidad: String,
    val observaciones: String,
    val tema: String,
    val justificacionAtraso: String? = null,
    val alumnosAusentes: List<Int>,
    val codigosPorAlumno: Map<Int, List<String>>,
)

@Serializable
data class ClaseDadaDto(
    val id: Int = 0,
    val fechaClase: String? = null,
    val tema: String = "",
    val cursoDescripcion: String = "",
    val materiaNombre: String? = null,
    val totalAlumnos: Int = 0,
    val totalAusentes: Int = 0,
)

@Serializable
data class AsistenciaDto(
    val id: Int = 0,
    val alumnoId: Int = 0,
    val alumnoNombreCompleto: String = "",
    val estado: String = "",
    val codigos: List<String> = emptyList(),
)

@Serializable
data class ClaseDetalleDto(
    val id: Int = 0,
    val tema: String = "",
    val fechaClase: String? = null,
    val asistencias: List<AsistenciaDto> = emptyList(),
)

@Serializable
data class TareaDto(
    val id: Int = 0,
    val planillaId: Int = 0,
    val fecha: String? = null,
    val total: Int = 10,
    val titulo: String = "",
    val googleCourseworkId: String? = null,
)

@Serializable
data class PlanillaHeaderDto(
    val id: Int = 0,
    val materiaNombre: String = "",
    val etapaIndex: Int = 1,
)

@Serializable
data class CursoPlanillaDto(val especialidad: String = "", val seccion: String = "", val nivel: Int = 0)

@Serializable
data class GradeValueDto(val tareaId: Int = 0, val puntos: Int? = null)

@Serializable
data class StudentRowDto(
    val alumnoId: Int = 0,
    val alumnoNombre: String = "",
    val grades: List<GradeValueDto> = emptyList(),
)

@Serializable
data class PlanillaDetalleDto(
    val planilla: PlanillaHeaderDto = PlanillaHeaderDto(),
    val curso: CursoPlanillaDto? = null,
    val tareas: List<TareaDto> = emptyList(),
    val rows: List<StudentRowDto> = emptyList(),
)

/** Cuerpo de POST /api/planillas/{id}/notas: { grades: [{ alumnoId, items: [{ tareaId, puntos }] }] }. */
@Serializable
data class GuardarNotasRequest(val grades: List<AlumnoNotas>)

@Serializable
data class AlumnoNotas(val alumnoId: Int, val items: List<ItemNota>)

@Serializable
data class ItemNota(val tareaId: Int, val puntos: Int)
