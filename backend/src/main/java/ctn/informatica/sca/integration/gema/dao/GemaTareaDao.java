package ctn.informatica.sca.integration.gema.dao;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * Acceso a la columna {@code tarea.gema_tarea_id} (Fase 2), agregada por la migración
 * {@code 006_add_gema_tarea_id.sql}. Separado de {@link ctn.informatica.sca.dao.TareaDao} porque
 * ese DAO no se toca: {@code TareaDao#insertarTarea} no incluye esta columna en su INSERT.
 * {@code TareaDao#update(Tarea)} sí puede reutilizarse tal cual para actualizar una tarea de GEMA
 * — no toca {@code gema_tarea_id} porque no está en su UPDATE, así que la preserva.
 */
@Repository
public class GemaTareaDao extends conexion {

    public record TareaRef(int id, int planillaId) {
    }

    public TareaRef findByGemaTareaId(String gemaTareaId) throws SQLException {
        String sql = "SELECT id, planilla_id FROM tarea WHERE gema_tarea_id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, gemaTareaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new TareaRef(rs.getInt("id"), rs.getInt("planilla_id"));
                }
            }
        }
        return null;
    }

    /** {@code tareaId -> gemaTareaId} para las tareas de origen GEMA de una planilla. */
    public Map<Integer, String> findGemaTareaIdsByPlanilla(int planillaId) throws SQLException {
        String sql = "SELECT id, gema_tarea_id FROM tarea WHERE planilla_id = ? AND gema_tarea_id IS NOT NULL";
        Map<Integer, String> out = new HashMap<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planillaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getInt("id"), rs.getString("gema_tarea_id"));
                }
            }
        }
        return out;
    }

    public int insert(int planillaId, int instrumentoId, LocalDate fecha, LocalDate fechaInicio,
            LocalDate fechaLimite, int total, String titulo, String gemaTareaId) throws SQLException {
        String sql = "INSERT INTO tarea (planilla_id, instrumento_id, fecha, fecha_inicio, fecha_limite, total, titulo, gema_tarea_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, planillaId);
            ps.setInt(2, instrumentoId);
            ps.setDate(3, java.sql.Date.valueOf(fecha));
            ps.setDate(4, fechaInicio != null ? java.sql.Date.valueOf(fechaInicio) : null);
            ps.setDate(5, fechaLimite != null ? java.sql.Date.valueOf(fechaLimite) : null);
            ps.setInt(6, total);
            ps.setString(7, titulo);
            ps.setString(8, gemaTareaId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Inserting tarea de GEMA failed, no se generó id");
    }
}
