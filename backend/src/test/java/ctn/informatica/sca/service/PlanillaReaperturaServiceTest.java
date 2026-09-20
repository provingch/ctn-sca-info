package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PlanillaReaperturaServiceTest {

    private PlanillaDao planillaDao;
    private ActivityLogService activityLogService;
    private NotificacionDao notificacionDao;
    private PlanillaReaperturaService service;

    @BeforeEach
    void setUp() throws Exception {
        planillaDao = mock(PlanillaDao.class);
        activityLogService = mock(ActivityLogService.class);
        notificacionDao = mock(NotificacionDao.class);
        UserDao userDao = mock(UserDao.class);
        Planilla planilla = new Planilla();
        planilla.setProfesorId(7);
        planilla.setEtapa1Confirmada(true);
        when(planillaDao.findById(10)).thenReturn(planilla);
        when(planillaDao.updateEtapa1Confirmada(10, false)).thenReturn(true);
        when(userDao.findById(7)).thenReturn(new User(7, "profe", "Profesor Uno", 1));
        service = new PlanillaReaperturaService(planillaDao, notificacionDao, userDao, activityLogService);
    }

    private static void assertBadRequest(String motivo) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> PlanillaReaperturaService.normalizarMotivo(motivo));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void elMotivoVacioONuloSeRechaza() {
        assertBadRequest(null);
        assertBadRequest("");
        assertBadRequest(" \t\n ");
    }

    @Test
    void elMotivoSeLimpiaYSeColapsaElEspacioEnBlanco() {
        assertEquals("Cierre por error de fecha", PlanillaReaperturaService.normalizarMotivo("  Cierre   por\nerror\tde fecha  "));
    }

    @Test
    void elMotivoLargoSeRechaza() {
        assertEquals("x".repeat(500), PlanillaReaperturaService.normalizarMotivo("x".repeat(500)));
        assertBadRequest("x".repeat(501));
    }

    @Test
    void unMotivoConSaltosDeLineaNoPuedeFabricarEntradasFalsasEnElLog() throws Exception {
        service.reabrirEtapa(10, 1, 3, "ok\n[2026-01-01 00:00:00] Eliminó a un usuario");

        ArgumentCaptor<String> line = ArgumentCaptor.forClass(String.class);
        verify(activityLogService).registrar(org.mockito.ArgumentMatchers.eq(3), line.capture());
        assertEquals("Reabrió Etapa 1 de la planilla 10 — motivo: ok [2026-01-01 00:00:00] Eliminó a un usuario", line.getValue());
    }

    @Test
    void sinMotivoValidoNoSeEscribeNadaNiSeNotifica() throws Exception {
        assertThrows(ResponseStatusException.class, () -> service.reabrirEtapa(10, 1, 3, "  "));

        org.mockito.Mockito.verifyNoInteractions(activityLogService, notificacionDao);
        org.mockito.Mockito.verify(planillaDao, org.mockito.Mockito.never()).updateEtapa1Confirmada(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void laNotificacionVaAlProfesorDuenoConLaPlanillaComoEntidad() throws Exception {
        service.reabrirEtapa(10, 1, 3, "Faltó cargar una nota");

        verify(notificacionDao).crear(org.mockito.ArgumentMatchers.eq(7), org.mockito.ArgumentMatchers.eq("profesor"),
                org.mockito.ArgumentMatchers.eq("PLANILLA_REABIERTA"), anyString(),
                org.mockito.ArgumentMatchers.contains("Faltó cargar una nota"),
                org.mockito.ArgumentMatchers.eq("PLANILLA"), org.mockito.ArgumentMatchers.eq(10L));
    }
}
