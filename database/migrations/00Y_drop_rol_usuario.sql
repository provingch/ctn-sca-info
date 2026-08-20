-- PASO 3: eliminación segura de la columna rol.
-- Ejecutar manualmente sólo después de confirmar que el paso 2 quedó desplegado
-- y funcionando en producción sin errores durante al menos un día.
-- Recomendado: hacer backup previo antes de ejecutar.

ALTER TABLE usuario DROP COLUMN rol;
