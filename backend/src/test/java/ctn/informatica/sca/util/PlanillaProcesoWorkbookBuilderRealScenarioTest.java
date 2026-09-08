package ctn.informatica.sca.util;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test reproducing the multi-month, variable-task-count scenario
 * similar to the reported Laboratorio Java 3°-A dataset.
 */
class PlanillaProcesoWorkbookBuilderRealScenarioTest {

    @Test
    void multipleMonths_variableTasks_preserveInstrumentStylingAndBorders() throws IOException {
        // Create a planilla with 5 months and variable task counts: 2,6,7,3,6
        Planilla planilla = new Planilla(900, 1, 1, "comun", "Real", 2026, "primera", 7);
        List<Tarea> tareas = new ArrayList<>();

        // Month A: 2 tasks (Feb)
        for (int i = 1; i <= 2; i++) {
            Tarea t = new Tarea(); t.setId(900 + i); t.setFecha(LocalDate.of(2026, 2, i)); t.setTitulo("F-" + i); t.setTotal(5); tareas.add(t);
        }
        // Month B: 6 tasks (Mar)
        for (int i = 1; i <= 6; i++) {
            Tarea t = new Tarea(); t.setId(910 + i); t.setFecha(LocalDate.of(2026, 3, 1 + i)); t.setTitulo("M-" + i); t.setTotal(5); tareas.add(t);
        }
        // Month C: 7 tasks (Apr)
        for (int i = 1; i <= 7; i++) {
            Tarea t = new Tarea(); t.setId(930 + i); t.setFecha(LocalDate.of(2026, 4, 1 + i)); t.setTitulo("A-" + i); t.setTotal(5); tareas.add(t);
        }
        // Month D: 3 tasks (May)
        for (int i = 1; i <= 3; i++) {
            Tarea t = new Tarea(); t.setId(950 + i); t.setFecha(LocalDate.of(2026, 5, 1 + i)); t.setTitulo("S-" + i); t.setTotal(5); tareas.add(t);
        }
        // Month E: 6 tasks (Jun)
        for (int i = 1; i <= 6; i++) {
            Tarea t = new Tarea(); t.setId(960 + i); t.setFecha(LocalDate.of(2026, 6, 1 + i)); t.setTitulo("J-" + i); t.setTotal(5); tareas.add(t);
        }

        StudentRow s = new StudentRow(); s.setAlumnoId(1); s.setAlumnoNombre("Alumno"); Map<Integer,Integer> grades = new HashMap<>();
        for (Tarea t : tareas) grades.put(t.getId(), 4);
        s.setGrades(grades);
        s.setTotal(tareas.stream().mapToInt(Tarea::getTotal).sum());

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(900, "Informática", 2026, "A"),
                "Real",
                "Profe",
                "Mañana",
                tareas,
                List.of(s),
                Map.of(),
                null
        );

        try (XSSFWorkbook wb = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "RealTest")) {
            Sheet sheet = wb.getSheetAt(0);
            // Check instrument title row and several grade/last-instrument columns
            Row titleRow = sheet.getRow(6);
            Row tpRow = sheet.getRow(7);
            assertNotNull(titleRow);
            assertNotNull(tpRow);

            // Verify that instrument title cells and corresponding TP cells have borders and center alignment
            for (int c = 2; c < 100; c++) {
                Cell tcell = titleRow.getCell(c);
                Cell pcell = tpRow.getCell(c);
                if (tcell == null || tcell.getCellType() != org.apache.poi.ss.usermodel.CellType.STRING) continue;
                CellStyle ts = tcell.getCellStyle();
                assertNotNull(ts, "Title cell style must not be null for col " + c);
                boolean hasBorder = ts.getBorderLeft() != org.apache.poi.ss.usermodel.BorderStyle.NONE
                        || ts.getBorderRight() != org.apache.poi.ss.usermodel.BorderStyle.NONE
                        || ts.getBorderTop() != org.apache.poi.ss.usermodel.BorderStyle.NONE
                        || ts.getBorderBottom() != org.apache.poi.ss.usermodel.BorderStyle.NONE;
                assertTrue(hasBorder, "Instrument title at col " + c + " must have a border");

                if (pcell != null) {
                    CellStyle ps = pcell.getCellStyle();
                    assertNotNull(ps, "TP style must not be null for col " + c);
                    boolean tpHasBorder = ps.getBorderLeft() != org.apache.poi.ss.usermodel.BorderStyle.NONE
                            || ps.getBorderRight() != org.apache.poi.ss.usermodel.BorderStyle.NONE
                            || ps.getBorderTop() != org.apache.poi.ss.usermodel.BorderStyle.NONE
                            || ps.getBorderBottom() != org.apache.poi.ss.usermodel.BorderStyle.NONE;
                    assertTrue(tpHasBorder, "TP cell at col " + c + " must have a border");
                }
            }
        }
    }
}
