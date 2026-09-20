package ctn.informatica.sca.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import ctn.informatica.sca.controller.EvaluacionPlanillaController.PlanillaResumenDto;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.service.ActivityLogService;
import ctn.informatica.sca.service.PlanillaReaperturaService;
import ctn.informatica.sca.service.PlanillaReaperturaService.ReaperturaRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

/** Listado y reapertura para evaluación. El detalle de sólo lectura se prueba contra la base en EvaluacionPlanillasDbIntegrationTest. */
class EvaluacionPlanillaControllerTest {

    private static final int EVALUADOR = 14;
    private static final int CURSO_ID = 5;

    private PlanillaDao planillaDao;
    private ProfesorDao profesorDao;
    private ActivityLogService activityLogService;
    private NotificacionDao notificacionDao;
    private EvaluacionPlanillaController controller;
    private final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken((long) EVALUADOR, null, List.of());

    private static Planilla planilla(int id, int materiaId, String etapa, int profesorId) {
        Planilla p = new Planilla();
        p.setId(id);
        p.setMateriaId(materiaId);
        p.setCursoId(CURSO_ID);
        p.setPeriodo(2026);
        p.setEtapa(etapa);
        p.setProfesorId(profesorId);
        return p;
    }

    @BeforeEach
    void setUp() throws Exception {
        planillaDao = mock(PlanillaDao.class);
        profesorDao = mock(ProfesorDao.class);
        activityLogService = mock(ActivityLogService.class);
        notificacionDao = mock(NotificacionDao.class);
        CursoDao cursoDao = mock(CursoDao.class);
        EspecialidadDao especialidadDao = mock(EspecialidadDao.class);
        UserDao userDao = mock(UserDao.class);

        when(cursoDao.findById(CURSO_ID)).thenReturn(new Curso(CURSO_ID, "Informática", 2028, "A"));
        when(especialidadDao.findAll()).thenReturn(List.of(new Especialidad(1, "Electrónica"), new Especialidad(2, "Informática")));

        Planilla primera1 = planilla(100, 1, "primera", 7);
        primera1.setFechaCierreEtapa1(LocalDate.of(2026, 6, 30));
        primera1.setEtapa1Confirmada(true);
        Planilla primera2 = planilla(101, 2, "primera", 8);
        Planilla segunda1 = planilla(102, 1, "segunda", 7);
        when(planillaDao.findPlanillasByCourse(2, 2028, "A", 2026)).thenReturn(List.of(
                new PlanillaDao.PlanillaInfo(primera1, "Programación"),
                new PlanillaDao.PlanillaInfo(primera2, "Matemática"),
                new PlanillaDao.PlanillaInfo(segunda1, "Programación")));
        when(planillaDao.findById(100)).thenReturn(primera1);
        when(planillaDao.findById(101)).thenReturn(primera2);
        when(planillaDao.findById(102)).thenReturn(segunda1);
        Profesor p7 = new Profesor();
        p7.setNombre("Marcos");
        p7.setApellido("Benítez");
        when(profesorDao.findById(7)).thenReturn(p7);
        when(userDao.findById(7)).thenReturn(new User(7, "profe", "Profesor Uno", 1));

        PlanillaReaperturaService reapertura = new PlanillaReaperturaService(planillaDao, notificacionDao, userDao, activityLogService);
        controller = new EvaluacionPlanillaController(planillaDao, cursoDao, especialidadDao, profesorDao, reapertura);
    }

    @Test
    void listarPlanillas_filtraPorEtapa() {
        List<PlanillaResumenDto> primera = controller.listarPlanillas(CURSO_ID, "primera", 2026, null, auth);
        List<PlanillaResumenDto> segunda = controller.listarPlanillas(CURSO_ID, "segunda", 2026, null, auth);

        assertEquals(List.of(100, 101), primera.stream().map(PlanillaResumenDto::id).toList());
        assertEquals(List.of("Programación", "Matemática"), primera.stream().map(PlanillaResumenDto::materiaNombre).toList());
        assertEquals(List.of(102), segunda.stream().map(PlanillaResumenDto::id).toList());
    }

    @Test
    void listarPlanillas_filtraPorMateria_ySinMateriaOCeroTraeTodas() {
        assertEquals(List.of(101), controller.listarPlanillas(CURSO_ID, "primera", 2026, 2, auth).stream().map(PlanillaResumenDto::id).toList());
        assertEquals(2, controller.listarPlanillas(CURSO_ID, "primera", 2026, 0, auth).size());
    }

    @Test
    void listarPlanillas_devuelveProfesorYEstadoDeCierre() {
        PlanillaResumenDto dto = controller.listarPlanillas(CURSO_ID, "primera", 2026, 1, auth).get(0);

        assertEquals(7, dto.profesorId());
        assertEquals("Marcos Benítez", dto.profesorNombre());
        assertEquals(1, dto.etapaIndex());
        assertTrue(dto.etapa1Confirmada());
        assertEquals(LocalDate.of(2026, 6, 30), dto.fechaCierreEtapa1());
        assertEquals(false, dto.etapa2Confirmada());
    }

    @Test
    void listarPlanillas_cursoInexistente_responde404() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.listarPlanillas(999, "primera", 2026, null, auth));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void reabrirComoEvaluador_sinMotivo_responde400YNoTocaNada() throws Exception {
        planillaDao.findById(100).setEtapa1Confirmada(true);

        for (ReaperturaRequest body : new ReaperturaRequest[] { null, new ReaperturaRequest(""), new ReaperturaRequest("   ") }) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.reabrirEtapa1(100, body, auth));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        verify(planillaDao, never()).updateEtapa1Confirmada(anyInt(), anyBoolean());
        verify(activityLogService, never()).registrar(anyInt(), anyString());
    }

    @Test
    void reabrirComoEvaluador_conMotivo_reabreRegistraElLogConElMotivoYAvisaAlProfesor() throws Exception {
        planillaDao.findById(100).setEtapa1Confirmada(true);
        when(planillaDao.updateEtapa1Confirmada(100, false)).thenReturn(true);

        Map<String, Object> result = controller.reabrirEtapa1(100, new ReaperturaRequest("El profesor pidió corregir una nota"), auth);

        assertEquals(Map.of("planillaId", 100, "etapa1Confirmada", false), result);
        verify(planillaDao).updateEtapa1Confirmada(100, false);
        verify(activityLogService).registrar(EVALUADOR, "Reabrió Etapa 1 de la planilla 100 — motivo: El profesor pidió corregir una nota");
        verify(notificacionDao).crear(eq(7), eq("profesor"), eq("PLANILLA_REABIERTA"), anyString(),
                contains("El profesor pidió corregir una nota"), eq("PLANILLA"), eq(100L));
    }

    @Test
    void reabrirEtapa2ComoEvaluador_conMotivo_reabreLaEtapa2() throws Exception {
        planillaDao.findById(102).setEtapa2Confirmada(true);
        when(planillaDao.updateEtapa2Confirmada(102, false)).thenReturn(true);

        Map<String, Object> result = controller.reabrirEtapa2(102, new ReaperturaRequest("Cierre por error"), auth);

        assertEquals(Map.of("planillaId", 102, "etapa2Confirmada", false), result);
        verify(planillaDao).updateEtapa2Confirmada(102, false);
        verify(planillaDao, never()).updateEtapa1Confirmada(anyInt(), anyBoolean());
        verify(activityLogService).registrar(EVALUADOR, "Reabrió Etapa 2 de la planilla 102 — motivo: Cierre por error");
    }

    @Test
    void reabrirComoEvaluador_etapaNoCerrada_responde400() throws Exception {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.reabrirEtapa1(101, new ReaperturaRequest("x"), auth));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Etapa 1 no está cerrada", ex.getReason());
        verify(notificacionDao, never()).crear(anyInt(), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
    }
}
