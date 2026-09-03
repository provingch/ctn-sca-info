package ctn.informatica.sca.google;

import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.Tarea;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class ClassroomSyncOrchestratorOrphanCleanupTest {

    @Test
    public void deletesOnlyImportedTareasWhenCourseNotFound() throws IOException, SQLException {
        var cursoDao = mock(ctn.informatica.sca.dao.CursoDao.class);
        var alumnoDao = mock(ctn.informatica.sca.dao.AlumnoDao.class);
        var tareaDao = mock(ctn.informatica.sca.dao.TareaDao.class);
        var registroDao = mock(ctn.informatica.sca.dao.RegistroDao.class);
        var gradeDao = mock(ctn.informatica.sca.dao.GradeDao.class);
        var planillaDao = mock(ctn.informatica.sca.dao.PlanillaDao.class);
        var instrumentoDao = mock(ctn.informatica.sca.dao.InstrumentoDao.class);

        GoogleClassroomAdapter adapter = mock(GoogleClassroomAdapter.class);

        Profesor profesor = new Profesor();
        profesor.setId(1);
        Planilla planilla = new Planilla();
        planilla.setId(100);
        planilla.setCursoId(7);

        when(adapter.isGoogleConnected(any())).thenReturn(true);
        when(adapter.resolveCourseForPlanilla(any(), any(), any())).thenReturn(Optional.empty());
        when(cursoDao.findById(7)).thenReturn(new ctn.informatica.sca.model.Curso(7, "esp", 2026, "A"));

        // existing tareas: one imported from Classroom, one manual (googleCourseworkId == null)
        Tarea imported = new Tarea();
        imported.setId(900);
        imported.setPlanillaId(100);
        imported.setGoogleCourseworkId("cw-900");

        Tarea manual = new Tarea();
        manual.setId(901);
        manual.setPlanillaId(100);
        manual.setGoogleCourseworkId(null);
        manual.setTitulo("Manual");
        manual.setFecha(LocalDate.now());

        when(tareaDao.consultarTarea(100)).thenReturn(new java.util.ArrayList<>(List.of(imported, manual)));

        ClassroomSyncOrchestrator orchestrator = new ClassroomSyncOrchestrator(
                cursoDao, alumnoDao, tareaDao, registroDao, gradeDao, planillaDao, instrumentoDao, adapter);

        ClassroomSyncOrchestrator.ClassroomSyncResult result = orchestrator.syncPlanillaWithClassroom(profesor, planilla);

        assertNotNull(result);
        // Verify the imported tarea was deleted
        verify(tareaDao, times(1)).delete(900);
        // Verify the manual tarea was not deleted
        verify(tareaDao, never()).delete(901);
        // Message should mention deletion count
        assertTrue(result.message().contains("1 tarea(s) importada(s)"));
    }
}
