package ctn.informatica.sca.util;

import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HorarioTemplateStyles {

    private static final Logger log = LoggerFactory.getLogger(HorarioTemplateStyles.class);
    private static final String TEMPLATE_RESOURCE = "/templates/plantilla-horario.xlsx";
    private static final String SHEET_NAME = "PLANTILLA_HORARIO";

    private final CellStyleSpec title;
    private final CellStyleSpec subtitle;
    private final CellStyleSpec headerDay;
    private final CellStyleSpec hourCell;
    private final CellStyleSpec materiaCell;
    private final CellStyleSpec profesorCell;
    private final CellStyleSpec salasLabel;
    private final CellStyleSpec salasData;
    private final CellStyleSpec receso;
    private final CellStyleSpec blockBanner;
    private final int hourColumnWidth;
    private final int dayColumnWidth;

    private HorarioTemplateStyles(
        CellStyleSpec title,
        CellStyleSpec subtitle,
        CellStyleSpec headerDay,
        CellStyleSpec hourCell,
        CellStyleSpec materiaCell,
        CellStyleSpec profesorCell,
        CellStyleSpec salasLabel,
        CellStyleSpec salasData,
        CellStyleSpec receso,
        CellStyleSpec blockBanner,
        int hourColumnWidth,
        int dayColumnWidth
    ) {
        this.title = title;
        this.subtitle = subtitle;
        this.headerDay = headerDay;
        this.hourCell = hourCell;
        this.materiaCell = materiaCell;
        this.profesorCell = profesorCell;
        this.salasLabel = salasLabel;
        this.salasData = salasData;
        this.receso = receso;
        this.blockBanner = blockBanner;
        this.hourColumnWidth = hourColumnWidth;
        this.dayColumnWidth = dayColumnWidth;
    }

    public static HorarioTemplateStyles load() {
        try (InputStream is = HorarioTemplateStyles.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (is == null) {
                log.warn("Horario template styles: no se encontró el recurso {}", TEMPLATE_RESOURCE);
                return fallback();
            }

            try (XSSFWorkbook workbook = new XSSFWorkbook(is)) {
                Sheet sheet = workbook.getSheet(SHEET_NAME);
                if (sheet == null) {
                    sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
                }
                if (sheet == null) {
                    log.warn("Horario template styles: la plantilla no tiene hojas utilizables");
                    return fallback();
                }
                return fromSheet(sheet);
            }
        } catch (Exception ex) {
            log.warn("Horario template styles: no se pudo leer la plantilla {}, usando fallback", TEMPLATE_RESOURCE, ex);
            return fallback();
        }
    }

    public CellStyleSpec title() {
        return title;
    }

    public CellStyleSpec subtitle() {
        return subtitle;
    }

    public CellStyleSpec headerDay() {
        return headerDay;
    }

    public CellStyleSpec hourCell() {
        return hourCell;
    }

    public CellStyleSpec materiaCell() {
        return materiaCell;
    }

    public CellStyleSpec profesorCell() {
        return profesorCell;
    }

    public CellStyleSpec salasLabel() {
        return salasLabel;
    }

    public CellStyleSpec salasData() {
        return salasData;
    }

    public CellStyleSpec receso() {
        return receso;
    }

    public CellStyleSpec blockBanner() {
        return blockBanner;
    }

    public int hourColumnWidth() {
        return hourColumnWidth;
    }

    public int dayColumnWidth() {
        return dayColumnWidth;
    }

    public int totalTemplateUnits(int dayCount) {
        return hourColumnWidth + (dayColumnWidth * dayCount);
    }

    private static HorarioTemplateStyles fromSheet(Sheet sheet) {
        CellStyleSpec title = fromCell(sheet, "C1");
        CellStyleSpec subtitle = fromCell(sheet, "C2");
        CellStyleSpec headerDay = fromCell(sheet, "A6");
        CellStyleSpec hourCell = fromCell(sheet, "A7");
        CellStyleSpec materiaCell = fromCell(sheet, "B7");
        CellStyleSpec profesorCell = fromCell(sheet, "B8");
        CellStyleSpec salasLabel = fromCell(sheet, "A11");
        CellStyleSpec salasData = fromCell(sheet, "B11");
        CellStyleSpec receso = fromCell(sheet, "A12");

        CellStyleSpec blockBanner = CellStyleSpec.banner(
            "Calibri",
            (short) 12,
            true,
            false,
            "000000",
            "B9BFC7",
            "999999",
            BorderStyle.THIN,
            HorizontalAlignment.CENTER,
            VerticalAlignment.CENTER,
            true
        );

        int hourColumnWidth = sheet.getColumnWidth(0);
        int dayColumnWidth = sheet.getColumnWidth(1);
        return new HorarioTemplateStyles(title, subtitle, headerDay, hourCell, materiaCell, profesorCell, salasLabel, salasData, receso, blockBanner, hourColumnWidth, dayColumnWidth);
    }

    private static CellStyleSpec fromCell(Sheet sheet, String cellRef) {
        Cell cell = getCell(sheet, cellRef);
        if (cell == null) {
            return CellStyleSpec.defaultStyle();
        }
        XSSFCellStyle style = (XSSFCellStyle) cell.getCellStyle();
        XSSFFont font = style.getFont();
        String fontColor = colorHex(font.getXSSFColor());
        String fillColor = colorHex(style.getFillForegroundColorColor());
        String borderColor = colorHex(style.getTopBorderXSSFColor());
        return new CellStyleSpec(
            font.getFontName(),
            font.getFontHeightInPoints(),
            font.getBold(),
            font.getItalic(),
            fontColor,
            fillColor,
            borderColor,
            style.getBorderTop(),
            style.getBorderRight(),
            style.getBorderBottom(),
            style.getBorderLeft(),
            style.getAlignment(),
            style.getVerticalAlignment(),
            style.getWrapText()
        );
    }

    private static Cell getCell(Sheet sheet, String ref) {
        if (sheet == null || ref == null || ref.isBlank()) {
            return null;
        }
        int rowIndex = rowIndex(ref);
        int colIndex = columnIndex(ref);
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return null;
        }
        return row.getCell(colIndex);
    }

    private static int rowIndex(String ref) {
        int start = 0;
        while (start < ref.length() && Character.isLetter(ref.charAt(start))) {
            start++;
        }
        return Integer.parseInt(ref.substring(start)) - 1;
    }

    private static int columnIndex(String ref) {
        int value = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = ref.charAt(i);
            if (!Character.isLetter(ch)) {
                break;
            }
            value = (value * 26) + (Character.toUpperCase(ch) - 'A' + 1);
        }
        return value - 1;
    }

    private static String colorHex(XSSFColor color) {
        if (color == null) {
            return null;
        }
        byte[] rgb = color.getRGB();
        if (rgb == null) {
            rgb = color.getARGB();
            if (rgb != null && rgb.length == 4) {
                return toHex(rgb[1], rgb[2], rgb[3]);
            }
            return null;
        }
        if (rgb.length >= 3) {
            return toHex(rgb[0], rgb[1], rgb[2]);
        }
        return null;
    }

    private static String toHex(byte r, byte g, byte b) {
        return String.format("%02X%02X%02X", r & 0xFF, g & 0xFF, b & 0xFF);
    }

    private static HorarioTemplateStyles fallback() {
        CellStyleSpec title = new CellStyleSpec("Calibri", (short) 13, true, false, "000000", null, null, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
        CellStyleSpec subtitle = new CellStyleSpec("Calibri", (short) 11, false, false, "000000", null, null, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
        CellStyleSpec headerDay = new CellStyleSpec("Calibri", (short) 11, true, false, "FFFFFF", "404040", "999999", BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true);
        CellStyleSpec hourCell = new CellStyleSpec("Calibri", (short) 11, false, false, "000000", null, "999999", BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true);
        CellStyleSpec materiaCell = new CellStyleSpec("Calibri", (short) 11, false, false, "000000", null, "999999", BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true);
        CellStyleSpec profesorCell = new CellStyleSpec("Calibri", (short) 9, false, true, "000000", null, "999999", BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true);
        CellStyleSpec salasLabel = new CellStyleSpec("Calibri", (short) 11, true, false, "000000", "D9D9D9", "999999", BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
        CellStyleSpec salasData = new CellStyleSpec("Calibri", (short) 9, true, false, "000000", "D9D9D9", "999999", BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
        CellStyleSpec receso = new CellStyleSpec("Calibri", (short) 11, true, false, "FFFFFF", "BFBFBF", "999999", BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false);
        CellStyleSpec blockBanner = CellStyleSpec.banner("Calibri", (short) 12, true, false, "000000", "B9BFC7", "999999", BorderStyle.THIN, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true);
        return new HorarioTemplateStyles(title, subtitle, headerDay, hourCell, materiaCell, profesorCell, salasLabel, salasData, receso, blockBanner, 16 * 256, 20 * 256);
    }

    public static final class CellStyleSpec {
        private final String fontName;
        private final short fontSizePt;
        private final boolean bold;
        private final boolean italic;
        private final String fontColorHex;
        private final String fillColorHex;
        private final String borderColorHex;
        private final BorderStyle borderTop;
        private final BorderStyle borderRight;
        private final BorderStyle borderBottom;
        private final BorderStyle borderLeft;
        private final HorizontalAlignment horizontalAlignment;
        private final VerticalAlignment verticalAlignment;
        private final boolean wrapText;

        private CellStyleSpec(
            String fontName,
            short fontSizePt,
            boolean bold,
            boolean italic,
            String fontColorHex,
            String fillColorHex,
            String borderColorHex,
            BorderStyle borderTop,
            BorderStyle borderRight,
            BorderStyle borderBottom,
            BorderStyle borderLeft,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment,
            boolean wrapText
        ) {
            this.fontName = fontName;
            this.fontSizePt = fontSizePt;
            this.bold = bold;
            this.italic = italic;
            this.fontColorHex = fontColorHex;
            this.fillColorHex = fillColorHex;
            this.borderColorHex = borderColorHex;
            this.borderTop = borderTop;
            this.borderRight = borderRight;
            this.borderBottom = borderBottom;
            this.borderLeft = borderLeft;
            this.horizontalAlignment = horizontalAlignment;
            this.verticalAlignment = verticalAlignment;
            this.wrapText = wrapText;
        }

        static CellStyleSpec defaultStyle() {
            return new CellStyleSpec("Calibri", (short) 11, false, false, "000000", null, null, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false);
        }

        static CellStyleSpec banner(String fontName, short fontSizePt, boolean bold, boolean italic, String fontColorHex, String fillColorHex, String borderColorHex, BorderStyle borderStyle, HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, boolean wrapText) {
            return new CellStyleSpec(fontName, fontSizePt, bold, italic, fontColorHex, fillColorHex, borderColorHex, borderStyle, borderStyle, borderStyle, borderStyle, horizontalAlignment, verticalAlignment, wrapText);
        }

        public String fontName() {
            return fontName;
        }

        public short fontSizePt() {
            return fontSizePt;
        }

        public boolean bold() {
            return bold;
        }

        public boolean italic() {
            return italic;
        }

        public String fontColorHex() {
            return fontColorHex;
        }

        public String fillColorHex() {
            return fillColorHex;
        }

        public String borderColorHex() {
            return borderColorHex;
        }

        public BorderStyle borderTop() {
            return borderTop;
        }

        public BorderStyle borderRight() {
            return borderRight;
        }

        public BorderStyle borderBottom() {
            return borderBottom;
        }

        public BorderStyle borderLeft() {
            return borderLeft;
        }

        public HorizontalAlignment horizontalAlignment() {
            return horizontalAlignment;
        }

        public VerticalAlignment verticalAlignment() {
            return verticalAlignment;
        }

        public boolean wrapText() {
            return wrapText;
        }

        public java.awt.Color toAwtFontColor() {
            return toAwtColor(fontColorHex, java.awt.Color.BLACK);
        }

        public java.awt.Color toAwtFillColor(java.awt.Color fallback) {
            return toAwtColor(fillColorHex, fallback);
        }

        private static java.awt.Color toAwtColor(String hex, java.awt.Color fallback) {
            if (hex == null || hex.isBlank()) {
                return fallback;
            }
            String clean = hex.replace("#", "");
            if (clean.length() != 6) {
                return fallback;
            }
            try {
                return new java.awt.Color(Integer.parseInt(clean.substring(0, 2), 16), Integer.parseInt(clean.substring(2, 4), 16), Integer.parseInt(clean.substring(4, 6), 16));
            } catch (Exception ex) {
                return fallback;
            }
        }
    }
}
