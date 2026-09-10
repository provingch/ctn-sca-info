package ctn.informatica.sca.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ctn.informatica.sca.model.ParentSummaryItem;
import ctn.informatica.sca.model.ParentTaskGrade;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class ParentReportPdfBuilderTest {

    private ParentSummaryItem item(int planillaId, int materiaId, String materia, String etapa,
            int puntos, int total, String categoria) {
        ParentSummaryItem it = new ParentSummaryItem();
        it.setAlumnoId(7);
        it.setPlanillaId(planillaId);
        it.setMateriaId(materiaId);
        it.setMateriaNombre(materia);
        it.setEtapa(etapa);
        it.setCategoria(categoria);
        it.setPuntos(puntos);
        it.setTotalPosible(total);
        it.recomputeDerivedValues();
        return it;
    }

    private ParentTaskGrade task(int planillaId, String titulo, LocalDate fecha, Integer puntos, int total, String estado) {
        ParentTaskGrade t = new ParentTaskGrade();
        t.setPlanillaId(planillaId);
        t.setTareaTitulo(titulo);
        t.setFecha(fecha);
        t.setPuntos(puntos);
        t.setTotal(total);
        t.setEstado(estado);
        return t;
    }

    @Test
    void monthlyReportIsValidPdfAndKeepsTasksOfThatMonth() throws Exception {
        List<ParentSummaryItem> items = List.of(
                item(11, 1, "Matemática", "primera", 30, 40, "comun"),
                item(12, 2, "Física", "primera", 10, 20, "especifico"));
        List<ParentTaskGrade> tasks = List.of(
                task(11, "Práctica 1", LocalDate.of(2026, 5, 8), 18, 20, ParentTaskGrade.CALIFICADA),
                task(11, "Práctica 2", LocalDate.of(2026, 5, 22), 12, 20, ParentTaskGrade.CALIFICADA),
                task(11, "Tarea de abril", LocalDate.of(2026, 4, 3), 15, 20, ParentTaskGrade.CALIFICADA),
                task(12, "Informe", LocalDate.of(2026, 5, 15), null, 20, ParentTaskGrade.NO_ENTREGADA));

        byte[] bytes = save(new ParentReportPdfBuilder()
                .buildMonthlyReport("Ana María Acosta", "Informática", 5, 2026, items, tasks));

        try (PDDocument doc = Loader.loadPDF(bytes)) {
            assertTrue(doc.getNumberOfPages() >= 1);
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("REPORTE MENSUAL"), text);
            assertTrue(text.contains("Ana Mar"), text);
            assertTrue(text.contains("Practica 1") || text.contains("Práctica 1"), text);
            assertTrue(text.contains("Informe"), text);
            assertFalse(text.contains("Tarea de abril"), "no debe incluir tareas de otro mes: " + text);
        }
    }

    @Test
    void dumpSamplesForManualReview() throws Exception {
        String dir = System.getProperty("scaPdfDumpDir");
        org.junit.jupiter.api.Assumptions.assumeTrue(dir != null, "set -DscaPdfDumpDir=... to dump samples");
        List<ParentSummaryItem> items = List.of(
                item(11, 1, "Matemática", "primera", 30, 40, "comun"),
                item(21, 1, "Matemática", "segunda", 35, 40, "comun"),
                item(12, 2, "Física General", "primera", 12, 20, "especifico"),
                item(22, 2, "Física General", "segunda", 16, 20, "especifico"));
        List<ParentTaskGrade> tasks = List.of(
                task(11, "Práctica de ecuaciones", LocalDate.of(2026, 5, 8), 18, 20, ParentTaskGrade.CALIFICADA),
                task(11, "Trabajo práctico grupal", LocalDate.of(2026, 5, 22), 12, 20, ParentTaskGrade.CALIFICADA),
                task(12, "Informe de laboratorio", LocalDate.of(2026, 5, 15), null, 20, ParentTaskGrade.NO_ENTREGADA));
        java.nio.file.Files.write(java.nio.file.Path.of(dir, "reporte-mensual-sample.pdf"),
                save(new ParentReportPdfBuilder().buildMonthlyReport("Ana María Acosta Rodríguez", "Informática", 5, 2026, items, tasks)));
        java.nio.file.Files.write(java.nio.file.Path.of(dir, "libreta-sample.pdf"),
                save(new ParentReportPdfBuilder().buildLibreta("Ana María Acosta Rodríguez", "Informática", items)));
    }

    @Test
    void libretaIsValidPdfWithBothStages() throws Exception {
        List<ParentSummaryItem> items = List.of(
                item(11, 1, "Matemática", "primera", 30, 40, "comun"),
                item(21, 1, "Matemática", "segunda", 35, 40, "comun"),
                item(12, 2, "Física", "primera", 12, 20, "especifico"));

        byte[] bytes = save(new ParentReportPdfBuilder()
                .buildLibreta("Ana María Acosta", "Informática", items));

        try (PDDocument doc = Loader.loadPDF(bytes)) {
            assertEquals(1, doc.getNumberOfPages());
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("LIBRETA DE CALIFICACIONES"), text);
            assertTrue(text.contains("Final"), text);
        }
    }

    private static void assertFalse(boolean cond, String msg) {
        assertTrue(!cond, msg);
    }

    private static byte[] save(PDDocument doc) throws Exception {
        try (doc; ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.save(baos);
            byte[] out = baos.toByteArray();
            assertTrue(out.length > 400 && out[0] == '%' && out[1] == 'P', "no parece un PDF");
            return out;
        }
    }
}
