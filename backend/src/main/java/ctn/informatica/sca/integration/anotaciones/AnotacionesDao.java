package ctn.informatica.sca.integration.anotaciones;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * Consultas de la API de la app de anotaciones. Sobre las tablas del colegio solo hace SELECT;
 * las únicas escrituras son los INSERT en {@code anotacion} y {@code anotacion_foto}
 * (migraciones {@code 007}/{@code 008}). Las filas salen como mapas con los nombres de columna
 * exactos que espera la app (ver {@code ceciapi/API_REQUERIMIENTOS.md}).
 */
@Repository
public class AnotacionesDao extends conexion {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public record Tipo(boolean requiereFoto, boolean requiereObservacion, boolean repetible) {
    }

    public record Foto(String tipoMime, byte[] imagen) {
    }

    public Map<String, Object> profesorPorCi(long ci) throws SQLException {
        String sql = "SELECT id, nombre, apellido, ci, nivel FROM usuario WHERE ci = ? AND nivel = 1 LIMIT 1";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, ci);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("nombre", rs.getString("nombre"));
                row.put("apellido", rs.getString("apellido"));
                row.put("ci", rs.getLong("ci"));
                row.put("nivel", rs.getInt("nivel"));
                return row;
            }
        }
    }

    public Map<String, Object> menus(int profesorId) throws SQLException {
        Map<String, Object> out = new LinkedHashMap<>();
        try (Connection con = getCon()) {
            out.put("especialidades", lista(con,
                    "SELECT DISTINCT e.id, e.nombre FROM asignacion a "
                            + "JOIN curso_base cb ON cb.id = a.curso_base_id JOIN especialidad e ON e.id = cb.especialidad_id "
                            + "WHERE a.usuario_id = ? ORDER BY e.nombre",
                    profesorId, rs -> fila("id", rs.getInt("id"), "nombre", rs.getString("nombre"))));
            out.put("cursos", lista(con,
                    "SELECT DISTINCT cb.id, cb.especialidad_id, cb.nivel, cb.seccion FROM asignacion a "
                            + "JOIN curso_base cb ON cb.id = a.curso_base_id "
                            + "WHERE a.usuario_id = ? ORDER BY cb.especialidad_id, cb.nivel, cb.seccion",
                    profesorId, rs -> fila("id", rs.getInt("id"), "especialidad_id", rs.getInt("especialidad_id"),
                            "nivel", rs.getInt("nivel"), "seccion", rs.getString("seccion"),
                            "etiqueta", rs.getInt("nivel") + "° " + rs.getString("seccion"))));
            out.put("materias", lista(con,
                    "SELECT DISTINCT m.id, m.nombre, m.categoria FROM asignacion a JOIN materia m ON m.id = a.materia_id "
                            + "WHERE a.usuario_id = ? ORDER BY m.nombre",
                    profesorId, rs -> fila("id", rs.getInt("id"), "nombre", rs.getString("nombre"),
                            "categoria", rs.getString("categoria"))));
            out.put("asignaciones", lista(con,
                    "SELECT DISTINCT cb.especialidad_id, a.curso_base_id, a.materia_id FROM asignacion a "
                            + "JOIN curso_base cb ON cb.id = a.curso_base_id "
                            + "WHERE a.usuario_id = ? ORDER BY cb.especialidad_id, a.curso_base_id, a.materia_id",
                    profesorId, rs -> fila("especialidad_id", rs.getInt("especialidad_id"),
                            "curso_base_id", rs.getInt("curso_base_id"), "materia_id", rs.getInt("materia_id"))));
        }
        return out;
    }

    /** Alumnos del curso asignado: nivel 3 → promoción = año actual, nivel 2 → año + 1, nivel 1 → año + 2. */
    public List<Map<String, Object>> alumnos(int profesorId, int materiaId, int cursoBaseId, int anioActual) throws SQLException {
        String sql = "SELECT al.id, al.ci, al.nombre, al.apellido, al.curso_id FROM asignacion a "
                + "JOIN curso_base cb ON cb.id = a.curso_base_id "
                + "JOIN curso c ON c.especialidad_id = cb.especialidad_id AND c.seccion = cb.seccion "
                + "AND c.promocion = ? + (3 - cb.nivel) "
                + "JOIN alumno al ON al.curso_id = c.id "
                + "WHERE a.usuario_id = ? AND a.materia_id = ? AND a.curso_base_id = ? "
                + "ORDER BY al.apellido, al.nombre";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, anioActual);
            ps.setInt(2, profesorId);
            ps.setInt(3, materiaId);
            ps.setInt(4, cursoBaseId);
            List<Map<String, Object>> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(fila("id", rs.getInt("id"), "ci", rs.getString("ci"), "nombre", rs.getString("nombre"),
                            "apellido", rs.getString("apellido"), "curso_id", rs.getInt("curso_id")));
                }
            }
            return out;
        }
    }

    public List<Map<String, Object>> tiposActivos() throws SQLException {
        try (Connection con = getCon()) {
            return lista(con,
                    "SELECT id, descripcion, codigo_conducta, requiere_foto, requiere_observacion FROM tipo_anotacion "
                            + "WHERE activo = 1 ORDER BY orden, id",
                    null, rs -> fila("id", rs.getInt("id"), "descripcion", rs.getString("descripcion"),
                            "codigo_conducta", rs.getString("codigo_conducta"),
                            "requiere_foto", rs.getBoolean("requiere_foto"),
                            "requiere_observacion", rs.getBoolean("requiere_observacion")));
        }
    }

    public List<Map<String, Object>> historial(int alumnoId) throws SQLException {
        String sql = "SELECT a.id, a.alumno_id, a.usuario_id, a.tipo_id, a.materia_id, a.fecha_hora, a.observacion, "
                + "t.descripcion AS tipo_descripcion, t.requiere_foto, m.nombre AS materia_nombre, "
                + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                + "(SELECT f.id FROM anotacion_foto f WHERE f.anotacion_id = a.id ORDER BY f.id DESC LIMIT 1) AS foto_id "
                + "FROM anotacion a JOIN tipo_anotacion t ON t.id = a.tipo_id "
                + "LEFT JOIN materia m ON m.id = a.materia_id LEFT JOIN usuario u ON u.id = a.usuario_id "
                + "WHERE a.alumno_id = ? ORDER BY a.fecha_hora DESC, a.id DESC";
        try (Connection con = getCon()) {
            return lista(con, sql, alumnoId, rs -> {
                Integer fotoId = rs.getObject("foto_id", Integer.class);
                return fila("id", rs.getInt("id"), "alumno_id", rs.getInt("alumno_id"), "usuario_id", rs.getInt("usuario_id"),
                        "tipo_id", rs.getInt("tipo_id"), "materia_id", rs.getObject("materia_id", Integer.class),
                        "fecha_hora", rs.getObject("fecha_hora", LocalDateTime.class).format(FECHA_HORA),
                        "observacion", rs.getString("observacion"), "tipo_descripcion", rs.getString("tipo_descripcion"),
                        "requiere_foto", rs.getBoolean("requiere_foto"), "materia_nombre", rs.getString("materia_nombre"),
                        "profesor_nombre", rs.getString("profesor_nombre"), "profesor_apellido", rs.getString("profesor_apellido"),
                        "foto_id", fotoId, "tiene_foto", fotoId != null);
            });
        }
    }

    /** Por {@code id} de la foto, o la última foto de la anotación si {@code fotoId} es null. */
    public Foto foto(Integer fotoId, Integer anotacionId) throws SQLException {
        String sql = fotoId != null
                ? "SELECT tipo_mime, imagen FROM anotacion_foto WHERE id = ?"
                : "SELECT tipo_mime, imagen FROM anotacion_foto WHERE anotacion_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fotoId != null ? fotoId : anotacionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Foto(rs.getString("tipo_mime"), rs.getBytes("imagen")) : null;
            }
        }
    }

    public Tipo tipoActivo(int tipoId) throws SQLException {
        String sql = "SELECT requiere_foto, requiere_observacion, repetible FROM tipo_anotacion WHERE id = ? AND activo = 1";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, tipoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? new Tipo(rs.getBoolean("requiere_foto"), rs.getBoolean("requiere_observacion"), rs.getBoolean("repetible"))
                        : null;
            }
        }
    }

    public boolean existeAlumno(int id) throws SQLException {
        return existe("SELECT 1 FROM alumno WHERE id = ?", id);
    }

    public boolean existeProfesor(int id) throws SQLException {
        return existe("SELECT 1 FROM usuario WHERE id = ? AND nivel = 1", id);
    }

    public boolean existeMateria(int id) throws SQLException {
        return existe("SELECT 1 FROM materia WHERE id = ?", id);
    }

    /**
     * ¿Ya hay una anotación del mismo alumno y tipo en el día {@code [desde, hasta)}, cargada por
     * cualquier profesor? Con {@code compararObservacion} solo choca si la observación es idéntica
     * (incluido el caso de ambas null): es la regla del tipo "Otro...".
     */
    public boolean existeDuplicado(int alumnoId, int tipoId, LocalDateTime desde, LocalDateTime hasta,
            boolean compararObservacion, String observacion) throws SQLException {
        String sql = "SELECT 1 FROM anotacion WHERE alumno_id = ? AND tipo_id = ? AND fecha_hora >= ? AND fecha_hora < ?"
                + (compararObservacion ? " AND observacion <=> ?" : "") + " LIMIT 1";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, alumnoId);
            ps.setInt(2, tipoId);
            ps.setObject(3, desde);
            ps.setObject(4, hasta);
            if (compararObservacion) ps.setString(5, observacion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Inserta la anotación y su foto (si hay) en una sola transacción; devuelve el id de la anotación. */
    public int insertar(int alumnoId, int profesorId, int tipoId, Integer materiaId, LocalDateTime fechaHora,
            String observacion, byte[] foto) throws SQLException {
        try (Connection con = getCon()) {
            con.setAutoCommit(false);
            try {
                int id;
                String sql = "INSERT INTO anotacion (alumno_id, usuario_id, tipo_id, materia_id, fecha_hora, observacion) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, alumnoId);
                    ps.setInt(2, profesorId);
                    ps.setInt(3, tipoId);
                    ps.setObject(4, materiaId);
                    ps.setObject(5, fechaHora);
                    ps.setString(6, observacion);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getInt(1);
                    }
                }
                if (foto != null) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO anotacion_foto (anotacion_id, imagen, tipo_mime, tamano_bytes) VALUES (?, ?, 'image/jpeg', ?)")) {
                        ps.setInt(1, id);
                        ps.setBytes(2, foto);
                        ps.setInt(3, foto.length);
                        ps.executeUpdate();
                    }
                }
                con.commit();
                return id;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private boolean existe(String sql, int id) throws SQLException {
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
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
