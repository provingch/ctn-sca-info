-- Migración segura para alinear las tablas de rasgo con el esquema actual.
-- Es idempotente: se puede ejecutar varias veces sin borrar datos ni romper la BD.

SET @table_schema = DATABASE();

-- Crear tablas base si aún no existen (solo estructura mínima para evitar errores).
CREATE TABLE IF NOT EXISTS planilla_rasgo (
  id INT AUTO_INCREMENT PRIMARY KEY,
  curso_id INT NOT NULL,
  profesor_id INT NOT NULL,
  tema VARCHAR(150) NOT NULL,
  fecha_clase DATE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS rasgo_asistencia (
  id INT AUTO_INCREMENT PRIMARY KEY,
  planilla_rasgo_id INT NOT NULL,
  alumno_id INT NOT NULL,
  alumno_nombre VARCHAR(80) NOT NULL,
  alumno_apellido VARCHAR(80) NOT NULL,
  alumno_email VARCHAR(255) NOT NULL,
  estado ENUM('pendiente', 'presente', 'ausente') NOT NULL DEFAULT 'pendiente',
  falta_codigo VARCHAR(4) NULL,
  falta_observacion VARCHAR(500) NULL,
  responded_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Añadir fecha_clase si aún no existe en planilla_rasgo.
SET @table_name = 'planilla_rasgo';
SET @column_name = 'fecha_clase';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE planilla_rasgo ADD COLUMN fecha_clase DATE NOT NULL AFTER tema',
  'SELECT "fecha_clase ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Añadir created_at si aún no exista en planilla_rasgo.
SET @column_name = 'created_at';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE planilla_rasgo ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER fecha_clase',
  'SELECT "created_at ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Añadir alumno_email si aún no exista en rasgo_asistencia.
SET @table_name = 'rasgo_asistencia';
SET @column_name = 'alumno_email';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE rasgo_asistencia ADD COLUMN alumno_email VARCHAR(255) NOT NULL DEFAULT "" AFTER alumno_apellido',
  'SELECT "alumno_email ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Añadir estado si aún no exista en rasgo_asistencia.
SET @column_name = 'estado';
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @table_schema
     AND TABLE_NAME = @table_name
     AND COLUMN_NAME = @column_name) = 0,
  'ALTER TABLE rasgo_asistencia ADD COLUMN estado ENUM(\'pendiente\', \'presente\', \'ausente\') NOT NULL DEFAULT \'pendiente\' AFTER alumno_email',
  'SELECT "estado ya existe; se omite" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Añadir falta_codigo si aún no exista.
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

-- Añadir falta_observacion si aún no exista.
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

-- Añadir responded_at si aún no exista.
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

-- Normalizar correos vacíos para evitar errores de NOT NULL.
UPDATE rasgo_asistencia
SET alumno_email = ''
WHERE alumno_email IS NULL OR TRIM(alumno_email) = '';

SELECT 'Migración completada sin borrar datos previos.' AS resultado;
