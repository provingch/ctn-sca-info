package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

public class PlanCurricularTemplateBuilderTest {

    @Test
    public void etapaDos_debeIncluirHojasDeJulioANoviembre() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/plan-curricular-plantilla-etapa2.xlsx")) {
            assertNotNull(in, "Falta la plantilla de etapa 2");
            try (Workbook wb = WorkbookFactory.create(in)) {
                assertNotNull(wb.getSheet("Julio"));
                assertNotNull(wb.getSheet("Agosto"));
                assertNotNull(wb.getSheet("Septiembre"));
                assertNotNull(wb.getSheet("Octubre"));
                assertNotNull(wb.getSheet("Noviembre"));
                assertNull(wb.getSheet("Marzo"));
            }
        }
    }
}
