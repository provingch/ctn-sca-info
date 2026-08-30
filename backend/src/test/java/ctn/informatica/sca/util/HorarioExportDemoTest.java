package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ctn.informatica.sca.model.CursoBase;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class HorarioExportDemoTest {

    @Test
    void generatesDemoPdfInTarget() throws Exception {
        Path targetDir = Path.of("target");
        Files.createDirectories(targetDir);
        Path pdf = targetDir.resolve("demo-horario.pdf");

        CursoBase curso = new CursoBase(101, 1, "Informática", 1, "A");
        List<HoraCatedra> horas = List.of(
            new HoraCatedra(1, 1, "M", LocalTime.of(7, 0), LocalTime.of(7, 40)),
            new HoraCatedra(2, 2, "M", LocalTime.of(7, 40), LocalTime.of(8, 20)),
            new HoraCatedra(3, 4, "M", LocalTime.of(8, 30), LocalTime.of(9, 10)),
            new HoraCatedra(4, 1, "T", LocalTime.of(13, 0), LocalTime.of(13, 40))
        );

        HorarioSlot slot1 = new HorarioSlot(1, 11, 21, 101, 1, 1, 301);
        slot1.setMateriaNombre("Programación");
        slot1.setProfesorNombre("Ada Lovelace");
        slot1.setSalaNombre("Lab 1");

        HorarioSlot slot2 = new HorarioSlot(2, 11, 21, 101, 1, 2, 301);
        slot2.setMateriaNombre("Programación");
        slot2.setProfesorNombre("Ada Lovelace");
        slot2.setSalaNombre("Lab 1");

        HorarioSlot slot3 = new HorarioSlot(3, 11, 22, 101, 2, 3, 302);
        slot3.setMateriaNombre("Base de Datos");
        slot3.setProfesorNombre("Grace Hopper");
        slot3.setSalaNombre("Lab 2");

        HorarioSlot slot4 = new HorarioSlot(4, 11, 23, 101, 1, 4, 303);
        slot4.setMateriaNombre("Redes");
        slot4.setProfesorNombre("Alan Turing");
        slot4.setSalaNombre("Lab 3");

        List<HorarioSlot> slots = List.of(slot1, slot2, slot3, slot4);

        try (PDDocument document = new HorarioPdfBuilder().build(curso, horas, slots);
             OutputStream out = Files.newOutputStream(pdf)) {
            document.save(out);
            assertEquals(1, document.getNumberOfPages(), "El demo debe quedar en una sola página");
        }

        assertTrue(Files.size(pdf) > 0, "El PDF demo debe generarse");
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("MAÑANA"), "El PDF debe incluir el bloque de mañana");
            assertTrue(text.contains("T.O."), "El PDF debe incluir el bloque de tarde");
        }
    }
}
