package ctn.informatica.sca.util;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SignatureImageRegressionTest {

    @Test
    void signatureImageIsPreservedInWorkbook() throws IOException {
        ctn.informatica.sca.model.Planilla planilla = new ctn.informatica.sca.model.Planilla(901, 1, 1, "comun", "Sig", 2026, "primera", 7);
        ctn.informatica.sca.model.Tarea t1 = new ctn.informatica.sca.model.Tarea(); t1.setId(9902); t1.setFecha(java.time.LocalDate.of(2026,3,5)); t1.setTitulo("S1"); t1.setTotal(5);
        ctn.informatica.sca.model.StudentRow sr = new ctn.informatica.sca.model.StudentRow(); sr.setAlumnoId(2); sr.setAlumnoNombre("Tester"); sr.setGrades(java.util.Map.of(9902,5)); sr.setTotal(5);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData dataNoSig = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
            planilla,
            new ctn.informatica.sca.model.Curso(901, "Informática", 2026, "A"),
            "Informática",
            "Profesor",
            "Mañana",
            java.util.List.of(t1),
            java.util.List.of(sr),
            java.util.Map.of(),
            null
        );

        // tiny 1x1 transparent PNG data URL
        final String tinyPngDataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAAWgmWQ0AAAAASUVORK5CYII=";

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData dataWithSig = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
            planilla,
            new ctn.informatica.sca.model.Curso(901, "Informática", 2026, "A"),
            "Informática",
            "Profesor",
            "Mañana",
            java.util.List.of(t1),
            java.util.List.of(sr),
            java.util.Map.of(),
            tinyPngDataUrl
        );

        try (XSSFWorkbook wbNo = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(dataNoSig, "SigNo")) {
            int picsNo = wbNo.getAllPictures().size();
            try (XSSFWorkbook wbYes = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(dataWithSig, "SigYes")) {
                int picsYes = wbYes.getAllPictures().size();
                // The workbook with signature must have at least one more picture than without
                assertTrue(picsYes >= picsNo + 1, "Expected workbook with signature to contain an extra picture: before=" + picsNo + " after=" + picsYes);
            }
        }
    }
}
