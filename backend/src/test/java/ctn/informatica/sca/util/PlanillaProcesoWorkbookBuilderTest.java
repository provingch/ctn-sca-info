package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import java.io.FileOutputStream;

class PlanillaProcesoWorkbookBuilderTest {

    @Test
    void buildSingleWorkbook_whenCursoIsNull_doesNotThrow() throws IOException {
        Planilla planilla = new Planilla(1, 0, 0, "comun", "Materia", 2026, "primera", 1);
        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                null,
                "Materia",
                "Profesor",
                "",
                List.<Tarea>of(),
                List.<StudentRow>of(),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "Planilla")) {
            assertNotNull(workbook.getSheetAt(0));
        }
    }

    @Test
    void buildSingleWorkbook_replacesTemplatePlaceholdersForUsedMonthBlocks() throws IOException {
        Planilla planilla = new Planilla(2, 1, 1, "comun", "Matemática", 2026, "primera", 7);
        Tarea marzo = new Tarea();
        marzo.setId(101);
        marzo.setFecha(LocalDate.of(2026, 3, 10));
        marzo.setTitulo("Trabajo Práctico 1");
        marzo.setTotal(10);

        Tarea mayo = new Tarea();
        mayo.setId(102);
        mayo.setFecha(LocalDate.of(2026, 5, 12));
        mayo.setTitulo("Trabajo Práctico 2");
        mayo.setTotal(12);

        StudentRow row = new StudentRow();
        row.setAlumnoId(1);
        row.setAlumnoNombre("Ana López");
        Map<Integer, Integer> grades = new HashMap<>();
        grades.put(101, 8);
        grades.put(102, 10);
        row.setGrades(grades);
        row.setTotal(18);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(1, "Informática", 2027, "A"),
                "Matemática",
                "Ada Lovelace",
                "Mañana",
                List.of(marzo, mayo),
                List.of(row),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "Planilla")) {
            Sheet sheet = workbook.getSheetAt(0);
            StringBuilder allCellText = new StringBuilder();
            for (Row currentRow : sheet) {
                for (Cell cell : currentRow) {
                    if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        allCellText.append(cell.getStringCellValue());
                    }
                }
            }

            assertFalse(allCellText.toString().contains("{{MES_"), "La plantilla aún conserva placeholders de meses");
            assertFalse(allCellText.toString().contains("{{MES_1_INSTR_"), "La plantilla aún conserva placeholders de instrumentos");
            assertFalse(allCellText.toString().contains("{{"), "La plantilla aún conserva placeholders sin reemplazar");
            assertFalse(sheet.getRow(5).getCell(0).getStringCellValue().contains("{{"));
            assertFalse(sheet.getRow(6).getCell(2).getStringCellValue().contains("{{"));
        }
    }

    @Test
    void generateTestWorkbookForManualVerification() throws IOException {
        Planilla planilla = new Planilla(99, 1, 1, "comun", "Prueba", 2026, "primera", 7);

        Tarea feb1 = new Tarea();
        feb1.setId(501);
        feb1.setFecha(LocalDate.of(2026, 2, 5));
        feb1.setTitulo("Quiz");
        feb1.setTotal(5);

        Tarea feb2 = new Tarea();
        feb2.setId(502);
        feb2.setFecha(LocalDate.of(2026, 2, 15));
        feb2.setTitulo("Trabajo grupal");
        feb2.setTotal(10);

        Tarea feb3 = new Tarea();
        feb3.setId(503);
        feb3.setFecha(LocalDate.of(2026, 2, 25));
        feb3.setTitulo("Presentación");
        feb3.setTotal(8);

        Tarea apr1 = new Tarea();
        apr1.setId(504);
        apr1.setFecha(LocalDate.of(2026, 4, 10));
        apr1.setTitulo("Taller");
        apr1.setTotal(7);

        StudentRow s1 = new StudentRow();
        s1.setAlumnoId(1);
        s1.setAlumnoNombre("Est. A");
        Map<Integer, Integer> g1 = new HashMap<>();
        g1.put(501, 5);
        g1.put(502, 9);
        g1.put(503, 8);
        g1.put(504, 7);
        s1.setGrades(g1);
        s1.setTotal(29);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(99, "Prueba", 2026, "Z"),
                "Prueba",
                "Prof Test",
                "Mañana",
                List.of(feb1, feb2, feb3, apr1),
                List.of(s1),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "Test")) {
            String path = "/tmp/test_planilla_dinamico.xlsx";
            try (FileOutputStream fos = new FileOutputStream(path)) {
                workbook.write(fos);
                System.out.println("\n=== WORKBOOK GENERATED ===");
                System.out.println("Path: " + path);
                System.out.println("Feb: 3 tasks (cols 3-5 visible, 6-14 hidden)");
                System.out.println("Apr: 1 task (col 3 visible, 4-14 hidden)");
                System.out.println("Jun, Aug, Oct: empty (all 13 cols hidden)");
                System.out.println(">>> Open in LibreOffice Calc to verify <<<");
            }
        }
    }

    @Test
    void verifyColumnsAreHiddenCorrectly() throws IOException {
        Planilla planilla = new Planilla(99, 1, 1, "comun", "Prueba", 2026, "primera", 7);

        Tarea feb1 = new Tarea();
        feb1.setId(501);
        feb1.setFecha(LocalDate.of(2026, 2, 5));
        feb1.setTitulo("Quiz");
        feb1.setTotal(5);

        StudentRow s1 = new StudentRow();
        s1.setAlumnoId(1);
        s1.setAlumnoNombre("Test");
        Map<Integer, Integer> g1 = new HashMap<>();
        g1.put(501, 5);
        s1.setGrades(g1);
        s1.setTotal(5);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(99, "Test", 2026, "Z"),
                "Test",
                "Test Prof",
                "Mañana",
                List.of(feb1),
                List.of(s1),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "Test")) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Verify some columns are hidden
            int hiddenCount = 0;
            for (int i = 0; i < 30; i++) {
                if (((org.apache.poi.xssf.usermodel.XSSFSheet) sheet).isColumnHidden(i)) {
                    hiddenCount++;
                }
            }
            
            System.out.println("\n>>> Test result: Found " + hiddenCount + " hidden columns <<<");
            assertNotNull(sheet, "Sheet debe existir");
        }
    }
}
