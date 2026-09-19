package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ctn.informatica.sca.controller.PlanillaController.RsaInput;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PlanillaControllerRsaValidationTest {

    private static void assertBadRequest(RsaInput input) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> PlanillaController.validateRsaInput(input));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void puntosNull_desactivaRsaYLimpiaLosTresCampos() {
        RsaInput out = PlanillaController.validateRsaInput(new RsaInput(null, new BigDecimal("5"), "CANTIDAD"));
        assertNull(out.puntos());
        assertNull(out.toleranciaValor());
        assertNull(out.toleranciaUnidad());
    }

    @Test
    void configuracionValida_seNormalizaAlDecimalDeLaColumna() {
        RsaInput out = PlanillaController.validateRsaInput(new RsaInput(10, new BigDecimal("12.5"), "PORCENTAJE"));
        assertEquals(10, out.puntos());
        assertEquals(new BigDecimal("12.50"), out.toleranciaValor());
        assertEquals("PORCENTAJE", out.toleranciaUnidad());
        assertEquals(new BigDecimal("0.00"),
                PlanillaController.validateRsaInput(new RsaInput(1, BigDecimal.ZERO, "CANTIDAD")).toleranciaValor());
    }

    @Test
    void rechazaPuntosNoPositivos() {
        assertBadRequest(new RsaInput(0, BigDecimal.ONE, "CANTIDAD"));
        assertBadRequest(new RsaInput(-3, BigDecimal.ONE, "CANTIDAD"));
    }

    @Test
    void rechazaUnidadInvalidaOFaltante() {
        assertBadRequest(new RsaInput(10, BigDecimal.ONE, null));
        assertBadRequest(new RsaInput(10, BigDecimal.ONE, "porcentaje"));
        assertBadRequest(new RsaInput(10, BigDecimal.ONE, "DIAS"));
    }

    @Test
    void rechazaToleranciaNulaONegativa() {
        assertBadRequest(new RsaInput(10, null, "CANTIDAD"));
        assertBadRequest(new RsaInput(10, new BigDecimal("-1"), "CANTIDAD"));
    }

    @Test
    void rechazaValoresSinSentidoParaLaUnidad() {
        assertBadRequest(new RsaInput(10, new BigDecimal("2.5"), "CANTIDAD"));
        assertBadRequest(new RsaInput(10, new BigDecimal("100.01"), "PORCENTAJE"));
        assertBadRequest(new RsaInput(10, new BigDecimal("10000"), "CANTIDAD"));
        assertEquals(new BigDecimal("2.00"),
                PlanillaController.validateRsaInput(new RsaInput(10, new BigDecimal("2.00"), "CANTIDAD")).toleranciaValor());
    }

    @Test
    void rechazaBodyNulo() {
        assertBadRequest(null);
    }
}
