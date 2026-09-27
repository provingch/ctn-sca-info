package ctn.informatica.sca.integration.gema.service;

import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.integration.gema.dao.GemaTareaDao;
import ctn.informatica.sca.integration.gema.dto.CreateTareaRequest;
import ctn.informatica.sca.integration.gema.dto.CreateTareaResult;
import ctn.informatica.sca.integration.gema.dto.InstrumentoDto;
import ctn.informatica.sca.integration.gema.dto.TareaDto;
import ctn.informatica.sca.integration.gema.dto.UpdateTareaRequest;
import ctn.informatica.sca.model.Instrumento;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Tarea;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fase 2: lectura y creación/actualización de tareas para GEMA. La idempotencia y el INSERT con
 * {@code gema_tarea_id} van por {@link GemaTareaDao} (nuevo); el UPDATE reutiliza
 * {@link TareaDao#update(Tarea)} tal cual — no toca {@code gema_tarea_id} así que la preserva.
 */
@Service
public class GemaTareaService {

    private final PlanillaDao planillaDao;
    private final TareaDao tareaDao;
    private final InstrumentoDao instrumentoDao;
    private final GemaTareaDao gemaTareaDao;

    public GemaTareaService(PlanillaDao planillaDao, TareaDao tareaDao, InstrumentoDao instrumentoDao, GemaTareaDao gemaTareaDao) {
        this.planillaDao = planillaDao;
        this.tareaDao = tareaDao;
        this.instrumentoDao = instrumentoDao;
        this.gemaTareaDao = gemaTareaDao;
    }

    public List<TareaDto> listTareas(int planillaId) {
        requirePlanilla(planillaId);
        try {
            Map<Integer, String> instrumentos = instrumentoNombresPorId();
            Map<Integer, String> gemaIds = gemaTareaDao.findGemaTareaIdsByPlanilla(planillaId);
            return tareaDao.consultarTarea(planillaId).stream()
                    .map(t -> toDto(t, instrumentos, gemaIds))
                    .toList();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar las tareas de la planilla", ex);
        }
    }

    public List<InstrumentoDto> listInstrumentos() {
        try {
            return instrumentoDao.findAll().stream().map(i -> new InstrumentoDto(i.getId(), i.getNombre())).toList();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el catálogo de instrumentos", ex);
        }
    }

    public CreateTareaResult createTarea(int planillaId, CreateTareaRequest request) {
        validate(request);
        Planilla planilla = requirePlanilla(planillaId);
        requireEtapaAbierta(planilla);
        requireFechaEnEtapa(planilla, request.fechaLimite() != null ? request.fechaLimite() : request.fecha());

        try {
            GemaTareaDao.TareaRef existing = gemaTareaDao.findByGemaTareaId(request.gemaTareaId());
            Map<Integer, String> instrumentos = instrumentoNombresPorId();
            if (existing != null) {
                if (existing.planillaId() != planillaId) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya existe una tarea de GEMA con ese gemaTareaId en otra planilla");
                }
                Tarea tarea = tareaDao.findById(existing.id());
                applyRequest(tarea, request.titulo(), request.fecha(), request.fechaInicio(), request.fechaLimite(),
                        request.total(), request.instrumentoId());
                tareaDao.update(tarea);
                return new CreateTareaResult(toDto(tarea, instrumentos, Map.of(tarea.getId(), request.gemaTareaId())), false);
            }

            int nuevoId = gemaTareaDao.insert(planillaId, request.instrumentoId(), request.fecha(), request.fechaInicio(),
                    request.fechaLimite(), request.total(), request.titulo().trim(), request.gemaTareaId().trim());
            Tarea creada = tareaDao.findById(nuevoId);
            return new CreateTareaResult(toDto(creada, instrumentos, Map.of(nuevoId, request.gemaTareaId())), true);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo guardar la tarea de GEMA", ex);
        }
    }

    public TareaDto updateTarea(int planillaId, String gemaTareaId, UpdateTareaRequest request) {
        validateUpdate(request);
        try {
            GemaTareaDao.TareaRef existing = gemaTareaDao.findByGemaTareaId(gemaTareaId);
            if (existing == null || existing.planillaId() != planillaId) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay una tarea de GEMA con ese id en esa planilla");
            }
            Planilla planilla = requirePlanilla(planillaId);
            requireEtapaAbierta(planilla);
            requireFechaEnEtapa(planilla, request.fechaLimite() != null ? request.fechaLimite() : request.fecha());

            Tarea tarea = tareaDao.findById(existing.id());
            applyRequest(tarea, request.titulo(), request.fecha(), request.fechaInicio(), request.fechaLimite(),
                    request.total(), request.instrumentoId());
            tareaDao.update(tarea);
            return toDto(tarea, instrumentoNombresPorId(), Map.of(tarea.getId(), gemaTareaId));
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo actualizar la tarea de GEMA", ex);
        }
    }

    private Planilla requirePlanilla(int planillaId) {
        try {
            Planilla planilla = planillaDao.findById(planillaId);
            if (planilla == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planilla no encontrada");
            }
            return planilla;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar la planilla", ex);
        }
    }

    private static void requireEtapaAbierta(Planilla planilla) {
        boolean confirmada = planilla.getEtapaIndex() == 2 ? planilla.isEtapa2Confirmada() : planilla.isEtapa1Confirmada();
        if (confirmada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La etapa de esta planilla ya está confirmada");
        }
    }

    /** Misma regla que {@code PlanillaProcesoWorkbookBuilder#filterTasksByEtapa}: no se copia una nueva. */
    private static void requireFechaEnEtapa(Planilla planilla, LocalDate fecha) {
        if (fecha != null && planilla.sugerirEtapaParaTarea(fecha) != planilla.getEtapaIndex()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La fecha límite de la tarea no pertenece a una etapa activa de la planilla");
        }
    }

    private static void applyRequest(Tarea tarea, String titulo, LocalDate fecha, LocalDate fechaInicio,
            LocalDate fechaLimite, int total, int instrumentoId) {
        tarea.setTitulo(titulo.trim());
        tarea.setFecha(fecha);
        tarea.setFechaInicio(fechaInicio);
        tarea.setFechaLimite(fechaLimite);
        tarea.setTotal(total);
        tarea.setInstrumentoId(instrumentoId);
    }

    private void validate(CreateTareaRequest r) {
        if (r == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requerido");
        if (r.gemaTareaId() == null || r.gemaTareaId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gemaTareaId requerido");
        }
        validateCommon(r.titulo(), r.fecha(), r.total(), r.instrumentoId());
    }

    private void validateUpdate(UpdateTareaRequest r) {
        if (r == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requerido");
        validateCommon(r.titulo(), r.fecha(), r.total(), r.instrumentoId());
    }

    private void validateCommon(String titulo, LocalDate fecha, int total, int instrumentoId) {
        if (titulo == null || titulo.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "titulo requerido");
        if (fecha == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fecha requerida");
        if (total <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "total debe ser mayor que 0");
        if (instrumentoId <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instrumentoId inválido");
    }

    private Map<Integer, String> instrumentoNombresPorId() throws SQLException {
        Map<Integer, String> out = new java.util.HashMap<>();
        for (Instrumento i : instrumentoDao.findAll()) {
            out.put(i.getId(), i.getNombre());
        }
        return out;
    }

    private static TareaDto toDto(Tarea t, Map<Integer, String> instrumentos, Map<Integer, String> gemaIds) {
        String origen = t.getGoogleCourseworkId() != null && !t.getGoogleCourseworkId().isBlank()
                ? "CLASSROOM"
                : gemaIds.containsKey(t.getId()) ? "GEMA" : "SCA";
        return new TareaDto(t.getId(), t.getTitulo(), t.getFecha(), t.getFechaInicio(), t.getFechaLimite(),
                t.getTotal(), t.getInstrumentoId(), instrumentos.get(t.getInstrumentoId()), origen);
    }
}
