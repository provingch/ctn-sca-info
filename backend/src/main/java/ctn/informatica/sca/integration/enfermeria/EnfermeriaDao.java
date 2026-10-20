package ctn.informatica.sca.integration.enfermeria;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * Consultas de la API de enfermería (SHME). Sobre las tablas del colegio ({@code alumno},
 * {@code curso}, {@code especialidad}) solo hace SELECT; todas las escrituras van a tablas
 * {@code enf_*} (migración {@code 009}). Contrato: {@code shmeapi/API_ENFERMERIA_REQUERIMIENTOS.md}.
 */
@Repository
public class EnfermeriaDao extends conexion {

    public record Usuario(int id, String usuario, String hash, String rol) {
    }

    /** Lanzada cuando un INSERT choca con una clave única (usuario repetido). */
    public static class Duplicado extends Exception {
    }

    // Alumno + curso + ficha. El año sale de la promoción como en el resto del sistema: 3° = promoción del año actual.
    private static final String SELECT_ALUMNO = "SELECT al.id, al.ci, al.nombre, al.apellido, c.promocion, c.seccion, "
            + "e.nombre AS especialidad, f.alumno_id AS ficha_id, f.activo, f.enfermedadBase "
            + "FROM alumno al JOIN curso c ON c.id = al.curso_id JOIN especialidad e ON e.id = c.especialidad_id "
            + "LEFT JOIN enf_ficha f ON f.alumno_id = al.id ";

    public Usuario usuarioPorNombre(String usuario) throws SQLException {
        return usuario("SELECT id_usuario, usuario, contrasena, rol FROM enf_usuario WHERE usuario = ?", usuario);
    }

    public Usuario usuarioPorId(int id) throws SQLException {
        return usuario("SELECT id_usuario, usuario, contrasena, rol FROM enf_usuario WHERE id_usuario = ?", id);
    }

    public int crearUsuario(String usuario, String hash, String rol) throws SQLException, Duplicado {
        String sql = "INSERT INTO enf_usuario (usuario, contrasena, rol) VALUES (?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario);
            ps.setString(2, hash);
            ps.setString(3, rol);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new Duplicado();
        }
    }

    public void cambiarContrasena(int id, String hash) throws SQLException {
        try (Connection con = getCon();
                PreparedStatement ps = con.prepareStatement("UPDATE enf_usuario SET contrasena = ? WHERE id_usuario = ?")) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Busca por CI, nombre, apellido o "nombre apellido"; {@code q} vacío lista todos. Solo lectura. */
    public List<Map<String, Object>> buscarAlumnos(String q, int pagina, int limite, int anioActual) throws SQLException {
        String patron = "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String sql = SELECT_ALUMNO
                + "WHERE al.ci LIKE ? OR al.nombre LIKE ? OR al.apellido LIKE ? OR CONCAT(al.nombre, ' ', al.apellido) LIKE ? "
                + "ORDER BY al.apellido, al.nombre, al.id LIMIT ? OFFSET ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, patron);
            ps.setInt(5, limite);
            ps.setInt(6, (pagina - 1) * limite);
            List<Map<String, Object>> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = alumno(rs, anioActual);
                    row.put("tiene_ficha", rs.getObject("ficha_id") != null);
                    row.put("ficha_activa", rs.getObject("ficha_id") != null && rs.getBoolean("activo"));
                    out.add(row);
                }
            }
            return out;
        }
    }

    /** Datos del alumno + ficha (vacía si todavía no tiene); null si el alumno no existe. */
    public Map<String, Object> ficha(int alumnoId, int anioActual) throws SQLException {
        try (Connection con = getCon()) {
            Map<String, Object> out;
            try (PreparedStatement ps = con.prepareStatement(SELECT_ALUMNO + "WHERE al.id = ?")) {
                ps.setInt(1, alumnoId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    out = alumno(rs, anioActual);
                    boolean tiene = rs.getObject("ficha_id") != null;
                    out.put("tiene_ficha", tiene);
                    out.put("enfermedadBase", rs.getString("enfermedadBase"));
                    out.put("activo", tiene ? rs.getBoolean("activo") : null);
                }
            }
            out.put("alergias", lista(con, "SELECT a.id_alergia, a.nombre FROM enf_detalle_alergia d "
                    + "JOIN enf_alergia a ON a.id_alergia = d.id_alergia WHERE d.alumno_id = ? ORDER BY a.nombre",
                    alumnoId, rs -> fila("id_alergia", rs.getInt("id_alergia"), "nombre", rs.getString("nombre"))));
            List<Map<String, Object>> tutor = lista(con,
                    "SELECT nombre, apellido, correo, telefono FROM enf_tutor WHERE alumno_id = ? ORDER BY id_tutor LIMIT 1",
                    alumnoId, rs -> fila("nombre", rs.getString("nombre"), "apellido", rs.getString("apellido"),
                            "correo", rs.getString("correo"), "telefono", rs.getString("telefono")));
            out.put("tutor", tutor.isEmpty() ? fila("nombre", null, "apellido", null, "correo", null, "telefono", null) : tutor.get(0));
            return out;
        }
    }

    public boolean existeAlumno(int id) throws SQLException {
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement("SELECT 1 FROM alumno WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Cuántos de esos ids existen en el catálogo (para validar antes de guardar la ficha). */
    public int contarAlergias(List<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return 0;
        String sql = "SELECT COUNT(*) FROM enf_alergia WHERE id_alergia IN (" + "?,".repeat(ids.size() - 1) + "?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Upsert de ficha y tutor + reemplazo de alergias, en una sola transacción. Nunca toca {@code alumno}. */
    public void guardarFicha(int alumnoId, String enfermedadBase, List<Integer> alergias,
            String tutorNombre, String tutorApellido, String tutorCorreo, String tutorTelefono) throws SQLException {
        try (Connection con = getCon()) {
            con.setAutoCommit(false);
            try {
                actualizar(con, "INSERT INTO enf_ficha (alumno_id, enfermedadBase) VALUES (?, ?) "
                        + "ON DUPLICATE KEY UPDATE enfermedadBase = VALUES(enfermedadBase)", alumnoId, enfermedadBase);
                actualizar(con, "DELETE FROM enf_detalle_alergia WHERE alumno_id = ?", alumnoId);
                for (int alergia : alergias) {
                    actualizar(con, "INSERT INTO enf_detalle_alergia (alumno_id, id_alergia) VALUES (?, ?)", alumnoId, alergia);
                }
                // enf_tutor no tiene clave única por alumno: se actualiza el primero y, si no hay, se inserta.
                int filas = actualizar(con, "UPDATE enf_tutor SET nombre = ?, apellido = ?, correo = ?, telefono = ? "
                        + "WHERE alumno_id = ? ORDER BY id_tutor LIMIT 1",
                        tutorNombre, tutorApellido, tutorCorreo, tutorTelefono, alumnoId);
                if (filas == 0) {
                    actualizar(con, "INSERT INTO enf_tutor (alumno_id, nombre, apellido, correo, telefono) VALUES (?, ?, ?, ?, ?)",
                            alumnoId, tutorNombre, tutorApellido, tutorCorreo, tutorTelefono);
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /** false si el alumno no tiene ficha. */
    public boolean fichaEstado(int alumnoId, boolean activo) throws SQLException {
        try (Connection con = getCon()) {
            // Cuenta filas encontradas, no cambiadas: activar una ficha ya activa también es "ok".
            try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM enf_ficha WHERE alumno_id = ?")) {
                ps.setInt(1, alumnoId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                }
            }
            actualizar(con, "UPDATE enf_ficha SET activo = ? WHERE alumno_id = ?", activo, alumnoId);
            return true;
        }
    }

    public List<Map<String, Object>> alergias() throws SQLException {
        try (Connection con = getCon()) {
            return lista(con, "SELECT id_alergia, nombre FROM enf_alergia ORDER BY nombre", null,
                    rs -> fila("id_alergia", rs.getInt("id_alergia"), "nombre", rs.getString("nombre")));
        }
    }

    /** Devuelve el id de la alergia, creándola si no existía. */
    public int alergiaCrear(String nombre) throws SQLException {
        try (Connection con = getCon()) {
            actualizar(con, "INSERT IGNORE INTO enf_alergia (nombre) VALUES (?)", nombre);
            try (PreparedStatement ps = con.prepareStatement("SELECT id_alergia FROM enf_alergia WHERE nombre = ?")) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }

    private Usuario usuario(String sql, Object param) throws SQLException {
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? new Usuario(rs.getInt("id_usuario"), rs.getString("usuario"), rs.getString("contrasena"), rs.getString("rol"))
                        : null;
            }
        }
    }

    private static Map<String, Object> alumno(ResultSet rs, int anioActual) throws SQLException {
        int anio = anioActual - rs.getInt("promocion") + 3;
        return fila("id", rs.getInt("id"), "ci", rs.getString("ci"), "nombre", rs.getString("nombre"),
                "apellido", rs.getString("apellido"), "curso", anio + "° " + rs.getString("seccion"),
                "especialidad", rs.getString("especialidad"));
    }

    private static int actualizar(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            return ps.executeUpdate();
        }
    }

    private interface Mapper {
        Map<String, Object> map(ResultSet rs) throws SQLException;
    }

    private static List<Map<String, Object>> lista(Connection con, String sql, Integer param, Mapper mapper) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            List<Map<String, Object>> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapper.map(rs));
            }
            return out;
        }
    }

    /** Mapa ordenado a partir de pares clave/valor (admite valores null, a diferencia de {@code Map.of}). */
    private static Map<String, Object> fila(Object... kv) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) row.put((String) kv[i], kv[i + 1]);
        return row;
    }
}
