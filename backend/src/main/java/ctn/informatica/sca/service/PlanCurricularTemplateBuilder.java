package ctn.informatica.sca.service;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.HorarioSlotDao;
import ctn.informatica.sca.model.HorarioSlot;
import ctn.informatica.sca.model.Asignacion;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import ctn.informatica.sca.util.AcademicPeriod;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PlanCurricularTemplateBuilder {

    @Autowired
    private AsignacionDao asignacionDao;

    @Autowired
    private HorarioSlotDao horarioSlotDao;

    public byte[] buildForAsignacion(int asignacionId) throws Exception {
        Asignacion a = asignacionDao.findById(asignacionId);
        if (a == null) throw new IllegalArgumentException("Asignación no encontrada");

        try (InputStream in = getClass().getResourceAsStream("/plan-curricular-plantilla.xlsx"); Workbook wb = WorkbookFactory.create(in); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String title = String.format("PLAN DE DESARROLLO CURRICULAR ETAPA:_%s_%d", AcademicPeriod.currentEtapa() + "°", AcademicPeriod.current());
            String disciplina = a.getMateriaNombre() == null ? "" : a.getMateriaNombre();
            String docente = a.getProfesorNombre() == null ? "" : a.getProfesorNombre();
            String curso = a.getCursoDescripcion() == null ? "" : a.getCursoDescripcion();
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

            String[] sheets = new String[]{"Marzo","Abril","Mayo","Junio"};
            for (String shName : sheets) {
                Sheet sh = wb.getSheet(shName);
                if (sh == null) continue;
                // B5 (4,1)
                setCell(sh,4,1,title);
                // B7 (6,1)
                setCell(sh,6,1,"Disciplina: " + disciplina);
                // W7 (6,22)
                setCell(sh,6,22,"Docente: " + docente);
                // B9 (8,1)
                setCell(sh,8,1,"Curso: " + curso);
                // M9 (8,12)
                setCell(sh,8,12,"Sección: " + seccion);
                // S9 (8,18)
                setCell(sh,8,18,"Turno: " + turno);
                // W9 (8,22)
                setCell(sh,8,22,"Especialidad: " + especialidad);
            }

            wb.write(out);
            return out.toByteArray();
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
