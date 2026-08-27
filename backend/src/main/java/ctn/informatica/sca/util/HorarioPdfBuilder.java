package ctn.informatica.sca.util;

import ctn.informatica.sca.model.CursoBase;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class HorarioPdfBuilder {

    private static final float MARGIN = 28f;
    private static final float TITLE_SIZE = 16f;
    private static final float SUBTITLE_SIZE = 10.5f;
    private static final float BLOCK_TITLE_SIZE = 11f;
    private static final float HEADER_SIZE = 9.5f;
    private static final float BODY_SIZE = 9.4f;
    private static final float RECESO_SIZE = 9f;
    private static final float HOUR_WIDTH = 62f;
    private static final float HEADER_HEIGHT = 20f;
    private static final float ROW_HEIGHT = 28f;
    private static final float RECESO_HEIGHT = 18f;
    private static final float BLOCK_TITLE_HEIGHT = 18f;

    private final PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDFont regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    public PDDocument build(CursoBase curso, List<HoraCatedra> horas, List<HorarioSlot> slots) throws IOException {
        PDDocument document = new PDDocument();
        renderCourse(document, curso, horas, slots);
        return document;
    }

    public PDDocument buildEspecialidad(String especialidadNombre, List<CursoBase> cursos, List<HoraCatedra> horas, List<HorarioSlot> slots) throws IOException {
        PDDocument document = new PDDocument();
        List<CursoBase> safeCourses = cursos == null ? List.of() : cursos;
        if (safeCourses.isEmpty()) {
            renderEmptyDocument(document, especialidadNombre);
            return document;
        }

        for (CursoBase curso : safeCourses) {
            if (curso == null) {
                continue;
            }
            List<HorarioSlot> courseSlots = filterSlotsForCourse(slots, curso.getId());
            renderCourse(document, curso, horas, courseSlots);
        }

        return document;
    }

    private void renderCourse(PDDocument document, CursoBase curso, List<HoraCatedra> horas, List<HorarioSlot> slots) throws IOException {
        List<HorarioScheduleLayout.BlockLayout> blocks = HorarioScheduleLayout.buildBlocks(horas, slots, detectDayCount(slots));
        if (blocks.isEmpty()) {
            renderEmptyDocument(document, curso == null ? null : curso.getEspecialidad());
            return;
        }
        for (HorarioScheduleLayout.BlockLayout block : blocks) {
            PDPage page = new PDPage(landscapeA4());
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.OVERWRITE, true, true)) {
                renderPage(contentStream, page, curso, block);
            }
        }
    }

    private void renderEmptyDocument(PDDocument document, String label) throws IOException {
        PDPage page = new PDPage(landscapeA4());
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.OVERWRITE, true, true)) {
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();
            float y = height - MARGIN;
            y = drawText(contentStream, width, y, "HORARIO DE CLASES", TITLE_SIZE, boldFont, true);
            y -= 10f;
            drawText(contentStream, width, y, "No hay cursos para " + (label == null ? "esta especialidad" : label), SUBTITLE_SIZE, regularFont, true);
        }
    }

    private void renderPage(PDPageContentStream contentStream, PDPage page, CursoBase curso, HorarioScheduleLayout.BlockLayout block) throws IOException {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float availableWidth = pageWidth - (MARGIN * 2);
        float dayWidth = (availableWidth - HOUR_WIDTH) / block.dayCount;
        float y = pageHeight - MARGIN;

        y = drawText(contentStream, pageWidth, y, "HORARIO DE CLASES", TITLE_SIZE, boldFont, true);
        y -= 8f;
        String subtitle = curso == null ? "" : ("Curso: " + curso.getCursoOrdinal() + " " + curso.getEspecialidad()
                + "    Turno: Mañana - Tarde    Sección: " + curso.getSeccion());
        y = drawText(contentStream, pageWidth, y, subtitle, SUBTITLE_SIZE, boldFont, true);
        y -= 10f;

        y = drawFilledBanner(contentStream, MARGIN, y, availableWidth, BLOCK_TITLE_HEIGHT, block.name);
        y -= 2f;

        y = drawHeaderRow(contentStream, y, dayWidth, block.dayCount);
        y -= 1f;

        for (int rowIndex = 0; rowIndex < block.rows.size(); rowIndex++) {
            HorarioScheduleLayout.RowLayout row = block.rows.get(rowIndex);
            float rowHeight = row.receso ? RECESO_HEIGHT : ROW_HEIGHT;

            if (row.receso) {
                drawMergedCell(contentStream, MARGIN, y, availableWidth, rowHeight, row.horaLabel, RECESO_SIZE, new Color(243, 232, 234), Color.DARK_GRAY);
                y -= rowHeight;
                continue;
            }

            drawCell(contentStream, MARGIN, y, HOUR_WIDTH, rowHeight, row.horaLabel, BODY_SIZE, new Color(246, 247, 249), Color.DARK_GRAY);

            for (int day = 1; day <= block.dayCount; day++) {
                HorarioScheduleLayout.MergedRange merge = findMerge(block.mergedRanges, rowIndex, day);
                if (merge != null && merge.startRow == rowIndex) {
                    float spanHeight = sumHeights(block.rows, merge.startRow, merge.endRow);
                    HorarioScheduleLayout.CellLayout cellLayout = row.cells.get(day);
                    drawCell(contentStream, MARGIN + HOUR_WIDTH + ((day - 1) * dayWidth), y, dayWidth, spanHeight, cellLayout == null ? "" : cellLayout.text,
                            BODY_SIZE, Color.WHITE, Color.BLACK);
                    continue;
                }
                if (isMergedContinuation(block.mergedRanges, rowIndex, day)) {
                    continue;
                }

                HorarioScheduleLayout.CellLayout cellLayout = row.cells.get(day);
                drawCell(contentStream, MARGIN + HOUR_WIDTH + ((day - 1) * dayWidth), y, dayWidth, rowHeight,
                        cellLayout == null ? "" : cellLayout.text, BODY_SIZE, Color.WHITE, Color.BLACK);
            }

            y -= rowHeight;
        }

        drawFooter(contentStream, pageWidth, y - 10f, block);
    }

    private void drawFooter(PDPageContentStream contentStream, float pageWidth, float y, HorarioScheduleLayout.BlockLayout block) throws IOException {
        StringBuilder sb = new StringBuilder("Salas: ");
        boolean first = true;
        for (int day = 1; day <= block.dayCount; day++) {
            if (!first) {
                sb.append("  |  ");
            }
            first = false;
            sb.append(HorarioScheduleLayout.DIAS[day - 1]).append(": ");
            sb.append(String.join(" / ", block.salasPorDia.get(day)));
        }
        drawText(contentStream, pageWidth, y, sb.toString(), 8.4f, regularFont, false);
    }

    private float drawHeaderRow(PDPageContentStream contentStream, float y, float dayWidth, int dayCount) throws IOException {
        drawCell(contentStream, MARGIN, y, HOUR_WIDTH, HEADER_HEIGHT, "Hora", HEADER_SIZE, new Color(230, 232, 236), Color.BLACK);
        for (int day = 1; day <= dayCount; day++) {
            drawCell(contentStream, MARGIN + HOUR_WIDTH + ((day - 1) * dayWidth), y, dayWidth, HEADER_HEIGHT, HorarioScheduleLayout.DIAS[day - 1],
                    HEADER_SIZE, new Color(230, 232, 236), Color.BLACK);
        }
        return y - HEADER_HEIGHT;
    }

    private float drawFilledBanner(PDPageContentStream contentStream, float x, float y, float width, float height, String text) throws IOException {
        contentStream.setNonStrokingColor(new Color(185, 191, 199));
        contentStream.addRect(x, y - height, width, height);
        contentStream.fill();
        drawBorder(contentStream, x, y - height, width, height);
        drawTextInBox(contentStream, x, y - height, width, height, text, BLOCK_TITLE_SIZE, boldFont, true, Color.BLACK);
        return y - height;
    }

    private void drawCell(PDPageContentStream contentStream, float x, float y, float width, float height, String text, float fontSize, Color fill, Color stroke) throws IOException {
        contentStream.setNonStrokingColor(fill);
        contentStream.addRect(x, y - height, width, height);
        contentStream.fill();
        drawBorder(contentStream, x, y - height, width, height, stroke);
        drawTextInBox(contentStream, x, y - height, width, height, text, fontSize, regularFont, false, Color.BLACK);
    }

    private void drawMergedCell(PDPageContentStream contentStream, float x, float y, float width, float height, String text, float fontSize, Color fill, Color stroke) throws IOException {
        contentStream.setNonStrokingColor(fill);
        contentStream.addRect(x, y - height, width, height);
        contentStream.fill();
        drawBorder(contentStream, x, y - height, width, height, stroke);
        drawTextInBox(contentStream, x, y - height, width, height, text, fontSize, boldFont, true, Color.BLACK);
    }

    private void drawBorder(PDPageContentStream contentStream, float x, float y, float width, float height) throws IOException {
        drawBorder(contentStream, x, y, width, height, Color.BLACK);
    }

    private void drawBorder(PDPageContentStream contentStream, float x, float y, float width, float height, Color color) throws IOException {
        contentStream.setStrokingColor(color);
        contentStream.moveTo(x, y);
        contentStream.lineTo(x + width, y);
        contentStream.lineTo(x + width, y + height);
        contentStream.lineTo(x, y + height);
        contentStream.closePath();
        contentStream.stroke();
    }

    private float drawText(PDPageContentStream contentStream, float pageWidth, float y, String text, float fontSize, PDFont font, boolean centered) throws IOException {
        if (text == null || text.isBlank()) {
            return y;
        }
        float maxWidth = pageWidth - (MARGIN * 2);
        float textWidth = measureText(font, fontSize, text);
        float x = centered ? Math.max(MARGIN, (pageWidth - textWidth) / 2f) : MARGIN;
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y - fontSize);
        contentStream.showText(text);
        contentStream.endText();
        return y - fontSize - 2f;
    }

    private void drawTextInBox(PDPageContentStream contentStream, float x, float y, float width, float height, String text, float fontSize, PDFont font, boolean centerHorizontally, Color color) throws IOException {
        List<String> lines = wrapText(font, fontSize, text == null ? "" : text, width - 8f);
        if (lines.isEmpty()) {
            return;
        }
        float lineHeight = fontSize * 1.15f;
        float totalHeight = lineHeight * lines.size();
        float startY = y + ((height - totalHeight) / 2f) + (lines.size() > 1 ? lineHeight : 0f);
        contentStream.beginText();
        contentStream.setNonStrokingColor(color);
        contentStream.setFont(font, fontSize);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float lineWidth = measureText(font, fontSize, line);
            float tx = centerHorizontally ? x + Math.max(4f, (width - lineWidth) / 2f) : x + 4f;
            if (i == 0) {
                contentStream.newLineAtOffset(tx, startY);
            } else {
                contentStream.newLineAtOffset(0, -lineHeight);
            }
            contentStream.showText(line);
        }
        contentStream.endText();
    }

    private List<String> wrapText(PDFont font, float fontSize, String text, float maxWidth) throws IOException {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\\R")) {
            if (paragraph.isBlank()) {
                out.add("");
                continue;
            }
            String[] words = paragraph.trim().split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (measureText(font, fontSize, candidate) <= maxWidth || line.isEmpty()) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            if (!line.isEmpty()) {
                out.add(line.toString());
            }
        }
        return out;
    }

    private float measureText(PDFont font, float fontSize, String text) throws IOException {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    private HorarioScheduleLayout.MergedRange findMerge(List<HorarioScheduleLayout.MergedRange> merges, int rowIndex, int day) {
        for (HorarioScheduleLayout.MergedRange merge : merges) {
            if (merge.day == day && merge.startRow == rowIndex) {
                return merge;
            }
        }
        return null;
    }

    private boolean isMergedContinuation(List<HorarioScheduleLayout.MergedRange> merges, int rowIndex, int day) {
        for (HorarioScheduleLayout.MergedRange merge : merges) {
            if (merge.day == day && merge.startRow < rowIndex && merge.endRow >= rowIndex) {
                return true;
            }
        }
        return false;
    }

    private float sumHeights(List<HorarioScheduleLayout.RowLayout> rows, int startRow, int endRow) {
        float total = 0f;
        for (int i = startRow; i <= endRow; i++) {
            total += rows.get(i).receso ? RECESO_HEIGHT : ROW_HEIGHT;
        }
        return total;
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
        if (slots == null || slots.isEmpty()) {
            return Collections.emptyList();
        }
        List<HorarioSlot> out = new ArrayList<>();
        for (HorarioSlot slot : slots) {
            if (slot != null && slot.getCursoId() == cursoBaseId) {
                out.add(slot);
            }
        }
        return out;
    }

    private PDRectangle landscapeA4() {
        return new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    }
}
