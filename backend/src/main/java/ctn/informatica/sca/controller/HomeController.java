package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.dao.PlanCurricularDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.dto.AssignFaltaCodigoRequest;
import ctn.informatica.sca.dto.AlumnoDto;
import ctn.informatica.sca.dto.CursoDto;
import ctn.informatica.sca.dto.CreateRasgoPlanillaRequest;
import ctn.informatica.sca.dto.HomeGoogleClassroomCourseDto;
import ctn.informatica.sca.dto.HomeMateriaDto;
import ctn.informatica.sca.dto.HomeResponse;
import ctn.informatica.sca.dto.InstrumentoDto;
import ctn.informatica.sca.dto.PlanillaDto;
import ctn.informatica.sca.dto.RasgoAsistenciaDto;
import ctn.informatica.sca.dto.RasgoPlanillaDto;
import ctn.informatica.sca.dto.SubmitRasgoAsistenciaRequest;
import ctn.informatica.sca.dto.UpdateRasgoCodigosRequest;
import ctn.informatica.sca.google.GoogleClassroomService;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Instrumento;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.RasgoAsistencia;
import ctn.informatica.sca.model.RasgoPlanilla;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.ScaUiContext;
import ctn.informatica.sca.service.TemaVerificacionService;
import ctn.informatica.sca.service.VerificacionResultado;
import com.google.api.services.classroom.model.Course;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private static final String VIEW_CLASE = "clase";
    private static final String VIEW_PLANILLAS = "planillas";

    private final CursoDao cursoDao;
    private final ProfesorDao profesorDao;
    private final PlanillaDao planillaDao;
    private final MateriaDao materiaDao;
    private final AlumnoDao alumnoDao;
    private final RasgoPlanillaDao rasgoPlanillaDao;
    private final PlanCurricularDao planCurricularDao;
    private final TemaVerificacionService temaVerificacionService;
    private final InstrumentoDao instrumentoDao;
    private final UserDao userDao;

    public HomeController() {
        this(new CursoDao(), new ProfesorDao(), new PlanillaDao(), new MateriaDao(), new AlumnoDao(), new RasgoPlanillaDao(), new InstrumentoDao(), new UserDao(), new PlanCurricularDao(), new TemaVerificacionService());
    }

    @Autowired
    public HomeController(
            CursoDao cursoDao,
            ProfesorDao profesorDao,
            PlanillaDao planillaDao,
            MateriaDao materiaDao,
            AlumnoDao alumnoDao,
            RasgoPlanillaDao rasgoPlanillaDao,
            InstrumentoDao instrumentoDao,
            UserDao userDao,
            PlanCurricularDao planCurricularDao,
            TemaVerificacionService temaVerificacionService) {
        this.cursoDao = cursoDao;
        this.profesorDao = profesorDao;
        this.planillaDao = planillaDao;
        this.materiaDao = materiaDao;
        this.alumnoDao = alumnoDao;
        this.rasgoPlanillaDao = rasgoPlanillaDao;
        this.planCurricularDao = planCurricularDao;
        this.temaVerificacionService = temaVerificacionService;
        this.instrumentoDao = instrumentoDao;
        this.userDao = userDao;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public HomeResponse getHome(
            @RequestParam(required = false) Integer cursoId,
            @RequestParam(required = false) Integer etapa,
            @RequestParam(required = false) String view,
            Authentication authentication) {
        User user = requireUser(authentication);
        List<Curso> cursos;
        try {
            cursos = cursoDao.consultarCursos(user.getId());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar los cursos", ex);
        }

        Curso selectedCurso = null;
        if (cursoId != null && cursoId > 0) {
            for (Curso curso : cursos) {
                if (curso != null && curso.getId() == cursoId) {
                    selectedCurso = curso;
                    break;
                }
            }
        }
        if (selectedCurso == null && !cursos.isEmpty()) {
            selectedCurso = cursos.get(0);
        }

        int selectedEtapa = etapa != null && (etapa == 1 || etapa == 2) ? etapa : 1;
        String viewMode = resolveViewMode(view);

        boolean googleClassroomConnected = false;
        String googleClassroomError = null;
        String googleClassroomPlaceholder = null;
        String googleClassroomVisibilityNotice = null;
        List<Course> googleClassroomCourses = Collections.emptyList();
        List<String> teacherSubjects = new ArrayList<>();
        String manualTeacherSubjectsText = "";
        Profesor profesor = null;

        if (user.getLevel() != 4) {
            try {
                profesor = profesorDao.findById(user.getId());
            } catch (Exception ex) {
                // ignore; not critical for rendering general home state
            }
        }

        if (profesor != null) {
            googleClassroomConnected = GoogleClassroomService.isGoogleConnected(profesor);
            try {
                teacherSubjects.addAll(planillaDao.findSubjectsByProfesor(profesor.getId()));
                teacherSubjects.addAll(materiaDao.findNamesByProfesor(profesor.getId()));
                manualTeacherSubjectsText = profesorDao.findManualSubjectsText(profesor.getId());
            } catch (SQLException ex) {
                // ignore; fallback to empty teacher subject list
            }
        }

        if (googleClassroomConnected && profesor != null) {
            try {
                List<Curso> classroomSelectionContext = selectedCurso != null
                        ? Collections.singletonList(selectedCurso)
                        : cursos;
                googleClassroomCourses = GoogleClassroomService.listAllowedCourses(profesor, classroomSelectionContext, teacherSubjects);
            } catch (Exception ex) {
                googleClassroomError = "No se pudieron cargar los cursos de Google Classroom: " + ex.getMessage();
            }
        } else if (!googleClassroomConnected) {
            googleClassroomPlaceholder = "Conecte su classroom y vuelva a intentarlo";
        }

        List<Planilla> planillas = Collections.emptyList();
        List<HomeMateriaDto> materiasDetectadas = Collections.emptyList();
        List<RasgoPlanilla> rasgoPlanillas = Collections.emptyList();
        RasgoPlanilla selectedPlanilla = null;
        List<RasgoAsistencia> asistencias = Collections.emptyList();
        List<Alumno> alumnosValidos = Collections.emptyList();
        List<Alumno> alumnosInvalidos = Collections.emptyList();
        List<Instrumento> instrumentos = Collections.emptyList();

        if (selectedCurso != null) {
            try {
                planillas = planillaDao.consultarPlanillas(user.getId(), selectedCurso.getId(), selectedEtapa);
            } catch (SQLException ex) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar las planillas", ex);
            }

            try {
                List<ctn.informatica.sca.model.Materia> materias = planillaDao.findMateriasSinPlanilla(user.getId(), selectedCurso.getId(), selectedEtapa);
                materiasDetectadas = materias.stream()
                        .map(m -> new HomeMateriaDto(m.getId(), m.getNombre(), m.getCategoria()))
                        .collect(Collectors.toList());
            } catch (SQLException ex) {
                materiasDetectadas = Collections.emptyList();
            }

            if (googleClassroomConnected && !googleClassroomCourses.isEmpty()) {
                try {
                    Map<String, Integer> planillaMap = new HashMap<>();
                    Map<String, Integer> planillaMateriaMap = new HashMap<>();
                    for (Planilla planilla : planillas) {
                        try {
                            Optional<Course> resolved = GoogleClassroomService.resolveCourseForPlanilla(profesor, selectedCurso, planilla);
                            if (resolved.isPresent()) {
                                Course course = resolved.get();
                                if (course.getId() != null && !course.getId().isBlank()) {
                                    planillaMap.put(course.getId(), planilla.getId());
                                    planillaMateriaMap.put(course.getId(), planilla.getMateriaId());
                                }
                            }
                        } catch (IOException ioe) {
                            // ignore mappings when Google Classroom resolution fails
                        }
                    }
                    if (!planillaMap.isEmpty()) {
                        // preserve values for response building below
                    }
                } catch (Exception ignored) {
                }
            }

            try {
                List<Alumno> alumnos = alumnoDao.findByCursoId(selectedCurso.getId());
                alumnosValidos = alumnos.stream().filter(this::isCompleteName).collect(Collectors.toList());
                alumnosInvalidos = alumnos.stream().filter(a -> !isCompleteName(a)).collect(Collectors.toList());
            } catch (SQLException ex) {
                alumnosValidos = Collections.emptyList();
                alumnosInvalidos = Collections.emptyList();
            }

            try {
                rasgoPlanillas = rasgoPlanillaDao.listarPorProfesorCurso(user.getId(), selectedCurso.getId());
                if (!rasgoPlanillas.isEmpty()) {
                    selectedPlanilla = rasgoPlanillas.get(0);
                    asistencias = rasgoPlanillaDao.listarAsistencias(selectedPlanilla.getId());
                }
            } catch (SQLException ex) {
                rasgoPlanillas = Collections.emptyList();
                asistencias = Collections.emptyList();
            }
        }

        Map<String, Integer> classroomPlanillaMap = new HashMap<>();
        Map<String, Integer> classroomPlanillaMateriaMap = new HashMap<>();
        if (googleClassroomConnected && !googleClassroomCourses.isEmpty() && selectedCurso != null) {
            try {
                for (Planilla planilla : planillas) {
                    try {
                        Optional<Course> resolved = GoogleClassroomService.resolveCourseForPlanilla(profesor, selectedCurso, planilla);
                        if (resolved.isPresent()) {
                            Course course = resolved.get();
                            if (course.getId() != null && !course.getId().isBlank()) {
                                classroomPlanillaMap.put(course.getId(), planilla.getId());
                                classroomPlanillaMateriaMap.put(course.getId(), planilla.getMateriaId());
                            }
                        }
                    } catch (IOException ioe) {
                        // ignore failure per planilla resolution
                    }
                }
                if (!googleClassroomCourses.isEmpty() && classroomPlanillaMateriaMap.isEmpty()) {
                    googleClassroomVisibilityNotice = "Se encontraron cursos en Google Classroom, pero ninguno pudo asociarse automáticamente a este curso. Revisa el nombre del curso en Classroom o vincúlalo manualmente.";
                }
            } catch (Exception ignored) {
            }
        }

        if (!googleClassroomConnected) {
            googleClassroomVisibilityNotice = null;
        }

        try {
            instrumentos = instrumentoDao.findAll();
        } catch (SQLException ex) {
            instrumentos = Collections.emptyList();
        }

        return new HomeResponse(
                cursos.stream().map(this::toCursoDto).collect(Collectors.toList()),
                selectedCurso == null ? null : toCursoDto(selectedCurso),
                selectedEtapa,
                viewMode,
                planillas.stream().map(this::toPlanillaDto).collect(Collectors.toList()),
                !planillas.isEmpty(),
                classroomPlanillaMap,
                classroomPlanillaMateriaMap,
                materiasDetectadas,
                googleClassroomConnected,
                googleClassroomError,
                googleClassroomPlaceholder,
                googleClassroomVisibilityNotice,
                googleClassroomCourses.stream().map(this::toGoogleClassroomCourseDto).collect(Collectors.toList()),
                rasgoPlanillas.stream().map(this::toRasgoPlanillaDto).collect(Collectors.toList()),
                selectedPlanilla == null ? null : toRasgoPlanillaDto(selectedPlanilla),
                asistencias.stream().map(this::toRasgoAsistenciaDto).collect(Collectors.toList()),
                alumnosValidos.stream().map(this::toAlumnoDto).collect(Collectors.toList()),
                alumnosInvalidos.stream().map(this::toAlumnoDto).collect(Collectors.toList()),
                instrumentos.stream().map(this::toInstrumentoDto).collect(Collectors.toList())
        );
    }

    @PostMapping("/create-rasgo-planilla")
    @PreAuthorize("hasRole('LEVEL_1')")
    @ResponseStatus(HttpStatus.CREATED)
    public void createRasgoPlanilla(
            @RequestBody CreateRasgoPlanillaRequest request,
            Authentication authentication) {
        User user = requireUser(authentication);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El request es requerido.");
        }

        int cursoId = request.cursoId() == null ? 0 : request.cursoId();
        if (cursoId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso es requerido.");
        }
        String tema = safeTrim(request.tema());
        if (tema.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tema es requerido.");
        }

        List<Alumno> alumnos;
        try {
            alumnos = alumnoDao.findByCursoId(cursoId);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar alumnos para el curso", ex);
        }

        List<Alumno> elegibles = alumnos.stream().filter(this::isCompleteName).collect(Collectors.toList());
        if (elegibles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay alumnos válidos para crear la planilla de rasgos.");
        }

        Set<Integer> ausentes = request.alumnosAusentes() == null
                ? Collections.emptySet()
                : request.alumnosAusentes().stream().filter(id -> id != null && id > 0).collect(Collectors.toSet());

        String temaPersistido = composeTemaConContexto(request.instrumentoId() == null ? 0 : request.instrumentoId(), request.turno(), tema);
        try {
            int planillaId = rasgoPlanillaDao.crearPlanillaRasgo(cursoId, user.getId(), temaPersistido, elegibles, ausentes, request.codigosPorAlumno(), request.asignacionId());

            // Si se indicó asignacionId, intentamos verificar el tema contra el plan curricular.
            if (request.asignacionId() != null) {
                try {
                    VerificacionResultado resultado = temaVerificacionService.verificar(request.asignacionId(), temaPersistido);
                    rasgoPlanillaDao.actualizarVerificacionPlanilla(planillaId, resultado.estado(), resultado.temaPlanCurricularId());
                    if ("OK".equalsIgnoreCase(resultado.estado()) && resultado.temaPlanCurricularId() != null) {
                        try {
                            planCurricularDao.marcarCubierto(resultado.temaPlanCurricularId(), planillaId);
                        } catch (SQLException ex) {
                            // No bloquear la creación de la planilla si el marcado falla; loguear y continuar
                            System.err.println("Error marcando tema como cubierto: " + ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    // No bloquear la creación de la planilla si la verificación falla; loguear y continuar
                    System.err.println("Error verificando tema contra plan curricular: " + ex.getMessage());
                }
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear la planilla de rasgos", ex);
        }
    }

    @PostMapping("/submit-rasgo-asistencia")
    @PreAuthorize("hasRole('LEVEL_1')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitRasgoAsistencia(
            @RequestBody SubmitRasgoAsistenciaRequest request,
            Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        if (request == null || request.asistenciaId() == null || request.asistenciaId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id de asistencia es requerido.");
        }
        String estado = "presente".equalsIgnoreCase(request.estado()) ? "presente" : "ausente";
        try {
            rasgoPlanillaDao.registrarRespuesta(request.asistenciaId(), estado);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo registrar la asistencia", ex);
        }
    }

    @PostMapping("/update-rasgo-codigos")
    @PreAuthorize("hasRole('LEVEL_1')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRasgoCodigos(
            @RequestBody UpdateRasgoCodigosRequest request,
            Authentication authentication) {
        User user = requireUser(authentication);
        if (request == null || request.asistenciaId() == null || request.asistenciaId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id de asistencia es requerido.");
        }
        try {
            RasgoAsistencia asistencia = rasgoPlanillaDao.findAsistenciaById(request.asistenciaId());
            if (asistencia == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asistencia no encontrada");
            }
            RasgoPlanilla planilla = rasgoPlanillaDao.findPlanillaById(asistencia.getPlanillaRasgoId());
            if (planilla == null || planilla.getProfesorId() != user.getId()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta clase");
            }
            rasgoPlanillaDao.reemplazarCodigos(request.asistenciaId(), request.codigos());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron guardar los códigos", ex);
        }
    }

    @PostMapping("/assign-falta-codigo")
    @PreAuthorize("hasRole('LEVEL_1')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignFaltaCodigo(
            @RequestBody AssignFaltaCodigoRequest request,
            Authentication authentication) {
        ApiAuth.requireUserId(authentication);
        if (request == null || request.asistenciaId() == null || request.asistenciaId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id de asistencia es requerido.");
        }

        try {
            RasgoAsistencia asistencia = rasgoPlanillaDao.findAsistenciaById(request.asistenciaId());
            if (asistencia == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asistencia no encontrada");
            }
            String estado = asistencia.getEstado();
            if (estado == null || estado.isBlank()) {
                estado = "pendiente";
            }
            if (request.faltaCodigo() == null || request.faltaCodigo().trim().isEmpty()) {
                rasgoPlanillaDao.registrarRespuesta(request.asistenciaId(), estado);
            } else {
                rasgoPlanillaDao.registrarRespuesta(request.asistenciaId(), estado, request.faltaCodigo(), request.faltaObservacion());
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo asignar el código de falta", ex);
        }
    }

    private User requireUser(Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            User user = userDao.findById(userId);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
            }
            return user;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar el usuario", ex);
        }
    }

    private String resolveViewMode(String requestedView) {
        if (VIEW_CLASE.equals(requestedView)) {
            return VIEW_CLASE;
        }
        if (VIEW_PLANILLAS.equals(requestedView)) {
            return VIEW_PLANILLAS;
        }
        if ("rasgos".equals(requestedView)) {
            return VIEW_CLASE;
        }
        return VIEW_CLASE;
    }

    private CursoDto toCursoDto(Curso curso) {
        return new CursoDto(curso.getId(), curso.getEspecialidad(), curso.getCurso(), curso.getSeccion());
    }

    private PlanillaDto toPlanillaDto(Planilla planilla) {
        return new PlanillaDto(planilla.getId(), planilla.getNombre(), planilla.getPeriodo(), planilla.getTareasCount(), planilla.getMateriaId());
    }

    private HomeGoogleClassroomCourseDto toGoogleClassroomCourseDto(Course course) {
        return new HomeGoogleClassroomCourseDto(course.getId(), course.getName(), course.getSection(), course.getRoom());
    }

    private RasgoPlanillaDto toRasgoPlanillaDto(RasgoPlanilla planilla) {
        return new RasgoPlanillaDto(planilla.getId(), planilla.getTema(), planilla.getFechaClase() == null ? null : planilla.getFechaClase().toString());
    }

    private RasgoAsistenciaDto toRasgoAsistenciaDto(RasgoAsistencia asistencia) {
        return new RasgoAsistenciaDto(asistencia.getId(), asistencia.getAlumnoId(), asistencia.getAlumnoNombreCompleto(), asistencia.getEstado(), asistencia.getFaltaCodigo(), asistencia.getFaltaObservacion(), asistencia.getCodigos());
    }

    private AlumnoDto toAlumnoDto(Alumno alumno) {
        return new AlumnoDto(alumno.getId(), alumno.getNombre(), alumno.getApellido());
    }

    private InstrumentoDto toInstrumentoDto(Instrumento instrumento) {
        return new InstrumentoDto(instrumento.getId(), instrumento.getNombre());
    }

    private boolean isCompleteName(Alumno alumno) {
        if (alumno == null) {
            return false;
        }
        String nombre = safeTrim(alumno.getNombre());
        String apellido = safeTrim(alumno.getApellido());
        return !nombre.isEmpty() && !apellido.isEmpty() && nombre.length() >= 2 && apellido.length() >= 2;
    }

    private String composeTemaConContexto(int instrumentoId, String turno, String temaBase) {
        StringBuilder contexto = new StringBuilder();
        if (turno != null && !turno.isBlank()) {
            contexto.append("[Turno: ").append(turno).append("] ");
        }
        if (instrumentoId <= 0) {
            return contexto.append(temaBase).toString();
        }
        for (Instrumento instrumento : loadInstrumentos()) {
            if (instrumento.getId() == instrumentoId) {
                return contexto.append("[").append(instrumento.getNombre()).append("] ").append(temaBase).toString();
            }
        }
        return contexto.append(temaBase).toString();
    }

    private List<Instrumento> loadInstrumentos() {
        try {
            return instrumentoDao.findAll();
        } catch (SQLException ex) {
            return Collections.emptyList();
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
