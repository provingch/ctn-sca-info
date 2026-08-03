/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.PlanillaDao;
import ctn.informatica.sca.dao.PlanillaDao.PlanillaInfo;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.StudentRowDao;
import ctn.informatica.sca.dao.TareaDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Planilla;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author jonat
 */

@WebServlet(name = "ExportCoursePlanillasServlet", urlPatterns = {"/planilla/export-course"})
public class ExportCoursePlanillasServlet extends HttpServlet {

    private final PlanillaProcesoWorkbookBuilder workbookBuilder = new PlanillaProcesoWorkbookBuilder();

    // helper: sanitize sheet name to 31 chars and remove invalid chars
    private String safeSheetName(String name, int maxLength) {
        if (name == null) name = "Sheet";
        // remove invalid characters for sheet name: : \ / ? * [ ]
        String s = name.replaceAll("[:\\\\/?*\\[\\]]", " ");
        s = s.trim();
        if (s.length() > maxLength) s = s.substring(0, maxLength);
        if (s.isEmpty()) s = "Sheet";
        return s;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // session + admin permission check
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        // ADJUST this check to match your app's admin level
        if (user == null || user.getLevel() < 2) { // example: nivel >= 2 allowed
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        String espIdStr = request.getParameter("especialidad");
        String cursoStr = request.getParameter("curso");
        String seccion = request.getParameter("seccion");
        String periodoStr = request.getParameter("periodo");

        if (espIdStr == null || cursoStr == null || seccion == null || periodoStr == null ||
            espIdStr.isEmpty() || cursoStr.isEmpty() || seccion.isEmpty() || periodoStr.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters. Required: especialidad, curso, seccion, periodo");
            return;
        }

        try {
            int especialidadId = Integer.parseInt(espIdStr.trim());
            int curso = Integer.parseInt(cursoStr.trim()); // 1..3 as user enters
            int periodo = Integer.parseInt(periodoStr.trim()); // period (year)
            // compute promocion from the selected course level (1º/2º/3º)
            int promocion = periodo - curso + 3;
            if (promocion <= 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "El curso seleccionado no corresponde a una promoción válida");
                return;
            }

            PlanillaDao planillaDao = new PlanillaDao();
            // get all planillas for the course
            List<PlanillaInfo> planillas = planillaDao.findPlanillasByCourse(especialidadId, promocion, seccion, periodo);

            if (planillas == null || planillas.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No planillas found for the selected course");
                return;
            }

            // optional: get especialidad name for nicer filename
            String especialidadName = null;
            try {
                EspecialidadDao espDao = new EspecialidadDao();
                var esp = espDao.findById(especialidadId);
                if (esp != null) especialidadName = esp.getNombre();
            } catch (Exception ex) {
                // ignore - just fallback to id in filename
            }

            List<PlanillaProcesoWorkbookBuilder.PlanillaSheetData> exportSheets = new ArrayList<>();
            CursoDao cursoDao = new CursoDao();
            ProfesorDao profesorDao = new ProfesorDao();

            for (PlanillaInfo pi : planillas) {
                Planilla p = planillaDao.findById(pi.getPlanilla().getId());
                if (p == null) {
                    continue;
                }
                List<Tarea> tareas = new TareaDao().consultarTarea(p.getId());
                Map<Integer, Integer> tareaMax = new HashMap<>();
                int totalPossiblePoints = 0;
                for (Tarea t : tareas) {
                    tareaMax.put(t.getId(), t.getTotal());
                    totalPossiblePoints += t.getTotal();
                }
                p.computeGradeRanges(totalPossiblePoints);

                List<StudentRow> rows = new StudentRowDao().loadRowsForPlanilla(p, tareaMax, totalPossiblePoints);
                Curso cursoData = cursoDao.findById(p.getCursoId());
                Profesor profesor = profesorDao.findById(p.getProfesorId());
                String profesorNombre = profesor == null ? "" : profesor.getFullName();

                exportSheets.add(new PlanillaProcesoWorkbookBuilder.PlanillaSheetData(
                        p,
                        cursoData,
                        pi.getMateriaNombre(),
                        profesorNombre,
                        "",
                        tareas,
                        rows,
                        resolveFirstStageGrades(planillaDao, p)
                ));
            }

            try (XSSFWorkbook wb = workbookBuilder.buildCourseWorkbook(exportSheets)) {
                // prepare response filename
                String safeEspecialidad = (especialidadName != null && !especialidadName.trim().isEmpty())
                        ? especialidadName.replaceAll("[^A-Za-z0-9 _-]", "").replaceAll("\\s+", "_")
                        : ("especialidad" + especialidadId);
                String filenameBase = "Planillas_" + safeEspecialidad + "_P" + promocion + "_S" + seccion + "_PER" + periodo;
                String filename = URLEncoder.encode(filenameBase + ".xlsx", "UTF-8").replaceAll("\\+", "%20");

                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + URLEncoder.encode(filenameBase + ".xlsx", "UTF-8"));

                try (OutputStream out = response.getOutputStream()) {
                    wb.write(out);
                    out.flush();
                }
            } // end workbook try

        } catch (NumberFormatException nfe) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric parameter");
        } catch (IllegalStateException ise) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ise.getMessage());
        } catch (SQLException | ClassNotFoundException ex) {
            log("DB error generating course planillas", ex);
            throw new ServletException("Database error", ex);
        } catch (Exception ex) {
            log("Error generating excel", ex);
            throw new ServletException("Unexpected error", ex);
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

        List<Tarea> tareasPrimeraEtapa = new TareaDao().consultarTarea(firstStagePlanilla.getId());
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
}
