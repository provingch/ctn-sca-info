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

    public List<CodigoConducta> listar(boolean incluirInactivos) throws SQLException {
        String sql = "SELECT id, codigo, descripcion, activo FROM codigo_conducta" + (incluirInactivos ? "" : " WHERE activo = TRUE") + " ORDER BY codigo";
        List<CodigoConducta> result = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(fromResultSet(rs));
        }
        return result;
    }

    public CodigoConducta crear(String codigo, String descripcion) throws SQLException {
        String normalizedCode = normalizarCodigo(codigo);
        String normalizedDescription = normalizarDescripcion(descripcion);

        try (Connection con = getCon()) {
            try (PreparedStatement existing = con.prepareStatement("SELECT id, activo FROM codigo_conducta WHERE codigo = ?")) {
                existing.setString(1, normalizedCode);
                try (ResultSet rs = existing.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getBoolean("activo")) throw new IllegalArgumentException("El código ya existe.");
                        // Desactivado = "eliminado" en la UI; no se puede borrar porque el historial lo referencia, se reactiva
                        int id = rs.getInt("id");
                        try (PreparedStatement ps = con.prepareStatement("UPDATE codigo_conducta SET descripcion = ?, activo = TRUE WHERE id = ?")) {
                            ps.setString(1, normalizedDescription);
                            ps.setInt(2, id);
                            ps.executeUpdate();
                        }
                        return new CodigoConducta(id, normalizedCode, normalizedDescription, true);
                    }
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

    /** Cambiar el código se propaga al historial por el FK ON UPDATE CASCADE de rasgo_asistencia_codigo. */
    public boolean editar(int id, String codigo, String descripcion) throws SQLException {
        String normalizedCode = normalizarCodigo(codigo);
        String normalizedDescription = normalizarDescripcion(descripcion);
        try (Connection con = getCon()) {
            try (PreparedStatement existing = con.prepareStatement("SELECT id FROM codigo_conducta WHERE codigo = ? AND id <> ?")) {
                existing.setString(1, normalizedCode);
                existing.setInt(2, id);
                try (ResultSet rs = existing.executeQuery()) {
                    if (rs.next()) throw new IllegalArgumentException("El código ya existe.");
                }
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE codigo_conducta SET codigo = ?, descripcion = ? WHERE id = ? AND activo = TRUE")) {
                ps.setString(1, normalizedCode);
                ps.setString(2, normalizedDescription);
                ps.setInt(3, id);
                return ps.executeUpdate() == 1;
            }
        }
    }

    private static String normalizarCodigo(String codigo) {
        String normalized = codigo == null ? "" : codigo.trim().toUpperCase();
        if (!normalized.matches("N[A-Z0-9]{0,9}")) {
            throw new IllegalArgumentException("El código debe comenzar con N y tener hasta 10 caracteres.");
        }
        return normalized;
    }

    private static String normalizarDescripcion(String descripcion) {
        String normalized = descripcion == null ? "" : descripcion.trim();
        if (normalized.isBlank() || normalized.length() > 255) {
            throw new IllegalArgumentException("La descripción es requerida y no puede superar 255 caracteres.");
        }
        return normalized;
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
