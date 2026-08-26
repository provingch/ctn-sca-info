package ctn.informatica.sca.util;

import ctn.informatica.sca.model.CursoBase;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import java.io.InputStream;

/**
 * Genera el horario de un curso en dos bloques (Mañana y T.O. / turno
 * opuesto), imitando el formato en uso por el colegio: columnas fijas de
 * Lunes a Viernes (más Sábado solo si hay datos cargados ese día), una fila
 * de RECESO donde corresponde según el hueco horario, y una fila de SALAS
 * al pie de cada bloque con los ambientes usados por día.
 */
public class HorarioWorkbookBuilder {

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
    private static final String ETIQUETA_MANANA = "M";

    public XSSFWorkbook build(CursoBase curso, List<HoraCatedra> horas, List<HorarioSlot> slots) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Horario");

        Styles styles = new Styles(workbook);

        int lastDay = 5;
        for (HorarioSlot slot : slots) {
            if (slot.getDiaSemana() == 6) {
                lastDay = 6;
            }
        }
        int dayCount = lastDay;
        int lastCol = dayCount;

        Map<Integer, List<HorarioSlot>> slotsPorHoraCatedra = new LinkedHashMap<>();
        for (HorarioSlot slot : slots) {
            slotsPorHoraCatedra.computeIfAbsent(slot.getHoraCatedraId(), key -> new ArrayList<>()).add(slot);
        }

        List<HoraCatedra> manana = new ArrayList<>();
        List<HoraCatedra> tarde = new ArrayList<>();
        for (HoraCatedra hora : horas) {
            if (ETIQUETA_MANANA.equalsIgnoreCase(hora.getEtiqueta())) {
                manana.add(hora);
            } else {
                tarde.add(hora);
            }
        }

        int rowIndex = 0;
        rowIndex = writeTitle(sheet, styles, curso, lastCol, rowIndex, workbook, sheet);
        rowIndex++;

        rowIndex = writeBloque(sheet, styles, "MAÑANA", manana, slotsPorHoraCatedra, dayCount, lastCol, rowIndex);
        rowIndex++;
        rowIndex = writeBloque(sheet, styles, "T.O.", tarde, slotsPorHoraCatedra, dayCount, lastCol, rowIndex);

        sheet.setColumnWidth(0, 3200);
        for (int i = 1; i <= lastCol; i++) {
            sheet.setColumnWidth(i, 6500);
        }

        workbook.setActiveSheet(0);
        return workbook;
    }

    private int writeTitle(Sheet sheet, Styles styles, CursoBase curso, int lastCol, int rowIndex, XSSFWorkbook workbook, Sheet workbookSheet) {
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("HORARIO DE CLASES");
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, lastCol));

        Row subtitleRow = sheet.createRow(rowIndex++);
        Cell subtitleCell = subtitleRow.createCell(0);
        String subtitle = curso == null ? "" : ("Curso: " + curso.getCursoOrdinal() + " " + curso.getEspecialidad()
                + "    Turno: Mañana - Tarde    Sección: " + curso.getSeccion());
        subtitleCell.setCellValue(subtitle);
        subtitleCell.setCellStyle(styles.subtitle);
        sheet.addMergedRegion(new CellRangeAddress(subtitleRow.getRowNum(), subtitleRow.getRowNum(), 0, lastCol));

        // Intentional: try to load institutional logo from backend resources
        try (InputStream is = HorarioWorkbookBuilder.class.getResourceAsStream("/static/logo-institucional.png")) {
            if (is != null) {
                byte[] bytes = IOUtils.toByteArray(is);
                int pictureIdx = workbook.addPicture(bytes, XSSFWorkbook.PICTURE_TYPE_PNG);
                CreationHelper helper = workbook.getCreationHelper();
                Drawing<?> drawing = workbookSheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                // place the logo at column 0, row 0 with an offset
                anchor.setCol1(0);
                anchor.setRow1(titleRow.getRowNum());
                anchor.setCol2(0);
                anchor.setRow2(titleRow.getRowNum() + 1);
                drawing.createPicture(anchor, pictureIdx);
            }
        } catch (Exception ignored) {
            // If logo not found or insertion fails, continue without failing the whole export
        }

        return rowIndex;
    }

    private int writeBloque(Sheet sheet, Styles styles, String nombreBloque, List<HoraCatedra> horasBloque,
            Map<Integer, List<HorarioSlot>> slotsPorHoraCatedra, int dayCount, int lastCol, int rowIndex) {

        if (horasBloque.isEmpty()) {
            return rowIndex;
        }

        Row bloqueTitleRow = sheet.createRow(rowIndex++);
        Cell bloqueTitleCell = bloqueTitleRow.createCell(0);
        bloqueTitleCell.setCellValue(nombreBloque);
        bloqueTitleCell.setCellStyle(styles.bloqueTitle);
        sheet.addMergedRegion(new CellRangeAddress(bloqueTitleRow.getRowNum(), bloqueTitleRow.getRowNum(), 0, lastCol));

        Row headerRow = sheet.createRow(rowIndex++);
        Cell horaHeader = headerRow.createCell(0);
        horaHeader.setCellValue("Hora");
        horaHeader.setCellStyle(styles.header);
        for (int dia = 1; dia <= dayCount; dia++) {
            Cell cell = headerRow.createCell(dia);
            cell.setCellValue(DIAS[dia - 1]);
            cell.setCellStyle(styles.header);
        }

        // Filas de datos, con una fila de RECESO insertada donde hay un salto
        // en la hora de fin de una hora cátedra y el inicio de la siguiente.
        Map<Integer, Set<String>> salasPorDia = new LinkedHashMap<>();
        for (int dia = 1; dia <= dayCount; dia++) {
            salasPorDia.put(dia, new LinkedHashSet<>());
        }

        for (int i = 0; i < horasBloque.size(); i++) {
            HoraCatedra hora = horasBloque.get(i);

            Row row = sheet.createRow(rowIndex++);
            Cell horaCell = row.createCell(0);
            String horaInicio = hora.getHoraInicio() == null ? "" : hora.getHoraInicio().toString();
            String horaFin = hora.getHoraFin() == null ? "" : hora.getHoraFin().toString();
            horaCell.setCellValue((i + 1) + "° " + horaInicio + " - " + horaFin);
            horaCell.setCellStyle(styles.grid);

            for (int dia = 1; dia <= dayCount; dia++) {
                Cell cell = row.createCell(dia);
                cell.setCellStyle(styles.grid);
                HorarioSlot slot = findSlot(slotsPorHoraCatedra, hora.getId(), dia);
                if (slot != null) {
                    String materia = slot.getMateriaNombre() == null ? "" : slot.getMateriaNombre();
                    String profesor = slot.getProfesorNombre() == null ? "" : slot.getProfesorNombre();
                    String texto = materia.isBlank() ? profesor : (profesor.isBlank() ? materia : materia + "\n" + profesor);
                    if (slot.getSalaNombre() != null && !slot.getSalaNombre().isBlank()) {
                        texto += "\nSala: " + slot.getSalaNombre();
                    }
                    cell.setCellValue(texto);
                    if (slot.getSalaNombre() != null && !slot.getSalaNombre().isBlank()) {
                        salasPorDia.get(dia).add(slot.getSalaNombre());
                    }
                }
            }

            boolean hayHuecoDespues = i + 1 < horasBloque.size()
                    && !continuaSinHueco(hora, horasBloque.get(i + 1));
            if (hayHuecoDespues) {
                rowIndex = writeReceso(sheet, styles, dayCount, lastCol, rowIndex);
            }
        }

        Row salasRow = sheet.createRow(rowIndex++);
        Cell salasHeaderCell = salasRow.createCell(0);
        salasHeaderCell.setCellValue("Salas");
        salasHeaderCell.setCellStyle(styles.header);
        for (int dia = 1; dia <= dayCount; dia++) {
            Cell cell = salasRow.createCell(dia);
            cell.setCellValue(String.join(" / ", salasPorDia.get(dia)));
            cell.setCellStyle(styles.header);
        }

        return rowIndex;
    }

    private int writeReceso(Sheet sheet, Styles styles, int dayCount, int lastCol, int rowIndex) {
        Row recesoRow = sheet.createRow(rowIndex++);
        Cell recesoCell = recesoRow.createCell(0);
        recesoCell.setCellValue("RECESO");
        recesoCell.setCellStyle(styles.receso);
        sheet.addMergedRegion(new CellRangeAddress(recesoRow.getRowNum(), recesoRow.getRowNum(), 0, lastCol));
        for (int i = 1; i <= dayCount; i++) {
            Cell fillerCell = recesoRow.createCell(i);
            fillerCell.setCellStyle(styles.receso);
        }
        return rowIndex;
    }

    /** True si no hay hueco entre el fin de una hora cátedra y el inicio de la siguiente (mismo minuto). */
    private boolean continuaSinHueco(HoraCatedra anterior, HoraCatedra siguiente) {
        if (anterior.getHoraFin() == null || siguiente.getHoraInicio() == null) {
            return true;
        }
        return anterior.getHoraFin().equals(siguiente.getHoraInicio());
    }

    private HorarioSlot findSlot(Map<Integer, List<HorarioSlot>> slotsPorHoraCatedra, int horaCatedraId, int dia) {
        List<HorarioSlot> candidatos = slotsPorHoraCatedra.get(horaCatedraId);
        if (candidatos == null) {
            return null;
        }
        for (HorarioSlot slot : candidatos) {
            if (slot.getDiaSemana() == dia) {
                return slot;
            }
        }
        return null;
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
