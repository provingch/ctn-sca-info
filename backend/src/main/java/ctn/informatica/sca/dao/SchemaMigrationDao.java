package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.stereotype.Repository;

@Repository
public class SchemaMigrationDao extends conexion {

    public void ensureSchema() throws SQLException {
        try (Connection con = getCon(); Statement statement = con.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS schema_migrations (version VARCHAR(255) PRIMARY KEY, applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
        }
    }

    public boolean isApplied(String version) throws SQLException {
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement("SELECT 1 FROM schema_migrations WHERE version = ?")) {
            ps.setString(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void renameVersion(String oldVersion, String newVersion) throws SQLException {
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(
                "UPDATE schema_migrations SET version = ? WHERE version = ? "
                        + "AND NOT EXISTS (SELECT 1 FROM schema_migrations WHERE version = ?)")) {
            ps.setString(1, newVersion);
            ps.setString(2, oldVersion);
            ps.setString(3, newVersion);
            ps.executeUpdate();
        }
    }

    public void executeAndRecord(String version, String sql) throws SQLException {
        try (Connection con = getCon()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                try (Statement statement = con.createStatement()) {
                    String sqlNoComments = sql.replaceAll("(?m)^\\s*--.*$", "");
                    for (String command : sqlNoComments.split(";")) {
                        String trimmed = command.trim();
                        if (!trimmed.isEmpty()) statement.execute(trimmed);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO schema_migrations (version) VALUES (?)")) {
                    ps.setString(1, version);
                    ps.executeUpdate();
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
}