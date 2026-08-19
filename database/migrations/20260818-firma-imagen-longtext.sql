-- Migración: aumentar capacidad de firma_imagen
-- Fecha: 2026-08-18
-- Idempotente: modifica solo si la columna existe y no es LONGTEXT

SET @table_schema = DATABASE();
SET @table_name = 'profesor';
SET @column_name = 'firma_imagen';

-- Si la columna no existe, añadimos como LONGTEXT; si existe y no es LONGTEXT, la modificamos
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE profesor ADD COLUMN firma_imagen LONGTEXT NULL AFTER totp_secret',
  (SELECT IF(
       (SELECT DATA_TYPE FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @table_schema
          AND TABLE_NAME = @table_name
          AND COLUMN_NAME = @column_name) = 'longtext',
       'SELECT "firma_imagen ya es LONGTEXT; se omite"',
       'ALTER TABLE profesor MODIFY COLUMN firma_imagen LONGTEXT NULL'
     )
  )
);

PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'Migración firma_imagen LONGTEXT ejecutada.' AS resultado;
