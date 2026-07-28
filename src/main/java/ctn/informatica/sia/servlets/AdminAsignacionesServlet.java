package ctn.informatica.sia.servlets;

import ctn.informatica.sia.dao.AsignacionDao;
import ctn.informatica.sia.dao.CursoDao;
import ctn.informatica.sia.dao.EspecialidadDao;
import ctn.informatica.sia.dao.MateriaDao;
import ctn.informatica.sia.dao.ProfesorDao;
import ctn.informatica.sia.model.Asignacion;
import ctn.informatica.sia.model.Curso;
import ctn.informatica.sia.model.Especialidad;
import ctn.informatica.sia.model.Materia;
import ctn.informatica.sia.model.Profesor;
import ctn.informatica.sia.model.User;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "AdminAsignacionesServlet", urlPatterns = {"/AdminAsignacionesServlet"})
public class AdminAsignacionesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || user.getLevel() != 3) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        try {
            AsignacionDao aDao = new AsignacionDao();
            List<Asignacion> asignaciones = aDao.findAll();
            req.setAttribute("asignaciones", asignaciones);

            ProfesorDao pDao = new ProfesorDao();
            List<Profesor> profesores = pDao.findAll();
            // filter nivel==1 for now
            List<Profesor> profesoresNivel1 = new ArrayList<>();
            for (Profesor p : profesores) if (p.getNivel() == 1) profesoresNivel1.add(p);
            req.setAttribute("profesores", profesoresNivel1);

            MateriaDao mDao = new MateriaDao();
            List<Materia> materias = mDao.listAll();
            req.setAttribute("materias", materias);

            CursoDao cDao = new CursoDao();
            List<Curso> cursos = cDao.findAll();
            req.setAttribute("cursos", cursos);

            EspecialidadDao eDao = new EspecialidadDao();
            List<Especialidad> especialidades = eDao.findAll();
            req.setAttribute("especialidades", especialidades);

        } catch (Exception ex) {
            log("Error loading asignaciones admin", ex);
            req.setAttribute("errors", List.of("No se pudo cargar la lista de asignaciones."));
        }
        req.getRequestDispatcher("/AdminAsignaciones.jsp").forward(req, resp);
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
                int profesorId = Integer.parseInt(req.getParameter("profesorId"));
                int materiaId = Integer.parseInt(req.getParameter("materiaId"));
                int cursoId = Integer.parseInt(req.getParameter("cursoId"));

                ProfesorDao pDao = new ProfesorDao();
                MateriaDao mDao = new MateriaDao();
                CursoDao cDao = new CursoDao();

                if (pDao.findById(profesorId) == null) {
                    session.setAttribute("errors", List.of("Profesor no existe."));
                    resp.sendRedirect(req.getContextPath() + "/AdminAsignacionesServlet");
                    return;
                }
                if (mDao.findById(materiaId) == null) {
                    session.setAttribute("errors", List.of("Materia no existe."));
                    resp.sendRedirect(req.getContextPath() + "/AdminAsignacionesServlet");
                    return;
                }
                if (cDao.findById(cursoId) == null) {
                    session.setAttribute("errors", List.of("Curso no existe."));
                    resp.sendRedirect(req.getContextPath() + "/AdminAsignacionesServlet");
                    return;
                }

                AsignacionDao aDao = new AsignacionDao();
                if (aDao.existe(profesorId, materiaId, cursoId)) {
                    session.setAttribute("errors", List.of("La asignación ya existe."));
                    resp.sendRedirect(req.getContextPath() + "/AdminAsignacionesServlet");
                    return;
                }
                int created = aDao.crear(profesorId, materiaId, cursoId);
                // ensure profesor_materia link exists
                try { mDao.linkProfesorMateria(profesorId, materiaId); } catch (SQLException ignored) {}
                session.setAttribute("flashMessage", "Asignación creada correctamente.");

            } else if ("eliminar".equals(action)) {
                int id = Integer.parseInt(req.getParameter("id"));
                AsignacionDao aDao = new AsignacionDao();
                boolean ok = aDao.eliminar(id);
                if (ok) session.setAttribute("flashMessage", "Asignación eliminada."); else session.setAttribute("errors", List.of("No se pudo eliminar la asignación."));
            }
        } catch (NumberFormatException ex) {
            session.setAttribute("errors", List.of("Ids inválidos."));
        } catch (SQLException ex) {
            session.setAttribute("errors", List.of("Error de base de datos: " + ex.getMessage()));
        } catch (Exception ex) {
            session.setAttribute("errors", List.of("Error interno: " + ex.getMessage()));
        }

        resp.sendRedirect(req.getContextPath() + "/AdminAsignacionesServlet");
    }

}
