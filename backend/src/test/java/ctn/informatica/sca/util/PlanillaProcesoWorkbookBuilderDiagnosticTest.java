package ctn.informatica.sca.util;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;

import java.io.IOException;

class PlanillaProcesoWorkbookBuilderDiagnosticTest {

    @Test
    void diag_captureTotalGeneralPositions_snapshotSteps() throws IOException {
        Planilla planilla = new Planilla(9999, 1, 1, "comun", "Diag", 2026, "primera", 7);
        Tarea t1 = new Tarea(); t1.setId(90001); t1.setFecha(LocalDate.of(2026, 3, 5)); t1.setTitulo("T1"); t1.setTotal(5);
        StudentRow s = new StudentRow(); s.setAlumnoId(1); s.setAlumnoNombre("Alumno"); s.setGrades(Map.of(90001,5)); s.setTotal(5);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                null,
                "Diag",
                "Profe",
                "Mañana",
                List.of(t1),
                List.of(s),
                Map.of(),
                null
        );

        PlanillaProcesoWorkbookBuilder builder = new PlanillaProcesoWorkbookBuilder();
        Map<String, java.util.List<Integer>> snaps = new LinkedHashMap<>();
        builder.setDiagnosticSnapshots(snaps);

        try (XSSFWorkbook w = builder.buildSingleWorkbook(data, "Diag")) {
            // workbook generated, snapshots collected in builder.diagnosticSnapshots
        }

        // Print snapshots so surefire captures them in test output
        for (Map.Entry<String, java.util.List<Integer>> e : snaps.entrySet()) {
            System.out.println(e.getKey() + " => " + e.getValue());
        }

        // cleanup
        builder.setDiagnosticSnapshots(null);
    }
}
