package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.ParentSummaryItem;
import ctn.informatica.sca.model.ParentTaskGrade;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/padre")
public class ParentController {
    private final PadreDao padreDao = new PadreDao();

    @GetMapping
    public ParentResponse getSummary(@RequestParam(required = false) Integer alumnoId, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            List<Alumno> children = padreDao.findChildrenByPadreId(userId);
            int selected = resolveSelected(children, alumnoId);
            List<ChildDto> childDtos = new ArrayList<>();
            List<ParentSummaryItem> allSummary = padreDao.findParentSummary(userId);
            for (Alumno child : children) {
                int points = 0;
                int possible = 0;
                for (ParentSummaryItem item : allSummary) {
                    if (item.getAlumnoId() != null && item.getAlumnoId() == child.getId()) {
                        points += item.getPuntos();
                        possible += item.getTotalPosible();
                    }
                }
                childDtos.add(new ChildDto(child.getId(), child.getNombre(), child.getApellido(), child.getEspecialidadNombre(), possible == 0 ? 0 : (int) Math.round(points * 100.0 / possible)));
            }

            List<SubjectDto> subjects = new ArrayList<>();
            if (selected > 0) {
                for (ParentSummaryItem item : allSummary) {
                    if (item.getAlumnoId() == null || item.getAlumnoId() != selected) continue;
                    List<TaskDto> tasks = new ArrayList<>();
                    for (ParentTaskGrade task : padreDao.findTaskGradesForAlumnoPlanilla(selected, item.getPlanillaId())) {
                        tasks.add(new TaskDto(task.getTareaId(), task.getTareaTitulo(), task.getFecha(), task.getPuntos(), task.getTotal(), task.getEstado()));
                    }
                    subjects.add(new SubjectDto(item.getPlanillaId(), item.getMateriaId(), item.getMateriaNombre(), item.getEtapa(), item.getPuntos(), item.getTotalPosible(), item.getPorcentaje(), item.getNota(), tasks));
                }
            }
            return new ParentResponse(childDtos, selected > 0 ? selected : null, subjects);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo cargar el resumen académico", ex);
        }
    }

    private int resolveSelected(List<Alumno> children, Integer requested) {
        if (children.isEmpty()) return 0;
        if (requested != null && children.stream().anyMatch(child -> child.getId() == requested)) return requested;
        return children.get(0).getId();
    }

    public record ParentResponse(List<ChildDto> hijos, Integer selectedAlumnoId, List<SubjectDto> materias) {}
    public record ChildDto(int id, String nombre, String apellido, String especialidad, int promedio) {}
    public record SubjectDto(int planillaId, int materiaId, String materia, String etapa, int puntos, int total, int porcentaje, int nota, List<TaskDto> tareas) {}
    public record TaskDto(int id, String titulo, LocalDate fecha, Integer puntos, int total, String estado) {}
}
