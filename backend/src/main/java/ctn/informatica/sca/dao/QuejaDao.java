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

    public int crear(int profesorId, int cursoId, int especialidadId, String motivo, int creadaPor) throws SQLException {
        String sql = "INSERT INTO queja (profesor_id, curso_id, especialidad_id, motivo, creada_por) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, profesorId);
            ps.setInt(2, cursoId);
            ps.setInt(3, especialidadId);
            ps.setString(4, motivo == null ? "" : motivo.trim());
            ps.setInt(5, creadaPor);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return 0;
    }

    public long contarPorProfesor(int profesorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM queja WHERE profesor_id = ?";
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

    public List<Map<String, Object>> listar() throws SQLException {
        String sql = "SELECT q.id, q.profesor_id, q.curso_id, q.especialidad_id, q.motivo, q.creada_por, q.creada_en, "
                + "u.nombre AS profesor_nombre, u.apellido AS profesor_apellido, c.especialidad AS curso_especialidad, c.seccion AS curso_seccion, c.nivel AS curso_nivel "
                + "FROM queja q "
                + "LEFT JOIN usuario u ON u.id = q.profesor_id "
                + "LEFT JOIN curso c ON c.id = q.curso_id "
                + "ORDER BY q.creada_en DESC";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("profesorId", rs.getLong("profesor_id"));
                row.put("cursoId", rs.getLong("curso_id"));
                row.put("especialidadId", rs.getInt("especialidad_id"));
                row.put("motivo", rs.getString("motivo"));
                row.put("creadaPor", rs.getLong("creada_por"));
                row.put("creadaEn", rs.getTimestamp("creada_en"));
                row.put("profesorNombre", rs.getString("profesor_nombre"));
                row.put("profesorApellido", rs.getString("profesor_apellido"));
                row.put("cursoEspecialidad", rs.getString("curso_especialidad"));
                row.put("cursoSeccion", rs.getString("curso_seccion"));
                row.put("cursoNivel", rs.getInt("curso_nivel"));
                items.add(row);
            }
        }
        return items;
    }
}
