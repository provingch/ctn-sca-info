package ctn.informatica.sca.util;

import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.util.Base64;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.CellRangeAddress;
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
            throw new IllegalStateException("La plantilla oficial solo soporta " + MONTH_BLOCK_COUNT + " meses con tareas por etapa. Se encontraron " + tareasPorMes.size() + " meses ocupados en la planilla " + data.planilla().getId() + ": " + describeMonths(tareasPorMes.keySet()));
        }

        Map<Integer, Integer> taskColumnById = allocateTaskColumns(tareasPorMes, layout, data.planilla().getId());
        resizeStudentArea(sheet, data.rows().size());
        replaceCommonMarkers(sheet, data);
        fillMonthBlocks(sheet, tareasPorMes, taskColumnById, layout);

        // Compute runtime positions for fixed-final columns (to the right of month blocks)
        int nextAvailable = layout.firstMonthColumn();
        for (Map.Entry<YearMonth, List<Tarea>> entry : tareasPorMes.entrySet()) {
            List<Tarea> tareasMes = entry.getValue();
            if (tareasMes == null || tareasMes.isEmpty()) continue;
            Integer firstTaskId = tareasMes.get(0).getId();
            Integer firstCol = taskColumnById.get(firstTaskId);
            if (firstCol == null) continue;
            int subtotalCol = firstCol + tareasMes.size();
            nextAvailable = Math.max(nextAvailable, subtotalCol + 1);
        }

        // Build month blocks info (first/last/subtotal columns) to use when writing student formulas
        java.util.List<MonthBlock> monthBlocks = new java.util.ArrayList<>();
        for (Map.Entry<YearMonth, List<Tarea>> entry : tareasPorMes.entrySet()) {
            List<Tarea> tareasMes = entry.getValue();
            if (tareasMes == null || tareasMes.isEmpty()) continue;
            Integer firstTaskId = tareasMes.get(0).getId();
            Integer firstCol = taskColumnById.get(firstTaskId);
            if (firstCol == null) continue;
            int lastInstrument = firstCol + tareasMes.size() - 1;
            int subtotalCol = firstCol + tareasMes.size();
            monthBlocks.add(new MonthBlock(firstCol, lastInstrument, subtotalCol));
        }

        ComputedLayout computed = new ComputedLayout(
            layout.firstMonthColumn(),
            nextAvailable,           // totalGeneralColumn
            nextAvailable + 1,       // currentStageGradeColumn
            layout.firstStageGradeColumn() >= 0 ? nextAvailable + 2 : -1, // firstStageGradeColumn
            nextAvailable + 3,       // stageSumColumn
            nextAvailable + 4,       // finalAverageColumn
            nextAvailable + 5,       // complementaryColumn
            nextAvailable + 6        // regularizationColumn
        );

        // Ensure final-column headers are written at their computed positions.
        // Read original header texts from template positions (if present) and
        // write them into the computed columns so labels follow the values.
        Row headerRow = getOrCreateRow(sheet, MONTH_HEADER_ROW);
        Row titleRow = getOrCreateRow(sheet, INSTRUMENT_TITLE_ROW);

        // helper to read template label safely
        java.util.function.IntFunction<String> readTemplateLabel = (int col) -> {
            if (col < 0) return null;
            Cell c = headerRow.getCell(col);
            if (c != null && c.getCellType() == CellType.STRING) {
                String v = c.getStringCellValue();
                return v == null ? null : v.trim();
            }
            return null;
        };

        // total general
        String totalLabel = readTemplateLabel.apply(layout.totalGeneralColumn());
        if (totalLabel == null || totalLabel.isBlank()) {
            totalLabel = "Total General";
        }
        setStringCell(getOrCreateCell(headerRow, computed.totalGeneralColumn()), totalLabel);
        sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, computed.totalGeneralColumn(), computed.totalGeneralColumn()));

        // current stage grade (label near total general in template)
        String currentStageLabel = readTemplateLabel.apply(layout.currentStageGradeColumn());
        if (currentStageLabel == null || currentStageLabel.isBlank()) {
            currentStageLabel = data.planilla().getEtapaIndex() == 2 ? "Calificación Final 2ª Etapa" : "Calificación Final 1º Etapa";
        }
        setStringCell(getOrCreateCell(headerRow, computed.currentStageGradeColumn()), currentStageLabel);
        sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, computed.currentStageGradeColumn(), computed.currentStageGradeColumn()));

        // first stage / extra columns (only for etapa 2)
        if (layout.firstStageGradeColumn() >= 0) {
            String firstStageLabel = readTemplateLabel.apply(layout.firstStageGradeColumn());
            if (firstStageLabel == null || firstStageLabel.isBlank()) {
                firstStageLabel = "Calificación Final 1º Etapa";
            }
            setStringCell(getOrCreateCell(headerRow, computed.firstStageGradeColumn()), firstStageLabel);
            sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, computed.firstStageGradeColumn(), computed.firstStageGradeColumn()));

            String stageSumLabel = readTemplateLabel.apply(layout.stageSumColumn());
            if (stageSumLabel == null || stageSumLabel.isBlank()) {
                stageSumLabel = "Subtotal Etapa";
            }
            setStringCell(getOrCreateCell(headerRow, computed.stageSumColumn()), stageSumLabel);
            sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, computed.stageSumColumn(), computed.stageSumColumn()));

            String finalAvgLabel = readTemplateLabel.apply(layout.finalAverageColumn());
            if (finalAvgLabel == null || finalAvgLabel.isBlank()) {
                finalAvgLabel = "Promedio Final";
            }
            setStringCell(getOrCreateCell(headerRow, computed.finalAverageColumn()), finalAvgLabel);
            sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, computed.finalAverageColumn(), computed.finalAverageColumn()));

            String compLabel = readTemplateLabel.apply(layout.complementaryColumn());
            if (compLabel == null || compLabel.isBlank()) {
                compLabel = "Complementaria";
            }
            setStringCell(getOrCreateCell(headerRow, computed.complementaryColumn()), compLabel);
            sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, computed.complementaryColumn(), computed.complementaryColumn()));

            String regLabel = readTemplateLabel.apply(layout.regularizationColumn());
            if (regLabel == null || regLabel.isBlank()) {
                regLabel = "Regularización";
            }
            setStringCell(getOrCreateCell(headerRow, computed.regularizationColumn()), regLabel);
            sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, computed.regularizationColumn(), computed.regularizationColumn()));
        }

        fillStudentRows(sheet, data, taskColumnById, computed, monthBlocks);
        clearTemplatePlaceholders(sheet);

        // Clean any residual template content to the right of our computed columns.
        // This removes stale static headers, formulas and merged regions left by
        // the original template beyond the last real column we use.
        cleanColumnsAfter(sheet, computed.regularizationColumn());

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
            if (tareasMes == null || tareasMes.isEmpty()) {
                continue; // omit empty months entirely
            }
            for (int taskIndex = 0; taskIndex < tareasMes.size(); taskIndex++) {
                mapping.put(tareasMes.get(taskIndex).getId(), nextColumn + taskIndex);
            }
            // advance by number of instrument columns + 1 subtotal column
            nextColumn += tareasMes.size() + 1;
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

        // Remove merged regions inherited from template that overlap header rows
        removeHeaderMerges(sheet, layout);

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

            if (tareasMes.isEmpty()) {
                // omit empty month completely
                continue;
            }

            int firstCol = currentColumn;

            for (int instrumentIndex = 0; instrumentIndex < tareasMes.size(); instrumentIndex++) {
                int colIndex = firstCol + instrumentIndex;
                Cell titleCell = getOrCreateCell(titleRow, colIndex);
                Cell tpCell = getOrCreateCell(tpRow, colIndex);
                Tarea tarea = tareasMes.get(instrumentIndex);
                titleCell.setCellValue(safeString(tarea.getTitulo()));
                setNumericCell(tpCell, tarea.getTotal());
                taskColumnById.put(tarea.getId(), colIndex);

                // set column width based on title length (clamped)
                if (sheet instanceof XSSFSheet) {
                    int minChars = 4;
                    int maxChars = 18;
                    int titleLen = tarea.getTitulo() == null ? 0 : tarea.getTitulo().length();
                    int desired = Math.max(minChars, Math.min(maxChars, (titleLen / 4) + 4));
                    ((XSSFSheet) sheet).setColumnWidth(colIndex, desired * 256);
                }
            }

            // Subtotal column immediately after instruments
            int subtotalCol = firstCol + tareasMes.size();
            setStringCell(monthHeaderRow, firstCol, monthLabel);
            int lastInstrumentCol = firstCol + tareasMes.size() - 1;
            // merge month header across instrument columns if it spans 2+ cols
            if (lastInstrumentCol > firstCol) {
                sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, MONTH_HEADER_ROW, firstCol, lastInstrumentCol));
            }
            // set and merge subtotal header vertically (header -> title row)
            setStringCell(getOrCreateCell(monthHeaderRow, subtotalCol), "Subtotal");
            sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, subtotalCol, subtotalCol));

            // advance to next available column (after subtotal)
            currentColumn = subtotalCol + 1;
        }
    }

    private void removeHeaderMerges(Sheet sheet, StageLayout layout) {
        if (!(sheet instanceof XSSFSheet)) return;
        XSSFSheet xssf = (XSSFSheet) sheet;
        java.util.List<CellRangeAddress> merges = xssf.getMergedRegions();
        for (int i = merges.size() - 1; i >= 0; i--) {
            CellRangeAddress ca = merges.get(i);
            // if the merged region intersects the header rows we use, and
            // the region starts at or after the first month column, remove it
            if (ca.getFirstRow() <= TP_ROW && ca.getLastRow() >= MONTH_HEADER_ROW
                    && ca.getFirstColumn() >= layout.firstMonthColumn()) {
                xssf.removeMergedRegion(i);
            }
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

    private void fillStudentRows(Sheet sheet, PlanillaSheetData data, Map<Integer, Integer> taskColumnById, ComputedLayout layout, java.util.List<MonthBlock> monthBlocks) {
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

            // Write subtotal formulas per month (SUM of instruments for that month)
            java.util.List<String> subtotalAddresses = new java.util.ArrayList<>();
            for (MonthBlock mb : monthBlocks) {
                String firstColRef = CellReference.convertNumToColString(mb.firstInstrumentCol());
                String lastColRef = CellReference.convertNumToColString(mb.lastInstrumentCol());
                int excelRowIndex = excelRow.getRowNum() + 1; // formulas use 1-based row numbers
                String range = firstColRef + excelRowIndex + ":" + lastColRef + excelRowIndex;
                Cell subtotalCell = getOrCreateCell(excelRow, mb.subtotalCol());
                subtotalCell.setCellFormula("SUM(" + range + ")");
                subtotalAddresses.add(CellReference.convertNumToColString(mb.subtotalCol()) + excelRowIndex);
            }

            // Write Total General as SUM of existing subtotals
            if (!subtotalAddresses.isEmpty()) {
                String totalFormula = "SUM(" + String.join(",", subtotalAddresses) + ")";
                Cell totalCell = getOrCreateCell(excelRow, layout.totalGeneralColumn());
                totalCell.setCellFormula(totalFormula);
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

    private record ComputedLayout(
            int firstMonthColumn,
            int totalGeneralColumn,
            int currentStageGradeColumn,
            int firstStageGradeColumn,
            int stageSumColumn,
            int finalAverageColumn,
            int complementaryColumn,
            int regularizationColumn) {
    }

        private record MonthBlock(
            int firstInstrumentCol,
            int lastInstrumentCol,
            int subtotalCol) {
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
        if (data == null) {
            return;
        }

        Cell targetCell = null;
        for (Row row : sheet) {
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                if (cell == null) {
                    continue;
                }
                if (cell.getCellType() != CellType.STRING) {
                    continue;
                }
                String value = cell.getStringCellValue();
                if (value != null && value.toLowerCase(Locale.ROOT).contains("firma del docente")) {
                    targetCell = cell;
                    break;
                }
            }
            if (targetCell != null) {
                break;
            }
        }

        if (targetCell == null) {
            return;
        }

        if (data.firmaImagen() != null && !data.firmaImagen().isBlank()) {
            try {
                insertSignatureImage(sheet, targetCell, data.firmaImagen());
                return;
            } catch (Exception ex) {
                targetCell.setCellValue("");
            }
        }

        if (data.profesorNombre() != null && !data.profesorNombre().isBlank()) {
            targetCell.setCellValue(data.profesorNombre());
        }
    }

    private void cleanColumnsAfter(Sheet sheet, int lastAllowedColumn) {
        if (sheet == null) return;

        // Remove merged regions that are entirely to the right of lastAllowedColumn
        if (sheet instanceof XSSFSheet) {
            XSSFSheet xssf = (XSSFSheet) sheet;
            java.util.List<CellRangeAddress> merges = xssf.getMergedRegions();
            for (int i = merges.size() - 1; i >= 0; i--) {
                CellRangeAddress ca = merges.get(i);
                if (ca.getFirstColumn() > lastAllowedColumn) {
                    xssf.removeMergedRegion(i);
                }
            }
        }

        // Prepare a neutral cell style to remove old formatting
        org.apache.poi.ss.usermodel.Workbook wb = sheet.getWorkbook();
        org.apache.poi.ss.usermodel.CellStyle neutral = wb.createCellStyle();

        int lastRow = sheet.getLastRowNum();
        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            short lastCellNum = row.getLastCellNum();
            if (lastCellNum <= 0) continue;
            for (int c = lastAllowedColumn + 1; c < lastCellNum; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) continue;
                // remove formula/state/value
                try {
                    cell.setBlank();
                } catch (Exception ex) {
                    // fallback: set empty string
                    cell.setCellValue("");
                }
                // reset style
                cell.setCellStyle(neutral);
            }
        }

        // Optionally shrink/hide any leftover columns to avoid visual remnants
        if (sheet instanceof XSSFSheet) {
            XSSFSheet xssf = (XSSFSheet) sheet;
            int maxColToCheck = Math.max(lastAllowedColumn + 1, 256); // check at least some range
            for (int col = lastAllowedColumn + 1; col <= maxColToCheck; col++) {
                try {
                    xssf.setColumnWidth(col, 1);
                    xssf.setColumnHidden(col, true);
                } catch (Exception ex) {
                    // ignore out-of-range columns
                }
            }
        }
    }

    private void insertSignatureImage(Sheet sheet, Cell targetCell, String firmaImagen) throws IOException {
        if (sheet == null || targetCell == null || firmaImagen == null || firmaImagen.isBlank()) {
            return;
        }

        String dataUrl = firmaImagen.trim();
        if (!dataUrl.startsWith("data:image/")) {
            throw new IOException("Firma inválida: no es una imagen base64");
        }

        String encoded = dataUrl.substring(dataUrl.indexOf(',') + 1);
        byte[] imageBytes = Base64.getDecoder().decode(encoded);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Firma inválida: no se pudo decodificar la imagen");
        }

        BufferedImage transparent = removeWhiteBackground(image);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(transparent, "PNG", out);

        int col = targetCell.getColumnIndex();
        int row = targetCell.getRowIndex();
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        CreationHelper helper = sheet.getWorkbook().getCreationHelper();
        ClientAnchor anchor = helper.createClientAnchor();
        anchor.setCol1(col + 1);
        anchor.setRow1(row);
        anchor.setCol2(col + 4);
        anchor.setRow2(row + 2);

        int pictureIndex = ((Workbook) sheet.getWorkbook()).addPicture(out.toByteArray(), Workbook.PICTURE_TYPE_PNG);
        drawing.createPicture(anchor, pictureIndex);
        targetCell.setCellValue("");
    }

    private BufferedImage removeWhiteBackground(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage processed = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = original.getRGB(x, y);
                Color color = new Color(argb, true);
                int alpha = color.getAlpha();
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();
                boolean isBackgroundLike = alpha < 230
                        || (red > 240 && green > 240 && blue > 240);
                if (isBackgroundLike) {
                    processed.setRGB(x, y, new Color(255, 255, 255, 0).getRGB());
                } else {
                    processed.setRGB(x, y, argb);
                }
            }
        }

        return processed;
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
            Map<Integer, Integer> firstStageGrades,
            String firmaImagen) {
    }
}
