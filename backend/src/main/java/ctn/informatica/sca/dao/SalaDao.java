package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Sala;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SalaDao extends conexion {
    private Sala from(ResultSet rs) throws SQLException {
        Sala sala = new Sala(rs.getInt("id"), rs.getString("nombre"),
                rs.getObject("especialidad_id") == null ? null : rs.getInt("especialidad_id"),
                rs.getString("especialidad_nombre"));
        sala.setBloquesAsignados(rs.getInt("bloques_asignados"));
        return sala;
    }

    public List<Sala> findAll() throws SQLException { return query(null, false); }

    public List<Sala> findByEspecialidad(Integer especialidadId) throws SQLException { return query(especialidadId, true); }

    private List<Sala> query(Integer especialidadId, boolean visible) throws SQLException {
        String sql = "SELECT s.id, s.nombre, s.especialidad_id, e.nombre AS especialidad_nombre, "
                + "(SELECT COUNT(*) FROM horario_slot hs WHERE hs.sala_id = s.id) AS bloques_asignados FROM sala s "
                + "LEFT JOIN especialidad e ON e.id = s.especialidad_id "
                + (visible ? "WHERE s.especialidad_id IS NULL OR s.especialidad_id = ? " : "")
                + "ORDER BY s.especialidad_id IS NOT NULL, e.nombre, s.nombre";
        List<Sala> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (visible) ps.setInt(1, especialidadId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(from(rs)); }
        }
        return out;
    }

    public int crear(String nombre, Integer especialidadId) throws SQLException {
        String sql = "INSERT INTO sala (nombre, especialidad_id) VALUES (?, ?)";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre); if (especialidadId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, especialidadId);
            ps.executeUpdate(); try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
        }
        return -1;
    }

    public boolean editar(int id, String nombre, Integer especialidadId) throws SQLException {
        String sql = "UPDATE sala SET nombre = ?, especialidad_id = ? WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre); if (especialidadId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, especialidadId); ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        try (Connection c = getCon()) {
            c.setAutoCommit(false);
            try {
                // Lock the parent row: concurrent horario inserts require a FK lock
                // on this same room, so usage cannot change between check and delete.
                try (PreparedStatement lock = c.prepareStatement("SELECT id FROM sala WHERE id = ? FOR UPDATE")) {
                    lock.setInt(1, id);
                    try (ResultSet row = lock.executeQuery()) {
                        if (!row.next()) { c.rollback(); return false; }
                    }
                }
                try (PreparedStatement usage = c.prepareStatement("SELECT id FROM horario_slot WHERE sala_id = ? LIMIT 1 FOR UPDATE")) {
                    usage.setInt(1, id);
                    try (ResultSet row = usage.executeQuery()) {
                        if (row.next()) throw new SalaEnUsoException();
                    }
                }
                try (PreparedStatement delete = c.prepareStatement("DELETE FROM sala WHERE id = ?")) {
                    delete.setInt(1, id);
                    boolean deleted = delete.executeUpdate() == 1;
                    c.commit();
                    return deleted;
                }
            } catch (SQLException error) {
                c.rollback();
                throw error;
            }
        }
    }

    public static class SalaEnUsoException extends SQLException {
        public SalaEnUsoException() { super("La sala tiene bloques de horario asignados. Reasigná o quitá esos bloques antes de eliminarla."); }
    }
}
