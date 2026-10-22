package py.edu.ctn.sca.padres.data.coordinacion

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CoordinacionLogicTest {
    private fun q(id: Long, tipo: String = "CONTRA_PROFESOR", prof: Long = 1, curso: Long = 1, estado: String? = null, rechazadaEn: String? = null) =
        QuejaDto(id = id, tipo = tipo, profesorId = prof, cursoId = curso, estado = estado, rechazadaEn = rechazadaEn?.let { JsonPrimitive(it) })

    @Test fun estadoRechazadaGanaSobreAceptada() {
        assertEquals(QuejaEstado.RECHAZADA, q(1, estado = "aceptada", rechazadaEn = "123").estadoEfectivo())
        assertEquals(QuejaEstado.PENDIENTE, q(2).estadoEfectivo())
        assertEquals(QuejaEstado.ACEPTADA, q(3, estado = "aceptada").estadoEfectivo())
    }

    @Test fun filtrosDeQuejas() {
        val l = listOf(q(1), q(2, tipo = "CONTRA_CURSO"), q(3, estado = "aceptada"))
        assertEquals(listOf(1L, 2L), filtrarQuejas(l, QuejaFiltro.PENDIENTES).map { it.id })
        assertEquals(listOf(1L, 3L), filtrarQuejas(l, QuejaFiltro.CONTRA_PROFESORES).map { it.id })
        assertEquals(listOf(2L), filtrarQuejas(l, QuejaFiltro.SOBRE_CURSOS).map { it.id })
    }

    @Test fun cantidadPorProfesorOCurso() {
        val l = listOf(q(1, prof = 7), q(2, prof = 7, estado = "aceptada"), q(3, prof = 8), q(4, tipo = "CONTRA_CURSO", prof = 7, curso = 5))
        assertEquals(2, cantidadPorObjetivo(l, l[0]))
        assertEquals(1, cantidadPorObjetivo(l, l[3]))
    }

    @Test fun nombreCortoPrimerasPalabras() {
        assertEquals("Ana Gómez", nombreCorto("Ana María", "Gómez Paz"))
        assertEquals("Profesor #9", q(1, prof = 9).objetivo())
    }

    private fun a(id: Int, fecha: String?, tipo: String = "Uniforme", foto: Boolean = false, alumno: String = "Lucas", curso: String = "Informática 1° A", prof: String = "Diego") =
        AnotacionDto(id = id, fechaHora = fecha, tipo = tipo, tieneFoto = foto, alumnoNombre = alumno, alumnoApellido = "Acosta", curso = curso, profesor = prof)

    private val hoy = LocalDate.of(2026, 10, 22)

    @Test fun ventanaDeSieteDias() {
        val l = listOf(a(1, "2026-10-22 07:10:00"), a(2, "2026-10-15 00:00:00"), a(3, "2026-10-14 23:59:59"), a(4, null))
        assertEquals(listOf(1, 2), ultimosDias(l, hoy).map { it.id })
    }

    @Test fun busquedaYChips() {
        val l = listOf(a(1, "x", foto = true), a(2, "x", tipo = "Conducta", alumno = "Sofía", prof = "Marta"), a(3, "x", curso = "Mecánica 1° A"))
        assertEquals(listOf(1, 2, 3), filtrarAnotaciones(l, "", AnotChip.TODAS).map { it.id })
        assertEquals(listOf(1, 3), filtrarAnotaciones(l, "", AnotChip.UNIFORME).map { it.id })
        assertEquals(listOf(2), filtrarAnotaciones(l, "", AnotChip.CONDUCTA).map { it.id })
        assertEquals(listOf(1), filtrarAnotaciones(l, "", AnotChip.CON_FOTO).map { it.id })
        assertEquals(listOf(2), filtrarAnotaciones(l, " MARTA ", AnotChip.TODAS).map { it.id })
        assertEquals(listOf(3), filtrarAnotaciones(l, "mecánica", AnotChip.UNIFORME).map { it.id })
        assertEquals(2, AnotChip.UNIFORME.cuenta(l))
    }

    @Test fun cuandoAnotacionFormatos() {
        assertEquals("hoy 07:10", cuandoAnotacion(a(1, "2026-10-22 07:10:00").fecha(), hoy))
        assertEquals("ayer", cuandoAnotacion(a(1, "2026-10-21 10:00:00").fecha(), hoy))
        assertEquals("19/10", cuandoAnotacion(a(1, "2026-10-19 10:00:00").fecha(), hoy))
    }
}
