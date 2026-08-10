package ctn.informatica.sca.google;

import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.StudentSubmission;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.Tarea;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ClassroomSyncOrchestratorHappyPathTest {

    @Test
    public void syncPlanilla_happyPath_countsMatch() throws IOException, SQLException {
        // Mocks
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
        course.setId("C-1");
        when(adapter.isGoogleConnected(any())).thenReturn(true);
        when(adapter.resolveCourseForPlanilla(any(), any(), any())).thenReturn(Optional.of(course));

        CourseWork cw = new CourseWork();
        cw.setId("cw-1");
        cw.setTitle("Tarea 1");
        when(adapter.listCourseWorkForCourse(any(), anyString())).thenReturn(List.of(cw));

        when(tareaDao.getGoogleCourseworkIdsForPlanilla(42)).thenReturn(Set.of());

        // alumnos
        Alumno a = new Alumno();
        a.setId(11);
        when(alumnoDao.findByCursoId(7)).thenReturn(List.of(a));

        when(adapter.syncStudentIdentities(any(), anyString(), anyList())).thenReturn(1);

        when(adapter.linkStudentsForCourse(any(), anyString(), anyList())).thenReturn(Map.of("user-1", 11));

        StudentSubmission s = new StudentSubmission();
        s.setUserId("user-1");
        s.setAssignedGrade(8.0);
        when(adapter.listStudentSubmissionsForCourseWork(any(), anyString(), anyString())).thenReturn(List.of(s));

        Tarea tareaExistente = new Tarea();
        tareaExistente.setId(500);
        tareaExistente.setPlanillaId(42);
        tareaExistente.setGoogleCourseworkId("cw-1");
        tareaExistente.setTotal(10);
        when(tareaDao.consultarTarea(42)).thenReturn(List.of(tareaExistente));

        // registrar registros
        when(registroDao.getRegistroIdsForPlanilla(eq(42), anySet())).thenReturn(Map.of(11, 1001));

        // Create orchestrator with mocks
        ClassroomSyncOrchestrator orchestrator = new ClassroomSyncOrchestrator(
                cursoDao, alumnoDao, tareaDao, registroDao, gradeDao, planillaDao, instrumentoDao, adapter);

        ClassroomSyncOrchestrator.ClassroomSyncResult result = orchestrator.syncPlanillaWithClassroom(profesor, planilla);

        assertNotNull(result);
        assertEquals("C-1", result.googleCourseId());
        assertTrue(result.classroomCourseMapped());
        assertEquals(1, result.importedCourseworks());
        assertEquals(1, result.linkedStudents());
        // La tarea mockeada tiene googleCourseworkId="cw-1", que matchea el CourseWork mockeado,
        // así que el submission con assignedGrade=8.0 debe importarse como 1 nota.
        assertEquals(1, result.importedGrades());
    }
}
