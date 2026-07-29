package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.User;
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

@WebServlet(name = "AdminUsuariosServlet", urlPatterns = {"/AdminUsuariosServlet"})
public class AdminUsuariosServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || user.getLevel() != 3) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        try {
            ProfesorDao profesorDao = new ProfesorDao();
            List<Profesor> profesores = profesorDao.findAll();
            req.setAttribute("profesores", profesores);
            List<Especialidad> especialidades = new EspecialidadDao().findAll();
            req.setAttribute("especialidades", especialidades);

            String editIdParam = req.getParameter("editId");
            if (editIdParam != null && !editIdParam.isBlank()) {
                try {
                    int editId = Integer.parseInt(editIdParam);
                    Profesor editProfesor = profesorDao.findById(editId);
                    if (editProfesor != null) {
                        req.setAttribute("editMode", Boolean.TRUE);
                        req.setAttribute("editProfesor", editProfesor);
                    } else {
                        req.setAttribute("errors", List.of("Profesor no encontrado para edición."));
                    }
                } catch (NumberFormatException ex) {
                    req.setAttribute("errors", List.of("Id de edición inválido."));
                }
            }
        } catch (Exception ex) {
            log("Error cargando usuarios para administración", ex);
            req.setAttribute("errors", List.of("No se pudo cargar la lista de usuarios."));
        }

        req.getRequestDispatcher("/AdminUsuarios.jsp").forward(req, resp);
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
        ProfesorDao profesorDao = new ProfesorDao();
        List<String> errors = new ArrayList<>();
        String flashMessage = null;

        try {
            if ("create".equals(action)) {
                String nombre = req.getParameter("nombre");
                String apellido = req.getParameter("apellido");
                String usuarioValue = req.getParameter("usuario");
                String contrasenia = req.getParameter("contrasenia");
                String nivelValue = req.getParameter("nivel");
                String ciValue = req.getParameter("ci");
                String telefonoValue = req.getParameter("telefono");
                String celularValue = req.getParameter("celular");
                String correo = req.getParameter("correo");
                String especialidadIdValue = req.getParameter("especialidadId");

                if (usuarioValue == null || usuarioValue.isBlank()) {
                    errors.add("El nombre de usuario es obligatorio.");
                } else if (profesorDao.existsByUsuario(usuarioValue)) {
                    errors.add("El usuario ya existe. Elija otro nombre de usuario.");
                }
                int nivel = parseNivel(nivelValue, errors);

                if (errors.isEmpty()) {
                    Profesor nuevo = new Profesor();
                    nuevo.setNombre(nombre);
                    nuevo.setApellido(apellido);
                    nuevo.setUsuario(usuarioValue);
                    nuevo.setContrasenia(contrasenia == null || contrasenia.isBlank() ? "password" : contrasenia.trim());
                    nuevo.setNivel(nivel);
                    nuevo.setCi(parseInteger(ciValue));
                    nuevo.setTelefono(parseInteger(telefonoValue));
                    nuevo.setCelular(parseInteger(celularValue));
                    nuevo.setCorreo(correo);
                    nuevo.setEspecialidadId(parseInteger(especialidadIdValue));

                    int createdId = profesorDao.create(nuevo);
                    if (createdId > 0) {
                        flashMessage = "Usuario creado correctamente.";
                    } else {
                        errors.add("No se pudo crear el usuario. Revise los datos e intente nuevamente.");
                    }
                }
            } else if ("edit".equals(action)) {
                String profesorIdValue = req.getParameter("profesorId");
                String nombre = req.getParameter("nombre");
                String apellido = req.getParameter("apellido");
                String usuarioValue = req.getParameter("usuario");
                String contrasenia = req.getParameter("contrasenia");
                String nivelValue = req.getParameter("nivel");
                String ciValue = req.getParameter("ci");
                String telefonoValue = req.getParameter("telefono");
                String celularValue = req.getParameter("celular");
                String correo = req.getParameter("correo");
                String especialidadIdValue = req.getParameter("especialidadId");

                if (profesorIdValue == null || profesorIdValue.isBlank()) {
                    errors.add("Falta el identificador del usuario para editar.");
                } else {
                    int profesorId = Integer.parseInt(profesorIdValue);
                    Profesor existing = profesorDao.findById(profesorId);
                    if (existing == null) {
                        errors.add("Profesor no encontrado.");
                    } else {
                        if (usuarioValue == null || usuarioValue.isBlank()) {
                            errors.add("El nombre de usuario es obligatorio.");
                        } else if (profesorDao.existsByUsuario(usuarioValue, profesorId)) {
                            errors.add("El usuario ya existe para otro profesor.");
                        }
                        int nivel = parseNivel(nivelValue, errors);
                        if (existing.getNivel() == 3 && nivel != 3 && profesorDao.countByNivel(3) <= 1) {
                            errors.add("No se puede demotar al último administrador activo.");
                        }
                        if (errors.isEmpty()) {
                            Profesor updated = new Profesor();
                            updated.setId(profesorId);
                            updated.setNombre(nombre);
                            updated.setApellido(apellido);
                            updated.setUsuario(usuarioValue);
                            updated.setContrasenia(contrasenia == null || contrasenia.isBlank() ? existing.getContrasenia() : contrasenia.trim());
                            updated.setNivel(nivel);
                            updated.setCi(parseInteger(ciValue));
                            updated.setTelefono(parseInteger(telefonoValue));
                            updated.setCelular(parseInteger(celularValue));
                            updated.setCorreo(correo);
                            updated.setEspecialidadId(parseInteger(especialidadIdValue));

                            if (profesorDao.update(updated)) {
                                flashMessage = "Usuario actualizado correctamente.";
                            } else {
                                errors.add("No se pudo actualizar el usuario.");
                            }
                        }
                    }
                }
            } else if ("delete".equals(action)) {
                int profesorId = Integer.parseInt(req.getParameter("profesorId"));
                if (profesorId == user.getId()) {
                    errors.add("No puede eliminar su propia cuenta mientras está conectado.");
                } else {
                    Profesor existing = profesorDao.findById(profesorId);
                    if (existing == null) {
                        errors.add("Profesor no encontrado.");
                    } else if (existing.getNivel() == 3 && profesorDao.countByNivel(3) <= 1) {
                        errors.add("No se puede eliminar al último administrador activo.");
                    } else {
                        if (profesorDao.delete(profesorId)) {
                            flashMessage = "Usuario eliminado correctamente.";
                        } else {
                            errors.add("No se pudo eliminar el usuario.");
                        }
                    }
                }
            } else if ("reset".equals(action)) {
                int profesorId = Integer.parseInt(req.getParameter("profesorId"));
                Profesor existing = profesorDao.findById(profesorId);
                if (existing == null) {
                    errors.add("Profesor no encontrado.");
                } else if (existing.getNivel() == 3 && profesorDao.countByNivel(3) <= 1 && profesorId == user.getId()) {
                    errors.add("No se puede restablecer la contraseña del último administrador con este formulario.");
                } else {
                    if (profesorDao.resetPassword(profesorId, "password")) {
                        flashMessage = "Contraseña restablecida a 'password'.";
                    } else {
                        errors.add("No se pudo restablecer la contraseña.");
                    }
                }
            }
        } catch (NumberFormatException ex) {
            errors.add("Se proporcionó un identificador inválido.");
        } catch (Exception ex) {
            log("Error ejecutando operación de administración de usuarios", ex);
            errors.add("Error del servidor al procesar la solicitud.");
        }

        if (!errors.isEmpty()) {
            session.setAttribute("errors", errors);
        } else if (flashMessage != null) {
            session.setAttribute("flashMessage", flashMessage);
        }

        resp.sendRedirect(req.getContextPath() + "/AdminUsuariosServlet");
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int parseNivel(String nivelValue, List<String> errors) {
        if (nivelValue == null || nivelValue.isBlank()) {
            errors.add("El nivel de usuario es obligatorio.");
            return 1;
        }
        try {
            int nivel = Integer.parseInt(nivelValue);
            if (nivel < 1 || nivel > 3) {
                errors.add("Nivel inválido. Elija profesor, evaluador o administrador.");
                return 1;
            }
            return nivel;
        } catch (NumberFormatException ex) {
            errors.add("Nivel inválido. Elija profesor, evaluador o administrador.");
            return 1;
        }
    }
}
