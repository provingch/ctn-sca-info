package py.edu.ctn.sca.padres.ui.parent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import py.edu.ctn.sca.padres.data.ParentResponse
import py.edu.ctn.sca.padres.data.RasgoConductaDto
import py.edu.ctn.sca.padres.data.SubjectDto

class ParentUiStateTest {
    private fun materia(id: Int, nombre: String, etapa: String = "primera") =
        SubjectDto(planillaId = id, materiaId = id, materia = nombre, etapa = etapa)

    private fun estado(materias: List<SubjectDto>, busqueda: String = "", stage: Stage = Stage.PRIMERA) =
        ParentUiState(data = ParentResponse(materias = materias), stage = stage, materiaSearch = busqueda)

    @Test fun elBuscadorApareceConSeisOMasMateriasDeLaEtapa() {
        val cinco = estado((1..5).map { materia(it, "M$it") })
        val seis = estado((1..6).map { materia(it, "M$it") })
        assertFalse(cinco.mostrarBuscadorMateria)
        assertTrue(seis.mostrarBuscadorMateria)
    }

    @Test fun elUmbralCuentaSoloLasMateriasDeLaEtapaSeleccionada() {
        val materias = (1..4).map { materia(it, "P$it", "primera") } + (5..9).map { materia(it, "S$it", "segunda") }
        assertFalse(estado(materias, stage = Stage.PRIMERA).mostrarBuscadorMateria)
        assertFalse(estado(materias, stage = Stage.SEGUNDA).mostrarBuscadorMateria)
    }

    @Test fun laBusquedaFiltraLaGrillaPeroNoElTotalDeLaEtapa() {
        val e = estado(listOf(materia(1, "Matemática"), materia(2, "Física"), materia(3, "Química")), busqueda = "matematica")
        assertEquals(listOf("Matemática"), e.subjectsVisible.map { it.materia })
        assertEquals(3, e.subjectsForStage.size)
    }

    @Test fun conductaVisibleAplicaElFiltroYLasMateriasSalenDeLasNotas() {
        val notas = listOf(
            RasgoConductaDto(fechaClase = "2026-03-10", materia = "Matemática", codigo = "N1"),
            RasgoConductaDto(fechaClase = "2026-04-15", materia = "Física", codigo = "N2"),
        )
        val e = ParentUiState(conducta = notas, conductaFiltro = FiltroConducta(materia = "Física"))
        assertEquals(listOf("N2"), e.conductaVisible.map { it.codigo })
        assertEquals(listOf("Física", "Matemática"), e.conductaMateriasDisponibles)
    }
}
