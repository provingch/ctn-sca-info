package ctn.informatica.sca.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.HorarioSlotDao;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.HorarioSlot;
import ctn.informatica.sca.util.AcademicPeriod;

@Service
public class PlanCurricularTemplateBuilder {

    @Autowired
    private AsignacionDao asignacionDao;

    @Autowired
    private HorarioSlotDao horarioSlotDao;

    public byte[] buildForAsignacion(int asignacionId) throws Exception {
        Asignacion a = asignacionDao.findById(asignacionId);
        if (a == null) throw new IllegalArgumentException("Asignación no encontrada");

        try (InputStream in = getClass().getResourceAsStream("/plan-curricular-plantilla.xlsx"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Workbook baseWorkbook = WorkbookFactory.create(in);
            Workbook wb = baseWorkbook;
            try {
                if (AcademicPeriod.currentEtapa() == 2) {
                    wb = createEtapaDosWorkbook(baseWorkbook);
                }

                String title = String.format("PLAN DE DESARROLLO CURRICULAR ETAPA:_%s_%d", AcademicPeriod.currentEtapa() + "°", AcademicPeriod.current());
                String disciplina = a.getMateriaNombre() == null ? "" : a.getMateriaNombre();
                String docente = a.getProfesorNombre() == null ? "" : a.getProfesorNombre();
                String curso = a.getCursoOrdinal() == null ? "" : a.getCursoOrdinal();
                String seccion = a.getCursoSeccion() == null ? "" : a.getCursoSeccion();
                String especialidad = a.getEspecialidad() == null ? "" : a.getEspecialidad();
                String turno = "";
                try {
                    java.util.List<HorarioSlot> slots = horarioSlotDao.findByAsignacion(asignacionId);
                    if (slots == null || slots.isEmpty()) {
                        turno = "sin horario cargado";
                    } else {
                        boolean hasMorning = false;
                        boolean hasAfternoon = false;
                        for (HorarioSlot s : slots) {
                            String hi = s.getHoraInicio();
                            if (hi == null || hi.isBlank()) continue;
                            String[] parts = hi.split(":");
                            int hour = 0;
                            try { hour = Integer.parseInt(parts[0]); } catch (Exception e) { }
                            if (hour < 12) hasMorning = true; else hasAfternoon = true;
                        }
                        if (hasMorning && hasAfternoon) turno = "Mañana y Tarde";
                        else if (hasMorning) turno = "Mañana";
                        else if (hasAfternoon) turno = "Tarde";
                        else turno = "sin horario cargado";
                    }
                } catch (Exception e) {
                    turno = "";
                }

                String[] sheets = AcademicPeriod.currentEtapa() == 2
                        ? new String[] {"Julio", "Agosto", "Septiembre", "Octubre", "Noviembre"}
                        : new String[] {"Marzo", "Abril", "Mayo", "Junio"};
                for (String shName : sheets) {
                    Sheet sh = wb.getSheet(shName);
                    if (sh == null) continue;
                    setCell(sh, 4, 1, title);
                    setCell(sh, 6, 1, "Disciplina: " + disciplina);
                    setCell(sh, 6, 22, "Docente: " + docente);
                    setCell(sh, 8, 1, "Curso: " + curso);
                    setCell(sh, 8, 12, "Sección: " + seccion);
                    setCell(sh, 8, 18, "Turno: " + turno);
                    setCell(sh, 8, 22, "Especialidad: " + especialidad);
                }

                wb.write(out);
                return out.toByteArray();
            } finally {
                if (wb != baseWorkbook) {
                    wb.close();
                }
                baseWorkbook.close();
            }
        }
    }

    private Workbook createEtapaDosWorkbook(Workbook source) {
        Workbook target = new XSSFWorkbook();
        Sheet sourceSheet = source.getSheet("Marzo");
        if (sourceSheet == null) {
            sourceSheet = source.getSheetAt(0);
        }
        if (sourceSheet == null) {
            return target;
        }
        String[] monthNames = new String[] {"Julio", "Agosto", "Septiembre", "Octubre", "Noviembre"};
        for (String monthName : monthNames) {
            Sheet cloned = target.createSheet(monthName);
            cloneSheetContent(sourceSheet, cloned);
        }
        return target;
    }

    private void cloneSheetContent(Sheet source, Sheet target) {
        for (int rowIndex = 0; rowIndex <= source.getLastRowNum(); rowIndex++) {
            Row sourceRow = source.getRow(rowIndex);
            if (sourceRow == null) continue;
            Row targetRow = target.createRow(rowIndex);
            for (int colIndex = 0; colIndex <= sourceRow.getLastCellNum(); colIndex++) {
                Cell sourceCell = sourceRow.getCell(colIndex);
                if (sourceCell == null) continue;
                Cell targetCell = targetRow.createCell(colIndex);
                copyCell(sourceCell, targetCell);
            }
        }
    }

    private void copyCell(Cell source, Cell target) {
        switch (source.getCellType()) {
            case STRING -> target.setCellValue(source.getStringCellValue());
            case NUMERIC -> target.setCellValue(source.getNumericCellValue());
            case BOOLEAN -> target.setCellValue(source.getBooleanCellValue());
            case FORMULA -> target.setCellFormula(source.getCellFormula());
            case BLANK -> target.setCellType(CellType.BLANK);
            default -> target.setCellValue(source.toString());
        }
    }

    private void setCell(Sheet sh, int rowIndex, int colIndex, String value) {
        Row r = sh.getRow(rowIndex);
        if (r == null) r = sh.createRow(rowIndex);
        Cell c = r.getCell(colIndex);
        if (c == null) c = r.createCell(colIndex);
        c.setCellValue(value == null ? "" : value);
    }
}
