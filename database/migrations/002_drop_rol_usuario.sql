-- PASO 3: eliminación segura de la columna rol.
-- Ejecutar manualmente sólo después de confirmar que el paso 2 quedó desplegado
-- y funcionando en producción sin errores durante al menos un día.
-- Recomendado: hacer backup previo antes de ejecutar.

SET @drop_rol_sql = (
	SELECT IF(EXISTS (
		SELECT 1 FROM information_schema.columns
		WHERE table_schema = DATABASE() AND table_name = 'usuario' AND column_name = 'rol'
	), 'ALTER TABLE usuario DROP COLUMN rol', 'SELECT 1')
);
PREPARE drop_rol_statement FROM @drop_rol_sql;
EXECUTE drop_rol_statement;
DEALLOCATE PREPARE drop_rol_statement;
