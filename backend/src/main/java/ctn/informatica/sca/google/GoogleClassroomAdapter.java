package ctn.informatica.sca.google;

import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.StudentSubmission;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GoogleClassroomAdapter {

    public boolean isGoogleConnected(Profesor profesor) {
        return GoogleClassroomService.isGoogleConnected(profesor);
    }

    public Optional<Course> resolveCourseForPlanilla(Profesor profesor, Curso curso, Planilla planilla) throws IOException {
        return GoogleClassroomService.resolveCourseForPlanilla(profesor, curso, planilla, null);
    }

    public List<CourseWork> listCourseWorkForCourse(Profesor profesor, String courseId) throws IOException {
        return GoogleClassroomService.listCourseWorkForCourse(profesor, courseId);
    }

    public int syncStudentIdentities(Profesor profesor, String courseId, List<Alumno> alumnos) throws IOException {
        return GoogleClassroomService.syncStudentIdentities(profesor, courseId, alumnos);
    }

    public Map<String, Integer> linkStudentsForCourse(Profesor profesor, String courseId, List<Alumno> alumnos) throws IOException {
        return GoogleClassroomService.linkStudentsForCourse(profesor, courseId, alumnos);
    }

    public List<StudentSubmission> listStudentSubmissionsForCourseWork(Profesor profesor, String courseId, String courseWorkId) throws IOException {
        return GoogleClassroomService.listStudentSubmissionsForCourseWork(profesor, courseId, courseWorkId);
    }
}
