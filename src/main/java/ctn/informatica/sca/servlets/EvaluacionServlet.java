/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.ScaUiContext;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
/**
 *
 * @author jonat
 */
@WebServlet(name = "EvaluacionServlet", urlPatterns = {"/EvaluacionServlet"})
public class EvaluacionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        try {
            List<Especialidad> especialidades = new EspecialidadDao().findAll();
            request.setAttribute("especialidades", especialidades);

            Especialidad selected = null;
            String selId = request.getParameter("especialidad");
            if ((selId == null || selId.isBlank()) && session != null) {
                Object savedId = session.getAttribute("scaSpecialtyId");
                if (savedId != null) {
                    selId = String.valueOf(savedId);
                }
            }
            if (selId != null && !selId.isBlank()) {
                try {
                    int id = Integer.parseInt(selId);
                    selected = new EspecialidadDao().findById(id);
                } catch (NumberFormatException ignored) {
                }
            }
            if (selected == null && session != null) {
                Object savedToken = session.getAttribute("scaSpecialty");
                if (savedToken != null && !"general".equals(savedToken)) {
                    String normalizedSaved = ScaUiContext.normalizeSpecialty(String.valueOf(savedToken));
                    for (Especialidad especialidad : especialidades) {
                        if (especialidad != null
                                && normalizedSaved.equals(ScaUiContext.normalizeSpecialty(especialidad.getNombre()))) {
                            selected = especialidad;
                            break;
                        }
                    }
                }
            }
            if (selected != null) {
                request.setAttribute("selEspecialidad", selected);
                if (session != null) {
                    session.setAttribute("scaSpecialty", ScaUiContext.normalizeSpecialty(selected.getNombre()));
                    session.setAttribute("scaSpecialtyId", selected.getId());
                    session.setAttribute("scaSpecialtyName", selected.getNombre());
                }
            }

        } catch (Exception ex) {
            throw new ServletException("Error loading especialidades", ex);
        }

        request.getRequestDispatcher("/Evaluacion.jsp").forward(request, response);
        
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    }

}
