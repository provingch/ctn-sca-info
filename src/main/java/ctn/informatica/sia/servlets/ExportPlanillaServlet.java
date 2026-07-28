/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sia.servlets;

import ctn.informatica.sia.dao.PlanillaDao;
import ctn.informatica.sia.dao.StudentRowDao;
import ctn.informatica.sia.dao.TareaDao;
import ctn.informatica.sia.model.Planilla;
import ctn.informatica.sia.model.StudentRow;
import ctn.informatica.sia.model.Tarea;
import ctn.informatica.sia.model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author jonat
 */
@WebServlet(name = "ExportPlanillaServlet", urlPatterns = {"/ExportPlanillaServlet"})
public class ExportPlanillaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        String planillaIdStr = request.getParameter("planillaId");
        if (planillaIdStr == null || planillaIdStr.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing planillaId");
            return;
        }

        try {
            int planillaId = Integer.parseInt(planillaIdStr.trim());
            PlanillaDao planillaDao = new PlanillaDao();
            Planilla planilla = planillaDao.findById(planillaId);
            if (planilla == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Planilla not found");
                return;
            }
            // permission check (same as PlanillaServlet)
            if (user == null || planilla.getProfesorId() != user.getId()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // load tareas and rows (re-use the same DAO methods you use in PlanillaServlet)
            List<Tarea> tareas = new TareaDao().consultarTarea(planilla.getId());
            // Build a map tareaId -> total if you need it (optional)
            // load rows (StudentRow is the same class your PlanillaServlet uses)
            // Assumes StudentRowDao.loadRowsForPlanilla(planilla, tareaMax, totalPossiblePoints) exists
            // We reconstruct tareaMax/totalPossiblePoints similar to PlanillaServlet
            Map<Integer, Integer> tareaMax = new java.util.HashMap<>();
            int totalPossiblePoints = 0;
            for (Tarea t : tareas) {
                tareaMax.put(t.getId(), t.getTotal());
                totalPossiblePoints += t.getTotal();
            }

            List<StudentRow> rows = new StudentRowDao().loadRowsForPlanilla(planilla, tareaMax, totalPossiblePoints);

            // Create Excel workbook
            try (XSSFWorkbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet("Planilla");

                // header row
                int col = 0;
                Row header = sheet.createRow(0);
                header.createCell(col++).setCellValue("#");
                header.createCell(col++).setCellValue("Alumno");
                header.createCell(col++).setCellValue("Total (" + totalPossiblePoints + ")");
                header.createCell(col++).setCellValue("Porcentaje");
                header.createCell(col++).setCellValue("Nota");

                // tarea columns
                for (Tarea t : tareas) {
                    header.createCell(col++).setCellValue(t.getTitulo() + " (TP:" + t.getTotal() + ")");
                }

                // fill rows
                int r = 1;
                for (StudentRow sr : rows) {
                    Row excelRow = sheet.createRow(r++);
                    int c = 0;
                    excelRow.createCell(c++).setCellValue(r - 1); // index
                    excelRow.createCell(c++).setCellValue(sr.getAlumnoNombre());
                    // depending on StudentRow methods:
                    excelRow.createCell(c++).setCellValue(sr.getTotal());
                    excelRow.createCell(c++).setCellValue(sr.getPorcentaje());
                    excelRow.createCell(c++).setCellValue(sr.getNota());
                    Map<Integer, Integer> grades = sr.getGrades(); // Map<tareaId, puntos>
                    for (Tarea t : tareas) {
                        Integer val = grades != null ? grades.get(t.getId()) : null;
                        if (val != null) {
                            excelRow.createCell(c++).setCellValue(val);
                        } else {
                            excelRow.createCell(c++).setBlank();
                        }
                    }
                }

                // auto-size a few columns (optional; can be slow for very large sheets)
                for (int i = 0; i < Math.min(8 + tareas.size(), 30); i++) {
                    try {
                        sheet.autoSizeColumn(i);
                    } catch (Exception ignore) {
                    }
                }

                // Prepare response
                response.setContentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                );
                String safeName = URLEncoder.encode(planilla.getNombre(), "UTF-8").replaceAll("\\+", "%20");
                String filename = "Planilla-" + safeName + ".xlsx";
                // RFC5987 filename* for non-ASCII
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + URLEncoder.encode("Planilla-" + planilla.getNombre() + ".xlsx", "UTF-8"));

                try (OutputStream out = response.getOutputStream()) {
                    wb.write(out);
                    out.flush();
                }
            }

        } catch (NumberFormatException nfe) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid planillaId");
        } catch (SQLException sqle) {
            log("Database error in ExportPlanillaServlet", sqle);
            throw new ServletException("Database error", sqle);
        }

    }

}
