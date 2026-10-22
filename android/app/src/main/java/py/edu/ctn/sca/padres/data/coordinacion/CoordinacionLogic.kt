package py.edu.ctn.sca.padres.data.coordinacion

import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ---- Quejas (espejo de frontend/src/api/quejas.ts y pages/coordinacion/CoordinacionPage.tsx) ----

enum class QuejaEstado(val label: String) {
    PENDIENTE("Pendiente"), ACEPTADA("Aceptada"), REVISADA("Revisada"), RESUELTA("Resuelta"), RECHAZADA("Rechazada")
}

/** Orden de evaluación del web: rechazada (terminal) > resuelta > revisada > aceptada > pendiente. */
fun QuejaDto.estadoEfectivo(): QuejaEstado = when {
    estado == "rechazada" || rechazadaEn.presente() -> QuejaEstado.RECHAZADA
    estado == "resuelta" || resueltaEn.presente() -> QuejaEstado.RESUELTA
    estado == "revisada" || revisadaEn.presente() -> QuejaEstado.REVISADA
    estado == "aceptada" || aceptadaEn.presente() -> QuejaEstado.ACEPTADA
    else -> QuejaEstado.PENDIENTE
}

private fun JsonPrimitive?.presente() = this != null && content.isNotBlank() && content != "null"

val QuejaDto.sobreCurso get() = tipo == "CONTRA_CURSO"

enum class QuejaFiltro { PENDIENTES, CONTRA_PROFESORES, SOBRE_CURSOS }

/** Pendientes: solo estado pendiente. Contra profesores / Sobre cursos: dirección (como el web), todos los estados. */
fun filtrarQuejas(items: List<QuejaDto>, filtro: QuejaFiltro): List<QuejaDto> = when (filtro) {
    QuejaFiltro.PENDIENTES -> items.filter { it.estadoEfectivo() == QuejaEstado.PENDIENTE }
    QuejaFiltro.CONTRA_PROFESORES -> items.filter { !it.sobreCurso }
    QuejaFiltro.SOBRE_CURSOS -> items.filter { it.sobreCurso }
}

/** Umbral visual del web (`UMBRAL = 5`): más de 5 quejas se marca en rojo. */
const val UMBRAL_QUEJAS = 5

/**
 * Número que muestra el web por grupo: cantidad de quejas (de cualquier estado) del mismo
 * profesor (dirección contra profesor) o del mismo curso (sobre curso).
 */
fun cantidadPorObjetivo(todas: List<QuejaDto>, q: QuejaDto): Int =
    todas.count { it.sobreCurso == q.sobreCurso && (if (q.sobreCurso) it.cursoId == q.cursoId else it.profesorId == q.profesorId) }

private fun primeraPalabra(s: String?) = s?.trim()?.substringBefore(' ').orEmpty()

/** Primera palabra del nombre + primera del apellido (utils/nombre.ts). */
fun nombreCorto(nombre: String?, apellido: String?) =
    listOf(primeraPalabra(nombre), primeraPalabra(apellido)).filter { it.isNotEmpty() }.joinToString(" ")

fun QuejaDto.cursoLabel(): String =
    listOfNotNull(cursoEspecialidad?.takeIf { it.isNotBlank() }, cursoNivel?.takeIf { it > 0 }?.let { "$it°" }, cursoSeccion?.takeIf { it.isNotBlank() })
        .joinToString(" ").ifEmpty { "Curso #$cursoId" }

fun QuejaDto.objetivo(): String =
    if (sobreCurso) cursoLabel() else nombreCorto(profesorNombre, profesorApellido).ifEmpty { "Profesor #$profesorId" }

fun QuejaDto.creada(): LocalDateTime? = parseInstante(creadaEn)

fun parseInstante(p: JsonPrimitive?): LocalDateTime? {
    val raw = p?.content?.takeIf { it.isNotBlank() && it != "null" } ?: return null
    val zone = ZoneId.systemDefault()
    return runCatching {
        raw.toLongOrNull()?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zone) }
            ?: OffsetDateTime.parse(raw).atZoneSameInstant(zone).toLocalDateTime()
    }.recoverCatching { LocalDateTime.parse(raw) }.getOrNull()
}

private val DiaHora = DateTimeFormatter.ofPattern("dd/MM HH:mm")
fun fechaCorta(dt: LocalDateTime?): String = dt?.format(DiaHora).orEmpty()

// ---- Anotaciones (espejo de AnotacionesRecientes.tsx / AnotacionesList.tsx) ----

/** Cantidad pedida al backend, igual que el web. */
const val LIMITE_ANOTACIONES = 200
const val DIAS_ANOTACIONES = 7

enum class AnotChip(val label: String) { TODAS("Todas"), UNIFORME("Uniforme"), CONDUCTA("Conducta"), CON_FOTO("Con foto") }

private val FechaSql = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun AnotacionDto.fecha(): LocalDateTime? = fechaHora?.let { runCatching { LocalDateTime.parse(it, FechaSql) }.getOrNull() }

/** Solo los últimos [DIAS_ANOTACIONES] días (hoy incluido); sin fecha válida se descartan. */
fun ultimosDias(items: List<AnotacionDto>, hoy: LocalDate = LocalDate.now()): List<AnotacionDto> =
    items.filter { a -> a.fecha()?.toLocalDate()?.let { !it.isBefore(hoy.minusDays(DIAS_ANOTACIONES.toLong())) } == true }

private fun AnotacionDto.esTipo(t: String) = tipo.trim().equals(t, ignoreCase = true)

fun AnotChip.cuenta(items: List<AnotacionDto>): Int = items.count { coincideChip(it, this) }

private fun coincideChip(a: AnotacionDto, chip: AnotChip) = when (chip) {
    AnotChip.TODAS -> true
    AnotChip.UNIFORME -> a.esTipo("uniforme")
    AnotChip.CONDUCTA -> a.esTipo("conducta")
    AnotChip.CON_FOTO -> a.tieneFoto
}

/** Búsqueda como el web (alumno, curso, tipo, profesor, materia; sin distinguir mayúsculas) + chip. */
fun filtrarAnotaciones(items: List<AnotacionDto>, texto: String, chip: AnotChip): List<AnotacionDto> {
    val t = texto.trim().lowercase()
    return items.filter { a ->
        coincideChip(a, chip) &&
            (t.isEmpty() || listOf(a.alumnoNombre, a.alumnoApellido, a.curso, a.tipo, a.profesor, a.materia).any { it?.lowercase()?.contains(t) == true })
    }
}

fun AnotacionDto.alumno() = "$alumnoApellido, $alumnoNombre"

/** "hoy 07:10" / "ayer" / "19/10" (como el render m-50). */
fun cuandoAnotacion(dt: LocalDateTime?, hoy: LocalDate = LocalDate.now()): String {
    if (dt == null) return ""
    val d = dt.toLocalDate()
    return when {
        d == hoy -> "hoy " + dt.format(DateTimeFormatter.ofPattern("HH:mm"))
        d == hoy.minusDays(1) -> "ayer"
        else -> dt.format(DateTimeFormatter.ofPattern("dd/MM"))
    }
}
