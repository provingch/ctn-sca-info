package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import org.springframework.stereotype.Repository;

@Repository
public class ProfesorDao extends conexion {

    // ── helper para mapear un ResultSet a Profesor ──────────────────────────
    private Profesor map(ResultSet rs) throws SQLException {
        Profesor p = new Profesor();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setApellido(rs.getString("apellido"));
        p.setUsuario(rs.getString("usuario"));
        p.setContrasenia(rs.getString("contrasenia"));
        p.setNivel(rs.getInt("nivel"));

        int ci = rs.getInt("ci");
        if (!rs.wasNull()) p.setCi(ci);

        int tel = rs.getInt("telefono");
        if (!rs.wasNull()) p.setTelefono(tel);

        int cel = rs.getInt("celular");
        if (!rs.wasNull()) p.setCelular(cel);

        p.setCorreo(rs.getString("correo"));
        int especialidadId = rs.getInt("especialidad_id");
        if (!rs.wasNull()) p.setEspecialidadId(especialidadId);
        p.setGoogleEmail(rs.getString("google_email"));
        p.setGcAccessToken(rs.getString("google_access_token"));
        p.setGcRefreshToken(rs.getString("google_refresh_token"));
        long expiry = rs.getLong("google_token_expiry");
        if (!rs.wasNull()) p.setGcTokenExpiry(expiry);
        p.setTotpSecret(rs.getString("totp_secret"));
        return p;
    }

    // ── findById ─────────────────────────────────────────────────────────────
    public Profesor findById(int id) {
        final String sql = "SELECT id, nombre, apellido, usuario, contrasenia, nivel, "
                         + "ci, telefono, celular, correo, especialidad_id, google_email, "
                         + "google_access_token, google_refresh_token, google_token_expiry, totp_secret "
                         + "FROM profesor WHERE id = ?";
        try (Connection c = getCon();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // ── findByGoogleEmail ─────────────────────────────────────────────────────
    // Busca al profesor cuyo correo coincide con el email de Google.
    // Ajusta el nombre de columna si en tu tabla se llama distinto.
    public Profesor findByGoogleEmail(String email) {
        final String sql = "SELECT id, nombre, apellido, usuario, contrasenia, nivel, "
                         + "ci, telefono, celular, correo, especialidad_id, google_email, "
                         + "google_access_token, google_refresh_token, google_token_expiry, totp_secret "
                         + "FROM profesor WHERE google_email = ? OR correo = ?";
        try (Connection c = getCon();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // ── updateGoogleTokens ────────────────────────────────────────────────────
    // Guarda los tokens OAuth en la BD.
    // Requiere que la tabla tenga columnas: google_email, google_access_token, google_refresh_token, google_token_expiry
    public boolean updateGoogleTokens(int profesorId,
                                      String accessToken,
                                      String refreshToken,
                                      long expiryEpochSeconds,
                                      String googleEmail) {
        final String sql = "UPDATE profesor "
                         + "SET google_email = ?, "
                         + "    google_access_token = ?, "
                         + "    google_refresh_token = ?, "
                         + "    google_token_expiry  = ? "
                         + "WHERE id = ?";
        try (Connection c = getCon();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (googleEmail != null) {
                ps.setString(1, googleEmail);
            } else {
                ps.setNull(1, Types.VARCHAR);
            }
            ps.setString(2, accessToken);
            if (refreshToken != null) {
                ps.setString(3, refreshToken);
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setLong(4, expiryEpochSeconds);
            ps.setInt(5, profesorId);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public String findManualSubjectsText(int profesorId) {
        final String sql = "SELECT materias_manual FROM profesor WHERE id = ?";
        try (Connection c = getCon();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString("materias_manual");
                    return value == null ? "" : value;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public boolean updateManualSubjectsText(int profesorId, String materiasManual) {
        final String sql = "UPDATE profesor SET materias_manual = ? WHERE id = ?";
        try (Connection c = getCon();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (materiasManual != null && !materiasManual.trim().isEmpty()) {
                ps.setString(1, materiasManual.trim());
            } else {
                ps.setNull(1, Types.VARCHAR);
            }
            ps.setInt(2, profesorId);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // ── update ────────────────────────────────────────────────────────────────
    public boolean update(Profesor p) {
        final String sql = "UPDATE profesor "
                         + "SET nombre = ?, apellido = ?, usuario = ?, "
                         + "    contrasenia = ?, nivel = ?, ci = ?, telefono = ?, "
                         + "    celular = ?, correo = ?, especialidad_id = ? "
                         + "WHERE id = ?";
        try (Connection c = getCon();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getApellido());
            ps.setString(3, p.getUsuario());
            String password = p.getContrasenia();
            if (password == null || password.trim().isEmpty()) {
                password = "password";
            }
            ps.setString(4, PasswordUtil.hash(password));
            ps.setInt(5, p.getNivel());

            if (p.getCi() != null)       ps.setInt(6, p.getCi());
            else                          ps.setNull(6, Types.INTEGER);

            if (p.getTelefono() != null)  ps.setInt(7, p.getTelefono());
            else                          ps.setNull(7, Types.INTEGER);

            if (p.getCelular() != null)   ps.setInt(8, p.getCelular());
            else                          ps.setNull(8, Types.INTEGER);

            ps.setString(9, p.getCorreo());
            if (p.getEspecialidadId() != null) ps.setInt(10, p.getEspecialidadId());
            else                                ps.setNull(10, Types.INTEGER);
            ps.setInt(11, p.getId());

            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public java.util.List<Profesor> findAll() {
        final String sql = "SELECT id, nombre, apellido, usuario, contrasenia, nivel, "
                         + "ci, telefono, celular, correo, especialidad_id, google_email, "
                         + "google_access_token, google_refresh_token, google_token_expiry, totp_secret "
                         + "FROM profesor ORDER BY apellido, nombre";
        java.util.List<Profesor> out = new java.util.ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(map(rs));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return out;
    }

    public boolean updateTotpSecret(int profesorId, String totpSecret) {
        final String sql = "UPDATE profesor SET totp_secret = ? WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (totpSecret != null && !totpSecret.isBlank()) {
                ps.setString(1, totpSecret);
            } else {
                ps.setNull(1, Types.VARCHAR);
            }
            ps.setInt(2, profesorId);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean existsByUsuario(String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) return false;
        final String sql = "SELECT 1 FROM profesor WHERE usuario = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean existsByUsuario(String usuario, int excludeId) {
        if (usuario == null || usuario.trim().isEmpty()) return false;
        final String sql = "SELECT 1 FROM profesor WHERE usuario = ? AND id != ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario.trim());
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public int create(Profesor p) {
        final String sql = "INSERT INTO profesor (nombre, apellido, usuario, contrasenia, nivel, ci, telefono, celular, correo, especialidad_id) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getApellido());
            ps.setString(3, p.getUsuario());
            String password = p.getContrasenia();
            if (password == null || password.trim().isEmpty()) {
                password = "password";
            }
            ps.setString(4, PasswordUtil.hash(password));
            ps.setInt(5, p.getNivel());

            if (p.getCi() != null) ps.setInt(6, p.getCi()); else ps.setNull(6, Types.INTEGER);
            if (p.getTelefono() != null) ps.setInt(7, p.getTelefono()); else ps.setNull(7, Types.INTEGER);
            if (p.getCelular() != null) ps.setInt(8, p.getCelular()); else ps.setNull(8, Types.INTEGER);
            ps.setString(9, p.getCorreo());
            if (p.getEspecialidadId() != null) ps.setInt(10, p.getEspecialidadId()); else ps.setNull(10, Types.INTEGER);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    public boolean updateNivel(int id, int nivel) {
        final String sql = "UPDATE profesor SET nivel = ? WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, nivel);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean resetPassword(int id, String newPasswordPlainText) {
        final String sql = "UPDATE profesor SET contrasenia = ? WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            String plain = (newPasswordPlainText == null || newPasswordPlainText.trim().isEmpty())
                ? "password" : newPasswordPlainText;
            ps.setString(1, PasswordUtil.hash(plain));
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public int countByNivel(int nivel) {
        final String sql = "SELECT COUNT(*) AS cnt FROM profesor WHERE nivel = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, nivel);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    public boolean delete(int id) {
        final String sql = "DELETE FROM profesor WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
