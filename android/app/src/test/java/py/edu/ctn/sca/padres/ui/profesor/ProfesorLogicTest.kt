package py.edu.ctn.sca.padres.ui.profesor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import py.edu.ctn.sca.padres.data.profesor.BloqueHoyDto
import py.edu.ctn.sca.padres.data.profesor.ClaseDadaDto
import py.edu.ctn.sca.padres.data.profesor.PlanHistorialDto
import py.edu.ctn.sca.padres.data.profesor.TemaPlanDto
import java.time.LocalDate
import java.time.LocalTime

class ProfesorLogicTest {
    private fun b(ini: String, fin: String, reg: Boolean) = BloqueHoyDto(horaInicio = ini, horaFin = fin, registrada = reg, materiaNombre = "M$ini")

    @Test fun fechaLargaEnEspanol() {
        assertEquals("Miércoles 21 de octubre", fechaLarga(LocalDate.of(2026, 10, 21)))
    }

    @Test fun fechaCortaYParse() {
        assertEquals("12/06", fechaCorta("2026-06-12T10:00:00"))
        assertNull(fechaCorta(null))
    }

    @Test fun proximoSaltaRegistradasYTerminadas() {
        val l = listOf(b("07:00", "08:30", true), b("09:30", "11:00", false), b("13:00", "14:30", false))
        assertEquals(1, indiceProximo(l, LocalTime.of(9, 45)))
        assertEquals(2, indiceProximo(l, LocalTime.of(12, 0)))
        assertEquals(1, indiceProximo(l, LocalTime.of(20, 0))) // todo terminado: primera sin registrar
    }

    @Test fun proximoNuloSiTodoRegistrado() {
        assertNull(indiceProximo(listOf(b("07:00", "08:00", true)), LocalTime.NOON))
    }

    @Test fun resumenCuentaPresentesYConductas() {
        val r = resumenAsistencia(8, setOf(2), mapOf(3 to setOf("N3"), 4 to emptySet()))
        assertEquals(ResumenAsistencia(7, 1, 1), r)
    }

    @Test fun planesRechazadosSoloElUltimo() {
        val l = listOf(
            PlanHistorialDto(id = 1, estado = "RECHAZADO", etapa = "1", anio = 2026, asignacionId = 5),
            PlanHistorialDto(id = 2, estado = "APROBADO", etapa = "1", anio = 2026, asignacionId = 5),
            PlanHistorialDto(id = 3, estado = "RECHAZADO", etapa = "1", anio = 2026, asignacionId = 6),
        )
        assertEquals(listOf(3), planesRechazados(l).map { it.id })
    }

    @Test fun temasSugeridosPrefierePendientes() {
        val t = listOf(
            TemaPlanDto(ordenMes = 2, bloque = 1, temasContenidos = "B", estadoCobertura = "PENDIENTE"),
            TemaPlanDto(ordenMes = 1, bloque = 1, temasContenidos = "A", estadoCobertura = "CUBIERTO"),
            TemaPlanDto(ordenMes = 1, bloque = 2, temasContenidos = "C", estadoCobertura = "PENDIENTE"),
        )
        assertEquals(listOf("C", "B"), temasSugeridos(t))
    }

    @Test fun clasesAgrupadasDescendente() {
        val c = listOf(
            ClaseDadaDto(id = 1, fechaClase = "2026-10-20"),
            ClaseDadaDto(id = 2, fechaClase = "2026-10-21"),
            ClaseDadaDto(id = 3, fechaClase = "2026-10-21"),
        )
        val g = clasesPorFecha(c)
        assertEquals(LocalDate.of(2026, 10, 21), g[0].first)
        assertEquals(listOf(3, 2), g[0].second.map { it.id })
    }

    @Test fun puntajeSeLimita() {
        assertNull(parsePuntaje("", 10))
        assertEquals(7, parsePuntaje("7", 10))
        assertEquals(10, parsePuntaje("15", 10))
        assertEquals(0, parsePuntaje("0", 10))
    }

    @Test fun siguienteSinNotaDaLaVuelta() {
        val v = listOf(9, null, 7, null)
        assertEquals(1, siguienteSinNota(v))
        assertEquals(3, siguienteSinNota(v, 1))
        assertEquals(1, siguienteSinNota(v, 3))
        assertNull(siguienteSinNota(listOf(1, 2)))
    }
}
