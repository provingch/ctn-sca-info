package ctn.informatica.sca.config;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Migra de forma incremental el esquema anterior (profesor/padre) al esquema
 * consolidado en usuario. Se ejecuta una sola vez de forma efectiva: conserva
 * las tablas antiguas y hace que las operaciones sean idempotentes para no
 * poner en riesgo los datos de una base ya actualizada.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyUserRoleMigrationInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyUserRoleMigrationInitializer.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = new conexion().getCon()) {
            if (!tableExists(connection, "profesor") && !tableExists(connection, "padre")) {
                return;
            }

            ensureUsuarioTable(connection);
            validateNoAmbiguousUsers(connection);

            int profesores = migrateProfesores(connection);
            int padres = migratePadres(connection);
            ensureParentMapTable(connection);
            mapPadresToUsuarios(connection);

            ensureUsuarioMateriaTable(connection);
            int materias = migrateProfesorMaterias(connection);
            ensureUsuarioReferenceColumn(connection, "asignacion");
            ensureUsuarioReferenceColumn(connection, "planilla");
            ensureUsuarioReferenceColumn(connection, "planilla_rasgo");

            ensureAlumnoUsuarioTable(connection);
            int relaciones = migrateAlumnoPadre(connection);
            migrateParentRefreshTokens(connection);
            migrateParentPushSubscriptions(connection);

            log.info("Migración incremental de usuarios completada: {} profesores, {} padres, {} relaciones de materias y {} vínculos alumno-responsable.",
                    profesores, padres, materias, relaciones);
        }
    }

    private void ensureUsuarioTable(Connection connection) throws SQLException {
        if (tableExists(connection, "usuario")) {
            String[] required = {
                "id", "nombre", "apellido", "usuario", "contrasenia", "ci", "telefono", "celular", "correo",
                "google_email", "google_access_token", "google_refresh_token", "google_token_expiry", "materias_manual",
                "totp_secret", "firma_imagen", "foto_perfil", "especialidad_id", "nivel", "rol"
            };
            for (String column : required) {
                if (!columnExists(connection, "usuario", column)) {
                    throw new SQLException("La tabla usuario existe pero no contiene la columna requerida '" + column
                            + "'. La migración se detuvo sin modificar datos.");
                }
            }
            return;
        }

        execute(connection, """
                CREATE TABLE usuario (
                  id INT AUTO_INCREMENT PRIMARY KEY,
                  nombre VARCHAR(45) DEFAULT NULL,
                  apellido VARCHAR(45) DEFAULT NULL,
                  usuario VARCHAR(45) NOT NULL UNIQUE,
                  contrasenia VARCHAR(255) NOT NULL,
                  ci INT DEFAULT NULL,
                  telefono VARCHAR(45) DEFAULT NULL,
                  celular VARCHAR(45) DEFAULT NULL,
                  correo VARCHAR(255) DEFAULT NULL,
                  google_email VARCHAR(255) DEFAULT NULL,
                  google_access_token TEXT NULL,
                  google_refresh_token TEXT NULL,
                  google_token_expiry BIGINT NULL,
                  materias_manual TEXT NULL,
                  totp_secret VARCHAR(255) NULL,
                  firma_imagen LONGTEXT NULL,
                  foto_perfil LONGTEXT NULL,
                  especialidad_id INT NULL,
                  nivel TINYINT NOT NULL DEFAULT 1,
                  rol VARCHAR(30) NOT NULL DEFAULT 'profesor',
                  CONSTRAINT fk_usuario_especialidad FOREIGN KEY (especialidad_id)
                    REFERENCES especialidad (id) ON UPDATE CASCADE ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """);
    }

    private void validateNoAmbiguousUsers(Connection connection) throws SQLException {
        if (tableExists(connection, "profesor")) {
            assertNoRows(connection, """
                    SELECT p.id, p.usuario
                    FROM profesor p
                    JOIN usuario u ON u.id = p.id AND u.usuario <> p.usuario
                    LIMIT 1
                    """, "Hay un id de profesor que ya pertenece a otro usuario");

            assertNoRows(connection, """
                    SELECT p.id, p.usuario
                    FROM profesor p
                    JOIN usuario u ON u.usuario = p.usuario AND u.id <> p.id
                    LIMIT 1
                    """, "Hay un usuario de profesor que colisiona con un usuario ya migrado");
        }

        if (tableExists(connection, "padre")) {
            assertNoRows(connection, """
                    SELECT p.id, p.usuario
                    FROM padre p
                    JOIN usuario u ON u.usuario = p.usuario AND u.rol <> 'padre'
                    LIMIT 1
                    """, "Hay un usuario de padre que colisiona con otro rol");
        }
    }

    private int migrateProfesores(Connection connection) throws SQLException {
        if (!tableExists(connection, "profesor")) {
            return 0;
        }
        return execute(connection, """
                INSERT INTO usuario (
                  id, nombre, apellido, usuario, contrasenia, ci, telefono, celular, correo,
                  google_email, google_access_token, google_refresh_token, google_token_expiry,
                  materias_manual, totp_secret, firma_imagen, foto_perfil, especialidad_id, nivel, rol
                )
                SELECT p.id, p.nombre, p.apellido, p.usuario, p.contrasenia, p.ci, p.telefono, p.celular, p.correo,
                       p.google_email, p.google_access_token, p.google_refresh_token, p.google_token_expiry,
                       p.materias_manual, p.totp_secret, p.firma_imagen, p.foto_perfil, p.especialidad_id, p.nivel,
                       CASE p.nivel WHEN 3 THEN 'admin' WHEN 2 THEN 'evaluador' ELSE 'profesor' END
                FROM profesor p
                LEFT JOIN usuario u ON u.id = p.id
                WHERE u.id IS NULL
                """);
    }

    private int migratePadres(Connection connection) throws SQLException {
        if (!tableExists(connection, "padre")) {
            return 0;
        }
        return execute(connection, """
                INSERT INTO usuario (ci, nombre, apellido, usuario, contrasenia, telefono, correo, totp_secret, nivel, rol)
                SELECT p.ci, p.nombre, p.apellido, p.usuario, p.contrasenia, p.telefono, p.correo, p.totp_secret, 4, 'padre'
                FROM padre p
                LEFT JOIN usuario u ON u.usuario = p.usuario AND u.rol = 'padre'
                WHERE u.id IS NULL
                """);
    }

    private void ensureParentMapTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS sca_legacy_parent_user_map (
                  legacy_parent_id INT NOT NULL PRIMARY KEY,
                  usuario_id INT NOT NULL,
                  migrated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  KEY idx_sca_legacy_parent_user_map_usuario (usuario_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """);
    }

    private void mapPadresToUsuarios(Connection connection) throws SQLException {
        if (!tableExists(connection, "padre")) {
            return;
        }
        execute(connection, """
                INSERT INTO sca_legacy_parent_user_map (legacy_parent_id, usuario_id)
                SELECT p.id, u.id
                FROM padre p
                JOIN usuario u ON u.usuario = p.usuario AND u.rol = 'padre'
                ON DUPLICATE KEY UPDATE usuario_id = VALUES(usuario_id)
                """);
    }

    private void ensureUsuarioMateriaTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS usuario_materia (
                  usuario_id INT NOT NULL,
                  materia_id INT NOT NULL,
                  PRIMARY KEY (usuario_id, materia_id),
                  CONSTRAINT fk_um_usuario FOREIGN KEY (usuario_id)
                    REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE CASCADE,
                  CONSTRAINT fk_um_materia FOREIGN KEY (materia_id)
                    REFERENCES materia (id) ON UPDATE CASCADE ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """);
    }

    private int migrateProfesorMaterias(Connection connection) throws SQLException {
        if (!tableExists(connection, "profesor_materia")) {
            return 0;
        }
        return execute(connection, """
                INSERT IGNORE INTO usuario_materia (usuario_id, materia_id)
                SELECT pm.profesor_id, pm.materia_id
                FROM profesor_materia pm
                JOIN usuario u ON u.id = pm.profesor_id AND u.rol <> 'padre'
                """);
    }

    private void ensureUsuarioReferenceColumn(Connection connection, String table) throws SQLException {
        if (!tableExists(connection, table) || columnExists(connection, table, "usuario_id")) {
            return;
        }
        if (!columnExists(connection, table, "profesor_id")) {
            throw new SQLException("La tabla " + table + " no contiene ni usuario_id ni profesor_id.");
        }

        execute(connection, "ALTER TABLE " + table + " ADD COLUMN usuario_id INT NULL");
        execute(connection, "UPDATE " + table + " SET usuario_id = profesor_id WHERE usuario_id IS NULL");

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + table + " t LEFT JOIN usuario u ON u.id = t.usuario_id WHERE u.id IS NULL")) {
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                if (result.getInt(1) > 0) {
                    throw new SQLException("No se pudo relacionar todos los registros de " + table
                            + " con usuario. La migración se detuvo sin eliminar información.");
                }
            }
        }
        execute(connection, "ALTER TABLE " + table + " MODIFY usuario_id INT NOT NULL");
    }

    private void ensureAlumnoUsuarioTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS alumno_usuario (
                  alumno_id INT NOT NULL,
                  usuario_id INT NOT NULL,
                  parentesco VARCHAR(45) DEFAULT 'padre',
                  PRIMARY KEY (alumno_id, usuario_id),
                  CONSTRAINT fk_au_alumno FOREIGN KEY (alumno_id)
                    REFERENCES alumno (id) ON UPDATE CASCADE ON DELETE CASCADE,
                  CONSTRAINT fk_au_usuario FOREIGN KEY (usuario_id)
                    REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
                """);
    }

    private int migrateAlumnoPadre(Connection connection) throws SQLException {
        if (!tableExists(connection, "alumno_padre")) {
            return 0;
        }
        return execute(connection, """
                INSERT IGNORE INTO alumno_usuario (alumno_id, usuario_id, parentesco)
                SELECT ap.alumno_id, map.usuario_id, ap.parentesco
                FROM alumno_padre ap
                JOIN sca_legacy_parent_user_map map ON map.legacy_parent_id = ap.padre_id
                """);
    }

    private void migrateParentRefreshTokens(Connection connection) throws SQLException {
        if (!tableExists(connection, "refresh_token")) {
            return;
        }
        if (!columnExists(connection, "refresh_token", "user_level")) {
            execute(connection, "ALTER TABLE refresh_token ADD COLUMN user_level TINYINT NOT NULL DEFAULT 1 AFTER user_id");
        }
        execute(connection, """
                UPDATE refresh_token rt
                JOIN sca_legacy_parent_user_map map ON map.legacy_parent_id = rt.user_id
                SET rt.user_id = map.usuario_id
                WHERE rt.user_level = 4 AND rt.user_id <> map.usuario_id
                """);
    }

    private void migrateParentPushSubscriptions(Connection connection) throws SQLException {
        if (!tableExists(connection, "push_subscription")) {
            return;
        }
        execute(connection, """
                UPDATE push_subscription ps
                JOIN sca_legacy_parent_user_map map ON map.legacy_parent_id = ps.user_id
                SET ps.user_id = map.usuario_id
                WHERE LOWER(COALESCE(ps.user_type, '')) IN ('padre', 'parent')
                  AND ps.user_id <> map.usuario_id
                """);
    }

    private void assertNoRows(Connection connection, String sql, String reason) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            if (result.next()) {
                throw new SQLException(reason + " (id " + result.getInt(1) + ", usuario " + result.getString(2)
                        + "). Corrija esa colisión antes de reintentar; no se modificaron datos.");
            }
        }
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, null, new String[]{"TABLE"})) {
            while (tables.next()) {
                if (table.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, table, null)) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private int execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }
}
