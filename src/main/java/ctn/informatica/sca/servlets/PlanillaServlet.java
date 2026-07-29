/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.GradeDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.dao.RegistroDao;
import ctn.informatica.sca.dao.StudentRowDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.google.GoogleClassroomService;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.ScaUiContext;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jonat
 */
@WebServlet(name = "PlanillaServlet", urlPatterns = {"/planilla"})
public class PlanillaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        String planillaIdStr = request.getParameter("planillaId");
        String cursoIdStr = request.getParameter("cursoId");
        String materiaIdStr = request.getParameter("materiaId");
        String etapaStr = request.getParameter("etapa");
        int etapa = resolveDefaultEtapa(LocalDate.now());
        Planilla planilla = null;
        PlanillaDao dao = new PlanillaDao();
        Profesor profesor = null;
        Materia materia = null;

        try {
            if (etapaStr != null && !etapaStr.trim().isEmpty()) {
                etapa = Integer.parseInt(etapaStr.trim());
            }

            if (!hasValidPlanillaIdentifier(planillaIdStr, cursoIdStr, materiaIdStr)) {
                response.sendRedirect(request.getContextPath() + "/inicio");
                return;
            }

            if (cursoIdStr != null && materiaIdStr != null
                    && !cursoIdStr.isEmpty() && !materiaIdStr.isEmpty()) {

                int cursoId = Integer.parseInt(cursoIdStr.trim());
                int materiaId = Integer.parseInt(materiaIdStr.trim());
                planilla = dao.findByCompositeKey(cursoId, materiaId, etapa);

                if (planilla == null) {
                    // Todavia no existe (p.ej. venimos de un bloque de Google Classroom
                    // recien detectado en Home.jsp): la generamos al vuelo, sin pasos
                    // manuales de por medio, y seguimos el flujo normal. El bloque de
                    // sincronizacion de Classroom mas abajo se encarga de poblarla.
                    if (user == null) {
                        response.sendRedirect(request.getContextPath() + "/index.jsp?notice=login-required");
                        return;
                    }
                    planilla = dao.crear(cursoId, materiaId, etapa, user.getId());
                    if (planilla == null) {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No se pudo crear la planilla");
                        return;
                    }
                }

            } else if (planillaIdStr != null && !planillaIdStr.trim().isEmpty()) {
                int planillaId = Integer.parseInt(planillaIdStr.trim());
                planilla = dao.findById(planillaId);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing planilla identifier");
                return;
            }

            if (planilla == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Planilla not found");
                return;
            }

            if (planilla.getProfesorId() != user.getId()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (session != null) {
                profesor = (Profesor) session.getAttribute("profesor");
            }
            if (profesor == null && user != null) {
                profesor = new ctn.informatica.sca.dao.ProfesorDao().findById(user.getId());
                if (session != null) {
                    session.setAttribute("profesor", profesor);
                }
            }

            if (planilla.getMateriaId() > 0) {
                materia = new MateriaDao().findById(planilla.getMateriaId());
                if (materia != null && materia.getNombre() != null && !materia.getNombre().isBlank()) {
                    planilla.setNombre(materia.getNombre());
                }
            }

            Curso curso = new CursoDao().findById(planilla.getCursoId());
            request.setAttribute("cursoSpecialty", ScaUiContext.normalizeSpecialty(curso != null ? curso.getEspecialidad() : null));
            request.setAttribute("pageTitle", resolvePlanillaPageTitle(planilla, materia));

            RegistroDao registroDao = new RegistroDao();
            registroDao.ensureRegistroRowsForPlanilla(planilla.getId(), planilla.getCursoId());

            List<Tarea> tareas = filterTasksByEtapa(new TareaDao().consultarTarea(planilla.getId()), planilla.getEtapaIndex());
            boolean shouldSyncClassroom = profesor != null
                    && GoogleClassroomService.isGoogleConnected(profesor)
                    && (tareas.isEmpty() || (planilla.getGoogleCourseId() == null || planilla.getGoogleCourseId().isBlank()));
            if (shouldSyncClassroom) {
                try {
                    Curso classroomCurso = new CursoDao().findById(planilla.getCursoId());
                    java.util.Optional<com.google.api.services.classroom.model.Course> matchingCourse =
                            GoogleClassroomService.resolveCourseForPlanilla(profesor, classroomCurso, planilla, dao);
                    if (matchingCourse.isPresent()) {
                        dao.updateClassroomCourseId(planilla.getId(), matchingCourse.get().getId());
                        planilla.setGoogleCourseId(matchingCourse.get().getId());
                        int importedCount = importCourseworkForPlanilla(profesor, planilla, matchingCourse.get());
                        List<Alumno> alumnos = new AlumnoDao().findByCursoId(planilla.getCursoId());
                        int linkedStudents = GoogleClassroomService.syncStudentIdentities(profesor, matchingCourse.get().getId(), alumnos);
                        int importedGrades = importGradesForPlanilla(profesor, planilla, matchingCourse.get(), alumnos);
                        if (importedCount > 0 || linkedStudents > 0 || importedGrades > 0) {
                            tareas = filterTasksByEtapa(new TareaDao().consultarTarea(planilla.getId()), planilla.getEtapaIndex());
                            request.setAttribute("classroomSyncMessage", "Se sincronizaron " + linkedStudents + " alumno(s) y se importaron " + importedCount + " tarea(s) / " + importedGrades + " nota(s) desde Classroom.");
                        }
                    }
                } catch (IOException ioe) {
                    log("Error auto-synchronizing Classroom coursework for planilla " + planilla.getId(), ioe);
                }
            }
            request.setAttribute("tareas", tareas);

            // compute planilla date range (desde/hasta) from task start/end dates
            LocalDate minStart = null;
            LocalDate maxEnd = null;
            for (Tarea t : tareas) {
                if (t.getFechaInicio() != null) {
                    if (minStart == null || t.getFechaInicio().isBefore(minStart)) {
                        minStart = t.getFechaInicio();
                    }
                }
                if (t.getFechaLimite() != null) {
                    if (maxEnd == null || t.getFechaLimite().isAfter(maxEnd)) {
                        maxEnd = t.getFechaLimite();
                    }
                }
            }
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            request.setAttribute("planillaDesde", minStart != null ? minStart.format(df) : "--");
            request.setAttribute("planillaHasta", maxEnd != null ? maxEnd.format(df) : "--");

            // Diagnostic output: if ?diag=1 is provided, return a small JSON
            String diag = request.getParameter("diag");
            if (diag != null && ("1".equals(diag) || "true".equalsIgnoreCase(diag))) {
                response.setContentType("application/json;charset=UTF-8");
                try (PrintWriter out = response.getWriter()) {
                    String safeName = planilla.getNombre() == null ? "" : planilla.getNombre().replace("\\", "\\\\").replace("\"", "\\\"");
                    out.print("{\n");
                    out.print("  \"params\": {\n");
                    out.print("    \"planillaId\": \"" + (planillaIdStr == null ? "" : planillaIdStr) + "\",\n");
                    out.print("    \"cursoId\": \"" + (cursoIdStr == null ? "" : cursoIdStr) + "\",\n");
                    out.print("    \"materiaId\": \"" + (materiaIdStr == null ? "" : materiaIdStr) + "\",\n");
                    out.print("    \"etapa\": \"" + (etapaStr == null ? "" : etapaStr) + "\"\n");
                    out.print("  },\n");
                    out.print("  \"planilla\": {\n");
                    out.print("    \"id\": " + planilla.getId() + ",\n");
                    out.print("    \"cursoId\": " + planilla.getCursoId() + ",\n");
                    out.print("    \"materiaId\": " + planilla.getMateriaId() + ",\n");
                    out.print("    \"nombre\": \"" + safeName + "\"\n");
                    out.print("  }\n");
                    out.print("}");
                }
                return;
            }
            // preserve curso/materia/etapa for UI convenience
            request.setAttribute("curso", curso);
            request.setAttribute("cursoId", planilla.getCursoId());
            request.setAttribute("materiaId", planilla.getMateriaId());
            request.setAttribute("etapa", planilla.getEtapaIndex());

            Map<Integer, Integer> tareaMax = new HashMap<>();
            int totalPossiblePoints = 0;
            for (Tarea t : tareas) {
                tareaMax.put(t.getId(), t.getTotal());
                totalPossiblePoints += t.getTotal();
            }
            request.setAttribute("totalPossiblePoints", totalPossiblePoints); // you can use this in the JSP
            planilla.computeGradeRanges(totalPossiblePoints);

            // Now load registros + puntajes and build rows
            List<StudentRow> rows = new StudentRowDao().loadRowsForPlanilla(planilla, tareaMax, totalPossiblePoints);
            Map<String, Integer[]> gradeRanges = planilla.getGradeRangesForJsp();
            
            int exigencia = (int) Math.round(100 * planilla.getExigencia());
            request.setAttribute("exigencia", exigencia);
            
            request.setAttribute("gradeRanges", gradeRanges);
            request.setAttribute("planilla", planilla);
            request.setAttribute("rows", rows);

            request.setAttribute("googleClassroomConnected", user != null && user.getId() > 0 && GoogleClassroomService.isGoogleConnected(profesor));

            request.getRequestDispatcher("/Planilla.jsp").forward(request, response);
        } catch (NumberFormatException nfe) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric parameter");
        } catch (SQLException sqle) {
            log("Error loading planilla for user " + user.getId() + " params: planillaId=" + planillaIdStr
                    + " cursoId=" + cursoIdStr + " materiaId=" + materiaIdStr, sqle);
            throw new ServletException("Unable to load planilla", sqle);
        }
    }

    private boolean hasValidPlanillaIdentifier(String planillaIdStr, String cursoIdStr, String materiaIdStr) {
        boolean hasPlanillaId = planillaIdStr != null && !planillaIdStr.trim().isEmpty();
        boolean hasCompositeKey = cursoIdStr != null && !cursoIdStr.trim().isEmpty()
                && materiaIdStr != null && !materiaIdStr.trim().isEmpty();
        return hasPlanillaId || hasCompositeKey;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        // Accept planilla by planillaId or composite key (same logic as doGet)
        String planillaIdStr = request.getParameter("planillaId");
        String cursoIdStr = request.getParameter("cursoId");
        String materiaIdStr = request.getParameter("materiaId");
        String etapaStr = request.getParameter("etapa");
        String syncAction = request.getParameter("syncAction");
        int etapa = resolveDefaultEtapa(LocalDate.now());
        Planilla planilla = null;
        PlanillaDao dao = new PlanillaDao();

        try {
            if (etapaStr != null && !etapaStr.trim().isEmpty()) {
                etapa = Integer.parseInt(etapaStr.trim());
            }

            if (!hasValidPlanillaIdentifier(planillaIdStr, cursoIdStr, materiaIdStr)) {
                response.sendRedirect(request.getContextPath() + "/inicio");
                return;
            }

            if (cursoIdStr != null && materiaIdStr != null
                    && !cursoIdStr.isEmpty() && !materiaIdStr.isEmpty()) {

                int cursoId = Integer.parseInt(cursoIdStr.trim());
                int materiaId = Integer.parseInt(materiaIdStr.trim());
                planilla = dao.findByCompositeKey(cursoId, materiaId, etapa);

            } else if (planillaIdStr != null && !planillaIdStr.trim().isEmpty()) {
                int planillaId = Integer.parseInt(planillaIdStr.trim());
                planilla = dao.findById(planillaId);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing planilla identifier");
                return;

            }
            if (planilla == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Planilla not found");
                return;
            }

            if (planilla.getProfesorId() != user.getId()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if ("manual".equals(syncAction)) {
                Profesor profesor = null;
                if (session != null) {
                    profesor = (Profesor) session.getAttribute("profesor");
                }
                if (profesor == null && user != null) {
                    profesor = new ctn.informatica.sca.dao.ProfesorDao().findById(user.getId());
                    if (session != null) {
                        session.setAttribute("profesor", profesor);
                    }
                }
                if (profesor != null && GoogleClassroomService.isGoogleConnected(profesor)) {
                    try {
                        Curso curso = new CursoDao().findById(planilla.getCursoId());
                        request.setAttribute("cursoSpecialty", ScaUiContext.normalizeSpecialty(curso != null ? curso.getEspecialidad() : null));
                        java.util.Optional<com.google.api.services.classroom.model.Course> matchingCourse =
                                GoogleClassroomService.resolveCourseForPlanilla(profesor, curso, planilla, dao);
                        if (matchingCourse.isPresent()) {
                        dao.updateClassroomCourseId(planilla.getId(), matchingCourse.get().getId());
                        int importedCount = importCourseworkForPlanilla(profesor, planilla, matchingCourse.get());
                        List<Alumno> alumnos = new AlumnoDao().findByCursoId(planilla.getCursoId());
                        int linkedStudents = GoogleClassroomService.syncStudentIdentities(profesor, matchingCourse.get().getId(), alumnos);
                        int importedGrades = importGradesForPlanilla(profesor, planilla, matchingCourse.get(), alumnos);
                        if (importedCount > 0 || linkedStudents > 0 || importedGrades > 0) {
                            session.setAttribute("flashMessage", "Sincronización manual completada. Curso Classroom asociado, " + linkedStudents + " alumno(s) vinculados y " + importedCount + " tarea(s) / " + importedGrades + " nota(s) importadas.");
                        } else {
                            session.setAttribute("flashMessage", "Sincronización manual completada. Curso Classroom asociado, pero no había tareas nuevas para importar.");
                        }
                    } else {
                        session.setAttribute("flashMessage", "Sincronización manual completada. No se encontró un curso coincidente en Classroom.");
                    }
                    } catch (IOException ioe) {
                        session.setAttribute("flashErrors", java.util.Collections.singletonList("No se pudo sincronizar con Google Classroom: " + ioe.getMessage()));
                        session.setAttribute("flashMessage", "La sincronización no pudo completarse.");
                    }
                } else {
                    session.setAttribute("flashErrors", java.util.Collections.singletonList("Debes conectar Google Classroom antes de sincronizar manualmente."));
                    session.setAttribute("flashMessage", "La sincronización no pudo completarse.");
                }
                response.sendRedirect(request.getContextPath() + "/planilla?planillaId=" + planilla.getId());
                return;
            }

            // Load tareas to know bounds per tarea
            List<Tarea> tareas = filterTasksByEtapa(new TareaDao().consultarTarea(planilla.getId()), planilla.getEtapaIndex());
            Map<Integer, Integer> tareaMax = new HashMap<>();
            for (Tarea t : tareas) {
                tareaMax.put(t.getId(), t.getTotal());
            }

            Map<Integer, Map<Integer, Integer>> gradesByAlumno = new HashMap<>();
            Pattern p = Pattern.compile("^grade_(\\d+)_(\\d+)$");
            Enumeration<String> names = request.getParameterNames();
            List<String> paramErrors = new ArrayList<>();

            while (names.hasMoreElements()) {
                String name = names.nextElement();
                Matcher m = p.matcher(name);
                if (!m.matches()) {
                    continue;
                }
                int alumnoId = Integer.parseInt(m.group(1));
                int tareaId = Integer.parseInt(m.group(2));
                String raw = request.getParameter(name);

                // If the parameter is not present at all (null), skip it.
                // If it's present but empty/whitespace => treat as explicit NULL (user cleared the field).
                if (raw == null) {
                    // parameter not present in form (shouldn't normally happen), skip
                    continue;
                }

                Integer puntos = 0;
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) {
                    // empty means no score entered yet -> treat as 0
                    puntos = 0;
                } else {
                    // try parse numeric value
                    try {
                        puntos = Integer.parseInt(trimmed);
                    } catch (NumberFormatException nfe) {
                        paramErrors.add("Valor no numérico para alumno " + alumnoId + " tarea " + tareaId);
                        // skip this entry (don't save it)
                        continue;
                    }

                    // validate bounds if tarea known
                    Integer max = tareaMax.get(tareaId);
                    if (max != null) {
                        if (puntos < 0) {
                            puntos = 0;
                        }
                        if (puntos > max) {
                            paramErrors.add("Puntos ajustados a máximo (" + max + ") para alumno " + alumnoId + " tarea " + tareaId);
                            puntos = max;
                        }
                    }
                }

                // store the value (may be null)
                gradesByAlumno.computeIfAbsent(alumnoId, k -> new HashMap<>())
                        .put(tareaId, puntos);
            }

            // If no grade_* parameters were present at all (no inputs), nothing to save
            if (gradesByAlumno.isEmpty()) {
                session.setAttribute("flashMessage", "No hay calificaciones para guardar.");
                response.sendRedirect(request.getContextPath() + "/planilla?planillaId=" + planilla.getId());
                return;
            }

            Set<Integer> alumnoIds = gradesByAlumno.keySet();
            RegistroDao registroDao = new RegistroDao();
            Map<Integer, Integer> alumnoToRegistro = registroDao.getRegistroIdsForPlanilla(planilla.getId(), alumnoIds);

            Map<Integer, Map<Integer, Integer>> gradesByRegistro = new HashMap<>();
            for (Map.Entry<Integer, Map<Integer, Integer>> entry : gradesByAlumno.entrySet()) {
                int alumnoId = entry.getKey();
                Integer registroId = alumnoToRegistro.get(alumnoId);
                if (registroId == null) {
                    paramErrors.add("Alumno " + alumnoId + " no está registrado en esta planilla; calificación omitida.");
                    continue;
                }
                gradesByRegistro.put(registroId, entry.getValue());
            }

            if (gradesByRegistro.isEmpty()) {
                session.setAttribute("flashMessage", "No se encontraron registros válidos para guardar (ver advertencias).");
                session.setAttribute("flashErrors", paramErrors);
                response.sendRedirect(request.getContextPath() + "/planilla?planillaId=" + planilla.getId());
                return;
            }

            GradeDao gradeDao = new GradeDao();
            try {
                gradeDao.saveGradesBatch(planilla.getId(), gradesByRegistro);
            } catch (SQLException sqle) {
                log("Error saving grades for planilla " + planilla.getId(), sqle);
                throw new ServletException("Error saving calificaciones", sqle);
            }

            String msg = "Cambios guardados correctamente.";
            if (!paramErrors.isEmpty()) {
                session.setAttribute("flashErrors", paramErrors);
                msg += " (con advertencias)";
            }
            session.setAttribute("flashMessage", msg);

            // PRG redirect back to GET view
            response.sendRedirect(request.getContextPath() + "/planilla?planillaId=" + planilla.getId());
        } catch (NumberFormatException nfe) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric parameter");
        } catch (SQLException sqle) {
            log("Database error in PlanillaServlet.doPost", sqle);
            throw new ServletException("Database error", sqle);
        }
    }

    private int importGradesForPlanilla(Profesor profesor, Planilla planilla, Course classroomCourse, List<Alumno> alumnos) {
        if (profesor == null || planilla == null || classroomCourse == null || classroomCourse.getId() == null || classroomCourse.getId().isBlank()) {
            return 0;
        }

        try {
            Map<String, Integer> studentLookup = GoogleClassroomService.linkStudentsForCourse(profesor, classroomCourse.getId(), alumnos);
            if (studentLookup.isEmpty()) {
                return 0;
            }

            TareaDao tareaDao = new TareaDao();
            List<Tarea> tareas = tareaDao.consultarTarea(planilla.getId());
            Map<String, Integer> tareaIdByGoogleCourseworkId = new HashMap<>();
            for (Tarea tarea : tareas) {
                if (tarea.getGoogleCourseworkId() != null && !tarea.getGoogleCourseworkId().isBlank()) {
                    tareaIdByGoogleCourseworkId.put(tarea.getGoogleCourseworkId(), tarea.getId());
                }
            }
            if (tareaIdByGoogleCourseworkId.isEmpty()) {
                return 0;
            }

            RegistroDao registroDao = new RegistroDao();
            GradeDao gradeDao = new GradeDao();
            Map<Integer, Map<Integer, Integer>> gradesByRegistro = new HashMap<>();
            int imported = 0;

            for (Tarea tarea : tareas) {
                if (tarea.getGoogleCourseworkId() == null || tarea.getGoogleCourseworkId().isBlank()) {
                    continue;
                }
                List<com.google.api.services.classroom.model.StudentSubmission> submissions = GoogleClassroomService.listStudentSubmissionsForCourseWork(profesor, classroomCourse.getId(), tarea.getGoogleCourseworkId());
                for (com.google.api.services.classroom.model.StudentSubmission submission : submissions) {
                    if (submission == null || submission.getUserId() == null || submission.getUserId().isBlank()) {
                        continue;
                    }
                    Integer alumnoId = studentLookup.get(submission.getUserId());
                    if (alumnoId == null) {
                        continue;
                    }
                    Map<Integer, Integer> alumnoRegistroIds = registroDao.getRegistroIdsForPlanilla(planilla.getId(), java.util.Set.of(alumnoId));
                    Integer registroId = alumnoRegistroIds.get(alumnoId);
                    if (registroId == null) {
                        continue;
                    }
                    Double assignedGrade = submission.getAssignedGrade();
                    if (assignedGrade == null) {
                        assignedGrade = submission.getDraftGrade();
                    }
                    if (assignedGrade == null) {
                        continue;
                    }
                    int puntos = (int) Math.round(assignedGrade);
                    if (tarea.getTotal() > 0) {
                        puntos = Math.max(0, Math.min(tarea.getTotal(), puntos));
                    }
                    gradesByRegistro.computeIfAbsent(registroId, k -> new HashMap<>())
                            .put(tarea.getId(), puntos);
                    imported++;
                }
            }

            if (!gradesByRegistro.isEmpty()) {
                gradeDao.saveGradesBatch(planilla.getId(), gradesByRegistro);
            }
            return imported;
        } catch (IOException | SQLException ex) {
            log("Error importing Classroom grades for planilla " + planilla.getId(), ex);
            return 0;
        }
    }

    private int importCourseworkForPlanilla(Profesor profesor, Planilla planilla, Course classroomCourse) {
        if (profesor == null || planilla == null || classroomCourse == null) {
            return 0;
        }

        try {
            TareaDao tareaDao = new TareaDao();
            Set<String> existingCourseworkIds = tareaDao.getGoogleCourseworkIdsForPlanilla(planilla.getId());
            List<CourseWork> courseWorks = GoogleClassroomService.listCourseWorkForCourse(profesor, classroomCourse.getId());
            if (courseWorks.isEmpty()) {
                return 0;
            }

            int imported = 0;
            int defaultInstrumentId = selectDefaultInstrumentId();
            for (CourseWork courseWork : courseWorks) {
                if (courseWork == null || courseWork.getId() == null) {
                    continue;
                }
                if (existingCourseworkIds.contains(courseWork.getId())) {
                    continue;
                }

                Tarea tarea = new Tarea();
                tarea.setPlanillaId(planilla.getId());
                tarea.setInstrumentoId(defaultInstrumentId);
                tarea.setTitulo(courseWork.getTitle() != null && !courseWork.getTitle().isBlank()
                        ? courseWork.getTitle()
                        : "Tarea Classroom");
                tarea.setFecha(resolveCourseWorkDate(courseWork));
                tarea.setFechaInicio(resolveCourseWorkStartDate(courseWork));
                tarea.setFechaLimite(resolveCourseWorkDueDate(courseWork));
                tarea.setTotal(resolveCourseWorkTotal(courseWork));
                tarea.setGoogleCourseworkId(courseWork.getId());
                tarea.setGoogleCourseworkUrl(resolveCourseWorkUrl(courseWork));

                tareaDao.insertarTarea(tarea);
                imported++;
            }
            return imported;
        } catch (IOException | SQLException ex) {
            log("Error importing Classroom coursework for planilla " + planilla.getId(), ex);
            return 0;
        }
    }

    private String resolvePlanillaPageTitle(Planilla planilla, Materia materia) {
        if (materia != null && materia.getNombre() != null && !materia.getNombre().isBlank()) {
            return materia.getNombre().trim();
        }
        if (planilla != null && planilla.getNombre() != null && !planilla.getNombre().isBlank()) {
            return planilla.getNombre().trim();
        }
        return "Planilla";
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
        if (today == null) {
            return 1;
        }
        LocalDate transition = LocalDate.of(today.getYear(), 7, 15);
        return today.isBefore(transition) ? 1 : 2;
    }

    private int selectDefaultInstrumentId() {
        try {
            ctn.informatica.sca.dao.InstrumentoDao instrumentoDao = new ctn.informatica.sca.dao.InstrumentoDao();
            var instrumentos = instrumentoDao.findAll();
            if (instrumentos == null || instrumentos.isEmpty()) {
                return 1;
            }
            for (var ins : instrumentos) {
                String nombre = ins.getNombre() != null ? ins.getNombre().toLowerCase() : "";
                if (nombre.contains("prueba")) {
                    return ins.getId();
                }
            }
            for (var ins : instrumentos) {
                String nombre = ins.getNombre() != null ? ins.getNombre().toLowerCase() : "";
                if (nombre.contains("trabajo") || nombre.contains("fichas")) {
                    return ins.getId();
                }
            }
            return instrumentos.get(0).getId();
        } catch (SQLException ex) {
            log("Unable to select default instrument id", ex);
            return 1;
        }
    }

    private LocalDate resolveCourseWorkDate(CourseWork courseWork) {
        LocalDate due = resolveCourseWorkDueDate(courseWork);
        return due != null ? due : LocalDate.now();
    }

    private LocalDate resolveCourseWorkStartDate(CourseWork courseWork) {
        if (courseWork == null) {
            return null;
        }
        String scheduledDate = courseWork.getScheduledTime();
        if (scheduledDate != null && !scheduledDate.isBlank()) {
            try {
                return java.time.OffsetDateTime.parse(scheduledDate).toLocalDate();
            } catch (Exception ex) {
                try {
                    return java.time.Instant.parse(scheduledDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private LocalDate resolveCourseWorkDueDate(CourseWork courseWork) {
        if (courseWork == null) {
            return null;
        }
        var dueDate = courseWork.getDueDate();
        if (dueDate != null && dueDate.getYear() != null && dueDate.getMonth() != null && dueDate.getDay() != null) {
            try {
                return LocalDate.of(dueDate.getYear(), dueDate.getMonth(), dueDate.getDay());
            } catch (Exception ex) {
                // fallback to today
            }
        }
        return null;
    }

    private String resolveCourseWorkUrl(CourseWork courseWork) {
        if (courseWork == null) {
            return null;
        }
        if (courseWork.getAlternateLink() != null && !courseWork.getAlternateLink().isBlank()) {
            return courseWork.getAlternateLink();
        }
        if (courseWork.getId() != null && !courseWork.getId().isBlank()) {
            return "https://classroom.google.com/c/" + courseWork.getId();
        }
        return null;
    }

    private int resolveCourseWorkTotal(CourseWork courseWork) {
        if (courseWork == null) {
            return 10;
        }
        Number maxPoints = courseWork.getMaxPoints();
        if (maxPoints != null) {
            int total = maxPoints.intValue();
            return total > 0 ? total : 10;
        }
        return 10;
    }
}