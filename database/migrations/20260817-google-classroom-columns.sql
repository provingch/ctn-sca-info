-- Migración: Añadir columnas necesarias para integración con Google Classroom
-- Fecha: 2026-08-17
-- Idempotente: puede ejecutarse varias veces sin causar errores

SET @table_schema = DATABASE();

-- Profesor: columnas para OAuth y metadata
SET @table_name = 'profesor';

SET @column_name = 'google_email';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE profesor ADD COLUMN google_email VARCHAR(255) NULL AFTER correo',
  'SELECT "google_email ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_name = 'google_access_token';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE profesor ADD COLUMN google_access_token TEXT NULL AFTER google_email',
  'SELECT "google_access_token ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_name = 'google_refresh_token';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE profesor ADD COLUMN google_refresh_token TEXT NULL AFTER google_access_token',
  'SELECT "google_refresh_token ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_name = 'google_token_expiry';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE profesor ADD COLUMN google_token_expiry BIGINT NULL AFTER google_refresh_token',
  'SELECT "google_token_expiry ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_name = 'materias_manual';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE profesor ADD COLUMN materias_manual TEXT NULL AFTER google_token_expiry',
  'SELECT "materias_manual ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_name = 'totp_secret';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE profesor ADD COLUMN totp_secret VARCHAR(255) NULL AFTER materias_manual',
  'SELECT "totp_secret ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_name = 'firma_imagen';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE profesor ADD COLUMN firma_imagen TEXT NULL AFTER totp_secret',
  'SELECT "firma_imagen ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Alumno: columnas para identidad Google
SET @table_name = 'alumno';

SET @column_name = 'google_user_id';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE alumno ADD COLUMN google_user_id VARCHAR(255) NULL AFTER correo_encargado2',
  'SELECT "google_user_id ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_name = 'google_email';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE alumno ADD COLUMN google_email VARCHAR(255) NULL AFTER google_user_id',
  'SELECT "alumno.google_email ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Planilla: columna para enlazar course de Google
SET @table_name = 'planilla';
SET @column_name = 'google_course_id';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE planilla ADD COLUMN google_course_id VARCHAR(255) NULL AFTER profesor_id',
  'SELECT "planilla.google_course_id ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Tarea: columnas para coursework de Google
SET @table_name = 'tarea';

SET @column_name = 'google_coursework_id';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE tarea ADD COLUMN google_coursework_id VARCHAR(255) NULL AFTER titulo',
  'SELECT "tarea.google_coursework_id ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_name = 'google_coursework_url';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE tarea ADD COLUMN google_coursework_url VARCHAR(500) NULL AFTER google_coursework_id',
  'SELECT "tarea.google_coursework_url ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'Migración google-classroom-columns ejecutada.' AS resultado;
