/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.InstrumentoDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.model.Instrumento;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Tarea;
import ctn.informatica.sca.model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author jonat
 */
@WebServlet(name = "LegacyTareaServlet", urlPatterns = {"/LegacyTareaServlet"})
public class LegacyTareaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Debe iniciar sesión para acceder a esta página.");
            return;
        }

        String planillaIdStr = request.getParameter("planillaId");
        String etapaStr = request.getParameter("etapa");
        int planillaId;
        int etapa = 1;

        if (planillaIdStr == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing planillaId");
            return;
        }

        try {
            planillaId = Integer.parseInt(planillaIdStr);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid planillaId");
            return;
        }

        if (etapaStr != null && !etapaStr.trim().isEmpty()) {
            try {
                etapa = Integer.parseInt(etapaStr.trim());
            } catch (NumberFormatException ex) {
            }
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Asuncion")); // use Paraguay timezone
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                "EEEE, dd 'de' MMMM 'de' yyyy",
                new Locale("es", "ES")
        );
        String nowFormatted = now.format(fmt);
// Capitalize first letter (Spanish day names are usually lower-case)
        if (nowFormatted != null && !nowFormatted.isEmpty()) {
            nowFormatted = nowFormatted.substring(0, 1).toUpperCase() + nowFormatted.substring(1);
        }
        request.setAttribute("nowFormatted", nowFormatted);

        try {
            List<Planilla> planillas = new PlanillaDao().consultarPlanillasUser(user.getId(), etapa);
            Planilla selectedPlanilla = null;
            for (Planilla p : planillas) {
                if (p.getId() == planillaId) {
                    selectedPlanilla = p;
                    break;
                }
            }

            // Load instrumentos
            List<Instrumento> instrumentos = new InstrumentoDao().findAll();

            // Place data on request
            request.setAttribute("planillas", planillas);
            request.setAttribute("selPlanilla", selectedPlanilla);
            request.setAttribute("instrumentos", instrumentos);
            request.setAttribute("planillaId", planillaId);
            request.setAttribute("etapa", etapa);

            request.getRequestDispatcher("/Tarea.jsp").forward(request, response);
        } catch (SQLException sqle) {
            log("Error loading data for Tarea form", sqle);
            throw new ServletException("Unable to load required data", sqle);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Debe iniciar sesión para crear tareas.");
            return;
        }

        String planillaIdStr = request.getParameter("planillaId");
        String instrumentoIdStr = request.getParameter("instrumentoId");
        String fechaStr = request.getParameter("fecha"); // expected yyyy-MM-dd
        String totalStr = request.getParameter("total");
        String titulo = request.getParameter("titulo");
        String etapaStr = request.getParameter("etapa");

        List<String> errors = new ArrayList<>();

        if (planillaIdStr == null || planillaIdStr.trim().isEmpty()) {
            errors.add("Seleccione una materia.");
        }
        if (instrumentoIdStr == null || instrumentoIdStr.trim().isEmpty()) {
            errors.add("Seleccione un instrumento.");
        }
        if (fechaStr == null || fechaStr.trim().isEmpty()) {
            errors.add("Fecha requerida.");
        }
        if (totalStr == null || totalStr.trim().isEmpty()) {
            errors.add("Total requerido.");
        }
        if (titulo == null || titulo.trim().isEmpty()) {
            errors.add("Título requerido.");
        }

        int etapa = 1;
        int planillaId = 0;
        int instrumentoId = 0;
        int total = 0;
        java.time.LocalDate fecha = null;

        if (etapaStr != null && !etapaStr.trim().isEmpty()) {
            try {
                etapa = Integer.parseInt(etapaStr.trim());
            } catch (NumberFormatException ignored) {
                // keep default stage if the parameter is malformed
            }
        }

        try {
            if (planillaIdStr != null && !planillaIdStr.trim().isEmpty()) {
                planillaId = Integer.parseInt(planillaIdStr);
            }
            if (instrumentoIdStr != null && !instrumentoIdStr.trim().isEmpty()) {
                instrumentoId = Integer.parseInt(instrumentoIdStr);
            }
            if (totalStr != null && !totalStr.trim().isEmpty()) {
                total = Integer.parseInt(totalStr);
            }
        } catch (NumberFormatException nfe) {
            errors.add("Valores numéricos inválidos.");
        }

        try {
            if (fechaStr != null && !fechaStr.trim().isEmpty()) {
                fecha = java.time.LocalDate.parse(fechaStr);
            }
        } catch (Exception ex) {
            errors.add("Formato de fecha inválido. Use YYYY-MM-DD.");
        }

        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            try {
                InstrumentoDao instrumentoDao = new InstrumentoDao();
                request.setAttribute("instrumentos", instrumentoDao.findAll());
            } catch (SQLException ex) {
                throw new ServletException("Error loading instrumentos", ex);
            }

            ArrayList<Planilla> planillas;
            try {
                planillas = new PlanillaDao()
                        .consultarPlanillasUser(user.getId(), etapa);
            } catch (SQLException sqle) {
                log("Error loading planillas for user " + user.getId());
                throw new ServletException("Unable to load planillas", sqle);
            }

            // preserve submitted values so the form can re-populate
            request.setAttribute("planillaId", planillaIdStr);
            request.setAttribute("planillas", planillas);
            request.setAttribute("instrumentoId", instrumentoIdStr);
            request.setAttribute("fecha", fechaStr);
            request.setAttribute("total", totalStr);
            request.setAttribute("titulo", titulo);
            request.setAttribute("etapa", etapaStr);
            request.getRequestDispatcher("/Tarea.jsp").forward(request, response);
            return;
        }

        Planilla planilla;
        try {
            planilla = new PlanillaDao().findById(planillaId);
        } catch (SQLException ex) {
            throw new ServletException("Error loading planilla", ex);
        }
        
        if (planilla == null || planilla.getProfesorId() != user.getId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No tiene permiso para modificar esta planilla.");
            return;
        }

        // Build model and insert
        Tarea tarea = new Tarea();
        tarea.setPlanillaId(planillaId);
        tarea.setInstrumentoId(instrumentoId);
        tarea.setFecha(java.time.LocalDate.parse(fechaStr));
        tarea.setTotal(total);
        tarea.setTitulo(titulo);

        TareaDao tdao = new TareaDao();
        try {
            tdao.insertarTarea(tarea);
        } catch (SQLException ex) {
            throw new ServletException("Error saving tarea", ex);
        }

        // After successful insert, redirect back to Planilla view (include etapa/curso/materia if present)
        String redirect = request.getContextPath() + "/PlanillaServlet?planillaId=" + planillaId;
        if (etapaStr != null && !etapaStr.isEmpty()) {
            redirect += "&etapa=" + etapaStr;
        }

        response.sendRedirect(redirect);
    }

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str; // Handle null or empty strings
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
