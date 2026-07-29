/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.google.GoogleClassroomService;
import ctn.informatica.sca.google.GoogleClassroomUtils;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Instrumento;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.RasgoAsistencia;
import ctn.informatica.sca.model.RasgoPlanilla;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.ScaUiContext;
import com.google.api.services.classroom.model.Course;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author jonat
 */
@WebServlet(name = "HomeServlet", urlPatterns = {"/inicio"})
public class HomeServlet extends HttpServlet {

    private static final String VIEW_RASGOS = "rasgos";
    private static final String VIEW_CLASE = "clase";
    private static final String VIEW_RASGOS_FORM = "rasgos-form";
    private static final String VIEW_PLANILLAS = "planillas";
    private static final String ACTION_CREATE_RASGO = "create-rasgo-planilla";
    private static final String ACTION_SUBMIT_RASGO = "submit-rasgo-asistencia";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = safeTrim(request.getParameter("action"));
        if (ACTION_SUBMIT_RASGO.equals(action)) {
            submitRasgoAsistencia(request, response);
            return;
        }
        if (ACTION_CREATE_RASGO.equals(action)) {
            createRasgoPlanilla(request, response);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/inicio");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String requestedView = safeTrim(request.getParameter("view"));
        if (VIEW_RASGOS_FORM.equals(requestedView)) {
            renderRasgoFormView(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?notice=login-required");
            return;
        }

        String viewMode = resolveViewMode(requestedView, user);

        ArrayList<Curso> cursos;
        try {
            cursos = new CursoDao().consultarCursos(user.getId());
        } catch (SQLException sqle) {
            log("Error loading cursos for user " + user.getId(), sqle);
            throw new ServletException("Unable to load planillas", sqle);
        }
        String cursoIdStr = request.getParameter("cursoId");
        String etapaStr = request.getParameter("etapa");

        Curso selectedCurso = null;
        int selectedEtapa = 1;

        if (cursoIdStr != null && !cursoIdStr.trim().isEmpty()) {
            try {
                int id = Integer.parseInt(cursoIdStr.trim());
                for (Curso c : cursos) {// TODO get rid of unnecessary loop
                    if (c.getId() == id) {
                        selectedCurso = c;
                        break;
                    }
                }
            } catch (NumberFormatException ex) {
            }
        }
        if (selectedCurso == null && !cursos.isEmpty()) {
            selectedCurso = cursos.get(0);
        }
        if (selectedCurso != null && session != null) {
            session.setAttribute("scaSpecialty", ScaUiContext.normalizeSpecialty(selectedCurso.getEspecialidad()));
            session.setAttribute("scaSpecialtyName", selectedCurso.getEspecialidad());
        }

        if (etapaStr != null && !etapaStr.trim().isEmpty()) {
            try {
                int parsedEtapa = Integer.parseInt(etapaStr.trim());
                if (parsedEtapa == 1 || parsedEtapa == 2) {
                    selectedEtapa = parsedEtapa;
                }
            } catch (NumberFormatException ex) {
            }
        }

        List<Course> googleClassroomCourses = Collections.emptyList();
        boolean googleClassroomConnected = false;
        String googleClassroomError = null;
        String googleClassroomPlaceholder = null;
        String googleClassroomVisibilityNotice = null;

        Profesor profesor = new ProfesorDao().findById(user.getId());
        if (profesor != null) {
            googleClassroomConnected = GoogleClassroomService.isGoogleConnected(profesor);
        }

        List<String> teacherSubjects = new ArrayList<>();
        String manualTeacherSubjectsText = "";
        if (profesor != null) {
            try {
                teacherSubjects.addAll(new PlanillaDao().findSubjectsByProfesor(profesor.getId()));
                teacherSubjects.addAll(new MateriaDao().findNamesByProfesor(profesor.getId()));
            } catch (SQLException sqle) {
                log("Error loading teacher subjects for user " + user.getId(), sqle);
            }
            manualTeacherSubjectsText = new ProfesorDao().findManualSubjectsText(profesor.getId());
        }
        if (session != null) {
            Object manualTeacherSubjects = session.getAttribute("manualTeacherSubjects");
            if (manualTeacherSubjects instanceof String && !((String) manualTeacherSubjects).trim().isEmpty()) {
                manualTeacherSubjectsText = (String) manualTeacherSubjects;
            }
        }
        for (String subject : parseManualSubjects(manualTeacherSubjectsText)) {
            if (!teacherSubjects.contains(subject)) {
                teacherSubjects.add(subject);
            }
        }

        List<Curso> classroomSelectionContext = cursos;
        if (selectedCurso != null) {
            classroomSelectionContext = new ArrayList<>();
            classroomSelectionContext.add(selectedCurso);
        }

        if (googleClassroomConnected) {
            try {
                googleClassroomCourses = GoogleClassroomService.listAllowedCourses(profesor, classroomSelectionContext, teacherSubjects);
                // Diagnostic logging: print parsed course key and whether it matches the selected curso
                try {
                    for (com.google.api.services.classroom.model.Course course : googleClassroomCourses) {
                        java.util.Optional<GoogleClassroomUtils.CourseKey> key = GoogleClassroomService.parseCourseKey(course.getName(), course.getRoom());
                        boolean matches = GoogleClassroomService.courseMatchesTeacherCurso(course, classroomSelectionContext);
                        if (key.isPresent()) {
                            GoogleClassroomUtils.CourseKey k = key.get();
                            log("[GC DEBUG] courseId=" + course.getId() + " name='" + course.getName() + "' room='" + course.getRoom() + "' parsedKey=" + k.toString() + " matchesSelected=" + matches);
                        } else {
                            log("[GC DEBUG] courseId=" + course.getId() + " name='" + course.getName() + "' room='" + course.getRoom() + "' parsedKey=NONE matchesSelected=" + matches);
                        }
                    }
                } catch (Exception exx) {
                    log("[GC DEBUG] unable to log classroom diagnostics: " + exx.getMessage());
                }
            } catch (Exception ex) {
                log("Error loading Google Classroom courses for user " + user.getId(), ex);
                googleClassroomError = "No se pudieron cargar los cursos de Google Classroom: " + ex.getMessage();
            }
        } else {
            googleClassroomPlaceholder = "Conecte su classroom y vuelva a intentarlo";
        }

        if (selectedCurso != null) {
            ArrayList<Planilla> planillas;
            try {
                planillas = new PlanillaDao()
                        .consultarPlanillas(user.getId(), selectedCurso.getId(), selectedEtapa);
            } catch (SQLException sqle) {
                log("Error loading planillas for user " + user.getId()
                        + ", curso " + selectedCurso.getId()
                        + ", etapa " + selectedEtapa, sqle);

                throw new ServletException("Unable to load planillas", sqle);
            }

            // Map Classroom courseId -> planillaId and materiaId for quick linking from Home.jsp
            Map<String, Integer> classroomPlanillaMap = new HashMap<>();
            Map<String, Integer> classroomPlanillaMateriaMap = new HashMap<>();
            if (profesor != null && googleClassroomConnected) {
                for (Planilla p : planillas) {
                    try {
                        Optional<Course> resolved = GoogleClassroomService.resolveCourseForPlanilla(profesor, selectedCurso, p, new PlanillaDao());
                        if (resolved.isPresent()) {
                            Course c = resolved.get();
                            if (c.getId() != null && !c.getId().isBlank()) {
                                classroomPlanillaMap.put(c.getId(), p.getId());
                                classroomPlanillaMateriaMap.put(c.getId(), p.getMateriaId());
                            }
                        }
                    } catch (IOException ioe) {
                        log("Error resolving Classroom course for planilla id=" + p.getId() + ": " + ioe.getMessage());
                    }
                }

            }

            // Para los cursos de Classroom que no matchearon ninguna planilla ya
            // existente, tratamos de reconocer su materia (entre las que el
            // profesor ya dicta) por el nombre del curso, así el bloque igual
            // puede llevar a PlanillaServlet (que la crea al vuelo) en lugar de
            // mandar directo a Classroom. Solo se resuelven materias ya conocidas
            // del profesor; si el nombre es ambiguo o desconocido, no se adivina.
            if (profesor != null && googleClassroomConnected && !googleClassroomCourses.isEmpty()) {
                List<Materia> materiasProfesor = null;
                for (Course course : googleClassroomCourses) {
                    if (course.getId() == null || course.getId().isBlank()
                            || classroomPlanillaMateriaMap.containsKey(course.getId())) {
                        continue;
                    }
                    if (materiasProfesor == null) {
                        try {
                            materiasProfesor = new MateriaDao().listByProfesor(profesor.getId());
                        } catch (SQLException sqle) {
                            log("Error loading materias for profesor " + profesor.getId(), sqle);
                            materiasProfesor = Collections.emptyList();
                        }
                    }
                    GoogleClassroomService.resolveMateriaForCourse(course, materiasProfesor)
                            .ifPresent(m -> classroomPlanillaMateriaMap.put(course.getId(), m.getId()));
                }
            }

            if (googleClassroomConnected && !googleClassroomCourses.isEmpty() && selectedCurso != null
                    && classroomPlanillaMateriaMap.isEmpty()) {
                googleClassroomVisibilityNotice = "Se encontraron cursos en Google Classroom, pero ninguno pudo asociarse automáticamente a este curso. Revisa el nombre del curso en Classroom o vincúlalo manualmente.";
            }

            List<Materia> materiasDetectadas = Collections.emptyList();
            if (profesor != null) {
                try {
                    materiasDetectadas = new PlanillaDao().findMateriasSinPlanilla(user.getId(), selectedCurso.getId(), selectedEtapa);
                } catch (SQLException sqle) {
                    log("Error loading materias sin planilla for user " + user.getId()
                            + ", curso " + selectedCurso.getId()
                            + ", etapa " + selectedEtapa, sqle);
                    materiasDetectadas = Collections.emptyList();
                }
            }

            request.setAttribute("planillas", planillas);
            request.setAttribute("showPlanillaCards", shouldRenderPlanillaCards(planillas));
            request.setAttribute("classroomPlanillaMap", classroomPlanillaMap);
            request.setAttribute("classroomPlanillaMateriaMap", classroomPlanillaMateriaMap);
            request.setAttribute("matchedPlanillaIds", Collections.emptySet());
            request.setAttribute("materiasDetectadas", materiasDetectadas);

            if (VIEW_CLASE.equals(viewMode)) {
                loadRasgosViewData(request, user, selectedCurso);
            }
        } else {
            request.setAttribute("planillas", Collections.emptyList());
            request.setAttribute("showPlanillaCards", false);
            request.setAttribute("classroomPlanillaMap", Collections.emptyMap());
            request.setAttribute("classroomPlanillaMateriaMap", Collections.emptyMap());
            request.setAttribute("matchedPlanillaIds", Collections.emptySet());
            request.setAttribute("materiasDetectadas", Collections.emptyList());
            request.setAttribute("rasgoPlanillas", Collections.emptyList());
            request.setAttribute("rasgoAsistencias", Collections.emptyList());
            request.setAttribute("rasgoAlumnosInvalidos", Collections.emptyList());
            request.setAttribute("rasgoAlumnosValidos", Collections.emptyList());
            request.setAttribute("instrumentos", Collections.emptyList());
        }

        request.setAttribute("cursos", cursos);
        request.setAttribute("selCurso", selectedCurso);
        request.setAttribute("selEtapa", selectedEtapa);
        request.setAttribute("googleClassroomConnected", googleClassroomConnected);
        request.setAttribute("googleClassroomCourses", googleClassroomCourses);
        request.setAttribute("googleClassroomError", googleClassroomError);
        request.setAttribute("googleClassroomPlaceholder", googleClassroomPlaceholder);
        request.setAttribute("googleClassroomVisibilityNotice", googleClassroomVisibilityNotice);
        request.setAttribute("viewMode", viewMode);
        if (VIEW_CLASE.equals(viewMode)) {
            request.getRequestDispatcher("/InicioClase.jsp").forward(request, response);
            return;
        }

        request.getRequestDispatcher("/Home.jsp").forward(request, response);

    }

    static boolean shouldRenderPlanillaCards(List<Planilla> planillas) {
        return planillas != null && !planillas.isEmpty();
    }

    private List<String> parseManualSubjects(String rawSubjects) {
        if (rawSubjects == null || rawSubjects.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> subjects = new ArrayList<>();
        for (String token : rawSubjects.split("[,\r\n;]+")) {
            String normalized = token == null ? "" : token.trim();
            if (!normalized.isEmpty() && !subjects.contains(normalized)) {
                subjects.add(normalized);
            }
        }
        return subjects;
    }

    private void createRasgoPlanilla(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?notice=login-required");
            return;
        }

        int cursoId = parseIntOrDefault(request.getParameter("cursoId"), 0);
        if (cursoId <= 0) {
            cursoId = parseIntOrDefault(request.getParameter("formCursoId"), 0);
        }
        int etapa = parseIntOrDefault(request.getParameter("etapa"), 1);
        int instrumentoId = parseIntOrDefault(request.getParameter("instrumentoId"), 0);
        String turno = safeTrim(request.getParameter("turno"));
        String tema = safeTrim(request.getParameter("tema"));
        if (cursoId <= 0 || tema.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/inicio?view=" + VIEW_CLASE
                    + "&cursoId=" + cursoId + "&etapa=" + etapa + "&rasgoError=tema");
            return;
        }

        try {
            List<Alumno> alumnos = new AlumnoDao().findByCursoId(cursoId);
            Set<Integer> ausentes = parseIntegerSet(request.getParameterValues("alumnosAusentes"));
            List<Alumno> elegibles = new ArrayList<>();
            for (Alumno alumno : alumnos) {
                if (isCompleteName(alumno)) {
                    elegibles.add(alumno);
                }
            }

            if (elegibles.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/inicio?view=" + VIEW_CLASE
                        + "&cursoId=" + cursoId + "&etapa=" + etapa + "&rasgoError=sin-alumnos");
                return;
            }

            String temaPersistido = composeTemaConContexto(instrumentoId, turno, tema);
            int planillaRasgoId = new RasgoPlanillaDao().crearPlanillaRasgo(cursoId, user.getId(), temaPersistido, elegibles, ausentes);
            response.sendRedirect(request.getContextPath() + "/inicio?view=" + VIEW_CLASE
                    + "&cursoId=" + cursoId + "&etapa=" + etapa + "&rasgoPlanillaId=" + planillaRasgoId + "&rasgoOk=created");
        } catch (SQLException ex) {
            log("Error creating rasgo planilla", ex);
            throw new ServletException("No se pudo crear la planilla de rasgos", ex);
        }
    }

    private void submitRasgoAsistencia(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        int asistenciaId = parseIntOrDefault(request.getParameter("asistenciaId"), 0);
        String estadoRaw = safeTrim(request.getParameter("estado"));
        String estado = "presente".equalsIgnoreCase(estadoRaw) ? "presente" : "ausente";
        if (asistenciaId <= 0) {
            response.sendRedirect(request.getContextPath() + "/inicio?view=" + VIEW_RASGOS_FORM + "&error=id");
            return;
        }
        try {
            new RasgoPlanillaDao().registrarRespuesta(asistenciaId, estado);
            response.sendRedirect(request.getContextPath() + "/inicio?view=" + VIEW_RASGOS_FORM
                    + "&asistenciaId=" + asistenciaId + "&ok=1");
        } catch (SQLException ex) {
            log("Error saving rasgo attendance response", ex);
            throw new ServletException("No se pudo registrar la asistencia", ex);
        }
    }

    private void renderRasgoFormView(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int asistenciaId = parseIntOrDefault(request.getParameter("asistenciaId"), 0);
        if (asistenciaId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "asistenciaId requerido");
            return;
        }
        try {
            RasgoAsistencia asistencia = new RasgoPlanillaDao().findAsistenciaById(asistenciaId);
            if (asistencia == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Formulario no encontrado");
                return;
            }
            request.setAttribute("rasgoAsistencia", asistencia);
            request.setAttribute("rasgoSubmitSuccess", "1".equals(request.getParameter("ok")));
            request.getRequestDispatcher("/RasgoForm.jsp").forward(request, response);
        } catch (SQLException ex) {
            log("Error loading rasgo form", ex);
            throw new ServletException("No se pudo cargar el formulario", ex);
        }
    }

    private void loadRasgosViewData(HttpServletRequest request, User user, Curso selectedCurso) {
        try {
            List<Alumno> alumnosCurso = new AlumnoDao().findByCursoId(selectedCurso.getId());
            List<Alumno> alumnosValidos = new ArrayList<>();
            List<Alumno> alumnosInvalidos = new ArrayList<>();
            for (Alumno alumno : alumnosCurso) {
                if (isCompleteName(alumno)) {
                    alumnosValidos.add(alumno);
                } else {
                    alumnosInvalidos.add(alumno);
                }
            }

            RasgoPlanillaDao rasgoDao = new RasgoPlanillaDao();
            List<RasgoPlanilla> planillas = rasgoDao.listarPorProfesorCurso(user.getId(), selectedCurso.getId());
            int selectedPlanillaId = parseIntOrDefault(request.getParameter("rasgoPlanillaId"), 0);
            RasgoPlanilla selectedPlanilla = null;
            if (selectedPlanillaId > 0) {
                for (RasgoPlanilla planilla : planillas) {
                    if (planilla.getId() == selectedPlanillaId) {
                        selectedPlanilla = planilla;
                        break;
                    }
                }
            }
            if (selectedPlanilla == null && !planillas.isEmpty()) {
                selectedPlanilla = planillas.get(0);
            }

            List<RasgoAsistencia> asistencias = Collections.emptyList();
            if (selectedPlanilla != null) {
                asistencias = rasgoDao.listarAsistencias(selectedPlanilla.getId());
            }

            request.setAttribute("rasgoPlanillas", planillas);
            request.setAttribute("rasgoPlanillaSeleccionada", selectedPlanilla);
            request.setAttribute("rasgoAsistencias", asistencias);
            request.setAttribute("rasgoAlumnosValidos", alumnosValidos);
            request.setAttribute("rasgoAlumnosInvalidos", alumnosInvalidos);
            request.setAttribute("instrumentos", loadInstrumentos());
        } catch (SQLException ex) {
            log("Error loading rasgos data", ex);
            request.setAttribute("rasgoPlanillas", Collections.emptyList());
            request.setAttribute("rasgoPlanillaSeleccionada", null);
            request.setAttribute("rasgoAsistencias", Collections.emptyList());
            request.setAttribute("rasgoAlumnosValidos", Collections.emptyList());
            request.setAttribute("rasgoAlumnosInvalidos", Collections.emptyList());
            request.setAttribute("instrumentos", Collections.emptyList());
            request.setAttribute("rasgoErrorMessage", "No se pudo cargar la planilla de rasgos");
        }
    }

    private String resolveViewMode(String requestedView, User user) {
        if (VIEW_CLASE.equals(requestedView)) {
            return VIEW_CLASE;
        }
        if (VIEW_PLANILLAS.equals(requestedView)) {
            return VIEW_PLANILLAS;
        }
        if (VIEW_RASGOS.equals(requestedView)) {
            return VIEW_CLASE;
        }
        return VIEW_PLANILLAS;
    }

    private int parseIntOrDefault(String rawValue, int defaultValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isCompleteName(Alumno alumno) {
        if (alumno == null) {
            return false;
        }
        String nombre = safeTrim(alumno.getNombre());
        String apellido = safeTrim(alumno.getApellido());
        return !nombre.isEmpty() && !apellido.isEmpty() && nombre.length() >= 2 && apellido.length() >= 2;
    }

    private List<Instrumento> loadInstrumentos() {
        try {
            return new InstrumentoDao().findAll();
        } catch (SQLException ex) {
            log("Error loading instrumentos", ex);
            return Collections.emptyList();
        }
    }

    private Set<Integer> parseIntegerSet(String[] rawValues) {
        if (rawValues == null || rawValues.length == 0) {
            return Collections.emptySet();
        }
        return java.util.Arrays.stream(rawValues)
                .map(v -> parseIntOrDefault(v, 0))
                .filter(v -> v > 0)
                .collect(Collectors.toSet());
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

}
