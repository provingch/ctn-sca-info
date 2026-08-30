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
    void generatesRealisticTwoBlockPdfOnOnePage() throws Exception {
        Path targetDir = Path.of("target");
        Files.createDirectories(targetDir);
        Path pdf = targetDir.resolve("demo-horario-compacto.pdf");

        CursoBase curso = new CursoBase(102, 1, "Informática", 3, "A");
        List<HoraCatedra> horas = List.of(
            new HoraCatedra(1, 1, "M", LocalTime.of(7, 0), LocalTime.of(7, 40)),
            new HoraCatedra(2, 2, "M", LocalTime.of(7, 40), LocalTime.of(8, 20)),
            new HoraCatedra(3, 3, "M", LocalTime.of(8, 20), LocalTime.of(9, 0)),
            new HoraCatedra(4, 4, "M", LocalTime.of(9, 0), LocalTime.of(9, 40)),
            new HoraCatedra(5, 5, "M", LocalTime.of(10, 0), LocalTime.of(10, 40)),
            new HoraCatedra(6, 6, "M", LocalTime.of(10, 40), LocalTime.of(11, 20)),
            new HoraCatedra(7, 7, "M", LocalTime.of(11, 20), LocalTime.of(12, 0)),
            new HoraCatedra(8, 8, "M", LocalTime.of(12, 0), LocalTime.of(12, 40)),
            new HoraCatedra(9, 1, "T", LocalTime.of(13, 0), LocalTime.of(13, 40)),
            new HoraCatedra(10, 2, "T", LocalTime.of(13, 40), LocalTime.of(14, 20)),
            new HoraCatedra(11, 3, "T", LocalTime.of(14, 20), LocalTime.of(15, 0)),
            new HoraCatedra(12, 4, "T", LocalTime.of(15, 0), LocalTime.of(15, 40)),
            new HoraCatedra(13, 5, "T", LocalTime.of(16, 0), LocalTime.of(16, 40)),
            new HoraCatedra(14, 6, "T", LocalTime.of(16, 40), LocalTime.of(17, 20)),
            new HoraCatedra(15, 7, "T", LocalTime.of(17, 20), LocalTime.of(18, 0)),
            new HoraCatedra(16, 8, "T", LocalTime.of(18, 0), LocalTime.of(18, 40))
        );

        List<HorarioSlot> slots = List.of(
            slot(101, 501, 601, curso.getId(), 1, 1, "Programación", "Ada Lovelace", "Lab 1"),
            slot(102, 502, 602, curso.getId(), 1, 2, "Programación", "Ada Lovelace", "Lab 1"),
            slot(103, 503, 603, curso.getId(), 1, 3, "Programación", "Ada Lovelace", "Lab 1"),
            slot(104, 504, 604, curso.getId(), 1, 4, "Programación", "Ada Lovelace", "Lab 1"),
            slot(105, 505, 605, curso.getId(), 1, 5, "Bases", "Grace Hopper", "Lab 2"),
            slot(106, 506, 606, curso.getId(), 1, 6, "Bases", "Grace Hopper", "Lab 2"),
            slot(107, 507, 607, curso.getId(), 1, 7, "Bases", "Grace Hopper", "Lab 2"),
            slot(108, 508, 608, curso.getId(), 1, 8, "Bases", "Grace Hopper", "Lab 2"),
            slot(109, 509, 609, curso.getId(), 1, 9, "Redes", "Alan Turing", "Lab 3"),
            slot(110, 510, 610, curso.getId(), 1, 10, "Redes", "Alan Turing", "Lab 3"),
            slot(111, 511, 611, curso.getId(), 1, 11, "Redes", "Alan Turing", "Lab 3"),
            slot(112, 512, 612, curso.getId(), 1, 12, "Redes", "Alan Turing", "Lab 3"),
            slot(113, 513, 613, curso.getId(), 1, 13, "Redes", "Alan Turing", "Lab 3"),
            slot(114, 514, 614, curso.getId(), 1, 14, "Redes", "Alan Turing", "Lab 3"),
            slot(115, 515, 615, curso.getId(), 1, 15, "Redes", "Alan Turing", "Lab 3"),
            slot(116, 516, 616, curso.getId(), 1, 16, "Redes", "Alan Turing", "Lab 3")
        );

        try (PDDocument document = new HorarioPdfBuilder().build(curso, horas, slots);
             OutputStream out = Files.newOutputStream(pdf)) {
            document.save(out);
            assertEquals(1, document.getNumberOfPages(), "El horario compacto debe quedar en una sola página");
        }

        assertTrue(Files.size(pdf) > 0, "El PDF compacto debe generarse");
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("MAÑANA"), "El PDF debe incluir el bloque de mañana");
            assertTrue(text.contains("T.O."), "El PDF debe incluir el bloque de tarde");
        }
    }

    @Test
    void fallsBackToMultiplePagesWhenMinimumScaleIsNotEnough() throws Exception {
        Path targetDir = Path.of("target");
        Files.createDirectories(targetDir);
        Path pdf = targetDir.resolve("demo-horario-extremo.pdf");

        CursoBase curso = new CursoBase(103, 1, "Informática", 3, "B");
        List<HoraCatedra> horas = List.of(
            new HoraCatedra(1, 1, "M", LocalTime.of(7, 0), LocalTime.of(7, 40)),
            new HoraCatedra(2, 2, "M", LocalTime.of(7, 40), LocalTime.of(8, 20)),
            new HoraCatedra(3, 3, "M", LocalTime.of(8, 20), LocalTime.of(9, 0)),
            new HoraCatedra(4, 4, "M", LocalTime.of(9, 0), LocalTime.of(9, 40)),
            new HoraCatedra(5, 5, "M", LocalTime.of(9, 40), LocalTime.of(10, 20)),
            new HoraCatedra(6, 6, "M", LocalTime.of(10, 20), LocalTime.of(11, 0)),
            new HoraCatedra(7, 7, "M", LocalTime.of(11, 0), LocalTime.of(11, 40)),
            new HoraCatedra(8, 8, "M", LocalTime.of(11, 40), LocalTime.of(12, 20)),
            new HoraCatedra(9, 9, "M", LocalTime.of(12, 20), LocalTime.of(13, 0)),
            new HoraCatedra(10, 10, "M", LocalTime.of(13, 0), LocalTime.of(13, 40)),
            new HoraCatedra(11, 11, "M", LocalTime.of(13, 40), LocalTime.of(14, 20)),
            new HoraCatedra(12, 12, "M", LocalTime.of(14, 20), LocalTime.of(15, 0)),
            new HoraCatedra(13, 1, "T", LocalTime.of(15, 0), LocalTime.of(15, 40)),
            new HoraCatedra(14, 2, "T", LocalTime.of(15, 40), LocalTime.of(16, 20)),
            new HoraCatedra(15, 3, "T", LocalTime.of(16, 20), LocalTime.of(17, 0)),
            new HoraCatedra(16, 4, "T", LocalTime.of(17, 0), LocalTime.of(17, 40)),
            new HoraCatedra(17, 5, "T", LocalTime.of(17, 40), LocalTime.of(18, 20)),
            new HoraCatedra(18, 6, "T", LocalTime.of(18, 20), LocalTime.of(19, 0)),
            new HoraCatedra(19, 7, "T", LocalTime.of(19, 0), LocalTime.of(19, 40)),
            new HoraCatedra(20, 8, "T", LocalTime.of(19, 40), LocalTime.of(20, 20)),
            new HoraCatedra(21, 9, "T", LocalTime.of(20, 20), LocalTime.of(21, 0)),
            new HoraCatedra(22, 10, "T", LocalTime.of(21, 0), LocalTime.of(21, 40)),
            new HoraCatedra(23, 11, "T", LocalTime.of(21, 40), LocalTime.of(22, 20)),
            new HoraCatedra(24, 12, "T", LocalTime.of(22, 20), LocalTime.of(23, 0))
        );

        List<HorarioSlot> slots = List.of(
            slot(201, 701, 801, curso.getId(), 1, 1, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(202, 702, 802, curso.getId(), 1, 2, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(203, 703, 803, curso.getId(), 1, 3, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(204, 704, 804, curso.getId(), 1, 4, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(205, 705, 805, curso.getId(), 1, 5, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(206, 706, 806, curso.getId(), 1, 6, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(207, 707, 807, curso.getId(), 1, 7, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(208, 708, 808, curso.getId(), 1, 8, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(209, 709, 809, curso.getId(), 1, 9, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(210, 710, 810, curso.getId(), 1, 10, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(211, 711, 811, curso.getId(), 1, 11, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(212, 712, 812, curso.getId(), 1, 12, "Algoritmos", "Ada Lovelace", "Lab 1"),
            slot(213, 713, 813, curso.getId(), 1, 13, "Redes", "Alan Turing", "Lab 2"),
            slot(214, 714, 814, curso.getId(), 1, 14, "Redes", "Alan Turing", "Lab 2"),
            slot(215, 715, 815, curso.getId(), 1, 15, "Redes", "Alan Turing", "Lab 2"),
            slot(216, 716, 816, curso.getId(), 1, 16, "Redes", "Alan Turing", "Lab 2"),
            slot(217, 717, 817, curso.getId(), 1, 17, "Redes", "Alan Turing", "Lab 2"),
            slot(218, 718, 818, curso.getId(), 1, 18, "Redes", "Alan Turing", "Lab 2"),
            slot(219, 719, 819, curso.getId(), 1, 19, "Redes", "Alan Turing", "Lab 2"),
            slot(220, 720, 820, curso.getId(), 1, 20, "Redes", "Alan Turing", "Lab 2"),
            slot(221, 721, 821, curso.getId(), 1, 21, "Redes", "Alan Turing", "Lab 2"),
            slot(222, 722, 822, curso.getId(), 1, 22, "Redes", "Alan Turing", "Lab 2"),
            slot(223, 723, 823, curso.getId(), 1, 23, "Redes", "Alan Turing", "Lab 2"),
            slot(224, 724, 824, curso.getId(), 1, 24, "Redes", "Alan Turing", "Lab 2")
        );

        try (PDDocument document = new HorarioPdfBuilder().build(curso, horas, slots);
             OutputStream out = Files.newOutputStream(pdf)) {
            document.save(out);
            assertTrue(document.getNumberOfPages() > 1, "El caso extremo debe partirse en varias páginas");
        }

        assertTrue(Files.size(pdf) > 0, "El PDF extremo debe generarse");
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("MAÑANA"), "El PDF debe incluir el bloque de mañana");
            assertTrue(text.contains("T.O."), "El PDF debe incluir el bloque de tarde");
        }
    }

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

    private static HorarioSlot slot(int id, int asignacionId, int materiaId, int cursoId, int diaSemana, int horaCatedraId, String materia, String profesor, String sala) {
        HorarioSlot slot = new HorarioSlot(id, asignacionId, materiaId, cursoId, diaSemana, horaCatedraId, 900 + id);
        slot.setMateriaNombre(materia);
        slot.setProfesorNombre(profesor);
        slot.setSalaNombre(sala);
        return slot;
    }
}
