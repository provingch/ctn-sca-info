package ctn.informatica.sca.util;

import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import java.io.FileInputStream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportGeneratorTest {

    @Test
    public void generateXlsx() throws Exception {
        Planilla planilla = new Planilla();
        planilla.setId(1);
        planilla.setNombre("Planilla Prueba");
        planilla.setPeriodo(2026);
        planilla.setEtapa("primera");

        Curso curso = new Curso(1, "Informatica", 2026, "A");

        List<Tarea> tareas = new ArrayList<>();
        // two tasks in same month, one in next month
        tareas.add(new Tarea(101, 1, 1, LocalDate.of(2026, 5, 10), 10, "T1"));
        tareas.add(new Tarea(102, 1, 2, LocalDate.of(2026, 5, 20), 15, "T2"));
        tareas.add(new Tarea(103, 1, 3, LocalDate.of(2026, 6, 5), 20, "T3"));

        List<StudentRow> rows = new ArrayList<>();
        StudentRow s1 = new StudentRow(1, 1001, "Perez, Juan");
        Map<Integer,Integer> grades1 = new HashMap<>();
        grades1.put(101, 8);
        grades1.put(102, 12);
        grades1.put(103, 18);
        s1.setGrades(grades1);
        s1.setTotal(38);
        rows.add(s1);

        StudentRow s2 = new StudentRow(2, 1002, "Gomez, Ana");
        Map<Integer,Integer> grades2 = new HashMap<>();
        grades2.put(101, 10);
        grades2.put(102, 15);
        grades2.put(103, 20);
        s2.setGrades(grades2);
        s2.setTotal(45);
        rows.add(s2);

        Map<Integer,Integer> firstStageGrades = new HashMap<>();

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                curso,
                "Programación",
                "Profesor X",
                "Mañana",
                tareas,
                rows,
                firstStageGrades,
                null
        );

        PlanillaProcesoWorkbookBuilder builder = new PlanillaProcesoWorkbookBuilder();
        XSSFWorkbook wb = builder.buildSingleWorkbook(data, "Planilla-Prueba");

        try (FileOutputStream out = new FileOutputStream("target/planilla-test.xlsx")) {
            wb.write(out);
        }
        wb.close();

        // Verify no residual template content exists beyond the last real column
        // Recompute the computed layout's last column (regularizationColumn)
        int firstMonthColumn = 2; // matches STAGE_1 in builder
        // group tasks by month
        java.util.Map<java.time.YearMonth, java.util.List<Tarea>> grouped = new java.util.LinkedHashMap<>();
        for (Tarea t : tareas) {
            grouped.computeIfAbsent(java.time.YearMonth.from(t.getFecha()), k -> new java.util.ArrayList<>()).add(t);
        }
        int nextAvailable = firstMonthColumn;
        for (java.util.List<Tarea> list : grouped.values()) {
            if (list == null || list.isEmpty()) continue;
            int subtotalCol = nextAvailable + list.size();
            nextAvailable = Math.max(nextAvailable, subtotalCol + 1);
            nextAvailable = subtotalCol + 1; // advance as builder does
        }
        int regularizationColumn = nextAvailable + 6;

        // Verify content and styles beyond the last real column for Etapa 1
        try (FileInputStream in = new FileInputStream("target/planilla-test.xlsx")) {
            XSSFWorkbook checkWb = new XSSFWorkbook(in);
            XSSFSheet sheet = checkWb.getSheet("Planilla-Prueba");
            assertNotNull(sheet, "Sheet Planilla-Prueba should exist");

            // For etapa 1, the last real column is the computed currentStageGradeColumn
            int lastRealColumn = nextAvailable + 1; // currentStageGradeColumn

            int lastRow = sheet.getLastRowNum();
            for (int r = 0; r <= lastRow; r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row == null) continue;
                short lastCellNum = row.getLastCellNum();
                if (lastCellNum <= 0) continue;
                for (int c = lastRealColumn + 1; c < lastCellNum; c++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(c);
                    if (cell == null) continue;
                    // Fail if any non-blank cell remains
                    assertTrue(cell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK || (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING && (cell.getStringCellValue() == null || cell.getStringCellValue().isBlank())),
                            "Found residual content at row " + r + " col " + c);

                    // Also assert that the style was reset to neutral (no borders, no fill)
                    CellStyle cs = cell.getCellStyle();
                    assertNotNull(cs, "CellStyle should be present");
                    assertEquals(BorderStyle.NONE, cs.getBorderTop(), "Border top should be NONE for cell " + r + "," + c);
                    assertEquals(BorderStyle.NONE, cs.getBorderBottom(), "Border bottom should be NONE for cell " + r + "," + c);
                    assertEquals(BorderStyle.NONE, cs.getBorderLeft(), "Border left should be NONE for cell " + r + "," + c);
                    assertEquals(BorderStyle.NONE, cs.getBorderRight(), "Border right should be NONE for cell " + r + "," + c);
                    assertEquals(FillPatternType.NO_FILL, cs.getFillPattern(), "Fill pattern should be NO_FILL for cell " + r + "," + c);
                }
            }
            checkWb.close();
        }

        // --- Now generate and check an Etapa 2 sheet to ensure cleaning also works there ---
        Planilla planilla2 = new Planilla();
        planilla2.setId(2);
        planilla2.setNombre("Planilla Prueba 2");
        planilla2.setPeriodo(2026);
        planilla2.setEtapa("segunda");

        List<Tarea> tareas2 = new ArrayList<>();
        // make several tasks to occupy month blocks
        tareas2.add(new Tarea(201, 1, 1, LocalDate.of(2026, 3, 5), 10, "A1"));
        tareas2.add(new Tarea(202, 1, 2, LocalDate.of(2026, 3, 12), 10, "A2"));
        tareas2.add(new Tarea(203, 1, 3, LocalDate.of(2026, 4, 2), 15, "B1"));
        tareas2.add(new Tarea(204, 1, 4, LocalDate.of(2026, 4, 20), 20, "B2"));
        tareas2.add(new Tarea(205, 1, 5, LocalDate.of(2026, 5, 10), 25, "C1"));

        List<StudentRow> rows2 = new ArrayList<>();
        StudentRow s21 = new StudentRow(1, 2001, "Lopez, Carla");
        s21.setGrades(new HashMap<>());
        s21.setTotal(0);
        rows2.add(s21);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data2 = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla2,
                curso,
                "Programación",
                "Profesor Y",
                "Tarde",
                tareas2,
                rows2,
                new HashMap<>(),
                null
        );

        PlanillaProcesoWorkbookBuilder builder2 = new PlanillaProcesoWorkbookBuilder();
        XSSFWorkbook wb2 = builder2.buildSingleWorkbook(data2, "Planilla-Prueba-2");
        try (FileOutputStream out = new FileOutputStream("target/planilla-test-2.xlsx")) {
            wb2.write(out);
        }
        wb2.close();

        // For etapa 2, last real column should include regularizationColumn (we compute same way builder does)
        int firstMonthColumn2 = 3; // matches STAGE_2 in builder
        java.util.Map<java.time.YearMonth, java.util.List<Tarea>> grouped2 = new java.util.LinkedHashMap<>();
        for (Tarea t : tareas2) {
            grouped2.computeIfAbsent(java.time.YearMonth.from(t.getFecha()), k -> new java.util.ArrayList<>()).add(t);
        }
        int nextAvailable2 = firstMonthColumn2;
        for (java.util.List<Tarea> list : grouped2.values()) {
            if (list == null || list.isEmpty()) continue;
            int subtotalCol = nextAvailable2 + list.size();
            nextAvailable2 = Math.max(nextAvailable2, subtotalCol + 1);
            nextAvailable2 = subtotalCol + 1;
        }
        int regularizationColumn2 = nextAvailable2 + 6;

        try (FileInputStream in2 = new FileInputStream("target/planilla-test-2.xlsx")) {
            XSSFWorkbook checkWb2 = new XSSFWorkbook(in2);
            XSSFSheet sheet2 = checkWb2.getSheet("Planilla-Prueba-2");
            assertNotNull(sheet2, "Sheet Planilla-Prueba-2 should exist");
            int lastRow2 = sheet2.getLastRowNum();
            for (int r = 0; r <= lastRow2; r++) {
                org.apache.poi.ss.usermodel.Row row = sheet2.getRow(r);
                if (row == null) continue;
                short lastCellNum = row.getLastCellNum();
                if (lastCellNum <= 0) continue;
                for (int c = regularizationColumn2 + 1; c < lastCellNum; c++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(c);
                    if (cell == null) continue;
                    assertTrue(cell.getCellType() == org.apache.poi.ss.usermodel.CellType.BLANK || (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING && (cell.getStringCellValue() == null || cell.getStringCellValue().isBlank())),
                            "Found residual content at row " + r + " col " + c + " in etapa 2");
                    CellStyle cs = cell.getCellStyle();
                    assertNotNull(cs);
                    assertEquals(BorderStyle.NONE, cs.getBorderTop());
                    assertEquals(FillPatternType.NO_FILL, cs.getFillPattern());
                }
            }
            checkWb2.close();
        }
    }
}
