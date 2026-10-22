package py.edu.ctn.sca.padres.data.evaluador

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EvaluadorLogicTest {
    private fun inc(tipo: String) = IncumplimientoDto(id = 1, tipo = tipo)

    @Test fun categorias() {
        assertEquals(Categoria.ATRASOS, categoriaDe("ATRASO"))
        assertEquals(Categoria.RETROACTIVAS, categoriaDe("INCONGRUENCIA_RETROACTIVA"))
        assertEquals(Categoria.BLOQUEOS, categoriaDe("BLOQUEO_ATRASO_TIEMPO_REAL"))
        assertEquals(Categoria.BLOQUEOS, categoriaDe("BLOQUEO_INCONGRUENCIA_RETROACTIVA"))
        assertNull(categoriaDe("OTRO"))
    }

    @Test fun filtro() {
        val items = listOf(inc("ATRASO"), inc("BLOQUEO_ATRASO_TIEMPO_REAL"), inc("OTRO"))
        assertEquals(3, filtrarIncumplimientos(items, null).size)
        assertEquals(1, filtrarIncumplimientos(items, Categoria.BLOQUEOS).size)
        assertEquals(0, filtrarIncumplimientos(items, Categoria.RETROACTIVAS).size)
    }

    @Test fun meses() {
        val temas = listOf(
            TemaDto("ABRIL", 4, 1, "Limado"), TemaDto("MARZO", 3, 1, "Seguridad"),
            TemaDto("MARZO", 3, 2, "Herramientas"), TemaDto("MARZO", 3, 3, "Herramientas"),
        )
        assertEquals(listOf(MesPlan("Marzo", "Seguridad · Herramientas"), MesPlan("Abril", "Limado")), mesesDelPlan(temas))
    }

    @Test fun fechas() {
        assertEquals("20/10", fechaCorta("2026-10-20 14:30:00"))
        assertEquals("05/01", fechaCorta("2026-01-05T10:00:00"))
        assertNull(fechaCorta(null))
        assertNull(fechaCorta("x"))
    }

    @Test fun nombreProfesor() {
        assertEquals("Cáceres Valentina", IncumplimientoDto(usuarioApellido = "Cáceres", usuarioNombre = "Valentina").profesor())
        assertEquals("Profesor #7", IncumplimientoDto(usuarioId = 7).profesor())
    }
}
