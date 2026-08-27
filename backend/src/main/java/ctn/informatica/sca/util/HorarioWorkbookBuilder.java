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

    public XSSFWorkbook build(CursoBase curso, List<HoraCatedra> horas, List<HorarioSlot> slots) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Styles styles = new Styles(workbook);
        createCourseSheet(workbook, styles, "Horario", curso, horas, slots);
        workbook.setActiveSheet(0);
        return workbook;
    }

    public XSSFWorkbook buildEspecialidad(String especialidadNombre, List<CursoBase> cursos, List<HoraCatedra> horas, List<HorarioSlot> slots) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Styles styles = new Styles(workbook);

        List<CursoBase> safeCourses = cursos == null ? List.of() : cursos;
        Set<String> usedNames = new LinkedHashSet<>();
        if (safeCourses.isEmpty()) {
            createEmptySpecialtySheet(workbook, styles, especialidadNombre);
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
            writeCourseSheet(workbook, sheet, styles, curso, horas, courseSlots);
        }

        workbook.setActiveSheet(0);
        return workbook;
    }

    private void createCourseSheet(XSSFWorkbook workbook, Styles styles, String sheetName, CursoBase curso, List<HoraCatedra> horas, List<HorarioSlot> slots) {
        Sheet sheet = workbook.createSheet(sheetName);
        writeCourseSheet(workbook, sheet, styles, curso, horas, slots);
    }

    private void createEmptySpecialtySheet(XSSFWorkbook workbook, Styles styles, String especialidadNombre) {
        Sheet sheet = workbook.createSheet("Sin cursos");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("No hay cursos para " + (especialidadNombre == null ? "esta especialidad" : especialidadNombre));
        cell.setCellStyle(styles.title);
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
            emptyCell.setCellStyle(styles.grid);
            sheet.addMergedRegion(new CellRangeAddress(emptyRow.getRowNum(), emptyRow.getRowNum(), 0, dayCount));
        } else {
            for (HorarioScheduleLayout.BlockLayout block : blocks) {
                rowIndex = writeBlock(sheet, styles, block, rowIndex);
                rowIndex++;
            }
        }

        sheet.setColumnWidth(0, 3200);
        for (int i = 1; i <= dayCount; i++) {
            sheet.setColumnWidth(i, 6500);
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

        try (InputStream is = HorarioWorkbookBuilder.class.getResourceAsStream("/static/logo-institucional.png")) {
            if (is != null) {
                byte[] bytes = IOUtils.toByteArray(is);
                int pictureIdx = workbook.addPicture(bytes, XSSFWorkbook.PICTURE_TYPE_PNG);
                CreationHelper helper = workbook.getCreationHelper();
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setCol1(0);
                anchor.setRow1(titleRow.getRowNum());
                anchor.setCol2(1);
                anchor.setRow2(subtitleRow.getRowNum() + 1);
                drawing.createPicture(anchor, pictureIdx);
            }
        } catch (Exception ignored) {
            // Si el logo no está disponible, seguimos sin abortar la exportación.
        }

        return rowIndex;
    }

    private int writeBlock(Sheet sheet, Styles styles, HorarioScheduleLayout.BlockLayout block, int rowIndex) {
        Row bloqueTitleRow = sheet.createRow(rowIndex++);
        bloqueTitleRow.setHeightInPoints(20);
        Cell bloqueTitleCell = bloqueTitleRow.createCell(0);
        bloqueTitleCell.setCellValue(block.name);
        bloqueTitleCell.setCellStyle(styles.bloqueTitle);
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

        int dataStartRow = rowIndex;
        for (int rowOffset = 0; rowOffset < block.rows.size(); rowOffset++) {
            HorarioScheduleLayout.RowLayout rowLayout = block.rows.get(rowOffset);
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(rowLayout.receso ? 18 : 40);

            if (rowLayout.receso) {
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

            Cell hourCell = row.createCell(0);
            hourCell.setCellValue(rowLayout.horaLabel);
            hourCell.setCellStyle(styles.grid);

            for (int dia = 1; dia <= block.dayCount; dia++) {
                Cell cell = row.createCell(dia);
                cell.setCellStyle(styles.grid);
                if (isMergedContinuation(block, rowOffset, dia)) {
                    continue;
                }
                HorarioScheduleLayout.CellLayout cellLayout = rowLayout.cells.get(dia);
                if (cellLayout != null && cellLayout.text != null && !cellLayout.text.isBlank()) {
                    cell.setCellValue(cellLayout.text);
                }
            }
        }

        for (HorarioScheduleLayout.MergedRange range : block.mergedRanges) {
            sheet.addMergedRegion(new CellRangeAddress(dataStartRow + range.startRow, dataStartRow + range.endRow, range.day, range.day));
        }

        Row salasRow = sheet.createRow(rowIndex++);
        salasRow.setHeightInPoints(22);
        Cell salasHeaderCell = salasRow.createCell(0);
        salasHeaderCell.setCellValue("Salas");
        salasHeaderCell.setCellStyle(styles.header);
        for (int dia = 1; dia <= block.dayCount; dia++) {
            Cell cell = salasRow.createCell(dia);
            cell.setCellValue(String.join(" / ", block.salasPorDia.get(dia)));
            cell.setCellStyle(styles.header);
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
        String base = curso == null ? "Curso" : (curso.getCursoOrdinal() + curso.getSeccion());
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
        final CellStyle bloqueTitle;
        final CellStyle header;
        final CellStyle grid;
        final CellStyle receso;

        Styles(XSSFWorkbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            title = workbook.createCellStyle();
            title.setFont(titleFont);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            Font subtitleFont = workbook.createFont();
            subtitleFont.setBold(true);
            subtitleFont.setFontHeightInPoints((short) 11);
            subtitle = workbook.createCellStyle();
            subtitle.setFont(subtitleFont);
            subtitle.setAlignment(HorizontalAlignment.CENTER);
            subtitle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font bloqueFont = workbook.createFont();
            bloqueFont.setBold(true);
            bloqueFont.setFontHeightInPoints((short) 12);
            bloqueTitle = workbook.createCellStyle();
            bloqueTitle.setFont(bloqueFont);
            bloqueTitle.setAlignment(HorizontalAlignment.CENTER);
            bloqueTitle.setVerticalAlignment(VerticalAlignment.CENTER);
            bloqueTitle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
            bloqueTitle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            header = workbook.createCellStyle();
            header.setFont(headerFont);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderTop(BorderStyle.THIN);
            header.setBorderBottom(BorderStyle.THIN);
            header.setBorderLeft(BorderStyle.THIN);
            header.setBorderRight(BorderStyle.THIN);
            header.setWrapText(true);

            grid = workbook.createCellStyle();
            grid.setBorderTop(BorderStyle.THIN);
            grid.setBorderBottom(BorderStyle.THIN);
            grid.setBorderLeft(BorderStyle.THIN);
            grid.setBorderRight(BorderStyle.THIN);
            grid.setAlignment(HorizontalAlignment.CENTER);
            grid.setVerticalAlignment(VerticalAlignment.CENTER);
            grid.setWrapText(true);

            Font recesoFont = workbook.createFont();
            recesoFont.setItalic(true);
            recesoFont.setBold(true);
            receso = workbook.createCellStyle();
            receso.setFont(recesoFont);
            receso.setAlignment(HorizontalAlignment.CENTER);
            receso.setVerticalAlignment(VerticalAlignment.CENTER);
            receso.setBorderTop(BorderStyle.THIN);
            receso.setBorderBottom(BorderStyle.THIN);
            receso.setBorderLeft(BorderStyle.THIN);
            receso.setBorderRight(BorderStyle.THIN);
            receso.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            receso.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
    }
}
