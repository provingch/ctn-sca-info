-- Migration: queja.curso_id pasa a apuntar a `curso` (la cohorte: especialidad + promoción + sección)
-- en vez de `curso_base` (especialidad + nivel + sección, sin año). Así una queja sobre "3° A" queda
-- ligada a la promoción que lo cursaba ese año y no se mezcla con las de otros años.
--
-- Cada queja existente se traduce a la cohorte que ocupaba ese nivel el año en que se cargó:
-- promocion = YEAR(creada_en) - nivel + 3 (mismo puente que usan asignaciones y planillas).
-- Si esa cohorte no existe, curso_id queda NULL en vez de apuntar a un curso equivocado.
--
-- El runner parte por ";" y el DDL de MariaDB/MySQL hace commit implícito, así que cada paso es
-- seguro de repetir si la migración se corta a mitad: el id original se copia primero a una
-- columna auxiliar y todo se recalcula desde ahí. La columna auxiliar se borra al final.
--
-- Después de aplicarla, actualizar también db-tables-properties.sql (FK de queja.curso_id a curso).

SET NAMES utf8mb4;

-- 1. Copia del id de curso_base original (solo la primera vez).
ALTER TABLE queja ADD COLUMN IF NOT EXISTS curso_base_id_migracion INT NULL;
UPDATE queja SET curso_base_id_migracion = curso_id WHERE curso_base_id_migracion IS NULL;

-- 2. Se quita la FK que tuviera curso_id (hacia curso_base, o la nueva si es un reintento).
SET @fk = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'queja' AND COLUMN_NAME = 'curso_id'
             AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk IS NULL, 'DO 0', CONCAT('ALTER TABLE queja DROP FOREIGN KEY `', @fk, '`'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. curso_id con el mismo tipo que curso.id y admitiendo NULL.
SET @tipo = (SELECT COLUMN_TYPE FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'curso' AND COLUMN_NAME = 'id');
SET @sql = CONCAT('ALTER TABLE queja MODIFY COLUMN curso_id ', @tipo, ' NULL');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. Traducción curso_base -> cohorte del año de la queja.
UPDATE queja q
SET q.curso_id = (
    SELECT c.id FROM curso_base cb
    JOIN curso c ON c.especialidad_id = cb.especialidad_id AND c.seccion = cb.seccion
                AND c.promocion = YEAR(q.creada_en) - cb.nivel + 3
    WHERE cb.id = q.curso_base_id_migracion
    LIMIT 1);

-- 5. FK nueva hacia curso. Sin ON DELETE CASCADE: borrar un curso no borra sus quejas.
ALTER TABLE queja ADD CONSTRAINT fk_queja_curso FOREIGN KEY (curso_id) REFERENCES curso (id);

-- 6. Limpieza.
ALTER TABLE queja DROP COLUMN curso_base_id_migracion;
