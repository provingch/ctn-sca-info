package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.ParentSummaryItem;
import ctn.informatica.sca.model.ParentTaskGrade;
import ctn.informatica.sca.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ParentServlet", urlPatterns = {"/padre"})
public class ParentServlet extends HttpServlet {

    Integer resolveSelectedAlumnoId(List<Alumno> hijos, Integer selectedAlumnoId) {
        if (hijos == null || hijos.isEmpty()) {
            return null;
        }
        if (selectedAlumnoId != null) {
            for (Alumno hijo : hijos) {
                if (hijo != null && hijo.getId() == selectedAlumnoId) {
                    return selectedAlumnoId;
                }
            }
        }
        return hijos.get(0).getId();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?notice=login-required");
            return;
        }

        try {
            PadreDao padreDao = new PadreDao();
            List<Alumno> hijos = padreDao.findChildrenByPadreId(user.getId());
            List<ParentSummaryItem> summary = padreDao.findParentSummary(user.getId());

            String alumnoIdParam = request.getParameter("alumnoId");
            String materiaIdParam = request.getParameter("materiaId");
            String planillaIdParam = request.getParameter("planillaId");

            Integer selectedAlumnoId = null;
            Integer selectedMateriaId = null;
            Integer selectedPlanillaId = null;

            if (alumnoIdParam != null && !alumnoIdParam.isBlank()) {
                selectedAlumnoId = Integer.parseInt(alumnoIdParam);
            }
            if (materiaIdParam != null && !materiaIdParam.isBlank()) {
                selectedMateriaId = Integer.parseInt(materiaIdParam);
            }
            if (planillaIdParam != null && !planillaIdParam.isBlank()) {
                selectedPlanillaId = Integer.parseInt(planillaIdParam);
            }

            selectedAlumnoId = resolveSelectedAlumnoId(hijos, selectedAlumnoId);

            List<ParentSummaryItem> selectedSummary = new ArrayList<>();
            String selectedEspecialidad = "general";
            for (ParentSummaryItem item : summary) {
                if (item.getAlumnoId() != null && item.getAlumnoId().equals(selectedAlumnoId)) {
                    selectedSummary.add(item);
                    if (item.getEspecialidadNombre() != null && !item.getEspecialidadNombre().isBlank()) {
                        selectedEspecialidad = item.getEspecialidadNombre();
                    }
                }
            }

            if (selectedAlumnoId != null) {
                for (Alumno hijo : hijos) {
                    if (hijo.getId() == selectedAlumnoId && hijo.getEspecialidadNombre() != null
                            && !hijo.getEspecialidadNombre().isBlank()) {
                        selectedEspecialidad = hijo.getEspecialidadNombre();
                        break;
                    }
                }
            }

            ParentSummaryItem selectedSubject = null;
            if (selectedPlanillaId != null) {
                for (ParentSummaryItem item : selectedSummary) {
                    if (item.getPlanillaId().equals(selectedPlanillaId)
                            && (selectedMateriaId == null || item.getMateriaId().equals(selectedMateriaId))) {
                        selectedSubject = item;
                        break;
                    }
                }
            }
            if (selectedSubject == null) {
                selectedPlanillaId = null;
                selectedMateriaId = null;
            } else {
                selectedPlanillaId = selectedSubject.getPlanillaId();
                selectedMateriaId = selectedSubject.getMateriaId();
            }

            List<ParentTaskGrade> tareasPorAlumno = new ArrayList<>();
            if (selectedAlumnoId != null && selectedPlanillaId != null) {
                tareasPorAlumno = padreDao.findTaskGradesForAlumnoPlanilla(selectedAlumnoId, selectedPlanillaId);
            }

            Map<Integer, Integer> promedioPorAlumno = new LinkedHashMap<>();
            Map<Integer, Integer> puntosPorAlumno = new LinkedHashMap<>();
            Map<Integer, Integer> totalPorAlumno = new LinkedHashMap<>();
            for (ParentSummaryItem item : summary) {
                int alumnoId = item.getAlumnoId();
                puntosPorAlumno.merge(alumnoId, item.getPuntos(), Integer::sum);
                totalPorAlumno.merge(alumnoId, item.getTotalPosible(), Integer::sum);
            }
            for (Alumno hijo : hijos) {
                int puntos = puntosPorAlumno.getOrDefault(hijo.getId(), 0);
                int total = totalPorAlumno.getOrDefault(hijo.getId(), 0);
                promedioPorAlumno.put(hijo.getId(), total > 0 ? (int) Math.round((puntos * 100.0) / total) : 0);
            }

            request.setAttribute("padre", session != null ? session.getAttribute("padre") : null);
            request.setAttribute("hijos", hijos);
            request.setAttribute("multipleHijos", hijos.size() > 1);
            request.setAttribute("selectedAlumnoId", selectedAlumnoId);
            request.setAttribute("selectedMateriaId", selectedMateriaId);
            request.setAttribute("selectedPlanillaId", selectedPlanillaId);
            request.setAttribute("selectedEspecialidad", selectedEspecialidad);
            request.setAttribute("selectedSummary", selectedSummary);
            request.setAttribute("selectedSubject", selectedSubject);
            request.setAttribute("promedioPorAlumno", promedioPorAlumno);
            request.setAttribute("tareasPorAlumno", tareasPorAlumno);
            request.getRequestDispatcher("/Parent.jsp").forward(request, response);
        } catch (SQLException ex) {
            throw new ServletException("Unable to load parent summary", ex);
        }
    }
}
