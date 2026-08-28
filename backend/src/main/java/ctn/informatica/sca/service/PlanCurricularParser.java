package ctn.informatica.sca.service;

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
import org.springframework.stereotype.Service;

import ctn.informatica.sca.dto.PlanCurricularDto;
import ctn.informatica.sca.dto.TemaPlanDto;

@Service
public class PlanCurricularParser {

    private static final String ETAPA_PARSE_ERROR =
            "No se pudo interpretar la etapa/año del plan. Descargá la plantilla actual y volvé a completarla.";
    private static final Map<String, Integer> MES_ORDEN = new LinkedHashMap<>();
    private static final Map<String, List<String>> MESES_POR_ETAPA = new LinkedHashMap<>();
    static {
        MESES_POR_ETAPA.put("1", List.of("Marzo", "Abril", "Mayo", "Junio"));
        MESES_POR_ETAPA.put("2", List.of("Julio", "Agosto", "Septiembre", "Octubre", "Noviembre"));
        MES_ORDEN.put("Marzo", 1);
        MES_ORDEN.put("Abril", 2);
        MES_ORDEN.put("Mayo", 3);
        MES_ORDEN.put("Junio", 4);
        MES_ORDEN.put("Julio", 1);
        MES_ORDEN.put("Agosto", 2);
        MES_ORDEN.put("Septiembre", 3);
        MES_ORDEN.put("Octubre", 4);
        MES_ORDEN.put("Noviembre", 5);
    }

    public PlanCurricularDto parse(InputStream in) throws Exception {
        try (Workbook wb = WorkbookFactory.create(in)) {
            String primerMes = findFirstAvailableSheet(wb);
            if (primerMes == null) {
                throw new IllegalArgumentException("El plan no contiene ninguna hoja de meses válida");
            }
            Sheet primerSheet = wb.getSheet(primerMes);
            EtapaAnio etapaAnio = parseEtapaAnio(getCellString(primerSheet, 4, 1));
            List<String> mesesEsperados = mesesEsperadosParaEtapa(etapaAnio.etapa());
            for (String nombre : mesesEsperados) {
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

            List<TemaPlanDto> temas = new ArrayList<>();
            int[] bloqueRows = new int[] {14, 20, 27, 34};
            for (String nombreMes : mesesEsperados) {
                Sheet sh = wb.getSheet(nombreMes);
                int ordenMes = MES_ORDEN.getOrDefault(nombreMes, 1);
                for (int i = 0; i < 4; i++) {
                    int row = bloqueRows[i];
                    String contenidos = getCellString(sh, row, 2);
                    if (contenidos == null || contenidos.isBlank()) continue;
                    TemaPlanDto t = new TemaPlanDto();
                    t.mes = nombreMes;
                    t.ordenMes = ordenMes;
                    t.bloque = i + 1;
                    t.capacidades = getCellString(sh, row, 2);
                    t.temasContenidos = getCellString(sh, row, 11);
                    t.actividades = getCellString(sh, row, 18);
                    t.instrumentos = getCellString(sh, row, 27);
                    t.indicadorConceptual = getCellString(sh, row, 34);
                    t.indicadorProcedimental = getCellString(sh, row + 2, 34);
                    t.indicadorActitudinal = getCellString(sh, row + 4, 34);
                    temas.add(t);
                }
            }
            out.temas = temas;
            if (temas.isEmpty()) throw new IllegalArgumentException("El plan no contiene temas en ninguna hoja");
            return out;
        }
    }

    private String findFirstAvailableSheet(Workbook wb) {
        for (String nombre : List.of("Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre")) {
            if (wb.getSheet(nombre) != null) {
                return nombre;
            }
        }
        return null;
    }

    private List<String> mesesEsperadosParaEtapa(String etapa) {
        if (etapa == null) {
            throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        }
        List<String> meses = MESES_POR_ETAPA.get(etapa.trim());
        if (meses == null) {
            throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        }
        return meses;
    }

    static EtapaAnio parseEtapaAnio(String tituloCelda) {
        if (tituloCelda == null) {
            throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        }

        int etapaIndex = tituloCelda.indexOf("ETAPA:");
        if (etapaIndex < 0) {
            throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        }

        String after = tituloCelda.substring(etapaIndex + "ETAPA:".length()).trim();
        after = after.replaceAll("^_+", "").replaceAll("_+$", "");
        String[] parts = after.split("_");
        if (parts.length < 2) {
            throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        }

        String etapa = parts[0].trim().replaceAll("[^0-9]", "");
        int anio;
        try {
            anio = Integer.parseInt(parts[parts.length - 1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ETAPA_PARSE_ERROR, e);
        }

        if (!("1".equals(etapa) || "2".equals(etapa)) || anio == 0) {
            throw new IllegalArgumentException(ETAPA_PARSE_ERROR);
        }
        return new EtapaAnio(etapa, anio);
    }

    record EtapaAnio(String etapa, int anio) {
    }

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
        return v.substring(idx+1).trim();
    }
}
