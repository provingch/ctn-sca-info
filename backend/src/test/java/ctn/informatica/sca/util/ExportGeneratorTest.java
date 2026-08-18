package ctn.informatica.sca.util;

import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
    }
}
