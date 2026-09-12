package ctn.informatica.sca.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ctn.informatica.sca.dto.PlanCurricularDto;
import ctn.informatica.sca.dto.TemaPlanDto;

@Service
public class PlanCurricularParser {

    private static final String ETAPA_PARSE_ERROR =
            "No se pudo interpretar la etapa/año del plan. Descargá la plantilla actual y volvé a completarla.";
    private static final Map<String, List<String>> MESES_POR_ETAPA = new LinkedHashMap<>();
    static {
        MESES_POR_ETAPA.put("1", List.of("Marzo", "Abril", "Mayo", "Junio", "Julio"));
        MESES_POR_ETAPA.put("2", List.of("Julio", "Agosto", "Septiembre", "Octubre", "Noviembre"));
    }

    private static int ordenMesEnEtapa(String mes, String etapa) {
        List<String> pool = MESES_POR_ETAPA.get(etapa == null ? "1" : etapa.trim());
        if (pool == null) return 1;
        int idx = pool.indexOf(mes);
        return idx < 0 ? 1 : idx + 1;
    }

    /** Layout viejo (plantillas pre-config dinámica): 4 bloques fijos por hoja. */
    private static final int[] LEGACY_BLOCK_ROWS = { 14, 20, 27, 34 };

    @Autowired(required = false)
    private PlanCurricularTemplateBuilder templateBuilder;

    public PlanCurricularDto parse(InputStream in) throws Exception {
        // Bufferizamos: puede que necesitemos re-abrir (no lo hacemos, pero es más seguro).
        try (Workbook wb = WorkbookFactory.create(in)) {
            PlanCurricularTemplateBuilder.MetaInfo meta = readMetaSafe(wb);
            String primerMes = findFirstAvailableSheet(wb, meta);
            if (primerMes == null) {
                throw new IllegalArgumentException("El plan no contiene ninguna hoja de meses válida");
            }
            Sheet primerSheet = wb.getSheet(primerMes);
            EtapaAnio etapaAnio;
            if (meta != null && meta.etapa != null && meta.anio > 0) {
                etapaAnio = new EtapaAnio(meta.etapa, meta.anio);
            } else {
                etapaAnio = parseEtapaAnio(getCellString(primerSheet, 4, 1));
            }

            List<String> hojasMeses;
            if (meta != null && !meta.meses.isEmpty()) {
                hojasMeses = meta.meses;
            } else {
                hojasMeses = mesesEsperadosParaEtapa(etapaAnio.etapa());
            }
            for (String nombre : hojasMeses) {
                if (wb.getSheet(nombre) == null) {
                    throw new IllegalArgumentException("Falta la hoja: " + nombre);
                }
            }

            PlanCurricularDto out = new PlanCurricularDto();
            out.etapa = etapaAnio.etapa();
            out.anio = etapaAnio.anio();
            out.disciplina = extractAfterColon(getCellString(primerSheet, 6, 1));
            out.turno = extractAfterColon(getCellString(primerSheet, 8, 18));
            out.curso = extractAfterColon(getCellString(primerSheet, 8, 1));
            out.seccion = extractAfterColon(getCellString(primerSheet, 8, 12));
            out.especialidad = extractAfterColon(getCellString(primerSheet, 8, 22));

            int colCap = meta != null ? meta.colCapacidades : 2;
            int colTemas = meta != null ? meta.colTemas : 11;
            int colAct = meta != null ? meta.colActividades : 18;
            int colInst = meta != null ? meta.colInstrumentos : 27;
            int colInd = meta != null ? meta.colIndicadores : 34;

            List<TemaPlanDto> temas = new ArrayList<>();
            for (String nombreMes : hojasMeses) {
                Sheet sh = wb.getSheet(nombreMes);
                int ordenMes = meta != null && meta.ordenPorMes.containsKey(nombreMes)
                        ? meta.ordenPorMes.get(nombreMes)
                        : ordenMesEnEtapa(nombreMes, etapaAnio.etapa());
                int[] rows = bloqueRowsFor(nombreMes, meta);
                for (int i = 0; i < rows.length; i++) {
                    int row = rows[i];
                    String contenidos = getCellString(sh, row, colCap);
                    if (contenidos == null || contenidos.isBlank()) continue;
                    TemaPlanDto t = new TemaPlanDto();
                    t.mes = nombreMes;
                    t.ordenMes = ordenMes;
                    t.bloque = i + 1;
                    t.capacidades = contenidos;
                    t.temasContenidos = getCellString(sh, row, colTemas);
                    t.actividades = getCellString(sh, row, colAct);
                    t.instrumentos = getCellString(sh, row, colInst);
                    t.indicadorConceptual = getCellString(sh, row, colInd);
                    t.indicadorProcedimental = getCellString(sh, row + 2, colInd);
                    t.indicadorActitudinal = getCellString(sh, row + 4, colInd);
                    temas.add(t);
                }
            }
            out.temas = temas;
            if (temas.isEmpty()) throw new IllegalArgumentException("El plan no contiene temas en ninguna hoja");
            return out;
        }
    }

    /** Sobrecarga por conveniencia. */
    public PlanCurricularDto parse(byte[] bytes) throws Exception {
        return parse(new ByteArrayInputStream(bytes));
    }

    private PlanCurricularTemplateBuilder.MetaInfo readMetaSafe(Workbook wb) {
        if (templateBuilder == null) return null;
        try { return templateBuilder.readMeta(wb); }
        catch (Exception e) { return null; }
    }

    private int[] bloqueRowsFor(String mes, PlanCurricularTemplateBuilder.MetaInfo meta) {
        if (meta == null || !meta.bloquesPorMes.containsKey(mes)) return LEGACY_BLOCK_ROWS;
        int bloques = meta.bloquesPorMes.get(mes);
        int[] rows = new int[bloques];
        for (int i = 0; i < bloques; i++) rows[i] = meta.blockStartRow + i * meta.blockStride;
        return rows;
    }

    private String findFirstAvailableSheet(Workbook wb, PlanCurricularTemplateBuilder.MetaInfo meta) {
        if (meta != null && !meta.meses.isEmpty()) {
            for (String name : meta.meses) if (wb.getSheet(name) != null) return name;
        }
        for (String nombre : List.of("Marzo", "Abril", "Mayo", "Junio", "Julio",
                "Agosto", "Septiembre", "Octubre", "Noviembre")) {
            if (wb.getSheet(nombre) != null) return nombre;
        }
        return null;
    }

    private List<String> mesesEsperadosParaEtapa(String etapa) {
        if (etapa == null) throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        List<String> meses = MESES_POR_ETAPA.get(etapa.trim());
        if (meses == null) throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        return meses;
    }

    static EtapaAnio parseEtapaAnio(String tituloCelda) {
        if (tituloCelda == null) throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        int etapaIndex = tituloCelda.indexOf("ETAPA:");
        if (etapaIndex < 0) throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        String after = tituloCelda.substring(etapaIndex + "ETAPA:".length()).trim();
        after = after.replaceAll("^_+", "").replaceAll("_+$", "");
        String[] parts = after.split("_");
        if (parts.length < 2) throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        String etapa = parts[0].trim().replaceAll("[^0-9]", "");
        int anio;
        try { anio = Integer.parseInt(parts[parts.length - 1].trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(ETAPA_PARSE_ERROR, e); }
        if (!("1".equals(etapa) || "2".equals(etapa)) || anio == 0) {
            throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        }
        return new EtapaAnio(etapa, anio);
    }

    record EtapaAnio(String etapa, int anio) { }

    private String getCellString(Sheet sh, int rowIndex, int colIndex) {
        Row r = sh.getRow(rowIndex);
        if (r == null) return null;
        Cell c = r.getCell(colIndex);
        if (c == null) return null;
        switch (c.getCellType()) {
            case STRING: return c.getStringCellValue().trim();
            case NUMERIC: return String.valueOf(c.getNumericCellValue());
            case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
            default: return c.toString().trim();
        }
    }

    private String extractAfterColon(String v) {
        if (v == null) return null;
        int idx = v.indexOf(":");
        if (idx < 0) return v.trim();
        return v.substring(idx + 1).trim();
    }
}
