package py.edu.ctn.sca.padres.ui.padre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import py.edu.ctn.sca.padres.data.RasgoConductaDto
import py.edu.ctn.sca.padres.data.padre.AnotacionDto
import java.time.LocalDate

class ConductaLogicTest {
    private val hoy = LocalDate.of(2026, 10, 21) // miércoles

    private fun codigo(fecha: String?, cod: String = "N3") = RasgoConductaDto(fechaClase = fecha, codigo = cod, materia = "Matemática")
    private fun anot(id: Int, fecha: String?, tipo: String = "Uniforme") = AnotacionDto(id = id, fechaHora = fecha, tipo = tipo)

    @Test fun mezclaOrdenaMasNuevoPrimeroYSinFechaAlFinal() {
        val items = mezclarConducta(
            listOf(codigo("2026-10-14"), codigo(null, "N1")),
            listOf(anot(1, "2026-10-21 07:10:00"), anot(2, "2026-09-30 07:00:00")),
        )
        assertEquals(
            listOf("A1", "N3", "A2", "N1"),
            items.map { if (it is ConductaItem.Anotacion) "A${it.dto.id}" else (it as ConductaItem.Codigo).dto.codigo },
        )
    }

    @Test fun seccionaEstaSemanaYAntes() {
        val items = mezclarConducta(listOf(codigo("2026-10-14"), codigo("2026-10-07"), codigo(null)), listOf(anot(1, "2026-10-21 07:10:00")))
        val s = seccionarConducta(items, hoy)
        assertEquals(listOf(ConductaSeccion.ESTA_SEMANA, ConductaSeccion.ANTES), s.map { it.first })
        assertEquals(2, s[0].second.size) // anotación de hoy + código del 14/10 (7 días)
        assertEquals(2, s[1].second.size) // 07/10 + sin fecha
    }

    @Test fun omiteSeccionesVacias() {
        val s = seccionarConducta(mezclarConducta(listOf(codigo("2026-01-01")), emptyList()), hoy)
        assertEquals(listOf(ConductaSeccion.ANTES), s.map { it.first })
    }

    @Test fun conteosYFiltros() {
        val items = mezclarConducta(listOf(codigo("2026-10-14"), codigo("2026-10-07")), listOf(anot(1, "2026-10-21 07:10:00")))
        assertEquals(ConductaConteo(3, 2, 1), contarConducta(items))
        assertEquals(3, filtrarConductaItems(items, ConductaFiltro.TODO).size)
        assertEquals(2, filtrarConductaItems(items, ConductaFiltro.CODIGOS).size)
        assertEquals(1, filtrarConductaItems(items, ConductaFiltro.ANOTACIONES).size)
    }

    @Test fun cuandoFormateaHoyAyerSemanaYAntes() {
        fun a(f: String) = anotacionItem(anot(1, f))
        assertEquals("Hoy 07:10", cuandoConducta(a("2026-10-21 07:10:00"), hoy))
        assertEquals("Ayer 07:15", cuandoConducta(a("2026-10-20 07:15:00"), hoy))
        assertEquals("Lun 19/10", cuandoConducta(a("2026-10-19 08:00:00"), hoy))
        assertEquals("30/09", cuandoConducta(a("2026-09-30 07:00:00"), hoy))
        assertEquals("Lun 19/10", cuandoConducta(codigoItem(codigo("2026-10-19")), hoy)) // código sin hora
        assertEquals("Sin fecha", cuandoConducta(codigoItem(codigo(null)), hoy))
    }

    @Test fun origenYFechaCompleta() {
        assertEquals("Preceptoría", origenAnotacion(anot(1, null)))
        assertEquals("Matemática · Ortiz, Camila", origenAnotacion(AnotacionDto(materia = "Matemática", profesor = "Ortiz, Camila")))
        assertEquals("21/10/26 07:10 · Preceptoría", fechaCompleta(anot(1, "2026-10-21 07:10:00")))
    }

    @Test fun parseFechaTolerante() {
        assertNull(parseFecha("basura"))
        assertNull(parseFecha(null))
        assertEquals(false, parseFecha("2026-10-14")!!.second)
        assertEquals(true, parseFecha("2026-10-14T07:10:00")!!.second)
    }

    @Test fun cursoLegible() {
        assertEquals("Informática 1° A", cursoLegible("Informática · 1° A"))
        assertNull(cursoLegible(null))
    }
}
