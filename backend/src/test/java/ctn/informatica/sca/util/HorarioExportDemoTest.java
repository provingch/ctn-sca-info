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

    @Test
    void generatesCombinedSpecialtyPdfWithOnePagePerCourse() throws Exception {
        Path targetDir = Path.of("target");
        Files.createDirectories(targetDir);
        Path pdf = targetDir.resolve("demo-horario-completo.pdf");

        CursoBase cursoA = new CursoBase(201, 1, "Informática", 1, "A");
        CursoBase cursoB = new CursoBase(202, 1, "Informática", 1, "B");
        List<HoraCatedra> horas = List.of(
            new HoraCatedra(1, 1, "M", LocalTime.of(7, 0), LocalTime.of(7, 40)),
            new HoraCatedra(2, 2, "M", LocalTime.of(7, 40), LocalTime.of(8, 20)),
            new HoraCatedra(3, 1, "T", LocalTime.of(13, 0), LocalTime.of(13, 40)),
            new HoraCatedra(4, 2, "T", LocalTime.of(13, 40), LocalTime.of(14, 20))
        );

        HorarioSlot a1 = new HorarioSlot(11, 11, 31, 201, 1, 1, 301);
        a1.setMateriaNombre("Programación");
        a1.setProfesorNombre("Ada Lovelace");
        a1.setSalaNombre("Lab 1");

        HorarioSlot a2 = new HorarioSlot(12, 11, 31, 201, 1, 2, 301);
        a2.setMateriaNombre("Programación");
        a2.setProfesorNombre("Ada Lovelace");
        a2.setSalaNombre("Lab 1");

        HorarioSlot a3 = new HorarioSlot(13, 11, 32, 201, 1, 3, 302);
        a3.setMateriaNombre("Redes");
        a3.setProfesorNombre("Alan Turing");
        a3.setSalaNombre("Lab 2");

        HorarioSlot a4 = new HorarioSlot(14, 11, 32, 201, 1, 4, 302);
        a4.setMateriaNombre("Redes");
        a4.setProfesorNombre("Alan Turing");
        a4.setSalaNombre("Lab 2");

        HorarioSlot b1 = new HorarioSlot(21, 11, 41, 202, 1, 1, 303);
        b1.setMateriaNombre("Matemática");
        b1.setProfesorNombre("Emmy Noether");
        b1.setSalaNombre("Lab 3");

        HorarioSlot b2 = new HorarioSlot(22, 11, 41, 202, 1, 2, 303);
        b2.setMateriaNombre("Matemática");
        b2.setProfesorNombre("Emmy Noether");
        b2.setSalaNombre("Lab 3");

        HorarioSlot b3 = new HorarioSlot(23, 11, 42, 202, 1, 3, 304);
        b3.setMateriaNombre("Base de Datos");
        b3.setProfesorNombre("Grace Hopper");
        b3.setSalaNombre("Lab 4");

        HorarioSlot b4 = new HorarioSlot(24, 11, 42, 202, 1, 4, 304);
        b4.setMateriaNombre("Base de Datos");
        b4.setProfesorNombre("Grace Hopper");
        b4.setSalaNombre("Lab 4");

        List<CursoBase> cursos = List.of(cursoA, cursoB);
        List<HorarioSlot> slots = List.of(a1, a2, a3, a4, b1, b2, b3, b4);

        try (PDDocument document = new HorarioPdfBuilder().buildEspecialidad("Informática", cursos, horas, slots);
             OutputStream out = Files.newOutputStream(pdf)) {
            document.save(out);
            assertEquals(2, document.getNumberOfPages(), "El PDF combinado debe quedar en una página por curso");
        }

        assertTrue(Files.size(pdf) > 0, "El PDF combinado debe generarse");
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertEquals(2, countOccurrences(text, "MAÑANA"), "Cada curso debe incluir un bloque de mañana");
            assertEquals(2, countOccurrences(text, "T.O."), "Cada curso debe incluir un bloque de tarde");
            assertTrue(text.contains("Sección: A"), "Debe incluir el primer curso");
            assertTrue(text.contains("Sección: B"), "Debe incluir el segundo curso");
        }
    }

    private static int countOccurrences(String text, String needle) {
        if (text == null || text.isEmpty() || needle == null || needle.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
