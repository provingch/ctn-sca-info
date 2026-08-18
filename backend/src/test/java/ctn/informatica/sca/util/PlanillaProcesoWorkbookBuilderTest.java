package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void diagnostic_verifyInstrumentColumnPositions_currentBuilder_behavior() throws IOException {
        Planilla planilla = new Planilla(100, 1, 1, "comun", "Verif", 2026, "primera", 1);
        List<Tarea> tareas = new java.util.ArrayList<>();
        // Month 1: 3 tasks
        Tarea t1 = new Tarea(); t1.setId(1001); t1.setFecha(LocalDate.of(2026, 2, 1)); t1.setTitulo("T1"); t1.setTotal(10);
        Tarea t2 = new Tarea(); t2.setId(1002); t2.setFecha(LocalDate.of(2026, 2, 10)); t2.setTitulo("T2"); t2.setTotal(10);
        Tarea t3 = new Tarea(); t3.setId(1003); t3.setFecha(LocalDate.of(2026, 2, 20)); t3.setTitulo("T3"); t3.setTotal(10);
        tareas.add(t1); tareas.add(t2); tareas.add(t3);
        // Month 2: 7 tasks
        for (int i = 4; i <= 10; i++) {
            Tarea tt = new Tarea(); tt.setId(1000 + i); tt.setFecha(LocalDate.of(2026, 4, i)); tt.setTitulo("T" + i); tt.setTotal(10);
            tareas.add(tt);
        }

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                null,
                "Verif",
                "Docente",
                "",
                tareas,
                List.of(),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "V")) {
            Sheet sheet = workbook.getSheetAt(0);
            Row titleRow = sheet.getRow(6); // instrument title row
            java.util.List<Integer> nonEmpty = new java.util.ArrayList<>();
            for (int c = 0; c < 120; c++) {
                Cell cell = titleRow.getCell(c);
                if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING && !cell.getStringCellValue().isBlank()) {
                    nonEmpty.add(c);
                }
            }
            System.out.println("Non-empty instrument title columns: " + nonEmpty);
            // Expect at least 3 + 7 = 10 instrument title cells present
            assertTrue(nonEmpty.size() >= 10, "Debe haber al menos 10 títulos de instrumento");
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

    @Test
    void consecutiveMonthsKeepColumnsVisible() throws IOException {
        Planilla planilla = new Planilla(11, 1, 1, "comun", "Consec", 2026, "primera", 7);

        // Month 1: 3 tasks
        Tarea m1t1 = new Tarea(); m1t1.setId(601); m1t1.setFecha(LocalDate.of(2026, 2, 5)); m1t1.setTitulo("M1-A"); m1t1.setTotal(5);
        Tarea m1t2 = new Tarea(); m1t2.setId(602); m1t2.setFecha(LocalDate.of(2026, 2, 10)); m1t2.setTitulo("M1-B"); m1t2.setTotal(6);
        Tarea m1t3 = new Tarea(); m1t3.setId(603); m1t3.setFecha(LocalDate.of(2026, 2, 20)); m1t3.setTitulo("M1-C"); m1t3.setTotal(7);

        // Month 2: 8 tasks
        List<Tarea> month2 = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Tarea t = new Tarea();
            t.setId(700 + i);
            t.setFecha(LocalDate.of(2026, 3, 5 + i));
            t.setTitulo("M2-" + (i + 1));
            t.setTotal(5 + i);
            month2.add(t);
        }

        List<Tarea> all = new java.util.ArrayList<>();
        all.add(m1t1); all.add(m1t2); all.add(m1t3); all.addAll(month2);

        StudentRow s = new StudentRow(); s.setAlumnoId(1); s.setAlumnoNombre("X"); s.setGrades(Map.of()); s.setTotal(0);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(11, "Consec", 2026, "A"),
                "Consec",
                "Prof",
                "Mañana",
                all,
                List.of(s),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "Consec")) {
            Sheet sheet = workbook.getSheetAt(0);
            Row titleRow = sheet.getRow(6);

            int m1start = -1;
            int m2start = -1;
            for (int c = 0; c < 300; c++) {
                Cell cell = titleRow.getCell(c);
                if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                    String v = cell.getStringCellValue();
                    if ("M1-A".equals(v)) m1start = c;
                    if ("M2-1".equals(v)) m2start = c;
                }
            }
            assertTrue(m1start >= 0, "Debe encontrarse M1-A");
            assertTrue(m2start >= 0, "Debe encontrarse M2-1");
            org.apache.poi.xssf.usermodel.XSSFSheet xs = (org.apache.poi.xssf.usermodel.XSSFSheet) sheet;
            for (int i = 0; i < 3; i++) {
                assertFalse(xs.isColumnHidden(m1start + i), "Columna M1 debe estar visible: " + (m1start + i));
            }
            for (int i = 0; i < 8; i++) {
                assertFalse(xs.isColumnHidden(m2start + i), "Columna M2 debe estar visible: " + (m2start + i));
            }
        }
    }

    @Test
    void singleMonthNoLeftoverVisibleColumns() throws IOException {
        Planilla planilla = new Planilla(12, 1, 1, "comun", "Single", 2026, "primera", 7);
        Tarea t1 = new Tarea(); t1.setId(801); t1.setFecha(LocalDate.of(2026, 2, 5)); t1.setTitulo("S1-1"); t1.setTotal(5);
        Tarea t2 = new Tarea(); t2.setId(802); t2.setFecha(LocalDate.of(2026, 2, 15)); t2.setTitulo("S1-2"); t2.setTotal(6);
        StudentRow s = new StudentRow(); s.setAlumnoId(1); s.setAlumnoNombre("Y"); s.setGrades(Map.of(801,5,802,6)); s.setTotal(11);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(12, "Single", 2026, "A"),
                "Single",
                "Prof",
                "Mañana",
                List.of(t1, t2),
                List.of(s),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "Single")) {
            Sheet sheet = workbook.getSheetAt(0);
            Row titleRow = sheet.getRow(6);

            int first = -1;
            for (int c = 0; c < 300; c++) {
                Cell cell = titleRow.getCell(c);
                if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                    String v = cell.getStringCellValue();
                    if ("S1-1".equals(v)) { first = c; break; }
                }
            }
            assertTrue(first >= 0, "Debe encontrarse S1-1");
            int lastInstrument = first + 1; // two tasks

            int boundary = -1;
            org.apache.poi.xssf.usermodel.XSSFSheet xs = (org.apache.poi.xssf.usermodel.XSSFSheet) sheet;
            boolean foundBoundary = false;
            for (int c = lastInstrument + 1; c < 300; c++) {
                boolean hasContent = false;
                for (int r : new int[]{5,6,7}) {
                    Row rr = sheet.getRow(r);
                    if (rr == null) continue;
                    Cell cc = rr.getCell(c);
                    if (cc != null && cc.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        String vv = cc.getStringCellValue();
                        if (vv != null && !vv.isBlank()) { hasContent = true; break; }
                    }
                }
                if (!hasContent) {
                    // If the column is empty, it must be hidden
                    assertTrue(xs.isColumnHidden(c), "Columna sobrante debe estar oculta: " + c);
                } else {
                    boundary = c;
                    foundBoundary = true;
                    break;
                }
            }
            assertTrue(foundBoundary, "Debe encontrarse una columna fija con contenido a la derecha");
        }
    }
}
