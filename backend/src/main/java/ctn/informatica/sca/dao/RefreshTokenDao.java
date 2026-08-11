package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenDao extends conexion {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS refresh_token (
              id BIGINT AUTO_INCREMENT PRIMARY KEY,
              token_hash CHAR(64) NOT NULL,
              user_id INT NOT NULL,
              user_level TINYINT NOT NULL,
              expires_at TIMESTAMP NOT NULL,
              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              revoked_at TIMESTAMP NULL,
              replaced_by_hash CHAR(64) NULL,
              user_agent VARCHAR(255) NULL,
              ip_address VARCHAR(64) NULL,
              UNIQUE KEY uq_refresh_token_hash (token_hash),
              KEY idx_refresh_token_user (user_id),
              KEY idx_refresh_token_expires (expires_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
            """;

    /**
     * Keeps the refresh-token schema available even when the legacy SQL
     * migration was not executed by the deployment script.
     */
    public void ensureSchema() throws SQLException {
        try (Connection con = getCon(); Statement statement = con.createStatement()) {
            if (!tableExists(con)) {
                statement.executeUpdate(CREATE_TABLE_SQL);
            }
            ensureColumn(con, statement, "user_level", "ALTER TABLE refresh_token ADD COLUMN user_level TINYINT NOT NULL DEFAULT 1 AFTER user_id");
            ensureColumn(con, statement, "revoked_at", "ALTER TABLE refresh_token ADD COLUMN revoked_at TIMESTAMP NULL AFTER created_at");
            ensureColumn(con, statement, "replaced_by_hash", "ALTER TABLE refresh_token ADD COLUMN replaced_by_hash CHAR(64) NULL AFTER revoked_at");
            ensureColumn(con, statement, "user_agent", "ALTER TABLE refresh_token ADD COLUMN user_agent VARCHAR(255) NULL AFTER replaced_by_hash");
            ensureColumn(con, statement, "ip_address", "ALTER TABLE refresh_token ADD COLUMN ip_address VARCHAR(64) NULL AFTER user_agent");
            ensureIndex(con, statement, "idx_refresh_token_user", "ALTER TABLE refresh_token ADD INDEX idx_refresh_token_user (user_id)");
            ensureIndex(con, statement, "idx_refresh_token_expires", "ALTER TABLE refresh_token ADD INDEX idx_refresh_token_expires (expires_at)");
            ensureIndex(con, statement, "uq_refresh_token_hash", "ALTER TABLE refresh_token ADD UNIQUE INDEX uq_refresh_token_hash (token_hash)");
        }
    }

    public void insert(String tokenHash, int userId, int userLevel, Instant expiresAt, String userAgent, String ipAddress) throws SQLException {
        String sql = "INSERT INTO refresh_token (token_hash, user_id, user_level, expires_at, user_agent, ip_address) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.setInt(2, userId);
            ps.setInt(3, userLevel);
            ps.setTimestamp(4, Timestamp.from(expiresAt));
            ps.setString(5, trimNullable(userAgent, 255));
            ps.setString(6, trimNullable(ipAddress, 64));
            ps.executeUpdate();
        }
    }

    public RefreshTokenRecord findActiveByHash(String tokenHash) throws SQLException {
        String sql = "SELECT token_hash, user_id, user_level, expires_at, revoked_at FROM refresh_token WHERE token_hash = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Timestamp expiresAt = rs.getTimestamp("expires_at");
                Timestamp revokedAt = rs.getTimestamp("revoked_at");
                RefreshTokenRecord record = new RefreshTokenRecord(
                        rs.getString("token_hash"),
                        rs.getInt("user_id"),
                        rs.getInt("user_level"),
                        expiresAt != null ? expiresAt.toInstant() : null,
                        revokedAt != null ? revokedAt.toInstant() : null);

                if (record.expiresAt() == null || record.expiresAt().isBefore(Instant.now())) {
                    return null;
                }
                if (record.revokedAt() != null) {
                    return null;
                }
                return record;
            }
        }
    }

    public void rotate(String oldHash, String newHash, Instant newExpiresAt, String userAgent, String ipAddress) throws SQLException {
        String updateOld = "UPDATE refresh_token SET revoked_at = CURRENT_TIMESTAMP, replaced_by_hash = ? WHERE token_hash = ? AND revoked_at IS NULL";
        String insertNew = "INSERT INTO refresh_token (token_hash, user_id, user_level, expires_at, user_agent, ip_address) "
                + "SELECT ?, user_id, user_level, ?, ?, ? FROM refresh_token WHERE token_hash = ? LIMIT 1";

        try (Connection con = getCon()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                int updated;
                try (PreparedStatement ps = con.prepareStatement(updateOld)) {
                    ps.setString(1, newHash);
                    ps.setString(2, oldHash);
                    updated = ps.executeUpdate();
                }

                if (updated == 0) {
                    throw new SQLException("Refresh token no activo o no encontrado");
                }

                try (PreparedStatement ps = con.prepareStatement(insertNew)) {
                    ps.setString(1, newHash);
                    ps.setTimestamp(2, Timestamp.from(newExpiresAt));
                    ps.setString(3, trimNullable(userAgent, 255));
                    ps.setString(4, trimNullable(ipAddress, 64));
                    ps.setString(5, oldHash);
                    int inserted = ps.executeUpdate();
                    if (inserted == 0) {
                        throw new SQLException("No se pudo insertar refresh token rotado");
                    }
                }

                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public void revoke(String tokenHash) throws SQLException {
        String sql = "UPDATE refresh_token SET revoked_at = CURRENT_TIMESTAMP WHERE token_hash = ? AND revoked_at IS NULL";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.executeUpdate();
        }
    }

    private String trimNullable(String value, int maxLen) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen);
    }

    private void ensureColumn(Connection con, Statement statement, String columnName, String alterSql) throws SQLException {
        DatabaseMetaData metadata = con.getMetaData();
        try (ResultSet columns = metadata.getColumns(con.getCatalog(), null, "refresh_token", null)) {
            while (columns.next()) {
                if (columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return;
                }
            }
        }
        statement.executeUpdate(alterSql);
    }

    private boolean tableExists(Connection con) throws SQLException {
        DatabaseMetaData metadata = con.getMetaData();
        try (ResultSet tables = metadata.getTables(con.getCatalog(), null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                if ("refresh_token".equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void ensureIndex(Connection con, Statement statement, String indexName, String alterSql) throws SQLException {
        DatabaseMetaData metadata = con.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(con.getCatalog(), null, "refresh_token", false, false)) {
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return;
                }
            }
        }
        statement.executeUpdate(alterSql);
    }

    public record RefreshTokenRecord(String tokenHash, int userId, int userLevel, Instant expiresAt, Instant revokedAt) {
    }
}
