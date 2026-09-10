package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.CodigoConducta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CodigoConductaDao extends conexion {

    public List<CodigoConducta> listarActivos() throws SQLException {
        String sql = "SELECT id, codigo, descripcion, activo FROM codigo_conducta WHERE activo = TRUE ORDER BY codigo";
        List<CodigoConducta> result = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(fromResultSet(rs));
        }
        return result;
    }

    public CodigoConducta crear(String codigo, String descripcion) throws SQLException {
        String normalizedCode = codigo == null ? "" : codigo.trim().toUpperCase();
        String normalizedDescription = descripcion == null ? "" : descripcion.trim();
        if (!normalizedCode.matches("N[A-Z0-9]{0,9}")) {
            throw new IllegalArgumentException("El código debe comenzar con N y tener hasta 10 caracteres.");
        }
        if (normalizedDescription.isBlank() || normalizedDescription.length() > 255) {
            throw new IllegalArgumentException("La descripción es requerida y no puede superar 255 caracteres.");
        }

        try (Connection con = getCon()) {
            try (PreparedStatement existing = con.prepareStatement("SELECT id FROM codigo_conducta WHERE codigo = ?")) {
                existing.setString(1, normalizedCode);
                try (ResultSet rs = existing.executeQuery()) {
                    if (rs.next()) throw new IllegalArgumentException("El código ya existe.");
                }
            }
            String sql = "INSERT INTO codigo_conducta (codigo, descripcion, activo) VALUES (?, ?, TRUE)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, normalizedCode);
                ps.setString(2, normalizedDescription);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se pudo generar el código de conducta.");
                    return new CodigoConducta(keys.getInt(1), normalizedCode, normalizedDescription, true);
                }
            }
        }
    }

    public boolean desactivar(int id) throws SQLException {
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement("UPDATE codigo_conducta SET activo = FALSE WHERE id = ? AND activo = TRUE")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private CodigoConducta fromResultSet(ResultSet rs) throws SQLException {
        return new CodigoConducta(rs.getInt("id"), rs.getString("codigo"), rs.getString("descripcion"), rs.getBoolean("activo"));
    }
}
