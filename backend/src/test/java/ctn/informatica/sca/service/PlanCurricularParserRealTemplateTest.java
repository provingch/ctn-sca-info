package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import ctn.informatica.sca.dto.PlanCurricularDto;

public class PlanCurricularParserRealTemplateTest {

    @Test
    public void plantillaReal_debeParsear4BloquesPorMes() throws Exception {
        try (InputStream res = getClass().getResourceAsStream("/plan-curricular-plantilla.xlsx")) {
            assertNotNull(res, "Falta la plantilla de etapa 1 en resources");
            try (Workbook wb = WorkbookFactory.create(res); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                // write header + four bloques in first month sheet (Marzo)
                Sheet sh = wb.getSheet("Marzo");
                if (sh == null) {
                    // if plantilla is etapa2 fallback to first sheet
                    sh = wb.getSheetAt(0);
                }
                // header fields expected by parser
                setCell(sh, 4, 1, "PLAN DE DESARROLLO CURRICULAR ETAPA:_1°_2026");
                setCell(sh, 6, 1, "Disciplina: Matemática");
                setCell(sh, 8, 1, "Curso: 5");
                setCell(sh, 8, 12, "Sección: A");
                setCell(sh, 8, 18, "Turno: Mañana");
                setCell(sh, 8, 22, "Especialidad: Informática");

                // four bloques at rows 15,21,28,35 -> indexes 14,20,27,34
                setCell(sh, 14, 2, "Capacidad A");
                setCell(sh, 14, 11, "Tema A");
                setCell(sh, 20, 2, "Capacidad B");
                setCell(sh, 20, 11, "Tema B");
                setCell(sh, 27, 2, "Capacidad C");
                setCell(sh, 27, 11, "Tema C");
                setCell(sh, 34, 2, "Capacidad D");
                setCell(sh, 34, 11, "Tema D");

                wb.write(bos);
                try (ByteArrayInputStream in = new ByteArrayInputStream(bos.toByteArray())) {
                    PlanCurricularDto dto = new PlanCurricularParser().parse(in);
                    // for the month we added, expect 4 temas distinct blocks
                    long count = dto.temas.stream().filter(t -> t.ordenMes == 1).count();
                    assertEquals(4, count);
                }
            }
        }
    }

    private void setCell(Sheet sh, int rowIndex, int colIndex, String value) {
        Row row = sh.getRow(rowIndex);
        if (row == null) row = sh.createRow(rowIndex);
        row.createCell(colIndex).setCellValue(value);
    }
}
