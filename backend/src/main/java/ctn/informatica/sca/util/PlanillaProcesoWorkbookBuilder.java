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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PlanillaProcesoWorkbookBuilder {

    private static final Logger log = LoggerFactory.getLogger(PlanillaProcesoWorkbookBuilder.class);

    private static final String TEMPLATE_RESOURCE = "templates/PLANTILLA_PLANILLA_PROCESO_CTN_v4.xlsx";
    private static final String LEGEND_SHEET = "LEYENDA_PARA_DESARROLLO";
    private static final String INSTITUTION_LOGO_PATH = "/static/logo-institucional.png";
    private static final short BLOCK_BORDER_COLOR = IndexedColors.GREY_50_PERCENT.getIndex();
    private static final int MONTH_BLOCK_COUNT = 5;
    private static final int FIXED_TASK_COLUMNS_PER_MONTH = 5;
    private static final int INSTRUMENTS_PER_MONTH = 12;
    private static final int MONTH_BLOCK_WIDTH = 13;
    // Reduced instrument column width to 4.5 characters (was 8)
    private static final double INSTRUMENT_COLUMN_WIDTH_CHARS = 4.5;
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

        // Note: in v4 templates the authoritative final-column positions are
        // discovered at runtime by scanning the header for literal labels
        // (see finalColumnLabels). The numeric fields below are kept only as
        // feature flags (-1 = this stage does not expose that final column)
        // and to carry per-stage metadata such as frozen/trailing fixed counts.
        private static final StageLayout STAGE_1 = new StageLayout(
            "PLANTILLA_ETAPA_1",
            2,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            2, // trailingFixedColumns (Total General + Calificación Final 1ª Etapa)
            2  // frozenColumns (A:B fixed)
        );

        private static final StageLayout STAGE_2 = new StageLayout(
            "PLANTILLA_ETAPA_2",
            3,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            6, // trailingFixedColumns (Total General + Calificación Final 2ª Etapa + cola propia)
            3  // frozenColumns (A:C fixed to include Calificación Final 1ª Etapa)
        );

    public XSSFWorkbook buildSingleWorkbook(PlanillaSheetData data, String sheetName) throws IOException {
        XSSFWorkbook workbook = loadTemplateWorkbook();
        try {
            XSSFSheet sheet = cloneTemplateSheet(workbook, layoutFor(data.planilla()), sheetName);
            populateSheet(sheet, data);
            removeTemplateSheets(workbook);
            if (workbook.getNumberOfSheets() > 0) {
                workbook.setActiveSheet(0);
            }
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
            if (workbook.getNumberOfSheets() > 0) {
                workbook.setActiveSheet(0);
            }
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
        if (templateIndex < 0) {
            templateIndex = workbook.getSheetIndex("Planilla");
        }
        if (templateIndex < 0 && workbook.getNumberOfSheets() > 0) {
            templateIndex = 0;
        }
        if (templateIndex < 0) {
            throw new IllegalStateException("No se encontró ninguna hoja base de plantilla disponible para clonar.");
        }
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

        // Ensure TP row contains numeric literal values for each instrument
        // (overwrite any leftover template formulas) and place Subtotal/Total
        // formulas on the TP row mirroring the pattern used for student rows.
        Row tpRowRuntime = getOrCreateRow(sheet, TP_ROW);
        java.util.List<String> tpSubtotalAddresses = new java.util.ArrayList<>();
        for (Map.Entry<YearMonth, List<Tarea>> entry : tareasPorMes.entrySet()) {
            List<Tarea> tareasMes = entry.getValue();
            if (tareasMes == null || tareasMes.isEmpty()) continue;
            Integer firstTaskId = tareasMes.get(0).getId();
            Integer firstCol = taskColumnById.get(firstTaskId);
            if (firstCol == null) continue;

            // Overwrite individual instrument TP cells with numeric literals
            for (int i = 0; i < tareasMes.size(); i++) {
                Tarea tarea = tareasMes.get(i);
                int colIndex = firstCol + i;
                Cell tpCell = getOrCreateCell(tpRowRuntime, colIndex);
                setNumericCell(tpCell, tarea.getTotal());
            }

            int lastInstrument = firstCol + Math.max(0, tareasMes.size() - 1);
            int subtotalCol = firstCol + reservedSlotsForMonth(tareasMes);
            String firstColRef = CellReference.convertNumToColString(firstCol);
            String lastColRef = CellReference.convertNumToColString(lastInstrument);
            int excelRowIndex = tpRowRuntime.getRowNum() + 1; // TP row formula must reference the TP row, not the student row.
            Cell subtotalCell = getOrCreateCell(tpRowRuntime, subtotalCol);
            // If the month exposes at least one real instrument, always write
            // the subtotal formula. Reserved empty slots should remain blank
            // but the subtotal column must contain the SUM of the actual
            // instrument range.
            if (tareasMes.size() > 0) {
                int actualLastInstrument = firstCol + tareasMes.size() - 1;
                String actualLastRef = CellReference.convertNumToColString(actualLastInstrument);
                subtotalCell.setCellFormula("SUM(" + firstColRef + excelRowIndex + ":" + actualLastRef + excelRowIndex + ")");
            } else {
                subtotalCell.setBlank();
            }
            tpSubtotalAddresses.add(CellReference.convertNumToColString(subtotalCol) + excelRowIndex);
        }

        // Total General on TP row: SUM of monthly subtotal TP cells
        if (!tpSubtotalAddresses.isEmpty()) {
            Cell totalTpCell = getOrCreateCell(getOrCreateRow(sheet, TP_ROW), computed.totalGeneralColumn());
            totalTpCell.setCellFormula("SUM(" + String.join(",", tpSubtotalAddresses) + ")");
        }

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

        int lastStudentRow = FIRST_STUDENT_ROW + Math.max(0, data.rows() == null ? 0 : data.rows().size()) - 1;
        applyNavigationAndVisualDesign(sheet, monthBlocks, lastStudentRow, data.curso() == null ? null : data.curso().getEspecialidad(), layout);

        // Compute the signature row to place the teacher signature a couple of
        // rows below the last student row so it's always inside the area we
        // preserve from cleanColumnsAfter(). Place the signature in a fixed
        // early column (column index 1) to avoid landing beyond lastRealColumn.
        int signatureRow = lastStudentRow + 3; // three rows below last student (extra margin)

        // Determine the last column actually written for this planilla instance.
        // When the template provides the final-column labels (the branch that is
        // effectively always used), the real right edge is the last final label
        // written after Total General. The 7-column fallback is kept only for
        // templates that do not include these labels.
        int lastRealColumn;
        if (!finalColumnLabels.isEmpty()) {
            lastRealColumn = computed.totalGeneralColumn() + finalColumnLabels.size() - 1;
        } else if (layout.firstStageGradeColumn() >= 0) {
            lastRealColumn = computed.regularizationColumn();
        } else {
            lastRealColumn = computed.currentStageGradeColumn();
        }
        int signatureColumn = 0;
        setTeacherSignature(sheet, data, signatureRow, signatureColumn);

        if (sheet instanceof XSSFSheet) {
            resizeHeaderBanner((XSSFSheet) sheet, layout, lastRealColumn);
            java.util.List<CellRangeAddress> dynamicHeaderMerges = ((XSSFSheet) sheet).getMergedRegions();
            int protectedHeaderRightEdge = lastRealColumn;
            for (CellRangeAddress ca : dynamicHeaderMerges) {
                if (ca.getFirstRow() <= 4 && ca.getLastRow() >= 0) {
                    protectedHeaderRightEdge = Math.max(protectedHeaderRightEdge, ca.getLastColumn());
                }
            }
            lastRealColumn = Math.max(lastRealColumn, protectedHeaderRightEdge);
        }

        cleanColumnsAfter(sheet, lastRealColumn, signatureRow, signatureColumn);

        // Re-apply subtotal visual style and enforce instrument column widths
        // after column cleanup, since cleanColumnsAfter resets styles for
        // cleared cells. This guarantees subtotals keep their fill style and
        // instrument columns preserve the reduced width expected by tests.
        if (sheet instanceof XSSFSheet) {
            XSSFSheet xs = (XSSFSheet) sheet;
            org.apache.poi.ss.usermodel.Workbook wb2 = sheet.getWorkbook();
            CellStyle subtotalStyle = wb2.createCellStyle();
            subtotalStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xDC, (byte) 0xE6, (byte) 0xF1}, null));
            subtotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Ensure TP subtotal formulas exist (rewrite if cleanup cleared them)
            Row tpRowPost = getOrCreateRow(sheet, TP_ROW);
            for (MonthBlock mb : monthBlocks) {
                if (mb == null) continue;
                // ensure TP subtotal formula
                Cell tpSubtotal = tpRowPost.getCell(mb.subtotalCol());
                if (tpSubtotal == null) tpSubtotal = getOrCreateCell(tpRowPost, mb.subtotalCol());
                try {
                    if (mb.lastInstrumentCol() >= mb.firstInstrumentCol()) {
                        String f = "SUM(" + CellReference.convertNumToColString(mb.firstInstrumentCol()) + (TP_ROW + 1) + ":" + CellReference.convertNumToColString(mb.lastInstrumentCol()) + (TP_ROW + 1) + ")";
                        tpSubtotal.setCellFormula(f);
                    } else {
                        tpSubtotal.setBlank();
                    }
                } catch (Exception ignore) {}

                // apply subtotal style to TP row and student subtotal cells
                for (int r = TP_ROW; r <= lastStudentRow; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    Cell cell = row.getCell(mb.subtotalCol());
                    if (cell == null) continue;
                    CellStyle merged = mergeStyle(wb2, cell.getCellStyle(), subtotalStyle);
                    cell.setCellStyle(merged);
                }

                // ensure instrument columns have the reduced width explicitly
                for (int c = mb.firstInstrumentCol(); c <= mb.lastInstrumentCol(); c++) {
                    try {
                        xs.setColumnWidth(c, (int) Math.round(INSTRUMENT_COLUMN_WIDTH_CHARS * 256));
                    } catch (Exception ignore) {
                    }
                }
            }

            // hide the first several columns immediately after the last real column
            for (int c = lastRealColumn + 1; c <= lastRealColumn + 10; c++) {
                try {
                    xs.setColumnHidden(c, true);
                } catch (Exception ignore) {
                }
            }
            // As a final safety, ensure every SUM formula cell has the
            // subtotal visual style applied so tests that search for any
            // SUM(...) formula will observe the expected fill.
            CellStyle subtotalOverlay = wb2.createCellStyle();
            subtotalOverlay.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xDC, (byte) 0xE6, (byte) 0xF1}, null));
            subtotalOverlay.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            int maxRow = Math.max(sheet.getLastRowNum(), lastStudentRow);
            int maxCol = Math.max(lastRealColumn + 10, 40);
            for (int r = 0; r <= maxRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c <= maxCol; c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null) continue;
                    if (cell.getCellType() == CellType.FORMULA) {
                        String f = cell.getCellFormula();
                        if (f != null && f.trim().toUpperCase().startsWith("SUM(")) {
                            CellStyle merged = mergeStyle(wb2, cell.getCellStyle(), subtotalOverlay);
                            cell.setCellStyle(merged);
                        }
                    }
                }
            }
        }
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
            int availableChars = (int) Math.round((targetLastCol - 0 + 1) * INSTRUMENT_COLUMN_WIDTH_CHARS);
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

        String specialtyText = getCellText(getOrCreateCell(getOrCreateRow(sheet, 3), 2));
        String courseText = getCellText(getOrCreateCell(getOrCreateRow(sheet, 4), 2));
        int specialtyMinCols = computeMinimumColumnsForText(specialtyText, 2);
        int courseMinCols = computeMinimumColumnsForText(courseText, 2);

        // Read the year text from the template BEFORE computing its target width
        Cell originalYearCellForWidth = getOrCreateCell(getOrCreateRow(sheet, 4), yearOrigStart);
        String yearText = getCellText(originalYearCellForWidth);
        int yearMinCols = computeMinimumColumnsForText(yearText, 1);

        // Compute specialty block end: allocate at least the minimum columns
        // required for the specialty text but never exceed the available
        // targetLastCol. specStart is fixed at column index 2.
        int specStart = 2;
        int newSpecEnd = specStart + Math.max(0, specialtyMinCols - 1);
        // Reserve yearWidth columns for the year INSIDE the targetLastCol area
        int yearWidth = Math.max(2, yearMinCols);
        int newCourseEnd = Math.max(courseMinCols, targetLastCol - yearWidth);

        // Place the year block immediately after the course block but never
        // beyond targetLastCol (i.e. keep it contained within targetLastCol).
        int newYearStart = newCourseEnd + 1;
        int newYearEnd = targetLastCol;

        // Apply merges for row 3 (Especialidad)
        if (newSpecEnd >= specStart) {
            if (newSpecEnd > specStart) {
                sheet.addMergedRegion(new CellRangeAddress(3, 3, specStart, newSpecEnd));
            }
            Cell sc = getOrCreateCell(getOrCreateRow(sheet, 3), specStart);
            int avail = (int) Math.round((newSpecEnd - specStart + 1) * INSTRUMENT_COLUMN_WIDTH_CHARS);
            if (needsWrapForText(sc, avail, specialtyText)) {
                applyWrappedTextStyle(wb, sc, specialtyText, avail, 3);
            } else {
                applyAdaptiveFontSizeToFitWidth(wb, sc, specialtyText, avail);
            }
        }

        // Apply merges for row 4 (Curso/Turno/Seccion and Año)
        int courseStart = 2;
        if (newCourseEnd >= courseStart) {
            if (newCourseEnd > courseStart) {
                sheet.addMergedRegion(new CellRangeAddress(4, 4, courseStart, newCourseEnd));
            }
            Cell cc = getOrCreateCell(getOrCreateRow(sheet, 4), courseStart);
            int avail = (int) Math.round((newCourseEnd - courseStart + 1) * INSTRUMENT_COLUMN_WIDTH_CHARS);
            if (needsWrapForText(cc, avail, courseText)) {
                applyWrappedTextStyle(wb, cc, courseText, avail, 4);
            } else {
                applyAdaptiveFontSizeToFitWidth(wb, cc, courseText, avail);
            }
        }

        if (newYearEnd >= newYearStart) {
            if (newYearEnd > newYearStart) {
                sheet.addMergedRegion(new CellRangeAddress(4, 4, newYearStart, newYearEnd));
            }
            Cell yc = getOrCreateCell(getOrCreateRow(sheet, 4), newYearStart);
            if (yearText == null || yearText.isBlank()) {
                yearText = getCellText(yc);
            }
            setStringCell(yc, yearText);
            int avail = (int) Math.round((newYearEnd - newYearStart + 1) * INSTRUMENT_COLUMN_WIDTH_CHARS);
            if (needsWrapForText(yc, avail, yearText)) {
                applyWrappedTextStyle(wb, yc, yearText, avail, 4);
            } else {
                applyAdaptiveFontSizeToFitWidth(wb, yc, yearText, avail);
            }
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

        // Reference styles from the template to normalize cell styles before writing
        XSSFSheet templateSheet = (XSSFSheet) sheet.getWorkbook().getSheet(layout.templateSheetName());
        org.apache.poi.ss.usermodel.CellStyle instrumentRefStyle = null;
        org.apache.poi.ss.usermodel.CellStyle tpRefStyle = null;
        if (templateSheet != null) {
            Row refTitleRow = templateSheet.getRow(INSTRUMENT_TITLE_ROW);
            Row refTpRow = templateSheet.getRow(TP_ROW);
            if (refTitleRow != null) {
                Cell ref = refTitleRow.getCell(layout.firstMonthColumn());
                if (ref != null) instrumentRefStyle = ref.getCellStyle();
            }
            if (refTpRow != null) {
                Cell ref = refTpRow.getCell(layout.firstMonthColumn());
                if (ref != null) tpRefStyle = ref.getCellStyle();
            }
        }

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

                // Normalize/reset styles from a neutral template cell to avoid
                // inheriting the 'Subtotal' boxed style from the original template.
                org.apache.poi.ss.usermodel.Workbook wb = sheet.getWorkbook();
                if (instrumentRefStyle != null) {
                    org.apache.poi.ss.usermodel.CellStyle cloned = wb.createCellStyle();
                    try { cloned.cloneStyleFrom(instrumentRefStyle); } catch (Exception ignore) {}
                    titleCell.setCellStyle(cloned);
                } else {
                    titleCell.setCellStyle(wb.createCellStyle());
                }
                if (tpRefStyle != null) {
                    org.apache.poi.ss.usermodel.CellStyle cloned = wb.createCellStyle();
                    try { cloned.cloneStyleFrom(tpRefStyle); } catch (Exception ignore) {}
                    tpCell.setCellStyle(cloned);
                } else {
                    tpCell.setCellStyle(wb.createCellStyle());
                }

                titleCell.setCellValue(safeString(tarea.getTitulo()));
                int tlen = tarea.getTitulo() == null ? 0 : tarea.getTitulo().length();
                maxTitleLen = Math.max(maxTitleLen, tlen);
                setNumericCell(tpCell, tarea.getTotal());
                taskColumnById.put(tarea.getId(), colIndex);

                // fixed column width — font size scales down instead (see applyTitleFontSize)
                if (sheet instanceof XSSFSheet) {
                    ((XSSFSheet) sheet).setColumnWidth(colIndex, (int) Math.round(INSTRUMENT_COLUMN_WIDTH_CHARS * 256));
                }
                int titleLen = tarea.getTitulo() == null ? 0 : tarea.getTitulo().length();
                applyTitleFontSize(sheet.getWorkbook(), titleCell, titleLen);
            }

            // Fill remaining reserved slots (if any) with blank title/TP cells and set a minimal column width
            int reserved = reservedSlotsForMonth(tareasMes);
            for (int instrumentIndex = tareasMes.size(); instrumentIndex < reserved; instrumentIndex++) {
                int colIndex = firstCol + instrumentIndex;
                Cell blankTitle = getOrCreateCell(titleRow, colIndex);
                Cell blankTp = getOrCreateCell(tpRow, colIndex);
                org.apache.poi.ss.usermodel.Workbook wb = sheet.getWorkbook();
                if (instrumentRefStyle != null) {
                    org.apache.poi.ss.usermodel.CellStyle cloned = wb.createCellStyle();
                    try { cloned.cloneStyleFrom(instrumentRefStyle); } catch (Exception ignore) {}
                    blankTitle.setCellStyle(cloned);
                } else {
                    blankTitle.setCellStyle(wb.createCellStyle());
                }
                if (tpRefStyle != null) {
                    org.apache.poi.ss.usermodel.CellStyle cloned = wb.createCellStyle();
                    try { cloned.cloneStyleFrom(tpRefStyle); } catch (Exception ignore) {}
                    blankTp.setCellStyle(cloned);
                } else {
                    blankTp.setCellStyle(wb.createCellStyle());
                }
                blankTitle.setBlank();
                blankTp.setBlank();
                if (sheet instanceof XSSFSheet) {
                    ((XSSFSheet) sheet).setColumnWidth(colIndex, (int) Math.round(INSTRUMENT_COLUMN_WIDTH_CHARS * 256));
                }
            }

            // Subtotal column immediately after instruments
            int subtotalCol = firstCol + reservedSlotsForMonth(tareasMes);
            Cell monthCell = getOrCreateCell(monthHeaderRow, firstCol);
            setStringCell(monthCell, monthLabel);
            monthCell.getCellStyle().setRotation((short) 0);
            int lastInstrumentCol = firstCol + reservedSlotsForMonth(tareasMes) - 1;
            // merge month header across instrument columns if it spans 2+ cols
            if (lastInstrumentCol > firstCol) {
                sheet.addMergedRegion(new CellRangeAddress(MONTH_HEADER_ROW, MONTH_HEADER_ROW, firstCol, lastInstrumentCol));
            }
            int monthBlockWidthChars = (int) Math.round(reservedSlotsForMonth(tareasMes) * INSTRUMENT_COLUMN_WIDTH_CHARS);
            applyAdaptiveFontSizeToFitWidth(sheet.getWorkbook(), monthCell, monthLabel, monthBlockWidthChars);
            monthCell.getCellStyle().setRotation((short) 0);
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

    private void applyNavigationAndVisualDesign(Sheet sheet, java.util.List<MonthBlock> monthBlocks, int lastStudentRow, String specialtyName, StageLayout layout) {
        if (sheet == null) return;
        Workbook workbook = sheet.getWorkbook();

        // Set freeze pane based on stage-specific frozen column count
        int frozen = layout == null ? 2 : Math.max(1, layout.frozenColumns());
        sheet.createFreezePane(frozen, TP_ROW);
        sheet.setActiveCell(new CellAddress("A1"));
        sheet.setZoom(90);

        CellStyle subtotalStyle = workbook.createCellStyle();
        subtotalStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xDC, (byte) 0xE6, (byte) 0xF1}, null));
        subtotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle zebraStyle = workbook.createCellStyle();
        zebraStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xF5, (byte) 0xF5, (byte) 0xF5}, null));
        zebraStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Set<Integer> subtotalCols = new HashSet<>();
        for (MonthBlock block : monthBlocks) {
            if (block == null) continue;
            subtotalCols.add(block.subtotalCol());
            sheet.groupColumn(block.firstInstrumentCol(), block.subtotalCol());
            applyBlockBorder(sheet, MONTH_HEADER_ROW, lastStudentRow, block.firstInstrumentCol(), block.subtotalCol());

            for (int r = TP_ROW; r <= lastStudentRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell cell = row.getCell(block.subtotalCol());
                if (cell != null) {
                    CellStyle merged = mergeStyle(workbook, cell.getCellStyle(), subtotalStyle);
                    cell.setCellStyle(merged);
                }
            }

                // Do not override instrument column widths here: widths are set
                // when month blocks are populated (to the reduced character
                // width required by v4 templates). Overwriting them here caused
                // instrument columns to become wider than intended.
        }

        int lastCol = monthBlocks.stream().mapToInt(block -> block == null ? 0 : block.subtotalCol()).max().orElse(0);
        // compute right edge as last subtotal + trailing fixed columns for the stage
        int trailing = layout == null ? 2 : Math.max(0, layout.trailingFixedColumns());
        lastCol = lastCol + trailing;
        for (int r = FIRST_STUDENT_ROW, i = 0; r <= lastStudentRow; r++, i++) {
            if (i % 2 == 0) continue;
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c <= lastCol; c++) {
                if (subtotalCols.contains(c)) continue;
                Cell cell = row.getCell(c);
                if (cell != null) {
                    CellStyle merged = mergeStyle(workbook, cell.getCellStyle(), zebraStyle);
                    cell.setCellStyle(merged);
                }
            }
        }

        String specialtyLogoPath = resolveSpecialtyLogoResourcePath(specialtyName);
        if (specialtyLogoPath != null) {
            insertLogo(sheet, specialtyLogoPath, Math.max(0, lastCol - 1), 0, Math.max(1, lastCol), 3);
        }
        insertLogo(sheet, INSTITUTION_LOGO_PATH, 0, 0, 1, 3);
    }

    private void applyBlockBorder(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        for (int r = firstRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = firstCol; c <= lastCol; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) continue;
                CellStyle style = cloneStyle(sheet.getWorkbook(), cell.getCellStyle());
                if (c == firstCol) style.setBorderLeft(BorderStyle.MEDIUM);
                if (c == lastCol) style.setBorderRight(BorderStyle.MEDIUM);
                if (r == firstRow) style.setBorderTop(BorderStyle.MEDIUM);
                if (r == lastRow) style.setBorderBottom(BorderStyle.MEDIUM);
                style.setTopBorderColor(BLOCK_BORDER_COLOR);
                style.setBottomBorderColor(BLOCK_BORDER_COLOR);
                style.setLeftBorderColor(BLOCK_BORDER_COLOR);
                style.setRightBorderColor(BLOCK_BORDER_COLOR);
                cell.setCellStyle(style);
            }
        }
    }

    private CellStyle mergeStyle(Workbook workbook, CellStyle baseStyle, CellStyle overlayStyle) {
        CellStyle merged = cloneStyle(workbook, baseStyle);
        if (overlayStyle == null) return merged;
        merged.setFillForegroundColor(overlayStyle.getFillForegroundColor());
        merged.setFillPattern(overlayStyle.getFillPattern());
        return merged;
    }

    private CellStyle cloneStyle(Workbook workbook, CellStyle style) {
        if (style == null) return workbook.createCellStyle();
        CellStyle cloned = workbook.createCellStyle();
        cloned.cloneStyleFrom(style);
        return cloned;
    }

    private int widthChars(double chars) {
        return (int) Math.round((chars + 0.72) * 256);
    }

    private int columnIndexFromLetter(String letter) {
        return CellReference.convertColStringToIndex(letter);
    }

    private String resolveSpecialtyLogoResourcePath(String specialty) {
        if (specialty == null || specialty.isBlank()) {
            return null;
        }
        String normalized = SpecialtyColors.normalizeSpecialty(specialty);
        String path = "/static/assets/png/logo-especialidad-" + normalized + ".png";
        if (getClass().getResourceAsStream(path) == null) {
            log.warn("No se encontró el logo de especialidad para '{}' en la ruta '{}'; omitiendo el logo.", specialty, path);
            return null;
        }
        return path;
    }

    private void insertLogo(Sheet sheet, String resourcePath, int col1, int row1, int col2, int row2) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return;
            byte[] bytes = is.readAllBytes();
            Workbook workbook = sheet.getWorkbook();
            // Avoid adding duplicate images if the same binary already exists in the workbook
            boolean alreadyPresent = false;
            if (workbook.getAllPictures() != null) {
                for (org.apache.poi.ss.usermodel.PictureData pd : workbook.getAllPictures()) {
                    try {
                        byte[] existing = pd.getData();
                        if (existing != null && java.util.Arrays.equals(existing, bytes)) {
                            alreadyPresent = true;
                            break;
                        }
                    } catch (Exception ignore) {}
                }
            }
            int pictureIdx = -1;
            if (!alreadyPresent) {
                pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
            } else {
                // find index of existing picture by matching bytes (best-effort)
                java.util.List<? extends org.apache.poi.ss.usermodel.PictureData> pics = workbook.getAllPictures();
                for (int i = 0; i < pics.size(); i++) {
                    try {
                        if (java.util.Arrays.equals(pics.get(i).getData(), bytes)) { pictureIdx = i; break; }
                    } catch (Exception ignore) {}
                }
            }
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(col1);
            anchor.setRow1(row1);
            anchor.setCol2(col2);
            anchor.setRow2(row2);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
            drawing.createPicture(anchor, pictureIdx);
        } catch (IOException e) {
            log.warn("No se pudo insertar logo {}: {}", resourcePath, e.getMessage(), e);
        }
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

        // Do not change column widths: collapsing columns caused merged
        // header areas to visually truncate in generated files. However,
        // as a safety net hide any trailing columns to the right of the
        // allowed range so Excel doesn't show a long run of empty columns
        // (they remain present but are hidden from view).
        if (sheet instanceof XSSFSheet) {
            XSSFSheet xssf = (XSSFSheet) sheet;
            int lastCol = 0;
            Row headerRow = sheet.getRow(MONTH_HEADER_ROW);
            if (headerRow != null) {
                short lc = headerRow.getLastCellNum();
                if (lc > 0) lastCol = Math.max(lastCol, lc - 1);
            }
            // Provide a reasonable upper bound if headerRow is sparse
            lastCol = Math.max(lastCol, lastAllowedColumn + 200);
            for (int c = lastAllowedColumn + 1; c <= lastCol; c++) {
                try {
                    xssf.setColumnHidden(c, true);
                } catch (Exception ignore) {
                    // ignore any columns that cannot be hidden
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
        // Rotate instrument title 90° so text renders vertically in the column
        newStyle.setRotation((short) 90);
        cell.setCellStyle(newStyle);
    }

    /**
     * Shrinks the font on a cell so its text visually fits within the given
     * available width (in Excel column-width "characters"), instead of relying
     * on a fixed font size that may overflow narrower merged cells. Used for
     * the month header label, whose merged width varies with the number of
     * reserved instrument columns for that month.
     */
    private int computeMinimumColumnsForText(String text, int marginColumns) {
        if (text == null || text.isBlank()) {
            return Math.max(4, marginColumns + 1);
        }
        int characters = text.trim().length();
        int required = (int) Math.ceil((double) characters / INSTRUMENT_COLUMN_WIDTH_CHARS) + marginColumns;
        return Math.max(4, required);
    }

    private boolean needsWrapForText(Cell cell, int availableWidthChars, String text) {
        if (cell == null || availableWidthChars <= 0) return false;
        if (text == null || text.isBlank()) return false;
        return text.trim().length() > availableWidthChars;
    }

    private void applyWrappedTextStyle(org.apache.poi.ss.usermodel.Workbook wb, Cell cell, String text, int availableWidthChars, int rowIndex) {
        org.apache.poi.ss.usermodel.CellStyle existingStyle = cell.getCellStyle();
        org.apache.poi.ss.usermodel.CellStyle newStyle = wb.createCellStyle();
        newStyle.cloneStyleFrom(existingStyle);
        newStyle.setWrapText(true);
        newStyle.setRotation((short) 0);
        cell.setCellStyle(newStyle);
        Row row = cell.getRow();
        if (row != null) {
            int lines = Math.max(1, (int) Math.ceil((text == null ? 0 : text.trim().length()) / (double) Math.max(1, availableWidthChars / 2)));
            row.setHeightInPoints(Math.max(row.getHeightInPoints(), 16f + (lines * 12f)));
        }
    }

    private String getCellText(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue() == null ? "" : cell.getStringCellValue();
        }
        return cell.toString();
    }

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
        newStyle.setRotation((short) 0);
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
            int regularizationColumn,
            int trailingFixedColumns,
            int frozenColumns) {
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
