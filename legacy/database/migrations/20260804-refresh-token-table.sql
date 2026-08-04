-- Migracion segura para remember-me con refresh tokens persistidos.
-- Idempotente: se puede ejecutar varias veces sin romper el esquema.

SET @table_schema = DATABASE();

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- user_level para distinguir profesor/padre cuando comparten el mismo id numerico.
SET @table_name = 'refresh_token';
SET @column_name = 'user_level';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE refresh_token ADD COLUMN user_level TINYINT NOT NULL DEFAULT 1 AFTER user_id',
  'SELECT "user_level ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_name = 'revoked_at';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE refresh_token ADD COLUMN revoked_at TIMESTAMP NULL AFTER created_at',
  'SELECT "revoked_at ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_name = 'replaced_by_hash';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE refresh_token ADD COLUMN replaced_by_hash CHAR(64) NULL AFTER revoked_at',
  'SELECT "replaced_by_hash ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_name = 'user_agent';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE refresh_token ADD COLUMN user_agent VARCHAR(255) NULL AFTER replaced_by_hash',
  'SELECT "user_agent ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_name = 'ip_address';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE refresh_token ADD COLUMN ip_address VARCHAR(64) NULL AFTER user_agent',
  'SELECT "ip_address ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_name = 'idx_refresh_token_user';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND INDEX_NAME = @idx_name) = 0,
  'ALTER TABLE refresh_token ADD INDEX idx_refresh_token_user (user_id)',
  'SELECT "idx_refresh_token_user ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_name = 'idx_refresh_token_expires';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND INDEX_NAME = @idx_name) = 0,
  'ALTER TABLE refresh_token ADD INDEX idx_refresh_token_expires (expires_at)',
  'SELECT "idx_refresh_token_expires ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_name = 'uq_refresh_token_hash';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND INDEX_NAME = @idx_name) = 0,
  'ALTER TABLE refresh_token ADD UNIQUE INDEX uq_refresh_token_hash (token_hash)',
  'SELECT "uq_refresh_token_hash ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migracion de refresh_token completada.' AS resultado;
