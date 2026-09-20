package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.google.ClassroomSyncOrchestrator;
import ctn.informatica.sca.model.Planilla;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

/** Cargar la fecha de cierre sólo la programa: cerrar (confirmar) la etapa exige que esa fecha ya haya llegado. */
class PlanillaControllerConfirmarEtapaTest {

    private static final int PLANILLA_ID = 10;
    private static final int PROFESOR = 7;
    private static final ZoneId PARAGUAY = ZoneId.of("America/Asuncion");

    private PlanillaDao planillaDao;
    private PlanillaController controller;
    private Planilla planilla;
    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken((long) PROFESOR, null, List.of());

    @BeforeEach
    void setUp() throws Exception {
        planillaDao = mock(PlanillaDao.class);
        planilla = new Planilla();
        planilla.setProfesorId(PROFESOR);
        when(planillaDao.findById(PLANILLA_ID)).thenReturn(planilla);
        when(planillaDao.updateEtapa1Confirmada(PLANILLA_ID, true)).thenReturn(true);
        when(planillaDao.updateEtapa2Confirmada(PLANILLA_ID, true)).thenReturn(true);
        controller = new PlanillaController(planillaDao, mock(ProfesorDao.class), mock(ClassroomSyncOrchestrator.class));
    }

    private static LocalDate hoy() {
        return LocalDate.now(PARAGUAY);
    }

    private void assertBadRequest(Runnable accion, String fragmento) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains(fragmento), ex.getReason());
    }

    // ---- Etapa 1 --------------------------------------------------------------------------------------

    @Test
    void etapa1_conFechaDeCierreFutura_responde400YNoCierra() throws Exception {
        planilla.setFechaCierreEtapa1(hoy().plusDays(10));

        assertBadRequest(() -> controller.confirmarEtapa1(PLANILLA_ID, authentication), "Todavía no llegó la fecha de cierre de Etapa 1 (" + hoy().plusDays(10) + ")");

        verify(planillaDao, never()).updateEtapa1Confirmada(anyInt(), anyBoolean());
    }

    @Test
    void etapa1_conFechaDeCierreDeMañana_todaviaNoSePuedeCerrar() throws Exception {
        planilla.setFechaCierreEtapa1(hoy().plusDays(1));

        assertBadRequest(() -> controller.confirmarEtapa1(PLANILLA_ID, authentication), "Todavía no llegó");

        verify(planillaDao, never()).updateEtapa1Confirmada(anyInt(), anyBoolean());
    }

    @Test
    void etapa1_conFechaDeCierreDeHoy_cierraNormal() throws Exception {
        planilla.setFechaCierreEtapa1(hoy());

        controller.confirmarEtapa1(PLANILLA_ID, authentication);

        verify(planillaDao).updateEtapa1Confirmada(PLANILLA_ID, true);
        assertTrue(planilla.getEtapa1Confirmada());
    }

    @Test
    void etapa1_conFechaDeCierrePasada_cierraNormal() throws Exception {
        planilla.setFechaCierreEtapa1(hoy().minusDays(30));

        controller.confirmarEtapa1(PLANILLA_ID, authentication);

        verify(planillaDao).updateEtapa1Confirmada(PLANILLA_ID, true);
    }

    @Test
    void etapa1_sinFechaDeCierre_sigueRespondiendo400() throws Exception {
        assertBadRequest(() -> controller.confirmarEtapa1(PLANILLA_ID, authentication), "Debe indicar la fecha de cierre de Etapa 1");

        verify(planillaDao, never()).updateEtapa1Confirmada(anyInt(), anyBoolean());
    }

    @Test
    void etapa1_yaCerrada_sigueRespondiendo409() throws Exception {
        planilla.setFechaCierreEtapa1(hoy().minusDays(1));
        planilla.setEtapa1Confirmada(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.confirmarEtapa1(PLANILLA_ID, authentication));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---- Etapa 2 --------------------------------------------------------------------------------------

    @Test
    void etapa2_conFechaDeCierreFutura_responde400YNoCierra() throws Exception {
        planilla.setFechaCierreEtapa2(hoy().plusDays(10));

        assertBadRequest(() -> controller.confirmarEtapa2(PLANILLA_ID, authentication), "Todavía no llegó la fecha de cierre de Etapa 2 (" + hoy().plusDays(10) + ")");

        verify(planillaDao, never()).updateEtapa2Confirmada(anyInt(), anyBoolean());
    }

    @Test
    void etapa2_conFechaDeCierreDeMañana_todaviaNoSePuedeCerrar() throws Exception {
        planilla.setFechaCierreEtapa2(hoy().plusDays(1));

        assertBadRequest(() -> controller.confirmarEtapa2(PLANILLA_ID, authentication), "Todavía no llegó");

        verify(planillaDao, never()).updateEtapa2Confirmada(anyInt(), anyBoolean());
    }

    @Test
    void etapa2_conFechaDeCierreDeHoy_cierraNormal() throws Exception {
        planilla.setFechaCierreEtapa2(hoy());

        controller.confirmarEtapa2(PLANILLA_ID, authentication);

        verify(planillaDao).updateEtapa2Confirmada(PLANILLA_ID, true);
        assertTrue(planilla.getEtapa2Confirmada());
    }

    @Test
    void etapa2_conFechaDeCierrePasada_cierraNormal() throws Exception {
        planilla.setFechaCierreEtapa2(hoy().minusDays(30));

        controller.confirmarEtapa2(PLANILLA_ID, authentication);

        verify(planillaDao).updateEtapa2Confirmada(PLANILLA_ID, true);
    }

    @Test
    void etapa2_sinFechaDeCierre_sigueRespondiendo400() throws Exception {
        assertBadRequest(() -> controller.confirmarEtapa2(PLANILLA_ID, authentication), "Debe indicar la fecha de cierre de Etapa 2");

        verify(planillaDao, never()).updateEtapa2Confirmada(anyInt(), anyBoolean());
    }

    @Test
    void etapa2_yaCerrada_sigueRespondiendo409() throws Exception {
        planilla.setFechaCierreEtapa2(hoy().minusDays(1));
        planilla.setEtapa2Confirmada(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.confirmarEtapa2(PLANILLA_ID, authentication));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
