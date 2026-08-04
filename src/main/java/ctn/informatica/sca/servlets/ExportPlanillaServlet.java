/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.StudentRowDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.PlanillaProcesoWorkbookBuilder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author jonat
 */
@WebServlet(name = "ExportPlanillaServlet", urlPatterns = {"/planilla/export"})
public class ExportPlanillaServlet extends HttpServlet {

    private final PlanillaProcesoWorkbookBuilder workbookBuilder = new PlanillaProcesoWorkbookBuilder();

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
                List<Tarea> tareas = PlanillaProcesoWorkbookBuilder.filterTasksByEtapa(
                    new TareaDao().consultarTarea(planilla.getId()),
                    planilla.getEtapaIndex());
            Map<Integer, Integer> tareaMax = new HashMap<>();
            int totalPossiblePoints = 0;
            for (Tarea t : tareas) {
                tareaMax.put(t.getId(), t.getTotal());
                totalPossiblePoints += t.getTotal();
            }

            planilla.computeGradeRanges(totalPossiblePoints);

            List<StudentRow> rows = new StudentRowDao().loadRowsForPlanilla(planilla, tareaMax, totalPossiblePoints);
            Curso curso = new CursoDao().findById(planilla.getCursoId());
            Profesor profesor = new ProfesorDao().findById(planilla.getProfesorId());
            String profesorNombre = profesor != null && !profesor.getFullName().isBlank()
                    ? profesor.getFullName()
                    : (user == null ? "" : user.getFullName());

            Map<Integer, Integer> firstStageGrades = resolveFirstStageGrades(planillaDao, planilla);

            PlanillaProcesoWorkbookBuilder.PlanillaSheetData data = new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                    planilla,
                    curso,
                    planilla.getNombre(),
                    profesorNombre,
                    safeTrim(request.getParameter("turno")),
                    tareas,
                    rows,
                    firstStageGrades
            );

            try (XSSFWorkbook wb = workbookBuilder.buildSingleWorkbook(data, planilla.getNombre())) {
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
        } catch (IllegalStateException ise) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ise.getMessage());
        } catch (SQLException sqle) {
            log("Database error in ExportPlanillaServlet", sqle);
            throw new ServletException("Database error", sqle);
        }

    }

    private Map<Integer, Integer> resolveFirstStageGrades(PlanillaDao planillaDao, Planilla planilla) throws SQLException {
        if (planilla.getEtapaIndex() != 2) {
            return Map.of();
        }

        Planilla firstStagePlanilla = planillaDao.findByCompositeKey(planilla.getCursoId(), planilla.getMateriaId(), 1);
        if (firstStagePlanilla == null) {
            return Map.of();
        }

        List<Tarea> tareasPrimeraEtapa = PlanillaProcesoWorkbookBuilder.filterTasksByEtapa(
            new TareaDao().consultarTarea(firstStagePlanilla.getId()),
            firstStagePlanilla.getEtapaIndex());
        Map<Integer, Integer> tareaMaxPrimeraEtapa = new HashMap<>();
        int totalPossiblePointsPrimeraEtapa = 0;
        for (Tarea tarea : tareasPrimeraEtapa) {
            tareaMaxPrimeraEtapa.put(tarea.getId(), tarea.getTotal());
            totalPossiblePointsPrimeraEtapa += tarea.getTotal();
        }
        firstStagePlanilla.computeGradeRanges(totalPossiblePointsPrimeraEtapa);

        List<StudentRow> firstStageRows = new StudentRowDao().loadRowsForPlanilla(firstStagePlanilla, tareaMaxPrimeraEtapa, totalPossiblePointsPrimeraEtapa);
        Map<Integer, Integer> gradesByAlumno = new HashMap<>();
        for (StudentRow row : firstStageRows) {
            gradesByAlumno.put(row.getAlumnoId(), firstStagePlanilla.getNotaForSum(row.getTotal()));
        }
        return gradesByAlumno;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

}
