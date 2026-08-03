package ctn.informatica.sca.util;

import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanillaProcesoWorkbookBuilderTest {

    @Test
    void buildSingleWorkbookUsesOfficialTemplateForFirstStage() throws IOException {
        PlanillaProcesoWorkbookBuilder builder = new PlanillaProcesoWorkbookBuilder();
        Planilla planilla = new Planilla(11, 3, 7, "comun", "Matemática", 2026, "primera", 5);
        Curso curso = new Curso(3, "Informática", 2027, "A");

        List<Tarea> tareas = List.of(
                tarea(101, LocalDate.of(2026, 3, 10), 10, "Trabajo Práctico 1"),
                tarea(102, LocalDate.of(2026, 3, 24), 15, "Trabajo Práctico 2"),
                tarea(103, LocalDate.of(2026, 4, 3), 20, "Prueba Parcial")
        );

        List<StudentRow> rows = List.of(
                studentRow(1, "Alfa, Ana", Map.of(101, 8, 102, 14, 103, 18)),
                studentRow(2, "Beta, Beto", Map.of(101, 9, 102, 10)),
                studentRow(3, "Gamma, Gabi", Map.of(103, 19)),
                studentRow(4, "Delta, Dami", Map.of(101, 10, 102, 15, 103, 20))
        );

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                curso,
                "Matemática",
                "Ada Lovelace",
                "Mañana",
                tareas,
                rows,
                Map.of()
        );

        try (XSSFWorkbook workbook = builder.buildSingleWorkbook(data, "Planilla Matemática")) {
            assertEquals(1, workbook.getNumberOfSheets());

            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("Disciplina: Matemática", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals("Marzo", sheet.getRow(5).getCell(2).getStringCellValue());
            assertEquals("Abril", sheet.getRow(5).getCell(15).getStringCellValue());
            assertEquals("Trabajo Práctico 1", sheet.getRow(6).getCell(2).getStringCellValue());
            assertEquals(10d, sheet.getRow(7).getCell(2).getNumericCellValue());
            assertEquals("Alfa, Ana", sheet.getRow(8).getCell(1).getStringCellValue());
            assertEquals(8d, sheet.getRow(8).getCell(2).getNumericCellValue());
            assertEquals("Delta, Dami", sheet.getRow(11).getCell(1).getStringCellValue());
            assertEquals("SUM(C12:N12)", sheet.getRow(11).getCell(14).getCellFormula());
            assertEquals(5d, sheet.getRow(11).getCell(68).getNumericCellValue());
        }
    }

    @Test
    void buildSingleWorkbookPopulatesStageTwoWithFirstStageGradeAndKeepsAverageFormula() throws IOException {
        PlanillaProcesoWorkbookBuilder builder = new PlanillaProcesoWorkbookBuilder();
        Planilla planilla = new Planilla(22, 3, 7, "comun", "Matemática", 2026, "segunda", 5);
        Curso curso = new Curso(3, "Informática", 2027, "A");

        List<Tarea> tareas = List.of(
                tarea(201, LocalDate.of(2026, 7, 10), 12, "Laboratorio 1"),
                tarea(202, LocalDate.of(2026, 8, 18), 18, "Laboratorio 2")
        );

        List<StudentRow> rows = List.of(
                studentRow(10, "Alfa, Ana", Map.of(201, 10, 202, 17))
        );

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                curso,
                "Matemática",
                "Ada Lovelace",
                "Tarde",
                tareas,
                rows,
                Map.of(10, 4)
        );

        try (XSSFWorkbook workbook = builder.buildSingleWorkbook(data, "Planilla Matemática Segunda")) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("Julio", sheet.getRow(5).getCell(3).getStringCellValue());
            assertEquals(4d, sheet.getRow(8).getCell(2).getNumericCellValue());
            assertEquals(4d, sheet.getRow(8).getCell(69).getNumericCellValue());
            assertEquals(CellType.FORMULA, sheet.getRow(8).getCell(70).getCellType());
            assertEquals("SUM(C9+BR9)", sheet.getRow(8).getCell(70).getCellFormula());
            assertEquals("IF((C9+BR9)/2<=1.5,1,IF(BR9=1,1,ROUND((C9+BR9)/2,0)))", sheet.getRow(8).getCell(71).getCellFormula());
            assertEquals(CellType.BLANK, sheet.getRow(8).getCell(72).getCellType());
            assertEquals(CellType.BLANK, sheet.getRow(8).getCell(73).getCellType());
        }
    }

    @Test
    void buildSingleWorkbookFailsWhenMonthExceedsTemplateCapacity() {
        PlanillaProcesoWorkbookBuilder builder = new PlanillaProcesoWorkbookBuilder();
        Planilla planilla = new Planilla(33, 3, 7, "comun", "Matemática", 2026, "primera", 5);
        Curso curso = new Curso(3, "Informática", 2027, "A");

        List<Tarea> tareas = java.util.stream.IntStream.rangeClosed(1, 13)
                .mapToObj(index -> tarea(300 + index, LocalDate.of(2026, 3, Math.min(index, 28)), 5, "Tarea " + index))
                .toList();

        PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                planilla,
                curso,
                "Matemática",
                "Ada Lovelace",
                "",
                tareas,
                List.of(),
                Map.of()
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> builder.buildSingleWorkbook(data, "Planilla Matemática"));

        assertEquals(true, ex.getMessage().contains("12 columnas"));
    }

    private static Tarea tarea(int id, LocalDate fecha, int total, String titulo) {
        Tarea tarea = new Tarea();
        tarea.setId(id);
        tarea.setFecha(fecha);
        tarea.setTotal(total);
        tarea.setTitulo(titulo);
        return tarea;
    }

    private static StudentRow studentRow(int alumnoId, String nombre, Map<Integer, Integer> grades) {
        StudentRow row = new StudentRow();
        row.setAlumnoId(alumnoId);
        row.setAlumnoNombre(nombre);
        row.setGrades(new HashMap<>(grades));
        int total = grades.values().stream().filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        row.setTotal(total);
        return row;
    }
}