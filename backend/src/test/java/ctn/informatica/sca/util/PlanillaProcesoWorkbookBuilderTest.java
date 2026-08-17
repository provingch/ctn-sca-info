package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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

    @Test
    void buildSingleWorkbook_replacesTemplatePlaceholdersForUsedMonthBlocks() throws IOException {
        Planilla planilla = new Planilla(2, 1, 1, "comun", "Matemática", 2026, "primera", 7);
        Tarea marzo = new Tarea();
        marzo.setId(101);
        marzo.setFecha(LocalDate.of(2026, 3, 10));
        marzo.setTitulo("Trabajo Práctico 1");
        marzo.setTotal(10);

        Tarea mayo = new Tarea();
        mayo.setId(102);
        mayo.setFecha(LocalDate.of(2026, 5, 12));
        mayo.setTitulo("Trabajo Práctico 2");
        mayo.setTotal(12);

        StudentRow row = new StudentRow();
        row.setAlumnoId(1);
        row.setAlumnoNombre("Ana López");
        Map<Integer, Integer> grades = new HashMap<>();
        grades.put(101, 8);
        grades.put(102, 10);
        row.setGrades(grades);
        row.setTotal(18);

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                new ctn.informatica.sca.model.Curso(1, "Informática", 2027, "A"),
                "Matemática",
                "Ada Lovelace",
                "Mañana",
                List.of(marzo, mayo),
                List.of(row),
                Map.of()
        );

        try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(data, "Planilla")) {
            Sheet sheet = workbook.getSheetAt(0);
            StringBuilder allCellText = new StringBuilder();
            for (Row currentRow : sheet) {
                for (Cell cell : currentRow) {
                    if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        allCellText.append(cell.getStringCellValue());
                    }
                }
            }

            assertFalse(allCellText.toString().contains("{{MES_"), "La plantilla aún conserva placeholders de meses");
            assertFalse(allCellText.toString().contains("{{MES_1_INSTR_"), "La plantilla aún conserva placeholders de instrumentos");
            assertFalse(allCellText.toString().contains("{{"), "La plantilla aún conserva placeholders sin reemplazar");
            assertFalse(sheet.getRow(5).getCell(0).getStringCellValue().contains("{{"));
            assertFalse(sheet.getRow(6).getCell(2).getStringCellValue().contains("{{"));
        }
    }
}
