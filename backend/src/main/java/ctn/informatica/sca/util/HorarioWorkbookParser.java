package ctn.informatica.sca.util;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.CursoBaseDao;
import ctn.informatica.sca.dao.HoraCatedraDao;
import ctn.informatica.sca.dao.SalaDao;
import ctn.informatica.sca.dto.HorarioImportRowDto;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.Sala;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class HorarioWorkbookParser {
    private static final Pattern RANGE = Pattern.compile("\\b\\d{1,2}:\\d{2}\\s*-\\s*\\d{1,2}:\\d{2}\\b");
    private static final Map<String, Integer> DAYS = Map.of("lunes", 1, "martes", 2, "miércoles", 3, "miercoles", 3, "jueves", 4, "viernes", 5, "sábado", 6, "sabado", 6);
    private record ParsedCell(int day, HoraCatedra hour, String subject, String professor, String sala) {}

    public List<HorarioImportRowDto> parse(InputStream input, int cursoId) throws IOException, java.sql.SQLException {
        Map<String, Asignacion> assignments = new HashMap<>();
        for (Asignacion assignment : new AsignacionDao().findAll()) if (assignment.getCursoId() == cursoId) assignments.putIfAbsent(normalize(assignment.getMateriaNombre()), assignment);
        Map<String, HoraCatedra> hours = new HashMap<>();
        for (HoraCatedra hour : new HoraCatedraDao().findAll()) hours.put(key(hour.getHoraInicio(), hour.getHoraFin()), hour);
        int specialtyId = new CursoBaseDao().findEspecialidadId(cursoId);
        Map<String, Sala> salas = new HashMap<>();
        for (Sala sala : new SalaDao().findByEspecialidad(specialtyId)) salas.put(normalize(sala.getNombre()), sala);

        List<HorarioImportRowDto> result = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheet("Horario");
            if (sheet == null) throw new IllegalArgumentException("El archivo no contiene la hoja Horario");
            DataFormatter formatter = new DataFormatter();
            for (Row header : sheet) {
                if (!"Hora".equals(cell(header, 0, formatter))) continue;
                Map<Integer, Integer> columns = dayColumns(header, formatter);
                List<ParsedCell> cells = new ArrayList<>();
                Map<Integer, String> blockRooms = new HashMap<>();
                for (int rowIndex = header.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    String label = cell(row, 0, formatter);
                    if (label.isBlank() || "RECESO".equalsIgnoreCase(label)) continue;
                    if ("Salas".equalsIgnoreCase(label)) {
                        for (Map.Entry<Integer, Integer> column : columns.entrySet()) blockRooms.put(column.getValue(), cell(row, column.getKey(), formatter).trim());
                        break;
                    }
                    if ("Hora".equals(label)) break;
                    Matcher matcher = RANGE.matcher(label);
                    if (!matcher.find()) continue;
                    String[] range = matcher.group().replace(" ", "").split("-");
                    HoraCatedra hour = hours.get(key(LocalTime.parse(range[0]), LocalTime.parse(range[1])));
                    if (hour == null) continue;
                    for (Map.Entry<Integer, Integer> column : columns.entrySet()) {
                        String text = cell(row, column.getKey(), formatter).trim();
                        if (text.isBlank()) continue;
                        String[] lines = text.split("\\R");
                        String sala = lines.length > 2 && lines[2].toLowerCase(Locale.ROOT).startsWith("sala:") ? lines[2].substring(5).trim() : blockRooms.get(column.getValue());
                        cells.add(new ParsedCell(column.getValue(), hour, lines[0].trim(), lines.length > 1 ? lines[1].trim() : "", sala));
                    }
                }
                for (ParsedCell parsed : cells) result.add(toDto(parsed, assignments, salas));
            }
        }
        return result;
    }

    private HorarioImportRowDto toDto(ParsedCell parsed, Map<String, Asignacion> assignments, Map<String, Sala> salas) {
        Asignacion assignment = assignments.get(normalize(parsed.subject()));
        Sala sala = parsed.sala() == null || parsed.sala().isBlank() || parsed.sala().contains("/") ? null : salas.get(normalize(parsed.sala()));
        String state = assignment == null ? "sin_asignacion" : (parsed.sala() != null && !parsed.sala().isBlank() && sala == null ? "sala_no_reconocida" : "ok");
        String detail = assignment == null ? "No coincide con una asignación del curso." : ("sala_no_reconocida".equals(state) ? "La sala no coincide con el catálogo visible para la especialidad." : null);
        return new HorarioImportRowDto(parsed.day(), parsed.hour().getId(), parsed.hour().getEtiqueta(), parsed.subject(), parsed.professor(), assignment == null ? null : assignment.getId(), sala == null ? null : sala.getId(), sala == null ? parsed.sala() : sala.getNombre(), state, detail);
    }

    private Map<Integer, Integer> dayColumns(Row row, DataFormatter formatter) {
        Map<Integer, Integer> columns = new HashMap<>();
        for (int col = 1; col <= row.getLastCellNum(); col++) { Integer day = DAYS.get(normalize(cell(row, col, formatter))); if (day != null) columns.put(col, day); }
        return columns;
    }
    private String cell(Row row, int column, DataFormatter formatter) { return row == null || row.getCell(column) == null ? "" : formatter.formatCellValue(row.getCell(column)); }
    private String key(LocalTime start, LocalTime end) { return start + "|" + end; }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
