package ctn.informatica.sca.util;

import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PlanillaProcesoWorkbookBuilder {

    private static final String TEMPLATE_RESOURCE = "templates/PLANTILLA_PLANILLA_PROCESO_CTN.xlsx";
    private static final String LEGEND_SHEET = "LEYENDA_PARA_DESARROLLO";
    private static final int MONTH_BLOCK_COUNT = 5;
    private static final int INSTRUMENTS_PER_MONTH = 12;
    private static final int MONTH_BLOCK_WIDTH = 13;
    private static final int MONTH_HEADER_ROW = 5;
    private static final int INSTRUMENT_TITLE_ROW = 6;
    private static final int TP_ROW = 7;
    private static final int FIRST_STUDENT_ROW = 8;
    private static final int TEMPLATE_STUDENT_COUNT = 3;
    private static final Locale SPANISH = Locale.forLanguageTag("es-PY");

    private static final StageLayout STAGE_1 = new StageLayout(
            "PLANTILLA_ETAPA_1",
            2,
            CellReference.convertColStringToIndex("BP"),
            CellReference.convertColStringToIndex("BQ"),
            -1,
            -1,
            -1,
            -1,
            -1
    );

    private static final StageLayout STAGE_2 = new StageLayout(
            "PLANTILLA_ETAPA_2",
            3,
            CellReference.convertColStringToIndex("BQ"),
            CellReference.convertColStringToIndex("BR"),
            CellReference.convertColStringToIndex("C"),
            CellReference.convertColStringToIndex("BS"),
            CellReference.convertColStringToIndex("BT"),
            CellReference.convertColStringToIndex("BU"),
            CellReference.convertColStringToIndex("BV")
    );

    public XSSFWorkbook buildSingleWorkbook(PlanillaSheetData data, String sheetName) throws IOException {
        XSSFWorkbook workbook = loadTemplateWorkbook();
        try {
            XSSFSheet sheet = cloneTemplateSheet(workbook, layoutFor(data.planilla()), sheetName);
            populateSheet(sheet, data);
            removeTemplateSheets(workbook);
            workbook.setActiveSheet(0);
            workbook.setForceFormulaRecalculation(true);
            return workbook;
        } catch (RuntimeException ex) {
            workbook.close();
            throw ex;
        }
    }

    public XSSFWorkbook buildCourseWorkbook(Collection<PlanillaSheetData> sheets) throws IOException {
        XSSFWorkbook workbook = loadTemplateWorkbook();
        try {
            for (PlanillaSheetData data : sheets) {
                String desiredName = data.disciplina() == null || data.disciplina().isBlank()
                        ? "Planilla-" + data.planilla().getId()
                        : data.disciplina();
                XSSFSheet sheet = cloneTemplateSheet(workbook, layoutFor(data.planilla()), uniqueSheetName(workbook, desiredName));
                populateSheet(sheet, data);
            }
            removeTemplateSheets(workbook);
            workbook.setActiveSheet(0);
            workbook.setForceFormulaRecalculation(true);
            return workbook;
        } catch (RuntimeException ex) {
            workbook.close();
            throw ex;
        }
    }

    private XSSFWorkbook loadTemplateWorkbook() throws IOException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(TEMPLATE_RESOURCE);
        if (stream == null) {
            throw new IOException("No se pudo encontrar la plantilla oficial: " + TEMPLATE_RESOURCE);
        }
        try (InputStream in = stream) {
            return new XSSFWorkbook(in);
        }
    }

    private XSSFSheet cloneTemplateSheet(XSSFWorkbook workbook, StageLayout layout, String desiredName) {
        int templateIndex = workbook.getSheetIndex(layout.templateSheetName());
        workbook.cloneSheet(templateIndex);
        int clonedIndex = workbook.getNumberOfSheets() - 1;
        String safeName = uniqueSheetName(workbook, desiredName, clonedIndex);
        workbook.setSheetName(clonedIndex, safeName);
        XSSFSheet sheet = workbook.getSheetAt(clonedIndex);
        sheet.setForceFormulaRecalculation(true);
        return sheet;
    }

    private void removeTemplateSheets(XSSFWorkbook workbook) {
        removeSheetIfPresent(workbook, LEGEND_SHEET);
        removeSheetIfPresent(workbook, STAGE_1.templateSheetName());
        removeSheetIfPresent(workbook, STAGE_2.templateSheetName());
    }

    private void removeSheetIfPresent(XSSFWorkbook workbook, String sheetName) {
        int index = workbook.getSheetIndex(sheetName);
        if (index >= 0) {
            workbook.removeSheetAt(index);
        }
    }

    private StageLayout layoutFor(Planilla planilla) {
        return planilla != null && planilla.getEtapaIndex() == 2 ? STAGE_2 : STAGE_1;
    }

    private void populateSheet(XSSFSheet sheet, PlanillaSheetData data) {
        StageLayout layout = layoutFor(data.planilla());
        List<Tarea> tareasEtapa = filterTasksByEtapa(data.tareas(), data.planilla().getEtapaIndex());
        int currentTotalPossiblePoints = totalPossiblePoints(tareasEtapa);
        data.planilla().computeGradeRanges(currentTotalPossiblePoints);

        Map<YearMonth, List<Tarea>> tareasPorMes = groupTasksByMonth(tareasEtapa);
        if (tareasPorMes.size() > MONTH_BLOCK_COUNT) {
            // TODO: si una etapa usa más meses que los 5 bloques de la plantilla, hay que acordar con negocio
            // si se regeneran columnas/meses o si se rediseña la plantilla oficial. No truncar silenciosamente.
            throw new IllegalStateException("La plantilla oficial solo soporta " + MONTH_BLOCK_COUNT + " meses con tareas por etapa. Se encontraron " + tareasPorMes.size() + " meses ocupados en la planilla " + data.planilla().getId() + ": " + describeMonths(tareasPorMes.keySet()));
        }

        Map<Integer, Integer> taskColumnById = allocateTaskColumns(tareasPorMes, layout, data.planilla().getId());
        resizeStudentArea(sheet, data.rows().size());
        replaceCommonMarkers(sheet, data);
        fillMonthBlocks(sheet, tareasPorMes, taskColumnById, layout);
        fillStudentRows(sheet, data, taskColumnById, layout);
        clearTemplatePlaceholders(sheet);
        setTeacherSignature(sheet, data);
    }

    private int monthBlockWidth(List<Tarea> tareasMes) {
        int actualTasks = tareasMes == null ? 0 : tareasMes.size();
        return Math.max(2, actualTasks + 2);
    }

    private Map<YearMonth, List<Tarea>> groupTasksByMonth(List<Tarea> tareas) {
        Map<YearMonth, List<Tarea>> grouped = new LinkedHashMap<>();
        if (tareas == null) {
            return grouped;
        }
        List<Tarea> sorted = new ArrayList<>(tareas);
        sorted.sort(Comparator
                .comparing(Tarea::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Tarea::getId));
        for (Tarea tarea : sorted) {
            if (tarea == null || tarea.getFecha() == null) {
                continue;
            }
            grouped.computeIfAbsent(YearMonth.from(tarea.getFecha()), ignored -> new ArrayList<>()).add(tarea);
        }
        return grouped;
    }

    public static List<Tarea> filterTasksByEtapa(List<Tarea> tareas, int planillaEtapaIndex) {
        if (tareas == null || tareas.isEmpty() || (planillaEtapaIndex != 1 && planillaEtapaIndex != 2)) {
            return tareas == null ? List.of() : tareas;
        }

        List<Tarea> filtered = new ArrayList<>();
        for (Tarea tarea : tareas) {
            if (tarea == null || tarea.getFecha() == null) {
                continue;
            }
            if (Tarea.resolveEtapaIndexByPublicationDate(tarea.getFecha()) == planillaEtapaIndex) {
                filtered.add(tarea);
            }
        }
        return filtered;
    }

    private String describeMonths(Collection<YearMonth> months) {
        List<String> labels = new ArrayList<>();
        for (YearMonth month : months) {
            labels.add(capitalize(month.getMonth().getDisplayName(TextStyle.FULL, SPANISH)) + " " + month.getYear());
        }
        return String.join(", ", labels);
    }

    private Map<Integer, Integer> allocateTaskColumns(Map<YearMonth, List<Tarea>> tareasPorMes, StageLayout layout, int planillaId) {
        Map<Integer, Integer> mapping = new HashMap<>();
        int nextColumn = layout.firstMonthColumn();
        for (List<Tarea> tareasMes : tareasPorMes.values()) {
            int availableSlots = Math.max(0, tareasMes == null ? 0 : tareasMes.size());
            if (availableSlots > 0) {
                for (int taskIndex = 0; taskIndex < tareasMes.size(); taskIndex++) {
                    mapping.put(tareasMes.get(taskIndex).getId(), nextColumn + 2 + taskIndex);
                }
            }
            nextColumn += monthBlockWidth(tareasMes);
        }
        return mapping;
    }

    private void replaceCommonMarkers(Sheet sheet, PlanillaSheetData data) {
        Map<String, String> replacements = new LinkedHashMap<>();
        Curso curso = data.curso();
        replacements.put("{{DISCIPLINA}}", safeString(data.disciplina()));
        replacements.put("{{ESPECIALIDAD}}", curso == null ? "" : safeString(curso.getEspecialidad()));
        replacements.put("{{PROFESOR}}", safeString(data.profesorNombre()));
        replacements.put("{{CURSO}}", curso == null ? "" : stripOrdinalSuffix(safeString(curso.getCursoOrdinal())));
        replacements.put("{{TURNO}}", safeString(data.turno()));
        replacements.put("{{SECCION}}", curso == null ? "" : safeString(curso.getSeccion()));
        replacements.put("{{ANIO}}", String.valueOf(data.planilla().getPeriodo()));

        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() != CellType.STRING) {
                    continue;
                }
                String value = cell.getStringCellValue();
                String replaced = value;
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    replaced = replaced.replace(entry.getKey(), entry.getValue());
                }
                if (!replaced.equals(value)) {
                    cell.setCellValue(replaced);
                }
            }
        }
    }

    private void fillMonthBlocks(Sheet sheet, Map<YearMonth, List<Tarea>> tareasPorMes, Map<Integer, Integer> taskColumnById, StageLayout layout) {
        List<Map.Entry<YearMonth, List<Tarea>>> months = new ArrayList<>(tareasPorMes.entrySet());
        int currentColumn = layout.firstMonthColumn();

        for (int monthBlockIndex = 0; monthBlockIndex < MONTH_BLOCK_COUNT; monthBlockIndex++) {
            Row monthHeaderRow = getOrCreateRow(sheet, MONTH_HEADER_ROW);
            Row titleRow = getOrCreateRow(sheet, INSTRUMENT_TITLE_ROW);
            Row tpRow = getOrCreateRow(sheet, TP_ROW);

            String monthLabel = "";
            List<Tarea> tareasMes = List.of();
            if (monthBlockIndex < months.size()) {
                Map.Entry<YearMonth, List<Tarea>> monthEntry = months.get(monthBlockIndex);
                monthLabel = capitalize(monthEntry.getKey().getMonth().getDisplayName(TextStyle.FULL, SPANISH));
                tareasMes = monthEntry.getValue() == null ? List.of() : monthEntry.getValue();
            }

            int firstCol = currentColumn;
            setStringCell(monthHeaderRow, firstCol, monthLabel);

            for (int instrumentIndex = 0; instrumentIndex < INSTRUMENTS_PER_MONTH; instrumentIndex++) {
                int colIndex = firstCol + 2 + instrumentIndex;
                Cell titleCell = getOrCreateCell(titleRow, colIndex);
                Cell tpCell = getOrCreateCell(tpRow, colIndex);

                if (instrumentIndex < tareasMes.size()) {
                    Tarea tarea = tareasMes.get(instrumentIndex);
                    titleCell.setCellValue(safeString(tarea.getTitulo()));
                    setNumericCell(tpCell, tarea.getTotal());
                    taskColumnById.put(tarea.getId(), colIndex);
                } else {
                    titleCell.setBlank();
                    tpCell.setBlank();
                }
            }

            currentColumn += monthBlockWidth(tareasMes);
        }
    }

    private void resizeStudentArea(XSSFSheet sheet, int studentCount) {
        int templateLastStudentRow = FIRST_STUDENT_ROW + TEMPLATE_STUDENT_COUNT - 1;
        if (studentCount < TEMPLATE_STUDENT_COUNT) {
            for (int rowIndex = templateLastStudentRow; rowIndex >= FIRST_STUDENT_ROW + studentCount; rowIndex--) {
                removeRowAndShiftUp(sheet, rowIndex);
            }
            return;
        }

        int additionalRows = studentCount - TEMPLATE_STUDENT_COUNT;
        if (additionalRows <= 0) {
            return;
        }

        int lastRowNum = sheet.getLastRowNum();
        sheet.shiftRows(templateLastStudentRow + 1, lastRowNum, additionalRows, true, false);

        CellCopyPolicy copyPolicy = new CellCopyPolicy.Builder()
                .cellFormula(true)
                .cellStyle(true)
                .cellValue(true)
                .mergedRegions(true)
                .build();

        int sourceRow = templateLastStudentRow;
        for (int offset = 1; offset <= additionalRows; offset++) {
            int destinationRow = templateLastStudentRow + offset;
            sheet.copyRows(sourceRow, sourceRow, destinationRow, copyPolicy);
            sourceRow = destinationRow;
        }
    }

    private void removeRowAndShiftUp(XSSFSheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row != null) {
            sheet.removeRow(row);
        }
        if (rowIndex < sheet.getLastRowNum()) {
            sheet.shiftRows(rowIndex + 1, sheet.getLastRowNum(), -1, true, false);
        }
    }

    private void fillStudentRows(Sheet sheet, PlanillaSheetData data, Map<Integer, Integer> taskColumnById, StageLayout layout) {
        int maxColumn = layout.firstMonthColumn();
        for (Integer col : taskColumnById.values()) {
            maxColumn = Math.max(maxColumn, col);
        }

        for (int rowOffset = 0; rowOffset < data.rows().size(); rowOffset++) {
            StudentRow studentRow = data.rows().get(rowOffset);
            Row excelRow = getOrCreateRow(sheet, FIRST_STUDENT_ROW + rowOffset);
            setNumericCell(getOrCreateCell(excelRow, 0), rowOffset + 1);
            setStringCell(excelRow, 1, studentRow.getAlumnoNombre());

            for (int col = layout.firstMonthColumn(); col <= maxColumn; col++) {
                getOrCreateCell(excelRow, col).setBlank();
            }

            Map<Integer, Integer> grades = studentRow.getGrades();
            if (grades != null) {
                for (Map.Entry<Integer, Integer> entry : grades.entrySet()) {
                    Integer columnIndex = taskColumnById.get(entry.getKey());
                    if (columnIndex == null) {
                        continue;
                    }
                    Cell gradeCell = getOrCreateCell(excelRow, columnIndex);
                    if (entry.getValue() == null) {
                        gradeCell.setBlank();
                    } else {
                        setNumericCell(gradeCell, entry.getValue());
                    }
                }
            }

            int currentStageGrade = data.planilla().getNotaForSum(studentRow.getTotal());
            setNumericCell(getOrCreateCell(excelRow, layout.currentStageGradeColumn()), currentStageGrade);

            if (layout.firstStageGradeColumn() >= 0) {
                Integer firstStageGrade = data.firstStageGrades().get(studentRow.getAlumnoId());
                Cell firstStageCell = getOrCreateCell(excelRow, layout.firstStageGradeColumn());
                if (firstStageGrade == null) {
                    firstStageCell.setBlank();
                } else {
                    setNumericCell(firstStageCell, firstStageGrade);
                }
                getOrCreateCell(excelRow, layout.complementaryColumn()).setBlank();
                getOrCreateCell(excelRow, layout.regularizationColumn()).setBlank();
            }
        }
    }

    private int totalPossiblePoints(List<Tarea> tareas) {
        int total = 0;
        if (tareas == null) {
            return total;
        }
        for (Tarea tarea : tareas) {
            if (tarea != null) {
                total += tarea.getTotal();
            }
        }
        return total;
    }

    private String safeSheetName(String value) {
        if (value == null || value.isBlank()) {
            return "Planilla";
        }
        String sanitized = value.replaceAll("[:\\\\/?*\\[\\]]", " ").trim();
        if (sanitized.isEmpty()) {
            return "Planilla";
        }
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }

    private String uniqueSheetName(XSSFWorkbook workbook, String desiredName) {
        return uniqueSheetName(workbook, desiredName, -1);
    }

    private String uniqueSheetName(XSSFWorkbook workbook, String desiredName, int ignoredIndex) {
        String baseName = safeSheetName(desiredName);
        String candidate = baseName;
        int suffix = 1;
        while (sheetNameExists(workbook, candidate, ignoredIndex)) {
            String suffixText = "-" + suffix;
            int maxBaseLength = Math.max(1, 31 - suffixText.length());
            String trimmedBase = baseName.length() > maxBaseLength ? baseName.substring(0, maxBaseLength) : baseName;
            candidate = trimmedBase + suffixText;
            suffix++;
        }
        return candidate;
    }

    private boolean sheetNameExists(XSSFWorkbook workbook, String name, int ignoredIndex) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (i == ignoredIndex) {
                continue;
            }
            if (name.equals(workbook.getSheetName(i))) {
                return true;
            }
        }
        return false;
    }

    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row != null ? row : sheet.createRow(rowIndex);
    }

    private Cell getOrCreateCell(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        return cell != null ? cell : row.createCell(columnIndex);
    }

    private void setStringCell(Row row, int columnIndex, String value) {
        setStringCell(getOrCreateCell(row, columnIndex), value);
    }

    private void setStringCell(Cell cell, String value) {
        cell.setCellValue(safeString(value));
    }

    private void clearTemplatePlaceholders(Sheet sheet) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell == null || cell.getCellType() != CellType.STRING) {
                    continue;
                }
                String value = cell.getStringCellValue();
                if (value != null && value.contains("{{")) {
                    cell.setCellValue("");
                }
            }
        }
    }

    private void setTeacherSignature(Sheet sheet, PlanillaSheetData data) {
        if (data == null || data.profesorNombre() == null || data.profesorNombre().isBlank()) {
            return;
        }

        Row signatureRow = sheet.getRow(14);
        if (signatureRow == null) {
            return;
        }

        for (Cell cell : signatureRow) {
            if (cell == null) {
                continue;
            }
            String value = cell.getCellType() == CellType.STRING ? cell.getStringCellValue() : "";
            if (value != null && value.toLowerCase(Locale.ROOT).contains("firma del docente")) {
                cell.setCellValue(data.profesorNombre());
                return;
            }
        }
    }

    private void setNumericCell(Cell cell, Number value) {
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(SPANISH) + value.substring(1);
    }

    private String stripOrdinalSuffix(String value) {
        return value == null ? "" : value.replace("º", "").replace("°", "");
    }

    private record StageLayout(
            String templateSheetName,
            int firstMonthColumn,
            int totalGeneralColumn,
            int currentStageGradeColumn,
            int firstStageGradeColumn,
            int stageSumColumn,
            int finalAverageColumn,
            int complementaryColumn,
            int regularizationColumn) {
    }

    public record PlanillaSheetData(
            Planilla planilla,
            Curso curso,
            String disciplina,
            String profesorNombre,
            String turno,
            List<Tarea> tareas,
            List<StudentRow> rows,
            Map<Integer, Integer> firstStageGrades) {
    }
}
