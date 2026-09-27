package ctn.informatica.sca.integration.gema.service;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.RegistroDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.integration.gema.dao.GemaGradeDao;
import ctn.informatica.sca.integration.gema.dao.GemaTareaDao;
import ctn.informatica.sca.integration.gema.dto.CalificacionDto;
import ctn.informatica.sca.integration.gema.dto.NotaInputDto;
import ctn.informatica.sca.integration.gema.dto.NotaRechazadaDto;
import ctn.informatica.sca.integration.gema.dto.SaveNotasRequest;
import ctn.informatica.sca.integration.gema.dto.SaveNotasResponse;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Tarea;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fase 3: calificaciones para GEMA, todo o nada — acordado con el usuario y con GEMA (no parcial).
 * Solo sobre tareas de origen GEMA (tienen {@code gema_tarea_id}); GEMA identifica todo por
 * {@code gemaTareaId} y {@code alumnoCi}, nunca por ids internos de SCA.
 */
@Service
public class GemaCalificacionService {

    private final PlanillaDao planillaDao;
    private final AlumnoDao alumnoDao;
    private final TareaDao tareaDao;
    private final GemaTareaDao gemaTareaDao;
    private final GemaGradeDao gemaGradeDao;
    private final GradeDao gradeDao;
    private final RegistroDao registroDao;

    public GemaCalificacionService(PlanillaDao planillaDao, AlumnoDao alumnoDao, TareaDao tareaDao,
            GemaTareaDao gemaTareaDao, GemaGradeDao gemaGradeDao, GradeDao gradeDao, RegistroDao registroDao) {
        this.planillaDao = planillaDao;
        this.alumnoDao = alumnoDao;
        this.tareaDao = tareaDao;
        this.gemaTareaDao = gemaTareaDao;
        this.gemaGradeDao = gemaGradeDao;
        this.gradeDao = gradeDao;
        this.registroDao = registroDao;
    }

    public List<CalificacionDto> listCalificaciones(int planillaId) {
        requirePlanilla(planillaId);
        try {
            return gemaGradeDao.findGradesForPlanilla(planillaId).stream()
                    .map(r -> new CalificacionDto(r.alumnoCi(), r.alumnoId(), r.gemaTareaId(), r.puntos()))
                    .toList();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar las calificaciones", ex);
        }
    }

    public SaveNotasResponse saveNotas(int planillaId, SaveNotasRequest request) {
        if (request == null || request.notas() == null || request.notas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "notas requerido (lista no vacía)");
        }
        Planilla planilla = requirePlanilla(planillaId);
        GemaEtapaValidation.requireEtapaAbierta(planilla);

        try {
            Map<String, Alumno> alumnosPorCi = alumnoDao.findAllActivos().stream()
                    .filter(a -> a.getCi() != null)
                    .collect(java.util.stream.Collectors.toMap(Alumno::getCi, a -> a, (a, b) -> a));

            List<NotaRechazadaDto> rechazadas = new ArrayList<>();
            record NotaValida(int alumnoId, int tareaId, Integer puntos) {
            }
            List<NotaValida> validas = new ArrayList<>();

            for (NotaInputDto nota : request.notas()) {
                String motivo = validarFila(nota, planilla, alumnosPorCi);
                if (motivo != null) {
                    rechazadas.add(new NotaRechazadaDto(nota.alumnoCi(), nota.gemaTareaId(), motivo));
                    continue;
                }
                Alumno alumno = alumnosPorCi.get(nota.alumnoCi());
                GemaTareaDao.TareaRef tareaRef = gemaTareaDao.findByGemaTareaId(nota.gemaTareaId());
                validas.add(new NotaValida(alumno.getId(), tareaRef.id(), nota.puntos()));
            }

            int recibidas = request.notas().size();
            if (!rechazadas.isEmpty()) {
                return new SaveNotasResponse(recibidas, 0, rechazadas);
            }

            registroDao.ensureRegistroRowsForPlanilla(planillaId, planilla.getCursoId());
            java.util.Set<Integer> alumnoIds = validas.stream().map(NotaValida::alumnoId)
                    .collect(java.util.stream.Collectors.toSet());
            Map<Integer, Integer> registroIdPorAlumno = registroDao.getRegistroIdsForPlanilla(planillaId, alumnoIds);

            Map<Integer, Map<Integer, Integer>> porRegistro = new HashMap<>();
            for (NotaValida v : validas) {
                Integer registroId = registroIdPorAlumno.get(v.alumnoId());
                if (registroId == null) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo resolver el registro del alumno " + v.alumnoId());
                }
                porRegistro.computeIfAbsent(registroId, k -> new HashMap<>()).put(v.tareaId(), v.puntos());
            }
            gradeDao.saveGradesBatch(planillaId, porRegistro);
            return new SaveNotasResponse(recibidas, recibidas, List.of());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudieron guardar las calificaciones", ex);
        }
    }

    /** @return el motivo del rechazo, o {@code null} si la fila es válida. */
    private String validarFila(NotaInputDto nota, Planilla planilla, Map<String, Alumno> alumnosPorCi) throws SQLException {
        if (nota.alumnoCi() == null || nota.alumnoCi().isBlank()) {
            return "alumnoCi requerido";
        }
        if (nota.gemaTareaId() == null || nota.gemaTareaId().isBlank()) {
            return "gemaTareaId requerido";
        }
        Alumno alumno = alumnosPorCi.get(nota.alumnoCi());
        if (alumno == null || alumno.getCursoId() != planilla.getCursoId()) {
            return "el alumno no pertenece al curso de esta planilla";
        }
        GemaTareaDao.TareaRef tareaRef = gemaTareaDao.findByGemaTareaId(nota.gemaTareaId());
        if (tareaRef == null || tareaRef.planillaId() != planilla.getId()) {
            return "no hay una tarea de GEMA con ese gemaTareaId en esta planilla";
        }
        if (nota.puntos() != null) {
            Tarea tarea = tareaDao.findById(tareaRef.id());
            if (nota.puntos() < 0 || nota.puntos() > tarea.getTotal()) {
                return "puntos fuera de rango (0.." + tarea.getTotal() + ")";
            }
        }
        return null;
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
}
