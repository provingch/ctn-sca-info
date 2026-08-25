package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class PlanCurricularParserTest {

    @Test
    public void etapaUnoConFormatoDePlantilla_deberiaParsearse() {
        PlanCurricularParser.EtapaAnio result = PlanCurricularParser.parseEtapaAnio(
                "PLAN DE DESARROLLO CURRICULAR ETAPA:_1°_2026");

        assertEquals("1", result.etapa());
        assertEquals(2026, result.anio());
    }

    @Test
    public void etapaDosConFormatoDePlantilla_deberiaParsearse() {
        PlanCurricularParser.EtapaAnio result = PlanCurricularParser.parseEtapaAnio(
                "PLAN DE DESARROLLO CURRICULAR ETAPA:_2°_2026");

        assertEquals("2", result.etapa());
        assertEquals(2026, result.anio());
    }

    @Test
    public void etapaSinSimboloGradoYConEspacios_deberiaParsearse() {
        PlanCurricularParser.EtapaAnio result = PlanCurricularParser.parseEtapaAnio(
                " PLAN DE DESARROLLO CURRICULAR ETAPA:  _ 1 _ 2026 ");

        assertEquals("1", result.etapa());
        assertEquals(2026, result.anio());
    }

    @Test
    public void formatoInesperado_deberiaLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> PlanCurricularParser.parseEtapaAnio("PLAN DE DESARROLLO CURRICULAR"));
    }
}