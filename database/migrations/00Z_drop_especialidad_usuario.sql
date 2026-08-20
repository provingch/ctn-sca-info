-- Paso seguro: eliminar la columna legacy usuario.especialidad_id
-- Requiere validación previa de los datos reales en producci�n.
-- No se ejecuta desde esta tarea; se deja preparada para aplicaci�n manual.

START TRANSACTION;

ALTER TABLE usuario
    DROP FOREIGN KEY fk_usuario_especialidad;

ALTER TABLE usuario
    DROP COLUMN especialidad_id;

COMMIT;
