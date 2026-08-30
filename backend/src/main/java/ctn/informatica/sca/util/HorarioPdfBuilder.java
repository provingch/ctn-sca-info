package ctn.informatica.sca.util;

import ctn.informatica.sca.model.CursoBase;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import java.awt.Color;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.poi.util.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HorarioPdfBuilder {

    private static final Logger log = LoggerFactory.getLogger(HorarioPdfBuilder.class);
    private static final HorarioTemplateStyles TEMPLATE = HorarioTemplateStyles.load();
    private static final float MARGIN = 28f;
    private static final float TITLE_SIZE = TEMPLATE.title().fontSizePt();
    private static final float SUBTITLE_SIZE = TEMPLATE.subtitle().fontSizePt();
    private static final float BLOCK_TITLE_SIZE = TEMPLATE.anioBanner().fontSizePt();
    private static final float HEADER_SIZE = TEMPLATE.headerDay().fontSizePt();
    private static final float MATERIA_SIZE = TEMPLATE.materiaCell().fontSizePt();
    private static final float PROFESOR_SIZE = TEMPLATE.profesorCell().fontSizePt();
    private static final float RECESO_SIZE = TEMPLATE.receso().fontSizePt();
    private static final float HEADER_HEIGHT = 20f;
    private static final float DETAIL_ROW_HEIGHT = 18f;
    private static final float RECESO_HEIGHT = 18f;
    private static final float BLOCK_TITLE_HEIGHT = 18f;
    private static final float BLOCK_GAP = 8f;
    private static final float PAGE_BOTTOM_MARGIN = 28f;
    private static final float MIN_LAYOUT_SCALE = 0.80f;

    private final PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDFont regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont italicFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

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

        int blockIndex = 0;
        while (blockIndex < blocks.size()) {
            PDPage page = new PDPage(portraitA4());
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.OVERWRITE, true, true)) {
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float y = drawCourseHeader(document, contentStream, pageWidth, pageHeight, curso);
                float layoutScale = computeLayoutScale(y - PAGE_BOTTOM_MARGIN, blocks);

                int pageStart = blockIndex;
                while (blockIndex < blocks.size()) {
                    HorarioScheduleLayout.BlockLayout block = blocks.get(blockIndex);
                    float requiredHeight = estimateBlockHeight(block, layoutScale);
                    if (blockIndex > pageStart && y - requiredHeight < PAGE_BOTTOM_MARGIN) {
                        break;
                    }
                    if (blockIndex == pageStart && y - requiredHeight < PAGE_BOTTOM_MARGIN) {
                        log.warn("Horario export PDF: el bloque {} no entra completo en una sola página; se renderizará igualmente", block.name);
                    }
                    y = renderBlock(contentStream, pageWidth, y, curso, block, layoutScale);
                    y -= scaled(BLOCK_GAP, layoutScale);
                    blockIndex++;
                }
            }
        }
    }

    private void renderEmptyDocument(PDDocument document, String label) throws IOException {
        PDPage page = new PDPage(portraitA4());
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.OVERWRITE, true, true)) {
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();
            float y = drawCourseHeader(document, contentStream, width, height, null);
            drawText(contentStream, width, y, "No hay cursos para " + (label == null ? "esta especialidad" : label), SUBTITLE_SIZE, regularFont, true, Color.BLACK);
        }
    }

    private float drawCourseHeader(PDDocument document, PDPageContentStream contentStream, float pageWidth, float pageHeight, CursoBase curso) throws IOException {
        float y = pageHeight - MARGIN;

        drawLogoIfPresent(document, contentStream, "/static/logo-institucional.png", MARGIN, y - 28f, 78f, 28f, "logo institucional");
        drawLogoIfPresent(document, contentStream, "/static/assets/png/logo-especialidad-" + SpecialtyColors.normalizeSpecialty(curso == null ? null : curso.getEspecialidad()) + ".png", pageWidth - MARGIN - 78f, y - 28f, 78f, 28f, "logo de especialidad");

        y = drawText(contentStream, pageWidth, y, "HORARIO DE CLASES", TITLE_SIZE, boldFont, true, Color.BLACK);
        y -= 8f;
        String subtitle = curso == null ? "" : ("Curso: " + curso.getCursoOrdinal() + " " + curso.getEspecialidad()
                + "    Turno: Mañana - Tarde    Sección: " + curso.getSeccion());
        y = drawText(contentStream, pageWidth, y, subtitle, SUBTITLE_SIZE, regularFont, true, Color.BLACK);
        return y - 10f;
    }

    private float renderBlock(PDPageContentStream contentStream, float pageWidth, float y, CursoBase curso, HorarioScheduleLayout.BlockLayout block, float layoutScale) throws IOException {
        float availableWidth = pageWidth - (MARGIN * 2);
        float templateUnits = TEMPLATE.totalTemplateUnits(block.dayCount);
        float hourWidth = availableWidth * TEMPLATE.hourColumnWidth() / templateUnits;
        float dayWidth = availableWidth * TEMPLATE.dayColumnWidth() / templateUnits;
        float blockTitleHeight = scaled(BLOCK_TITLE_HEIGHT, layoutScale);
        float headerHeight = scaled(HEADER_HEIGHT, layoutScale);
        float detailRowHeight = scaled(DETAIL_ROW_HEIGHT, layoutScale);
        float recesoHeight = scaled(RECESO_HEIGHT, layoutScale);
        float blockTitleSize = scaled(BLOCK_TITLE_SIZE, layoutScale);
        float headerSize = scaled(HEADER_SIZE, layoutScale);
        float materiaSize = scaled(MATERIA_SIZE, layoutScale);
        float profesorSize = scaled(PROFESOR_SIZE, layoutScale);
        float recesoSize = scaled(RECESO_SIZE, layoutScale);

        y = drawFilledBanner(contentStream, MARGIN, y, availableWidth, blockTitleHeight, block.name, TEMPLATE.anioBanner(), blockTitleSize);
        y -= scaled(2f, layoutScale);

        y = drawHeaderRow(contentStream, y, hourWidth, dayWidth, block.dayCount, TEMPLATE.headerDay(), headerHeight, headerSize);
        y -= scaled(1f, layoutScale);

        Color recesoFill = resolveRecesoFillColor(curso == null ? null : curso.getEspecialidad());
        Color recesoBorder = colorForHex(TEMPLATE.receso().borderColorHex(), new Color(153, 153, 153));
        Color recesoText = colorForHex(TEMPLATE.receso().fontColorHex(), Color.WHITE);
        for (int rowIndex = 0; rowIndex < block.rows.size(); rowIndex++) {
            HorarioScheduleLayout.RowLayout row = block.rows.get(rowIndex);
            float rowHeight = row.receso ? recesoHeight : (detailRowHeight * 2f);

            if (row.receso) {
                drawMergedCell(contentStream, MARGIN, y, availableWidth, rowHeight, row.horaLabel, recesoSize, recesoFill, recesoBorder, boldFont, recesoText);
                y -= rowHeight;
                continue;
            }

            drawMergedCell(contentStream, MARGIN, y, hourWidth, rowHeight, row.horaLabel, materiaSize, colorForHex(TEMPLATE.hourCell().fillColorHex(), Color.WHITE), colorForHex(TEMPLATE.hourCell().borderColorHex(), new Color(153, 153, 153)), regularFont, colorForHex(TEMPLATE.hourCell().fontColorHex(), Color.BLACK), false);

            for (int day = 1; day <= block.dayCount; day++) {
                HorarioScheduleLayout.MergedRange merge = findMerge(block.mergedRanges, rowIndex, day);
                if (merge != null && merge.startRow == rowIndex) {
                    float spanHeight = sumHeights(block.rows, merge.startRow, merge.endRow, layoutScale);
                    HorarioScheduleLayout.CellLayout cellLayout = row.cells.get(day);
                    drawMergedCell(contentStream, MARGIN + hourWidth + ((day - 1) * dayWidth), y, dayWidth, spanHeight,
                            cellLayout == null ? "" : cellLayout.materiaText, materiaSize, regularFont,
                            cellLayout == null ? "" : cellLayout.profesorText, profesorSize, italicFont,
                            colorForHex(TEMPLATE.materiaCell().fillColorHex(), Color.WHITE), colorForHex(TEMPLATE.materiaCell().borderColorHex(), new Color(153, 153, 153)),
                            colorForHex(TEMPLATE.materiaCell().fontColorHex(), Color.BLACK), colorForHex(TEMPLATE.profesorCell().fontColorHex(), Color.BLACK));
                    continue;
                }
                if (isMergedContinuation(block.mergedRanges, rowIndex, day)) {
                    continue;
                }

                HorarioScheduleLayout.CellLayout cellLayout = row.cells.get(day);
                float topHeight = detailRowHeight;
                float bottomHeight = detailRowHeight;
                drawCell(contentStream, MARGIN + hourWidth + ((day - 1) * dayWidth), y, dayWidth, topHeight,
                        cellLayout == null ? "" : cellLayout.materiaText, materiaSize, colorForHex(TEMPLATE.materiaCell().fillColorHex(), Color.WHITE), colorForHex(TEMPLATE.materiaCell().borderColorHex(), new Color(153, 153, 153)), regularFont, colorForHex(TEMPLATE.materiaCell().fontColorHex(), Color.BLACK));
                drawCell(contentStream, MARGIN + hourWidth + ((day - 1) * dayWidth), y - topHeight, dayWidth, bottomHeight,
                        cellLayout == null ? "" : cellLayout.profesorText, profesorSize, colorForHex(TEMPLATE.profesorCell().fillColorHex(), Color.WHITE), colorForHex(TEMPLATE.profesorCell().borderColorHex(), new Color(153, 153, 153)), italicFont, colorForHex(TEMPLATE.profesorCell().fontColorHex(), Color.BLACK));
            }

            y -= rowHeight;
        }

        return drawFooter(contentStream, pageWidth, y - 10f, block);
    }

    private float drawFooter(PDPageContentStream contentStream, float pageWidth, float y, HorarioScheduleLayout.BlockLayout block) throws IOException {
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
        return drawText(contentStream, pageWidth, y, sb.toString(), 8.4f, regularFont, false, Color.DARK_GRAY);
    }

    private float drawHeaderRow(PDPageContentStream contentStream, float y, float hourWidth, float dayWidth, int dayCount, HorarioTemplateStyles.CellStyleSpec spec, float headerHeight, float headerFontSize) throws IOException {
        Color fill = colorForHex(spec.fillColorHex(), new Color(64, 64, 64));
        Color border = colorForHex(spec.borderColorHex(), new Color(153, 153, 153));
        Color text = colorForHex(spec.fontColorHex(), Color.WHITE);
        drawCell(contentStream, MARGIN, y, hourWidth, headerHeight, "Hora", headerFontSize, fill, border, boldFont, text);
        for (int day = 1; day <= dayCount; day++) {
            drawCell(contentStream, MARGIN + hourWidth + ((day - 1) * dayWidth), y, dayWidth, headerHeight, HorarioScheduleLayout.DIAS[day - 1],
                    headerFontSize, fill, border, boldFont, text);
        }
        return y - headerHeight;
    }

    private float drawFilledBanner(PDPageContentStream contentStream, float x, float y, float width, float height, String text, HorarioTemplateStyles.CellStyleSpec spec, float fontSize) throws IOException {
        Color fill = colorForHex(spec.fillColorHex(), new Color(185, 191, 199));
        Color border = colorForHex(spec.borderColorHex(), new Color(153, 153, 153));
        Color textColor = colorForHex(spec.fontColorHex(), Color.BLACK);
        contentStream.setNonStrokingColor(fill);
        contentStream.addRect(x, y - height, width, height);
        contentStream.fill();
        drawBorder(contentStream, x, y - height, width, height, border);
        drawTextInBox(contentStream, x, y - height, width, height, text, fontSize, boldFont, true, textColor);
        return y - height;
    }

    private void drawCell(PDPageContentStream contentStream, float x, float y, float width, float height, String text, float fontSize, Color fill, Color stroke, PDFont font, Color textColor) throws IOException {
        contentStream.setNonStrokingColor(fill);
        contentStream.addRect(x, y - height, width, height);
        contentStream.fill();
        drawBorder(contentStream, x, y - height, width, height, stroke);
        drawClippedText(contentStream, x, y - height, width, height,
                () -> drawTextInBox(contentStream, x, y - height, width, height, text, fontSize, font, false, textColor));
    }

    private void drawMergedCell(PDPageContentStream contentStream, float x, float y, float width, float height, String text, float fontSize, Color fill, Color stroke, PDFont font, Color textColor) throws IOException {
        drawMergedCell(contentStream, x, y, width, height, text, fontSize, fill, stroke, font, textColor, true);
    }

    private void drawMergedCell(PDPageContentStream contentStream, float x, float y, float width, float height, String text, float fontSize, Color fill, Color stroke, PDFont font, Color textColor, boolean centerHorizontally) throws IOException {
        contentStream.setNonStrokingColor(fill);
        contentStream.addRect(x, y - height, width, height);
        contentStream.fill();
        drawBorder(contentStream, x, y - height, width, height, stroke);
        drawClippedText(contentStream, x, y - height, width, height,
                () -> drawTextInBox(contentStream, x, y - height, width, height, text, fontSize, font, centerHorizontally, textColor));
    }

    private void drawMergedCell(PDPageContentStream contentStream, float x, float y, float width, float height,
            String firstText, float firstFontSize, PDFont firstFont,
            String secondText, float secondFontSize, PDFont secondFont,
            Color fill, Color stroke, Color firstTextColor, Color secondTextColor) throws IOException {
        contentStream.setNonStrokingColor(fill);
        contentStream.addRect(x, y - height, width, height);
        contentStream.fill();
        drawBorder(contentStream, x, y - height, width, height, stroke);
        drawClippedText(contentStream, x, y - height, width, height,
                () -> drawTextInSplitBox(contentStream, x, y - height, width, height, firstText, firstFontSize, firstFont, secondText, secondFontSize, secondFont, true, firstTextColor, secondTextColor));
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

    private float drawText(PDPageContentStream contentStream, float pageWidth, float y, String text, float fontSize, PDFont font, boolean centered, Color color) throws IOException {
        if (text == null || text.isBlank()) {
            return y;
        }
        float maxWidth = pageWidth - (MARGIN * 2);
        float textWidth = measureText(font, fontSize, text);
        float x = centered ? Math.max(MARGIN, (pageWidth - textWidth) / 2f) : MARGIN;
        contentStream.beginText();
        contentStream.setNonStrokingColor(color);
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y - fontSize);
        contentStream.showText(text);
        contentStream.endText();
        return y - fontSize - 2f;
    }

    private void drawTextInBox(PDPageContentStream contentStream, float x, float y, float width, float height, String text, float fontSize, PDFont font, boolean centerHorizontally, Color color) throws IOException {
        List<String> lines = wrapText(font, fontSize, text == null ? "" : text, width - 8f);
        drawTextInBox(contentStream, x, y, width, height, lines, fontSize, font, centerHorizontally, color);
    }

    private void drawTextInBox(PDPageContentStream contentStream, float x, float y, float width, float height, List<String> lines, float fontSize, PDFont font, boolean centerHorizontally, Color color) throws IOException {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        float lineHeight = fontSize * 1.15f;
        float totalHeight = lineHeight * lines.size();
        float startY = y + ((height - totalHeight) / 2f) + ((lines.size() - 1) * lineHeight);
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

    private void drawTextInSplitBox(PDPageContentStream contentStream, float x, float y, float width, float height,
            String firstText, float firstFontSize, PDFont firstFont,
            String secondText, float secondFontSize, PDFont secondFont,
            boolean centerHorizontally, Color firstTextColor, Color secondTextColor) throws IOException {
        drawClippedText(contentStream, x, y, width, height, () -> {
            List<String> firstLines = wrapText(firstFont, firstFontSize, firstText == null ? "" : firstText, width - 8f);
            List<String> secondLines = wrapText(secondFont, secondFontSize, secondText == null ? "" : secondText, width - 8f);
            if (firstLines.isEmpty() && secondLines.isEmpty()) {
                return;
            }

            float firstHeight = textBlockHeight(firstLines, firstFontSize);
            float secondHeight = textBlockHeight(secondLines, secondFontSize);
            float gap = firstLines.isEmpty() || secondLines.isEmpty() ? 0f : Math.min(firstFontSize, secondFontSize) * 0.15f;
            float totalHeight = firstHeight + gap + secondHeight;
            float startY = y + Math.max(0f, (height - totalHeight) / 2f);

            if (!firstLines.isEmpty()) {
                drawTextInBox(contentStream, x, startY + secondHeight + gap, width, firstHeight, firstLines, firstFontSize, firstFont, centerHorizontally, firstTextColor);
            }
            if (!secondLines.isEmpty()) {
                drawTextInBox(contentStream, x, startY, width, secondHeight, secondLines, secondFontSize, secondFont, centerHorizontally, secondTextColor);
            }
        });
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
                if (measureText(font, fontSize, candidate) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    if (!line.isEmpty()) {
                        out.add(line.toString());
                        line.setLength(0);
                    }
                    if (measureText(font, fontSize, word) <= maxWidth) {
                        line.append(word);
                    } else {
                        out.addAll(splitLongWord(font, fontSize, word, maxWidth));
                    }
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

    private List<String> splitLongWord(PDFont font, float fontSize, String word, float maxWidth) throws IOException {
        if (word == null || word.isEmpty()) {
            return List.of();
        }
        List<String> parts = new ArrayList<>();
        String remaining = word;
        while (!remaining.isEmpty()) {
            int cut = fitPrefixLength(font, fontSize, remaining, maxWidth);
            if (cut <= 0) {
                cut = 1;
            }
            parts.add(remaining.substring(0, cut));
            remaining = remaining.substring(cut);
        }
        return parts;
    }

    private int fitPrefixLength(PDFont font, float fontSize, String word, float maxWidth) throws IOException {
        int low = 1;
        int high = word.length();
        int best = 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            String prefix = word.substring(0, mid);
            if (measureText(font, fontSize, prefix) <= maxWidth) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return best;
    }

    private void drawClippedText(PDPageContentStream contentStream, float x, float y, float width, float height, IOExceptionRunnable runnable) throws IOException {
        contentStream.saveGraphicsState();
        try {
            contentStream.addRect(x, y, width, height);
            contentStream.clip();
            runnable.run();
        } finally {
            contentStream.restoreGraphicsState();
        }
    }

    @FunctionalInterface
    private interface IOExceptionRunnable {
        void run() throws IOException;
    }

    private void drawLogoIfPresent(PDDocument document, PDPageContentStream contentStream, String resourcePath, float x, float y, float width, float height, String label) {
        try (InputStream is = HorarioPdfBuilder.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("Horario export PDF: no se encontró {} en {}", label, resourcePath);
                return;
            }
            byte[] bytes = IOUtils.toByteArray(is);
            PDImageXObject image = PDImageXObject.createFromByteArray(document, bytes, label);
            contentStream.drawImage(image, x, y, width, height);
        } catch (Exception ex) {
            log.warn("Horario export PDF: no se pudo insertar {} desde {}", label, resourcePath, ex);
        }
    }

    private static Color colorForHex(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) {
            return fallback;
        }
        String clean = hex.replace("#", "").trim();
        if (clean.length() != 6) {
            return fallback;
        }
        try {
            return new Color(Integer.parseInt(clean.substring(0, 2), 16), Integer.parseInt(clean.substring(2, 4), 16), Integer.parseInt(clean.substring(4, 6), 16));
        } catch (Exception ex) {
            return fallback;
        }
    }

    static String resolveRecesoFillHex(String specialty) {
        return SpecialtyColors.getAccent(specialty);
    }

    static Color resolveRecesoFillColor(String specialty) {
        return colorForHex(resolveRecesoFillHex(specialty), colorForHex(TEMPLATE.receso().fillColorHex(), new Color(191, 191, 191)));
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

    private float sumHeights(List<HorarioScheduleLayout.RowLayout> rows, int startRow, int endRow, float layoutScale) {
        float total = 0f;
        for (int i = startRow; i <= endRow; i++) {
            total += rows.get(i).receso ? scaled(RECESO_HEIGHT, layoutScale) : (scaled(DETAIL_ROW_HEIGHT, layoutScale) * 2f);
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

    private float estimateBlockHeight(HorarioScheduleLayout.BlockLayout block, float layoutScale) {
        float rowsHeight = 0f;
        for (HorarioScheduleLayout.RowLayout row : block.rows) {
            rowsHeight += row.receso ? scaled(RECESO_HEIGHT, layoutScale) : (scaled(DETAIL_ROW_HEIGHT, layoutScale) * 2f);
        }
        return scaled(BLOCK_TITLE_HEIGHT, layoutScale) + scaled(HEADER_HEIGHT, layoutScale) + rowsHeight + 24f;
    }

    private float textBlockHeight(List<String> lines, float fontSize) {
        if (lines == null || lines.isEmpty()) {
            return 0f;
        }
        return fontSize * 1.15f * lines.size();
    }

    private float computeLayoutScale(float availableHeight, List<HorarioScheduleLayout.BlockLayout> blocks) {
        if (availableHeight <= 0f || blocks == null || blocks.isEmpty()) {
            return 1f;
        }
        float requiredHeight = 0f;
        for (HorarioScheduleLayout.BlockLayout block : blocks) {
            if (block != null) {
                requiredHeight += estimateBlockHeight(block, 1f);
            }
        }
        requiredHeight += BLOCK_GAP * Math.max(0, blocks.size() - 1);
        if (requiredHeight <= availableHeight) {
            return 1f;
        }
        return Math.max(MIN_LAYOUT_SCALE, availableHeight / requiredHeight);
    }

    private float scaled(float value, float layoutScale) {
        return value * layoutScale;
    }

    private PDRectangle portraitA4() {
        return new PDRectangle(PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight());
    }
}