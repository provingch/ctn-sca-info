package ctn.informatica.sca.controller;

import ctn.informatica.sca.google.ClassroomSyncOrchestrator;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
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
}
