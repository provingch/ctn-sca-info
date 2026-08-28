package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import ctn.informatica.sca.dto.PlanCurricularDto;

public class PlanCurricularParserTest {

    @Test
    public void etapaUnoConFormatoDePlantilla_deberiaParsearse() {
        PlanCurricularParser.EtapaAnio result = PlanCurricularParser.parseEtapaAnio(
                "PLAN DE DESARROLLO CURRICULAR ETAPA:_1°_2026");

        assertEquals("1", result.etapa());
        assertEquals(2026, result.anio());
    }

    @Test
    public void etapaDosConFormatoDePlantilla_deberiaParsearse() {
        PlanCurricularParser.EtapaAnio result = PlanCurricularParser.parseEtapaAnio(
                "PLAN DE DESARROLLO CURRICULAR ETAPA:_2°_2026");

        assertEquals("2", result.etapa());
        assertEquals(2026, result.anio());
    }

    @Test
    public void etapaSinSimboloGradoYConEspacios_deberiaParsearse() {
        PlanCurricularParser.EtapaAnio result = PlanCurricularParser.parseEtapaAnio(
                " PLAN DE DESARROLLO CURRICULAR ETAPA:  _ 1 _ 2026 ");

        assertEquals("1", result.etapa());
        assertEquals(2026, result.anio());
    }

    @Test
    public void formatoInesperado_deberiaLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> PlanCurricularParser.parseEtapaAnio("PLAN DE DESARROLLO CURRICULAR"));
    }

    @Test
    public void etapaDosConPlantillaGenerada_deberiaParsearseYOrdenarMeses() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            for (String nombre : new String[] {"Julio", "Agosto", "Septiembre", "Octubre", "Noviembre"}) {
                Sheet sh = wb.createSheet(nombre);
                setCell(sh, 4, 1, "PLAN DE DESARROLLO CURRICULAR ETAPA:_2°_2026");
                setCell(sh, 6, 1, "Disciplina: Matemática");
                setCell(sh, 8, 1, "Curso: 5");
                setCell(sh, 8, 12, "Sección: A");
                setCell(sh, 8, 18, "Turno: Mañana");
                setCell(sh, 8, 22, "Especialidad: Informática");
                setCell(sh, 14, 2, "Capacidad");
                setCell(sh, 14, 11, "Tema");
                setCell(sh, 14, 18, "Actividad");
                setCell(sh, 14, 27, "Instrumento");
                setCell(sh, 14, 34, "Indicador conceptual");
                setCell(sh, 16, 34, "Indicador procedimental");
                setCell(sh, 18, 34, "Indicador actitudinal");
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            try (ByteArrayInputStream in = new ByteArrayInputStream(bos.toByteArray())) {
                PlanCurricularDto dto = new PlanCurricularParser().parse(in);
                assertEquals("2", dto.etapa);
                assertEquals(2026, dto.anio);
                List<Integer> meses = dto.temas.stream().map(t -> t.ordenMes).distinct().sorted().collect(Collectors.toList());
                assertEquals(List.of(1, 2, 3, 4, 5), meses);
            }
        }
    }

    private void setCell(Sheet sh, int rowIndex, int colIndex, String value) {
        Row row = sh.getRow(rowIndex);
        if (row == null) row = sh.createRow(rowIndex);
        row.createCell(colIndex).setCellValue(value);
    }
}