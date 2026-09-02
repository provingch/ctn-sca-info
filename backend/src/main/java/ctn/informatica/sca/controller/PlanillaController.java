package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.RegistroDao;
import ctn.informatica.sca.dao.StudentRowDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.google.ClassroomSyncOrchestrator;
import ctn.informatica.sca.dao.ClassroomSyncLogDao;
import ctn.informatica.sca.service.ActivityLogService;
import ctn.informatica.sca.service.PlanillaService;
import ctn.informatica.sca.google.GoogleClassroomService;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ctn.informatica.sca.util.PlanillaProcesoWorkbookBuilder;
import ctn.informatica.sca.util.AcademicPeriod;

@RestController
@RequestMapping("/api/planillas")
public class PlanillaController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PlanillaController.class);

    private final PlanillaDao planillaDao;
    private final ProfesorDao profesorDao;
    private final ClassroomSyncOrchestrator classroomSyncOrchestrator;
    private final ClassroomSyncLogDao classroomSyncLogDao;
    private final ActivityLogService activityLogService;

    public PlanillaController() {
        this(new PlanillaDao(), new ProfesorDao(), new ClassroomSyncOrchestrator(), new ClassroomSyncLogDao(), null);
    }

    public PlanillaController(
            PlanillaDao planillaDao,
            ProfesorDao profesorDao,
            ClassroomSyncOrchestrator classroomSyncOrchestrator) {
        this(planillaDao, profesorDao, classroomSyncOrchestrator, new ClassroomSyncLogDao(), null);
    }

    @Autowired
    public PlanillaController(
            PlanillaDao planillaDao,
            ProfesorDao profesorDao,
            ClassroomSyncOrchestrator classroomSyncOrchestrator,
            ClassroomSyncLogDao classroomSyncLogDao,
            ActivityLogService activityLogService) {
        this.planillaDao = planillaDao;
        this.profesorDao = profesorDao;
        this.classroomSyncOrchestrator = classroomSyncOrchestrator;
        this.classroomSyncLogDao = classroomSyncLogDao;
        this.activityLogService = activityLogService;
    }

    @GetMapping("/{planillaId}")
    public PlanillaDetailResponse getById(@PathVariable int planillaId, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            Planilla planilla = requireOwnedPlanillaById(planillaId, userId);
            return buildPlanillaDetail(planilla);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar planilla", ex);
        }
    }

    @GetMapping
    public PlanillaDetailResponse getByComposite(
            @RequestParam Integer cursoId,
            @RequestParam Integer materiaId,
            @RequestParam(required = false) Integer etapa,
            Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        int etapaValue = etapa != null ? etapa : resolveDefaultEtapa(LocalDate.now());
        try {
            Planilla planilla = requireOwnedPlanillaByComposite(cursoId, materiaId, etapaValue, userId);
            return buildPlanillaDetail(planilla);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar planilla", ex);
        }
    }

    @PostMapping("/resolve")
    public ResponseEntity<ResolvePlanillaResponse> resolve(
            @RequestBody ResolvePlanillaRequest request,
            Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        if (request == null || request.cursoId() == null || request.materiaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cursoId y materiaId son requeridos");
        }

        int etapa = request.etapa() == null ? resolveDefaultEtapa(LocalDate.now()) : request.etapa();
        if (etapa != 1 && etapa != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "etapa debe ser 1 o 2");
        }

        try {
            Planilla planilla = planillaDao.findByCompositeKey(request.cursoId(), request.materiaId(), etapa);
            boolean created = false;

            if (planilla == null) {
                planilla = planillaDao.crear(request.cursoId(), request.materiaId(), etapa, userId);
                created = true;
            }

            if (planilla == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo resolver la planilla");
            }
            if (planilla.getProfesorId() != userId) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta planilla");
            }

            ResolvePlanillaResponse response = new ResolvePlanillaResponse(
                    planilla.getId(),
                    created,
                    "/api/planillas/" + planilla.getId());
            return created ? ResponseEntity.status(HttpStatus.CREATED).body(response) : ResponseEntity.ok(response);
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al resolver planilla", ex);
        }
    }

    @PostMapping("/{planillaId}/notas")
    public SaveGradesResponse saveGrades(
            @PathVariable int planillaId,
            @RequestBody SaveGradesRequest request,
            Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        if (request == null || request.grades() == null || request.grades().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El body debe incluir grades[]");
        }

        try {
            Planilla planilla = requireOwnedPlanillaById(planillaId, userId);
            List<Tarea> tareas = filterTasksByEtapa(new TareaDao().consultarTarea(planilla.getId()), planilla.getEtapaIndex());
            Map<Integer, Integer> tareaMax = new HashMap<>();
            for (Tarea tarea : tareas) {
                tareaMax.put(tarea.getId(), tarea.getTotal());
            }

            Map<Integer, Map<Integer, Integer>> gradesByAlumno = new HashMap<>();
            List<String> warnings = new ArrayList<>();
            int emptyAsZeroCount = 0;
            int skippedCount = 0;

            for (AlumnoGradesInput alumnoInput : request.grades()) {
                if (alumnoInput == null || alumnoInput.alumnoId() == null) {
                    skippedCount++;
                    warnings.add("Entrada sin alumnoId: omitida");
                    continue;
                }
                if (alumnoInput.items() == null || alumnoInput.items().isEmpty()) {
                    skippedCount++;
                    warnings.add("Alumno " + alumnoInput.alumnoId() + " sin items: omitido");
                    continue;
                }

                for (GradeItemInput item : alumnoInput.items()) {
                    if (item == null || item.tareaId() == null) {
                        skippedCount++;
                        warnings.add("Item sin tareaId para alumno " + alumnoInput.alumnoId() + ": omitido");
                        continue;
                    }

                    Integer max = tareaMax.get(item.tareaId());
                    if (max == null) {
                        skippedCount++;
                        warnings.add("Tarea " + item.tareaId() + " no pertenece a la planilla/etapa: omitida");
                        continue;
                    }

                    Integer puntos = item.puntos();
                    if (puntos == null) {
                        puntos = 0;
                        emptyAsZeroCount++;
                    }
                    if (puntos < 0) {
                        warnings.add("Puntos ajustados a mínimo (0) para alumno " + alumnoInput.alumnoId() + " tarea " + item.tareaId());
                        puntos = 0;
                    }
                    if (puntos > max) {
                        warnings.add("Puntos ajustados a máximo (" + max + ") para alumno " + alumnoInput.alumnoId() + " tarea " + item.tareaId());
                        puntos = max;
                    }

                    gradesByAlumno.computeIfAbsent(alumnoInput.alumnoId(), key -> new HashMap<>())
                            .put(item.tareaId(), puntos);
                }
            }

            if (gradesByAlumno.isEmpty()) {
                return new SaveGradesResponse(
                        "No hay calificaciones válidas para guardar.",
                        0,
                        skippedCount,
                        warnings,
                        planilla.getId());
            }

            Set<Integer> alumnoIds = gradesByAlumno.keySet();
            RegistroDao registroDao = new RegistroDao();
            Map<Integer, Integer> alumnoToRegistro = registroDao.getRegistroIdsForPlanilla(planilla.getId(), alumnoIds);

            Map<Integer, Map<Integer, Integer>> gradesByRegistro = new HashMap<>();
            for (Map.Entry<Integer, Map<Integer, Integer>> entry : gradesByAlumno.entrySet()) {
                Integer registroId = alumnoToRegistro.get(entry.getKey());
                if (registroId == null) {
                    skippedCount += entry.getValue().size();
                    warnings.add("Alumno " + entry.getKey() + " no está registrado en la planilla: calificaciones omitidas");
                    continue;
                }
                gradesByRegistro.put(registroId, entry.getValue());
            }

            if (emptyAsZeroCount > 0) {
                warnings.add(emptyAsZeroCount + " nota(s) vacías guardadas como 0");
            }

            int savedCount = 0;
            for (Map<Integer, Integer> value : gradesByRegistro.values()) {
                savedCount += value.size();
            }

            if (savedCount > 0) {
                new GradeDao().saveGradesBatch(planilla.getId(), gradesByRegistro);
            }

            String message = savedCount > 0
                    ? "Cambios guardados correctamente."
                    : "No se encontraron registros válidos para guardar.";

            return new SaveGradesResponse(message, savedCount, skippedCount, warnings, planilla.getId());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar notas", ex);
        }
    }

    @GetMapping("/{planillaId}/export")
    public void exportPlanilla(@PathVariable int planillaId, Authentication authentication, HttpServletResponse response) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            Planilla planilla = requireOwnedPlanillaById(planillaId, userId);
            try {
                if (activityLogService != null) {
                    activityLogService.registrar(userId, "Descargó planilla — " + planillaId);
                }
            } catch (Exception ex) {
                log.warn("No se pudo registrar actividad para usuario {}: {}", userId, ex.getMessage());
            }
            Curso curso = new CursoDao().findById(planilla.getCursoId());
            Materia materia = null;
            String disciplina = "";
            if (planilla.getMateriaId() > 0) {
                materia = new MateriaDao().findById(planilla.getMateriaId());
                if (materia != null && materia.getNombre() != null) {
                    disciplina = materia.getNombre();
                }
            }

            List<Tarea> tareas = filterTasksByEtapa(new TareaDao().consultarTarea(planilla.getId()), planilla.getEtapaIndex());
            Map<Integer, Integer> maxima = new LinkedHashMap<>();
            int total = 0;
            for (Tarea tarea : tareas) { maxima.put(tarea.getId(), tarea.getTotal()); total += tarea.getTotal(); }
            planilla.computeGradeRanges(total);
            List<StudentRow> rows = new StudentRowDao().loadRowsForPlanilla(planilla, maxima, total);

            Map<Integer, Integer> firstStageGrades = Map.of();
            if (planilla.getEtapaIndex() == 2) {
                Planilla first = new PlanillaDao().findByCompositeKey(planilla.getCursoId(), planilla.getMateriaId(), 1);
                if (first != null) {
                    List<Tarea> tareasPrimera = filterTasksByEtapa(new TareaDao().consultarTarea(first.getId()), 1);
                    Map<Integer, Integer> maximaPrim = new LinkedHashMap<>(); int totalPrim = 0;
                    for (Tarea t : tareasPrimera) { maximaPrim.put(t.getId(), t.getTotal()); totalPrim += t.getTotal(); }
                    first.computeGradeRanges(totalPrim);
                    for (StudentRow row : new StudentRowDao().loadRowsForPlanilla(first, maximaPrim, totalPrim)) {
                        firstStageGrades = new HashMap<>(firstStageGrades);
                        ((HashMap<Integer,Integer>) firstStageGrades).put(row.getAlumnoId(), first.getNotaForSum(row.getTotal()));
                    }
                }
            }

            Profesor profesor = profesorDao.findById(userId);
            String profesorNombre = profesor == null ? "" : profesor.getFullName();

            PlanillaProcesoWorkbookBuilder.PlanillaSheetData sheetData = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                    planilla,
                    curso,
                    disciplina,
                    profesorNombre,
                    "",
                    tareas,
                    rows,
                    firstStageGrades,
                    profesor == null ? null : profesor.getFirmaImagen()
            );

            String base = buildExportBaseFilename(planilla, curso, disciplina);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(base + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20"));

            try (XSSFWorkbook workbook = new PlanillaProcesoWorkbookBuilder().buildSingleWorkbook(sheetData, "Planilla")) {
                workbook.write(response.getOutputStream());
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el archivo", ex);
        }
    }

    @PostMapping("/{planillaId}/sync/classroom")
    public ClassroomSyncResponse syncClassroom(@PathVariable int planillaId, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            Planilla planilla = requireOwnedPlanillaById(planillaId, userId);
            Profesor profesor = profesorDao.findById(userId);
            if (profesor == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profesor no encontrado");
            }
                ClassroomSyncOrchestrator.ClassroomSyncResult result = classroomSyncOrchestrator.syncPlanillaWithClassroom(profesor, planilla);
                // try to persist a sync log (non-blocking)
                try {
                    if (classroomSyncLogDao != null) {
                        classroomSyncLogDao.insert(planilla.getId(), userId, result.importedCourseworks(), result.importedGrades());
                    }
                } catch (Exception e) {
                    // log and continue
                    log.warn("Could not persist classroom sync log: {}", e.getMessage());
                }
                try {
                    if (activityLogService != null) {
                        activityLogService.registrar(userId, "Sincronizó Classroom — planilla: " + planilla.getId());
                    }
                } catch (Exception e) {
                    log.warn("No se pudo registrar actividad para usuario {}: {}", userId, e.getMessage());
                }

                return new ClassroomSyncResponse(
                    planilla.getId(),
                    result.googleCourseId(),
                    result.classroomCourseMapped(),
                    result.importedCourseworks(),
                    result.linkedStudents(),
                    result.importedGrades(),
                    result.courseName(),
                    result.courseSection(),
                    result.courseAlternateLink(),
                    result.message());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al sincronizar Classroom", ex);
        } catch (IOException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("MISSING_REQUIRED_SCOPES")) {
                // Indicar al frontend que el usuario debe reconectar su cuenta para conceder permisos adicionales
                throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Faltan permisos necesarios de Classroom: reconectar cuenta de Google con los permisos solicitados.");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al sincronizar Classroom", ex);
        }
    }

    @PostMapping("/{planillaId}/classroom")
    public ConfirmClassroomResponse confirmClassroom(@PathVariable int planillaId, @RequestBody ConfirmClassroomRequest req, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        if (req == null || req.googleCourseId() == null || req.googleCourseId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "googleCourseId es requerido");
        }
        try {
            Planilla planilla = requireOwnedPlanillaById(planillaId, userId);
            boolean ok = this.planillaDao.updateClassroomCourseId(planillaId, req.googleCourseId());
            if (!ok) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo persistir la asociación");
            return new ConfirmClassroomResponse("Asociación con Classroom guardada.", planillaId, req.googleCourseId());
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar la asociación con Classroom", ex);
        }
    }

    @PutMapping("/{planillaId}/etapa1/fecha-cierre")
    public void guardarFechaCierreEtapa1(@PathVariable int planillaId, @RequestBody(required = false) LocalDate fecha, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            Planilla planilla = requireOwnedPlanillaById(planillaId, userId);
            if (planilla.getEtapa1Confirmada()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Etapa 1 cerrada, no se pueden modificar sus datos");
            }
            if (fecha == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de cierre de Etapa 1 es requerida");
            }
            boolean updated = planillaDao.updateFechaCierreEtapa1(planillaId, fecha);
            if (!updated) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planilla no encontrada");
            }
            planilla.setFechaCierreEtapa1(fecha);
            new PlanillaService(planillaDao, new TareaDao()).reclasificarEtapas(planillaId);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al guardar la fecha de cierre de Etapa 1", ex);
        }
    }

    @PostMapping("/{planillaId}/etapa1/confirmar")
    public void confirmarEtapa1(@PathVariable int planillaId, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        try {
            Planilla planilla = requireOwnedPlanillaById(planillaId, userId);
            if (planilla.getEtapa1Confirmada()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Etapa 1 cerrada, no se pueden modificar sus datos");
            }
            if (planilla.getFechaCierreEtapa1() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar la fecha de cierre de Etapa 1 antes de confirmar");
            }
            boolean updated = planillaDao.updateEtapa1Confirmada(planillaId, true);
            if (!updated) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planilla no encontrada");
            }
            planilla.setEtapa1Confirmada(true);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al confirmar Etapa 1", ex);
        }
    }

    private Planilla requireOwnedPlanillaById(int planillaId, int userId) throws SQLException {
        Planilla planilla = planillaDao.findById(planillaId);
        if (planilla == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planilla no encontrada");
        }
        if (planilla.getProfesorId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta planilla");
        }
        return planilla;
    }

    // Visible for testing: build the sanitized base filename used on export
    public static String buildExportBaseFilename(Planilla planilla, Curso curso, String disciplina) {
        String disciplinaPart = disciplina == null ? "" : disciplina;
        String cursoOrdinal = "";
        String seccion = "";
        if (curso != null) {
            cursoOrdinal = curso.getCursoOrdinal() == null ? "" : curso.getCursoOrdinal();
            seccion = curso.getSeccion() == null ? "" : curso.getSeccion();
        }
        String cursoSeccion = cursoOrdinal + "-" + seccion;
        String base = "Planilla_"
                + (disciplinaPart.isBlank() ? String.valueOf(planilla.getId()) : disciplinaPart)
                + "_" + cursoSeccion
                + "_" + planilla.getPeriodo();
        base = base.replaceAll("[^A-Za-z0-9_-]", "_");
        return base;
    }

    private Planilla requireOwnedPlanillaByComposite(Integer cursoId, Integer materiaId, int etapa, int userId) throws SQLException {
        if (cursoId == null || materiaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cursoId y materiaId son requeridos");
        }
        Planilla planilla = planillaDao.findByCompositeKey(cursoId, materiaId, etapa);
        if (planilla == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planilla no encontrada");
        }
        if (planilla.getProfesorId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta planilla");
        }
        return planilla;
    }

    private PlanillaDetailResponse buildPlanillaDetail(Planilla planilla) throws SQLException {
        Materia materia = null;
        if (planilla.getMateriaId() > 0) {
            materia = new MateriaDao().findById(planilla.getMateriaId());
            if (materia != null && materia.getNombre() != null && !materia.getNombre().isBlank()) {
                planilla.setNombre(materia.getNombre());
                planilla.setCategoria(materia.getCategoria());
            }
        }

        Curso curso = new CursoDao().findById(planilla.getCursoId());
        new RegistroDao().ensureRegistroRowsForPlanilla(planilla.getId(), planilla.getCursoId());

        List<Tarea> tareas = filterTasksByEtapa(new TareaDao().consultarTarea(planilla.getId()), planilla.getEtapaIndex());
        Map<Integer, Integer> tareaMax = new LinkedHashMap<>();
        int totalPossiblePoints = 0;
        LocalDate maxEnd = null;

        List<TareaDto> tareasDto = new ArrayList<>();
        for (Tarea t : tareas) {
            tareasDto.add(new TareaDto(
                    t.getId(),
                    t.getPlanillaId(),
                    t.getInstrumentoId(),
                    t.getFecha(),
                    t.getTotal(),
                    t.getTitulo(),
                    t.getFechaInicio(),
                    t.getFechaLimite(),
                    t.getGoogleCourseworkId(),
                    t.getGoogleCourseworkUrl()));
            tareaMax.put(t.getId(), t.getTotal());
            totalPossiblePoints += t.getTotal();

            if (t.getFechaLimite() != null && (maxEnd == null || t.getFechaLimite().isAfter(maxEnd))) {
                maxEnd = t.getFechaLimite();
            }
        }

        planilla.computeGradeRanges(totalPossiblePoints);
        List<StudentRow> rows = new StudentRowDao().loadRowsForPlanilla(planilla, tareaMax, totalPossiblePoints);

        List<StudentRowDto> rowsDto = new ArrayList<>();
        for (StudentRow row : rows) {
            List<GradeValueDto> gradeValues = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : row.getGrades().entrySet()) {
                gradeValues.add(new GradeValueDto(entry.getKey(), entry.getValue()));
            }
            rowsDto.add(new StudentRowDto(
                    row.getRegistroId(),
                    row.getAlumnoId(),
                    row.getAlumnoNombre(),
                    gradeValues,
                    row.getTotal(),
                    row.getPorcentaje(),
                    row.getNota()));
        }

        Map<String, GradeRangeDto> ranges = new LinkedHashMap<>();
        int li = planilla.getLimiteInferior();
        if (li > 0) {
            ranges.put("1", new GradeRangeDto(0, li - 1));
        }
        Map<Integer, int[]> computedRanges = planilla.getGradeRanges();
        if (computedRanges != null) {
            for (Map.Entry<Integer, int[]> entry : computedRanges.entrySet()) {
                int[] value = entry.getValue();
                if (value != null && value.length >= 2) {
                    ranges.put(String.valueOf(entry.getKey()), new GradeRangeDto(value[0], value[1]));
                }
            }
        }

        PlanillaHeaderDto header = new PlanillaHeaderDto(
                planilla.getId(),
                planilla.getCursoId(),
                planilla.getMateriaId(),
                planilla.getNombre(),
                planilla.getCategoria(),
                planilla.getEtapa(),
                planilla.getEtapaIndex(),
                planilla.getEtapaSugerida(),
                planilla.getPeriodo(),
                planilla.getProfesorId(),
                (int) Math.round(100 * planilla.getExigencia()),
                totalPossiblePoints,
                AcademicPeriod.etapaStartDate(
                        planilla.getPeriodo() > 0 ? planilla.getPeriodo() : AcademicPeriod.current(),
                        planilla.getEtapaIndex()),
                maxEnd,
                planilla.getFechaCierreEtapa1(),
                planilla.getEtapa1Confirmada(),
                planilla.getGoogleCourseId());

        CursoDto cursoDto = curso == null
                ? null
                : new CursoDto(curso.getId(), curso.getEspecialidad(), curso.getSeccion(), curso.getNivel());

        return new PlanillaDetailResponse(
                header,
                cursoDto,
                tareasDto,
                rowsDto,
                ranges,
                Collections.emptyList());
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

    private int resolveDefaultEtapa(LocalDate today) {
        return AcademicPeriod.etapaAt(today);
    }

    public record ResolvePlanillaRequest(Integer cursoId, Integer materiaId, Integer etapa) {
    }

    public record ResolvePlanillaResponse(int planillaId, boolean created, String location) {
    }

    public record SaveGradesRequest(List<AlumnoGradesInput> grades) {
    }

    public record AlumnoGradesInput(Integer alumnoId, List<GradeItemInput> items) {
    }

    public record GradeItemInput(Integer tareaId, Integer puntos) {
    }

    public record SaveGradesResponse(
            String message,
            int savedCount,
            int skippedCount,
            List<String> warnings,
            int planillaId) {
    }

        public record ClassroomSyncResponse(
            int planillaId,
            String googleCourseId,
            boolean classroomCourseMapped,
            int importedCourseworks,
            int linkedStudents,
            int importedGrades,
            String courseName,
            String courseSection,
            String courseAlternateLink,
            String message) {
        }

    public record PlanillaDetailResponse(
            PlanillaHeaderDto planilla,
            CursoDto curso,
            List<TareaDto> tareas,
            List<StudentRowDto> rows,
            Map<String, GradeRangeDto> gradeRanges,
            List<String> warnings) {
    }

    public record PlanillaHeaderDto(
            int id,
            int cursoId,
            int materiaId,
            String materiaNombre,
            String categoria,
            String etapa,
            int etapaIndex,
            int etapaSugerida,
            int periodo,
            int profesorId,
            int exigenciaPorcentaje,
            int totalPossiblePoints,
            LocalDate planillaDesde,
            LocalDate planillaHasta,
            LocalDate fechaCierreEtapa1,
            boolean etapa1Confirmada,
            String googleCourseId) {
    }

    public record CursoDto(int id, String especialidad, String seccion, int nivel) {
    }

    public record TareaDto(
            int id,
            int planillaId,
            int instrumentoId,
            LocalDate fecha,
            int total,
            String titulo,
            LocalDate fechaInicio,
            LocalDate fechaLimite,
            String googleCourseworkId,
            String googleCourseworkUrl) {
    }

    public record StudentRowDto(
            int registroId,
            int alumnoId,
            String alumnoNombre,
            List<GradeValueDto> grades,
            int total,
            int porcentaje,
            int nota) {
    }

    public record GradeValueDto(int tareaId, Integer puntos) {
    }

    public record GradeRangeDto(int minInclusive, int maxInclusive) {
    }

    public record ConfirmClassroomRequest(String googleCourseId) {}

    public record ConfirmClassroomResponse(String message, int planillaId, String googleCourseId) {}
}
