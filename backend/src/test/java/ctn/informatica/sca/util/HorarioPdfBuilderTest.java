package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ctn.informatica.sca.model.CursoBase;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
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
}
