package py.edu.ctn.sca.padres.ui.profesor

import py.edu.ctn.sca.padres.data.profesor.AlumnoRiesgoDto
import py.edu.ctn.sca.padres.data.profesor.BloqueHoyDto
import py.edu.ctn.sca.padres.data.profesor.ClaseDadaDto
import py.edu.ctn.sca.padres.data.profesor.PlanHistorialDto
import py.edu.ctn.sca.padres.data.profesor.TemaPlanDto
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es")

/** "Miércoles 21 de octubre". */
fun fechaLarga(d: LocalDate): String =
    d.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() } +
        " ${d.dayOfMonth} de ${d.month.getDisplayName(TextStyle.FULL, ES)}"

/** "dd/MM" desde un ISO "2026-10-21..." (o null). */
fun fechaCorta(iso: String?): String? = parseFecha(iso)?.let { "%02d/%02d".format(it.dayOfMonth, it.monthValue) }

fun parseFecha(iso: String?): LocalDate? = iso?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

fun hora(s: String) = s.take(5)

/** Índice del bloque "actual o próximo" sin registrar (el que lleva el botón Iniciar relleno); null si no hay. */
fun indiceProximo(bloques: List<BloqueHoyDto>, ahora: LocalTime): Int? {
    val pendientes = bloques.indices.filter { !bloques[it].registrada }
    return pendientes.firstOrNull { i ->
        runCatching { LocalTime.parse(bloques[i].horaFin.take(5)) >= ahora }.getOrDefault(true)
    } ?: pendientes.firstOrNull()
}

/** Materias distintas del día, en orden de aparición, separadas por coma. */
fun materiasDelDia(bloques: List<BloqueHoyDto>) = bloques.map { it.materiaNombre }.distinct().joinToString(", ")

/** Último plan por asignación/etapa/año; devuelve los rechazados. */
fun planesRechazados(planes: List<PlanHistorialDto>): List<PlanHistorialDto> =
    planes.groupBy { Triple(it.asignacionId, it.etapa, it.anio) }
        .map { (_, v) -> v.maxBy { it.id } }
        .filter { it.estado.equals("RECHAZADO", ignoreCase = true) }

/** Alumnos en riesgo agrupados por curso (cursoId). */
fun riesgoPorCurso(alumnos: List<AlumnoRiesgoDto>): List<List<AlumnoRiesgoDto>> = alumnos.groupBy { it.cursoId }.values.toList()

data class ResumenAsistencia(val presentes: Int, val ausentes: Int, val conductas: Int)

fun resumenAsistencia(total: Int, ausentes: Set<Int>, codigos: Map<Int, Set<String>>): ResumenAsistencia {
    val aus = ausentes.size
    return ResumenAsistencia(total - aus, aus, codigos.count { it.value.isNotEmpty() })
}

/** Temas pendientes del plan en orden (mes, bloque); si todos están cubiertos, todos. Máx. 150 chars como el web. */
fun temasSugeridos(temas: List<TemaPlanDto>): List<String> {
    val ordenados = temas.sortedWith(compareBy({ it.ordenMes }, { it.bloque }))
    val pendientes = ordenados.filter { !it.estadoCobertura.equals("CUBIERTO", ignoreCase = true) }
    return (pendientes.ifEmpty { ordenados }).map { it.temasContenidos.trim().take(150) }.filter { it.isNotEmpty() }.distinct()
}

/** Clases agrupadas por fecha, más recientes primero. */
fun clasesPorFecha(clases: List<ClaseDadaDto>): List<Pair<LocalDate?, List<ClaseDadaDto>>> =
    clases.sortedWith(compareByDescending<ClaseDadaDto> { parseFecha(it.fechaClase) ?: LocalDate.MIN }.thenByDescending { it.id })
        .groupBy { parseFecha(it.fechaClase) }.toList()

/** Texto tipeado → puntaje entero entre 0 y [max]; "" → null (igual que normalizeGradeInput del web). */
fun parsePuntaje(texto: String, max: Int): Int? {
    val digitos = texto.filter { it.isDigit() }.take(3)
    if (digitos.isEmpty()) return null
    return digitos.toInt().coerceIn(0, max)
}

/** Nota normalizada a escala /10 para el color. */
fun sobreDiez(puntos: Int, total: Int): Double = if (total <= 0) 0.0 else puntos * 10.0 / total

/** Primer índice (después de [desde], con vuelta) cuyo valor está vacío; null si todos tienen nota. */
fun siguienteSinNota(valores: List<Int?>, desde: Int = -1): Int? {
    if (valores.isEmpty()) return null
    val n = valores.size
    return (1..n).map { (desde + it).mod(n) }.firstOrNull { valores[it] == null }
}
