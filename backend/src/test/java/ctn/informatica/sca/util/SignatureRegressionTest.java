package ctn.informatica.sca.util;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SignatureRegressionTest {

    @Test
    void signatureImage_shouldBePresent_inWorkbook() throws IOException {
        // minimal planilla
        ctn.informatica.sca.model.Planilla planilla = new ctn.informatica.sca.model.Planilla(1000, 1, 1, "comun", "Sig", 2026, "primera", 7);
        ctn.informatica.sca.model.Tarea t = new ctn.informatica.sca.model.Tarea(); t.setId(2001); t.setFecha(java.time.LocalDate.of(2026,2,5)); t.setTitulo("T"); t.setTotal(5);
        ctn.informatica.sca.model.StudentRow sr = new ctn.informatica.sca.model.StudentRow(); sr.setAlumnoId(1); sr.setAlumnoNombre("S"); sr.setGrades(java.util.Map.of(2001,5)); sr.setTotal(5);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData noSig = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
            planilla,
            new ctn.informatica.sca.model.Curso(1000, "Informática", 2026, "A"),
            "Disc", "Profe", "Mañana",
            java.util.List.of(t), java.util.List.of(sr), java.util.Map.of(), null
        );

        String tinyPngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAAWgmWQ0AAAAASUVORK5CYII=";
        String dataUrl = "data:image/png;base64," + tinyPngBase64;

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData withSig = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
            planilla,
            new ctn.informatica.sca.model.Curso(1000, "Informática", 2026, "A"),
            "Disc", "Profe", "Mañana",
            java.util.List.of(t), java.util.List.of(sr), java.util.Map.of(), dataUrl
        );

        int picsWithout;
        int picsWith;
        try (XSSFWorkbook w1 = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(noSig, "NoSig")) {
            picsWithout = w1.getAllPictures() == null ? 0 : w1.getAllPictures().size();
        }
        try (XSSFWorkbook w2 = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(withSig, "WithSig")) {
            picsWith = w2.getAllPictures() == null ? 0 : w2.getAllPictures().size();
        }

        assertTrue(picsWith > picsWithout, "El workbook con firma debe tener más imágenes que sin firma");
    }
}
