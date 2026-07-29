package ctn.informatica.sca.google;

import static org.junit.jupiter.api.Assertions.*;

import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.util.AcademicPeriod;
import com.google.api.services.classroom.model.Course;
import java.util.List;
import org.junit.jupiter.api.Test;

public class GoogleClassroomUtilsTest {

    @Test
    public void testParseCourseKey() {
        assertEquals(1, GoogleClassroomUtils.parseCourseKey("Matemáticas Primero A").get().getNivel());
        assertEquals("A", GoogleClassroomUtils.parseCourseKey("Matemáticas Primero A").get().getSeccion());
        assertEquals(AcademicPeriod.current(), GoogleClassroomUtils.parseCourseKey("Matemáticas Primero A").get().getPeriodo());

        assertEquals(2, GoogleClassroomUtils.parseCourseKey("Historia Segundo B").get().getNivel());
        assertEquals("B", GoogleClassroomUtils.parseCourseKey("Historia Segundo B").get().getSeccion());

        assertEquals(3, GoogleClassroomUtils.parseCourseKey("Ciencias tercero c").get().getNivel());
        assertEquals("C", GoogleClassroomUtils.parseCourseKey("Ciencias tercero c").get().getSeccion());

        assertTrue(GoogleClassroomUtils.isAllowedClassroomCourse("Primero A - Física"));
        assertFalse(GoogleClassroomUtils.isAllowedClassroomCourse("Física Avanzada"));
    }

    @Test
    public void testParseCourseKeyUsesCourseNameOnlyForSpecialty() {
        var key = GoogleClassroomUtils.parseCourseKey("Matemática Primero A", "Informática");
        assertTrue(key.isPresent());
        assertEquals(1, key.get().getNivel());
        assertEquals("A", key.get().getSeccion());
        assertEquals(AcademicPeriod.current(), key.get().getPeriodo());
        assertEquals("matematica", key.get().getSala());
    }

    @Test
    public void testParseCourseKeyRequiresCurrentYearWhenExplicitlySpecified() {
        int currentYear = AcademicPeriod.current();
        int futureYear = currentYear + 1;

        assertTrue(GoogleClassroomUtils.parseCourseKey("Matemática Primero A " + currentYear).isPresent());
        assertFalse(GoogleClassroomUtils.parseCourseKey("Matemática Primero A " + futureYear).isPresent());

        String range = "Matemática Primero A " + (currentYear - 1) + "/" + currentYear;
        assertTrue(GoogleClassroomUtils.parseCourseKey(range).isPresent());

        String mismatchedRange = "Matemática Primero A " + (currentYear - 2) + "/" + (currentYear - 1);
        assertFalse(GoogleClassroomUtils.parseCourseKey(mismatchedRange).isPresent());
    }

    @Test
    public void testParseCourseKeyWithCompactLevelAndSection() {
        var key = GoogleClassroomUtils.parseCourseKey("Matemática 1A");
        assertTrue(key.isPresent());
        assertEquals(1, key.get().getNivel());
        assertEquals("A", key.get().getSeccion());

        var keyWithOrdinalSymbol = GoogleClassroomUtils.parseCourseKey("Historia 2°B");
        assertTrue(keyWithOrdinalSymbol.isPresent());
        assertEquals(2, keyWithOrdinalSymbol.get().getNivel());
        assertEquals("B", keyWithOrdinalSymbol.get().getSeccion());
    }

    @Test
    public void testTeacherCourseMatchUsesCourseIdentity() {
        Curso curso = new Curso(1, "Algoritmia", 2026, "A");
        Course classroomCourse = new Course();
        classroomCourse.setName("Algoritmia 2do A");
        classroomCourse.setRoom("");

        assertTrue(GoogleClassroomService.courseMatchesTeacherCurso(classroomCourse, List.of(curso)));
    }

    @Test
    public void testExtractSpecialtyHintUsesCourseNameOnly() {
        assertEquals("laboratorio redes", GoogleClassroomUtils.extractSpecialtyHint("Laboratorio redes 3ro A", "Informática"));
        assertEquals("algoritmia", GoogleClassroomUtils.extractSpecialtyHint("Algoritmia 2do A", "Química"));
        assertEquals("", GoogleClassroomUtils.extractSpecialtyHint("2do A", "Informática"));
    }

    @Test
    public void testNormalizeTitleForMatching() {
        assertEquals("tarea de matematica", GoogleClassroomUtils.normalizeTitle("Tarea de Matemática"));
        assertEquals("tp 1", GoogleClassroomUtils.normalizeTitle("TP 1"));
        assertEquals("trabajo practico 2", GoogleClassroomUtils.normalizeTitle("Trabajo Práctico 2"));
    }
}
