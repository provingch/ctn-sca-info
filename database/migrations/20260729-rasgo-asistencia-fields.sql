-- Migración segura para rasgo_asistencia
-- No elimina datos existentes ni recrea tablas.
-- Sirve para aplicar los nuevos campos de código/observación sobre la BD actual.

USE ctndb;

-- Añadir falta_codigo si aún no existe
SET @table_schema = DATABASE();
SET @table_name = 'rasgo_asistencia';
SET @column_name = 'falta_codigo';

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE rasgo_asistencia ADD COLUMN falta_codigo VARCHAR(4) NULL AFTER estado',
  'SELECT "falta_codigo ya existe; se omite" AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Añadir falta_observacion si aún no existe
SET @column_name = 'falta_observacion';

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE rasgo_asistencia ADD COLUMN falta_observacion VARCHAR(500) NULL AFTER falta_codigo',
  'SELECT "falta_observacion ya existe; se omite" AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Añadir responded_at si aún no existe
SET @column_name = 'responded_at';

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE rasgo_asistencia ADD COLUMN responded_at TIMESTAMP NULL AFTER falta_observacion',
  'SELECT "responded_at ya existe; se omite" AS status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migración completada sin borrar datos previos.' AS resultado;
