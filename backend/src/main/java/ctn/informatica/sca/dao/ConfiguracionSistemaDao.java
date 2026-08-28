package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Repository;

@Repository
public class ConfiguracionSistemaDao extends conexion {

    public String get(String clave) throws SQLException {
        if (clave == null || clave.isBlank()) {
            return null;
        }
        String sql = "SELECT valor FROM configuracion_sistema WHERE clave = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("valor");
                }
            }
        }
        return null;
    }

    public Integer getInt(String clave, int defaultValue) throws SQLException {
        String valor = get(clave);
        if (valor == null || valor.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
