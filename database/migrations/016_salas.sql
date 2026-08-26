-- Catalogo de salas comunes y salas propias de una especialidad.
CREATE TABLE IF NOT EXISTS sala (
    id INT AUTO_INCREMENT,
    nombre VARCHAR(45) NOT NULL,
    especialidad_id INT NULL COMMENT 'NULL = sala comun',
    PRIMARY KEY (id),
    UNIQUE KEY uq_sala_nombre_especialidad (nombre, especialidad_id),
    CONSTRAINT fk_sala_especialidad FOREIGN KEY (especialidad_id)
        REFERENCES especialidad (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

ALTER TABLE horario_slot ADD COLUMN IF NOT EXISTS sala_id INT NULL AFTER sala;

SET @constraint_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'horario_slot'
      AND CONSTRAINT_NAME = 'fk_horario_slot_sala'
);
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE horario_slot ADD CONSTRAINT fk_horario_slot_sala FOREIGN KEY (sala_id) REFERENCES sala (id) ON UPDATE CASCADE ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE horario_slot DROP COLUMN IF EXISTS sala;
