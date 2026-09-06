package ctn.informatica.sca.util;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EmbeddedImageDiagnosticTest {

    @Test
    void diagnoseEmbeddedImages() throws IOException {
        // Build a minimal planilla similar to other tests
        ctn.informatica.sca.model.Planilla planilla = new ctn.informatica.sca.model.Planilla(900, 1, 1, "comun", "Diag", 2026, "primera", 7);
        ctn.informatica.sca.model.Tarea t1 = new ctn.informatica.sca.model.Tarea(); t1.setId(9901); t1.setFecha(java.time.LocalDate.of(2026,2,5)); t1.setTitulo("S1"); t1.setTotal(5);
        ctn.informatica.sca.model.StudentRow sr = new ctn.informatica.sca.model.StudentRow(); sr.setAlumnoId(1); sr.setAlumnoNombre("Tester"); sr.setGrades(java.util.Map.of(9901,5)); sr.setTotal(5);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
            planilla,
            new ctn.informatica.sca.model.Curso(900, "Diag", 2026, "A"),
            "Diag",
            "Profesor",
            "Mañana",
            java.util.List.of(t1),
            java.util.List.of(sr),
            java.util.Map.of(),
            null
        );
        try (XSSFWorkbook wb = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "Diag")) {
            int poiPics = wb.getAllPictures().size();
            System.out.println("POI workbook.getAllPictures().size() = " + poiPics);

            // write out to disk and inspect zip/xl/media contents
            File out = new File("target/diag_planilla_images.xlsx");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                wb.write(fos);
            }

            try (ZipFile zf = new ZipFile(out)) {
                int mediaCount = 0;
                System.out.println("Listing xl/media/ entries:");
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry ze = entries.nextElement();
                    String name = ze.getName();
                    if (name.startsWith("xl/media/")) {
                        mediaCount++;
                        System.out.println("  " + name);
                    }
                }
                System.out.println("zip xl/media/ count = " + mediaCount);
            }
        }
    }
}
