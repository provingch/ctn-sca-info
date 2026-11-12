-- Migration: agregar columnas email y telefono a la tabla alumno
-- Estas columnas se sincronizarán con ctnapi mediante el core-sync

SET NAMES utf8mb4;

ALTER TABLE alumno ADD COLUMN IF NOT EXISTS email VARCHAR(150) NULL AFTER curso_id;
ALTER TABLE alumno ADD COLUMN IF NOT EXISTS telefono VARCHAR(30) NULL AFTER email;

-- end migration