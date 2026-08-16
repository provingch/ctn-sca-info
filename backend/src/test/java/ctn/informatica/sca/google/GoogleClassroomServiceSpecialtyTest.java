package ctn.informatica.sca.google;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.services.classroom.model.Course;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.util.AcademicPeriod;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Convención de parseo de Google Classroom (ver también GoogleClassroomUtils):
 *  - Nombre de la clase: materia + nivel + sección, ej. "Algorítmica 2do A".
 *  - Sección (campo de Classroom): año académico, ej. "2026".
 *  - Sala (campo de Classroom): especialidad a la que pertenece la clase,
 *    ej. "Informática" -- NO se infiere más del nombre, porque lo que queda
 *    ahí es la materia puntual (ej. "algoritmica"), que casi nunca coincide
 *    como texto con el nombre de la especialidad.
 */
class GoogleClassroomServiceSpecialtyTest {

    private static final int CURRENT_PERIOD = AcademicPeriod.current();

    private Curso cursoInformatica2doA() {
        // promocion tal que getNivel() (= period - promocion + 3) dé 2.
        return new Curso(1, "Informática", CURRENT_PERIOD + 1, "A");
    }

    @Test
    void detectaLaClaseCuandoLaSalaDeclaraLaEspecialidad() {
        Course course = new Course()
                .setId("course-1")
                .setName("Algorítmica 2do A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("Informática");

        assertTrue(GoogleClassroomService.courseMatchesTeacherCurso(course, List.of(cursoInformatica2doA())));
    }

    @Test
    void noDetectaLaClaseSiLaSalaNoDeclaraNingunaEspecialidad() {
        // Caso real observado: la Sala tenía una lista de aulas físicas, no
        // la especialidad -- por diseño, ya no debe matchear.
        Course course = new Course()
                .setId("course-2")
                .setName("Algorítmica 2do A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("Sala 1, 3, 5, Laboratorio Software, Hardware, PC4");

        assertFalse(GoogleClassroomService.courseMatchesTeacherCurso(course, List.of(cursoInformatica2doA())));
    }

    @Test
    void noDetectaLaClaseSiLaSalaDeclaraOtraEspecialidad() {
        Course course = new Course()
                .setId("course-3")
                .setName("Algorítmica 2do A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("Electricidad");

        assertFalse(GoogleClassroomService.courseMatchesTeacherCurso(course, List.of(cursoInformatica2doA())));
    }

    @Test
    void noDetectaLaClaseSiElAnioDeSeccionNoCoincideConElPeriodoActual() {
        Course course = new Course()
                .setId("course-4")
                .setName("Algorítmica 2do A")
                .setSection(String.valueOf(CURRENT_PERIOD - 1))
                .setRoom("Informática");

        assertFalse(GoogleClassroomService.courseMatchesTeacherCurso(course, List.of(cursoInformatica2doA())));
    }
}
