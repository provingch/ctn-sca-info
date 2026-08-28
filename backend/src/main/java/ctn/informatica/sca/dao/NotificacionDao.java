package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class NotificacionDao extends conexion {

    public static String resolveUserType(UserDao userDao, int userId) {
        try {
            User user = userDao == null ? null : userDao.findById(userId);
            if (user == null) {
                return "profesor";
            }
            return user.getLevel() == 4 ? "padre" : "profesor";
        } catch (Exception ex) {
            return "profesor";
        }
    }

    public boolean crear(int usuarioId, String userType, String tipo, String titulo, String cuerpo,
            String entidadTipo, Long entidadId) throws SQLException {
        String sql = "INSERT INTO notificacion (usuario_id, user_type, tipo, titulo, cuerpo, entidad_tipo, entidad_id, leida) VALUES (?, ?, ?, ?, ?, ?, ?, 0)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, userType == null || userType.isBlank() ? "profesor" : userType.trim());
            ps.setString(3, tipo == null || tipo.isBlank() ? "GENERAL" : tipo.trim());
            ps.setString(4, titulo == null ? "" : titulo.trim());
            ps.setString(5, cuerpo == null ? "" : cuerpo.trim());
            ps.setString(6, entidadTipo);
            if (entidadId == null) {
                ps.setNull(7, java.sql.Types.BIGINT);
            } else {
                ps.setLong(7, entidadId);
            }
            return ps.executeUpdate() > 0;
        }
    }

    public List<Map<String, Object>> listarPorUsuario(int usuarioId, String userType, boolean soloNoLeidas) throws SQLException {
        String sql = "SELECT id, usuario_id, user_type, tipo, titulo, cuerpo, entidad_tipo, entidad_id, leida, created_at FROM notificacion WHERE usuario_id = ? AND user_type = ?";
        if (soloNoLeidas) {
            sql += " AND leida = 0";
        }
        sql += " ORDER BY created_at DESC, id DESC";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, userType == null || userType.isBlank() ? "profesor" : userType.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("usuarioId", rs.getInt("usuario_id"));
                    row.put("userType", rs.getString("user_type"));
                    row.put("tipo", rs.getString("tipo"));
                    row.put("titulo", rs.getString("titulo"));
                    row.put("cuerpo", rs.getString("cuerpo"));
                    row.put("entidadTipo", rs.getString("entidad_tipo"));
                    row.put("entidadId", rs.getObject("entidad_id", Long.class));
                    row.put("leida", rs.getBoolean("leida"));
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    items.add(row);
                }
            }
        }
        return items;
    }

    public boolean marcarLeida(int id, int usuarioId, String userType) throws SQLException {
        String sql = "UPDATE notificacion SET leida = 1 WHERE id = ? AND usuario_id = ? AND user_type = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, usuarioId);
            ps.setString(3, userType == null || userType.isBlank() ? "profesor" : userType.trim());
            return ps.executeUpdate() > 0;
        }
    }

    public long contarNoLeidas(int usuarioId, String userType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notificacion WHERE usuario_id = ? AND user_type = ? AND leida = 0";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, userType == null || userType.isBlank() ? "profesor" : userType.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0L;
    }
}
