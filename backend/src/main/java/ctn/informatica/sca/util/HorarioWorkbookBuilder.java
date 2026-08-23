package ctn.informatica.sca.util;

import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class HorarioWorkbookBuilder {

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};

    public XSSFWorkbook build(Curso curso, List<HoraCatedra> horas, List<HorarioSlot> slots) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Horario");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        CellStyle gridStyle = workbook.createCellStyle();
        gridStyle.setBorderTop(BorderStyle.THIN);
        gridStyle.setBorderBottom(BorderStyle.THIN);
        gridStyle.setBorderLeft(BorderStyle.THIN);
        gridStyle.setBorderRight(BorderStyle.THIN);
        gridStyle.setAlignment(HorizontalAlignment.CENTER);
        gridStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        gridStyle.setWrapText(true);

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue((curso == null ? "Horario" : "Horario - " + curso.getEspecialidad() + " " + curso.getNivel() + "° " + curso.getSeccion()));
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleCell.setCellStyle(styleWithFont(workbook, headerStyle, titleFont));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        int firstDataCol = 1;
        int diasUtilizados = 0;
        Map<Integer, List<HorarioSlot>> porDia = new LinkedHashMap<>();
        for (int dia = 1; dia <= 6; dia++) {
            porDia.put(dia, new ArrayList<>());
        }
        for (HorarioSlot slot : slots) {
            if (slot.getDiaSemana() >= 1 && slot.getDiaSemana() <= 6) {
                porDia.computeIfAbsent(slot.getDiaSemana(), key -> new ArrayList<>()).add(slot);
            }
        }
        for (int dia = 1; dia <= 6; dia++) {
            if (!porDia.getOrDefault(dia, List.of()).isEmpty()) {
                diasUtilizados++;
            }
        }
        if (diasUtilizados == 0) {
            diasUtilizados = Math.min(6, DIAS.length);
        }

        int startCol = 0;
        Row headerRow = sheet.createRow(2);
        Cell horaHeader = headerRow.createCell(startCol);
        horaHeader.setCellValue("Hora");
        horaHeader.setCellStyle(headerStyle);

        int usedDayCount = 0;
        for (int dia = 1; dia <= 6; dia++) {
            if (diasUtilizados == 0 || !porDia.getOrDefault(dia, List.of()).isEmpty()) {
                Cell cell = headerRow.createCell(startCol + usedDayCount + 1);
                cell.setCellValue(DIAS[dia - 1]);
                cell.setCellStyle(headerStyle);
                usedDayCount++;
            }
        }

        int rowIndex = 3;
        Map<Integer, HorarioSlot> slotByCell = new LinkedHashMap<>();
        for (HorarioSlot slot : slots) {
            slotByCell.put((slot.getDiaSemana() * 1000) + slot.getHoraCatedraId(), slot);
        }

        for (HoraCatedra hora : horas) {
            Row row = sheet.createRow(rowIndex++);
            Cell horaCell = row.createCell(startCol);
            horaCell.setCellValue((hora.getNumero() == 0 ? "" : String.valueOf(hora.getNumero())) + (hora.getEtiqueta() != null && !hora.getEtiqueta().isBlank() ? " - " + hora.getEtiqueta() : ""));
            horaCell.setCellStyle(gridStyle);

            int colIndex = 1;
            for (int dia = 1; dia <= 6; dia++) {
                if (diasUtilizados == 0 || !porDia.getOrDefault(dia, List.of()).isEmpty()) {
                    Cell cell = row.createCell(colIndex++);
                    cell.setCellStyle(gridStyle);
                    HorarioSlot slot = slotByCell.get((dia * 1000) + hora.getId());
                    if (slot != null) {
                        String materia = slot.getMateriaNombre() == null ? "" : slot.getMateriaNombre();
                        String profesor = slot.getProfesorNombre() == null ? "" : slot.getProfesorNombre();
                        String sala = slot.getSala() == null || slot.getSala().isBlank() ? "" : slot.getSala();
                        StringBuilder text = new StringBuilder();
                        if (!materia.isBlank()) {
                            text.append(materia);
                        }
                        if (!profesor.isBlank()) {
                            if (!text.isEmpty()) {
                                text.append("\n");
                            }
                            text.append(profesor);
                        }
                        if (!sala.isBlank()) {
                            if (!text.isEmpty()) {
                                text.append("\n");
                            }
                            text.append(sala);
                        }
                        cell.setCellValue(text.length() == 0 ? "" : text.toString());
                    }
                }
            }
        }

        sheet.setColumnWidth(0, 2200);
        for (int i = 1; i <= Math.max(6, usedDayCount + 1); i++) {
            sheet.setColumnWidth(i, 20000);
        }

        workbook.setActiveSheet(0);
        return workbook;
    }

    private CellStyle styleWithFont(Workbook workbook, CellStyle baseStyle, Font font) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        style.setFont(font);
        return style;
    }
}
