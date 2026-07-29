/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sia.servlets;

import ctn.informatica.sia.dao.EspecialidadDao;
import ctn.informatica.sia.model.Especialidad;
import ctn.informatica.sia.model.User;
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

            // optionally, set selected especialidad if a request param was provided
            String selId = request.getParameter("especialidad");
            if (selId != null && !selId.isEmpty()) {
                try {
                    int id = Integer.parseInt(selId);
                    Especialidad sel = new EspecialidadDao().findById(id);
                    if (sel != null) {
                        request.setAttribute("selEspecialidad", sel);
                    }
                } catch (NumberFormatException ignored) {
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
