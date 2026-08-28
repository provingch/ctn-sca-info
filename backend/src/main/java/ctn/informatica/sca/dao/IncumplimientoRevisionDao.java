package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class IncumplimientoRevisionDao extends conexion {

    public int registrar(int asignacionId, int usuarioId, Integer temaPlanCurricularId, String tipo, String descripcion,
            String estado, Integer evaluadorId, LocalDateTime suspensionDesde, LocalDateTime suspensionHasta) throws SQLException {
        String sql = "INSERT INTO incumplimiento_revision (asignacion_id, usuario_id, tema_plan_curricular_id, tipo, descripcion, estado, evaluador_id, fecha_resolucion, suspension_desde, suspension_hasta) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, usuarioId);
            if (temaPlanCurricularId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, temaPlanCurricularId);
            }
            ps.setString(4, tipo == null || tipo.isBlank() ? "ATRASO" : tipo.trim().toUpperCase());
            ps.setString(5, descripcion == null ? "" : descripcion.trim());
            ps.setString(6, estado == null || estado.isBlank() ? "PENDIENTE" : estado.trim().toUpperCase());
            if (evaluadorId == null) {
                ps.setNull(7, java.sql.Types.INTEGER);
            } else {
                ps.setInt(7, evaluadorId);
            }
            if (estado != null && "PENDIENTE".equalsIgnoreCase(estado)) {
                ps.setNull(8, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            }
            if (suspensionDesde == null) {
                ps.setNull(9, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(9, Timestamp.valueOf(suspensionDesde));
            }
            if (suspensionHasta == null) {
                ps.setNull(10, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(10, Timestamp.valueOf(suspensionHasta));
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return 0;
    }

    public long contarPorAsignacionYUsuario(int asignacionId, int usuarioId, String tipo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM incumplimiento_revision WHERE asignacion_id = ? AND usuario_id = ? AND tipo = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, asignacionId);
            ps.setInt(2, usuarioId);
            ps.setString(3, tipo == null || tipo.isBlank() ? "ATRASO" : tipo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0L;
    }

    public List<Map<String, Object>> listarPendientes() throws SQLException {
        String sql = "SELECT ir.id, ir.asignacion_id, ir.usuario_id, ir.tipo, ir.descripcion, ir.estado, ir.evaluador_id, ir.fecha_resolucion, ir.suspension_desde, ir.suspension_hasta, "
                + "u.nombre AS usuario_nombre, u.apellido AS usuario_apellido, a.materia_id, a.curso_id "
                + "FROM incumplimiento_revision ir "
                + "LEFT JOIN usuario u ON u.id = ir.usuario_id "
                + "LEFT JOIN asignacion a ON a.id = ir.asignacion_id "
                + "WHERE ir.estado = 'PENDIENTE' ORDER BY ir.created_at DESC";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("asignacionId", rs.getInt("asignacion_id"));
                row.put("usuarioId", rs.getInt("usuario_id"));
                row.put("tipo", rs.getString("tipo"));
                row.put("descripcion", rs.getString("descripcion"));
                row.put("estado", rs.getString("estado"));
                row.put("evaluadorId", rs.getObject("evaluador_id", Integer.class));
                row.put("fechaResolucion", rs.getTimestamp("fecha_resolucion"));
                row.put("suspensionDesde", rs.getTimestamp("suspension_desde"));
                row.put("suspensionHasta", rs.getTimestamp("suspension_hasta"));
                row.put("usuarioNombre", rs.getString("usuario_nombre"));
                row.put("usuarioApellido", rs.getString("usuario_apellido"));
                row.put("materiaId", rs.getObject("materia_id", Integer.class));
                row.put("cursoId", rs.getObject("curso_id", Integer.class));
                items.add(row);
            }
        }
        return items;
    }

    public boolean resolver(int id, String estado, int evaluadorId) throws SQLException {
        String sql = "UPDATE incumplimiento_revision SET estado = ?, evaluador_id = ?, fecha_resolucion = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado == null || estado.isBlank() ? "PERMITIDO" : estado.trim().toUpperCase());
            ps.setInt(2, evaluadorId);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }
}
