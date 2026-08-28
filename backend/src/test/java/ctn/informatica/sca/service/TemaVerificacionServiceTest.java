package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TemaVerificacionServiceTest {

    private static final class FakeTemaVerificacionService extends TemaVerificacionService {
        private int mes;
        private int etapa;
        private Integer planId;
        private TemaVerificacionService.TemaPendiente temaPendiente;

        @Override
        protected int mesActual() {
            return mes;
        }

        @Override
        protected int etapaActual() {
            return etapa;
        }

        @Override
        protected Integer buscarPlanCurricularId(int asignacionId) {
            return planId;
        }

        @Override
        protected TemaVerificacionService.TemaPendiente buscarTemaPendiente(int planId) {
            return temaPendiente;
        }
    }

    @Test
    void ordenEsperadoActual_enEtapaUno_abrilDebeSerDos() {
        FakeTemaVerificacionService service = new FakeTemaVerificacionService();
        service.mes = 4;
        service.etapa = 1;

        assertEquals(2, service.ordenEsperadoActual());
    }

    @Test
    void ordenEsperadoActual_enEtapaDos_septiembreDebeSerTres() {
        FakeTemaVerificacionService service = new FakeTemaVerificacionService();
        service.mes = 9;
        service.etapa = 2;

        assertEquals(3, service.ordenEsperadoActual());
    }

    @Test
    void normalizaYComparaTemasSinDependerDeTildesEspaciosNiMayusculas() {
        assertEquals("unidad 1 sistemas", TemaVerificacionService.normalizarTema("Unidád 1    Sistemas"));
        assertTrue(TemaVerificacionService.coincidenTemas("Unidád 1 Sistemas", "unidad 1 sistemas"));
        assertFalse(TemaVerificacionService.coincidenTemas("Unidad 2 sistemas", "unidad 1 sistemas"));
    }

    @Test
    void verificar_cuandoCoincidePeroEstaFueraDeFecha_deberiaMarcarAtrasoYSeguirSiendoOk() throws Exception {
        FakeTemaVerificacionService service = new FakeTemaVerificacionService();
        service.planId = 11;
        service.mes = 4;
        service.etapa = 1;
        service.temaPendiente = new TemaVerificacionService.TemaPendiente(7, "Unidad 1 Sistemas", 1);

        VerificacionResultado resultado = service.verificar(99, "Unidad 1 Sistemas");

        assertEquals("OK", resultado.estado());
        assertEquals(Integer.valueOf(7), resultado.temaPlanCurricularId());
        assertTrue(resultado.atrasado());
        assertTrue(service.estaAtrasado(99, "Unidad 1 Sistemas"));
    }

    @Test
    void verificar_cuandoCoincideYEstaAlDia_noDebeMarcarAtraso() throws Exception {
        FakeTemaVerificacionService service = new FakeTemaVerificacionService();
        service.planId = 11;
        service.mes = 4;
        service.etapa = 1;
        service.temaPendiente = new TemaVerificacionService.TemaPendiente(8, "Unidad 2 Sistemas", 2);

        VerificacionResultado resultado = service.verificar(99, "Unidad 2 Sistemas");

        assertEquals("OK", resultado.estado());
        assertEquals(Integer.valueOf(8), resultado.temaPlanCurricularId());
        assertFalse(resultado.atrasado());
        assertFalse(service.estaAtrasado(99, "Unidad 2 Sistemas"));
    }

    @Test
    void verificar_cuandoNoCoincideYNoEstaAtrasado_deberiaSerDudoso() throws Exception {
        FakeTemaVerificacionService service = new FakeTemaVerificacionService();
        service.planId = 11;
        service.mes = 4;
        service.etapa = 1;
        service.temaPendiente = new TemaVerificacionService.TemaPendiente(9, "Unidad 2 Sistemas", 2);

        VerificacionResultado resultado = service.verificar(99, "Unidad 3 Sistemas");

        assertEquals("DUDOSO", resultado.estado());
        assertEquals(Integer.valueOf(9), resultado.temaPlanCurricularId());
        assertFalse(resultado.atrasado());
        assertFalse(service.estaAtrasado(99, "Unidad 3 Sistemas"));
    }

    @Test
    void verificar_cuandoNoCoincideYEstaAtrasado_deberiaSerAtrasado() throws Exception {
        FakeTemaVerificacionService service = new FakeTemaVerificacionService();
        service.planId = 11;
        service.mes = 9;
        service.etapa = 2;
        service.temaPendiente = new TemaVerificacionService.TemaPendiente(10, "Unidad 2 Sistemas", 2);

        VerificacionResultado resultado = service.verificar(99, "Unidad 3 Sistemas");

        assertEquals("ATRASADO", resultado.estado());
        assertEquals(Integer.valueOf(10), resultado.temaPlanCurricularId());
        assertTrue(resultado.atrasado());
        assertTrue(service.estaAtrasado(99, "Unidad 3 Sistemas"));
    }
}
