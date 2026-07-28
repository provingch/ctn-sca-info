/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sia.servlets;

import ctn.informatica.sia.dao.CursoDao;
import ctn.informatica.sia.dao.MateriaDao;
import ctn.informatica.sia.dao.PlanillaDao;
import ctn.informatica.sia.dao.ProfesorDao;
import ctn.informatica.sia.google.GoogleClassroomService;
import ctn.informatica.sia.google.GoogleClassroomUtils;
import ctn.informatica.sia.model.Curso;
import ctn.informatica.sia.model.Materia;
import ctn.informatica.sia.model.Planilla;
import ctn.informatica.sia.model.Profesor;
import ctn.informatica.sia.model.User;
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

/**
 *
 * @author jonat
 */
@WebServlet(name = "HomeServlet", urlPatterns = {"/HomeServlet"})
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?notice=login-required");
            return;
        }

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
        } else {
            request.setAttribute("planillas", Collections.emptyList());
            request.setAttribute("showPlanillaCards", false);
            request.setAttribute("classroomPlanillaMap", Collections.emptyMap());
            request.setAttribute("classroomPlanillaMateriaMap", Collections.emptyMap());
            request.setAttribute("matchedPlanillaIds", Collections.emptySet());
            request.setAttribute("materiasDetectadas", Collections.emptyList());
        }

        request.setAttribute("cursos", cursos);
        request.setAttribute("selCurso", selectedCurso);
        request.setAttribute("selEtapa", selectedEtapa);
        request.setAttribute("googleClassroomConnected", googleClassroomConnected);
        request.setAttribute("googleClassroomCourses", googleClassroomCourses);
        request.setAttribute("googleClassroomError", googleClassroomError);
        request.setAttribute("googleClassroomPlaceholder", googleClassroomPlaceholder);
        request.setAttribute("googleClassroomVisibilityNotice", googleClassroomVisibilityNotice);

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

}