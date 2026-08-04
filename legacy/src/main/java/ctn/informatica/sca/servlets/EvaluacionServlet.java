/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.RasgoPlanillaDao;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.RasgoPlanilla;
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
@WebServlet(name = "EvaluacionServlet", urlPatterns = {"/evaluacion"})
public class EvaluacionServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);

        try {
            List<Especialidad> especialidades = new EspecialidadDao().findAll();
            request.setAttribute("especialidades", especialidades);

            Especialidad selected = null;
            Curso selectedCurso = null;
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

            String cursoParam = request.getParameter("curso");
            String seccionParam = request.getParameter("seccion");
            if (selected != null && cursoParam != null && !cursoParam.isBlank() && seccionParam != null && !seccionParam.isBlank()) {
                try {
                    int cursoNivel = Integer.parseInt(cursoParam.trim());
                    List<Curso> cursos = new CursoDao().findAll();
                    for (Curso curso : cursos) {
                        if (curso != null
                                && ScaUiContext.normalizeSpecialty(curso.getEspecialidad()).equals(ScaUiContext.normalizeSpecialty(selected.getNombre()))
                                && curso.getCurso() == cursoNivel
                                && seccionParam.equalsIgnoreCase(curso.getSeccion())) {
                            selectedCurso = curso;
                            break;
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            if (selectedCurso != null) {
                request.setAttribute("selCurso", selectedCurso);
                List<RasgoPlanilla> clasesRegistradas = new RasgoPlanillaDao().listarPorCurso(selectedCurso.getId());
                request.setAttribute("clasesRegistradas", clasesRegistradas);
            } else {
                request.setAttribute("clasesRegistradas", java.util.Collections.emptyList());
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
