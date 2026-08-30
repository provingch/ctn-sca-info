package ctn.informatica.sca.util;

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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class HorarioExportDemoTest {

    @Test
    void generatesDemoExcelAndPdfInTarget() throws Exception {
        Path targetDir = Path.of("target");
        Files.createDirectories(targetDir);
        Path xlsx = targetDir.resolve("demo-horario.xlsx");
        Path pdf = targetDir.resolve("demo-horario.pdf");

        CursoBase curso = new CursoBase(101, 1, "Informática", 1, "A");
        List<HoraCatedra> horas = List.of(
            new HoraCatedra(1, 1, "M", LocalTime.of(7, 0), LocalTime.of(7, 40)),
            new HoraCatedra(2, 2, "M", LocalTime.of(7, 40), LocalTime.of(8, 20)),
            new HoraCatedra(3, 4, "M", LocalTime.of(8, 30), LocalTime.of(9, 10))
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

        List<HorarioSlot> slots = List.of(slot1, slot2, slot3);

        try (XSSFWorkbook workbook = new HorarioWorkbookBuilder().build(curso, horas, slots);
             OutputStream out = Files.newOutputStream(xlsx)) {
            workbook.write(out);
        }

        try (PDDocument document = new HorarioPdfBuilder().build(curso, horas, slots);
             OutputStream out = Files.newOutputStream(pdf)) {
            document.save(out);
        }

        assertTrue(Files.size(xlsx) > 0, "El XLSX demo debe generarse");
        assertTrue(Files.size(pdf) > 0, "El PDF demo debe generarse");
    }
}
