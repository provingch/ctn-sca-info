package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.IncumplimientoRevisionDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Asignacion;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Instrumento;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.RasgoPlanilla;
import ctn.informatica.sca.util.ScaUiContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger log = LoggerFactory.getLogger(EvaluacionCatalogController.class);

    private final AsignacionDao asignacionDao;
    private final CursoDao cursoDao;
    private final EspecialidadDao especialidadDao;
    private final InstrumentoDao instrumentoDao;
    private final RasgoPlanillaDao rasgoPlanillaDao;
    private final IncumplimientoRevisionDao incumplimientoRevisionDao;
    private final NotificacionDao notificacionDao;
    private final UserDao userDao;
    private final ProfesorDao profesorDao;

    public EvaluacionCatalogController(
            CursoDao cursoDao,
            EspecialidadDao especialidadDao,
            InstrumentoDao instrumentoDao,
            RasgoPlanillaDao rasgoPlanillaDao,
            IncumplimientoRevisionDao incumplimientoRevisionDao) {
        this(cursoDao, especialidadDao, instrumentoDao, rasgoPlanillaDao, incumplimientoRevisionDao,
                new AsignacionDao(), new NotificacionDao(), new UserDao(), new ProfesorDao());
    }

    public EvaluacionCatalogController(
            CursoDao cursoDao,
            EspecialidadDao especialidadDao,
            InstrumentoDao instrumentoDao,
            RasgoPlanillaDao rasgoPlanillaDao,
            IncumplimientoRevisionDao incumplimientoRevisionDao,
            AsignacionDao asignacionDao,
            NotificacionDao notificacionDao,
            UserDao userDao) {
        this(cursoDao, especialidadDao, instrumentoDao, rasgoPlanillaDao, incumplimientoRevisionDao,
                asignacionDao, notificacionDao, userDao, new ProfesorDao());
    }

    @Autowired
    public EvaluacionCatalogController(
            CursoDao cursoDao,
            EspecialidadDao especialidadDao,
            InstrumentoDao instrumentoDao,
            RasgoPlanillaDao rasgoPlanillaDao,
            IncumplimientoRevisionDao incumplimientoRevisionDao,
            NotificacionDao notificacionDao,
            UserDao userDao) {
        this(cursoDao, especialidadDao, instrumentoDao, rasgoPlanillaDao, incumplimientoRevisionDao,
                new AsignacionDao(), notificacionDao, userDao, new ProfesorDao());
    }

    public EvaluacionCatalogController(
            CursoDao cursoDao,
            EspecialidadDao especialidadDao,
            InstrumentoDao instrumentoDao,
            RasgoPlanillaDao rasgoPlanillaDao,
            IncumplimientoRevisionDao incumplimientoRevisionDao,
            AsignacionDao asignacionDao,
            NotificacionDao notificacionDao,
            UserDao userDao,
            ProfesorDao profesorDao) {
        this.cursoDao = cursoDao;
        this.especialidadDao = especialidadDao;
        this.instrumentoDao = instrumentoDao;
        this.rasgoPlanillaDao = rasgoPlanillaDao;
        this.incumplimientoRevisionDao = incumplimientoRevisionDao == null ? new IncumplimientoRevisionDao() : incumplimientoRevisionDao;
        this.asignacionDao = asignacionDao == null ? new AsignacionDao() : asignacionDao;
        this.notificacionDao = notificacionDao == null ? new NotificacionDao() : notificacionDao;
        this.userDao = userDao == null ? new UserDao() : userDao;
        this.profesorDao = profesorDao == null ? new ProfesorDao() : profesorDao;
    }

    public EvaluacionCatalogController(
            CursoDao cursoDao,
            EspecialidadDao especialidadDao,
            InstrumentoDao instrumentoDao,
            RasgoPlanillaDao rasgoPlanillaDao,
            IncumplimientoRevisionDao incumplimientoRevisionDao,
            NotificacionDao notificacionDao,
            UserDao userDao,
            ProfesorDao profesorDao) {
        this(cursoDao, especialidadDao, instrumentoDao, rasgoPlanillaDao, incumplimientoRevisionDao,
                new AsignacionDao(), notificacionDao, userDao, profesorDao);
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
        int userId = ApiAuth.requireUserId(authentication);
        try {
            if (shouldUseTeacherAssignmentsScope(authentication)) {
                List<Integer> allowedEspecialidadIds = asignacionDao.findByProfesor(userId).stream()
                        .map(Asignacion::getEspecialidadId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();
                if (allowedEspecialidadIds.isEmpty()) {
                    return List.of();
                }
                return especialidadDao.findAll().stream()
                        .filter(especialidad -> allowedEspecialidadIds.contains(especialidad.getId()))
                        .map(especialidad -> new EspecialidadDto(especialidad.getId(), especialidad.getNombre()))
                        .toList();
            }
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
        int userId = ApiAuth.requireUserId(authentication);
        try {
            boolean useScope = shouldUseTeacherAssignmentsScope(authentication);
            List<Curso> cursos;
            if (useScope) {
                java.util.Set<Integer> allowedIds = allowedCursoIdsForUser(userId);
                if (allowedIds == null || allowedIds.isEmpty()) {
                    return List.of();
                }
                cursos = cursoDao.findAll().stream()
                        .filter(curso -> allowedIds.contains(curso.getId()))
                        .toList();
            } else {
                cursos = cursoDao.findAll();
            }

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

    private boolean shouldUseTeacherAssignmentsScope(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        try {
            int userLevel = ApiAuth.requireUserLevel(authentication);
            if (userLevel != 3) {
                return false;
            }
        } catch (ResponseStatusException ex) {
            return false;
        }

        int userId = ApiAuth.requireUserId(authentication);

        try {
            Profesor profesor = profesorDao.findById(userId);
            if (profesor == null) return false;

            // If the professor is an administrator for a specialty (perfil especialidad != null),
            // keep the institutional view (no scoping).
            if (profesor.getEspecialidadId() != null) return false;

            // Otherwise, use the professor's real assignments to determine scope: if the
            // professor has at least one assignment, limit lists to those assignments.
            return !asignacionDao.findByProfesor(userId).isEmpty();
        } catch (Exception ex) {
            log.warn("No se pudo resolver el alcance por asignaciones del profesor {}: {}", userId, ex.getMessage());
            return false;
        }
    }

    private java.util.Set<Integer> allowedCursoIdsForUser(int userId) throws Exception {
        return asignacionDao.findByProfesor(userId).stream()
                .map(Asignacion::getCursoId)
                .filter(id -> id > 0)
                .collect(java.util.stream.Collectors.toSet());
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
        String estado = payload.get("estado") instanceof String s ? s.trim().toUpperCase() : "PERMITIDO";
        Map<String, Object> incumplimiento = null;
        try {
            incumplimiento = incumplimientoRevisionDao.findById(id);
            if (incumplimiento == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el incumplimiento a resolver.");
            }

            LocalDateTime suspensionDesde = parseDateTime(payload.get("suspensionDesde"), "suspensionDesde");
            LocalDateTime suspensionHasta = parseDateTime(payload.get("suspensionHasta"), "suspensionHasta");
            if ("RECHAZADO".equals(estado)) {
                if (suspensionDesde == null || suspensionHasta == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Para rechazar un incumplimiento debes indicar suspensión desde y hasta.");
                }
                if (!suspensionHasta.isAfter(suspensionDesde)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La suspensión hasta debe ser posterior a la suspensión desde.");
                }
            }

            boolean updated = incumplimientoRevisionDao.resolver(id, estado, evaluadorId, suspensionDesde, suspensionHasta);
            if (!updated) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el incumplimiento a resolver.");
            }

            int usuarioId = ((Number) incumplimiento.get("usuarioId")).intValue();
            String userType = NotificacionDao.resolveUserType(userDao, usuarioId);
            String cuerpo = "El incumplimiento #" + id + " fue resuelto como " + estado.toLowerCase();
            if ("RECHAZADO".equals(estado)) {
                cuerpo += " con suspensión desde " + suspensionDesde + " hasta " + suspensionHasta;
            }
            try {
                notificacionDao.crear(
                        usuarioId,
                        userType,
                        "INCUMPLIMIENTO_RESUELTO",
                        "Incumplimiento resuelto",
                        cuerpo,
                        "INCUMPLIMIENTO_REVISION",
                        (long) id);
            } catch (Exception ex) {
                log.warn("No se pudo notificar la resolución del incumplimiento {}: {}", id, ex.getMessage());
            }

            return Map.of("ok", true, "id", id, "estado", estado);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo resolver el incumplimiento", ex);
        }
    }

    private LocalDateTime parseDateTime(Object raw, String fieldName) {
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato inválido para " + fieldName + ".");
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

    public record MateriaEvaluacionDto(int id, String nombre) {
    }

    @GetMapping("/evaluacion/cursos/{cursoId}/materias")
    public List<MateriaEvaluacionDto> listMaterias(@PathVariable int cursoId, @RequestParam int periodo, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            boolean useScope = shouldUseTeacherAssignmentsScope(authentication);
            if (useScope) {
                java.util.Set<Integer> allowedIds = allowedCursoIdsForUser(userId);
                if (allowedIds == null || !allowedIds.contains(cursoId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este curso");
                }
            } else {
                boolean exists = cursoDao.findAll().stream().anyMatch(c -> c.getId() == cursoId);
                if (!exists) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado");
            }

            PlanillaDao planillaDao = new PlanillaDao();
            List<ctn.informatica.sca.model.Materia> materias = planillaDao.findMateriasWithPlanilla(cursoId, periodo);
            List<MateriaEvaluacionDto> response = new ArrayList<>();
            for (ctn.informatica.sca.model.Materia m : materias) {
                response.add(new MateriaEvaluacionDto(m.getId(), m.getNombre()));
            }
            return response;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al listar materias", ex);
        }
    }
}
