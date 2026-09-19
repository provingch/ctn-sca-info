package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Test;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.HorarioSlotDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.PlanCurricularDto;
import ctn.informatica.sca.dto.PlanTemplateConfigDto;
import ctn.informatica.sca.model.Asignacion;

public class PlanCurricularTemplateBuilderTest {

    @Test
    public void etapaDos_debeIncluirHojasDeJulioANoviembre() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/plan-curricular-plantilla-etapa2.xlsx")) {
            assertNotNull(in, "Falta la plantilla de etapa 2");
            try (Workbook wb = WorkbookFactory.create(in)) {
                assertNotNull(wb.getSheet("Julio"));
                assertNotNull(wb.getSheet("Agosto"));
                assertNotNull(wb.getSheet("Septiembre"));
                assertNotNull(wb.getSheet("Octubre"));
                assertNotNull(wb.getSheet("Noviembre"));
                assertNull(wb.getSheet("Marzo"));
            }
        }
    }

    @Test
    public void etapaUno_debeIncluirHojaDeJulio() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/plan-curricular-plantilla.xlsx")) {
            assertNotNull(in, "Falta la plantilla de etapa 1");
            try (Workbook wb = WorkbookFactory.create(in)) {
                assertNotNull(wb.getSheet("Marzo"));
                assertNotNull(wb.getSheet("Abril"));
                assertNotNull(wb.getSheet("Mayo"));
                assertNotNull(wb.getSheet("Junio"));
                assertNotNull(wb.getSheet("Julio"));
            }
        }
    }

    @Test
    public void generado_debeTraerLogosYUnaSolaCopiaDeMedia() throws Exception {
        PlanTemplateConfigDto config = configCon("1", mes("Marzo", 4), mes("Abril", 4));
        byte[] xlsx = generar(config);

        int mediaCount = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().startsWith("xl/media/")) mediaCount++;
            }
        }
        assertEquals(2, mediaCount, "La plantilla comparte 2 partes de media entre todas las hojas de mes");
    }

    @Test
    public void generado_debeRespetarBloquesPorMes() throws Exception {
        PlanTemplateConfigDto config = configCon("1", mes("Marzo", 6), mes("Abril", 2));
        byte[] xlsx = generar(config);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            escribirCapacidadesEnCadaBloque(wb.getSheet("Marzo"), 6);
            escribirCapacidadesEnCadaBloque(wb.getSheet("Abril"), 2);
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                wb.write(bos);
                PlanCurricularParser parser = new PlanCurricularParser();
                setParserTemplateBuilder(parser, new PlanCurricularTemplateBuilder());
                PlanCurricularDto dto = parser.parse(bos.toByteArray());
                long marzo = dto.temas.stream().filter(t -> "Marzo".equals(t.mes)).count();
                long abril = dto.temas.stream().filter(t -> "Abril".equals(t.mes)).count();
                assertEquals(6, marzo, "Marzo debe conservar los 6 bloques configurados");
                assertEquals(2, abril, "Abril debe conservar los 2 bloques configurados");
            }
        }
    }

    @Test
    public void generado_noDebeTraerContenidoDeEjemplo() throws Exception {
        PlanTemplateConfigDto config = configCon("1", mes("Marzo", 4), mes("Abril", 4), mes("Mayo", 4));
        byte[] xlsx = generar(config);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            for (String mesNombre : List.of("Marzo", "Abril", "Mayo")) {
                Sheet sh = wb.getSheet(mesNombre);
                assertNotNull(sh, mesNombre);
                String valor = getCellString(sh, PlanCurricularTemplateBuilder.BLOCK_START_ROW,
                        PlanCurricularTemplateBuilder.COL_CAPACIDADES);
                assertFalse(valor != null && valor.contains("etiquetas básicas"),
                        "La hoja " + mesNombre + " no debe traer el contenido de ejemplo del bloque 1 original");
            }
        }
    }

    @Test
    public void generado_debeConservarLaCabeceraDeLaPlantilla() throws Exception {
        PlanTemplateConfigDto config = configCon("1", mes("Marzo", 4));
        byte[] xlsx = generar(config);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            Sheet sh = wb.getSheet("Marzo");
            boolean tieneMergeTitulo = false;
            for (CellRangeAddress region : sh.getMergedRegions()) {
                if (region.getFirstRow() == 4 && region.getLastRow() == 5
                        && region.getFirstColumn() == 1 && region.getLastColumn() == 35) {
                    tieneMergeTitulo = true;
                    break;
                }
            }
            assertTrue(tieneMergeTitulo, "El merge B5:AJ6 del título debe seguir intacto");
            assertEquals("MINISTERIO DE EDUCACIÓN Y CIENCIAS", getCellString(sh, 1, 2).trim());
            assertTrue(getCellString(sh, 6, 1).startsWith("Disciplina: "));
        }
    }

    @Test
    public void generado_conMaxBloques_noDebeSolaparMerges() throws Exception {
        PlanTemplateConfigDto config = configCon("1", mes("Marzo", PlanCurricularTemplateBuilder.MAX_BLOCKS_PER_MONTH));
        byte[] xlsx = generar(config);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            Sheet sh = wb.getSheet("Marzo");
            List<CellRangeAddress> regiones = sh.getMergedRegions();
            for (int i = 0; i < regiones.size(); i++) {
                for (int j = i + 1; j < regiones.size(); j++) {
                    assertFalse(regiones.get(i).intersects(regiones.get(j)),
                            "Merges solapados: " + regiones.get(i) + " vs " + regiones.get(j));
                }
            }
        }
    }

    // ---- helpers ----

    private byte[] generar(PlanTemplateConfigDto config) throws Exception {
        Asignacion asignacion = new Asignacion(1, 7, 2, 13);
        asignacion.setMateriaNombre("Matemática");
        asignacion.setProfesorNombre("Prof. Prueba");
        asignacion.setCursoOrdinal("5");
        asignacion.setCursoSeccion("A");
        asignacion.setEspecialidad("Informática");

        AsignacionDao asignacionDao = mock(AsignacionDao.class);
        HorarioSlotDao horarioSlotDao = mock(HorarioSlotDao.class);
        UserDao userDao = mock(UserDao.class);
        when(asignacionDao.findById(1)).thenReturn(asignacion);
        when(horarioSlotDao.findByAsignacion(1)).thenReturn(List.of());
        when(userDao.findFirmaImagenById(anyInt())).thenReturn(null);

        PlanCurricularTemplateBuilder builder = new PlanCurricularTemplateBuilder();
        setField(builder, "asignacionDao", asignacionDao);
        setField(builder, "horarioSlotDao", horarioSlotDao);
        setField(builder, "userDao", userDao);

        return builder.buildForAsignacion(1, config);
    }

    private static PlanTemplateConfigDto configCon(String etapa, PlanTemplateConfigDto.MesConfig... meses) {
        PlanTemplateConfigDto config = new PlanTemplateConfigDto();
        config.etapa = etapa;
        List<PlanTemplateConfigDto.MesConfig> list = new ArrayList<>();
        for (PlanTemplateConfigDto.MesConfig mc : meses) list.add(mc);
        config.meses = list;
        return config;
    }

    private static PlanTemplateConfigDto.MesConfig mes(String nombre, int bloques) {
        PlanTemplateConfigDto.MesConfig mc = new PlanTemplateConfigDto.MesConfig();
        mc.mes = nombre;
        mc.bloques = bloques;
        return mc;
    }

    private static void escribirCapacidadesEnCadaBloque(Sheet sh, int bloques) {
        for (int i = 0; i < bloques; i++) {
            int row = PlanCurricularTemplateBuilder.BLOCK_START_ROW + i * PlanCurricularTemplateBuilder.BLOCK_STRIDE;
            Row r = sh.getRow(row);
            if (r == null) r = sh.createRow(row);
            Cell c = r.getCell(PlanCurricularTemplateBuilder.COL_CAPACIDADES);
            if (c == null) c = r.createCell(PlanCurricularTemplateBuilder.COL_CAPACIDADES);
            c.setCellValue("Capacidad bloque " + (i + 1));
        }
    }

    private static String getCellString(Sheet sh, int rowIndex, int colIndex) {
        Row r = sh.getRow(rowIndex);
        if (r == null) return null;
        Cell c = r.getCell(colIndex);
        if (c == null) return null;
        return c.toString();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = PlanCurricularTemplateBuilder.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void setParserTemplateBuilder(PlanCurricularParser parser, PlanCurricularTemplateBuilder builder) throws Exception {
        Field f = PlanCurricularParser.class.getDeclaredField("templateBuilder");
        f.setAccessible(true);
        f.set(parser, builder);
    }
}
