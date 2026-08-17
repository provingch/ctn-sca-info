package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

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
                Map.of()
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "Planilla")) {
            assertNotNull(workbook.getSheetAt(0));
        }
    }
}
