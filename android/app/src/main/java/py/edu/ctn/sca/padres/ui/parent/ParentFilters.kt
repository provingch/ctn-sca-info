package py.edu.ctn.sca.padres.ui.parent

import py.edu.ctn.sca.padres.data.RasgoConductaDto
import py.edu.ctn.sca.padres.data.SubjectDto
import py.edu.ctn.sca.padres.data.TaskDto
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Port de `frontend/src/pages/parent/parentFilters.ts` (funciones puras, sin Compose ni Android).
 * Si cambia la lógica en la web, cambiarla acá también: las dos pantallas tienen que filtrar igual.
 */

/** Con menos materias que esto la lista se ve entera de una: el buscador sólo aparece con listas largas. */
const val MATERIAS_PARA_BUSCADOR = 6

private val MARCAS_DIACRITICAS = Regex("\\p{Mn}+")

private fun normalizar(valor: String?): String =
    Normalizer.normalize(valor ?: "", Normalizer.Form.NFD)
        .replace(MARCAS_DIACRITICAS, "")
        .lowercase()
        .trim()

/** true si `busqueda` (vacía = todo) aparece en alguno de los campos, sin distinguir tildes ni mayúsculas. */
fun coincideBusqueda(busqueda: String, vararg campos: String?): Boolean {
    val termino = normalizar(busqueda)
    if (termino.isEmpty()) return true
    return normalizar(campos.joinToString(" ") { it ?: "" }).contains(termino)
}

fun filtrarMaterias(materias: List<SubjectDto>, busqueda: String): List<SubjectDto> =
    materias.filter { coincideBusqueda(busqueda, it.materia) }

/** `desde` / `hasta` en `yyyy-MM-dd`; vacío = sin límite. */
data class FiltroConducta(val materia: String = "", val desde: String = "", val hasta: String = "")

fun hayFiltroConducta(filtro: FiltroConducta): Boolean =
    filtro.materia.isNotEmpty() || filtro.desde.isNotEmpty() || filtro.hasta.isNotEmpty()

/** "Desde" posterior a "hasta": no hay ninguna fecha que cumpla, es un error del usuario y no un resultado vacío. */
fun rangoInvalido(filtro: FiltroConducta): Boolean =
    filtro.desde.isNotEmpty() && filtro.hasta.isNotEmpty() && filtro.desde > filtro.hasta

/** Materias que realmente aparecen en las notas del hijo (así ninguna opción del filtro da una lista vacía). */
fun materiasDeConducta(notas: List<RasgoConductaDto>): List<String> =
    notas.mapNotNull { it.materia?.trim()?.takeIf { materia -> materia.isNotEmpty() } }
        .distinct()
        .sortedWith(Collator.getInstance(Locale.forLanguageTag("es")))

fun filtrarConducta(notas: List<RasgoConductaDto>, filtro: FiltroConducta): List<RasgoConductaDto> {
    if (rangoInvalido(filtro)) return emptyList()
    val conRango = filtro.desde.isNotEmpty() || filtro.hasta.isNotEmpty()
    return notas.filter { nota ->
        if (filtro.materia.isNotEmpty() && nota.materia != filtro.materia) return@filter false
        if (!conRango) return@filter true
        val fecha = nota.fechaClase?.take(10) ?: return@filter false // una nota sin fecha no puede estar dentro de un rango
        (filtro.desde.isEmpty() || fecha >= filtro.desde) && (filtro.hasta.isEmpty() || fecha <= filtro.hasta)
    }
}

// ---- Detalle de tareas: filtro por mes y agrupación por mes ----

val NOMBRES_MESES_LARGOS = arrayOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)

/** Clave de las tareas sin fecha válida; van al final de la lista. */
const val SIN_FECHA = "sin-fecha"

private val ANIO_MES = Regex("^(\\d{4})-(\\d{2})")

/** "YYYY-MM" de la fecha de una tarea (o [SIN_FECHA]). */
fun mesDeTarea(fecha: String?): String {
    val (anio, mes) = fecha?.let { ANIO_MES.find(it) }?.destructured ?: return SIN_FECHA
    return if (mes.toInt() in 1..12) "$anio-$mes" else SIN_FECHA
}

fun etiquetaMes(mes: String): String {
    if (mes == SIN_FECHA) return "Sin fecha"
    val (anio, numero) = mes.split("-")
    return "${NOMBRES_MESES_LARGOS[numero.toInt() - 1]} $anio"
}

/** `numero` es la posición en la lista cronológica completa: no cambia al filtrar por mes. */
data class TareaNumerada(val tarea: TaskDto, val numero: Int)

/** Cronológico (por fecha y después por id), con las tareas sin fecha al final. */
fun ordenarTareas(tareas: List<TaskDto>): List<TareaNumerada> =
    tareas.sortedWith(
        compareBy<TaskDto> { mesDeTarea(it.fecha) == SIN_FECHA }
            .thenBy { it.fecha ?: "" }
            .thenBy { it.id },
    ).mapIndexed { indice, tarea -> TareaNumerada(tarea, indice + 1) }

data class OpcionMes(val value: String, val label: String)

/** Los meses que realmente tienen tareas, en orden cronológico. */
fun mesesConTareas(tareas: List<TaskDto>): List<OpcionMes> =
    ordenarTareas(tareas).map { mesDeTarea(it.tarea.fecha) }.distinct().map { OpcionMes(it, etiquetaMes(it)) }

data class GrupoDeTareas(val mes: String, val label: String, val tareas: List<TareaNumerada>)

/** Con `mes` vacío ("Todos") agrupa todas por mes; con un mes puntual queda sólo ese grupo. */
fun agruparTareasPorMes(tareas: List<TaskDto>, mes: String = ""): List<GrupoDeTareas> =
    ordenarTareas(tareas)
        .filter { mes.isEmpty() || mesDeTarea(it.tarea.fecha) == mes }
        .groupBy { mesDeTarea(it.tarea.fecha) } // LinkedHashMap: conserva el orden cronológico
        .map { (clave, lista) -> GrupoDeTareas(clave, etiquetaMes(clave), lista) }
