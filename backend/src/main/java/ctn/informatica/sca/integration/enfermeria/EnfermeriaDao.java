package ctn.informatica.sca.integration.enfermeria;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

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
                    rs -> fila("id_alergia", rs.getInt("id_alergia"), "nombre", rs.getString("nombre")), alumnoId));
            List<Map<String, Object>> tutor = lista(con,
                    "SELECT nombre, apellido, correo, telefono FROM enf_tutor WHERE alumno_id = ? ORDER BY id_tutor LIMIT 1",
                    rs -> fila("nombre", rs.getString("nombre"), "apellido", rs.getString("apellido"),
                            "correo", rs.getString("correo"), "telefono", rs.getString("telefono")), alumnoId);
            out.put("tutor", tutor.isEmpty() ? fila("nombre", null, "apellido", null, "correo", null, "telefono", null) : tutor.get(0));
            // Padres ya cargados en el colegio (usuario nivel 4 vinculado por alumno_usuario). Solo lectura y
            // solo estas columnas: nunca contrasenia, totp_secret, google_*, firma, foto ni session_version.
            out.put("tutores_colegio", lista(con,
                    "SELECT u.id, u.nombre, u.apellido, u.celular, u.telefono, u.correo, au.parentesco "
                            + "FROM alumno_usuario au JOIN usuario u ON u.id = au.usuario_id "
                            + "WHERE au.alumno_id = ? AND u.nivel = 4 ORDER BY au.parentesco, u.apellido, u.nombre",
                    rs -> fila("id", rs.getInt("id"), "nombre", rs.getString("nombre"), "apellido", rs.getString("apellido"),
                            "celular", rs.getString("celular"), "telefono", rs.getString("telefono"),
                            "correo", rs.getString("correo"), "parentesco", rs.getString("parentesco")), alumnoId));
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
            return lista(con, "SELECT id_alergia, nombre FROM enf_alergia ORDER BY nombre",
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

    // ---------------------------------------------------------------- Consultas

    /** Campos de una consulta que manda la app (alta y edición). */
    public record DatosConsulta(int alumnoId, String motivo, LocalDate fecha, String tratamiento,
            LocalTime horaEntrada, LocalTime horaSalida, String observaciones) {
    }

    private static final String SELECT_CONSULTA = "SELECT c.idConsulta, c.alumno_id, c.motivo, c.fecha, c.tratamiento, "
            + "c.horaEntrada, c.horaSalida, c.observaciones, c.idUsuario, u.usuario "
            + "FROM enf_consulta c LEFT JOIN enf_usuario u ON u.id_usuario = c.idUsuario ";

    public List<Map<String, Object>> consultas(int alumnoId) throws SQLException {
        try (Connection con = getCon()) {
            return lista(con, SELECT_CONSULTA + "WHERE c.alumno_id = ? ORDER BY c.fecha DESC, c.horaEntrada DESC, c.idConsulta DESC",
                    EnfermeriaDao::consulta, alumnoId);
        }
    }

    /** Consulta con sus insumos; null si no existe. */
    public Map<String, Object> consulta(int id) throws SQLException {
        try (Connection con = getCon()) {
            List<Map<String, Object>> filas = lista(con, SELECT_CONSULTA + "WHERE c.idConsulta = ?", EnfermeriaDao::consulta, id);
            if (filas.isEmpty()) return null;
            Map<String, Object> out = filas.get(0);
            out.put("insumos", lista(con, "SELECT d.id_insumo, i.nombre, d.cantidad FROM enf_detalle_consulta d "
                    + "JOIN enf_insumo i ON i.id_insumo = d.id_insumo WHERE d.idConsulta = ? ORDER BY i.nombre",
                    EnfermeriaDao::insumoUsado, id));
            return out;
        }
    }

    /**
     * Alta de consulta: inserta consulta y detalle y descuenta stock, todo o nada. Si algún
     * insumo no alcanza, 409 y se revierte (el stock nunca queda negativo).
     */
    public int crearConsulta(DatosConsulta d, Map<Integer, Integer> insumos, int usuarioId) throws SQLException {
        return enTransaccion(con -> {
            int id = insertar(con, "INSERT INTO enf_consulta (alumno_id, motivo, fecha, tratamiento, horaEntrada, horaSalida, "
                    + "observaciones, idUsuario) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", d.alumnoId(), d.motivo(), d.fecha(),
                    d.tratamiento(), d.horaEntrada(), d.horaSalida(), d.observaciones(), usuarioId);
            usarInsumos(con, id, insumos);
            return id;
        });
    }

    /** Edición: devuelve el stock de los insumos anteriores y descuenta los nuevos, en una transacción. false si no existe. */
    public boolean editarConsulta(int id, DatosConsulta d, Map<Integer, Integer> insumos) throws SQLException {
        return enTransaccion(con -> {
            if (!bloquearConsulta(con, id)) return false;
            devolverInsumos(con, id);
            actualizar(con, "UPDATE enf_consulta SET alumno_id = ?, motivo = ?, fecha = ?, tratamiento = ?, horaEntrada = ?, "
                    + "horaSalida = ?, observaciones = ? WHERE idConsulta = ?", d.alumnoId(), d.motivo(), d.fecha(),
                    d.tratamiento(), d.horaEntrada(), d.horaSalida(), d.observaciones(), id);
            usarInsumos(con, id, insumos);
            return true;
        });
    }

    /** Devuelve el stock y borra la consulta (el detalle se va por cascada). false si no existe. */
    public boolean eliminarConsulta(int id) throws SQLException {
        return enTransaccion(con -> {
            if (!bloquearConsulta(con, id)) return false;
            devolverInsumos(con, id);
            actualizar(con, "DELETE FROM enf_consulta WHERE idConsulta = ?", id);
            return true;
        });
    }

    public List<String> motivos() throws SQLException {
        List<String> out = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(
                "SELECT DISTINCT motivo FROM enf_consulta ORDER BY motivo"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    /** Consultas de {@code [desde, hasta]} con alumno, curso e insumos, para los reportes Jasper. */
    public List<Map<String, Object>> reporteConsultas(LocalDate desde, LocalDate hasta, Integer alumnoId, String motivo,
            int anioActual) throws SQLException {
        String filtro = "WHERE c.fecha BETWEEN ? AND ?" + (alumnoId != null ? " AND c.alumno_id = ?" : "")
                + (motivo != null ? " AND c.motivo = ?" : "");
        List<Object> params = new ArrayList<>(List.of(desde, hasta));
        if (alumnoId != null) params.add(alumnoId);
        if (motivo != null) params.add(motivo);
        try (Connection con = getCon()) {
            List<Map<String, Object>> out = lista(con, "SELECT c.idConsulta, c.alumno_id, c.motivo, c.fecha, c.tratamiento, "
                    + "c.horaEntrada, c.horaSalida, c.observaciones, c.idUsuario, u.usuario, "
                    + "al.ci, al.nombre, al.apellido, cu.promocion, cu.seccion, e.nombre AS especialidad "
                    + "FROM enf_consulta c LEFT JOIN enf_usuario u ON u.id_usuario = c.idUsuario "
                    + "JOIN alumno al ON al.id = c.alumno_id JOIN curso cu ON cu.id = al.curso_id "
                    + "JOIN especialidad e ON e.id = cu.especialidad_id " + filtro
                    + " ORDER BY c.fecha, c.horaEntrada, c.idConsulta", rs -> {
                        Map<String, Object> row = consulta(rs);
                        row.put("ci", rs.getString("ci"));
                        row.put("nombre", rs.getString("nombre"));
                        row.put("apellido", rs.getString("apellido"));
                        row.put("curso", (anioActual - rs.getInt("promocion") + 3) + "° " + rs.getString("seccion"));
                        row.put("especialidad", rs.getString("especialidad"));
                        row.put("insumos", new ArrayList<Map<String, Object>>());
                        return row;
                    }, params.toArray());
            Map<Integer, Map<String, Object>> porId = new LinkedHashMap<>();
            for (Map<String, Object> row : out) porId.put((Integer) row.get("idConsulta"), row);
            for (Map<String, Object> det : lista(con, "SELECT d.idConsulta, d.id_insumo, i.nombre, d.cantidad "
                    + "FROM enf_detalle_consulta d JOIN enf_insumo i ON i.id_insumo = d.id_insumo "
                    + "JOIN enf_consulta c ON c.idConsulta = d.idConsulta " + filtro + " ORDER BY i.nombre",
                    rs -> {
                        Map<String, Object> row = insumoUsado(rs);
                        row.put("idConsulta", rs.getInt("idConsulta"));
                        return row;
                    }, params.toArray())) {
                Map<String, Object> consulta = porId.get((Integer) det.remove("idConsulta"));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> insumos = (List<Map<String, Object>>) consulta.get("insumos");
                insumos.add(det);
            }
            return out;
        }
    }

    // ---------------------------------------------------------------- Insumos y solicitudes

    public List<Map<String, Object>> insumos(boolean todos) throws SQLException {
        try (Connection con = getCon()) {
            return lista(con, "SELECT id_insumo, nombre, stock_actual, stock_minimo, activo FROM enf_insumo "
                    + (todos ? "" : "WHERE activo = 1 ") + "ORDER BY nombre",
                    rs -> fila("id_insumo", rs.getInt("id_insumo"), "nombre", rs.getString("nombre"),
                            "stock_actual", rs.getInt("stock_actual"), "stock_minimo", rs.getInt("stock_minimo"),
                            "activo", rs.getBoolean("activo")));
        }
    }

    /** Alta; si el nombre existe inactivo lo reactiva con el stock nuevo. 409 si existe activo. */
    public int crearInsumo(String nombre, int stockActual, int stockMinimo) throws SQLException {
        return enTransaccion(con -> {
            try (PreparedStatement ps = con.prepareStatement("SELECT id_insumo, activo FROM enf_insumo WHERE nombre = ? FOR UPDATE")) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getBoolean("activo")) throw new Rechazo(HttpStatus.CONFLICT, "Ya existe un insumo activo con ese nombre");
                        int id = rs.getInt("id_insumo");
                        actualizar(con, "UPDATE enf_insumo SET activo = 1, stock_actual = ?, stock_minimo = ? WHERE id_insumo = ?",
                                stockActual, stockMinimo, id);
                        return id;
                    }
                }
            }
            return insertar(con, "INSERT INTO enf_insumo (nombre, stock_actual, stock_minimo) VALUES (?, ?, ?)",
                    nombre, stockActual, stockMinimo);
        });
    }

    /** false si no existe; 409 si el nombre ya lo usa otro insumo. */
    public boolean editarInsumo(int id, String nombre, int stockActual, int stockMinimo) throws SQLException {
        try (Connection con = getCon()) {
            if (!existe(con, "SELECT 1 FROM enf_insumo WHERE id_insumo = ?", id)) return false;
            try {
                actualizar(con, "UPDATE enf_insumo SET nombre = ?, stock_actual = ?, stock_minimo = ? WHERE id_insumo = ?",
                        nombre, stockActual, stockMinimo, id);
            } catch (SQLIntegrityConstraintViolationException ex) {
                throw new Rechazo(HttpStatus.CONFLICT, "Ya existe otro insumo con ese nombre");
            }
            return true;
        }
    }

    public boolean insumoEstado(int id, boolean activo) throws SQLException {
        try (Connection con = getCon()) {
            if (!existe(con, "SELECT 1 FROM enf_insumo WHERE id_insumo = ?", id)) return false;
            actualizar(con, "UPDATE enf_insumo SET activo = ? WHERE id_insumo = ?", activo, id);
            return true;
        }
    }

    public boolean sumarStock(int id, int cantidad) throws SQLException {
        try (Connection con = getCon()) {
            return actualizar(con, "UPDATE enf_insumo SET stock_actual = stock_actual + ? WHERE id_insumo = ?", cantidad, id) > 0;
        }
    }

    public record ItemSolicitud(int insumoId, int cantidad, String observacion) {
    }

    /** Pedido de insumos con sus ítems, en una transacción. 422 si algún insumo no existe. */
    public int crearSolicitud(List<ItemSolicitud> items, LocalDate fecha, int usuarioId) throws SQLException {
        return enTransaccion(con -> {
            int id = insertar(con, "INSERT INTO enf_solicitud_insumo (fecha, id_usuario) VALUES (?, ?)", fecha, usuarioId);
            for (ItemSolicitud item : items) {
                if (!existe(con, "SELECT 1 FROM enf_insumo WHERE id_insumo = ?", item.insumoId())) {
                    throw new Rechazo(HttpStatus.UNPROCESSABLE_CONTENT, "El insumo " + item.insumoId() + " no existe");
                }
                actualizar(con, "INSERT INTO enf_detalle_solicitud_insumo (id_solicitud, id_insumo, cantidad, observacion) "
                        + "VALUES (?, ?, ?, ?)", id, item.insumoId(), item.cantidad(), item.observacion());
            }
            return id;
        });
    }

    public int registrarReporte(LocalDate fecha, int usuarioId) throws SQLException {
        try (Connection con = getCon()) {
            return insertar(con, "INSERT INTO enf_reporte (fecha, id_usuario) VALUES (?, ?)", fecha, usuarioId);
        }
    }

    public Map<String, Object> resumen() throws SQLException {
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(
                "SELECT (SELECT COUNT(*) FROM enf_ficha WHERE activo = 1) AS fichas, "
                        + "(SELECT COUNT(*) FROM enf_insumo WHERE activo = 1 AND stock_actual <= stock_minimo) AS bajos");
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return fila("fichas_activas", rs.getInt("fichas"), "insumos_stock_bajo", rs.getInt("bajos"));
        }
    }

    /** Rechazo de negocio dentro de una transacción: la revierte y sale como {@code {ok:false}} con su código. */
    public static class Rechazo extends ResponseStatusException {
        public Rechazo(HttpStatus status, String mensaje) {
            super(status, mensaje);
        }
    }

    private interface Trabajo<T> {
        T hacer(Connection con) throws SQLException;
    }

    private <T> T enTransaccion(Trabajo<T> trabajo) throws SQLException {
        try (Connection con = getCon()) {
            con.setAutoCommit(false);
            try {
                T out = trabajo.hacer(con);
                con.commit();
                return out;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private static boolean bloquearConsulta(Connection con, int id) throws SQLException {
        return existe(con, "SELECT 1 FROM enf_consulta WHERE idConsulta = ? FOR UPDATE", id);
    }

    /** Descuenta cada insumo solo si alcanza; ids en orden para que dos consultas no se bloqueen entre sí. */
    private static void usarInsumos(Connection con, int consultaId, Map<Integer, Integer> insumos) throws SQLException {
        for (Map.Entry<Integer, Integer> e : new TreeMap<>(insumos).entrySet()) {
            int filas = actualizar(con, "UPDATE enf_insumo SET stock_actual = stock_actual - ? "
                    + "WHERE id_insumo = ? AND stock_actual >= ?", e.getValue(), e.getKey(), e.getValue());
            if (filas == 0) {
                List<Map<String, Object>> insumo = lista(con, "SELECT nombre, stock_actual FROM enf_insumo WHERE id_insumo = ?",
                        rs -> fila("nombre", rs.getString("nombre"), "stock", rs.getInt("stock_actual")), e.getKey());
                if (insumo.isEmpty()) throw new Rechazo(HttpStatus.UNPROCESSABLE_CONTENT, "El insumo " + e.getKey() + " no existe");
                throw new Rechazo(HttpStatus.CONFLICT, "No hay stock suficiente de " + insumo.get(0).get("nombre")
                        + " (quedan " + insumo.get(0).get("stock") + ")");
            }
            actualizar(con, "INSERT INTO enf_detalle_consulta (idConsulta, id_insumo, cantidad) VALUES (?, ?, ?)",
                    consultaId, e.getKey(), e.getValue());
        }
    }

    private static void devolverInsumos(Connection con, int consultaId) throws SQLException {
        for (Map<String, Object> d : lista(con, "SELECT id_insumo, SUM(cantidad) AS cantidad FROM enf_detalle_consulta "
                + "WHERE idConsulta = ? GROUP BY id_insumo ORDER BY id_insumo",
                rs -> fila("id", rs.getInt("id_insumo"), "cantidad", rs.getInt("cantidad")), consultaId)) {
            actualizar(con, "UPDATE enf_insumo SET stock_actual = stock_actual + ? WHERE id_insumo = ?", d.get("cantidad"), d.get("id"));
        }
        actualizar(con, "DELETE FROM enf_detalle_consulta WHERE idConsulta = ?", consultaId);
    }

    private static Map<String, Object> consulta(ResultSet rs) throws SQLException {
        // DATE y TIME como texto: "2026-10-20" y "08:30:00", el formato que espera la app.
        return fila("idConsulta", rs.getInt("idConsulta"), "alumno_id", rs.getInt("alumno_id"), "idAlumno", rs.getInt("alumno_id"), "motivo", rs.getString("motivo"),
                "fecha", rs.getString("fecha"), "tratamiento", rs.getString("tratamiento"),
                "horaEntrada", rs.getString("horaEntrada"), "horaSalida", rs.getString("horaSalida"),
                "observaciones", rs.getString("observaciones"), "idUsuario", rs.getObject("idUsuario", Integer.class),
                "usuario", rs.getString("usuario"));
    }

    private static Map<String, Object> insumoUsado(ResultSet rs) throws SQLException {
        return fila("id_insumo", rs.getInt("id_insumo"), "nombre", rs.getString("nombre"), "cantidad", rs.getInt("cantidad"));
    }

    private static boolean existe(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int insertar(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
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
                "seccion", rs.getString("seccion"), "especialidad", rs.getString("especialidad"));
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

    private static List<Map<String, Object>> lista(Connection con, String sql, Mapper mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
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
