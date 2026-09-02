package ctn.informatica.sca.google;

import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.Tarea;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ClassroomSyncOrchestratorImportUpdateDeleteTest {

    @Test
    public void import_update_delete_areHandled() throws IOException, SQLException {
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
        planilla.setId(42);
        planilla.setCursoId(7);

        Curso curso = new Curso(7, "esp", 2026, "A");
        when(cursoDao.findById(7)).thenReturn(curso);

        Course course = new Course();
        course.setId("C-9");
        when(adapter.isGoogleConnected(any())).thenReturn(true);
        when(adapter.resolveCourseForPlanilla(any(), any(), any())).thenReturn(Optional.of(course));

        // courseworks from Classroom: one new, one that should update
        CourseWork cwNew = new CourseWork();
        cwNew.setId("cw-new");
        cwNew.setTitle("Tarea nueva");

        CourseWork cwUpdate = new CourseWork();
        cwUpdate.setId("cw-update");
        cwUpdate.setTitle("Tarea actualizada");
        cwUpdate.setMaxPoints(20.0);

        when(adapter.listCourseWorkForCourse(any(), anyString())).thenReturn(List.of(cwNew, cwUpdate));

        // existing local tareas: one matching cw-update (old values), one that will be deleted (cw-deleted)
        Tarea tUpdate = new Tarea();
        tUpdate.setId(501);
        tUpdate.setPlanillaId(42);
        tUpdate.setTitulo("Tarea vieja");
        tUpdate.setTotal(10);
        tUpdate.setFecha(LocalDate.now());
        tUpdate.setGoogleCourseworkId("cw-update");

        Tarea tDeleted = new Tarea();
        tDeleted.setId(502);
        tDeleted.setPlanillaId(42);
        tDeleted.setTitulo("Tarea a borrar");
        tDeleted.setGoogleCourseworkId("cw-deleted");

        when(tareaDao.consultarTarea(42)).thenReturn(new java.util.ArrayList<>(List.of(tUpdate, tDeleted)));

        // instrument selection
        when(instrumentoDao.findAll()).thenReturn(List.of());

        when(alumnoDao.findByCursoId(7)).thenReturn(List.of(new Alumno()));

        // registro mapping
        when(adapter.syncStudentIdentities(any(), anyString(), anyList())).thenReturn(0);

        ClassroomSyncOrchestrator orchestrator = new ClassroomSyncOrchestrator(
                cursoDao, alumnoDao, tareaDao, registroDao, gradeDao, planillaDao, instrumentoDao, adapter);

        ClassroomSyncOrchestrator.ClassroomSyncResult result = orchestrator.syncPlanillaWithClassroom(profesor, planilla);

        assertNotNull(result);
        assertEquals(1, result.importedCourseworks()); // only the new one is counted here
        assertTrue(result.message().contains("actualizada") || result.message().contains("actualiz"));
        assertTrue(result.message().contains("eliminada") || result.message().contains("elimin") );
    }
}
