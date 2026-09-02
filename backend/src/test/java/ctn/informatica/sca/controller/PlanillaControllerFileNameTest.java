package ctn.informatica.sca.controller;

import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlanillaControllerFileNameTest {

    @Test
    void buildExportBaseFilename_includesMateriaCursoSeccionPeriodo_and_sanitizes() {
        Planilla p = new Planilla(123, 1, 1, "comun", "Algorítmica", 2026, "primera", 7);
        // promocion chosen so that getCursoOrdinal() resolves to "2º"
        Curso c = new Curso(10, "CursoTest", 2027, "A");

        String base = PlanillaController.buildExportBaseFilename(p, c, "Algorítmica");
        // Should contain disciplina (sanitized), curso-seccion (ordinal sanitized), and periodo
        assertTrue(base.contains("Algor_tmica"), "Disciplina debe estar sanitizada en el nombre: " + base);
        assertTrue(base.contains("2_-A"), "Curso-Seccion debe aparecer sanitizado: " + base);
        assertTrue(base.endsWith("_2026"), "Debe terminar con el período: " + base);
    }

    @Test
    void buildExportBaseFilename_handles_nullSeccion_and_usesPlanillaId_whenDisciplinaBlank() {
        Planilla p = new Planilla(999, 1, 1, "comun", "", 2025, "primera", 7);
        // promocion chosen so that getCursoOrdinal() resolves to "3º" (default period assumed)
        Curso c = new Curso(11, "X", 2026, null);

        String base = PlanillaController.buildExportBaseFilename(p, c, "");
        // disciplina blank -> use planilla id
        assertTrue(base.startsWith("Planilla_999_"), "Debe usar planilla id cuando disciplina está vacía: " + base);
        // Curso ordinal sanitized and hyphen present even if section empty
        assertTrue(base.contains("3_-"), "Curso ordinal debe estar presente y sanitizado: " + base);
        assertTrue(base.endsWith("_2025"), "Debe terminar con período: " + base);
    }
}
