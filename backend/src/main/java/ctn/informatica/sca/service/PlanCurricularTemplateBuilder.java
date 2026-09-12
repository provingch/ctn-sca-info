package ctn.informatica.sca.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
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
 * Genera el xlsx de la plantilla de plan curricular de forma dinámica en base a
 * la config del profesor (meses + bloques por mes). Escribe una hoja oculta
 * <code>_META</code> con el layout JSON para que el parser pueda recuperar la
 * configuración exacta al procesar el archivo entregado.
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

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Styles styles = new Styles(wb);

            for (PlanTemplateConfigDto.MesConfig mc : mesesConfig) {
                Sheet sh = wb.createSheet(mc.mes);
                setColumnWidths(sh);
                writeHeader(sh, title, disciplina, docente, curso, seccion, turno, especialidad, styles);
                writeBlocks(sh, mc.bloques, styles);

                int signatureRow = BLOCK_START_ROW + mc.bloques * BLOCK_STRIDE + 2;
                writeSignatureArea(wb, sh, signatureRow, firmaProfesor, null, styles);
            }

            writeMetaSheet(wb, etapaActual, AcademicPeriod.current(), asignacionId, mesesConfig);

            wb.write(out);
            return out.toByteArray();
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

    private void setColumnWidths(Sheet sh) {
        sh.setColumnWidth(1, 3 * 256);
        for (int c = COL_CAPACIDADES; c < COL_TEMAS; c++) sh.setColumnWidth(c, 12 * 256);
        for (int c = COL_TEMAS; c < COL_ACTIVIDADES; c++) sh.setColumnWidth(c, 12 * 256);
        for (int c = COL_ACTIVIDADES; c < COL_INSTRUMENTOS; c++) sh.setColumnWidth(c, 12 * 256);
        for (int c = COL_INSTRUMENTOS; c < COL_INDICADORES; c++) sh.setColumnWidth(c, 12 * 256);
        for (int c = COL_INDICADORES; c < COL_INDICADORES + 7; c++) sh.setColumnWidth(c, 12 * 256);
    }

    private void writeHeader(Sheet sh, String title, String disciplina, String docente,
                             String curso, String seccion, String turno, String especialidad,
                             Styles styles) {
        setCell(sh, 4, 1, title, styles.title);
        mergeSafely(sh, 4, 4, 1, 40);

        setCell(sh, 6, 1, "Disciplina: " + disciplina, styles.header);
        mergeSafely(sh, 6, 6, 1, 20);
        setCell(sh, 6, 22, "Docente: " + docente, styles.header);
        mergeSafely(sh, 6, 6, 22, 40);

        setCell(sh, 8, 1, "Curso: " + curso, styles.label);
        mergeSafely(sh, 8, 8, 1, 10);
        setCell(sh, 8, 12, "Sección: " + seccion, styles.label);
        mergeSafely(sh, 8, 8, 12, 16);
        setCell(sh, 8, 18, "Turno: " + turno, styles.label);
        mergeSafely(sh, 8, 8, 18, 20);
        setCell(sh, 8, 22, "Especialidad: " + especialidad, styles.label);
        mergeSafely(sh, 8, 8, 22, 40);

        // fila 12: encabezados de columnas
        setCell(sh, 12, COL_CAPACIDADES, "Capacidades", styles.columnHeader);
        mergeSafely(sh, 12, 12, COL_CAPACIDADES, COL_TEMAS - 1);
        setCell(sh, 12, COL_TEMAS, "Temas / Contenidos", styles.columnHeader);
        mergeSafely(sh, 12, 12, COL_TEMAS, COL_ACTIVIDADES - 1);
        setCell(sh, 12, COL_ACTIVIDADES, "Actividades", styles.columnHeader);
        mergeSafely(sh, 12, 12, COL_ACTIVIDADES, COL_INSTRUMENTOS - 1);
        setCell(sh, 12, COL_INSTRUMENTOS, "Instrumentos de Evaluación", styles.columnHeader);
        mergeSafely(sh, 12, 12, COL_INSTRUMENTOS, COL_INDICADORES - 1);
        setCell(sh, 12, COL_INDICADORES, "Indicadores", styles.columnHeader);
        mergeSafely(sh, 12, 12, COL_INDICADORES, COL_INDICADORES + 6);
    }

    private void writeBlocks(Sheet sh, int bloques, Styles styles) {
        for (int i = 0; i < bloques; i++) {
            int row = BLOCK_START_ROW + i * BLOCK_STRIDE;
            setCell(sh, row, 1, "B" + (i + 1), styles.blockLabel);
            mergeSafely(sh, row, row + 4, 1, 1);
            fillBordered(sh, row, COL_CAPACIDADES, row + 4, COL_TEMAS - 1, styles.cell);
            fillBordered(sh, row, COL_TEMAS, row + 4, COL_ACTIVIDADES - 1, styles.cell);
            fillBordered(sh, row, COL_ACTIVIDADES, row + 4, COL_INSTRUMENTOS - 1, styles.cell);
            fillBordered(sh, row, COL_INSTRUMENTOS, row + 4, COL_INDICADORES - 1, styles.cell);
            // Indicadores: 3 sub-filas para conceptual/procedimental/actitudinal en (row, row+2, row+4)
            fillBordered(sh, row, COL_INDICADORES, row + 1, COL_INDICADORES + 6, styles.cell);
            fillBordered(sh, row + 2, COL_INDICADORES, row + 3, COL_INDICADORES + 6, styles.cell);
            fillBordered(sh, row + 4, COL_INDICADORES, row + 4, COL_INDICADORES + 6, styles.cell);
        }
    }

    private void fillBordered(Sheet sh, int r1, int c1, int r2, int c2, CellStyle style) {
        for (int r = r1; r <= r2; r++) {
            Row row = sh.getRow(r);
            if (row == null) row = sh.createRow(r);
            for (int c = c1; c <= c2; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) cell = row.createCell(c);
                if (cell.getCellStyle() == null || cell.getCellStyle().getIndex() == 0) {
                    cell.setCellStyle(style);
                }
            }
        }
        mergeSafely(sh, r1, r2, c1, c2);
    }

    private void mergeSafely(Sheet sh, int r1, int r2, int c1, int c2) {
        if (r1 == r2 && c1 == c2) return;
        try { sh.addMergedRegion(new CellRangeAddress(r1, r2, c1, c2)); }
        catch (IllegalStateException ignored) { }
    }

    private void writeSignatureArea(Workbook wb, Sheet sh, int row, byte[] firmaProfesor,
                                    byte[] firmaEvaluador, Styles styles) {
        setCell(sh, row, 1, "Firma del Profesor", styles.signatureLabel);
        mergeSafely(sh, row, row, 1, 15);
        setCell(sh, row, 22, "Firma del Evaluador", styles.signatureLabel);
        mergeSafely(sh, row, row, 22, 36);

        fillBordered(sh, row + 1, 1, row + 6, 15, styles.cell);
        fillBordered(sh, row + 1, 22, row + 6, 36, styles.cell);

        if (firmaProfesor != null) {
            insertPicture(wb, sh, firmaProfesor, row + 1, 2, row + 6, 14);
        }
        if (firmaEvaluador != null) {
            insertPicture(wb, sh, firmaEvaluador, row + 1, 23, row + 6, 35);
        }
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
                int signatureRow = startRow + bloques * stride + 2;
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

    // ---- Estilos ----

    private static final class Styles {
        final CellStyle title;
        final CellStyle header;
        final CellStyle label;
        final CellStyle columnHeader;
        final CellStyle cell;
        final CellStyle blockLabel;
        final CellStyle signatureLabel;

        Styles(Workbook wb) {
            this.title = buildTitle(wb);
            this.header = buildHeader(wb);
            this.label = buildLabel(wb);
            this.columnHeader = buildColumnHeader(wb);
            this.cell = buildCell(wb);
            this.blockLabel = buildBlockLabel(wb);
            this.signatureLabel = buildSignatureLabel(wb);
        }

        private CellStyle buildTitle(Workbook wb) {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 14);
            s.setFont(f); s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }

        private CellStyle buildHeader(Workbook wb) {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont(); f.setBold(true);
            s.setFont(f); s.setAlignment(HorizontalAlignment.LEFT);
            return s;
        }

        private CellStyle buildLabel(Workbook wb) {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont(); f.setBold(true);
            s.setFont(f);
            return s;
        }

        private CellStyle buildColumnHeader(Workbook wb) {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
            s.setFont(f);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            applyBorders(s);
            return s;
        }

        private CellStyle buildCell(Workbook wb) {
            CellStyle s = wb.createCellStyle();
            s.setAlignment(HorizontalAlignment.LEFT);
            s.setVerticalAlignment(VerticalAlignment.TOP);
            s.setWrapText(true);
            applyBorders(s);
            return s;
        }

        private CellStyle buildBlockLabel(Workbook wb) {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont(); f.setBold(true);
            s.setFont(f);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            applyBorders(s);
            return s;
        }

        private CellStyle buildSignatureLabel(Workbook wb) {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont(); f.setBold(true); f.setItalic(true);
            s.setFont(f);
            s.setAlignment(HorizontalAlignment.CENTER);
            return s;
        }

        private static void applyBorders(CellStyle s) {
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
        }
    }
}
