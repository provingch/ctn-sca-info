-- Migración 012_verificacion_tema.sql
-- Añade columnas y claves necesarias para la verificación de tema contra plan curricular
-- Usa IF NOT EXISTS para ser idempotente en MySQL 8+

ALTER TABLE planilla_rasgo
  ADD COLUMN IF NOT EXISTS asignacion_id INT NULL AFTER usuario_id,
  ADD COLUMN IF NOT EXISTS estado_verificacion_tema ENUM('OK','DUDOSO','NO_COINCIDE','SIN_PLAN') NOT NULL DEFAULT 'SIN_PLAN',
  ADD COLUMN IF NOT EXISTS tema_plan_curricular_id INT NULL;

ALTER TABLE tema_plan_curricular
  ADD COLUMN IF NOT EXISTS estado_cobertura ENUM('PENDIENTE','CUBIERTO') NOT NULL DEFAULT 'PENDIENTE',
  ADD COLUMN IF NOT EXISTS fecha_cobertura TIMESTAMP NULL,
  ADD COLUMN IF NOT EXISTS planilla_rasgo_id INT NULL;

-- Constraints (ignorar si ya existen)
SET @constraint_exists = (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'planilla_rasgo'
    AND CONSTRAINT_NAME = 'fk_planilla_rasgo_asignacion'
);
SET @sql = IF(@constraint_exists = 0,
  'ALTER TABLE planilla_rasgo ADD CONSTRAINT fk_planilla_rasgo_asignacion FOREIGN KEY (asignacion_id) REFERENCES asignacion (id) ON UPDATE CASCADE ON DELETE SET NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @constraint_exists = (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'planilla_rasgo'
    AND CONSTRAINT_NAME = 'fk_planilla_rasgo_tema'
);
SET @sql = IF(@constraint_exists = 0,
  'ALTER TABLE planilla_rasgo ADD CONSTRAINT fk_planilla_rasgo_tema FOREIGN KEY (tema_plan_curricular_id) REFERENCES tema_plan_curricular (id) ON UPDATE CASCADE ON DELETE SET NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @constraint_exists = (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tema_plan_curricular'
    AND CONSTRAINT_NAME = 'fk_tema_plan_planilla_rasgo'
);
SET @sql = IF(@constraint_exists = 0,
  'ALTER TABLE tema_plan_curricular ADD CONSTRAINT fk_tema_plan_planilla_rasgo FOREIGN KEY (planilla_rasgo_id) REFERENCES planilla_rasgo (id) ON UPDATE CASCADE ON DELETE SET NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
