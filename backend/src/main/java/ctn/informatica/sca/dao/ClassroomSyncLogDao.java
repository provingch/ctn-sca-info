package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.stereotype.Repository;

@Repository
public class ClassroomSyncLogDao extends conexion {

    public boolean insert(int planillaId, int usuarioId, int tareasCreadas, int calificacionesActualizadas) throws SQLException {
        final String sql = "INSERT INTO classroom_sync_log (planilla_id, usuario_id, tareas_creadas, calificaciones_actualizadas) VALUES (?, ?, ?, ?)";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, planillaId);
            ps.setInt(2, usuarioId);
            ps.setInt(3, tareasCreadas);
            ps.setInt(4, calificacionesActualizadas);
            return ps.executeUpdate() == 1;
        }
    }

    public Timestamp getLastSyncedAt() throws SQLException {
        final String sql = "SELECT MAX(synced_at) AS last_sync FROM classroom_sync_log";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getTimestamp("last_sync");
            }
        }
        return null;
    }
}
