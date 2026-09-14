package ctn.informatica.sca.util;

import ctn.informatica.sca.dao.QuejaDao.Documento;
import java.io.*;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import static org.junit.jupiter.api.Assertions.*;

class QuejaDocumentBuildersTest {
    private final LocalDateTime time = LocalDateTime.of(2026, 9, 14, 10, 30);
    private Documento doc(String original, boolean resolved) {
        Timestamp date = Timestamp.valueOf(time);
        return new Documento(42, 21, "Informática", "Ana Pérez", "2° A", original, date, resolved ? date : null,
                "Se verificó la situación.", "Se entrevistó al profesor y al delegado.\nSe revisaron las consignas y los registros de clase.",
                "Se publicaron las consignas pendientes y se acordó un seguimiento semanal.", "María López", resolved ? date : null);
    }
    @Test void generaEjemplosYVerificaContenidoProteccionYTipos() throws Exception {
        String original = "No se recibieron las consignas de la actividad de Redes.\nSolicitamos una explicación y una fecha de entrega clara.";
        byte[] pdf = new QuejaPdfBuilder().build(doc(original, true), time);
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertEquals(1, document.getNumberOfPages());
            for (String expected : new String[] {"REPORTE DE SOLUCIÓN", "Qué se revisó", "Solución aplicada", "María López", "Firma y sello", "Coordinación Pedagógica", "Queja #42"}) assertTrue(text.contains(expected), expected);
            assertTrue(text.contains("No se recibieron las consignas"));
        }
        byte[] xlsx = new QuejaWorkbookBuilder().build(doc(original, false), time);
        try (var book = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            var sheet = book.getSheetAt(0);
            assertTrue(sheet.getProtect());
            assertEquals("Coordinación de Informática", sheet.getRow(5).getCell(3).getStringCellValue());
            assertTrue(sheet.getRow(5).getCell(3).getCellStyle().getLocked());
            assertEquals(CellType.NUMERIC, sheet.getRow(8).getCell(3).getCellType());
            int editableRows = 0;
            for (Row row : sheet) {
                Cell cell = row.getCell(0);
                if (cell != null && !cell.getCellStyle().getLocked()) editableRows++;
            }
            assertEquals(3, editableRows);
            assertEquals(1, book.getAllPictures().size());
        }
        if (Boolean.getBoolean("queja.qa")) {
            Path out = Path.of("target/queja-document-qa"); Files.createDirectories(out);
            Files.write(out.resolve("reporte-solucion-42.pdf"), pdf);
            Files.write(out.resolve("solicitud-revision-42.xlsx"), xlsx);
        }
    }
    @Test void paginaTextosLargosYConservaElFinalYLaFirma() throws Exception {
        String original = "Descripción extensa con tildes y detalles de la queja. ".repeat(180) + " FIN ORIGINAL";
        byte[] pdf = new QuejaPdfBuilder().build(doc(original, true), time);
        try (var document = Loader.loadPDF(pdf)) {
            assertTrue(document.getNumberOfPages() > 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("FIN ORIGINAL")); assertTrue(text.contains("Firma y sello"));
        }
        if (Boolean.getBoolean("queja.qa")) {
            Path out = Path.of("target/queja-document-qa"); Files.createDirectories(out);
            Files.write(out.resolve("reporte-largo.pdf"), pdf);
        }
    }
    @Test void excelNoInterpretaFormulasYNuncaTruncaLaQueja() throws Exception {
        String original = "=HYPERLINK(\"https://example.test\")\n" + "Detalle\n".repeat(6000);
        byte[] bytes = new QuejaWorkbookBuilder().build(doc(original, false), time);
        try (var book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            StringBuilder reconstructed = new StringBuilder(); boolean copying = false;
            for (Row row : book.getSheetAt(0)) {
                Cell cell = row.getCell(0); if (cell == null) continue;
                assertNotEquals(CellType.FORMULA, cell.getCellType());
                String text = cell.getStringCellValue();
                if (text.equals("ACEPTACIÓN DE LA REVISIÓN")) break;
                if (copying) { assertTrue(cell.getCellStyle().getLocked()); reconstructed.append(text); }
                if (text.equals("QUEJA ORIGINAL")) copying = true;
            }
            assertEquals(original, reconstructed.toString());
        }
    }
}
