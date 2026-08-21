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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PlanillaProcesoWorkbookBuilder.class);

    private static final String TEMPLATE_RESOURCE = "templates/PLANTILLA_PLANILLA_PROCESO_CTN.xlsx";
    private static final String LEGEND_SHEET = "LEYENDA_PARA_DESARROLLO";
    private static final int MONTH_BLOCK_COUNT = 5;
    private static final int FIXED_TASK_COLUMNS_PER_MONTH = 5;
    private static final int INSTRUMENTS_PER_MONTH = 12;
    private static final int MONTH_BLOCK_WIDTH = 13;
    private static final int INSTRUMENT_COLUMN_WIDTH_CHARS = 8;
    private static final int MIN_HEADER_COLUMNS = 15;
    private static final int MONTH_HEADER_ROW = 5;
    private static final int INSTRUMENT_TITLE_ROW = 6;
    private static final int TP_ROW = 7;
    private static final int FIRST_STUDENT_ROW = 8;
    private static final int TEMPLATE_STUDENT_COUNT = 3;
    // Ancho del bloque de firma en columnas (antes 4 -> abarcaba la columna
    // "Nombre" que mide 30 de ancho y hacía la línea desproporcionadamente
    // larga). Con 2 alcanza para la línea y el texto "Firma del Docente".
    private static final int SIGNATURE_SPAN_COLUMNS = 2;
    // Altura fija (en puntos) para las filas del bloque de firma, así la
    // imagen de la firma tiene espacio real y no se ve aplastada al forzarla
    // dentro de filas con la altura por defecto (15pt).
    private static final float SIGNATURE_TOP_ROW_HEIGHT = 55f;
    private static final float SIGNATURE_LINE_ROW_HEIGHT = 14f;
    private static final float SIGNATURE_LABEL_ROW_HEIGHT = 15f;
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
        // Read authoritative texts and styles from the original template sheet
        XSSFSheet templateSheet = (XSSFSheet) sheet.getWorkbook().getSheet(layout.templateSheetName());
        Row templateHeaderRow = templateSheet == null ? null : templateSheet.getRow(MONTH_HEADER_ROW);
        // Find the anchor column for the final-columns block (Total General)
        Integer templateTotalGeneralCol = null;
        if (templateHeaderRow != null) {
            short last = templateHeaderRow.getLastCellNum();
            for (int c = 0; c < last; c++) {
                Cell tc = templateHeaderRow.getCell(c);
                if (tc != null && tc.getCellType() == CellType.STRING) {
                    String v = tc.getStringCellValue();
                    if (v != null && v.trim().equalsIgnoreCase("Total General")) {
                        templateTotalGeneralCol = c;
                        break;
                    }
                }
            }
        }

        // Build ordered list of final-column definitions (text + template cell) starting at templateTotalGeneralCol
        java.util.List<String> finalColumnLabels = new java.util.ArrayList<>();
        java.util.List<Cell> finalColumnTemplateCells = new java.util.ArrayList<>();
        if (templateHeaderRow != null && templateTotalGeneralCol != null) {
            int last = templateHeaderRow.getLastCellNum();
            for (int c = templateTotalGeneralCol; c < last; c++) {
                Cell tc = templateHeaderRow.getCell(c);
                if (tc != null && tc.getCellType() == CellType.STRING) {
                    String v = tc.getStringCellValue();
                    if (v != null && !v.trim().isBlank()) {
                        finalColumnLabels.add(v.trim());
                        finalColumnTemplateCells.add(tc);
                    }
                }
            }
        }

        // Find a template cell for the "Subtotal" label to reuse style/text if available
        Cell subtotalTemplateCell = null;
        if (templateHeaderRow != null) {
            short last = templateHeaderRow.getLastCellNum();
            for (int c = 0; c < last; c++) {
                Cell tc = templateHeaderRow.getCell(c);
                if (tc != null && tc.getCellType() == CellType.STRING) {
                    String v = tc.getStringCellValue();
                    if (v != null && v.toLowerCase().contains("subtotal")) {
                        subtotalTemplateCell = tc;
                        break;
                    }
                }
            }
        }

        // Find template cell for signature label (e.g. "Firma del Docente")
        Cell signatureTemplateCell = null;
        if (templateSheet != null) {
            for (Row r : templateSheet) {
                for (Cell c : r) {
                    if (c != null && c.getCellType() == CellType.STRING) {
                        String v = c.getStringCellValue();
                        if (v != null && v.toLowerCase().contains("firma del docente")) {
                            signatureTemplateCell = c;
                            break;
                        }
                    }
                }
                if (signatureTemplateCell != null) break;
            }
        }
        List<Tarea> tareasEtapa = filterTasksByEtapa(data.tareas(), data.planilla().getEtapaIndex());
        int currentTotalPossiblePoints = totalPossiblePoints(tareasEtapa);
        data.planilla().computeGradeRanges(currentTotalPossiblePoints);

        Map<YearMonth, List<Tarea>> tareasPorMes = groupTasksByMonth(tareasEtapa);
        if (tareasPorMes.size() > MONTH_BLOCK_COUNT) {
            throw new IllegalStateException("La plantilla oficial solo soporta " + MONTH_BLOCK_COUNT + " meses con tareas por etapa. Se encontraron " + tareasPorMes.size() + " meses ocupados en la planilla " + data.planilla().getId() + ": " + describeMonths(tareasPorMes.keySet()));
        }

        Map<Integer, Integer> taskColumnById = allocateTaskColumns(tareasPorMes, layout, data.planilla().getId());
        // Ensure template has enough room to render reserved slots for months
        int requiredRightmost = layout.firstMonthColumn();
        for (Map.Entry<YearMonth, List<Tarea>> entry : tareasPorMes.entrySet()) {
            List<Tarea> tareasMes = entry.getValue();
            if (tareasMes == null || tareasMes.isEmpty()) continue;
            requiredRightmost += reservedSlotsForMonth(tareasMes) + 1; // instruments + subtotal
        }
        requiredRightmost = requiredRightmost - 1;
        if (layout.totalGeneralColumn() >= 0 && requiredRightmost >= layout.totalGeneralColumn()) {
            throw new IllegalStateException("La plantilla oficial no tiene suficiente ancho para reservar " + FIXED_TASK_COLUMNS_PER_MONTH + " columnas por mes para " + tareasPorMes.size() + " meses. Amplía la plantilla antes de generar la planilla.");
        }
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
            int subtotalCol = firstCol + reservedSlotsForMonth(tareasMes);
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
            int subtotalCol = firstCol + reservedSlotsForMonth(tareasMes);
            monthBlocks.add(new MonthBlock(firstCol, lastInstrument, subtotalCol));
        }

        // Hide any trailing empty columns immediately to the right of the
        // last instrument column so downstream logic/tests observe a clean
        // boundary of visible instrument columns. Avoid hiding columns that
        // belong to header merged regions so we don't cut header text prematurely.
        if (!monthBlocks.isEmpty() && sheet instanceof XSSFSheet) {
            int lastInstrumentGlobal = monthBlocks.stream().mapToInt(mb -> mb.lastInstrumentCol()).max().orElse(layout.firstMonthColumn() - 1);
            XSSFSheet xs = (XSSFSheet) sheet;
            // compute the rightmost column used by any merged region that intersects the header rows
            int lastHeaderMergeCol = layout.firstMonthColumn() - 1;
            java.util.List<CellRangeAddress> merges = xs.getMergedRegions();
            for (CellRangeAddress ca : merges) {
                if (ca.getFirstRow() <= TP_ROW && ca.getLastRow() >= 0) {
                    lastHeaderMergeCol = Math.max(lastHeaderMergeCol, ca.getLastColumn());
                }
            }
            int startHide = Math.max(lastInstrumentGlobal, lastHeaderMergeCol) + 1;
            final int MAX_SCAN = startHide + 120; // upper safety bound
            for (int c = startHide; c < MAX_SCAN; c++) {
                boolean hasContent = false;
                for (int r : new int[]{MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, TP_ROW}) {
                    Row rr = sheet.getRow(r);
                    if (rr == null) continue;
                    Cell cc = rr.getCell(c);
                    if (cc != null && cc.getCellType() == CellType.STRING) {
                        String vv = cc.getStringCellValue();
                        if (vv != null && !vv.isBlank()) { hasContent = true; break; }
                    }
                }
                if (!hasContent) {
                    xs.setColumnHidden(c, true);
                } else {
                    break;
                }
            }
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

        // final columns: write labels and clone styles from template's row 6 entries (if found)
        org.apache.poi.ss.usermodel.Workbook wb = sheet.getWorkbook();
        if (!finalColumnLabels.isEmpty()) {
            for (int i = 0; i < finalColumnLabels.size(); i++) {
                int colIndex = computed.totalGeneralColumn + i;
                String label = finalColumnLabels.get(i);
                setStringCell(getOrCreateCell(headerRow, colIndex), label);
                // clone style from template cell if available
                if (i < finalColumnTemplateCells.size() && finalColumnTemplateCells.get(i) != null) {
                    Cell tmpl = finalColumnTemplateCells.get(i);
                    org.apache.poi.ss.usermodel.CellStyle newStyle = wb.createCellStyle();
                    try {
                        newStyle.cloneStyleFrom(tmpl.getCellStyle());
                        getOrCreateCell(headerRow, colIndex).setCellStyle(newStyle);
                    } catch (Exception ignore) {
                        // ignore cloning issues and continue
                    }
                }
                sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, colIndex, colIndex));
            }
        } else {
            // fallback to previous behavior using template positions
            String totalLabel = readTemplateLabel.apply(layout.totalGeneralColumn());
            if (totalLabel == null || totalLabel.isBlank()) {
                totalLabel = "Total General";
            }
            setStringCell(getOrCreateCell(headerRow, computed.totalGeneralColumn()), totalLabel);
            sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, computed.totalGeneralColumn(), computed.totalGeneralColumn()));

            String currentStageLabel = readTemplateLabel.apply(layout.currentStageGradeColumn());
            if (currentStageLabel == null || currentStageLabel.isBlank()) {
                currentStageLabel = data.planilla().getEtapaIndex() == 2 ? "Calificación Final 2ª Etapa" : "Calificación Final 1º Etapa";
            }
            setStringCell(getOrCreateCell(headerRow, computed.currentStageGradeColumn()), currentStageLabel);
            sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, computed.currentStageGradeColumn(), computed.currentStageGradeColumn()));

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
        }

        fillStudentRows(sheet, data, taskColumnById, computed, monthBlocks);
        clearTemplatePlaceholders(sheet);

        // Compute the signature row to place the teacher signature a couple of
        // rows below the last student row so it's always inside the area we
        // preserve from cleanColumnsAfter(). Place the signature in a fixed
        // early column (column index 1) to avoid landing beyond lastRealColumn.
        int lastStudentRow = FIRST_STUDENT_ROW + Math.max(0, data.rows() == null ? 0 : data.rows().size()) - 1;
        int signatureRow = lastStudentRow + 3; // three rows below last student (extra margin)

        // Determine the last column that was actually written for this planilla
        // instance: if the layout declares first-stage columns (etapa 2) we
        // consider the regularization column; otherwise the current stage
        // grade column is the last real column for etapa 1. Clean everything
        // to the right of that column so we don't leave template styling
        // remnants on sheets that don't use the full theoretical layout.
        int lastRealColumn = layout.firstStageGradeColumn() >= 0
            ? computed.regularizationColumn()
            : computed.currentStageGradeColumn();
        int signatureColumn = 0;
        setTeacherSignature(sheet, data, signatureRow, signatureColumn);

        if (sheet instanceof XSSFSheet) {
            resizeHeaderBanner((XSSFSheet) sheet, layout, lastRealColumn);
        }

        cleanColumnsAfter(sheet, lastRealColumn, signatureRow, signatureColumn);
    }

    /**
     * Resize and re-merge the header banner and info blocks so they fit the
     * actual table width (lastRealColumn). This prevents the fixed-template
     * merges from leaving large empty areas or being cut by column-hiding.
     */
    private void resizeHeaderBanner(XSSFSheet sheet, StageLayout layout, int lastRealColumn) {
        if (sheet == null || layout == null) return;
        org.apache.poi.ss.usermodel.Workbook wb = sheet.getWorkbook();

        int minCols = Math.max(MIN_HEADER_COLUMNS, layout.firstMonthColumn() + 10);
        int targetLastCol = Math.max(lastRealColumn, minCols);

        // Remove existing merges that occupy the header rows (0..4)
        java.util.List<CellRangeAddress> merges = sheet.getMergedRegions();
        for (int i = merges.size() - 1; i >= 0; i--) {
            CellRangeAddress ca = merges.get(i);
            if (ca.getFirstRow() <= 4 && ca.getLastRow() >= 0) {
                sheet.removeMergedRegion(i);
            }
        }

        // Restaurar la fusión fija de las etiquetas Disciplina/Profesor (A:B,
        // filas 4 y 5 en la representación 1-based de Excel -> 3 y 4 0-based)
        // El bucle anterior las borra junto con el resto de merges del header,
        // por lo que debemos volver a crearlas aquí.
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 1));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 1));

        // Re-merge title rows (0..2) from column 0 to targetLastCol and adapt font
        for (int titleRow = 0; titleRow <= 2; titleRow++) {
            sheet.addMergedRegion(new CellRangeAddress(titleRow, titleRow, 0, targetLastCol));
            Cell c = getOrCreateCell(getOrCreateRow(sheet, titleRow), 0);
            int availableChars = (targetLastCol - 0 + 1) * INSTRUMENT_COLUMN_WIDTH_CHARS;
            applyAdaptiveFontSizeToFitWidth(wb, c, c.getCellType() == CellType.STRING ? c.getStringCellValue() : c.getStringCellValue(), availableChars);
        }

        // Recalculate and place specialty / course / year blocks on rows 3 and 4
        // Fallback original template extents (C:N -> 2..13, C:R -> 2..17, T:V -> 19..21)
        int specOrigEnd = 13;
        int courseOrigEnd = 17;
        int yearOrigStart = 19;
        int yearOrigEnd = 21;

        // Discover template merges for rows 3 and 4 to get original extents if present
        java.util.List<CellRangeAddress> currentMerges = sheet.getMergedRegions();
        for (CellRangeAddress ca : currentMerges) {
            if (ca.getFirstRow() == 3) {
                if (ca.getFirstColumn() >= 2) specOrigEnd = Math.max(specOrigEnd, ca.getLastColumn());
            }
            if (ca.getFirstRow() == 4) {
                if (ca.getFirstColumn() >= 2 && ca.getLastColumn() <= 18) courseOrigEnd = Math.max(courseOrigEnd, ca.getLastColumn());
                if (ca.getFirstColumn() >= yearOrigStart) { yearOrigStart = Math.min(yearOrigStart, ca.getFirstColumn()); yearOrigEnd = Math.max(yearOrigEnd, ca.getLastColumn()); }
            }
        }

        int newSpecEnd = Math.min(specOrigEnd, targetLastCol);
        int newCourseEnd = Math.min(courseOrigEnd, targetLastCol);

        // If year area falls beyond targetLastCol, reposition it immediately after course block
        int newYearStart = yearOrigStart;
        int newYearEnd = Math.min(yearOrigEnd, targetLastCol);
        if (newYearStart > targetLastCol) {
            newYearStart = newCourseEnd + 1;
            newYearStart = Math.min(newYearStart, targetLastCol);
            int width = Math.max(4, yearOrigEnd - yearOrigStart + 1);
            newYearEnd = Math.min(targetLastCol, newYearStart + width - 1);
        }

        // Apply merges for row 3 (Especialidad)
        int specStart = 2;
        if (newSpecEnd >= specStart) {
            if (newSpecEnd > specStart) {
                sheet.addMergedRegion(new CellRangeAddress(3, 3, specStart, newSpecEnd));
            }
            Cell sc = getOrCreateCell(getOrCreateRow(sheet, 3), specStart);
            int avail = (newSpecEnd - specStart + 1) * INSTRUMENT_COLUMN_WIDTH_CHARS;
            applyAdaptiveFontSizeToFitWidth(wb, sc, sc.getCellType() == CellType.STRING ? sc.getStringCellValue() : sc.getStringCellValue(), avail);
        }

        // Apply merges for row 4 (Curso/Turno/Seccion and Año)
        int courseStart = 2;
        if (newCourseEnd >= courseStart) {
            if (newCourseEnd > courseStart) {
                sheet.addMergedRegion(new CellRangeAddress(4, 4, courseStart, newCourseEnd));
            }
            Cell cc = getOrCreateCell(getOrCreateRow(sheet, 4), courseStart);
            int avail = (newCourseEnd - courseStart + 1) * INSTRUMENT_COLUMN_WIDTH_CHARS;
            applyAdaptiveFontSizeToFitWidth(wb, cc, cc.getCellType() == CellType.STRING ? cc.getStringCellValue() : cc.getStringCellValue(), avail);
        }

        if (newYearEnd >= newYearStart) {
            if (newYearEnd > newYearStart) {
                sheet.addMergedRegion(new CellRangeAddress(4, 4, newYearStart, newYearEnd));
            }
            Cell yc = getOrCreateCell(getOrCreateRow(sheet, 4), newYearStart);
            int avail = (newYearEnd - newYearStart + 1) * INSTRUMENT_COLUMN_WIDTH_CHARS;
            applyAdaptiveFontSizeToFitWidth(wb, yc, yc.getCellType() == CellType.STRING ? yc.getStringCellValue() : yc.getStringCellValue(), avail);
        }
    }

    private int monthBlockWidth(List<Tarea> tareasMes) {
        int actualTasks = tareasMes == null ? 0 : tareasMes.size();
        return Math.max(2, actualTasks + 2);
    }

    private int reservedSlotsForMonth(List<Tarea> tareasMes) {
        int actual = tareasMes == null ? 0 : tareasMes.size();
        return Math.max(FIXED_TASK_COLUMNS_PER_MONTH, actual);
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
            // advance by reserved instrument columns (may be >= actual tasks) + 1 subtotal column
            nextColumn += reservedSlotsForMonth(tareasMes) + 1;
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
        // Track longest instrument title so we can set an appropriate row height
        int maxTitleLen = 0;
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
                int tlen = tarea.getTitulo() == null ? 0 : tarea.getTitulo().length();
                maxTitleLen = Math.max(maxTitleLen, tlen);
                setNumericCell(tpCell, tarea.getTotal());
                taskColumnById.put(tarea.getId(), colIndex);

                // fixed column width — font size scales down instead (see applyTitleFontSize)
                if (sheet instanceof XSSFSheet) {
                    ((XSSFSheet) sheet).setColumnWidth(colIndex, INSTRUMENT_COLUMN_WIDTH_CHARS * 256);
                }
                int titleLen = tarea.getTitulo() == null ? 0 : tarea.getTitulo().length();
                applyTitleFontSize(sheet.getWorkbook(), titleCell, titleLen);
            }

            // Fill remaining reserved slots (if any) with blank title/TP cells and set a minimal column width
            int reserved = reservedSlotsForMonth(tareasMes);
            for (int instrumentIndex = tareasMes.size(); instrumentIndex < reserved; instrumentIndex++) {
                int colIndex = firstCol + instrumentIndex;
                getOrCreateCell(titleRow, colIndex).setBlank();
                getOrCreateCell(tpRow, colIndex).setBlank();
                if (sheet instanceof XSSFSheet) {
                    ((XSSFSheet) sheet).setColumnWidth(colIndex, INSTRUMENT_COLUMN_WIDTH_CHARS * 256);
                }
            }

            // Subtotal column immediately after instruments
            int subtotalCol = firstCol + reservedSlotsForMonth(tareasMes);
            setStringCell(monthHeaderRow, firstCol, monthLabel);
            int lastInstrumentCol = firstCol + reservedSlotsForMonth(tareasMes) - 1;
            // merge month header across instrument columns if it spans 2+ cols
            if (lastInstrumentCol > firstCol) {
                sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, MONTH_HEADER_ROW, firstCol, lastInstrumentCol));
            }
            int monthBlockWidthChars = reservedSlotsForMonth(tareasMes) * INSTRUMENT_COLUMN_WIDTH_CHARS;
            applyAdaptiveFontSizeToFitWidth(sheet.getWorkbook(), getOrCreateCell(monthHeaderRow, firstCol), monthLabel, monthBlockWidthChars);
            // set and merge subtotal header vertically (header -> title row)
            setStringCell(getOrCreateCell(monthHeaderRow, subtotalCol), "Subtotal");
            sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, INSTRUMENT_TITLE_ROW, subtotalCol, subtotalCol));

            // advance to next available column (after subtotal)
            currentColumn = subtotalCol + 1;
        }

        // After populating all instrument titles, set a dynamic row height for the
        // INSTRUMENT_TITLE_ROW based on the longest title found so short titles do
        // not keep an excessively tall fixed height from the template.
        Row titleRowGlobal = getOrCreateRow(sheet, INSTRUMENT_TITLE_ROW);
        if (maxTitleLen <= 0) {
            // keep template default if no titles
        } else {
            float minHeight = 60f; // points
            float maxHeight = 110f; // points
            int clampLen = Math.max(1, Math.min(40, maxTitleLen));
            float ratio = Math.min(1f, clampLen / 40f);
            float desired = minHeight + (maxHeight - minHeight) * ratio;
            titleRowGlobal.setHeightInPoints(desired);
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

    private void setTeacherSignature(Sheet sheet, PlanillaSheetData data, int targetRowIndex, int targetColumnIndex) {
        if (data == null) {
            return;
        }
        

        // Rows for the 3-line signature block
        Row topRow = getOrCreateRow(sheet, targetRowIndex);
        Row lineRow = getOrCreateRow(sheet, targetRowIndex + 1);
        Row labelRow = getOrCreateRow(sheet, targetRowIndex + 2);

        // Altura explícita: sin esto, las filas usan la altura por defecto
        // (15pt) y la imagen de firma queda comprimida verticalmente.
        topRow.setHeightInPoints(SIGNATURE_TOP_ROW_HEIGHT);
        lineRow.setHeightInPoints(SIGNATURE_LINE_ROW_HEIGHT);
        labelRow.setHeightInPoints(SIGNATURE_LABEL_ROW_HEIGHT);

        Cell topCell = getOrCreateCell(topRow, targetColumnIndex);

        boolean imageInserted = false;
        if (data.firmaImagen() != null && !data.firmaImagen().isBlank()) {
            try {
                insertSignatureImage(sheet, topCell, data.firmaImagen());
                imageInserted = true;
            } catch (Exception ex) {
                // Log a warning with context so the failure can be diagnosed
                try {
                    String pid = data.planilla() != null ? String.valueOf(data.planilla().getId()) : "?";
                    log.warn("No se pudo insertar la imagen de firma para planilla {}: {}", pid, ex.getMessage(), ex);
                } catch (Exception ignore) {
                    // swallow logging errors to avoid masking original exception
                }
                topCell.setCellValue("");
            }
        }

        if (!imageInserted) {
            if (data.profesorNombre() != null && !data.profesorNombre().isBlank()) {
                topCell.setCellValue(data.profesorNombre());
            }
        }

        // Create a thin bottom border across several columns to act as signature line
        org.apache.poi.ss.usermodel.Workbook wb = sheet.getWorkbook();
        org.apache.poi.ss.usermodel.CellStyle lineStyle = wb.createCellStyle();
        lineStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);

        int spanStart = targetColumnIndex;
        int spanEnd = targetColumnIndex + SIGNATURE_SPAN_COLUMNS;
        for (int c = spanStart; c <= spanEnd; c++) {
            Cell cell = getOrCreateCell(lineRow, c);
            cell.setCellStyle(lineStyle);
        }

        // Label beneath the line
        Cell labelCell = getOrCreateCell(labelRow, targetColumnIndex);
        labelCell.setCellValue("Firma del Docente");
    }

    private void cleanColumnsAfter(Sheet sheet, int lastAllowedColumn, int firstRowToProtect, int signatureStartCol) {
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
            // For rows that are part of the signature block, only protect the
            // specific signature columns; all other cells in those rows should
            // still be cleared if they are to the right of lastAllowedColumn.
            short lastCellNum = row.getLastCellNum();
            if (lastCellNum <= 0) continue;
            for (int c = lastAllowedColumn + 1; c < lastCellNum; c++) {
                // Skip clearing the small signature cell area (span of a few cols)
                if ((r == firstRowToProtect || r == firstRowToProtect + 1 || r == firstRowToProtect + 2)
                    && c >= signatureStartCol && c <= signatureStartCol + SIGNATURE_SPAN_COLUMNS) {
                    continue;
                }
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

        // Do not change column widths or hidden state: collapsing columns
        // caused merged header areas to visually truncate in generated files.
        // Leave column sizing as defined by the template or earlier logic so
        // merged headers retain their intended width.
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
        anchor.setCol1(col);
        anchor.setRow1(row);
        // Mismo ancho que la línea de firma (SIGNATURE_SPAN_COLUMNS) y
        // limitado a las 2 filas reales del bloque (topRow + lineRow), para
        // que la caja del anchor no quede desproporcionada respecto a la
        // imagen y esta no se vea aplastada.
        anchor.setCol2(col + SIGNATURE_SPAN_COLUMNS);
        anchor.setRow2(row + 2);

        int pictureIndex = ((Workbook) sheet.getWorkbook()).addPicture(out.toByteArray(), Workbook.PICTURE_TYPE_PNG);
        drawing.createPicture(anchor, pictureIndex);
        targetCell.setCellValue("");
    }

    private BufferedImage removeWhiteBackground(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage processed = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // More forgiving thresholds to avoid removing anti-aliased strokes.
        final int ALPHA_THRESHOLD = 50; // allow semi-transparent pixels to stay
        final int RGB_THRESHOLD = 235; // consider near-white as background

        // Convert based on distance to white instead of strict per-channel checks
        long nonTransparentCount = 0L;
        long total = (long) width * (long) height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = original.getRGB(x, y);
                Color color = new Color(argb, true);
                int alpha = color.getAlpha();
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();

                boolean isMostlyWhite = (red >= RGB_THRESHOLD && green >= RGB_THRESHOLD && blue >= RGB_THRESHOLD);
                boolean lowAlpha = alpha <= ALPHA_THRESHOLD;

                if (isMostlyWhite && lowAlpha) {
                    // treat as transparent
                    processed.setRGB(x, y, new Color(255, 255, 255, 0).getRGB());
                } else if (isMostlyWhite && alpha <= 255) {
                    // if near-white but fully opaque, still make transparent to remove paper background
                    processed.setRGB(x, y, new Color(255, 255, 255, 0).getRGB());
                } else {
                    processed.setRGB(x, y, argb);
                    if ((processed.getRGB(x, y) >>> 24) != 0) nonTransparentCount++;
                }
            }
        }

        // If after background removal almost all pixels are transparent, consider the image invalid
        double nonTransparentRatio = total == 0 ? 0.0 : ((double) nonTransparentCount) / (double) total;
        if (nonTransparentRatio < 0.005) { // less than 0.5% pixels remain
            log.warn("removeWhiteBackground produced an almost-empty image (nonTransparentRatio={}). Treating as empty.", nonTransparentRatio);
            // return an empty transparent image
            BufferedImage empty = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            return empty;
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

    /**
     * Reduces the font size on the given cell as the title text grows longer,
     * so long titles still fit visually now that column width is fixed.
     * Clones the cell's current style so other formatting (rotation, borders,
     * alignment) inherited from the template is preserved.
     */
    private void applyTitleFontSize(org.apache.poi.ss.usermodel.Workbook wb, Cell cell, int titleLen) {
        int maxFontSize = 10;
        int minFontSize = 7;
        int shortThreshold = 12;   // titles at or under this length keep maxFontSize
        int longThreshold = 40;    // titles at or over this length get minFontSize

        int clampLen = Math.max(shortThreshold, Math.min(longThreshold, titleLen));
        double ratio = (double) (clampLen - shortThreshold) / (longThreshold - shortThreshold);
        int fontSize = (int) Math.round(maxFontSize - (maxFontSize - minFontSize) * ratio);

        org.apache.poi.ss.usermodel.CellStyle existingStyle = cell.getCellStyle();
        org.apache.poi.ss.usermodel.Font existingFont = wb.getFontAt(existingStyle.getFontIndexAsInt());

        org.apache.poi.ss.usermodel.Font scaledFont = wb.createFont();
        scaledFont.setFontName(existingFont.getFontName());
        scaledFont.setBold(existingFont.getBold());
        scaledFont.setItalic(existingFont.getItalic());
        scaledFont.setColor(existingFont.getColor());
        scaledFont.setFontHeightInPoints((short) fontSize);

        org.apache.poi.ss.usermodel.CellStyle newStyle = wb.createCellStyle();
        newStyle.cloneStyleFrom(existingStyle);
        newStyle.setFont(scaledFont);
        cell.setCellStyle(newStyle);
    }

    /**
     * Shrinks the font on a cell so its text visually fits within the given
     * available width (in Excel column-width "characters"), instead of relying
     * on a fixed font size that may overflow narrower merged cells. Used for
     * the month header label, whose merged width varies with the number of
     * reserved instrument columns for that month.
     */
    private void applyAdaptiveFontSizeToFitWidth(org.apache.poi.ss.usermodel.Workbook wb, Cell cell, String text, int availableWidthChars) {
        int baseFontSize = 11;
        int minFontSize = 7;
        int textLen = text == null ? 0 : text.length();
        if (textLen <= 0 || availableWidthChars <= 0) return;

        // Rough heuristic: one Excel column-width "char" unit approximates one
        // default-size character. If the label is longer than the available
        // width, shrink the font proportionally so it still fits.
        double ratio = (double) availableWidthChars / textLen;
        int fontSize = (int) Math.round(Math.min(baseFontSize, baseFontSize * ratio));
        fontSize = Math.max(minFontSize, fontSize);

        org.apache.poi.ss.usermodel.CellStyle existingStyle = cell.getCellStyle();
        org.apache.poi.ss.usermodel.Font existingFont = wb.getFontAt(existingStyle.getFontIndexAsInt());

        org.apache.poi.ss.usermodel.Font scaledFont = wb.createFont();
        scaledFont.setFontName(existingFont.getFontName());
        scaledFont.setBold(existingFont.getBold());
        scaledFont.setItalic(existingFont.getItalic());
        scaledFont.setColor(existingFont.getColor());
        scaledFont.setFontHeightInPoints((short) fontSize);

        org.apache.poi.ss.usermodel.CellStyle newStyle = wb.createCellStyle();
        newStyle.cloneStyleFrom(existingStyle);
        newStyle.setFont(scaledFont);
        cell.setCellStyle(newStyle);
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
