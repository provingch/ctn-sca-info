package ctn.informatica.sca.controller;

import ctn.informatica.sca.google.ClassroomSyncOrchestrator;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import java.time.LocalDate;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PlanillaControllerTest {

    @Test
    public void syncClassroom_profesorNotFound_throwsBadRequest() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);

        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);

        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(5);

        when(profesorDao.findById(5)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.syncClassroom(10, auth));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    public void syncClassroom_success_returnsResponse() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);

        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);

        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);

        Profesor prof = new Profesor();
        prof.setId(5);
        when(profesorDao.findById(5)).thenReturn(prof);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(5);

        ClassroomSyncOrchestrator.ClassroomSyncResult result = new ClassroomSyncOrchestrator.ClassroomSyncResult("C100", true, 2, 3, 4, null, null, null, "ok");
        when(orchestrator.syncPlanillaWithClassroom(eq(prof), eq(p))).thenReturn(result);

        var response = controller.syncClassroom(10, auth);
        assertNotNull(response);
        assertEquals(10, response.planillaId());
        assertEquals("C100", response.googleCourseId());
        assertEquals(2, response.importedCourseworks());
        assertEquals(3, response.linkedStudents());
        assertEquals(4, response.importedGrades());
        assertEquals("ok", response.message());
    }

    @Test
    public void guardaFechaCierreEtapa1_planillaCerrada_retornaConflict() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);

        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);
        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        p.setEtapa1Confirmada(true);
        when(planillaDao.findById(10)).thenReturn(p);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(5);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.guardarFechaCierreEtapa1(10, LocalDate.of(2026, 6, 20), auth));
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Etapa 1 cerrada"));
    }

    @Test
    public void confirmarEtapa1_sinFecha_rechazaSolicitud() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);

        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);
        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        p.setEtapa1Confirmada(false);
        when(planillaDao.findById(10)).thenReturn(p);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(5);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.confirmarEtapa1(10, auth));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    public void guardarPortada_dueño_guardaElDataUriTalCual() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);
        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);

        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);
        when(planillaDao.updatePortada(eq(10), anyString())).thenReturn(true);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(5);

        String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString("hola".getBytes());
        controller.guardarPortada(10, new PlanillaController.PortadaInput(dataUri), auth);

        verify(planillaDao).updatePortada(10, dataUri);
    }

    @Test
    public void guardarPortada_otroProfesor_retorna403SinTocarLaBaseDeDatos() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);
        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);

        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(99);

        String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString("hola".getBytes());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.guardarPortada(10, new PlanillaController.PortadaInput(dataUri), auth));
        assertEquals(403, ex.getStatusCode().value());
        verify(planillaDao, never()).updatePortada(anyInt(), any());
    }

    @Test
    public void guardarPortada_demasiadoGrande_retorna400() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);
        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);

        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(5);

        // ~900.000 caracteres base64 => ~675.000 bytes, por encima del tope de 600 KB.
        String dataUri = "data:image/png;base64," + "A".repeat(900_000);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.guardarPortada(10, new PlanillaController.PortadaInput(dataUri), auth));
        assertEquals(400, ex.getStatusCode().value());
        verify(planillaDao, never()).updatePortada(anyInt(), any());
    }

    @Test
    public void guardarPortada_tipoNoPermitido_retorna400() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);
        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);

        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(5);

        String dataUri = "data:application/pdf;base64,SGVsbG8=";
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.guardarPortada(10, new PlanillaController.PortadaInput(dataUri), auth));
        assertEquals(400, ex.getStatusCode().value());
        verify(planillaDao, never()).updatePortada(anyInt(), any());
    }

    @Test
    public void eliminarPortada_dueño_borraLaColumna() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);
        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);

        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);
        when(planillaDao.updatePortada(10, null)).thenReturn(true);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(5);

        controller.eliminarPortada(10, auth);
        verify(planillaDao).updatePortada(10, null);
    }

    @Test
    public void eliminarPortada_otroProfesor_retorna403() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);
        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);

        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(99);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.eliminarPortada(10, auth));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    public void getPortada_sinPortada_retorna404NoUn200Vacio() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);
        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);

        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);
        when(planillaDao.findPortada(10)).thenReturn(null);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(5);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.getPortada(10, auth));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    public void getPortada_conPortada_devuelveBytesDecodificadosYContentTypeReal() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);
        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);

        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);
        byte[] original = "contenido-de-prueba".getBytes();
        when(planillaDao.findPortada(10)).thenReturn("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(original));

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(5);

        var response = controller.getPortada(10, auth);
        assertEquals("image/jpeg", response.getHeaders().getFirst("Content-Type"));
        assertEquals("private, max-age=86400", response.getHeaders().getFirst("Cache-Control"));
        assertArrayEquals(original, response.getBody());
    }

    @Test
    public void getPortada_otroProfesor_retorna403() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        var orchestrator = mock(ClassroomSyncOrchestrator.class);
        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, orchestrator);

        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(99);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.getPortada(10, auth));
        assertEquals(403, ex.getStatusCode().value());
    }

    private static ctn.informatica.sca.model.Tarea tarea(int id, int total, String courseworkId) {
        var t = new ctn.informatica.sca.model.Tarea();
        t.setId(id);
        t.setTotal(total);
        t.setGoogleCourseworkId(courseworkId);
        return t;
    }

    private static Authentication authDe(int userId) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userId);
        return auth;
    }

    @Test
    public void saveGrades_tareaDeClassroom_noSeGuardaYAvisa() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, mock(ClassroomSyncOrchestrator.class));
        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);
        var tareas = new java.util.ArrayList<>(java.util.List.of(tarea(1, 10, "cw-1")));

        try (var tareaDao = mockConstruction(ctn.informatica.sca.dao.TareaDao.class, (m, c) -> when(m.consultarTarea(10)).thenReturn(tareas));
                var builder = mockStatic(ctn.informatica.sca.util.PlanillaProcesoWorkbookBuilder.class);
                var gradeDao = mockConstruction(ctn.informatica.sca.dao.GradeDao.class)) {
            builder.when(() -> ctn.informatica.sca.util.PlanillaProcesoWorkbookBuilder.filterTasksByEtapa(any(), any())).thenReturn(tareas);

            var request = new PlanillaController.SaveGradesRequest(java.util.List.of(
                    new PlanillaController.AlumnoGradesInput(100, java.util.List.of(new PlanillaController.GradeItemInput(1, 9)))));
            var response = controller.saveGrades(10, request, authDe(5));

            assertEquals(0, response.savedCount());
            assertEquals(1, response.skippedCount());
            assertTrue(response.warnings().stream().anyMatch(w -> w.contains("Google Classroom")));
            for (var created : gradeDao.constructed()) {
                verify(created, never()).saveGradesBatch(anyInt(), any());
            }
        }
    }

    @Test
    public void saveGrades_lote_mixto_guardaSoloLasTareasLocales() throws Exception {
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var profesorDao = mock(ctn.informatica.sca.dao.ProfesorDao.class);
        PlanillaController controller = new PlanillaController(planillaDao, profesorDao, mock(ClassroomSyncOrchestrator.class));
        Planilla p = new Planilla();
        p.setId(10);
        p.setProfesorId(5);
        when(planillaDao.findById(10)).thenReturn(p);
        var tareas = new java.util.ArrayList<>(java.util.List.of(tarea(1, 10, "cw-1"), tarea(2, 10, null)));

        try (var tareaDao = mockConstruction(ctn.informatica.sca.dao.TareaDao.class, (m, c) -> when(m.consultarTarea(10)).thenReturn(tareas));
                var builder = mockStatic(ctn.informatica.sca.util.PlanillaProcesoWorkbookBuilder.class);
                var registroDao = mockConstruction(ctn.informatica.sca.dao.RegistroDao.class, (m, c) -> {
                    when(m.findAlumnoIdsTrasladados(anyInt(), any())).thenReturn(java.util.Set.of());
                    when(m.getRegistroIdsForPlanilla(anyInt(), any())).thenReturn(java.util.Map.of(100, 900));
                });
                var gradeDao = mockConstruction(ctn.informatica.sca.dao.GradeDao.class)) {
            builder.when(() -> ctn.informatica.sca.util.PlanillaProcesoWorkbookBuilder.filterTasksByEtapa(any(), any())).thenReturn(tareas);

            var request = new PlanillaController.SaveGradesRequest(java.util.List.of(
                    new PlanillaController.AlumnoGradesInput(100, java.util.List.of(
                            new PlanillaController.GradeItemInput(1, 9),
                            new PlanillaController.GradeItemInput(2, 7)))));
            var response = controller.saveGrades(10, request, authDe(5));

            assertEquals(1, response.savedCount());
            assertEquals(1, response.skippedCount());
            verify(gradeDao.constructed().get(0)).saveGradesBatch(10, java.util.Map.of(900, java.util.Map.of(2, 7)));
        }
    }
}
