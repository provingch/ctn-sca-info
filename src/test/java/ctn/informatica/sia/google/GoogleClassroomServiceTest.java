package ctn.informatica.sia.google;

import ctn.informatica.sia.model.Alumno;
import ctn.informatica.sia.model.Curso;
import ctn.informatica.sia.model.Profesor;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.UserProfile;
import com.google.api.services.classroom.model.Name;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleClassroomServiceTest {

    @Test
    void shouldMatchStudentByNameAndEmail() {
        Alumno local = new Alumno();
        local.setId(10);
        local.setNombre("Ana");
        local.setApellido("Pérez");
        local.setGoogleEmail("ana.perez@example.com");

        Student classroomStudent = new Student();
        classroomStudent.setUserId("student-123");
        UserProfile profile = new UserProfile();
        Name name = new Name();
        name.setFullName("Ana Pérez");
        profile.setName(name);
        profile.setEmailAddress("ana.perez@example.com");
        classroomStudent.setProfile(profile);

        Optional<Alumno> match = GoogleClassroomService.findBestStudentMatch(List.of(local), classroomStudent);

        assertTrue(match.isPresent());
        assertEquals(10, match.get().getId());
    }

    @Test
    void shouldTreatProfessorAsConnectedWhenRefreshTokenExists() {
        Profesor profesor = new Profesor();
        profesor.setGcRefreshToken("refresh-token");

        assertTrue(GoogleClassroomService.isGoogleConnected(profesor));
    }

    @Test
    void shouldNotMatchCoursesFromDifferentSpecialtyWhenOnlyLevelAndSectionAlign() {
        Curso curso = new Curso(1, "Informática", ctn.informatica.sia.util.AcademicPeriod.current() + 1, "A");
        Course classroomCourse = new Course();
        classroomCourse.setName("Historia 2do A");
        classroomCourse.setRoom("");

        assertFalse(GoogleClassroomService.courseMatchesTeacherCurso(classroomCourse, List.of(curso)));
    }

    @Test
    void shouldMatchWhenSpecialtyComesFromCourseName() {
        Curso curso = new Curso(1, "Informática", ctn.informatica.sia.util.AcademicPeriod.current() + 1, "A");
        Course classroomCourse = new Course();
        classroomCourse.setName("Informática 2do A");
        classroomCourse.setRoom("Otra sala");

        assertTrue(GoogleClassroomService.courseMatchesTeacherCurso(classroomCourse, List.of(curso)));
    }

    @Test
    void shouldNotMatchWhenSpecialtyOnlyComesFromRoom() {
        Curso curso = new Curso(1, "Informática", ctn.informatica.sia.util.AcademicPeriod.current() + 1, "A");
        Course classroomCourse = new Course();
        classroomCourse.setName("2do A");
        classroomCourse.setRoom("Informática");

        assertFalse(GoogleClassroomService.courseMatchesTeacherCurso(classroomCourse, List.of(curso)));
    }
}
