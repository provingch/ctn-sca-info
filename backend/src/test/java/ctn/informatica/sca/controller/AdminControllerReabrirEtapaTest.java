package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.QuejaDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.service.ActivityLogService;
import ctn.informatica.sca.service.PlanillaReaperturaService;
import ctn.informatica.sca.service.PlanillaReaperturaService.ReaperturaRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

/** Reabrir una etapa ya confirmada como admin global: exige motivo, lo deja en el log y avisa al profesor. */
class AdminControllerReabrirEtapaTest {

    private static final int PLANILLA_ID = 42;
    private static final int ADMIN = 1;
    private static final int PROFESOR = 7;
    private static final ReaperturaRequest MOTIVO = new ReaperturaRequest("Se cerró antes de tiempo por error");

    /** Controller cuyo alcance de admin sale del constructor (null = admin global) en vez de consultar la base. */
    private static final class ScopedAdminController extends AdminController {
        private final Integer specialtyId;

        private ScopedAdminController(PlanillaDao planillaDao, ActivityLogService activityLogService,
                PlanillaReaperturaService reapertura, Integer specialtyId) {
            super(mock(TareaDao.class), mock(GradeDao.class), planillaDao, mock(QuejaDao.class), activityLogService, mock(AlumnoDao.class), reapertura);
            this.specialtyId = specialtyId;
        }

        @Override
        protected Integer getSpecialtyAdminIdForUser(int userId) {
            return specialtyId;
        }
    }

    private PlanillaDao planillaDao;
    private ActivityLogService activityLogService;
    private NotificacionDao notificacionDao;
    private UserDao userDao;
    private PlanillaReaperturaService reapertura;
    private Planilla planilla;
    private AdminController controller;
    private final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken((long) ADMIN, null, List.of());

    @BeforeEach
    void setUp() throws Exception {
        planillaDao = mock(PlanillaDao.class);
        activityLogService = mock(ActivityLogService.class);
        notificacionDao = mock(NotificacionDao.class);
        userDao = mock(UserDao.class);
        planilla = new Planilla();
        planilla.setProfesorId(PROFESOR);
        when(planillaDao.findById(PLANILLA_ID)).thenReturn(planilla);
        when(planillaDao.updateEtapa1Confirmada(PLANILLA_ID, false)).thenReturn(true);
        when(planillaDao.updateEtapa2Confirmada(PLANILLA_ID, false)).thenReturn(true);
        when(userDao.findById(PROFESOR)).thenReturn(new User(PROFESOR, "profe", "Profesor Uno", 1));
        reapertura = new PlanillaReaperturaService(planillaDao, notificacionDao, userDao, activityLogService);
        controller = new ScopedAdminController(planillaDao, activityLogService, reapertura, null);
    }

    private static ResponseStatusException assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
        assertEquals(expected, ex.getStatusCode());
        return ex;
    }

    // ---- Etapa 1 --------------------------------------------------------------------------------------

    @Test
    void reabrirEtapa1_cerradaConMotivo_reabreYRegistraElMotivoEnElLog() throws Exception {
        planilla.setEtapa1Confirmada(true);

        Map<String, Object> result = controller.reabrirEtapa1(PLANILLA_ID, MOTIVO, auth);

        assertEquals(Map.of("planillaId", PLANILLA_ID, "etapa1Confirmada", false), result);
        verify(planillaDao).updateEtapa1Confirmada(PLANILLA_ID, false);
        verify(planillaDao, never()).updateEtapa2Confirmada(anyInt(), anyBoolean());
        verify(activityLogService).registrar(ADMIN, "Reabrió Etapa 1 de la planilla " + PLANILLA_ID + " — motivo: Se cerró antes de tiempo por error");
    }

    @Test
    void reabrirEtapa1_avisaAlProfesorDuenoConElMotivo() throws Exception {
        planilla.setEtapa1Confirmada(true);

        controller.reabrirEtapa1(PLANILLA_ID, MOTIVO, auth);

        verify(notificacionDao).crear(eq(PROFESOR), eq("profesor"), eq("PLANILLA_REABIERTA"), anyString(),
                contains("Motivo: Se cerró antes de tiempo por error"), eq("PLANILLA"), eq((long) PLANILLA_ID));
    }

    @Test
    void reabrirEtapa1_sinMotivo_responde400YNoTocaNada() throws Exception {
        planilla.setEtapa1Confirmada(true);

        for (ReaperturaRequest body : new ReaperturaRequest[] { null, new ReaperturaRequest(null), new ReaperturaRequest(""), new ReaperturaRequest("   \n  ") }) {
            ResponseStatusException ex = assertStatus(HttpStatus.BAD_REQUEST, () -> controller.reabrirEtapa1(PLANILLA_ID, body, auth));
            assertEquals("Debés indicar el motivo de la reapertura", ex.getReason());
        }

        verify(planillaDao, never()).updateEtapa1Confirmada(anyInt(), anyBoolean());
        verify(activityLogService, never()).registrar(anyInt(), anyString());
        verify(notificacionDao, never()).crear(anyInt(), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void reabrirEtapa1_noCerrada_responde400YNoTocaNada() throws Exception {
        ResponseStatusException ex = assertStatus(HttpStatus.BAD_REQUEST, () -> controller.reabrirEtapa1(PLANILLA_ID, MOTIVO, auth));

        assertEquals("Etapa 1 no está cerrada", ex.getReason());
        verify(planillaDao, never()).updateEtapa1Confirmada(anyInt(), anyBoolean());
        verify(activityLogService, never()).registrar(anyInt(), anyString());
    }

    @Test
    void reabrirEtapa1_conLaEtapa2CerradaPeroLaEtapa1No_responde400() throws Exception {
        planilla.setEtapa2Confirmada(true);

        assertStatus(HttpStatus.BAD_REQUEST, () -> controller.reabrirEtapa1(PLANILLA_ID, MOTIVO, auth));

        verify(planillaDao, never()).updateEtapa1Confirmada(anyInt(), anyBoolean());
    }

    @Test
    void reabrirEtapa1_planillaInexistente_responde404() throws Exception {
        assertStatus(HttpStatus.NOT_FOUND, () -> controller.reabrirEtapa1(999, MOTIVO, auth));

        verify(planillaDao, never()).updateEtapa1Confirmada(anyInt(), anyBoolean());
    }

    @Test
    void reabrirEtapa1_siElUpdateNoAfectaFilas_responde500YNoRegistraActividad() throws Exception {
        planilla.setEtapa1Confirmada(true);
        when(planillaDao.updateEtapa1Confirmada(PLANILLA_ID, false)).thenReturn(false);

        assertStatus(HttpStatus.INTERNAL_SERVER_ERROR, () -> controller.reabrirEtapa1(PLANILLA_ID, MOTIVO, auth));

        verify(activityLogService, never()).registrar(anyInt(), anyString());
        verify(notificacionDao, never()).crear(anyInt(), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void reabrirEtapa1_adminDeEspecialidad_responde403SinImportarElMotivo() throws Exception {
        planilla.setEtapa1Confirmada(true);
        AdminController scoped = new ScopedAdminController(planillaDao, activityLogService, reapertura, 5);

        assertStatus(HttpStatus.FORBIDDEN, () -> scoped.reabrirEtapa1(PLANILLA_ID, MOTIVO, auth));
        assertStatus(HttpStatus.FORBIDDEN, () -> scoped.reabrirEtapa1(PLANILLA_ID, null, auth));

        verify(planillaDao, never()).updateEtapa1Confirmada(anyInt(), anyBoolean());
    }

    @Test
    void reabrirEtapa1_siFallaElRegistroDeActividadONotificar_igualReabre() throws Exception {
        planilla.setEtapa1Confirmada(true);
        org.mockito.Mockito.doThrow(new RuntimeException("log caído")).when(activityLogService).registrar(anyInt(), anyString());
        org.mockito.Mockito.doThrow(new RuntimeException("db caída")).when(notificacionDao)
                .crear(anyInt(), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());

        Map<String, Object> result = controller.reabrirEtapa1(PLANILLA_ID, MOTIVO, auth);

        assertEquals(false, result.get("etapa1Confirmada"));
        verify(planillaDao).updateEtapa1Confirmada(PLANILLA_ID, false);
    }

    // ---- Etapa 2 --------------------------------------------------------------------------------------

    @Test
    void reabrirEtapa2_cerradaConMotivo_reabreYRegistraElMotivoEnElLog() throws Exception {
        planilla.setEtapa2Confirmada(true);

        Map<String, Object> result = controller.reabrirEtapa2(PLANILLA_ID, MOTIVO, auth);

        assertEquals(Map.of("planillaId", PLANILLA_ID, "etapa2Confirmada", false), result);
        verify(planillaDao).updateEtapa2Confirmada(PLANILLA_ID, false);
        verify(planillaDao, never()).updateEtapa1Confirmada(anyInt(), anyBoolean());
        verify(activityLogService).registrar(ADMIN, "Reabrió Etapa 2 de la planilla " + PLANILLA_ID + " — motivo: Se cerró antes de tiempo por error");
    }

    @Test
    void reabrirEtapa2_sinMotivo_responde400YNoTocaNada() throws Exception {
        planilla.setEtapa2Confirmada(true);

        assertStatus(HttpStatus.BAD_REQUEST, () -> controller.reabrirEtapa2(PLANILLA_ID, new ReaperturaRequest(" "), auth));

        verify(planillaDao, never()).updateEtapa2Confirmada(anyInt(), anyBoolean());
    }

    @Test
    void reabrirEtapa2_noCerrada_responde400YNoTocaNada() throws Exception {
        planilla.setEtapa1Confirmada(true);

        ResponseStatusException ex = assertStatus(HttpStatus.BAD_REQUEST, () -> controller.reabrirEtapa2(PLANILLA_ID, MOTIVO, auth));

        assertEquals("Etapa 2 no está cerrada", ex.getReason());
        verify(planillaDao, never()).updateEtapa2Confirmada(anyInt(), anyBoolean());
    }

    @Test
    void reabrirEtapa2_planillaInexistente_responde404() throws Exception {
        assertStatus(HttpStatus.NOT_FOUND, () -> controller.reabrirEtapa2(999, MOTIVO, auth));
    }

    @Test
    void reabrirEtapa2_adminDeEspecialidad_responde403() throws Exception {
        planilla.setEtapa2Confirmada(true);
        AdminController scoped = new ScopedAdminController(planillaDao, activityLogService, reapertura, 5);

        assertStatus(HttpStatus.FORBIDDEN, () -> scoped.reabrirEtapa2(PLANILLA_ID, MOTIVO, auth));

        verify(planillaDao, never()).updateEtapa2Confirmada(anyInt(), anyBoolean());
    }
}
