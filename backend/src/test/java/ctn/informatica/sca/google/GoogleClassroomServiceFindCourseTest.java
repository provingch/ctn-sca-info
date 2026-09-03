package ctn.informatica.sca.google;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.services.classroom.model.Course;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.util.AcademicPeriod;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GoogleClassroomServiceFindCourseTest {

    private static final int CURRENT_PERIOD = AcademicPeriod.current();

    private Curso cursoInformatica2doA() {
        return new Curso(1, "Informática", CURRENT_PERIOD + 1, "A");
    }

    private Curso cursoElectricidad2doA() {
        return new Curso(2, "Electricidad", CURRENT_PERIOD + 1, "A");
    }

    @Test
    void eligeLaClaseCorrectaSegunLaSalaEspecialidad() {
        Course c1 = new Course()
                .setId("c-informatica")
                .setName("Matemática 2do A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("Informática");

        Course c2 = new Course()
                .setId("c-electricidad")
                .setName("Matemática 2do A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("Electricidad");

        Planilla p = new Planilla(10, 0, 0, "Matemática", CURRENT_PERIOD, "primera", 0, 0, "");

        Optional<Course> chosenForInformatica = GoogleClassroomService.chooseCourseFromList(List.of(c1, c2), cursoInformatica2doA(), p);
        assertTrue(chosenForInformatica.isPresent());
        assertEquals("c-informatica", chosenForInformatica.get().getId());

        Optional<Course> chosenForElectricidad = GoogleClassroomService.chooseCourseFromList(List.of(c1, c2), cursoElectricidad2doA(), p);
        assertTrue(chosenForElectricidad.isPresent());
        assertEquals("c-electricidad", chosenForElectricidad.get().getId());
    }

    /**
     * Caso real reportado: la profesora Ruth Estigarribia tiene en Google
     * Classroom "Laboratorio Redes", "Algorítmica" y "Orientación" para 3ro A
     * Informática. Las dos primeras son específicas de Informática (su Sala
     * declara la especialidad); "Orientación" es una materia común, cargada
     * en la BD como "Orientación Informática" para esa especialidad puntual,
     * pero la clase real en Classroom se llama simplemente "Orientación" y su
     * Sala no necesariamente declara "Informática". Antes de este fix, la
     * planilla de Orientación nunca detectaba ninguna clase.
     */
    @Test
    void detectaMateriaComunAunqueLaSalaNoDeclareLaEspecialidad() {
        Course laboratorioRedes = new Course()
                .setId("c-lab-redes")
                .setName("Laboratorio Redes 3ro A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("Informática");

        Course algoritmica = new Course()
                .setId("c-algoritmica")
                .setName("Algorítmica 3ro A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("Informática");

        Course orientacion = new Course()
                .setId("c-orientacion")
                .setName("Orientación 3ro A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom(""); // Sala vacía: no declara ninguna especialidad

        Curso curso3roAInformatica = new Curso(1, "Informática", CURRENT_PERIOD, "A");
        Planilla planillaOrientacion = new Planilla(20, 0, 0, "Orientación Informática", CURRENT_PERIOD, "primera", 0, 0, "");

        Optional<Course> chosen = GoogleClassroomService.chooseCourseFromList(
                List.of(laboratorioRedes, algoritmica, orientacion), curso3roAInformatica, planillaOrientacion);

        assertTrue(chosen.isPresent());
        assertEquals("c-orientacion", chosen.get().getId());
    }

    @Test
    void eligeLaClaseCorrectaCuandoHayDosMateriasEspecificasEnElMismoCursoYUnaSalaEstaVacia() {
        Course laboratorioJava = new Course()
                .setId("c-lab-java")
                .setName("Laboratorio Java 3ro A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("Informática");

        Course laboratorioAndroid = new Course()
                .setId("c-lab-android")
                .setName("Laboratorio Android 3ro A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("");

        Curso curso3roAInformatica = new Curso(1, "Informática", CURRENT_PERIOD, "A");

        Planilla planillaJava = new Planilla(30, 0, 0, "Laboratorio Java", CURRENT_PERIOD, "primera", 0, 0, "");
        Optional<Course> chosenJava = GoogleClassroomService.chooseCourseFromList(
                List.of(laboratorioJava, laboratorioAndroid), curso3roAInformatica, planillaJava);
        assertTrue(chosenJava.isPresent());
        assertEquals("c-lab-java", chosenJava.get().getId());

        Planilla planillaAndroid = new Planilla(31, 0, 0, "Laboratorio Android", CURRENT_PERIOD, "primera", 0, 0, "");
        Optional<Course> chosenAndroid = GoogleClassroomService.chooseCourseFromList(
                List.of(laboratorioJava, laboratorioAndroid), curso3roAInformatica, planillaAndroid);
        assertTrue(chosenAndroid.isPresent());
        assertEquals("c-lab-android", chosenAndroid.get().getId());
    }

    @Test
    void ignoraUnCursoGuardadoObsoletoCuandoLaMateriaYaNoCoincide() {
        Course laboratorioRedes = new Course()
                .setId("c-lab-redes")
                .setName("Laboratorio Redes 3ro A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("Informática");

        Course algoritmica = new Course()
                .setId("c-algoritmica")
                .setName("Algorítmica 3ro A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("Informática");

        Course orientacion = new Course()
                .setId("c-orientacion")
                .setName("Orientación 3ro A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("");

        Curso curso3roAInformatica = new Curso(1, "Informática", CURRENT_PERIOD, "A");
        Planilla planillaOrientacion = new Planilla(20, 0, 0, "Orientación Informática", CURRENT_PERIOD, "primera", 0, 0, "");
        List<Course> courses = List.of(laboratorioRedes, algoritmica, orientacion);

        assertEquals("c-orientacion", GoogleClassroomService.chooseCourseFromList(courses, curso3roAInformatica, planillaOrientacion).orElseThrow().getId());
        assertTrue(GoogleClassroomService.isCourseCompatibleWithPlanilla(orientacion, curso3roAInformatica, planillaOrientacion, courses));
        assertTrue(!GoogleClassroomService.isCourseCompatibleWithPlanilla(laboratorioRedes, curso3roAInformatica, planillaOrientacion, courses));
    }

    @Test
    void noAdivinaCuandoSoloExisteOtraMateriaEnClassroom() {
        // Solo existe en Classroom la clase de Laboratorio Java; no hay clase de Laboratorio Android
        Course laboratorioJava = new Course()
                .setId("c-lab-java")
                .setName("Laboratorio Java 3ro A")
                .setSection(String.valueOf(CURRENT_PERIOD))
                .setRoom("Informática");

        Curso curso3roAInformatica = new Curso(1, "Informática", CURRENT_PERIOD, "A");

        Planilla planillaJava = new Planilla(40, 0, 0, "Laboratorio Java", CURRENT_PERIOD, "primera", 0, 0, "");
        Optional<Course> chosenJava = GoogleClassroomService.chooseCourseFromList(
                List.of(laboratorioJava), curso3roAInformatica, planillaJava);
        assertTrue(chosenJava.isPresent());
        assertEquals("c-lab-java", chosenJava.get().getId());

        Planilla planillaAndroid = new Planilla(41, 0, 0, "Laboratorio Android", CURRENT_PERIOD, "primera", 0, 0, "");
        Optional<Course> chosenAndroid = GoogleClassroomService.chooseCourseFromList(
                List.of(laboratorioJava), curso3roAInformatica, planillaAndroid);
        // No debe adivinar: como no existe una clase que coincida por materia, debe devolver vacío
        assertTrue(chosenAndroid.isEmpty());
    }
}