package ctn.informatica.sca.util;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Helper test that writes the real multi-month workbook to disk for inspection.
 */
class PlanillaProcesoWorkbookBuilderDumpRealTest {

    @Test
    void dumpRealPlanillaToFile() throws IOException {
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

        try (XSSFWorkbook wb = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "Laboratorio-Java")) {
            try (FileOutputStream out = new FileOutputStream("target/real_planilla.xlsx")) {
                wb.write(out);
            }
        }
    }
}
