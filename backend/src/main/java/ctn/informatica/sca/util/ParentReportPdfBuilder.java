package ctn.informatica.sca.util;

import ctn.informatica.sca.model.ParentSummaryItem;
import ctn.informatica.sca.model.ParentTaskGrade;
import ctn.informatica.sca.model.Planilla;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Genera los PDF de la vista de padres: el reporte mensual por alumno y la
 * libreta (boletín consolidado). Es un renderer puro — el controller resuelve
 * autenticación y datos ({@code PadreDao.findParentSummary} /
 * {@code findTaskGradesForAlumno}) y se los pasa ya filtrados por alumno.
 */
public class ParentReportPdfBuilder {

    private static final Logger log = LoggerFactory.getLogger(ParentReportPdfBuilder.class);
    private static final String LOGO_RESOURCE = "/static/logo-sca-color.png";
    private static final float MARGIN = 42f;
    private static final float BOTTOM_MARGIN = 48f;
    private static final float LINE_GAP = 4f;
    private static final Color INK = new Color(0x14, 0x23, 0x3B);
    private static final Color MUTED = new Color(0x64, 0x74, 0x8B);
    private static final Color RULE = new Color(0xD4, 0xDE, 0xE9);
    private static final Color HEADER_FILL = new Color(0xEE, 0xF0, 0xFF);
    private static final Color ACCENT = new Color(0x31, 0x44, 0xCF);

    private static final String[] MESES = {
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    // ---- API pública ---------------------------------------------------------

    public PDDocument buildMonthlyReport(String alumnoNombre, String especialidad, int mes, int anio,
            List<ParentSummaryItem> items, List<ParentTaskGrade> tasks) throws IOException {
        PDDocument doc = new PDDocument();
        Page page = newPage(doc);
        String mesLabel = (mes >= 1 && mes <= 12) ? MESES[mes - 1] : String.valueOf(mes);
        drawHeader(doc, page, "REPORTE MENSUAL", alumnoNombre, especialidad,
                "Período: " + capitalize(mesLabel) + " " + anio);

        Map<Integer, String> materiaPorPlanilla = new LinkedHashMap<>();
        Map<Integer, String> etapaPorPlanilla = new LinkedHashMap<>();
        for (ParentSummaryItem item : items) {
            if (item.getPlanillaId() != null) {
                materiaPorPlanilla.putIfAbsent(item.getPlanillaId(), item.getMateriaNombre());
                etapaPorPlanilla.putIfAbsent(item.getPlanillaId(), etapaLabel(item.getEtapa()));
            }
        }

        Map<Integer, List<ParentTaskGrade>> delMes = new LinkedHashMap<>();
        for (ParentTaskGrade task : tasks) {
            LocalDate f = task.getFecha();
            if (f == null || f.getYear() != anio || f.getMonthValue() != mes) {
                continue;
            }
            delMes.computeIfAbsent(task.getPlanillaId(), k -> new ArrayList<>()).add(task);
        }

        if (delMes.isEmpty()) {
            text(page, MARGIN, page.y, "No hay tareas registradas en " + mesLabel + " de " + anio + ".", 11f, regular, MUTED);
            finish(page);
            return doc;
        }

        String[] cols = {"#", "Tarea", "Fecha", "Puntaje", "Estado"};
        float[] widths = {24f, 210f, 78f, 78f, 122f};

        for (Map.Entry<Integer, List<ParentTaskGrade>> entry : delMes.entrySet()) {
            int planillaId = entry.getKey();
            List<ParentTaskGrade> lista = entry.getValue();
            page = ensureSpace(doc, page, 96f);
            String titulo = materiaPorPlanilla.getOrDefault(planillaId, "Materia");
            String etapa = etapaPorPlanilla.getOrDefault(planillaId, "");
            page.y -= 8f;
            page.y = text(page, MARGIN, page.y, titulo + (etapa.isBlank() ? "" : "  -  " + etapa), 12.5f, bold, ACCENT);
            page.y = tableHeader(page, widths, cols);

            int puntos = 0;
            int total = 0;
            int n = 0;
            for (ParentTaskGrade task : lista) {
                page = ensureSpace(doc, page, 24f);
                Integer p = task.getPuntos();
                page.y = tableRow(page, widths, new String[]{
                    String.valueOf(++n),
                    nz(task.getTareaTitulo()),
                    task.getFecha() == null ? "-" : task.getFecha().toString(),
                    (p == null ? "-" : p) + " / " + task.getTotal(),
                    estadoLabel(task.getEstado())
                });
                if (p != null) {
                    puntos += p;
                }
                total += task.getTotal();
            }
            int pct = total > 0 ? Math.round(puntos * 100f / total) : 0;
            page.y = text(page, MARGIN, page.y - 12f,
                    "Subtotal del mes: " + puntos + " / " + total + "  (" + pct + "%)", 10.5f, bold, INK);
            page.y -= 10f;
        }

        finish(page);
        return doc;
    }

    public PDDocument buildLibreta(String alumnoNombre, String especialidad,
            List<ParentSummaryItem> items) throws IOException {
        PDDocument doc = new PDDocument();
        Page page = newPage(doc);
        drawHeader(doc, page, "LIBRETA DE CALIFICACIONES", alumnoNombre, especialidad,
                "Consolidado de Primera y Segunda Etapa");

        Map<String, ParentSummaryItem> primera = new LinkedHashMap<>();
        Map<String, ParentSummaryItem> segunda = new LinkedHashMap<>();
        List<String> orden = new ArrayList<>();
        for (ParentSummaryItem item : items) {
            String key = item.getMateriaId() != null ? String.valueOf(item.getMateriaId()) : nz(item.getMateriaNombre());
            if (!orden.contains(key)) {
                orden.add(key);
            }
            if ("segunda".equalsIgnoreCase(item.getEtapa())) {
                segunda.putIfAbsent(key, item);
            } else {
                primera.putIfAbsent(key, item);
            }
        }

        if (orden.isEmpty()) {
            text(page, MARGIN, page.y, "No hay materias con calificaciones publicadas.", 11f, regular, MUTED);
            finish(page);
            return doc;
        }

        String[] cols = {"Materia", "1a Etapa", "2a Etapa", "Final"};
        float[] widths = {214f, 98f, 98f, 102f};
        page.y = tableHeader(page, widths, cols);

        for (String key : orden) {
            ParentSummaryItem p1 = primera.get(key);
            ParentSummaryItem p2 = segunda.get(key);
            ParentSummaryItem any = p1 != null ? p1 : p2;
            if (any == null) {
                continue;
            }
            page = ensureSpace(doc, page, 26f);

            int puntos = (p1 == null ? 0 : p1.getPuntos()) + (p2 == null ? 0 : p2.getPuntos());
            int total = (p1 == null ? 0 : p1.getTotalPosible()) + (p2 == null ? 0 : p2.getTotalPosible());
            int pctFinal = total > 0 ? Math.round(puntos * 100f / total) : 0;
            int notaFinal = notaFor(any.getCategoria(), total, puntos);

            page.y = tableRow(page, widths, new String[]{
                nz(any.getMateriaNombre()),
                cell(p1),
                cell(p2),
                pctFinal + "%  -  " + notaFinal
            });
        }

        page.y = text(page, MARGIN, page.y - 14f,
                "El porcentaje de cada etapa surge de los puntos logrados sobre los puntos posibles de las "
                + "tareas publicadas. La columna Final combina los puntos de ambas etapas.", 8.5f, regular, MUTED);

        finish(page);
        return doc;
    }

    // ---- Helpers de contenido ----------------------------------------------

    private String cell(ParentSummaryItem item) {
        return item == null ? "-" : item.getPorcentaje() + "%  -  " + item.getNota();
    }

    private int notaFor(String categoria, int total, int puntos) {
        if (total <= 0) {
            return 1;
        }
        Planilla planilla = new Planilla();
        planilla.setCategoria(categoria);
        planilla.computeGradeRanges(total);
        return planilla.getNotaForSum(puntos);
    }

    private static String etapaLabel(String etapa) {
        return "segunda".equalsIgnoreCase(etapa) ? "Segunda etapa" : "Primera etapa";
    }

    private static String estadoLabel(String estado) {
        if (estado == null) {
            return "Pendiente";
        }
        return switch (estado) {
            case ParentTaskGrade.CALIFICADA -> "Calificada";
            case ParentTaskGrade.ENTREGADA_PENDIENTE -> "Entregada - sin calificar";
            case ParentTaskGrade.NO_ENTREGADA -> "No entregada";
            default -> "Pendiente";
        };
    }

    private static String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static float sum(float[] widths) {
        float t = 0f;
        for (float w : widths) {
            t += w;
        }
        return t;
    }

    // ---- Layout / dibujo --------------------------------------------------

    private static final class Page {
        PDPage pdPage;
        PDPageContentStream cs;
        float width;
        float y;
    }

    private Page newPage(PDDocument doc) throws IOException {
        Page page = new Page();
        page.pdPage = new PDPage(new PDRectangle(PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight()));
        doc.addPage(page.pdPage);
        page.cs = new PDPageContentStream(doc, page.pdPage);
        page.width = page.pdPage.getMediaBox().getWidth();
        page.y = page.pdPage.getMediaBox().getHeight() - MARGIN;
        return page;
    }

    private void finish(Page page) throws IOException {
        page.cs.close();
    }

    private Page ensureSpace(PDDocument doc, Page page, float needed) throws IOException {
        if (page.y - needed >= BOTTOM_MARGIN) {
            return page;
        }
        page.cs.close();
        return newPage(doc);
    }

    private void drawHeader(PDDocument doc, Page page, String title, String alumno, String especialidad, String periodo)
            throws IOException {
        float top = page.y;
        drawLogo(doc, page.cs, MARGIN, top - 46f, 92f, 46f);
        text(page, MARGIN + 110f, top - 6f, title, 17f, bold, INK);
        text(page, MARGIN + 110f, top - 26f, "Sistema de Carpetas Académicas · Colegio Técnico Nacional de Asunción", 8.5f, regular, MUTED);
        float y = top - 58f;
        y = text(page, MARGIN, y, "Alumno/a: " + nz(alumno), 11.5f, bold, INK);
        if (especialidad != null && !especialidad.isBlank()) {
            y = text(page, MARGIN, y, "Especialidad: " + especialidad, 10f, regular, MUTED);
        }
        y = text(page, MARGIN, y, periodo, 10f, regular, MUTED);
        y -= 6f;
        rule(page, MARGIN, y, page.width - MARGIN);
        page.y = y - 16f;
    }

    private float tableHeader(Page page, float[] widths, String[] cols) throws IOException {
        float rowTop = page.y;
        float rowH = 16f;
        page.cs.setNonStrokingColor(HEADER_FILL);
        page.cs.addRect(MARGIN, rowTop - rowH, sum(widths), rowH);
        page.cs.fill();
        float x = MARGIN;
        for (int i = 0; i < cols.length; i++) {
            page.cs.beginText();
            page.cs.setNonStrokingColor(ACCENT);
            page.cs.setFont(bold, 8.8f);
            page.cs.newLineAtOffset(x + 4f, rowTop - rowH + 5f);
            page.cs.showText(safe(cols[i]));
            page.cs.endText();
            x += widths[i];
        }
        rule(page, MARGIN, rowTop - rowH, MARGIN + sum(widths));
        return rowTop - rowH - 2f;
    }

    private float tableRow(Page page, float[] widths, String[] cells) throws IOException {
        float rowH = 15f;
        List<List<String>> wrapped = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            List<String> lines = wrap(regular, 8.6f, nz(cells[i]), widths[i] - 8f);
            wrapped.add(lines);
            rowH = Math.max(rowH, 5f + lines.size() * 10.5f);
        }
        float rowTop = page.y;
        float x = MARGIN;
        for (int i = 0; i < cells.length; i++) {
            float ly = rowTop - 11f;
            for (String line : wrapped.get(i)) {
                page.cs.beginText();
                page.cs.setNonStrokingColor(INK);
                page.cs.setFont(regular, 8.6f);
                page.cs.newLineAtOffset(x + 4f, ly);
                page.cs.showText(safe(line));
                page.cs.endText();
                ly -= 10.5f;
            }
            x += widths[i];
        }
        rule(page, MARGIN, rowTop - rowH, MARGIN + sum(widths), RULE);
        return rowTop - rowH;
    }

    private float text(Page page, float x, float y, String s, float size, PDFont font, Color color) throws IOException {
        if (s == null || s.isEmpty()) {
            return y;
        }
        float maxWidth = page.width - x - MARGIN;
        float cy = y;
        for (String line : wrap(font, size, s, maxWidth)) {
            page.cs.beginText();
            page.cs.setNonStrokingColor(color);
            page.cs.setFont(font, size);
            page.cs.newLineAtOffset(x, cy - size);
            page.cs.showText(safe(line));
            page.cs.endText();
            cy -= size + LINE_GAP;
        }
        return cy;
    }

    private void rule(Page page, float x1, float y, float x2) throws IOException {
        rule(page, x1, y, x2, RULE);
    }

    private void rule(Page page, float x1, float y, float x2, Color color) throws IOException {
        page.cs.setStrokingColor(color);
        page.cs.setLineWidth(0.6f);
        page.cs.moveTo(x1, y);
        page.cs.lineTo(x2, y);
        page.cs.stroke();
    }

    private void drawLogo(PDDocument doc, PDPageContentStream cs, float x, float y, float maxW, float h) {
        try (InputStream is = ParentReportPdfBuilder.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (is == null) {
                log.warn("Reporte padres PDF: no se encontro el logo en {}", LOGO_RESOURCE);
                return;
            }
            byte[] bytes = is.readAllBytes();
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, bytes, "logo-sca-color");
            float ratio = image.getHeight() == 0 ? 1f : (float) image.getWidth() / image.getHeight();
            float drawW = Math.min(maxW, h * ratio);
            cs.drawImage(image, x, y, drawW, h);
        } catch (Exception ex) {
            log.warn("Reporte padres PDF: no se pudo insertar el logo", ex);
        }
    }

    private List<String> wrap(PDFont font, float size, String text, float maxWidth) throws IOException {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            out.add("");
            return out;
        }
        for (String paragraph : text.split("\\R", -1)) {
            String trimmed = paragraph.trim();
            String[] words = trimmed.isEmpty() ? new String[]{""} : trimmed.split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() == 0 || stringWidth(font, size, candidate) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    private float stringWidth(PDFont font, float size, String s) throws IOException {
        return font.getStringWidth(safe(s)) / 1000f * size;
    }

    /**
     * Helvetica Standard14 usa WinAnsi (CP1252): cubre acentos y ñ, pero no
     * caracteres fuera de ese rango. Se sustituyen los pocos que puedan colarse
     * (comillas tipográficas, guiones largos) para no romper {@code showText}.
     */
    private static String safe(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\t') {
                sb.append(' ');
            } else if ((c >= 0x20 && c <= 0x7E) || (c >= 0xA0 && c <= 0xFF)) {
                sb.append(c);
            } else {
                switch (c) {
                    case '‘', '’', '‚' -> sb.append('\'');
                    case '“', '”', '„' -> sb.append('"');
                    case '–', '—', '−' -> sb.append('-');
                    case '•', '·' -> sb.append('-');
                    case '…' -> sb.append("...");
                    case ' ' -> sb.append(' ');
                    default -> sb.append(' ');
                }
            }
        }
        return sb.toString();
    }
}
