package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.AlumnoDao;
import ctn.informatica.sca.dao.CursoDao;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.ScaUiContext;
import ctn.informatica.sca.util.StudentLoadPolicy;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "AdminIngresantesServlet", urlPatterns = {"/AdminIngresantesServlet"})
public class AdminIngresantesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || user.getLevel() != 3) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        try {
            CursoDao cursoDao = new CursoDao();
            List<Curso> cursos = cursoDao.findAll();
            req.setAttribute("cursos", cursos);

            Map<String, Map<String, List<Curso>>> cursosAgrupados = new LinkedHashMap<>();
            Map<String, String> specialtyTokenByName = new LinkedHashMap<>();
            for (Curso curso : cursos) {
                String especialidad = curso.getEspecialidad() == null || curso.getEspecialidad().isBlank()
                        ? "Sin especialidad"
                        : curso.getEspecialidad();
                specialtyTokenByName.putIfAbsent(especialidad, ScaUiContext.normalizeSpecialty(especialidad));
                cursosAgrupados
                        .computeIfAbsent(especialidad, key -> new LinkedHashMap<>())
                        .computeIfAbsent(curso.getCursoOrdinal(), key -> new ArrayList<>())
                        .add(curso);
            }
            req.setAttribute("cursosAgrupados", cursosAgrupados);
            req.setAttribute("specialtyTokenByName", specialtyTokenByName);

            EspecialidadDao especialidadDao = new EspecialidadDao();
            List<Especialidad> especialidades = especialidadDao.findAll();
            req.setAttribute("especialidades", especialidades);

            AlumnoDao alumnoDao = new AlumnoDao();
            List<Alumno> alumnos = alumnoDao.findAll();
            req.setAttribute("alumnos", alumnos);

            Map<Integer, String> courseLabels = new HashMap<>();
            for (Curso curso : cursos) {
                courseLabels.put(curso.getId(), curso.getEspecialidad() + " · " + curso.getCursoOrdinal() + " · Sección " + curso.getSeccion());
            }
            req.setAttribute("courseLabels", courseLabels);

            Map<Integer, Integer> countsByCurso = new HashMap<>();
            Map<Integer, String> statusByCurso = new HashMap<>();
            Map<Integer, String> messageByCurso = new HashMap<>();
            for (Curso curso : cursos) {
                int current = alumnoDao.countByCursoId(curso.getId());
                countsByCurso.put(curso.getId(), current);
                statusByCurso.put(curso.getId(), StudentLoadPolicy.getCapacityStatus(current, StudentLoadPolicy.getTargetCapacity()));
                messageByCurso.put(curso.getId(), StudentLoadPolicy.getCapacityMessage(current, StudentLoadPolicy.getTargetCapacity()));
            }
            req.setAttribute("countsByCurso", countsByCurso);
            req.setAttribute("statusByCurso", statusByCurso);
            req.setAttribute("messageByCurso", messageByCurso);
            req.setAttribute("targetCapacity", StudentLoadPolicy.getTargetCapacity());
        } catch (Exception ex) {
            log("Error loading ingresantes admin", ex);
            req.setAttribute("errors", List.of("No se pudo cargar la carga de ingresantes."));
        }

        req.getRequestDispatcher("/AdminIngresantes.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || user.getLevel() != 3) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = req.getParameter("action");
        try {
            if ("crear".equals(action)) {
                String nombre = trim(req.getParameter("nombre"));
                String apellido = trim(req.getParameter("apellido"));
                String cursoIdParam = req.getParameter("cursoId");
                String ciParam = req.getParameter("ci");
                String correoEncargado = trim(req.getParameter("correoEncargado"));
                String correoEncargado2 = trim(req.getParameter("correoEncargado2"));

                if (nombre.isBlank() || apellido.isBlank() || cursoIdParam == null || cursoIdParam.isBlank()) {
                    session.setAttribute("errors", List.of("Faltan datos obligatorios para crear el alumno."));
                    resp.sendRedirect(req.getContextPath() + "/AdminIngresantesServlet");
                    return;
                }

                int cursoId = Integer.parseInt(cursoIdParam);
                Integer ci = null;
                if (ciParam != null && !ciParam.isBlank()) {
                    ci = Integer.valueOf(ciParam);
                }

                CursoDao cursoDao = new CursoDao();
                if (cursoDao.findById(cursoId) == null) {
                    session.setAttribute("errors", List.of("El curso seleccionado no existe."));
                    resp.sendRedirect(req.getContextPath() + "/AdminIngresantesServlet");
                    return;
                }

                AlumnoDao alumnoDao = new AlumnoDao();
                int created = alumnoDao.create(nombre, apellido, cursoId, ci, correoEncargado, correoEncargado2);
                if (created > 0) {
                    int currentCount = alumnoDao.countByCursoId(cursoId);
                    String status = StudentLoadPolicy.getCapacityStatus(currentCount, StudentLoadPolicy.getTargetCapacity());
                    String message = StudentLoadPolicy.getCapacityMessage(currentCount, StudentLoadPolicy.getTargetCapacity());
                    session.setAttribute("flashMessage", "Alumno creado correctamente. " + message);
                    if (!"ideal".equals(status)) {
                        session.setAttribute("warnings", List.of(message));
                    }
                } else {
                    session.setAttribute("errors", List.of("No se pudo crear el alumno."));
                }
            } else if ("editar".equals(action)) {
                String alumnoIdParam = req.getParameter("alumnoId");
                String nombre = trim(req.getParameter("nombre"));
                String apellido = trim(req.getParameter("apellido"));
                String cursoIdParam = req.getParameter("cursoId");
                String ciParam = req.getParameter("ci");
                String correoEncargado = trim(req.getParameter("correoEncargado"));
                String correoEncargado2 = trim(req.getParameter("correoEncargado2"));

                if (alumnoIdParam == null || alumnoIdParam.isBlank() || nombre.isBlank() || apellido.isBlank() || cursoIdParam == null || cursoIdParam.isBlank()) {
                    session.setAttribute("errors", List.of("Faltan datos obligatorios para actualizar el alumno."));
                    resp.sendRedirect(req.getContextPath() + "/AdminIngresantesServlet");
                    return;
                }

                int alumnoId = Integer.parseInt(alumnoIdParam);
                int cursoId = Integer.parseInt(cursoIdParam);
                Integer ci = null;
                if (ciParam != null && !ciParam.isBlank()) {
                    ci = Integer.valueOf(ciParam);
                }

                CursoDao cursoDao = new CursoDao();
                if (cursoDao.findById(cursoId) == null) {
                    session.setAttribute("errors", List.of("El curso seleccionado no existe."));
                    resp.sendRedirect(req.getContextPath() + "/AdminIngresantesServlet");
                    return;
                }

                AlumnoDao alumnoDao = new AlumnoDao();
                boolean updated = alumnoDao.update(alumnoId, nombre, apellido, cursoId, ci, correoEncargado, correoEncargado2);
                if (updated) {
                    session.setAttribute("flashMessage", "Alumno actualizado correctamente.");
                } else {
                    session.setAttribute("errors", List.of("No se pudo actualizar el alumno."));
                }
            }
        } catch (NumberFormatException ex) {
            session.setAttribute("errors", List.of("Datos inválidos."));
        } catch (SQLException ex) {
            session.setAttribute("errors", List.of("No se pudo crear el alumno: " + ex.getMessage()));
        } catch (Exception ex) {
            session.setAttribute("errors", List.of("Error interno: " + ex.getMessage()));
        }

        resp.sendRedirect(req.getContextPath() + "/AdminIngresantesServlet");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
