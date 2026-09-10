package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.util.TextEncodingRepair;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TextEncodingMigrationDao extends conexion {
    static final String VERSION = "V017__repair_display_text_utf8";
    // Only human-readable text. Never credentials, usernames, addresses, files or JSON.
    private static final List<String> COLUMNS = List.of(
            "especialidad.nombre", "materia.nombre", "usuario.nombre", "usuario.apellido",
            "alumno.nombre", "alumno.apellido", "sala.nombre", "instrumento.nombre",
            "hora_catedra.etiqueta", "tarea.titulo", "planilla_rasgo.tema",
            "rasgo_asistencia.alumno_nombre", "rasgo_asistencia.alumno_apellido",
            "rasgo_asistencia.falta_observacion", "plan_curricular.observaciones_evaluador",
            "tema_plan_curricular.mes", "tema_plan_curricular.capacidades",
            "tema_plan_curricular.temas_contenidos", "tema_plan_curricular.actividades",
            "tema_plan_curricular.instrumentos_evaluacion", "tema_plan_curricular.indicador_conceptual",
            "tema_plan_curricular.indicador_procedimental", "tema_plan_curricular.indicador_actitudinal",
            "incumplimiento_revision.descripcion", "notificacion.titulo", "notificacion.cuerpo", "queja.motivo");

    public int repairOnce() throws SQLException {
        try (Connection connection = getCon()) {
            try (PreparedStatement check = connection.prepareStatement("SELECT 1 FROM schema_migrations WHERE version = ?")) {
                check.setString(1, VERSION);
                try (ResultSet result = check.executeQuery()) { if (result.next()) return 0; }
            }
            // DDL before the transaction: MySQL implicitly commits DDL. Original text is
            // kept byte-for-byte in the database, not logged or exposed through the API.
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS text_encoding_repair_backup ("
                        + "table_name VARCHAR(64) NOT NULL, column_name VARCHAR(64) NOT NULL, row_id BIGINT NOT NULL, "
                        + "original_utf8 LONGBLOB NOT NULL, repaired_utf8 LONGBLOB NOT NULL, "
                        + "repaired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "PRIMARY KEY (table_name, column_name, row_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            }
            connection.setAutoCommit(false);
            try {
                int changed = 0;
                for (String field : COLUMNS) {
                    String[] parts = field.split("\\.");
                    if (hasColumn(connection, parts[0], parts[1])) changed += repairColumn(connection, parts[0], parts[1]);
                }
                try (PreparedStatement record = connection.prepareStatement("INSERT INTO schema_migrations (version) VALUES (?)")) {
                    record.setString(1, VERSION);
                    record.executeUpdate();
                }
                connection.commit();
                return changed;
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        try (PreparedStatement check = connection.prepareStatement("SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?")) {
            check.setString(1, table);
            check.setString(2, column);
            try (ResultSet result = check.executeQuery()) { return result.next(); }
        }
    }

    private int repairColumn(Connection connection, String table, String column) throws SQLException {
        // Identifiers come exclusively from the fixed allowlist above.
        try (PreparedStatement select = connection.prepareStatement("SELECT id, `" + column + "` FROM `" + table + "` WHERE `" + column + "` IS NOT NULL FOR UPDATE");
             PreparedStatement backup = connection.prepareStatement("INSERT INTO text_encoding_repair_backup (table_name, column_name, row_id, original_utf8, repaired_utf8) VALUES (?, ?, ?, ?, ?)");
             PreparedStatement update = connection.prepareStatement("UPDATE `" + table + "` SET `" + column + "` = ? WHERE id = ?");
             ResultSet rows = select.executeQuery()) {
            int changed = 0;
            while (rows.next()) {
                String original = rows.getString(2);
                String repaired = TextEncodingRepair.repair(original);
                if (original.equals(repaired)) continue;
                long id = rows.getLong(1);
                backup.setString(1, table);
                backup.setString(2, column);
                backup.setLong(3, id);
                backup.setBytes(4, original.getBytes(StandardCharsets.UTF_8));
                backup.setBytes(5, repaired.getBytes(StandardCharsets.UTF_8));
                backup.executeUpdate();
                update.setString(1, repaired);
                update.setLong(2, id);
                changed += update.executeUpdate();
            }
            return changed;
        }
    }
}
