package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NombreUtilTest {

    @Test
    void completo_ponePrimeroElNombreYDespuesElApellido() {
        assertEquals("Graciela Noemí López Molinas", NombreUtil.completo("Graciela Noemí", "López Molinas"));
    }

    @Test
    void completo_toleraNullsYBlancos() {
        assertEquals("Graciela", NombreUtil.completo("Graciela", null));
        assertEquals("López", NombreUtil.completo(null, "López"));
        assertEquals("López", NombreUtil.completo("   ", " López "));
        assertEquals("Graciela", NombreUtil.completo(" Graciela ", "  "));
        assertEquals("", NombreUtil.completo(null, null));
    }

    @Test
    void corto_tomaLaPrimeraPalabraDeCadaCampo() {
        assertEquals("Graciela López", NombreUtil.corto("Graciela Noemí", "López Molinas"));
        assertEquals("Ana Gómez", NombreUtil.corto("Ana", "Gómez"));
    }

    @Test
    void corto_toleraNullsYBlancos() {
        assertEquals("Graciela", NombreUtil.corto("Graciela Noemí", null));
        assertEquals("López", NombreUtil.corto("  ", "López Molinas"));
        assertEquals("Graciela López", NombreUtil.corto("  Graciela   Noemí ", "  López  Molinas"));
        assertEquals("", NombreUtil.corto(null, ""));
    }
}
