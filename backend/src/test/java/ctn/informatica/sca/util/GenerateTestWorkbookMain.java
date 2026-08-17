package ctn.informatica.sca.util;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class GenerateTestWorkbookMain {

    public static void main(String[] args) throws Exception {
        String outputPath = args.length > 0 ? args[0] : "/tmp/test_planilla_dinamico.xlsx";

        Planilla planilla = new Planilla(99, 1, 1, "comun", "Prueba Dinámico", 2026, "primera", 7);

        // February: 3 tasks
        Tarea t1 = new Tarea();
        t1.setId(501);
        t1.setFecha(LocalDate.of(2026, 2, 5));
        t1.setTitulo("Quiz Corto");
        t1.setTotal(5);

        Tarea t2 = new Tarea();
        t2.setId(502);
        t2.setFecha(LocalDate.of(2026, 2, 15));
        t2.setTitulo("Trabajo de investigación grupal");
        t2.setTotal(10);

        Tarea t3 = new Tarea();
        t3.setId(503);
        t3.setFecha(LocalDate.of(2026, 2, 25));
        t3.setTitulo("Presentación");
        t3.setTotal(8);

        // April: 1 task
        Tarea t4 = new Tarea();
        t4.setId(504);
        t4.setFecha(LocalDate.of(2026, 4, 10));
        t4.setTitulo("Taller práctico");
        t4.setTotal(7);

        StudentRow s1 = new StudentRow();
        s1.setAlumnoId(1);
        s1.setAlumnoNombre("Estudiante A");
        Map<Integer, Integer> g1 = new HashMap<>();
        g1.put(501, 5);
        g1.put(502, 9);
        g1.put(503, 8);
        g1.put(504, 7);
        s1.setGrades(g1);
        s1.setTotal(29);

        StudentRow s2 = new StudentRow();
        s2.setAlumnoId(2);
        s2.setAlumnoNombre("Estudiante B");
        Map<Integer, Integer> g2 = new HashMap<>();
        g2.put(501, 4);
        g2.put(502, 8);
        g2.put(503, 7);
        g2.put(504, 6);
        s2.setGrades(g2);
        s2.setTotal(25);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(99, "Prueba", 2026, "Z"),
                "Prueba Dinámico",
                "Profesor Test",
                "Mañana",
                List.of(t1, t2, t3, t4),
                List.of(s1, s2),
                Map.of(),
                null
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "Prueba")) {
            FileOutputStream fos = new FileOutputStream(outputPath);
            workbook.write(fos);
            fos.close();
            System.out.println("✓ Generated test workbook: " + outputPath);
            System.out.println("  - February: 3 tasks (columns 3-5 visible, 6-14 hidden)");
            System.out.println("  - April: 1 task (column 3 visible, 4-14 hidden)");
            System.out.println("  - June, August, October: empty months (all 13 columns hidden)");
            System.out.println("  Open in LibreOffice Calc to verify column hiding and formulas");
        }
    }
}
