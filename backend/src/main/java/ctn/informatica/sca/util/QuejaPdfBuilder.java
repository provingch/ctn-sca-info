package ctn.informatica.sca.util;

import ctn.informatica.sca.dao.QuejaDao.Documento;
import java.awt.Color;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/** Reporte institucional con párrafos paginados; no recorta la descripción original. */
public class QuejaPdfBuilder {
    private static final float MARGIN = 44, BOTTOM = 62, LEADING = 15;
    private static final Color INK = new Color(0x14233B), MUTED = new Color(0x64748B), ACCENT = new Color(0x3144CF);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private PDDocument doc;
    private PDPageContentStream stream;
    private PDFont regular, bold;
    private float y;
    private int pageNumber;
    private long id;

    public byte[] build(Documento q, LocalDateTime generatedAt) throws IOException {
        if (q.resueltaEn() == null || q.revisadaEn() == null) throw new IllegalArgumentException("La queja no está resuelta");
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            doc = document; id = q.id(); pageNumber = 0;
            try (InputStream font = resource("/fonts/LiberationSans-Regular.ttf"); InputStream heavy = resource("/fonts/LiberationSans-Bold.ttf")) {
                regular = PDType0Font.load(doc, font); bold = PDType0Font.load(doc, heavy);
            }
            try {
                page();
                paragraph("REPORTE DE SOLUCIÓN", bold, 20, ACCENT);
                paragraph("Queja #" + q.id() + "   /   Resuelta", bold, 11, INK);
                y -= 10;
                paragraph("Especialidad: " + value(q.especialidad()), regular, 10, INK);
                paragraph("Curso: " + value(q.curso()) + "   /   Profesor: " + value(q.profesor()), regular, 10, INK);
                paragraph("Registrada: " + date(q.creadaEn()) + "   /   Resuelta: " + date(q.resueltaEn()), regular, 10, MUTED);
                paragraph("Generado: " + generatedAt.format(DATE), regular, 10, MUTED);
                section("01   Queja original", q.motivo());
                section("02   Qué se revisó", q.procesoRevision());
                section("03   Solución aplicada", q.solucionAplicada());
                section("04   Persona que corrigió", q.corregidaPorNombre());
                ensure(118);
                y -= 28;
                stream.setStrokingColor(MUTED); stream.setLineWidth(.7f);
                stream.moveTo(MARGIN, y); stream.lineTo(MARGIN + 255, y); stream.stroke();
                y -= 20;
                paragraph("Coordinación Pedagógica", bold, 11, INK);
                paragraph("Firma y sello", regular, 10, MUTED);
            } finally { if (stream != null) { stream.close(); stream = null; } }
            doc.save(bytes); return bytes.toByteArray();
        }
    }

    private static InputStream resource(String path) throws IOException {
        InputStream input = QuejaPdfBuilder.class.getResourceAsStream(path);
        if (input == null) throw new IOException("Recurso institucional no disponible: " + path);
        return input;
    }
    private void page() throws IOException {
        if (stream != null) stream.close();
        PDPage page = new PDPage(PDRectangle.A4); doc.addPage(page);
        stream = new PDPageContentStream(doc, page); pageNumber++;
        try (InputStream logo = resource("/static/logo-institucional.png")) {
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, logo.readAllBytes(), "Institución");
            float scale = Math.min(90f / image.getWidth(), 43f / image.getHeight());
            float width = image.getWidth() * scale, height = image.getHeight() * scale;
            stream.drawImage(image, MARGIN, 752 + (43 - height) / 2, width, height);
        }
        text(MARGIN + 110, 779, "Colegio Técnico Nacional de Asunción", bold, 12, INK);
        text(MARGIN + 110, 761, "Sistema de Carpetas Académicas", regular, 10, MUTED);
        stream.setStrokingColor(ACCENT); stream.setLineWidth(1.5f);
        stream.moveTo(MARGIN, 737); stream.lineTo(551, 737); stream.stroke();
        text(MARGIN, 32, "Queja #" + id + " - Reporte de solución", regular, 8, MUTED);
        text(490, 32, "Página " + pageNumber, regular, 8, MUTED);
        y = 708;
    }
    private void ensure(float height) throws IOException { if (y - height < BOTTOM) page(); }
    private void section(String title, String body) throws IOException {
        ensure(65); y -= 18; paragraph(title, bold, 11, ACCENT); y -= 3;
        paragraph(value(body), regular, 10.5f, INK);
    }
    private void paragraph(String text, PDFont font, float size, Color color) throws IOException {
        for (String line : wrap(text.replace("\t", "    "), font, size, 507)) {
            ensure(LEADING); text(MARGIN, y, line, font, size, color); y -= Math.max(LEADING, size + 5);
        }
    }
    // Divide también palabras largas y respeta saltos de línea sin truncar texto.
    static List<String> wrap(String value, PDFont font, float size, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : value.split("\\R", -1)) {
            StringBuilder line = new StringBuilder();
            for (int offset = 0; offset < paragraph.length();) {
                int cp = paragraph.codePointAt(offset); String ch = new String(Character.toChars(cp)); offset += ch.length();
                if (font.getStringWidth(line + ch) * size / 1000 > width && !line.isEmpty()) {
                    int space = line.lastIndexOf(" ");
                    if (space > 0) { lines.add(line.substring(0, space)); line.delete(0, space + 1); }
                    else { lines.add(line.toString()); line.setLength(0); }
                }
                line.append(ch);
            }
            lines.add(line.toString());
        }
        return lines;
    }
    private void text(float x, float baseline, String text, PDFont font, float size, Color color) throws IOException {
        stream.beginText(); stream.setFont(font, size); stream.setNonStrokingColor(color);
        stream.newLineAtOffset(x, baseline); stream.showText(text); stream.endText();
    }
    static String date(java.sql.Timestamp date) { return date == null ? "No disponible" : date.toLocalDateTime().format(DATE); }
    static String value(String value) { return value == null || value.isBlank() ? "No disponible" : value; }
}
