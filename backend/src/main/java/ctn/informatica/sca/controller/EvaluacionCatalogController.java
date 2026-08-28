package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.IncumplimientoRevisionDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Instrumento;
import ctn.informatica.sca.model.RasgoPlanilla;
import ctn.informatica.sca.util.ScaUiContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class EvaluacionCatalogController {

    private final CursoDao cursoDao;
    private final EspecialidadDao especialidadDao;
    private final InstrumentoDao instrumentoDao;
    private final RasgoPlanillaDao rasgoPlanillaDao;
    private final IncumplimientoRevisionDao incumplimientoRevisionDao;

    public EvaluacionCatalogController(
            CursoDao cursoDao,
            EspecialidadDao especialidadDao,
            InstrumentoDao instrumentoDao,
            RasgoPlanillaDao rasgoPlanillaDao,
            IncumplimientoRevisionDao incumplimientoRevisionDao) {
        this.cursoDao = cursoDao;
        this.especialidadDao = especialidadDao;
        this.instrumentoDao = instrumentoDao;
        this.rasgoPlanillaDao = rasgoPlanillaDao;
        this.incumplimientoRevisionDao = incumplimientoRevisionDao == null ? new IncumplimientoRevisionDao() : incumplimientoRevisionDao;
    }

    @GetMapping("/instrumentos")
    public List<InstrumentoDto> listInstrumentos(Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        try {
            List<Instrumento> instrumentos = instrumentoDao.findAll();
            List<InstrumentoDto> response = new ArrayList<>();
            for (Instrumento instrumento : instrumentos) {
                response.add(new InstrumentoDto(instrumento.getId(), instrumento.getNombre()));
            }
            return response;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar instrumentos", ex);
        }
    }

    @GetMapping("/evaluacion/especialidades")
    public List<EspecialidadDto> listEspecialidades(Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        try {
            List<Especialidad> especialidades = especialidadDao.findAll();
            List<EspecialidadDto> response = new ArrayList<>();
            for (Especialidad especialidad : especialidades) {
                response.add(new EspecialidadDto(especialidad.getId(), especialidad.getNombre()));
            }
            return response;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar especialidades", ex);
        }
    }

    @GetMapping("/evaluacion/cursos")
    public List<CursoEvaluacionDto> listCursos(
            @RequestParam(required = false) Integer especialidadId,
            Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        try {
            // Evaluación es un módulo institucional: el evaluador necesita ver
            // todos los cursos, no solo los vinculados como si fuera profesor.
            List<Curso> cursos = cursoDao.findAll();

            String selectedEspecialidad = null;
            if (especialidadId != null) {
                Especialidad especialidad = especialidadDao.findById(especialidadId);
                if (especialidad == null) {
                    return List.of();
                }
                selectedEspecialidad = ScaUiContext.normalizeSpecialty(especialidad.getNombre());
            }

            List<CursoEvaluacionDto> response = new ArrayList<>();
            for (Curso curso : cursos) {
                if (selectedEspecialidad != null
                        && !selectedEspecialidad.equals(ScaUiContext.normalizeSpecialty(curso.getEspecialidad()))) {
                    continue;
                }
                response.add(new CursoEvaluacionDto(
                        curso.getId(),
                        curso.getEspecialidad(),
                        curso.getNivel(),
                        curso.getSeccion()));
            }
            return response;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar cursos", ex);
        }
    }

    @GetMapping("/evaluacion/cursos/{cursoId}/clases")
    public List<ClaseRegistradaDto> listClases(@PathVariable int cursoId, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            boolean canAccessCurso = cursoDao.findAll()
                    .stream()
                    .anyMatch(curso -> curso.getId() == cursoId);
            if (!canAccessCurso) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este curso");
            }

            List<RasgoPlanilla> clases = rasgoPlanillaDao.listarPorCurso(cursoId);
            List<ClaseRegistradaDto> response = new ArrayList<>();
            for (RasgoPlanilla clase : clases) {
                response.add(new ClaseRegistradaDto(
                        clase.getId(),
                        clase.getCursoId(),
                        clase.getProfesorId(),
                        clase.getTema(),
                        clase.getFechaClase(),
                        clase.getCreatedAt()));
            }
            return response;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar clases", ex);
        }
    }

    @GetMapping("/evaluacion/incumplimientos")
    @PreAuthorize("hasRole('LEVEL_2')")
    public List<Map<String, Object>> listarIncumplimientos(Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        try {
            return incumplimientoRevisionDao.listarPendientes();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron cargar los incumplimientos", ex);
        }
    }

    @PostMapping("/evaluacion/incumplimientos/{id}/resolver")
    @PreAuthorize("hasRole('LEVEL_2')")
    public Map<String, Object> resolverIncumplimiento(@PathVariable int id, @RequestBody Map<String, Object> payload, Authentication authentication) {
        int evaluadorId = ApiAuth.requireUserId(authentication);
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere el estado de resolución.");
        }
        String estado = payload.get("estado") instanceof String s ? s.trim() : "PERMITIDO";
        LocalDateTime suspensionDesde = parseDateTime(payload.get("suspensionDesde"));
        LocalDateTime suspensionHasta = parseDateTime(payload.get("suspensionHasta"));
        try {
            boolean updated = incumplimientoRevisionDao.resolver(id, estado, evaluadorId, suspensionDesde, suspensionHasta);
            if (!updated) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el incumplimiento a resolver.");
            }
            return Map.of("ok", true, "id", id, "estado", estado.toUpperCase());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo resolver el incumplimiento", ex);
        }
    }

    private LocalDateTime parseDateTime(Object raw) {
        if (raw == null || raw.toString().isBlank()) {
            return null;
        }
        String text = raw.toString();
        try {
            return LocalDateTime.parse(text);
        } catch (Exception ignored) {
            try {
                return java.time.OffsetDateTime.parse(text).toLocalDateTime();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public record InstrumentoDto(int id, String nombre) {
    }

    public record EspecialidadDto(int id, String nombre) {
    }

    public record CursoEvaluacionDto(int id, String especialidad, int nivel, String seccion) {
    }

    public record ClaseRegistradaDto(
            int id,
            int cursoId,
            int profesorId,
            String tema,
            java.sql.Date fechaClase,
            java.sql.Timestamp createdAt) {
    }
}
