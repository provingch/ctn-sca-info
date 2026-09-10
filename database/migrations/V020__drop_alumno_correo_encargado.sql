-- Migration: se elimina alumno.correo_encargado / correo_encargado2.
-- La relación real padre↔alumno vive en alumno_usuario; el correo del
-- encargado se obtiene del usuario (nivel 4) vinculado. Las columnas
-- competían con esa fuente de verdad y ya no las consume ningún proceso.
-- Run-order: V020

ALTER TABLE alumno DROP COLUMN IF EXISTS correo_encargado;
ALTER TABLE alumno DROP COLUMN IF EXISTS correo_encargado2;

-- end migration
