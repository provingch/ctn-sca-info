package ctn.informatica.sca.util;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Tarea;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;

public class ExportPlanillaForDebugTest {

    @Test
    void generateWorkbook_for_tp_multi_months() throws Exception {
        Planilla planilla = new Planilla(300, 1, 1, "comun", "TPTest", 2026, "primera", 7);

        Tarea a1 = new Tarea(); a1.setId(4001); a1.setFecha(LocalDate.of(2026, 2, 5)); a1.setTitulo("A1"); a1.setTotal(2);
        Tarea a2 = new Tarea(); a2.setId(4002); a2.setFecha(LocalDate.of(2026, 2, 15)); a2.setTitulo("A2"); a2.setTotal(3);
        Tarea a3 = new Tarea(); a3.setId(4003); a3.setFecha(LocalDate.of(2026, 2, 25)); a3.setTitulo("A3"); a3.setTotal(4);
        Tarea b1 = new Tarea(); b1.setId(4101); b1.setFecha(LocalDate.of(2026, 4, 10)); b1.setTitulo("B1"); b1.setTotal(5);

        ctn.informatica.sca.util.PlanillaProcesoWorkbookBuilder.PlanillaSheetData data =
                new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                        planilla,
                        new ctn.informatica.sca.model.Curso(300, "TPTest", 2026, "Z"),
                        "TPTest",
                        "Prof",
                        "Mañana",
                        List.of(a1,a2,a3,b1),
                        List.of(new ctn.informatica.sca.model.StudentRow() {{
                            setAlumnoId(1);
                            setAlumnoNombre("X");
                            setGrades(java.util.Map.of(4001,2,4002,3,4003,4,4101,5));
                            setTotal(14);
                        }}),
                        java.util.Map.of(),
                        null
                );

        // write to file for inspection
        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "TPDebug")) {
            try (FileOutputStream fos = new FileOutputStream("target/debug_test_planilla_etapa2_multi.xlsx")) {
                workbook.write(fos);
            }
        }
    }
}
