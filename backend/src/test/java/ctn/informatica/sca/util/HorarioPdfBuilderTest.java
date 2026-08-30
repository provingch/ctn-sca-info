package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ctn.informatica.sca.model.CursoBase;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class HorarioPdfBuilderTest {

    @Test
    void resolvesSpecialtyAccentForRecesoInPdf() {
        assertEquals("#7a1f2b", HorarioPdfBuilder.resolveRecesoFillHex("Informática"));
        assertNotEquals("BFBFBF", HorarioPdfBuilder.resolveRecesoFillHex("Informática"));
    }

    @Test
    void rendersMergedCellProfessorWithSmallerFontThanMateria() throws Exception {
        Path targetDir = Path.of("target");
        Files.createDirectories(targetDir);
        Path pdf = targetDir.resolve("horario-merged-fonts.pdf");

        CursoBase curso = new CursoBase(301, 1, "Informática", 2, "A");
        List<HoraCatedra> horas = List.of(
            new HoraCatedra(1, 1, "M", LocalTime.of(7, 0), LocalTime.of(7, 40)),
            new HoraCatedra(2, 2, "M", LocalTime.of(7, 40), LocalTime.of(8, 20))
        );

        HorarioSlot slot1 = new HorarioSlot(1, 31, 51, curso.getId(), 1, 1, 401);
        slot1.setMateriaNombre("Programación");
        slot1.setProfesorNombre("Delgado Cristian");
        slot1.setSalaNombre("Lab 1");

        HorarioSlot slot2 = new HorarioSlot(2, 31, 51, curso.getId(), 1, 2, 401);
        slot2.setMateriaNombre("Programación");
        slot2.setProfesorNombre("Delgado Cristian");
        slot2.setSalaNombre("Lab 1");

        try (PDDocument document = new HorarioPdfBuilder().build(curso, horas, List.of(slot1, slot2));
             OutputStream out = Files.newOutputStream(pdf)) {
            document.save(out);
            assertEquals(1, document.getNumberOfPages(), "El caso de prueba debe entrar en una sola página");
        }

        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            CapturingStripper stripper = new CapturingStripper();
            String text = stripper.getText(document);
            assertTrue(text.contains("Programación"), "La materia debe aparecer en el PDF");
            assertTrue(text.contains("Delgado Cristian"), "El profesor debe aparecer completo en el PDF");
            assertTrue(stripper.fontSizeFor("Programación") > stripper.fontSizeFor("Delgado Cristian"),
                    "El profesor debe dibujarse con una fuente más chica que la materia en la celda fusionada");
            assertEquals(11f, stripper.fontSizeFor("Programación"), 0.2f);
            assertEquals(9f, stripper.fontSizeFor("Delgado Cristian"), 0.2f);
        }
    }

    @Test
    void wrapTextSplitsWordsThatDoNotFit() throws Exception {
        HorarioPdfBuilder builder = new HorarioPdfBuilder();
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        Method method = HorarioPdfBuilder.class.getDeclaredMethod("wrapText", PDFont.class, float.class, String.class, float.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) method.invoke(builder, font, 11f, "Supercalifragilisticexpialidocious", 26f);

        assertTrue(lines.size() > 1, "Una palabra demasiado larga debe partirse en varios fragmentos");
        for (String line : lines) {
            assertTrue(measureText(font, 11f, line) <= 26f + 0.01f, "Cada fragmento debe entrar en el ancho disponible");
        }
    }

    @Test
    void rendersMergedCellWithLongTextsWithoutThrowingOrTruncatingExtraction() throws Exception {
        Path targetDir = Path.of("target");
        Files.createDirectories(targetDir);
        Path pdf = targetDir.resolve("horario-merged-long-texts.pdf");

        CursoBase curso = new CursoBase(302, 1, "Informática", 2, "B");
        List<HoraCatedra> horas = List.of(
            new HoraCatedra(1, 1, "M", LocalTime.of(7, 0), LocalTime.of(7, 40)),
            new HoraCatedra(2, 2, "M", LocalTime.of(7, 40), LocalTime.of(8, 20))
        );

        HorarioSlot slot1 = new HorarioSlot(11, 41, 61, curso.getId(), 1, 1, 501);
        slot1.setMateriaNombre("Formación Ética y Ciudadana");
        slot1.setProfesorNombre("Profesora ExtremadamenteLarga");
        slot1.setSalaNombre("Laboratorio 12");

        HorarioSlot slot2 = new HorarioSlot(12, 41, 61, curso.getId(), 1, 2, 501);
        slot2.setMateriaNombre("Formación Ética y Ciudadana");
        slot2.setProfesorNombre("Profesora ExtremadamenteLarga");
        slot2.setSalaNombre("Laboratorio 12");

        try (PDDocument document = new HorarioPdfBuilder().build(curso, horas, List.of(slot1, slot2));
             OutputStream out = Files.newOutputStream(pdf)) {
            document.save(out);
            assertEquals(1, document.getNumberOfPages(), "El caso con textos largos debe seguir entrando en una página");
        }

        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(document);
            String normalized = text.replaceAll("\\s+", " ");
            assertTrue(normalized.contains("Formación Ética y Ciudadana"), "La materia debe extraerse completa");
            assertTrue(normalized.replaceAll("\\s+", "").contains("ProfesoraExtremadamenteLarga"), "El profesor debe extraerse completo (ignora saltos de línea)");
            assertTrue(normalized.contains("Laboratorio 12"), "La sala vecina debe seguir apareciendo intacta");
        }
    }

    private static final class CapturingStripper extends PDFTextStripper {
        private final List<TextSample> samples = new ArrayList<>();

        CapturingStripper() throws java.io.IOException {
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws java.io.IOException {
            if (string == null || string.isBlank() || textPositions == null || textPositions.isEmpty()) {
                return;
            }
            samples.add(new TextSample(string, textPositions.get(0).getFontSizeInPt()));
            super.writeString(string, textPositions);
        }

        float fontSizeFor(String needle) {
            for (TextSample sample : samples) {
                if (sample.text.contains(needle)) {
                    return sample.fontSize;
                }
            }
            throw new IllegalStateException("No se encontró el texto: " + needle);
        }
    }

    private record TextSample(String text, float fontSize) {
    }

    private static float measureText(PDFont font, float fontSize, String text) throws Exception {
        return font.getStringWidth(text) / 1000f * fontSize;
    }
}
