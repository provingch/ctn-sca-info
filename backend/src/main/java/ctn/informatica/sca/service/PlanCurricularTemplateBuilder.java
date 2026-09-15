package ctn.informatica.sca.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.HorarioSlotDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.PlanTemplateConfigDto;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.HorarioSlot;
import ctn.informatica.sca.util.AcademicPeriod;

/**
 * Genera el xlsx de la plantilla de plan curricular a partir de la plantilla real del
 * colegio (recursos {@code plan-curricular-plantilla*.xlsx}): carga el archivo, ajusta
 * qué hojas de mes quedan y cuántos bloques tiene cada una, y completa los 7 campos de
 * cabecera. El resto del formato (logos, colores, bordes, page setup) viene tal cual de
 * la plantilla. Escribe además una hoja oculta <code>_META</code> con el layout JSON
 * para que el parser pueda recuperar la configuración exacta al procesar el archivo
 * entregado.
 */
@Service
public class PlanCurricularTemplateBuilder {

    public static final int BLOCK_START_ROW = 14;
    public static final int BLOCK_STRIDE = 7;
    public static final int COL_CAPACIDADES = 2;
    public static final int COL_TEMAS = 11;
    public static final int COL_ACTIVIDADES = 18;
    public static final int COL_INSTRUMENTOS = 27;
    public static final int COL_INDICADORES = 34;
    public static final String META_SHEET = "_META";
    public static final int MAX_BLOCKS_PER_MONTH = 20;

    private static final String TEMPLATE_ETAPA_1 = "/plan-curricular-plantilla.xlsx";
    private static final String TEMPLATE_ETAPA_2 = "/plan-curricular-plantilla-etapa2.xlsx";
    private static final String HOJA_INSTRUCCIONES = "Instrucciones";

    /** Columna de la etiqueta vertical ("1ra./2da. Etapa"), fila donde arranca su merge. */
    private static final int LABEL_COL = 1;
    private static final int LABEL_ROW = 11;
    /** Fila (0-indexed) del segundo bloque real de la plantilla: sirve de modelo de estilos. */
    private static final int MODEL_BLOCK_ROW = BLOCK_START_ROW + BLOCK_STRIDE - 1;

    private static final Map<String, List<String>> MESES_POR_ETAPA;
    /** Pool viejo (sin Julio en etapa 1) usado por el endpoint GET legacy. */
    private static final Map<String, List<String>> MESES_LEGACY;
    static {
        MESES_POR_ETAPA = new LinkedHashMap<>();
        MESES_POR_ETAPA.put("1", List.of("Marzo", "Abril", "Mayo", "Junio", "Julio"));
        MESES_POR_ETAPA.put("2", List.of("Julio", "Agosto", "Septiembre", "Octubre", "Noviembre"));
        MESES_LEGACY = new LinkedHashMap<>();
        MESES_LEGACY.put("1", List.of("Marzo", "Abril", "Mayo", "Junio"));
        MESES_LEGACY.put("2", List.of("Julio", "Agosto", "Septiembre", "Octubre", "Noviembre"));
    }

    static int ordenMesEnEtapa(String mes, String etapa) {
        List<String> pool = MESES_POR_ETAPA.get(etapa == null ? "1" : etapa.trim());
        if (pool == null) return 1;
        int idx = pool.indexOf(mes);
        return idx < 0 ? 1 : idx + 1;
    }

    @Autowired
    private AsignacionDao asignacionDao;

    @Autowired
    private HorarioSlotDao horarioSlotDao;

    @Autowired
    private UserDao userDao;

    /** Meses habilitados para la etapa; sirve al frontend para armar el selector. */
    public List<String> mesesPosibles(String etapa) {
        List<String> l = MESES_POR_ETAPA.get(etapa == null ? "1" : etapa.trim());
        return l == null ? MESES_POR_ETAPA.get("1") : l;
    }

    /**
     * Compat: se conserva la firma antigua para no romper llamadores existentes. Usa el
     * pool "legacy" (etapa 1 sin Julio) para no cambiar la salida del endpoint GET viejo.
     */
    public byte[] buildForAsignacion(int asignacionId, String etapaOverride) throws Exception {
        String etapa = (etapaOverride != null && ("1".equals(etapaOverride.trim()) || "2".equals(etapaOverride.trim())))
                ? etapaOverride.trim()
                : String.valueOf(AcademicPeriod.currentEtapa());
        PlanTemplateConfigDto def = new PlanTemplateConfigDto();
        def.etapa = etapa;
        for (String m : MESES_LEGACY.get(etapa)) {
            PlanTemplateConfigDto.MesConfig mc = new PlanTemplateConfigDto.MesConfig();
            mc.mes = m;
            mc.bloques = 4;
            def.meses.add(mc);
        }
        return buildForAsignacion(asignacionId, def);
    }

    public byte[] buildForAsignacion(int asignacionId, PlanTemplateConfigDto config) throws Exception {
        Asignacion a = asignacionDao.findById(asignacionId);
        if (a == null) throw new IllegalArgumentException("Asignación no encontrada");

        String etapaActual;
        if (config != null && config.etapa != null
                && ("1".equals(config.etapa.trim()) || "2".equals(config.etapa.trim()))) {
            etapaActual = config.etapa.trim();
        } else {
            etapaActual = String.valueOf(AcademicPeriod.currentEtapa());
        }

        List<String> pool = mesesPosibles(etapaActual);
        List<PlanTemplateConfigDto.MesConfig> mesesConfig = normalizeMesesConfig(config, pool);

        String title = String.format("PLAN DE DESARROLLO CURRICULAR ETAPA:_%s_%d",
                etapaActual + "°", AcademicPeriod.current());
        String disciplina = nullToEmpty(a.getMateriaNombre());
        String docente = nullToEmpty(a.getProfesorNombre());
        String curso = nullToEmpty(a.getCursoOrdinal());
        String seccion = nullToEmpty(a.getCursoSeccion());
        String especialidad = nullToEmpty(a.getEspecialidad());
        String turno = resolveTurno(asignacionId);

        byte[] firmaProfesor = decodeFirma(a.getProfesorId());

        String templateResource = "1".equals(etapaActual) ? TEMPLATE_ETAPA_1 : TEMPLATE_ETAPA_2;
        try (InputStream templateIn = getClass().getResourceAsStream(templateResource)) {
            if (templateIn == null) {
                throw new IllegalStateException("No se encontró el recurso de plantilla: " + templateResource);
            }
            try (Workbook wb = new XSSFWorkbook(templateIn);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                // OJO: _META se crea ANTES de borrar hojas de mes sobrantes. Si se crea después,
                // wb.createSheet reutiliza el nombre de parte físico (sheetN.xml) de una hoja recién
                // borrada por removeSheetAt, y arrastra su _rels/sheetN.xml.rels huérfano (apunta a
                // un drawing ya eliminado) -> xlsx que POI abre con "Skipped invalid entry". Crear
                // _META mientras todas las hojas originales siguen presentes le asigna un nombre de
                // parte nunca usado antes, así que no hay colisión posible.
                writeMetaSheet(wb, etapaActual, AcademicPeriod.current(), asignacionId, mesesConfig);

                selectAndOrderSheets(wb, etapaActual, mesesConfig);

                CellStyle signatureLabelStyle = buildSignatureLabelStyle(wb);
                for (PlanTemplateConfigDto.MesConfig mc : mesesConfig) {
                    Sheet sh = wb.getSheet(mc.mes);
                    fillHeaderData(sh, title, disciplina, docente, curso, seccion, turno, especialidad);
                    regenerateBlocks(sh, mc.bloques);

                    int signatureRow = signatureRowFor(mc.bloques, BLOCK_START_ROW, BLOCK_STRIDE);
                    CellStyle signatureBoxStyle = sh.getRow(BLOCK_START_ROW).getCell(COL_CAPACIDADES).getCellStyle();
                    writeSignatureArea(wb, sh, signatureRow, firmaProfesor, null, signatureBoxStyle, signatureLabelStyle);
                }

                wb.write(out);
                return out.toByteArray();
            }
        }
    }

    /** Deja en el workbook sólo las hojas de mes pedidas por el config, en ese orden. */
    private void selectAndOrderSheets(Workbook wb, String etapaActual, List<PlanTemplateConfigDto.MesConfig> mesesConfig) {
        Set<String> requeridos = new LinkedHashSet<>();
        for (PlanTemplateConfigDto.MesConfig mc : mesesConfig) requeridos.add(mc.mes);

        for (String mes : requeridos) {
            if (wb.getSheet(mes) == null) {
                throw new IllegalArgumentException("La plantilla de la etapa " + etapaActual + " no tiene la hoja: " + mes);
            }
        }

        for (int i = wb.getNumberOfSheets() - 1; i >= 0; i--) {
            String nombre = wb.getSheetName(i);
            if (!HOJA_INSTRUCCIONES.equals(nombre) && !META_SHEET.equals(nombre) && !requeridos.contains(nombre)) {
                wb.removeSheetAt(i);
            }
        }

        int posicion = wb.getSheetIndex(HOJA_INSTRUCCIONES) + 1;
        for (PlanTemplateConfigDto.MesConfig mc : mesesConfig) {
            wb.setSheetOrder(mc.mes, posicion++);
        }
    }

    private List<PlanTemplateConfigDto.MesConfig> normalizeMesesConfig(PlanTemplateConfigDto config, List<String> pool) {
        List<PlanTemplateConfigDto.MesConfig> result = new ArrayList<>();
        if (config != null && config.meses != null) {
            for (PlanTemplateConfigDto.MesConfig mc : config.meses) {
                if (mc == null || mc.mes == null) continue;
                if (!pool.contains(mc.mes)) {
                    throw new IllegalArgumentException("Mes no habilitado para la etapa: " + mc.mes);
                }
                PlanTemplateConfigDto.MesConfig copy = new PlanTemplateConfigDto.MesConfig();
                copy.mes = mc.mes;
                copy.bloques = Math.max(1, Math.min(MAX_BLOCKS_PER_MONTH, mc.bloques));
                result.add(copy);
            }
        }
        if (result.isEmpty()) {
            // Default: todos los meses del pool con 4 bloques (mismo comportamiento previo).
            for (String m : pool) {
                PlanTemplateConfigDto.MesConfig def = new PlanTemplateConfigDto.MesConfig();
                def.mes = m;
                def.bloques = 4;
                result.add(def);
            }
        }
        return result;
    }

    private String resolveTurno(int asignacionId) {
        try {
            List<HorarioSlot> slots = horarioSlotDao.findByAsignacion(asignacionId);
            if (slots == null || slots.isEmpty()) return "sin horario cargado";
            boolean m = false, t = false;
            for (HorarioSlot s : slots) {
                String hi = s.getHoraInicio();
                if (hi == null || hi.isBlank()) continue;
                int hour;
                try { hour = Integer.parseInt(hi.split(":")[0]); }
                catch (Exception e) { continue; }
                if (hour < 12) m = true; else t = true;
            }
            if (m && t) return "Mañana y Tarde";
            if (m) return "Mañana";
            if (t) return "Tarde";
            return "sin horario cargado";
        } catch (Exception e) {
            return "";
        }
    }

    private byte[] decodeFirma(int usuarioId) {
        try {
            String firma = userDao.findFirmaImagenById(usuarioId);
            if (firma == null || firma.isBlank()) return null;
            int comma = firma.indexOf(',');
            String b64 = comma >= 0 ? firma.substring(comma + 1) : firma;
            return Base64.getDecoder().decode(b64.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** Escribe sólo los 7 campos de datos de la cabecera; labels, merges y estilos ya vienen de la plantilla. */
    private void fillHeaderData(Sheet sh, String title, String disciplina, String docente,
                                String curso, String seccion, String turno, String especialidad) {
        setValueOnly(sh, 4, 1, title);
        setValueOnly(sh, 6, 1, "Disciplina: " + disciplina);
        setValueOnly(sh, 6, 22, "Docente: " + docente);
        setValueOnly(sh, 8, 1, "Curso: " + curso);
        setValueOnly(sh, 8, 12, "Sección: " + seccion);
        setValueOnly(sh, 8, 18, "Turno: " + turno);
        setValueOnly(sh, 8, 22, "Especialidad: " + especialidad);
    }

    private void setValueOnly(Sheet sh, int rowIndex, int colIndex, String value) {
        Row r = sh.getRow(rowIndex);
        if (r == null) r = sh.createRow(rowIndex);
        Cell c = r.getCell(colIndex);
        if (c == null) c = r.createCell(colIndex);
        c.setCellValue(value == null ? "" : value);
    }

    /**
     * Borra el área de bloques (fila BLOCK_START_ROW en adelante) y la vuelve a escribir
     * con N bloques, clonando celda por celda el estilo del segundo bloque real de la
     * plantilla (fila {@link #MODEL_BLOCK_ROW}, único que sigue el patrón stride=7 sin el
     * contenido de ejemplo). El bloque 1 original NO se usa como modelo.
     */
    private void regenerateBlocks(Sheet sh, int bloques) {
        int lastDataCol = COL_INDICADORES + 1;

        CellStyle[][] modelStyle = new CellStyle[BLOCK_STRIDE][lastDataCol + 1];
        float[] modelHeight = new float[BLOCK_STRIDE];
        for (int ro = 0; ro < BLOCK_STRIDE; ro++) {
            Row modelRow = sh.getRow(MODEL_BLOCK_ROW + ro);
            modelHeight[ro] = modelRow != null ? modelRow.getHeightInPoints() : -1f;
            for (int c = LABEL_COL; c <= lastDataCol; c++) {
                Cell cell = modelRow != null ? modelRow.getCell(c) : null;
                modelStyle[ro][c] = cell != null ? cell.getCellStyle() : null;
            }
        }

        removeVerticalLabelMerge(sh);
        for (int i = sh.getNumMergedRegions() - 1; i >= 0; i--) {
            if (sh.getMergedRegion(i).getFirstRow() >= BLOCK_START_ROW) {
                sh.removeMergedRegion(i);
            }
        }
        for (int r = sh.getLastRowNum(); r >= BLOCK_START_ROW; r--) {
            Row row = sh.getRow(r);
            if (row != null) sh.removeRow(row);
        }

        int lastRow = BLOCK_START_ROW - 1;
        for (int i = 0; i < bloques; i++) {
            int row0 = BLOCK_START_ROW + i * BLOCK_STRIDE;
            lastRow = row0 + BLOCK_STRIDE - 1;

            for (int ro = 0; ro < BLOCK_STRIDE; ro++) {
                Row row = sh.createRow(row0 + ro);
                if (modelHeight[ro] >= 0) row.setHeightInPoints(modelHeight[ro]);
                for (int c = LABEL_COL; c <= lastDataCol; c++) {
                    CellStyle style = modelStyle[ro][c];
                    if (style == null) continue;
                    row.createCell(c).setCellStyle(style);
                }
            }

            mergeRegion(sh, row0, row0 + 6, COL_CAPACIDADES, COL_TEMAS - 1);
            mergeRegion(sh, row0, row0 + 6, COL_TEMAS, COL_ACTIVIDADES - 1);
            mergeRegion(sh, row0, row0 + 6, COL_ACTIVIDADES, COL_INSTRUMENTOS - 1);
            mergeRegion(sh, row0, row0 + 6, COL_INSTRUMENTOS, COL_INSTRUMENTOS + 3);
            mergeRegion(sh, row0, row0 + 1, COL_INDICADORES, COL_INDICADORES + 1);
            mergeRegion(sh, row0 + 2, row0 + 3, COL_INDICADORES, COL_INDICADORES + 1);
            mergeRegion(sh, row0 + 4, row0 + 6, COL_INDICADORES, COL_INDICADORES + 1);
        }

        mergeRegion(sh, LABEL_ROW, lastRow, LABEL_COL, LABEL_COL);
    }

    /** Saca el merge vertical de la etiqueta de etapa (B12:B..); el resto de merges con fila < BLOCK_START_ROW no se tocan. */
    private void removeVerticalLabelMerge(Sheet sh) {
        for (int i = sh.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress region = sh.getMergedRegion(i);
            if (region.getFirstColumn() == LABEL_COL && region.getLastColumn() == LABEL_COL
                    && region.getFirstRow() < BLOCK_START_ROW) {
                sh.removeMergedRegion(i);
            }
        }
    }

    private void mergeRegion(Sheet sh, int r1, int r2, int c1, int c2) {
        if (r1 == r2 && c1 == c2) return;
        sh.addMergedRegion(new CellRangeAddress(r1, r2, c1, c2));
    }

    static int signatureRowFor(int bloques, int blockStartRow, int blockStride) {
        return blockStartRow + bloques * blockStride + 2;
    }

    private void writeSignatureArea(Workbook wb, Sheet sh, int row, byte[] firmaProfesor,
                                    byte[] firmaEvaluador, CellStyle boxStyle, CellStyle labelStyle) {
        setCell(sh, row, 1, "Firma del Profesor", labelStyle);
        mergeRegion(sh, row, row, 1, 15);
        setCell(sh, row, 22, "Firma del Evaluador", labelStyle);
        mergeRegion(sh, row, row, 22, 36);

        fillBoxStyle(sh, row + 1, 1, row + 6, 15, boxStyle);
        fillBoxStyle(sh, row + 1, 22, row + 6, 36, boxStyle);

        if (firmaProfesor != null) {
            insertPicture(wb, sh, firmaProfesor, row + 1, 2, row + 6, 14);
        }
        if (firmaEvaluador != null) {
            insertPicture(wb, sh, firmaEvaluador, row + 1, 23, row + 6, 35);
        }
    }

    private void fillBoxStyle(Sheet sh, int r1, int c1, int r2, int c2, CellStyle style) {
        for (int r = r1; r <= r2; r++) {
            Row row = sh.getRow(r);
            if (row == null) row = sh.createRow(r);
            for (int c = c1; c <= c2; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) cell = row.createCell(c);
                cell.setCellStyle(style);
            }
        }
        mergeRegion(sh, r1, r2, c1, c2);
    }

    private CellStyle buildSignatureLabelStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setItalic(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private void insertPicture(Workbook wb, Sheet sh, byte[] png, int r1, int c1, int r2, int c2) {
        try {
            int pictureIdx = wb.addPicture(png, Workbook.PICTURE_TYPE_PNG);
            CreationHelper helper = wb.getCreationHelper();
            Drawing<?> drawing = sh.createDrawingPatriarch();
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(c1); anchor.setRow1(r1);
            anchor.setCol2(c2); anchor.setRow2(r2);
            drawing.createPicture(anchor, pictureIdx);
        } catch (Exception ignored) { }
    }

    private void writeMetaSheet(Workbook wb, String etapa, int anio, int asignacionId,
                                List<PlanTemplateConfigDto.MesConfig> mesesConfig) {
        Sheet meta = wb.createSheet(META_SHEET);
        // Formato simple: cada fila es "clave" | "valor". Para la lista de meses,
        // se marcan filas contiguas con clave "mes" y columnas mes/bloques/ordenMes.
        int r = 0;
        putKV(meta, r++, "etapa", etapa);
        putKV(meta, r++, "anio", String.valueOf(anio));
        putKV(meta, r++, "asignacionId", String.valueOf(asignacionId));
        putKV(meta, r++, "blockStartRow", String.valueOf(BLOCK_START_ROW));
        putKV(meta, r++, "blockStride", String.valueOf(BLOCK_STRIDE));
        putKV(meta, r++, "colCapacidades", String.valueOf(COL_CAPACIDADES));
        putKV(meta, r++, "colTemas", String.valueOf(COL_TEMAS));
        putKV(meta, r++, "colActividades", String.valueOf(COL_ACTIVIDADES));
        putKV(meta, r++, "colInstrumentos", String.valueOf(COL_INSTRUMENTOS));
        putKV(meta, r++, "colIndicadores", String.valueOf(COL_INDICADORES));
        for (PlanTemplateConfigDto.MesConfig mc : mesesConfig) {
            Row row = meta.createRow(r++);
            row.createCell(0).setCellValue("mes");
            row.createCell(1).setCellValue(mc.mes);
            row.createCell(2).setCellValue(String.valueOf(mc.bloques));
            row.createCell(3).setCellValue(String.valueOf(ordenMesEnEtapa(mc.mes, etapa)));
        }
        int idx = wb.getSheetIndex(META_SHEET);
        wb.setSheetHidden(idx, true);
    }

    private void putKV(Sheet sh, int row, String k, String v) {
        Row r = sh.createRow(row);
        r.createCell(0).setCellValue(k);
        r.createCell(1).setCellValue(v == null ? "" : v);
    }

    // ---- Utilidades expuestas para el parser y aprobación ----

    public static class MetaInfo {
        public String etapa;
        public int anio;
        public int blockStartRow = BLOCK_START_ROW;
        public int blockStride = BLOCK_STRIDE;
        public int colCapacidades = COL_CAPACIDADES;
        public int colTemas = COL_TEMAS;
        public int colActividades = COL_ACTIVIDADES;
        public int colInstrumentos = COL_INSTRUMENTOS;
        public int colIndicadores = COL_INDICADORES;
        public List<String> meses = new ArrayList<>();
        public Map<String, Integer> bloquesPorMes = new LinkedHashMap<>();
        public Map<String, Integer> ordenPorMes = new LinkedHashMap<>();
    }

    public MetaInfo readMeta(Workbook wb) {
        Sheet meta = wb.getSheet(META_SHEET);
        if (meta == null) return null;
        MetaInfo info = new MetaInfo();
        boolean hadAnything = false;
        for (int i = 0; i <= meta.getLastRowNum(); i++) {
            Row r = meta.getRow(i);
            if (r == null) continue;
            String key = cellString(r, 0);
            if (key == null) continue;
            String v1 = cellString(r, 1);
            if ("mes".equalsIgnoreCase(key)) {
                if (v1 == null || v1.isBlank()) continue;
                int b = parseIntOr(cellString(r, 2), 4);
                int ord = parseIntOr(cellString(r, 3), ordenMesEnEtapa(v1, info.etapa));
                info.meses.add(v1);
                info.bloquesPorMes.put(v1, b);
                info.ordenPorMes.put(v1, ord);
                hadAnything = true;
                continue;
            }
            switch (key) {
                case "etapa": info.etapa = v1; hadAnything = true; break;
                case "anio": info.anio = parseIntOr(v1, 0); hadAnything = true; break;
                case "blockStartRow": info.blockStartRow = parseIntOr(v1, BLOCK_START_ROW); break;
                case "blockStride": info.blockStride = parseIntOr(v1, BLOCK_STRIDE); break;
                case "colCapacidades": info.colCapacidades = parseIntOr(v1, COL_CAPACIDADES); break;
                case "colTemas": info.colTemas = parseIntOr(v1, COL_TEMAS); break;
                case "colActividades": info.colActividades = parseIntOr(v1, COL_ACTIVIDADES); break;
                case "colInstrumentos": info.colInstrumentos = parseIntOr(v1, COL_INSTRUMENTOS); break;
                case "colIndicadores": info.colIndicadores = parseIntOr(v1, COL_INDICADORES); break;
                default: break;
            }
        }
        return hadAnything ? info : null;
    }

    private static String cellString(Row r, int col) {
        Cell c = r.getCell(col);
        if (c == null) return null;
        switch (c.getCellType()) {
            case STRING: return c.getStringCellValue();
            case NUMERIC: {
                double d = c.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
                return String.valueOf(d);
            }
            case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
            default: return c.toString();
        }
    }

    private static int parseIntOr(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    /** Estampa la firma del evaluador en un xlsx ya generado. No-op si el usuario no tiene firma. */
    public byte[] injectEvaluadorSignature(byte[] originalXlsx, int evaluadorUsuarioId) throws Exception {
        if (originalXlsx == null || originalXlsx.length == 0) return originalXlsx;
        byte[] firma = decodeFirma(evaluadorUsuarioId);
        if (firma == null) return originalXlsx;
        try (ByteArrayInputStream in = new ByteArrayInputStream(originalXlsx);
             Workbook wb = WorkbookFactory.create(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            MetaInfo meta = readMeta(wb);
            List<String> hojas;
            if (meta != null) {
                hojas = meta.meses;
            } else {
                hojas = new ArrayList<>();
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    String name = wb.getSheetName(i);
                    if (MESES_POR_ETAPA.get("1").contains(name) || MESES_POR_ETAPA.get("2").contains(name)) {
                        hojas.add(name);
                    }
                }
            }
            int startRow = meta != null ? meta.blockStartRow : BLOCK_START_ROW;
            int stride = meta != null ? meta.blockStride : BLOCK_STRIDE;
            for (String hoja : hojas) {
                Sheet sh = wb.getSheet(hoja);
                if (sh == null) continue;
                int bloques = meta != null && meta.bloquesPorMes.containsKey(hoja)
                        ? meta.bloquesPorMes.get(hoja) : 4;
                int signatureRow = signatureRowFor(bloques, startRow, stride);
                insertPicture(wb, sh, firma, signatureRow + 1, 23, signatureRow + 6, 35);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static String nullToEmpty(String v) { return v == null ? "" : v; }

    private void setCell(Sheet sh, int rowIndex, int colIndex, String value, CellStyle style) {
        Row r = sh.getRow(rowIndex);
        if (r == null) r = sh.createRow(rowIndex);
        Cell c = r.getCell(colIndex);
        if (c == null) c = r.createCell(colIndex);
        c.setCellValue(value == null ? "" : value);
        if (style != null) c.setCellStyle(style);
    }
}
