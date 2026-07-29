package ctn.informatica.sia.google;

import ctn.informatica.sia.model.Curso;
import java.util.List;
import java.util.Optional;

public class GoogleClassroomSyncService {

    private GoogleClassroomSyncService() {
        // helper class only
    }

    public static Optional<GoogleClassroomUtils.CourseKey> parseCourseKey(String classroomCourseName) {
        return GoogleClassroomUtils.parseCourseKey(classroomCourseName);
    }

    public static Optional<GoogleClassroomUtils.CourseKey> parseCourseKey(String classroomCourseName, String room) {
        return GoogleClassroomUtils.parseCourseKey(classroomCourseName, room);
    }

    public static boolean isAllowedClassroomCourse(String classroomCourseName) {
        return GoogleClassroomUtils.isAllowedClassroomCourse(classroomCourseName);
    }

    public static boolean matchesAnyCurso(String classroomCourseName, List<Curso> cursos) {
        return matchesAnyCurso(classroomCourseName, null, cursos);
    }

    public static boolean matchesAnyCurso(String classroomCourseName, String room, List<Curso> cursos) {
        Optional<GoogleClassroomUtils.CourseKey> courseKeyOpt = parseCourseKey(classroomCourseName, room);
        if (courseKeyOpt.isEmpty() || cursos == null || cursos.isEmpty()) {
            return false;
        }
        GoogleClassroomUtils.CourseKey courseKey = courseKeyOpt.get();
        for (Curso curso : cursos) {
            if (curso.matchesCourseKey(courseKey)) {
                return true;
            }
        }
        return false;
    }
}
