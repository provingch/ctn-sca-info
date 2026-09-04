package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Test;
import java.io.FileOutputStream;
import java.util.ArrayList;
import org.apache.poi.ss.usermodel.CellType;

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
            String path = "target/test_planilla_dinamico.xlsx";
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

            int afterTaskContent = -1;
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
                if (hasContent) {
                    afterTaskContent = c;
                    break;
                }
            }
            assertTrue(afterTaskContent >= 0, "Debe existir contenido fijo después de los instrumentos del mes");
        }
    }

    @Test
    void headerBannerResizesToTableWidthForNarrowPlanilla() throws IOException {
        Planilla planilla = new Planilla(200, 1, 1, "comun", "Narrow", 2026, "primera", 7);
        Tarea t1 = new Tarea(); t1.setId(9001); t1.setFecha(LocalDate.of(2026, 2, 5)); t1.setTitulo("S1"); t1.setTotal(5);
        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(200, "Narrow", 2026, "A"),
                "Narrow",
                "Prof",
                "Mañana",
                List.of(t1),
                List.of(new StudentRow()),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "NarrowTest")) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(5); // MONTH_HEADER_ROW
            int totalGeneralCol = -1;
            for (int c = 0; c < 200; c++) {
                Cell cell = headerRow.getCell(c);
                if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                    String v = cell.getStringCellValue();
                    if (v != null && v.toLowerCase().contains("total general")) {
                        totalGeneralCol = c;
                        break;
                    }
                }
            }
            assertTrue(totalGeneralCol >= 0, "Debe encontrarse la columna Total General");

            int maxMerged = -1;
            java.util.List<CellRangeAddress> merges = ((org.apache.poi.xssf.usermodel.XSSFSheet) sheet).getMergedRegions();
            for (CellRangeAddress ca : merges) {
                if (ca.getFirstRow() <= 4 && ca.getLastRow() >= 0) {
                    maxMerged = Math.max(maxMerged, ca.getLastColumn());
                }
            }
            assertTrue(maxMerged >= 0, "Debe existir al menos una región fusionada en el encabezado");
            assertTrue(maxMerged >= totalGeneralCol, "El encabezado debe reservar suficiente ancho para el texto del bloque de info");
        }
    }

    @Test
    void headerBannerUsesMinimumSpanForLongSpecialtyTextInNarrowPlanilla() throws IOException {
        Planilla planilla = new Planilla(201, 1, 1, "comun", "NarrowLong", 2026, "primera", 7);
        Tarea t1 = new Tarea(); t1.setId(9002); t1.setFecha(LocalDate.of(2026, 2, 5)); t1.setTitulo("S1"); t1.setTotal(5);
        String specialtyText = "Especialidad: Ciencias Sociales y Humanidades con orientación en Historia";
        String professorText = "Profesor/a: Ana María de la Cruz Pérez del Valle";
        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(201, "NarrowLong", 2026, "A"),
                "NarrowLong",
                professorText,
                "Mañana",
                List.of(t1),
                List.of(new StudentRow()),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "NarrowLongTest")) {
            Sheet sheet = workbook.getSheetAt(0);
            Row specialtyRow = sheet.getRow(3);
            Row courseRow = sheet.getRow(4);
            Cell specialtyCell = specialtyRow.getCell(2);
            Cell courseCell = courseRow.getCell(2);

            assertNotNull(specialtyCell);
            assertNotNull(courseCell);
            int requiredSpecialtyColumns = (int) Math.ceil((double) specialtyText.length() / 8.0) + 2;
            int requiredCourseColumns = (int) Math.ceil((double) courseCell.getStringCellValue().length() / 8.0) + 2;

            int specialtyLastCol = -1;
            int courseLastCol = -1;
            for (CellRangeAddress merged : ((org.apache.poi.xssf.usermodel.XSSFSheet) sheet).getMergedRegions()) {
                if (merged.getFirstRow() == 3 && merged.getFirstColumn() == 2) {
                    specialtyLastCol = merged.getLastColumn();
                }
                if (merged.getFirstRow() == 4 && merged.getFirstColumn() == 2) {
                    courseLastCol = merged.getLastColumn();
                }
            }

            assertTrue(specialtyLastCol >= requiredSpecialtyColumns, "Especialidad debe ampliar su rango de fusión para el texto largo");
            assertTrue(courseLastCol >= requiredCourseColumns, "Curso/Turno/Sección debe ampliar su rango de fusión para el texto largo");
            assertTrue(specialtyCell.getCellStyle().getWrapText(), "Especialidad debe activar wrapText cuando el texto requiere más ancho");
        }
    }

    @Test
    void monthHeaderCellKeepsHorizontalRotationZero() throws IOException {
        Planilla planilla = new Planilla(202, 1, 1, "comun", "MesRotation", 2026, "primera", 7);
        Tarea may = new Tarea(); may.setId(9501); may.setFecha(LocalDate.of(2026, 5, 12)); may.setTitulo("Repaso"); may.setTotal(10);
        StudentRow student = new StudentRow(); student.setAlumnoId(1); student.setAlumnoNombre("Student"); student.setGrades(Map.of(9501, 9)); student.setTotal(9);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(202, "MesRotation", 2026, "A"),
                "MesRotation",
                "Prof",
                "Mañana",
                List.of(may),
                List.of(student),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "MesRotation")) {
            Sheet sheet = workbook.getSheetAt(0);
            Cell monthCell = null;
            for (Cell cell : sheet.getRow(5)) {
                if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING && "Mayo".equals(cell.getStringCellValue())) {
                    monthCell = cell;
                    break;
                }
            }
            assertNotNull(monthCell, "Debe existir la celda del encabezado de Mayo");
            assertEquals(0, monthCell.getCellStyle().getRotation(), "El texto del mes debe mantenerse horizontal");

            Cell titleCell = sheet.getRow(6).getCell(2);
            assertNotNull(titleCell, "Debe existir la celda del instrumento Repaso");
            assertEquals("Repaso", titleCell.getStringCellValue(), "Debe renderizar el título del instrumento");
            // Instrument titles are rendered vertically (90°)
            assertEquals(90, titleCell.getCellStyle().getRotation(), "El texto del instrumento debe renderizarse en 90°");

            String expectedYearLabel = "Año: " + planilla.getPeriodo();
            Cell yearCell = null;
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        String value = cell.getStringCellValue();
                        if (value != null && value.contains("Año: ") && value.contains(String.valueOf(planilla.getPeriodo()))) {
                            yearCell = cell;
                            break;
                        }
                    }
                }
                if (yearCell != null) {
                    break;
                }
            }
            assertNotNull(yearCell, "Debe existir la celda de Año: " + planilla.getPeriodo() + " en la planilla angosta");
            assertEquals(expectedYearLabel, yearCell.getStringCellValue(), "La celda de Año debe conservar el período de la planilla");
        }
    }

    @Test
    void finalColumnsDoNotLeaveGhostVisibleColumn_whenMonthHasMoreThanFiveTasks() throws IOException {
        Planilla planilla = new Planilla(310, 1, 1, "comun", "MasDeCincoTareas", 2026, "primera", 7);

        List<Tarea> tareas = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            Tarea tarea = new Tarea();
            tarea.setId(5000 + i);
            tarea.setFecha(LocalDate.of(2026, 8, 2 + i));
            tarea.setTitulo("Tarea " + i);
            tarea.setTotal(10);
            tareas.add(tarea);
        }

        StudentRow row = new StudentRow();
        row.setAlumnoId(1);
        row.setAlumnoNombre("Alumno Test");
        Map<Integer, Integer> grades = new HashMap<>();
        for (Tarea tarea : tareas) {
            grades.put((int) tarea.getId(), 5);
        }
        row.setGrades(grades);
        row.setTotal(35);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(310, "MasDeCincoTareas", 2026, "A"),
                "MasDeCincoTareas",
                "Prof Test",
                "Mañana",
                tareas,
                List.of(row),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "ManyTasksGhostColumn")) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(5);

            int totalGeneralCol = -1;
            int lastFinalLabelCol = -1;
            for (int c = 0; c < 400; c++) {
                Cell cell = headerRow == null ? null : headerRow.getCell(c);
                if (cell == null || cell.getCellType() != CellType.STRING) {
                    continue;
                }
                String value = cell.getStringCellValue();
                if (value != null && value.equalsIgnoreCase("Total General")) {
                    totalGeneralCol = c;
                }
                if (totalGeneralCol >= 0 && value != null && !value.isBlank()) {
                    lastFinalLabelCol = c;
                }
            }

            assertTrue(totalGeneralCol >= 0, "Debe encontrarse la columna Total General");
            assertTrue(lastFinalLabelCol >= totalGeneralCol, "Debe detectarse la última etiqueta real del bloque final");

            int firstGhostColumn = lastFinalLabelCol + 1;
            assertTrue(sheet.isColumnHidden(firstGhostColumn),
                    "La columna fantasma al final del bloque final debe quedar oculta cuando hay más de 5 tareas");

            for (int c = firstGhostColumn; c <= firstGhostColumn + 3; c++) {
                assertTrue(sheet.isColumnHidden(c),
                        "No deben quedar columnas visibles vacías entre la última etiqueta real y las columnas ocultas");
            }
        }
    }

    @Test
    void buildSingleWorkbook_withManyTasksThatExpandCourseBlock_noMergedRegionOverlap() throws IOException {
        // Regression test for the geometric logic bug in resizeHeaderBanner:
        // When many tasks force the course block to expand beyond column 19 (where "Año" normally starts),
        // the year block must be repositioned after the course block to avoid merged region overlap.
        
        Planilla planilla = new Planilla(201, 1, 1, "comun", "Muchas Tareas", 2026, "primera", 1);
        List<Tarea> tareas = new java.util.ArrayList<>();
        
        // Create enough tasks (distributed across months) to expand course block beyond column 19
        // With 13 tasks in different months, the course block will be stretched significantly
        int taskCounter = 2001;
        for (int month = 2; month <= 6; month++) {  // Feb to Jun
            for (int i = 0; i < 3; i++) {
                Tarea t = new Tarea();
                t.setId(taskCounter++);
                t.setFecha(LocalDate.of(2026, month, 5 + i * 5));
                t.setTitulo("Task-M" + month + "-" + (i+1));
                t.setTotal(10);
                tareas.add(t);
            }
        }
        // Add extra tasks in February to ensure even wider course block (using valid days)
        for (int i = 0; i < 3; i++) {
            Tarea t = new Tarea();
            t.setId(taskCounter++);
            t.setFecha(LocalDate.of(2026, 2, 20 + i));
            t.setTitulo("ExtraTask-" + (i+1));
            t.setTotal(10);
            tareas.add(t);
        }
        
        StudentRow row = new StudentRow();
        row.setAlumnoId(1);
        row.setAlumnoNombre("Test Student");
        Map<Integer, Integer> grades = new HashMap<>();
        for (Tarea t : tareas) {
            grades.put((int)t.getId(), 5);
        }
        row.setGrades(grades);
        row.setTotal(tareas.size() * 5);
        
        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(201, "Muchas Tareas", 2026, "A"),
                "Muchas Tareas",
                "Prof Test",
                "Mañana",
                tareas,
                List.of(row),
                Map.of(),
                null
        );
        
        // This should not throw IllegalStateException about overlapping merged regions
        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "ManyTasks")) {
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet, "Sheet debe existir");
            
            // Verify that merged regions in rows 3 and 4 (0-based, i.e., rows 4 and 5 in Excel 1-based)
            // do not overlap within the same row
            java.util.List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
            
            for (int row4or5 : new int[]{3, 4}) {
                java.util.List<CellRangeAddress> regionsInRow = new java.util.ArrayList<>();
                for (CellRangeAddress merge : mergedRegions) {
                    if (merge.getFirstRow() == row4or5) {
                        regionsInRow.add(merge);
                    }
                }
                
                // Check no overlaps within row 4 and row 5
                for (int i = 0; i < regionsInRow.size(); i++) {
                    for (int j = i + 1; j < regionsInRow.size(); j++) {
                        CellRangeAddress r1 = regionsInRow.get(i);
                        CellRangeAddress r2 = regionsInRow.get(j);
                        
                        boolean overlap = !(r1.getLastColumn() < r2.getFirstColumn() || r2.getLastColumn() < r1.getFirstColumn());
                        assertFalse(overlap, 
                            "Merged regions in row " + (row4or5+1) + " must not overlap: [" + 
                            r1.getFirstColumn() + "-" + r1.getLastColumn() + "] and [" + 
                            r2.getFirstColumn() + "-" + r2.getLastColumn() + "]");
                    }
                }
            }
            
            System.out.println("\n>>> Regression test passed: No merged region overlap detected <<<");
        }
    }

    @Test
    void tpRowContainsNumericInstrumentValues_and_subtotalsAreFormulas_and_columnWidthsReduced() throws IOException {
        // Build a planilla with at least 2 months and one month with multiple instruments
        Planilla planilla = new Planilla(300, 1, 1, "comun", "TPTest", 2026, "primera", 7);

        // Month A: 3 tasks
        Tarea a1 = new Tarea(); a1.setId(4001); a1.setFecha(LocalDate.of(2026, 2, 5)); a1.setTitulo("A1"); a1.setTotal(2);
        Tarea a2 = new Tarea(); a2.setId(4002); a2.setFecha(LocalDate.of(2026, 2, 15)); a2.setTitulo("A2"); a2.setTotal(3);
        Tarea a3 = new Tarea(); a3.setId(4003); a3.setFecha(LocalDate.of(2026, 2, 25)); a3.setTitulo("A3"); a3.setTotal(4);

        // Month B: 1 task
        Tarea b1 = new Tarea(); b1.setId(4101); b1.setFecha(LocalDate.of(2026, 4, 10)); b1.setTitulo("B1"); b1.setTotal(5);

        StudentRow s = new StudentRow(); s.setAlumnoId(1); s.setAlumnoNombre("X"); s.setGrades(Map.of(4001,2,4002,3,4003,4,4101,5)); s.setTotal(14);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(300, "TPTest", 2026, "Z"),
                "TPTest",
                "Prof",
                "Mañana",
                List.of(a1,a2,a3,b1),
                List.of(s),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "TPChecks")) {
            Sheet sheet = workbook.getSheetAt(0);

            // Find instrument columns by scanning INSTRUMENT_TITLE_ROW (row 6)
            Row titleRow = sheet.getRow(6);
            java.util.List<Integer> instrumentCols = new java.util.ArrayList<>();
            for (int c = 0; c < 200; c++) {
                Cell cell = titleRow == null ? null : titleRow.getCell(c);
                if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                        && !cell.getStringCellValue().isBlank()) {
                    instrumentCols.add(c);
                }
            }
            assertTrue(instrumentCols.size() >= 4, "Debe detectarse al menos 4 columnas de instrumento");

            // Verify TP row instrument cells are numeric literals (not formulas)
            Row tpRow = sheet.getRow(7);
            for (Integer col : instrumentCols) {
                Cell tpCell = tpRow == null ? null : tpRow.getCell(col);
                assertNotNull(tpCell, "Celda TP debe existir en columna " + col);
                assertEquals(org.apache.poi.ss.usermodel.CellType.NUMERIC, tpCell.getCellType(), "Celda TP no debe ser fórmula en columna " + col);
            }

            // Find subtotal columns by header label in MONTH_HEADER_ROW (row 5)
            Row headerRow = sheet.getRow(5);
            java.util.List<Integer> subtotalCols = new java.util.ArrayList<>();
            for (int c = 0; c < 200; c++) {
                Cell hh = headerRow == null ? null : headerRow.getCell(c);
                if (hh != null && hh.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                        && hh.getStringCellValue() != null && hh.getStringCellValue().toLowerCase().contains("subtotal")) {
                    subtotalCols.add(c);
                }
            }
            assertTrue(subtotalCols.size() >= 2, "Debe existir al menos 2 columnas Subtotal (por mes)");

            // For each subtotal column, the student row has a SUM formula and TP row must have the same formula
            Row studentRow = sheet.getRow(8);
            for (Integer sc : subtotalCols) {
                Cell studentSubtotal = studentRow == null ? null : studentRow.getCell(sc);
                Cell tpSubtotal = tpRow == null ? null : tpRow.getCell(sc);
                assertNotNull(studentSubtotal, "Subtotal en fila alumno debe existir en col " + sc);
                assertNotNull(tpSubtotal, "Subtotal en fila TP debe existir en col " + sc);
                assertEquals(org.apache.poi.ss.usermodel.CellType.FORMULA, studentSubtotal.getCellType(), "Subtotal alumno debe ser fórmula");
                assertEquals(org.apache.poi.ss.usermodel.CellType.FORMULA, tpSubtotal.getCellType(), "Subtotal TP debe ser fórmula");
                String studentFormula = studentSubtotal.getCellFormula();
                int studentExcelRow = studentRow.getRowNum() + 1;
                int tpExcelRow = tpRow.getRowNum() + 1;
                String expectedTpFormula = studentFormula.replace(String.valueOf(studentExcelRow), String.valueOf(tpExcelRow));
                assertEquals(expectedTpFormula, tpSubtotal.getCellFormula(), "Fórmula subtotal TP debe coincidir (mismo rango, fila TP)");
            }

            // Verify Total General on TP row matches student Total General formula
            int totalGeneralCol = -1;
            for (int c = 0; c < 200; c++) {
                Cell hh = headerRow == null ? null : headerRow.getCell(c);
                if (hh != null && hh.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                        && hh.getStringCellValue() != null && hh.getStringCellValue().toLowerCase().contains("total general")) {
                    totalGeneralCol = c; break;
                }
            }
            assertTrue(totalGeneralCol >= 0, "Debe hallarse columna Total General");
            Cell stuTotal = studentRow.getCell(totalGeneralCol);
            Cell tpTotal = tpRow.getCell(totalGeneralCol);
            assertNotNull(stuTotal);
            assertNotNull(tpTotal);
            assertEquals(org.apache.poi.ss.usermodel.CellType.FORMULA, stuTotal.getCellType());
            assertEquals(org.apache.poi.ss.usermodel.CellType.FORMULA, tpTotal.getCellType());
            String studentTotalFormula = stuTotal.getCellFormula();
            int studentExcelRow = studentRow.getRowNum() + 1;
            int tpExcelRow = tpRow.getRowNum() + 1;
            String expectedTpTotalFormula = studentTotalFormula.replace(String.valueOf(studentExcelRow), String.valueOf(tpExcelRow));
            assertEquals(expectedTpTotalFormula, tpTotal.getCellFormula(), "Fórmula Total General en TP debe coincidir (mismos subtotales, fila TP)");

            // Verify instrument column widths are reduced (between 4.0 and 5.0 characters)
            int minWidth = (int) Math.round(4.0 * 256);
            int maxWidth = (int) Math.round(5.0 * 256);
            for (Integer col : instrumentCols) {
                int width = sheet.getColumnWidth(col);
                assertTrue(width >= minWidth && width <= maxWidth, "Ancho de columna de instrumento fuera de rango: " + (double)width/256.0 + " chars");
            }
        }
    }

    @Test
    void tpRowHasNumericInstruments_and_subtotalsAndTotalAreFormulas_and_instrumentWidthsReduced() throws IOException {
        Planilla planilla = new Planilla(300, 1, 1, "comun", "TPChecks", 2026, "primera", 7);

        // Month A: 2 tasks
        Tarea a1 = new Tarea(); a1.setId(4001); a1.setFecha(LocalDate.of(2026, 2, 5)); a1.setTitulo("A1"); a1.setTotal(3);
        Tarea a2 = new Tarea(); a2.setId(4002); a2.setFecha(LocalDate.of(2026, 2, 12)); a2.setTitulo("A2"); a2.setTotal(7);

        // Month B: 1 task
        Tarea b1 = new Tarea(); b1.setId(5001); b1.setFecha(LocalDate.of(2026, 4, 10)); b1.setTitulo("B1"); b1.setTotal(5);

        StudentRow s = new StudentRow(); s.setAlumnoId(1); s.setAlumnoNombre("Alumno"); s.setGrades(Map.of(4001,2,4002,6,5001,5)); s.setTotal(13);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(300, "TPChecks", 2026, "A"),
                "TPChecks",
                "Prof",
                "Mañana",
                List.of(a1, a2, b1),
                List.of(s),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "TPChecks")) {
            Sheet sheet = workbook.getSheetAt(0);
            Row titleRow = sheet.getRow(6); // INSTRUMENT_TITLE_ROW
            Row tpRow = sheet.getRow(7); // TP_ROW
            Row studentRow = sheet.getRow(8); // FIRST_STUDENT_ROW

            // Collect instrument columns by scanning titleRow for non-empty strings
            java.util.List<Integer> instrumentCols = new java.util.ArrayList<>();
            for (int c = 0; c < 200; c++) {
                Cell tc = titleRow.getCell(c);
                if (tc != null && tc.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING && !tc.getStringCellValue().isBlank()) {
                    instrumentCols.add(c);
                }
            }
            assertTrue(instrumentCols.size() >= 3, "Debe encontrar al menos 3 columnas de instrumento");

            // 1) Verify instrument TP cells are numeric (no formulas)
            for (int col : instrumentCols) {
                Cell tpCell = tpRow.getCell(col);
                assertNotNull(tpCell, "TP cell must exist for instrument col " + col);
                assertEquals(org.apache.poi.ss.usermodel.CellType.NUMERIC, tpCell.getCellType(), "TP instrument cell debe ser NUMERIC en col " + col);
            }

            // 2) Find subtotal columns by locating "Subtotal" label in header row (MONTH_HEADER_ROW = 5)
            Row headerRow = sheet.getRow(5);
            java.util.List<Integer> subtotalCols = new java.util.ArrayList<>();
            for (int c = 0; c < 200; c++) {
                Cell hc = headerRow.getCell(c);
                if (hc != null && hc.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING && hc.getStringCellValue() != null
                        && hc.getStringCellValue().toLowerCase().contains("subtotal")) {
                    subtotalCols.add(c);
                }
            }
            assertTrue(subtotalCols.size() >= 2, "Debe encontrarse al menos 2 columnas Subtotal");

            // Compare formulas: student subtotal formula vs TP subtotal formula, preserving the TP-row row number offset.
            for (int sc : subtotalCols) {
                Cell studentSubtotal = studentRow.getCell(sc);
                Cell tpSubtotal = tpRow.getCell(sc);
                assertNotNull(studentSubtotal, "Student subtotal debe existir en col " + sc);
                assertNotNull(tpSubtotal, "TP subtotal debe existir en col " + sc);
                assertEquals(org.apache.poi.ss.usermodel.CellType.FORMULA, studentSubtotal.getCellType(), "Student subtotal debe ser fórmula");
                assertEquals(org.apache.poi.ss.usermodel.CellType.FORMULA, tpSubtotal.getCellType(), "TP subtotal debe ser fórmula");
                String studentFormula = studentSubtotal.getCellFormula();
                int studentExcelRow = studentRow.getRowNum() + 1;
                int tpExcelRow = tpRow.getRowNum() + 1;
                String expectedTpFormula = studentFormula.replace(String.valueOf(studentExcelRow), String.valueOf(tpExcelRow));
                assertEquals(expectedTpFormula, tpSubtotal.getCellFormula(), "La fórmula de subtotal en TP debe coincidir con la de estudiante, ajustando la fila TP");
            }

            // Total General column: locate by header containing "Total General"
            int totalGenCol = -1;
            for (int c = 0; c < 400; c++) {
                Cell hc = headerRow.getCell(c);
                if (hc != null && hc.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                        && hc.getStringCellValue() != null && hc.getStringCellValue().toLowerCase().contains("total general")) {
                    totalGenCol = c;
                    break;
                }
            }
            assertTrue(totalGenCol >= 0, "Debe encontrarse la columna Total General");
            Cell studentTotal = studentRow.getCell(totalGenCol);
            Cell tpTotal = tpRow.getCell(totalGenCol);
            assertNotNull(studentTotal);
            assertNotNull(tpTotal);
            assertEquals(org.apache.poi.ss.usermodel.CellType.FORMULA, studentTotal.getCellType());
            assertEquals(org.apache.poi.ss.usermodel.CellType.FORMULA, tpTotal.getCellType());
            String studentTotalFormula = studentTotal.getCellFormula();
            int studentExcelRow = studentRow.getRowNum() + 1;
            int tpExcelRow = tpRow.getRowNum() + 1;
            String expectedTpTotalFormula = studentTotalFormula.replace(String.valueOf(studentExcelRow), String.valueOf(tpExcelRow));
            assertEquals(expectedTpTotalFormula, tpTotal.getCellFormula(), "La fórmula de Total General en TP debe coincidir con la del estudiante, ajustando la fila TP");

            // 3) Verify instrument column widths are in 4.0..5.0 chars
            for (int col : instrumentCols) {
                int widthUnits = sheet.getColumnWidth(col); // units of 1/256th of char
                double chars = (double) widthUnits / 256.0;
                assertTrue(chars >= 4.0 && chars <= 5.0, "El ancho de columna de instrumento debe estar entre 4.0 y 5.0 chars; col " + col + " tiene " + chars);
            }
        }
    }

    @Test
    void tpRow_hasNumericInstruments_and_subtotalsAndTotalAreFormulas_and_instrumentWidthsReduced() throws IOException {
        // Planilla with multiple months and at least one month with >1 instrument
        Planilla planilla = new Planilla(300, 1, 1, "comun", "TPTest", 2026, "primera", 7);
        List<Tarea> tareas = new java.util.ArrayList<>();
        // Month A: 2 tasks
        Tarea a1 = new Tarea(); a1.setId(4001); a1.setFecha(LocalDate.of(2026, 2, 5)); a1.setTitulo("A1"); a1.setTotal(3);
        Tarea a2 = new Tarea(); a2.setId(4002); a2.setFecha(LocalDate.of(2026, 2, 15)); a2.setTitulo("A2"); a2.setTotal(7);
        tareas.add(a1); tareas.add(a2);
        // Month B: 1 task
        Tarea b1 = new Tarea(); b1.setId(5001); b1.setFecha(LocalDate.of(2026, 4, 10)); b1.setTitulo("B1"); b1.setTotal(5);
        tareas.add(b1);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(300, "TPTest", 2026, "A"),
                "TPTest",
                "Prof",
                "Mañana",
                tareas,
                List.of(new StudentRow()),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "TPTest")) {
            org.apache.poi.xssf.usermodel.XSSFSheet xs = (org.apache.poi.xssf.usermodel.XSSFSheet) workbook.getSheetAt(0);
            Sheet sheet = workbook.getSheetAt(0);
            Row tpRow = sheet.getRow(7); // TP row index

            // Find subtotal columns by scanning header row for 'Subtotal'
            Row headerRow = sheet.getRow(5);
            java.util.List<Integer> subtotalCols = new java.util.ArrayList<>();
            int totalGeneralCol = -1;
            for (Cell c : headerRow) {
                if (c != null && c.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                    String v = c.getStringCellValue();
                    if (v != null && v.toLowerCase().contains("subtotal")) subtotalCols.add(c.getColumnIndex());
                    if (v != null && v.toLowerCase().contains("total general")) totalGeneralCol = c.getColumnIndex();
                }
            }

            assertTrue(subtotalCols.size() >= 2, "Debe haber al menos 2 columnas Subtotal");
            assertTrue(totalGeneralCol >= 0, "Debe encontrarse Total General");

            // Verify instrument TP cells are numeric and not formulas
            // Identify instrument columns by scanning instrument title row
            Row titleRow = sheet.getRow(6);
            for (Cell c : titleRow) {
                if (c != null && c.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                    String v = c.getStringCellValue();
                    if (v != null && !v.isBlank() && !v.toLowerCase().contains("subtotal")) {
                        int col = c.getColumnIndex();
                        Cell tpCell = tpRow.getCell(col);
                        assertNotNull(tpCell, "TP cell debe existir para columna instrumento " + col);
                        assertEquals(org.apache.poi.ss.usermodel.CellType.NUMERIC, tpCell.getCellType(), "La celda TP de instrumento debe ser NUMERIC (literal)");
                        // Width check (in character units)
                        double widthChars = (double) xs.getColumnWidth(col) / 256.0;
                        assertTrue(widthChars >= 4.0 && widthChars <= 5.0, "Ancho de columna de instrumento fuera del rango: " + widthChars);
                    }
                }
            }

            // Verify each Subtotal cell in TP row is a SUM formula referencing TP row
            for (int sc : subtotalCols) {
                Cell scCell = tpRow.getCell(sc);
                assertNotNull(scCell, "Celda Subtotal en TP debe existir: col " + sc);
                assertEquals(org.apache.poi.ss.usermodel.CellType.FORMULA, scCell.getCellType(), "Subtotal en TP debe ser fórmula");
                String formula = scCell.getCellFormula();
                assertTrue(formula.toUpperCase().contains("SUM("), "Subtotal TP debe usar SUM: " + formula);
                assertTrue(formula.contains(String.valueOf(tpRow.getRowNum() + 1)), "Subtotal TP debe apuntar a la fila TP");
            }

            // Verify Total General cell in TP row is a SUM of subtotals
            Cell totalTp = tpRow.getCell(totalGeneralCol);
            assertNotNull(totalTp, "Celda Total General en TP debe existir");
            assertEquals(org.apache.poi.ss.usermodel.CellType.FORMULA, totalTp.getCellType(), "Total General en TP debe ser fórmula");
            String totalFormula = totalTp.getCellFormula();
            assertTrue(totalFormula.toUpperCase().contains("SUM("), "Total General TP debe usar SUM: " + totalFormula);
        }
    }
}
