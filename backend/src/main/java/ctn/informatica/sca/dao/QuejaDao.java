package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class QuejaDao extends conexion {

    public record Revision(String estado, java.sql.Timestamp revisadaEn, int revisadaPor, String conclusion) {}

    public record Aceptacion(java.sql.Timestamp aceptadaEn, int aceptadaPor) {}

    public record Rechazo(java.sql.Timestamp rechazadaEn, int rechazadaPor, String motivoRechazo) {}

    public record Documento(long id, int especialidadId, String especialidad, String profesor, String curso,
            String motivo, java.sql.Timestamp creadaEn, java.sql.Timestamp revisadaEn, String conclusion,
            String procesoRevision, String solucionAplicada, String corregidaPorNombre, java.sql.Timestamp resueltaEn) {}
    public record Resolucion(String estado, String procesoRevision, String solucionAplicada,
            String corregidaPorNombre, java.sql.Timestamp resueltaEn) {}

    public Documento documento(long id) throws SQLException {
        String sql = "SELECT q.*, e.nombre AS especialidad, CONCAT_WS(' ', u.nombre, u.apellido) AS profesor, "
                + "CONCAT(cb.nivel, '° ', cb.seccion) AS curso FROM queja q "
                + "LEFT JOIN especialidad e ON e.id = q.especialidad_id "
                + "LEFT JOIN usuario u ON u.id = q.profesor_id LEFT JOIN curso_base cb ON cb.id = q.curso_id WHERE q.id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Documento(id, rs.getInt("especialidad_id"), rs.getString("especialidad"), rs.getString("profesor"),
                        rs.getString("curso"), rs.getString("motivo"), rs.getTimestamp("creada_en"), rs.getTimestamp("revisada_en"),
                        rs.getString("conclusion"), rs.getString("proceso_revision"), rs.getString("solucion_aplicada"),
                        rs.getString("corregida_por_nombre"), rs.getTimestamp("resuelta_en"));
            }
        }
    }

    public Resolucion resolver(long id, int especialidadId, String proceso, String solucion, String nombre, int userId) throws SQLException {
        String sql = "UPDATE queja SET proceso_revision = ?, solucion_aplicada = ?, corregida_por_nombre = ?, "
                + "resuelta_en = CURRENT_TIMESTAMP, resolucion_registrada_por = ? "
                + "WHERE id = ? AND especialidad_id = ? AND revisada_en IS NOT NULL AND resuelta_en IS NULL";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, proceso); ps.setString(2, solucion); ps.setString(3, nombre); ps.setInt(4, userId);
            ps.setLong(5, id); ps.setInt(6, especialidadId);
            if (ps.executeUpdate() == 0) return null;
            try (PreparedStatement read = con.prepareStatement("SELECT resuelta_en FROM queja WHERE id = ?")) {
                read.setLong(1, id);
                try (ResultSet rs = read.executeQuery()) {
                    if (!rs.next()) throw new SQLException("No se encontró la resolución guardada");
                    return new Resolucion("resuelta", proceso, solucion, nombre, rs.getTimestamp(1));
                }
            }
        }
    }

    public Integer findProfesorId(long id) throws SQLException {
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement("SELECT profesor_id FROM queja WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    // Solo se puede aceptar o rechazar una vez: la segunda solicitud (de cualquiera de los dos) no encuentra fila y vuelve null.
    public Aceptacion aceptar(long id, int userId) throws SQLException {
        String sql = "UPDATE queja SET aceptada_en = CURRENT_TIMESTAMP, aceptada_por = ? "
                + "WHERE id = ? AND aceptada_en IS NULL AND rechazada_en IS NULL";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setLong(2, id);
            if (ps.executeUpdate() == 0) return null;
            try (PreparedStatement read = con.prepareStatement("SELECT aceptada_en FROM queja WHERE id = ?")) {
                read.setLong(1, id);
                try (ResultSet rs = read.executeQuery()) {
                    if (!rs.next()) throw new SQLException("No se encontró la aceptación guardada");
                    return new Aceptacion(rs.getTimestamp(1), userId);
                }
            }
        }
    }

    public Rechazo rechazar(long id, int userId, String motivo) throws SQLException {
        String sql = "UPDATE queja SET rechazada_en = CURRENT_TIMESTAMP, rechazada_por = ?, motivo_rechazo = ? "
                + "WHERE id = ? AND aceptada_en IS NULL AND rechazada_en IS NULL";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, motivo);
            ps.setLong(3, id);
            if (ps.executeUpdate() == 0) return null;
            try (PreparedStatement read = con.prepareStatement("SELECT rechazada_en FROM queja WHERE id = ?")) {
                read.setLong(1, id);
                try (ResultSet rs = read.executeQuery()) {
                    if (!rs.next()) throw new SQLException("No se encontró el rechazo guardado");
                    return new Rechazo(rs.getTimestamp(1), userId, motivo);
                }
            }
        }
    }

    public Integer findEspecialidadId(long id) throws SQLException {
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement("SELECT especialidad_id FROM queja WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    // La condición evita que dos revisores sobrescriban la conclusión del otro, y exige que
    // Coordinación haya aceptado la queja antes de poder revisarla (flujo: aceptada -> revisada).
    public Revision completarRevision(long id, int especialidadId, String conclusion, int revisadaPor) throws SQLException {
        String sql = "UPDATE queja SET conclusion = ?, revisada_por = ?, revisada_en = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND especialidad_id = ? AND revisada_en IS NULL AND aceptada_en IS NOT NULL";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, conclusion);
            ps.setInt(2, revisadaPor);
            ps.setLong(3, id);
            ps.setInt(4, especialidadId);
            if (ps.executeUpdate() == 0) return null;
            try (PreparedStatement read = con.prepareStatement("SELECT revisada_en FROM queja WHERE id = ?")) {
                read.setLong(1, id);
                try (ResultSet rs = read.executeQuery()) {
                    if (!rs.next()) throw new SQLException("No se encontró la revisión guardada");
                    return new Revision("revisada", rs.getTimestamp(1), revisadaPor, conclusion);
                }
            }
        }
    }

    public int crear(int profesorId, int cursoId, int especialidadId, String tipo, String motivo, int creadaPor) throws SQLException {
        String sql = "INSERT INTO queja (profesor_id, curso_id, especialidad_id, tipo, motivo, creada_por) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, profesorId);
            ps.setInt(2, cursoId);
            ps.setInt(3, especialidadId);
            ps.setString(4, tipo);
            ps.setString(5, motivo == null ? "" : motivo.trim());
            ps.setInt(6, creadaPor);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return 0;
    }

    // El umbral de "profesor con quejas acumuladas" no debe contar las que el propio profesor
    // cargó sobre un curso (tipo CONTRA_CURSO).
    public long contarPorProfesor(int profesorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM queja WHERE profesor_id = ? AND tipo = 'CONTRA_PROFESOR'";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0L;
    }

    private static final String LISTAR_COLUMNAS =
            "SELECT q.id, q.profesor_id, q.curso_id, q.especialidad_id, q.tipo, q.motivo, q.creada_por, q.creada_en, "
                    + "q.aceptada_en, q.aceptada_por, q.rechazada_en, q.rechazada_por, q.motivo_rechazo, "
                    + "q.revisada_en, q.revisada_por, q.conclusion, q.proceso_revision, q.solucion_aplicada, q.corregida_por_nombre, q.resuelta_en, "
                    + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, "
                    + "cb.seccion AS curso_seccion, cb.nivel AS curso_nivel, "
                    + "e.nombre AS curso_especialidad "
                    + "FROM queja q "
                    + "LEFT JOIN usuario u ON u.id = q.profesor_id "
                    + "LEFT JOIN curso_base cb ON cb.id = q.curso_id "
                    + "LEFT JOIN especialidad e ON e.id = cb.especialidad_id ";

    public List<Map<String, Object>> listar() throws SQLException {
        String sql = LISTAR_COLUMNAS + "ORDER BY q.creada_en DESC";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(mapRow(rs));
        }
        return items;
    }

    /** Quejas cargadas por un usuario dado (p.ej. un profesor viendo sus propias quejas CONTRA_CURSO). */
    public List<Map<String, Object>> listarCreadasPor(int creadaPor) throws SQLException {
        String sql = LISTAR_COLUMNAS + "WHERE q.creada_por = ? ORDER BY q.creada_en DESC";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, creadaPor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapRow(rs));
            }
        }
        return items;
    }

    private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("profesorId", rs.getLong("profesor_id"));
        row.put("cursoId", rs.getLong("curso_id"));
        row.put("especialidadId", rs.getInt("especialidad_id"));
        row.put("tipo", rs.getString("tipo"));
        row.put("motivo", rs.getString("motivo"));
        row.put("creadaPor", rs.getLong("creada_por"));
        row.put("creadaEn", rs.getTimestamp("creada_en"));
        // Orden de evaluación: rechazada (terminal) > resuelta > revisada > aceptada > pendiente.
        row.put("estado", rs.getTimestamp("rechazada_en") != null ? "rechazada"
                : rs.getTimestamp("resuelta_en") != null ? "resuelta"
                : rs.getTimestamp("revisada_en") != null ? "revisada"
                : rs.getTimestamp("aceptada_en") != null ? "aceptada"
                : "pendiente");
        row.put("aceptadaEn", rs.getTimestamp("aceptada_en"));
        row.put("aceptadaPor", rs.getObject("aceptada_por"));
        row.put("rechazadaEn", rs.getTimestamp("rechazada_en"));
        row.put("rechazadaPor", rs.getObject("rechazada_por"));
        row.put("motivoRechazo", rs.getString("motivo_rechazo"));
        row.put("resueltaEn", rs.getTimestamp("resuelta_en"));
        row.put("procesoRevision", rs.getString("proceso_revision"));
        row.put("solucionAplicada", rs.getString("solucion_aplicada"));
        row.put("corregidaPorNombre", rs.getString("corregida_por_nombre"));
        row.put("revisadaEn", rs.getTimestamp("revisada_en"));
        row.put("revisadaPor", rs.getObject("revisada_por"));
        row.put("conclusion", rs.getString("conclusion"));
        row.put("profesorNombre", rs.getString("profesor_nombre"));
        row.put("profesorApellido", rs.getString("profesor_apellido"));
        row.put("cursoEspecialidad", rs.getString("curso_especialidad"));
        row.put("cursoSeccion", rs.getString("curso_seccion"));
        row.put("cursoNivel", (Integer) rs.getObject("curso_nivel"));
        return row;
    }
}
