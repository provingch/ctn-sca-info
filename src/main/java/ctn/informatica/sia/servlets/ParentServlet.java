package ctn.informatica.sia.servlets;

import ctn.informatica.sia.dao.PadreDao;
import ctn.informatica.sia.model.Alumno;
import ctn.informatica.sia.model.ParentSummaryItem;
import ctn.informatica.sia.model.ParentTaskGrade;
import ctn.informatica.sia.model.User;
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

@WebServlet(name = "ParentServlet", urlPatterns = {"/ParentServlet"})
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

            if (selectedPlanillaId == null && selectedAlumnoId != null && !summary.isEmpty()) {
                for (ParentSummaryItem item : summary) {
                    if (item.getAlumnoId() != null && item.getAlumnoId() == selectedAlumnoId) {
                        selectedPlanillaId = item.getPlanillaId();
                        selectedMateriaId = item.getMateriaId();
                        break;
                    }
                }
            }

            List<ParentTaskGrade> tareasPorAlumno = new ArrayList<>();
            if (selectedAlumnoId != null && selectedPlanillaId != null) {
                tareasPorAlumno = padreDao.findTaskGradesForAlumnoPlanilla(selectedAlumnoId, selectedPlanillaId);
            }

            Map<String, List<ParentSummaryItem>> summaryByEspecialidad = new LinkedHashMap<>();
            for (ParentSummaryItem item : summary) {
                String especialidad = item.getEspecialidadNombre() == null || item.getEspecialidadNombre().isBlank()
                        ? "Sin especialidad"
                        : item.getEspecialidadNombre();
                summaryByEspecialidad.computeIfAbsent(especialidad, key -> new ArrayList<>()).add(item);
            }

            request.setAttribute("padre", session != null ? session.getAttribute("padre") : null);
            request.setAttribute("hijos", hijos);
            request.setAttribute("selectedAlumnoId", selectedAlumnoId);
            request.setAttribute("selectedMateriaId", selectedMateriaId);
            request.setAttribute("selectedPlanillaId", selectedPlanillaId);
            request.setAttribute("summary", summary);
            request.setAttribute("summaryByEspecialidad", summaryByEspecialidad);
            request.setAttribute("tareasPorAlumno", tareasPorAlumno);
            request.getRequestDispatcher("/Parent.jsp").forward(request, response);
        } catch (SQLException ex) {
            throw new ServletException("Unable to load parent summary", ex);
        }
    }
}
