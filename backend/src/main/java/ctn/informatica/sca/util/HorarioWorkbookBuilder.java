package ctn.informatica.sca.util;

import ctn.informatica.sca.model.CursoBase;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Genera el horario de un curso en dos bloques (Mañana y T.O. / turno
 * opuesto), imitando el formato en uso por el colegio: columnas fijas de
 * Lunes a Sábado, una fila de RECESO donde corresponde según el hueco
 * horario, y una fila de SALAS al pie de cada bloque con los ambientes
 * usados por día.
 */
public class HorarioWorkbookBuilder {

    private static final Logger log = LoggerFactory.getLogger(HorarioWorkbookBuilder.class);
    private static final HorarioTemplateStyles TEMPLATE = HorarioTemplateStyles.load();

    public XSSFWorkbook build(CursoBase curso, List<HoraCatedra> horas, List<HorarioSlot> slots) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        String specialty = curso == null ? null : curso.getEspecialidad();
        Styles styles = new Styles(workbook, specialty, TEMPLATE);
        createCourseSheet(workbook, styles, "Horario", curso, horas, slots);
        workbook.setActiveSheet(0);
        return workbook;
    }

    public XSSFWorkbook buildEspecialidad(String especialidadNombre, List<CursoBase> cursos, List<HoraCatedra> horas, List<HorarioSlot> slots) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        // Styles will be created per-sheet (so receso color can vary por especialidad)

        List<CursoBase> safeCourses = cursos == null ? List.of() : cursos;
        Set<String> usedNames = new LinkedHashSet<>();
        if (safeCourses.isEmpty()) {
            createEmptySpecialtySheet(workbook, especialidadNombre);
            return workbook;
        }

        for (CursoBase curso : safeCourses) {
            if (curso == null) {
                continue;
            }
            String baseName = buildSheetName(curso);
            String sheetName = uniqueSheetName(baseName, usedNames);
            Sheet sheet = workbook.createSheet(sheetName);
            usedNames.add(sheetName);
            List<HorarioSlot> courseSlots = filterSlotsForCourse(slots, curso.getId());
            Styles sheetStyles = new Styles(workbook, curso == null ? null : curso.getEspecialidad(), TEMPLATE);
            writeCourseSheet(workbook, sheet, sheetStyles, curso, horas, courseSlots);
        }

        workbook.setActiveSheet(0);
        return workbook;
    }

    private void createCourseSheet(XSSFWorkbook workbook, Styles styles, String sheetName, CursoBase curso, List<HoraCatedra> horas, List<HorarioSlot> slots) {
        Sheet sheet = workbook.createSheet(sheetName);
        writeCourseSheet(workbook, sheet, styles, curso, horas, slots);
    }

    private void createEmptySpecialtySheet(XSSFWorkbook workbook, String especialidadNombre) {
        Styles localStyles = new Styles(workbook, especialidadNombre, TEMPLATE);
        Sheet sheet = workbook.createSheet("Sin cursos");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("No hay cursos para " + (especialidadNombre == null ? "esta especialidad" : especialidadNombre));
        cell.setCellStyle(localStyles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        sheet.setColumnWidth(0, 9000);
    }

    private void writeCourseSheet(XSSFWorkbook workbook, Sheet sheet, Styles styles, CursoBase curso, List<HoraCatedra> horas, List<HorarioSlot> slots) {
        int dayCount = detectDayCount(slots);
        List<HorarioScheduleLayout.BlockLayout> blocks = HorarioScheduleLayout.buildBlocks(horas, slots, dayCount);
        int rowIndex = 0;
        rowIndex = writeTitle(sheet, styles, curso, rowIndex, workbook, dayCount);
        rowIndex++;

        if (blocks.isEmpty()) {
            Row emptyRow = sheet.createRow(rowIndex++);
            Cell emptyCell = emptyRow.createCell(0);
            emptyCell.setCellValue("No hay horario cargado para este curso.");
            emptyCell.setCellStyle(styles.banner);
            sheet.addMergedRegion(new CellRangeAddress(emptyRow.getRowNum(), emptyRow.getRowNum(), 0, dayCount));
        } else {
            for (HorarioScheduleLayout.BlockLayout block : blocks) {
                rowIndex = writeBlock(sheet, styles, block, rowIndex);
                rowIndex++;
            }
        }

        sheet.setColumnWidth(0, TEMPLATE.hourColumnWidth());
        for (int i = 1; i <= dayCount; i++) {
            sheet.setColumnWidth(i, TEMPLATE.dayColumnWidth());
        }
    }

    private int writeTitle(Sheet sheet, Styles styles, CursoBase curso, int rowIndex, XSSFWorkbook workbook, int lastCol) {
        Row titleRow = sheet.createRow(rowIndex++);
        titleRow.setHeightInPoints(24);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("HORARIO DE CLASES");
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, lastCol));

        Row subtitleRow = sheet.createRow(rowIndex++);
        subtitleRow.setHeightInPoints(22);
        Cell subtitleCell = subtitleRow.createCell(0);
        String subtitle = curso == null ? "" : ("Curso: " + curso.getCursoOrdinal() + " " + curso.getEspecialidad()
                + "    Turno: Mañana - Tarde    Sección: " + curso.getSeccion());
        subtitleCell.setCellValue(subtitle);
        subtitleCell.setCellStyle(styles.subtitle);
        sheet.addMergedRegion(new CellRangeAddress(subtitleRow.getRowNum(), subtitleRow.getRowNum(), 0, lastCol));

        addPictureIfPresent(
            workbook,
            sheet,
            "/static/logo-institucional.png",
            0,
            titleRow.getRowNum(),
            1,
            subtitleRow.getRowNum() + 1,
            "logo institucional"
        );

        // Logo de especialidad: el nombre ya viene normalizado por SpecialtyColors.
        String normalized = SpecialtyColors.normalizeSpecialty(curso == null ? null : curso.getEspecialidad());
        String specialtyPath = "/static/assets/png/logo-especialidad-" + normalized + ".png";
        addPictureIfPresent(
            workbook,
            sheet,
            specialtyPath,
            Math.max(0, lastCol - 1),
            titleRow.getRowNum(),
            Math.max(1, lastCol),
            subtitleRow.getRowNum() + 1,
            "logo de especialidad " + normalized
        );

        return rowIndex;
    }

    private void addPictureIfPresent(
        XSSFWorkbook workbook,
        Sheet sheet,
        String resourcePath,
        int col1,
        int row1,
        int col2,
        int row2,
        String label
    ) {
        try (InputStream is = HorarioWorkbookBuilder.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Horario export: no se encontró {} en {}", label, resourcePath);
                return;
            }

            byte[] bytes = IOUtils.toByteArray(is);
            int pictureIdx = workbook.addPicture(bytes, XSSFWorkbook.PICTURE_TYPE_PNG);
            CreationHelper helper = workbook.getCreationHelper();
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(col1);
            anchor.setRow1(row1);
            anchor.setCol2(col2);
            anchor.setRow2(row2);
            drawing.createPicture(anchor, pictureIdx);
        } catch (Exception ex) {
            log.warn("Horario export: no se pudo insertar {} desde {}", label, resourcePath, ex);
        }
    }

    private int writeBlock(Sheet sheet, Styles styles, HorarioScheduleLayout.BlockLayout block, int rowIndex) {
        Row bloqueTitleRow = sheet.createRow(rowIndex++);
        bloqueTitleRow.setHeightInPoints(20);
        Cell bloqueTitleCell = bloqueTitleRow.createCell(0);
        bloqueTitleCell.setCellValue(block.name);
        bloqueTitleCell.setCellStyle(styles.banner);
        sheet.addMergedRegion(new CellRangeAddress(bloqueTitleRow.getRowNum(), bloqueTitleRow.getRowNum(), 0, block.dayCount));

        Row headerRow = sheet.createRow(rowIndex++);
        headerRow.setHeightInPoints(22);
        Cell horaHeader = headerRow.createCell(0);
        horaHeader.setCellValue("Hora");
        horaHeader.setCellStyle(styles.header);
        for (int dia = 1; dia <= block.dayCount; dia++) {
            Cell cell = headerRow.createCell(dia);
            cell.setCellValue(HorarioScheduleLayout.DIAS[dia - 1]);
            cell.setCellStyle(styles.header);
        }

        List<Integer> physicalRowStarts = new ArrayList<>();
        for (int rowOffset = 0; rowOffset < block.rows.size(); rowOffset++) {
            HorarioScheduleLayout.RowLayout rowLayout = block.rows.get(rowOffset);
            physicalRowStarts.add(rowIndex);

            if (rowLayout.receso) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(18);
                Cell recesoCell = row.createCell(0);
                recesoCell.setCellValue("RECESO");
                recesoCell.setCellStyle(styles.receso);
                sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, block.dayCount));
                for (int dia = 1; dia <= block.dayCount; dia++) {
                    Cell filler = row.createCell(dia);
                    filler.setCellStyle(styles.receso);
                }
                continue;
            }

            Row materiaRow = sheet.createRow(rowIndex++);
            Row profesorRow = sheet.createRow(rowIndex++);
            materiaRow.setHeightInPoints(18);
            profesorRow.setHeightInPoints(18);

            Cell hourTop = materiaRow.createCell(0);
            hourTop.setCellValue(rowLayout.horaLabel);
            hourTop.setCellStyle(styles.hourCell);
            Cell hourBottom = profesorRow.createCell(0);
            hourBottom.setCellStyle(styles.hourCell);
            sheet.addMergedRegion(new CellRangeAddress(materiaRow.getRowNum(), profesorRow.getRowNum(), 0, 0));

            for (int dia = 1; dia <= block.dayCount; dia++) {
                Cell materiaCell = materiaRow.createCell(dia);
                materiaCell.setCellStyle(styles.materiaCell);
                Cell profesorCell = profesorRow.createCell(dia);
                profesorCell.setCellStyle(styles.profesorCell);
                if (isMergedContinuation(block, rowOffset, dia)) {
                    continue;
                }
                HorarioScheduleLayout.CellLayout cellLayout = rowLayout.cells.get(dia);
                if (cellLayout != null) {
                    if (cellLayout.materiaText != null && !cellLayout.materiaText.isBlank()) {
                        materiaCell.setCellValue(cellLayout.materiaText);
                    }
                    if (cellLayout.profesorText != null && !cellLayout.profesorText.isBlank()) {
                        profesorCell.setCellValue(cellLayout.profesorText);
                    }
                }
            }
        }

        for (HorarioScheduleLayout.MergedRange range : block.mergedRanges) {
            int startRow = physicalRowStarts.get(range.startRow);
            int endRow = physicalRowStarts.get(range.endRow) + 1;
            sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, range.day, range.day));
        }

        Row salasRow = sheet.createRow(rowIndex++);
        salasRow.setHeightInPoints(22);
        Cell salasHeaderCell = salasRow.createCell(0);
        salasHeaderCell.setCellValue("Salas");
        salasHeaderCell.setCellStyle(styles.salasLabel);
        for (int dia = 1; dia <= block.dayCount; dia++) {
            Cell cell = salasRow.createCell(dia);
            cell.setCellValue(String.join(" / ", block.salasPorDia.get(dia)));
            cell.setCellStyle(styles.salasData);
        }

        return rowIndex;
    }

    private boolean isMergedContinuation(HorarioScheduleLayout.BlockLayout block, int rowOffset, int day) {
        for (HorarioScheduleLayout.MergedRange range : block.mergedRanges) {
            if (range.day == day && range.startRow < rowOffset && range.endRow >= rowOffset) {
                return true;
            }
        }
        return false;
    }

    private int detectDayCount(List<HorarioSlot> slots) {
        int maxDay = 5;
        if (slots != null) {
            for (HorarioSlot slot : slots) {
                if (slot != null) {
                    maxDay = Math.max(maxDay, slot.getDiaSemana());
                }
            }
        }
        return Math.min(6, Math.max(5, maxDay));
    }

    private List<HorarioSlot> filterSlotsForCourse(List<HorarioSlot> slots, int cursoBaseId) {
        List<HorarioSlot> out = new ArrayList<>();
        for (HorarioSlot slot : slots == null ? List.<HorarioSlot>of() : slots) {
            if (slot != null && slot.getCursoId() == cursoBaseId) {
                out.add(slot);
            }
        }
        return out;
    }

    private String buildSheetName(CursoBase curso) {
        String base = curso == null ? "Curso" : (curso.getNivel() + curso.getSeccion());
        if (base.length() > 31) {
            base = base.substring(0, 31);
        }
        return base;
    }

    private String uniqueSheetName(String base, Set<String> usedNames) {
        String candidate = sanitizeSheetName(base);
        if (!usedNames.contains(candidate)) {
            return candidate;
        }

        int suffix = 2;
        while (true) {
            String prefix = candidate;
            String suffixText = "_" + suffix++;
            if (prefix.length() + suffixText.length() > 31) {
                prefix = prefix.substring(0, 31 - suffixText.length());
            }
            String variant = prefix + suffixText;
            if (!usedNames.contains(variant)) {
                return variant;
            }
        }
    }

    private String sanitizeSheetName(String value) {
        String safe = value == null || value.isBlank() ? "Curso" : value.trim();
        safe = safe.replaceAll("[\\\\/?*\\[\\]:]", "_");
        if (safe.length() > 31) {
            safe = safe.substring(0, 31);
        }
        return safe;
    }

    private static final class Styles {
        final CellStyle title;
        final CellStyle subtitle;
        final CellStyle banner;
        final CellStyle header;
        final CellStyle hourCell;
        final CellStyle materiaCell;
        final CellStyle profesorCell;
        final CellStyle salasLabel;
        final CellStyle salasData;
        final CellStyle receso;

        Styles(XSSFWorkbook workbook, String specialty, HorarioTemplateStyles template) {
            title = createStyle(workbook, template.title(), null, false);
            subtitle = createStyle(workbook, template.subtitle(), null, false);
            banner = createStyle(workbook, template.blockBanner(), null, false);
            header = createStyle(workbook, template.headerDay(), null, false);
            hourCell = createStyle(workbook, template.hourCell(), null, false);
            materiaCell = createStyle(workbook, template.materiaCell(), null, false);
            profesorCell = createStyle(workbook, template.profesorCell(), null, false);
            salasLabel = createStyle(workbook, template.salasLabel(), null, false);
            salasData = createStyle(workbook, template.salasData(), null, false);
            receso = createStyle(workbook, template.receso(), SpecialtyColors.getAccent(specialty), true);
        }

        private CellStyle createStyle(XSSFWorkbook workbook, HorarioTemplateStyles.CellStyleSpec spec, String fillOverrideHex, boolean overrideFill) {
            XSSFCellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontName(spec.fontName());
            font.setFontHeightInPoints(spec.fontSizePt());
            font.setBold(spec.bold());
            font.setItalic(spec.italic());
            if (spec.fontColorHex() != null && !spec.fontColorHex().isBlank()) {
                font.setColor(toIndexedFontColor(spec.fontColorHex()));
            }
            style.setFont(font);
            style.setAlignment(spec.horizontalAlignment());
            style.setVerticalAlignment(spec.verticalAlignment());
            style.setWrapText(spec.wrapText());
            style.setBorderTop(spec.borderTop());
            style.setBorderRight(spec.borderRight());
            style.setBorderBottom(spec.borderBottom());
            style.setBorderLeft(spec.borderLeft());
            if (spec.borderColorHex() != null && !spec.borderColorHex().isBlank()) {
                XSSFColor borderColor = toXssfColor(spec.borderColorHex());
                style.setTopBorderColor(borderColor);
                style.setRightBorderColor(borderColor);
                style.setBottomBorderColor(borderColor);
                style.setLeftBorderColor(borderColor);
            }
            String fillHex = overrideFill ? fillOverrideHex : spec.fillColorHex();
            if (fillHex != null && !fillHex.isBlank()) {
                XSSFColor fillColor = toXssfColor(fillHex);
                style.setFillForegroundColor(fillColor);
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            return style;
        }
    }

    private static XSSFColor toXssfColor(String hex) {
        String clean = hex == null ? "" : hex.replace("#", "").trim();
        if (clean.length() == 3) {
            clean = "" + clean.charAt(0) + clean.charAt(0) + clean.charAt(1) + clean.charAt(1) + clean.charAt(2) + clean.charAt(2);
        }
        if (clean.length() != 6) {
            return new XSSFColor(new byte[] {(byte) 0, (byte) 0, (byte) 0}, null);
        }
        try {
            byte[] rgb = new byte[] {
                (byte) Integer.parseInt(clean.substring(0, 2), 16),
                (byte) Integer.parseInt(clean.substring(2, 4), 16),
                (byte) Integer.parseInt(clean.substring(4, 6), 16)
            };
            return new XSSFColor(rgb, null);
        } catch (Exception ex) {
            return new XSSFColor(new byte[] {(byte) 0, (byte) 0, (byte) 0}, null);
        }
    }

    private static short toIndexedFontColor(String hex) {
        if (hex == null) {
            return IndexedColors.AUTOMATIC.getIndex();
        }
        String clean = hex.replace("#", "").trim().toUpperCase();
        return switch (clean) {
            case "FFFFFF" -> IndexedColors.WHITE.getIndex();
            case "000000" -> IndexedColors.BLACK.getIndex();
            case "404040" -> IndexedColors.GREY_50_PERCENT.getIndex();
            case "BFBFBF", "D9D9D9", "B9BFC7", "999999" -> IndexedColors.GREY_25_PERCENT.getIndex();
            default -> IndexedColors.AUTOMATIC.getIndex();
        };
    }
}
