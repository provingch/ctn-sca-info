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
ALTER TABLE planilla_rasgo
  ADD CONSTRAINT IF NOT EXISTS fk_planilla_rasgo_asignacion FOREIGN KEY (asignacion_id) REFERENCES asignacion (id) ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE planilla_rasgo
  ADD CONSTRAINT IF NOT EXISTS fk_planilla_rasgo_tema FOREIGN KEY (tema_plan_curricular_id) REFERENCES tema_plan_curricular (id) ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE tema_plan_curricular
  ADD CONSTRAINT IF NOT EXISTS fk_tema_plan_planilla_rasgo FOREIGN KEY (planilla_rasgo_id) REFERENCES planilla_rasgo (id) ON UPDATE CASCADE ON DELETE SET NULL;

-- Nota: si su servidor MySQL no soporta IF NOT EXISTS en ALTER TABLE ADD COLUMN,
-- adaptar la migración usando cheques sobre INFORMATION_SCHEMA antes de ejecutar.
