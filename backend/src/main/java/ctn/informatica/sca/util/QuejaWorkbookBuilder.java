package ctn.informatica.sca.util;

import ctn.informatica.sca.dao.QuejaDao.Documento;
import java.awt.Color;
import java.io.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

/** Solicitud protegida: solo aceptación, fecha y observaciones son editables. */
public class QuejaWorkbookBuilder {
    private XSSFWorkbook book;
    private XSSFSheet sheet;
    private XSSFCellStyle title, section, label, value, editable, date;
    private int row;

    public byte[] build(Documento q, LocalDateTime generatedAt) throws IOException {
        if (q.revisadaEn() != null || q.resueltaEn() != null) throw new IllegalArgumentException("La queja no está pendiente");
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            book = workbook; sheet = book.createSheet("Solicitud de revisión"); row = 0;
            title = style(16, true, "14233B", "FFFFFF", true);
            section = style(11, true, "FFFFFF", "3144CF", true);
            label = style(10, true, "14233B", "EEF0FF", true);
            value = style(11, false, "14233B", "FFFFFF", true);
            editable = style(11, false, "14233B", "FFF3D6", false);
            date = style(11, false, "14233B", "FFFFFF", true);
            date.setDataFormat(book.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            for (int col = 0; col < 8; col++) sheet.setColumnWidth(col, 11 * 256);
            sheet.setDisplayGridlines(false);
            merged(0, 2, 7, "Colegio Técnico Nacional de Asunción", title, 24);
            merged(1, 2, 7, "Sistema de Carpetas Académicas", value, 20);
            try (InputStream logo = getClass().getResourceAsStream("/static/logo-institucional.png")) {
                if (logo == null) throw new IOException("Logo institucional no disponible");
                byte[] imageBytes = logo.readAllBytes();
                var image = javax.imageio.ImageIO.read(new ByteArrayInputStream(imageBytes));
                int picture = book.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
                double scale = Math.min(150d / image.getWidth(), 55d / image.getHeight());
                double width = image.getWidth() * scale, height = image.getHeight() * scale;
                double colWidth = sheet.getColumnWidthInPixels(0), rowHeight = sheet.getRow(0).getHeightInPoints() * 4 / 3;
                int endCol = width >= colWidth ? 1 : 0, endRow = height >= rowHeight ? 1 : 0;
                XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0,
                        org.apache.poi.util.Units.pixelToEMU((int) (width - endCol * colWidth)),
                        org.apache.poi.util.Units.pixelToEMU((int) (height - endRow * rowHeight)), 0, 0, endCol, endRow);
                anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
                sheet.createDrawingPatriarch().createPicture(anchor, picture);
            }
            merged(2, 0, 7, "SOLICITUD DE REVISIÓN DE QUEJA", title, 28);
            merged(3, 0, 7, "Queja #" + q.id() + " - Pendiente de revisión", value, 20);
            row = 5;
            field("Coordinación destinataria", "Coordinación de " + QuejaPdfBuilder.value(q.especialidad()));
            field("Especialidad de origen", QuejaPdfBuilder.value(q.especialidad()));
            field("Curso / Profesor", QuejaPdfBuilder.value(q.curso()) + " / " + QuejaPdfBuilder.value(q.profesor()));
            int registeredRow = row;
            field("Fecha de la queja", "");
            if (q.creadaEn() != null) {
                Cell cell = sheet.getRow(registeredRow).getCell(3);
                cell.setCellValue(q.creadaEn().toLocalDateTime()); cell.setCellStyle(date);
            }
            int generatedRow = row;
            field("Fecha de generación", "");
            Cell generated = sheet.getRow(generatedRow).getCell(3);
            generated.setCellValue(generatedAt); generated.setCellStyle(date);
            row++;
            merged(row++, 0, 7, "QUEJA ORIGINAL", section, 26);
            String original = q.motivo() == null ? "" : q.motivo();
            // Reparte textos extensos sin recortes y sin superar el límite de altura de Excel.
            for (int offset = 0; offset < original.length() || offset == 0;) {
                int end = Math.min(offset + 240, original.length());
                int breaks = 0;
                for (int i = offset; i < end; i++) if (original.charAt(i) == '\n' && ++breaks == 5) { end = i + 1; break; }
                if (end < original.length() && Character.isHighSurrogate(original.charAt(end - 1))) end--;
                String part = original.substring(offset, end);
                int explicitLines = part.split("\\R", -1).length;
                merged(row++, 0, 7, part, value, 12 + Math.max(3, explicitLines + 2) * 15);
                offset = end; if (end == original.length()) break;
            }
            row++;
            float usedHeight = 0;
            for (int i = 0; i < row; i++) usedHeight += sheet.getRow(i) == null ? 15 : sheet.getRow(i).getHeightInPoints();
            if (usedHeight > 380) sheet.setRowBreak(row - 1);
            merged(row++, 0, 7, "ACEPTACIÓN DE LA REVISIÓN", section, 26);
            merged(row++, 0, 7, "Acepto que se revise la queja identificada en esta solicitud.", value, 20);
            merged(row++, 0, 7, "Completá únicamente las celdas amarillas: nombre/firma, fecha de aceptación y observaciones.", value, 28);
            merged(row++, 0, 7, "Nombre o firma del coordinador o la coordinadora de la especialidad", label, 24);
            merged(row++, 0, 7, "", editable, 40);
            merged(row++, 0, 7, "Fecha de aceptación", label, 24);
            XSSFCellStyle inputDate = book.createCellStyle(); inputDate.cloneStyleFrom(editable);
            inputDate.setDataFormat(book.createDataFormat().getFormat("dd/mm/yyyy"));
            int acceptanceRow = row;
            merged(row++, 0, 7, "", inputDate, 28);
            DataValidationHelper helper = sheet.getDataValidationHelper();
            DataValidation validation = helper.createValidation(helper.createDateConstraint(DataValidationConstraint.OperatorType.BETWEEN,
                    "DATE(2000,1,1)", "DATE(2100,12,31)", "dd/mm/yyyy"),
                    new org.apache.poi.ss.util.CellRangeAddressList(acceptanceRow, acceptanceRow, 0, 0));
            validation.setEmptyCellAllowed(true); validation.setShowErrorBox(true);
            validation.createErrorBox("Fecha inválida", "Ingresá una fecha válida entre 2000 y 2100."); sheet.addValidationData(validation);
            merged(row++, 0, 7, "Observaciones", label, 24);
            merged(row++, 0, 7, "", editable, 55);
            row++;
            merged(row++, 0, 7, "Esta solicitud se completa en el archivo y se entrega a la coordinación de la especialidad.", value, 28);
            sheet.protectSheet(UUID.randomUUID().toString());
            sheet.lockSelectLockedCells(true); sheet.lockSelectUnlockedCells(false);
            sheet.setAutobreaks(true); sheet.setFitToPage(true);
            PrintSetup print = sheet.getPrintSetup(); print.setPaperSize(PrintSetup.A4_PAPERSIZE);
            print.setLandscape(false); print.setFitWidth((short) 1); print.setFitHeight((short) 0);
            sheet.setMargin(Sheet.LeftMargin, .35); sheet.setMargin(Sheet.RightMargin, .35);
            sheet.setMargin(Sheet.TopMargin, .4); sheet.setMargin(Sheet.BottomMargin, .45);
            sheet.setRepeatingRows(new CellRangeAddress(0, 3, -1, -1));
            sheet.getFooter().setLeft("Queja #" + q.id()); sheet.getFooter().setRight("Página &P de &N");
            book.setPrintArea(0, 0, 7, 0, row - 1);
            book.write(bytes); return bytes.toByteArray();
        }
    }
    private void field(String name, String content) {
        float height = Math.max(28, 8 + (float) Math.ceil(content.length() / 44d) * 15);
        merged(row, 0, 2, name, label, height); merged(row++, 3, 7, content, value, height);
    }
    private void merged(int index, int first, int last, String content, CellStyle style, float height) {
        Row r = sheet.getRow(index); if (r == null) r = sheet.createRow(index); r.setHeightInPoints(height);
        for (int c = first; c <= last; c++) { Cell cell = r.createCell(c); cell.setCellStyle(style); }
        if (content.isEmpty()) r.getCell(first).setBlank();
        else r.getCell(first).setCellValue(content);
        sheet.addMergedRegion(new CellRangeAddress(index, index, first, last));
    }
    private XSSFCellStyle style(int size, boolean bold, String ink, String fill, boolean locked) {
        XSSFCellStyle style = book.createCellStyle(); XSSFFont font = book.createFont();
        font.setFontName("Arial"); font.setFontHeightInPoints((short) size); font.setBold(bold);
        font.setColor(new XSSFColor(Color.decode("#" + ink), null)); style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(Color.decode("#" + fill), null)); style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true); style.setVerticalAlignment(VerticalAlignment.CENTER); style.setLocked(locked);
        return style;
    }
}
