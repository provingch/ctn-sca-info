package py.edu.ctn.sca.padres.ui.parent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import py.edu.ctn.sca.padres.data.RasgoConductaDto
import py.edu.ctn.sca.padres.data.SubjectDto
import py.edu.ctn.sca.padres.data.TaskDto

/** Mismos casos que `frontend/src/pages/parent/parentFilters.test.ts`: el port tiene que comportarse igual. */
class ParentFiltersTest {
    private fun nota(materia: String?, fechaClase: String?) =
        RasgoConductaDto(fechaClase = fechaClase, materia = materia, profesorNombre = "Prof", codigo = "N1")

    private val notas = listOf(
        nota("Matemática", "2026-03-10"),
        nota("Física", "2026-04-15"),
        nota("Matemática", "2026-05-20"),
        nota(null, "2026-05-21"),
        nota("Física", null),
    )

    private fun materia(nombre: String) = SubjectDto(planillaId = 1, materiaId = 1, materia = nombre, etapa = "primera")

    private fun tarea(id: Int, fecha: String?) = TaskDto(id = id, titulo = "TP $id", fecha = fecha, puntos = 10, total = 20, estado = "CALIFICADA")

    private val tareas = listOf(
        tarea(4, "2026-05-02"), tarea(1, "2026-03-10"), tarea(3, "2026-03-25"), tarea(2, "2026-08-01"), tarea(5, null),
    )

    // ---- materias ----

    @Test fun filtrarMateriasBuscaSinTildesNiMayusculas() {
        val materias = listOf(materia("Matemática"), materia("Educación Física"), materia("Química"))
        assertEquals(listOf("Matemática"), filtrarMaterias(materias, "matematica").map { it.materia })
        assertEquals(listOf("Educación Física"), filtrarMaterias(materias, "FISICA").map { it.materia })
        assertEquals(3, filtrarMaterias(materias, "").size)
        assertEquals(3, filtrarMaterias(materias, "   ").size)
    }

    // ---- conducta ----

    @Test fun sinFiltrosDevuelveTodasIncluidasLasSinFechaOSinMateria() {
        assertEquals(5, filtrarConducta(notas, FiltroConducta()).size)
    }

    @Test fun porMateria() {
        assertEquals(2, filtrarConducta(notas, FiltroConducta(materia = "Matemática")).size)
    }

    @Test fun porRangoConLosExtremosIncluidos() {
        val r = filtrarConducta(notas, FiltroConducta(desde = "2026-04-15", hasta = "2026-05-20"))
        assertEquals(listOf("2026-04-15", "2026-05-20"), r.map { it.fechaClase })
    }

    @Test fun conSoloUnExtremoElOtroQuedaAbierto() {
        assertEquals(2, filtrarConducta(notas, FiltroConducta(desde = "2026-05-01")).size)
        assertEquals(1, filtrarConducta(notas, FiltroConducta(hasta = "2026-03-31")).size)
    }

    @Test fun unaNotaSinFechaQuedaAfueraApenasHayUnRango() {
        assertFalse(filtrarConducta(notas, FiltroConducta(desde = "2026-01-01")).any { it.fechaClase == null })
    }

    @Test fun combinaMateriaYFechas() {
        assertEquals(1, filtrarConducta(notas, FiltroConducta("Física", "2026-04-01", "2026-04-30")).size)
    }

    @Test fun aceptaUnaFechaConHoraSinSacarElUltimoDiaDelRango() {
        val conHora = listOf(nota("Física", "2026-05-20 10:30:00"))
        assertEquals(1, filtrarConducta(conHora, FiltroConducta(hasta = "2026-05-20")).size)
    }

    @Test fun desdePosteriorAHastaEsInvalidoYNoDevuelveNada() {
        val invertido = FiltroConducta(desde = "2026-06-01", hasta = "2026-05-01")
        assertTrue(rangoInvalido(invertido))
        assertEquals(emptyList<RasgoConductaDto>(), filtrarConducta(notas, invertido))
        assertFalse(rangoInvalido(FiltroConducta(desde = "2026-05-01", hasta = "2026-05-01")))
    }

    @Test fun materiasDeConductaSinRepetirNiVaciasOrdenadas() {
        assertEquals(listOf("Física", "Matemática"), materiasDeConducta(notas))
    }

    @Test fun ordenaConCriterioEspanolNoPorPuntoDeCodigo() {
        // por código Unicode "Álgebra" (Á = U+00C1) iría después de "Zoología"
        assertEquals(listOf("Álgebra", "Biología", "Zoología"), materiasDeConducta(listOf(nota("Zoología", null), nota("Álgebra", null), nota("Biología", null))))
    }

    @Test fun detectaSiHayAlgunFiltroActivo() {
        assertFalse(hayFiltroConducta(FiltroConducta()))
        assertTrue(hayFiltroConducta(FiltroConducta(hasta = "2026-01-01")))
    }

    // ---- tareas por mes ----

    @Test fun mesDeTareaYEtiquetaMes() {
        assertEquals("2026-03", mesDeTarea("2026-03-10"))
        assertEquals("2026-03", mesDeTarea("2026-03-10 09:30:00"))
        assertEquals("Marzo 2026", etiquetaMes("2026-03"))
        assertEquals("Diciembre 2025", etiquetaMes("2025-12"))
        assertEquals(SIN_FECHA, mesDeTarea(null))
        assertEquals(SIN_FECHA, mesDeTarea(""))
        assertEquals(SIN_FECHA, mesDeTarea("2026-13-01"))
        assertEquals("Sin fecha", etiquetaMes(SIN_FECHA))
    }

    @Test fun ordenarTareasEsCronologicoConLasSinFechaAlFinalYNumeraPorPosicion() {
        val ordenadas = ordenarTareas(tareas)
        assertEquals(listOf(1, 3, 4, 2, 5), ordenadas.map { it.tarea.id })
        assertEquals(listOf(1, 2, 3, 4, 5), ordenadas.map { it.numero })
    }

    @Test fun ordenarTareasNoModificaLaListaOriginal() {
        val original = tareas.map { it.id }
        ordenarTareas(tareas)
        assertEquals(original, tareas.map { it.id })
    }

    @Test fun mesesConTareasSoloLosQueTienenCronologicosYSinFechaAlFinal() {
        assertEquals(
            listOf(
                OpcionMes("2026-03", "Marzo 2026"), OpcionMes("2026-05", "Mayo 2026"),
                OpcionMes("2026-08", "Agosto 2026"), OpcionMes(SIN_FECHA, "Sin fecha"),
            ),
            mesesConTareas(tareas),
        )
        assertEquals(listOf("Marzo 2025", "Marzo 2026"), mesesConTareas(listOf(tarea(1, "2025-03-10"), tarea(2, "2026-03-10"))).map { it.label })
    }

    @Test fun agruparConTodosAgrupaPorMesEnOrdenCronologico() {
        val grupos = agruparTareasPorMes(tareas)
        assertEquals(listOf("Marzo 2026", "Mayo 2026", "Agosto 2026", "Sin fecha"), grupos.map { it.label })
        assertEquals(listOf(1, 3), grupos[0].tareas.map { it.tarea.id })
    }

    @Test fun agruparConUnMesPuntualConservaElNumeroDeCadaTarea() {
        val grupos = agruparTareasPorMes(tareas, "2026-05")
        assertEquals(1, grupos.size)
        assertEquals(listOf(4 to 3), grupos[0].tareas.map { it.tarea.id to it.numero })
    }

    @Test fun unMesSinTareasOUnaListaVaciaDevuelveVacio() {
        assertEquals(emptyList<GrupoDeTareas>(), agruparTareasPorMes(tareas, "2026-11"))
        assertEquals(emptyList<GrupoDeTareas>(), agruparTareasPorMes(emptyList()))
    }
}
