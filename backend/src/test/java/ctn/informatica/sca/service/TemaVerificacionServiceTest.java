package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TemaVerificacionServiceTest {

    @Test
    void normalizaYComparaTemasSinDependerDeTildesEspaciosNiMayusculas() {
        assertEquals("unidad 1 sistemas", TemaVerificacionService.normalizarTema("Unidád 1    Sistemas"));
        assertTrue(TemaVerificacionService.coincidenTemas("Unidád 1 Sistemas", "unidad 1 sistemas"));
        assertFalse(TemaVerificacionService.coincidenTemas("Unidad 2 sistemas", "unidad 1 sistemas"));
    }
}
