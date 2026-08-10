package ctn.informatica.sca.google;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClassroomSyncOrchestratorTest {

    @Test
    public void syncPlanillaWithNulls_returnsErrorResult() throws Exception {
        ClassroomSyncOrchestrator orchestrator = new ClassroomSyncOrchestrator();
        ClassroomSyncOrchestrator.ClassroomSyncResult result = orchestrator.syncPlanillaWithClassroom(null, null);

        assertNotNull(result);
        assertEquals(0, result.importedCourseworks());
        assertEquals(0, result.linkedStudents());
        assertEquals(0, result.importedGrades());
        assertFalse(result.classroomCourseMapped());
        assertTrue(result.message().toLowerCase().contains("datos de planilla") || result.message().toLowerCase().contains("no disponibles"));
    }

    @Test
    public void syncPlanillaWhenProfesorNotConnected_returnsNotConnectedMessage() throws Exception {
        ClassroomSyncOrchestrator orchestrator = new ClassroomSyncOrchestrator();
        Profesor profesor = new Profesor();
        Planilla planilla = new Planilla();
        ClassroomSyncOrchestrator.ClassroomSyncResult result = orchestrator.syncPlanillaWithClassroom(profesor, planilla);

        assertNotNull(result);
        assertEquals(0, result.importedCourseworks());
        assertEquals(0, result.linkedStudents());
        assertEquals(0, result.importedGrades());
        assertFalse(result.classroomCourseMapped());
        assertTrue(result.message().toLowerCase().contains("google classroom no está conectado") || result.message().toLowerCase().contains("no está conectado"));
    }
}
