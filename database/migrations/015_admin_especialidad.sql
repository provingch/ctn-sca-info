-- Migración 015_admin_especialidad.sql
-- Añade la relación de especialidad a los administradores por especialidad.
-- Es idempotente y evita crear constraints duplicados.

ALTER TABLE usuario
  ADD COLUMN IF NOT EXISTS especialidad_id INT NULL AFTER activity_log_path;

SET @constraint_exists = (
  SELECT COUNT(*)
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'usuario'
    AND CONSTRAINT_NAME = 'fk_usuario_especialidad'
);
SET @sql = IF(@constraint_exists = 0,
  'ALTER TABLE usuario ADD CONSTRAINT fk_usuario_especialidad FOREIGN KEY (especialidad_id) REFERENCES especialidad (id) ON UPDATE CASCADE ON DELETE RESTRICT',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
