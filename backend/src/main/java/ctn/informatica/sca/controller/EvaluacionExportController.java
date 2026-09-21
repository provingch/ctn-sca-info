package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.StudentRowDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import ctn.informatica.sca.util.PlanillaProcesoWorkbookBuilder;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/evaluacion")
@PreAuthorize("hasRole('LEVEL_2')")
public class EvaluacionExportController {
    @GetMapping("/export")
        public void export(
            @RequestParam int cursoId,
            @RequestParam String etapa,
            @RequestParam int periodo,
            @RequestParam(required = false) Integer materiaId,
            Authentication authentication,
            HttpServletResponse response) {
        ApiAuth.requireUserId(authentication);
        try {
            PlanillaDao planillaDao = new PlanillaDao();
            EvaluacionPlanillaSelector.Selection selection = EvaluacionPlanillaSelector.select(
                    planillaDao, new CursoDao(), new EspecialidadDao(), cursoId, etapa, periodo, materiaId);
            Curso curso = selection.curso();
            List<PlanillaProcesoWorkbookBuilder.PlanillaSheetData> sheets = new ArrayList<>();
            for (EvaluacionPlanillaSelector.Match match : selection.matches()) {
                Planilla planilla = match.planilla();
                List<Tarea> tareas = PlanillaProcesoWorkbookBuilder.filterTasksByEtapa(new TareaDao().consultarTarea(planilla.getId()), planilla);
                Map<Integer, Integer> maxima = new HashMap<>(); int total = 0;
                for (Tarea tarea : tareas) { maxima.put(tarea.getId(), tarea.getTotal()); total += tarea.getTotal(); }
                planilla.computeGradeRanges(total);
                List<StudentRow> rows = new StudentRowDao().loadRowsForPlanilla(planilla, maxima, total);
                Profesor profesor = new ProfesorDao().findById(planilla.getProfesorId());
                sheets.add(new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                        planilla,
                        curso,
                        match.materiaNombre(),
                        profesor == null ? "" : profesor.getFullName(),
                        "",
                        tareas,
                        rows,
                        firstStageGrades(planillaDao, planilla),
                        profesor == null ? null : profesor.getFirmaImagen()
                ));
            }
            if (sheets.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay planillas para los filtros seleccionados");
            String base = "Planillas_" + curso.getEspecialidad().replaceAll("[^A-Za-z0-9_-]", "_") + "_" + curso.getNivel() + curso.getSeccion() + "_" + periodo;
            if (materiaId != null && materiaId.intValue() > 0 && sheets.size() == 1) {
                String materiaName = sheets.get(0).disciplina();
                if (materiaName != null && !materiaName.isBlank()) {
                    base = base + "_" + materiaName.replaceAll("[^A-Za-z0-9_-]", "_");
                }
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(base + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
            try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildCourseWorkbook(sheets)) { workbook.write(response.getOutputStream()); }
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el archivo", ex); }
    }

    private Map<Integer, Integer> firstStageGrades(PlanillaDao dao, Planilla current) throws Exception {
        if (current.getEtapaIndex() != 2) return Map.of();
        Planilla first = dao.findByCompositeKey(current.getCursoId(), current.getMateriaId(), 1);
        if (first == null) return Map.of();
        List<Tarea> tareas = PlanillaProcesoWorkbookBuilder.filterTasksByEtapa(new TareaDao().consultarTarea(first.getId()), first);
        Map<Integer, Integer> maxima = new HashMap<>(); int total = 0;
        for (Tarea tarea : tareas) { maxima.put(tarea.getId(), tarea.getTotal()); total += tarea.getTotal(); }
        first.computeGradeRanges(total);
        Map<Integer, Integer> result = new HashMap<>();
        for (StudentRow row : new StudentRowDao().loadRowsForPlanilla(first, maxima, total)) result.put(row.getAlumnoId(), first.getNotaForSum(row.getTotal()));
        return result;
    }
}
