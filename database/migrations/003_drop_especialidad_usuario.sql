-- Paso seguro: eliminar la columna legacy usuario.especialidad_id
-- Requiere validación previa de los datos reales en producci�n.
-- No se ejecuta desde esta tarea; se deja preparada para aplicaci�n manual.

START TRANSACTION;

SET @drop_especialidad_fk_sql = (
    SELECT IF(EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'usuario'
          AND constraint_name = 'fk_usuario_especialidad'
    ), 'ALTER TABLE usuario DROP FOREIGN KEY fk_usuario_especialidad', 'SELECT 1')
);
PREPARE drop_especialidad_fk_statement FROM @drop_especialidad_fk_sql;
EXECUTE drop_especialidad_fk_statement;
DEALLOCATE PREPARE drop_especialidad_fk_statement;

SET @drop_especialidad_column_sql = (
    SELECT IF(EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'usuario' AND column_name = 'especialidad_id'
    ), 'ALTER TABLE usuario DROP COLUMN especialidad_id', 'SELECT 1')
);
PREPARE drop_especialidad_column_statement FROM @drop_especialidad_column_sql;
EXECUTE drop_especialidad_column_statement;
DEALLOCATE PREPARE drop_especialidad_column_statement;

COMMIT;
