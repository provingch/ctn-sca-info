package ctn.informatica.sca.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final Map<String,Integer> MES_ORDEN = new HashMap<>();
    static {
        MES_ORDEN.put("Marzo", 1);
        MES_ORDEN.put("Abril", 2);
        MES_ORDEN.put("Mayo", 3);
        MES_ORDEN.put("Junio", 4);
    }

    public PlanCurricularDto parse(InputStream in) throws Exception {
        try (Workbook wb = WorkbookFactory.create(in)) {
            // verify sheets
            for (String nombre : new String[]{"Marzo","Abril","Mayo","Junio"}) {
                if (wb.getSheet(nombre) == null) throw new IllegalArgumentException("Falta la hoja: " + nombre);
            }
            PlanCurricularDto out = new PlanCurricularDto();
            // read header from first sheet (Marzo)
            Sheet marzo = wb.getSheet("Marzo");
            EtapaAnio etapaAnio = parseEtapaAnio(getCellString(marzo, 4, 1)); // B5 -> row 4 col 1
            out.etapa = etapaAnio.etapa();
            out.anio = etapaAnio.anio();
            out.disciplina = extractAfterColon(getCellString(marzo,6,1));
            out.turno = extractAfterColon(getCellString(marzo,8,18));
            out.curso = extractAfterColon(getCellString(marzo,8,1));
            out.seccion = extractAfterColon(getCellString(marzo,8,12));
            out.especialidad = extractAfterColon(getCellString(marzo,8,22));

            List<TemaPlanDto> temas = new ArrayList<>();
            // Blocks per month: fixed anchor rows: 15,21,28,35 (1-based). zero-based rows:14,20,27,34
            int[] bloqueRows = new int[]{14,20,27,34};
            for (Map.Entry<String,Integer> me : MES_ORDEN.entrySet()) {
                Sheet sh = wb.getSheet(me.getKey());
                int ordenMes = me.getValue();
                for (int i = 0; i < 4; i++) {
                    int row = bloqueRows[i];
                    String contenidos = getCellString(sh, row, 2); // C? C15 -> col2
                    if (contenidos == null || contenidos.isBlank()) continue;
                    TemaPlanDto t = new TemaPlanDto();
                    t.mes = me.getKey();
                    t.ordenMes = ordenMes;
                    t.bloque = i+1;
                    t.capacidades = getCellString(sh, row, 2);
                    t.temasContenidos = getCellString(sh, row, 11);
                    t.actividades = getCellString(sh, row, 18);
                    t.instrumentos = getCellString(sh, row, 27);
                    t.indicadorConceptual = getCellString(sh, row, 34);
                    t.indicadorProcedimental = getCellString(sh, row+2, 34);
                    t.indicadorActitudinal = getCellString(sh, row+4, 34);
                    temas.add(t);
                }
            }
            out.temas = temas;
            if (temas.isEmpty()) throw new IllegalArgumentException("El plan no contiene temas en ninguna hoja");
            return out;
        }
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
