-- PASO 1: sincronización segura de nivel/rol para la tabla usuario.
-- Ejecutar manualmente contra la base de producción, con backup previo:
--   mysqldump --user=... --password=... --single-transaction --routines --events --triggers DB_NAME > backup_rol_nivel_$(date +%Y%m%d_%H%M%S).sql
--
-- Regla de negocio objetivo:
--   - nivel = 4 representa 'padre'
--   - rol se mantiene solo como compatibilidad temporal hasta el paso 4
--
-- Este script es idempotente y no destructivo:
--   1) corrige nivel=4 para usuarios que actualmente permanecen en rol='padre'
--   2) corrige rol='padre' para usuarios ya marcados con nivel=4
--   3) reporta usuarios con nivel=0 para revisión manual antes de seguir

START TRANSACTION;

-- 1) Revisión manual: usuarios con nivel = 0 (pueden ser el origen del bug observado)
SELECT
    id,
    nombre,
    apellido,
    usuario,
    nivel,
    rol,
    correo,
    especialidad_id
FROM usuario
WHERE nivel = 0
ORDER BY id;

-- 2) Unificar a nivel como fuente de verdad: si un usuario es padre por rol, dejar nivel=4
UPDATE usuario
SET nivel = 4
WHERE rol = 'padre'
  AND nivel <> 4;

-- 3) Compatibilidad temporal: si un usuario ya está en nivel=4, dejar rol='padre'
UPDATE usuario
SET rol = 'padre'
WHERE nivel = 4
  AND (rol IS NULL OR rol <> 'padre');

COMMIT;

-- Resultado final esperado: nivel=0 queda vacío antes de continuar con el siguiente paso.
-- Si aparecen usuarios con nivel=0, revisarlos manualmente antes de migrar el backend.
