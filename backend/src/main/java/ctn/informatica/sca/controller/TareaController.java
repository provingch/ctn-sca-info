package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Tarea;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class TareaController {

    @GetMapping("/planillas/{planillaId}/tareas")
    public List<TareaResponse> listByPlanilla(@PathVariable int planillaId, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            Planilla planilla = requireOwnedPlanilla(planillaId, userId);
            List<Tarea> tareas = filterTasksByEtapa(new TareaDao().consultarTarea(planillaId), planilla.getEtapaIndex());
            List<TareaResponse> response = new ArrayList<>();
            for (Tarea tarea : tareas) {
                response.add(toResponse(tarea, false, null));
            }
            return response;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar tareas", ex);
        }
    }

    @GetMapping("/tareas/{tareaId}")
    public TareaResponse getById(@PathVariable int tareaId, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            Tarea tarea = requireTarea(tareaId);
            requireOwnedPlanilla(tarea.getPlanillaId(), userId);
            return toResponse(tarea, false, null);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar tarea", ex);
        }
    }

    @PostMapping("/planillas/{planillaId}/tareas")
    public ResponseEntity<TareaResponse> create(
            @PathVariable int planillaId,
            @RequestBody SaveTareaRequest request,
            Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        validateSaveRequest(request);
        try {
            requireOwnedPlanilla(planillaId, userId);
            Tarea tarea = new Tarea();
            tarea.setPlanillaId(planillaId);
            tarea.setInstrumentoId(request.instrumentoId());
            tarea.setFecha(request.fecha());
            tarea.setTotal(request.total());
            tarea.setTitulo(request.titulo().trim());

            new TareaDao().insertarTarea(tarea);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tarea, false, null));
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear tarea", ex);
        }
    }

    @PutMapping("/tareas/{tareaId}")
    public TareaResponse update(
            @PathVariable int tareaId,
            @RequestBody SaveTareaRequest request,
            Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        validateSaveRequest(request);

        try {
            Tarea existing = requireTarea(tareaId);
            requireOwnedPlanilla(existing.getPlanillaId(), userId);

            existing.setInstrumentoId(request.instrumentoId());
            existing.setFecha(request.fecha());
            existing.setTotal(request.total());
            existing.setTitulo(request.titulo().trim());

            boolean gradesCleared = new TareaDao().update(existing);
            String warning = gradesCleared
                    ? "Total modificado: se eliminaron las calificaciones anteriores de esta tarea"
                    : null;
            return toResponse(existing, gradesCleared, warning);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar tarea", ex);
        }
    }

    @DeleteMapping("/tareas/{tareaId}")
    public ResponseEntity<Void> delete(@PathVariable int tareaId, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            Tarea tarea = requireTarea(tareaId);
            requireOwnedPlanilla(tarea.getPlanillaId(), userId);
            new TareaDao().delete(tareaId);
            return ResponseEntity.noContent().build();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al eliminar tarea", ex);
        }
    }

    private void validateSaveRequest(SaveTareaRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requerido");
        }
        if (request.instrumentoId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instrumentoId inválido");
        }
        if (request.fecha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fecha requerida");
        }
        if (request.total() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "total debe ser mayor que 0");
        }
        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "titulo requerido");
        }
    }

    private Tarea requireTarea(int tareaId) throws SQLException {
        Tarea tarea = new TareaDao().findById(tareaId);
        if (tarea == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
        }
        return tarea;
    }

    private Planilla requireOwnedPlanilla(int planillaId, int userId) throws SQLException {
        Planilla planilla = new PlanillaDao().findById(planillaId);
        if (planilla == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planilla no encontrada");
        }
        if (planilla.getProfesorId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta planilla");
        }
        return planilla;
    }

    private List<Tarea> filterTasksByEtapa(List<Tarea> tareas, int planillaEtapaIndex) {
        if (tareas == null || tareas.isEmpty() || (planillaEtapaIndex != 1 && planillaEtapaIndex != 2)) {
            return tareas;
        }

        List<Tarea> filtered = new ArrayList<>();
        for (Tarea tarea : tareas) {
            if (Tarea.resolveEtapaIndexByPublicationDate(tarea.getFecha()) == planillaEtapaIndex) {
                filtered.add(tarea);
            }
        }
        return filtered;
    }

    private TareaResponse toResponse(Tarea tarea, boolean gradesCleared, String warning) {
        return new TareaResponse(
                tarea.getId(),
                tarea.getPlanillaId(),
                tarea.getInstrumentoId(),
                tarea.getFecha(),
                tarea.getTotal(),
                tarea.getTitulo(),
                tarea.getFechaInicio(),
                tarea.getFechaLimite(),
                tarea.getGoogleCourseworkId(),
                tarea.getGoogleCourseworkUrl(),
                gradesCleared,
                warning);
    }

    public record SaveTareaRequest(int instrumentoId, LocalDate fecha, int total, String titulo) {
    }

    public record TareaResponse(
            int id,
            int planillaId,
            int instrumentoId,
            LocalDate fecha,
            int total,
            String titulo,
            LocalDate fechaInicio,
            LocalDate fechaLimite,
            String googleCourseworkId,
            String googleCourseworkUrl,
            boolean gradesCleared,
            String warning) {
    }
}
