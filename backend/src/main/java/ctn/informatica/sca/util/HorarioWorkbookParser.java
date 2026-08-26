package ctn.informatica.sca.util;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.HoraCatedraDao;
import ctn.informatica.sca.dto.HorarioImportRowDto;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.HoraCatedra;
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
    private static final Map<String, Integer> DAYS = Map.of("lunes", 1, "martes", 2, "miércoles", 3,
            "miercoles", 3, "jueves", 4, "viernes", 5, "sábado", 6, "sabado", 6);

    public List<HorarioImportRowDto> parse(InputStream input, int cursoId) throws IOException, java.sql.SQLException {
        Map<String, Asignacion> assignments = new HashMap<>();
        for (Asignacion assignment : new AsignacionDao().findAll()) {
            if (assignment.getCursoId() == cursoId) {
                assignments.putIfAbsent(normalize(assignment.getMateriaNombre()), assignment);
            }
        }
        Map<String, HoraCatedra> hours = new HashMap<>();
        for (HoraCatedra hour : new HoraCatedraDao().findAll()) {
            hours.put(key(hour.getHoraInicio(), hour.getHoraFin()), hour);
        }

        List<HorarioImportRowDto> rows = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheet("Horario");
            if (sheet == null) throw new IllegalArgumentException("El archivo no contiene la hoja Horario");
            DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                if (!"Hora".equals(cell(row, 0, formatter))) continue;
                Map<Integer, Integer> columns = new HashMap<>();
                for (int col = 1; col <= row.getLastCellNum(); col++) {
                    Integer day = DAYS.get(normalize(cell(row, col, formatter)));
                    if (day != null) columns.put(col, day);
                }
                for (int rowIndex = row.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row data = sheet.getRow(rowIndex);
                    String label = cell(data, 0, formatter);
                    if (label.isBlank()) continue;
                    if ("RECESO".equalsIgnoreCase(label)) continue;
                    if ("Salas".equalsIgnoreCase(label) || "Hora".equals(label)) break;
                    Matcher matcher = RANGE.matcher(label);
                    if (!matcher.find()) continue;
                    String[] range = matcher.group().replace(" ", "").split("-");
                    HoraCatedra hour = hours.get(key(LocalTime.parse(range[0]), LocalTime.parse(range[1])));
                    if (hour == null) continue;
                    for (Map.Entry<Integer, Integer> column : columns.entrySet()) {
                        String content = cell(data, column.getKey(), formatter).trim();
                        if (content.isBlank()) continue;
                        String[] lines = content.split("\\R", 2);
                        String subject = lines[0].trim();
                        String professor = lines.length > 1 ? lines[1].trim() : "";
                        Asignacion assignment = assignments.get(normalize(subject));
                        if (assignment == null) {
                            rows.add(new HorarioImportRowDto(column.getValue(), hour.getId(), hour.getEtiqueta(), subject,
                                    professor, null, "sin_asignacion", "No coincide con una asignación del curso."));
                        } else {
                            rows.add(new HorarioImportRowDto(column.getValue(), hour.getId(), hour.getEtiqueta(), subject,
                                    professor, assignment.getId(), "ok", null));
                        }
                    }
                }
            }
        }
        return rows;
    }

    private String cell(Row row, int column, DataFormatter formatter) {
        return row == null || row.getCell(column) == null ? "" : formatter.formatCellValue(row.getCell(column));
    }

    private String key(LocalTime start, LocalTime end) {
        return start + "|" + end;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
