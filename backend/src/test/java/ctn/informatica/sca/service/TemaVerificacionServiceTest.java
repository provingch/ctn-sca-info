package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemaVerificacionServiceTest {

    private static final class FakeTemaVerificacionService extends TemaVerificacionService {
        private int mes;
        private int etapa;
        private Integer planId;
        private TemaVerificacionService.TemaPendiente temaPendiente;
        /** Plan aprobado de la etapa 1 (lo que ve el servicio al mirar la etapa anterior); null = no hay. */
        private Integer planEtapaAnterior;
        private List<TemaVerificacionService.TemaPendiente> pendientesEtapaAnterior = List.of();
        private final List<Integer> etapasConsultadas = new ArrayList<>();

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
        protected Integer buscarPlanCurricularId(int asignacionId, int etapa, int anio) {
            etapasConsultadas.add(etapa);
            return planEtapaAnterior;
        }

        @Override
        protected TemaVerificacionService.TemaPendiente buscarTemaPendiente(int planId) {
            return temaPendiente;
        }

        @Override
        protected TemaVerificacionService.TemaPendiente buscarPrimerTemaPendienteQueCoincida(int planId, String temaIngresado) {
            return pendientesEtapaAnterior.stream()
                    .filter(pendiente -> TemaVerificacionService.coincidenTemas(temaIngresado, pendiente.temasContenidos()))
                    .findFirst().orElse(null);
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

    // ---- Retomar un tema que quedó sin cubrir en la etapa anterior --------------------------------------

    private static FakeTemaVerificacionService enEtapaDos() {
        FakeTemaVerificacionService service = new FakeTemaVerificacionService();
        service.planId = 21;
        service.mes = 9;
        service.etapa = 2;
        // el próximo tema de la etapa 2 (orden 3) está al día con septiembre (esperado 3)
        service.temaPendiente = new TemaVerificacionService.TemaPendiente(30, "Unidad 5 Redes", 3);
        service.planEtapaAnterior = 11;
        service.pendientesEtapaAnterior = List.of(
                new TemaVerificacionService.TemaPendiente(7, "Unidad 1 Sistemas", 1),
                new TemaVerificacionService.TemaPendiente(8, "Unidad 2 Bases de datos", 2));
        return service;
    }

    @Test
    void verificar_unTemaPendienteDeLaEtapaAnterior_seReconoceComoAtrasadoYNoComoDudoso() throws Exception {
        FakeTemaVerificacionService service = enEtapaDos();

        VerificacionResultado resultado = service.verificar(99, "Unidad 2 Bases de datos");

        assertEquals("ATRASADO", resultado.estado());
        assertEquals(Integer.valueOf(8), resultado.temaPlanCurricularId());
        assertTrue(resultado.atrasado());
        assertTrue(resultado.temaDeEtapaAnterior());
        assertTrue(service.estaAtrasado(99, "Unidad 2 Bases de datos"));
        assertEquals(List.of(1, 1), service.etapasConsultadas, "sólo mira la etapa 1 (dos llamadas: verificar y estaAtrasado)");
    }

    @Test
    void verificar_elTemaDeLaEtapaAnteriorGanaAlAtrasoGenericoDeLaEtapaActual() throws Exception {
        FakeTemaVerificacionService service = enEtapaDos();
        service.temaPendiente = new TemaVerificacionService.TemaPendiente(30, "Unidad 5 Redes", 1); // ya atrasado en la etapa 2

        VerificacionResultado resultado = service.verificar(99, "Unidad 1 Sistemas");

        assertEquals(Integer.valueOf(7), resultado.temaPlanCurricularId(), "el tema retomado, no el próximo de la etapa 2");
        assertTrue(resultado.temaDeEtapaAnterior());
    }

    @Test
    void verificar_unTemaDeLaEtapaActualQueCoincideNoMiraLaAnterior() throws Exception {
        FakeTemaVerificacionService service = enEtapaDos();

        VerificacionResultado resultado = service.verificar(99, "Unidad 5 Redes");

        assertEquals("OK", resultado.estado());
        assertFalse(resultado.temaDeEtapaAnterior());
        assertTrue(service.etapasConsultadas.isEmpty());
    }

    @Test
    void verificar_siNoCoincideConNingunPendienteDeLaAnterior_seguiaSiendoDudoso() throws Exception {
        FakeTemaVerificacionService service = enEtapaDos();

        VerificacionResultado resultado = service.verificar(99, "Tema que no está en ningún plan");

        assertEquals("DUDOSO", resultado.estado());
        assertEquals(Integer.valueOf(30), resultado.temaPlanCurricularId());
        assertFalse(resultado.atrasado());
        assertFalse(resultado.temaDeEtapaAnterior());
    }

    @Test
    void verificar_siLaEtapaAnteriorNoTieneUnPlanAprobado_noHayNadaQueRetomar() throws Exception {
        FakeTemaVerificacionService service = enEtapaDos();
        service.planEtapaAnterior = null;

        assertEquals("DUDOSO", service.verificar(99, "Unidad 1 Sistemas").estado());
    }

    @Test
    void verificar_laEtapaUnoNoTieneEtapaAnterior() throws Exception {
        FakeTemaVerificacionService service = enEtapaDos();
        service.etapa = 1;
        service.mes = 4;
        service.temaPendiente = new TemaVerificacionService.TemaPendiente(30, "Unidad 5 Redes", 2);

        VerificacionResultado resultado = service.verificar(99, "Unidad 1 Sistemas");

        assertEquals("DUDOSO", resultado.estado());
        assertTrue(service.etapasConsultadas.isEmpty(), "en etapa 1 no se consulta ningún plan anterior");
    }

    @Test
    void verificar_sinPlanDeEtapaDos_igualReconoceUnTemaPendienteDeEtapaUno() throws Exception {
        FakeTemaVerificacionService service = enEtapaDos();
        service.planId = null;

        VerificacionResultado retomado = service.verificar(99, "Unidad 1 Sistemas");
        VerificacionResultado otro = service.verificar(99, "Cualquier otro tema");

        assertEquals("ATRASADO", retomado.estado());
        assertEquals(Integer.valueOf(7), retomado.temaPlanCurricularId());
        assertTrue(retomado.temaDeEtapaAnterior());
        assertEquals("SIN_PLAN", otro.estado());
    }

    @Test
    void verificar_conLaEtapaDosTodaCubierta_igualReconoceUnTemaPendienteDeEtapaUno() throws Exception {
        FakeTemaVerificacionService service = enEtapaDos();
        service.temaPendiente = null;

        assertEquals("ATRASADO", service.verificar(99, "Unidad 2 Bases de datos").estado());
        assertEquals("OK", service.verificar(99, "Otro tema").estado());
    }
}
