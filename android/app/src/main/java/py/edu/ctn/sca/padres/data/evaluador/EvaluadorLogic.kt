package py.edu.ctn.sca.padres.data.evaluador

/** Espeja `frontend/src/pages/evaluacion/filtrosIncumplimientos.ts`. */
enum class Categoria(val label: String) {
    BLOQUEOS("Bloqueos"),
    ATRASOS("Atrasos"),
    RETROACTIVAS("Retroactivas"),
}

fun categoriaDe(tipo: String): Categoria? = when (tipo) {
    "ATRASO" -> Categoria.ATRASOS
    "INCONGRUENCIA_RETROACTIVA" -> Categoria.RETROACTIVAS
    "BLOQUEO_INCONGRUENCIA_RETROACTIVA", "BLOQUEO_ATRASO_TIEMPO_REAL" -> Categoria.BLOQUEOS
    else -> null
}

/** `null` = todas las categorías. */
fun filtrarIncumplimientos(items: List<IncumplimientoDto>, categoria: Categoria?): List<IncumplimientoDto> =
    if (categoria == null) items else items.filter { categoriaDe(it.tipo) == categoria }

fun IncumplimientoDto.profesor(): String =
    listOfNotNull(usuarioApellido, usuarioNombre).filter { it.isNotBlank() }.joinToString(" ")
        .ifEmpty { "Profesor #$usuarioId" }

fun IncumplimientoDto.esBloqueo(): Boolean = categoriaDe(tipo) == Categoria.BLOQUEOS

fun PlanResumenDto.profesor(): String = profesorNombreCorto?.takeIf { it.isNotBlank() } ?: profesorNombre

fun PlanDetalleDto.profesor(): String = profesorNombreCorto?.takeIf { it.isNotBlank() } ?: profesorNombre

data class MesPlan(val mes: String, val contenidos: String)

/** Agrupa los temas por mes (orden por `ordenMes`) uniendo los contenidos con " · ". */
fun mesesDelPlan(temas: List<TemaDto>): List<MesPlan> =
    temas.sortedWith(compareBy({ it.ordenMes }, { it.bloque }))
        .groupBy { it.mes.trim() }
        .filterKeys { it.isNotEmpty() }
        .map { (mes, ts) ->
            MesPlan(
                mes.lowercase().replaceFirstChar { it.uppercase() },
                ts.map { it.temasContenidos.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(" · "),
            )
        }

private val FECHA = Regex("""(\d{4})-(\d{2})-(\d{2})""")

/** "2026-10-20 14:30:00" / ISO -> "20/10"; null si no se reconoce. */
fun fechaCorta(raw: String?): String? =
    raw?.let { FECHA.find(it) }?.destructured?.let { (_, m, d) -> "$d/$m" }
