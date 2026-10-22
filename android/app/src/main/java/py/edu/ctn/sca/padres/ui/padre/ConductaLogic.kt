package py.edu.ctn.sca.padres.ui.padre

import py.edu.ctn.sca.padres.data.RasgoConductaDto
import py.edu.ctn.sca.padres.data.padre.AnotacionDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Ítem de la pestaña Conducta: código N de un profesor o anotación de Preceptoría. */
sealed interface ConductaItem {
    /** Con hora solo las anotaciones (y los códigos si el backend la manda). */
    val fecha: LocalDateTime?
    val conHora: Boolean

    data class Codigo(val dto: RasgoConductaDto, override val fecha: LocalDateTime?, override val conHora: Boolean = false) : ConductaItem
    data class Anotacion(val dto: AnotacionDto, override val fecha: LocalDateTime?) : ConductaItem {
        override val conHora get() = fecha != null
    }
}

enum class ConductaFiltro { TODO, CODIGOS, ANOTACIONES }

enum class ConductaSeccion(val titulo: String) { ESTA_SEMANA("Esta semana"), ANTES("Antes") }

/** Acepta `yyyy-MM-dd`, `yyyy-MM-dd HH:mm[:ss]` y `yyyy-MM-ddTHH:mm[:ss]`. Null si no se puede leer. */
fun parseFecha(value: String?): Pair<LocalDateTime, Boolean>? {
    val v = value?.trim()?.takeIf { it.length >= 10 } ?: return null
    return runCatching {
        val d = LocalDate.parse(v.take(10))
        if (v.length >= 16) LocalDateTime.of(d, LocalTime.parse(v.substring(11, 16))) to true
        else d.atStartOfDay() to false
    }.getOrNull()
}

fun codigoItem(dto: RasgoConductaDto): ConductaItem.Codigo {
    val p = parseFecha(dto.fechaClase)
    return ConductaItem.Codigo(dto, p?.first, p?.second ?: false)
}

fun anotacionItem(dto: AnotacionDto) = ConductaItem.Anotacion(dto, parseFecha(dto.fechaHora)?.first)

/** Todo junto, más nuevo primero; los sin fecha al final (orden estable). */
fun mezclarConducta(codigos: List<RasgoConductaDto>, anotaciones: List<AnotacionDto>): List<ConductaItem> =
    (codigos.map(::codigoItem) + anotaciones.map(::anotacionItem))
        .sortedWith(compareByDescending<ConductaItem> { it.fecha != null }.thenByDescending { it.fecha })

fun filtrarConductaItems(items: List<ConductaItem>, filtro: ConductaFiltro): List<ConductaItem> = when (filtro) {
    ConductaFiltro.TODO -> items
    ConductaFiltro.CODIGOS -> items.filterIsInstance<ConductaItem.Codigo>()
    ConductaFiltro.ANOTACIONES -> items.filterIsInstance<ConductaItem.Anotacion>()
}

/** "Esta semana" = últimos 7 días (incluido hoy); lo demás y lo sin fecha, "Antes". Secciones vacías se omiten. */
fun seccionarConducta(items: List<ConductaItem>, hoy: LocalDate = LocalDate.now()): List<Pair<ConductaSeccion, List<ConductaItem>>> =
    items.groupBy { i ->
        val d = i.fecha?.toLocalDate()
        if (d != null && !d.isBefore(hoy.minusDays(7))) ConductaSeccion.ESTA_SEMANA else ConductaSeccion.ANTES
    }.toList().sortedBy { it.first.ordinal }

private val Hora = DateTimeFormatter.ofPattern("HH:mm")
private val DiaMes = DateTimeFormatter.ofPattern("dd/MM")
private val Es = Locale.forLanguageTag("es")

/** "Hoy 07:10", "Ayer 07:15", "Lun 14/10" (misma semana), "07/10" (antes). */
fun cuandoConducta(item: ConductaItem, hoy: LocalDate = LocalDate.now()): String {
    val dt = item.fecha ?: return "Sin fecha"
    val d = dt.toLocalDate()
    val hora = if (item.conHora) " " + dt.format(Hora) else ""
    return when {
        d == hoy -> "Hoy$hora"
        d == hoy.minusDays(1) -> "Ayer$hora"
        !d.isBefore(hoy.minusDays(7)) ->
            d.dayOfWeek.getDisplayName(TextStyle.SHORT, Es).trimEnd('.').replaceFirstChar { it.uppercase() } + " " + d.format(DiaMes)
        else -> d.format(DiaMes)
    }
}

/** "Preceptoría" cuando la anotación no tiene materia ni profesor. */
fun origenAnotacion(a: AnotacionDto): String =
    listOfNotNull(a.materia, a.profesor).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { "Preceptoría" }

/** "21/10/26 07:10 · Preceptoría" */
fun fechaCompleta(a: AnotacionDto): String {
    val dt = parseFecha(a.fechaHora)?.first ?: return origenAnotacion(a)
    return dt.format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")) + " · " + origenAnotacion(a)
}

/** "Informática · 1° A" -> "Informática 1° A" */
fun cursoLegible(curso: String?): String? = curso?.replace(" · ", " ")?.trim()?.takeIf { it.isNotEmpty() }

/** Chip "Todo 5", "Códigos N 2", "Anotaciones 3". */
data class ConductaConteo(val todo: Int, val codigos: Int, val anotaciones: Int)

fun contarConducta(items: List<ConductaItem>) = ConductaConteo(
    todo = items.size,
    codigos = items.count { it is ConductaItem.Codigo },
    anotaciones = items.count { it is ConductaItem.Anotacion },
)
