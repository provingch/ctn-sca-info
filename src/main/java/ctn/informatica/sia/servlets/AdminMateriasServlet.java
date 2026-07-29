package ctn.informatica.sia.servlets;

import ctn.informatica.sia.dao.MateriaDao;
import ctn.informatica.sia.model.Materia;
import ctn.informatica.sia.model.User;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "AdminMateriasServlet", urlPatterns = {"/AdminMateriasServlet"})
public class AdminMateriasServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || user.getLevel() != 3) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        try {
            MateriaDao dao = new MateriaDao();
            List<Materia> materias = dao.listAll();
            req.setAttribute("materias", materias);
            // load especialidades for create/edit form
            java.util.List<ctn.informatica.sia.model.Especialidad> especialidades = new ctn.informatica.sia.dao.EspecialidadDao().findAll();
            req.setAttribute("especialidades", especialidades);
            // professor counts per materia
            java.util.Map<Integer, Integer> profCounts = dao.countProfesoresForAll();
            req.setAttribute("profCounts", profCounts);
            // map materia->especialidad names as joined text for JSTL
            java.util.Map<Integer, String> materiaEspecialidadesTexto = new java.util.HashMap<>();
            for (Materia m : materias) {
                java.util.List<Integer> ids = dao.listEspecialidadIdsForMateria(m.getId());
                java.util.List<String> names = new java.util.ArrayList<>();
                for (Integer id : ids) {
                    ctn.informatica.sia.model.Especialidad e = new ctn.informatica.sia.dao.EspecialidadDao().findById(id);
                    if (e != null) names.add(e.getNombre());
                }
                materiaEspecialidadesTexto.put(m.getId(), String.join(", ", names));
            }
            req.setAttribute("materiaEspecialidadesTexto", materiaEspecialidadesTexto);

            String editIdParam = req.getParameter("editId");
            if (editIdParam != null && !editIdParam.isBlank()) {
                try {
                    int editId = Integer.parseInt(editIdParam);
                    Materia editMateria = dao.findById(editId);
                    if (editMateria != null) {
                        req.setAttribute("editMode", Boolean.TRUE);
                        req.setAttribute("editMateria", editMateria);
                        req.setAttribute("editEspecialidadIds", dao.listEspecialidadIdsForMateria(editId));
                    }
                } catch (NumberFormatException ignored) {
                    req.setAttribute("errors", java.util.List.of("Id de edición inválido."));
                }
            }
        } catch (Exception ex) {
            log("Error loading materias for admin", ex);
            req.setAttribute("errors", java.util.List.of("No se pudo cargar el catálogo de materias."));
        }
        req.getRequestDispatcher("/AdminMaterias.jsp").forward(req, resp);
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
            if ("check".equals(action)) {
                int fromId = Integer.parseInt(req.getParameter("fromId"));
                int toId = Integer.parseInt(req.getParameter("toId"));
                MateriaDao dao = new MateriaDao();
                List<String> conflicts = dao.checkMergeConflicts(fromId, toId);
                req.setAttribute("conflicts", conflicts);
                req.setAttribute("fromId", fromId);
                req.setAttribute("toId", toId);

                List<Materia> materias = dao.listAll();
                req.setAttribute("materias", materias);
                req.setAttribute("especialidades", new ctn.informatica.sia.dao.EspecialidadDao().findAll());
                req.setAttribute("profCounts", dao.countProfesoresForAll());
                java.util.Map<Integer, String> materiaEspecialidadesTexto = new java.util.HashMap<>();
                for (Materia m : materias) {
                    java.util.List<Integer> ids = dao.listEspecialidadIdsForMateria(m.getId());
                    java.util.List<String> names = new java.util.ArrayList<>();
                    for (Integer id : ids) {
                        ctn.informatica.sia.model.Especialidad e = new ctn.informatica.sia.dao.EspecialidadDao().findById(id);
                        if (e != null) names.add(e.getNombre());
                    }
                    materiaEspecialidadesTexto.put(m.getId(), String.join(", ", names));
                }
                req.setAttribute("materiaEspecialidadesTexto", materiaEspecialidadesTexto);
                req.getRequestDispatcher("/AdminMaterias.jsp").forward(req, resp);
                return;
            } else if ("merge".equals(action)) {
                int fromId = Integer.parseInt(req.getParameter("fromId"));
                int toId = Integer.parseInt(req.getParameter("toId"));
                // double-check conflicts before executing
                MateriaDao dao = new MateriaDao();
                List<String> conflicts = dao.checkMergeConflicts(fromId, toId);
                if (!conflicts.isEmpty()) {
                    session.setAttribute("errors", java.util.List.of("No se puede ejecutar merge: existen planillas en conflicto."));
                    resp.sendRedirect(req.getContextPath() + "/AdminMateriasServlet");
                    return;
                }
                dao.mergeMaterias(fromId, toId);
                // detailed admin activity log
                appendAdminActivity(session, "Merge materias: from=" + fromId + " to=" + toId);
                session.setAttribute("flashMessage", "Merge realizado correctamente.");
            } else if ("create".equals(action)) {
                String nombre = req.getParameter("nombre");
                String categoria = req.getParameter("categoria");
                String[] espVals = req.getParameterValues("especialidades");
                java.util.List<Integer> espIds = new java.util.ArrayList<>();
                if (espVals != null) for (String v : espVals) try { espIds.add(Integer.parseInt(v)); } catch (NumberFormatException ignore) {}
                MateriaDao dao = new MateriaDao();
                int created = dao.create(nombre, categoria);
                if (created > 0) dao.replaceEspecialidades(created, espIds);
                appendAdminActivity(session, "Crear materia: " + nombre + " id=" + created);
                session.setAttribute("flashMessage", "Materia creada correctamente.");
            } else if ("edit".equals(action)) {
                String materiaIdParam = req.getParameter("materiaId");
                String categoria = req.getParameter("categoria");
                String[] espVals = req.getParameterValues("especialidades");
                if (materiaIdParam == null || materiaIdParam.isBlank()) {
                    session.setAttribute("errors", java.util.List.of("Falta el identificador de la materia."));
                    resp.sendRedirect(req.getContextPath() + "/AdminMateriasServlet");
                    return;
                }
                if (categoria == null || categoria.isBlank()) {
                    session.setAttribute("errors", java.util.List.of("No se aplicaron cambios: faltó la categoría. Elija un valor antes de guardar."));
                    resp.sendRedirect(req.getContextPath() + "/AdminMateriasServlet");
                    return;
                }
                java.util.List<Integer> espIds = new java.util.ArrayList<>();
                if (espVals != null) for (String v : espVals) try { espIds.add(Integer.parseInt(v)); } catch (NumberFormatException ignore) {}
                MateriaDao dao = new MateriaDao();
                int materiaId = Integer.parseInt(materiaIdParam);
                dao.updateCategoria(materiaId, categoria);
                if (espVals != null) {
                    dao.replaceEspecialidades(materiaId, espIds);
                }
                appendAdminActivity(session, "Editar materia: id=" + materiaId);
                session.setAttribute("flashMessage", "Materia actualizada correctamente.");
            }
        } catch (NumberFormatException ex) {
            session.setAttribute("errors", java.util.List.of("Ids inválidos para merge."));
        } catch (SQLException ex) {
            session.setAttribute("errors", java.util.List.of("No se pudo realizar el merge: " + ex.getMessage()));
        } catch (Exception ex) {
            session.setAttribute("errors", java.util.List.of("Error interno: " + ex.getMessage()));
        }
        resp.sendRedirect(req.getContextPath() + "/AdminMateriasServlet");
    }

    @SuppressWarnings("unchecked")
    private void appendAdminActivity(HttpSession session, String entry) {
        if (session == null) return;
        java.util.List<String> log = (java.util.List<String>) session.getAttribute("adminActivityLog");
        if (log == null) {
            log = new java.util.ArrayList<>();
            session.setAttribute("adminActivityLog", log);
        }
        log.add(entry);
        if (log.size() > 50) log.remove(0);
    }

}
