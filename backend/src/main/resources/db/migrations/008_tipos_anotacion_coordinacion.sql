-- Migration: lista oficial de tipos de anotación (coordinación) + campos nuevos.
-- Corre DESPUÉS de 007. No borra anotaciones existentes: los tipos viejos solo se desactivan.
-- Origen: V034__tipos_anotacion_coordinacion.sql de la app de anotaciones (ceciapi/), más la columna repetible.

SET NAMES utf8mb4;

-- requiere_observacion: la app obliga a escribir un texto (se usa en "Otro...").
-- orden: orden en que se muestran las opciones en la app.
-- repetible: el tipo se puede registrar más de una vez el mismo día para el mismo alumno
-- (reemplaza los ids fijos tipos_repetibles = [12, 15, 17, 18] de la API PHP original).
ALTER TABLE tipo_anotacion
    ADD COLUMN IF NOT EXISTS requiere_observacion BOOLEAN NOT NULL DEFAULT FALSE AFTER requiere_foto,
    ADD COLUMN IF NOT EXISTS orden INT NOT NULL DEFAULT 0 AFTER requiere_observacion,
    ADD COLUMN IF NOT EXISTS repetible BOOLEAN NOT NULL DEFAULT FALSE AFTER orden;

-- Se desactivan todos los tipos anteriores (siguen existiendo para no romper el historial).
UPDATE tipo_anotacion SET activo = FALSE;

-- Lista de la coordinadora. La foto es opcional en todos (requiere_foto = FALSE);
-- para exigirla en alguno: UPDATE tipo_anotacion SET requiere_foto = TRUE WHERE descripcion = '...';
INSERT INTO tipo_anotacion (descripcion, codigo_conducta, requiere_foto, requiere_observacion, orden, repetible, activo)
SELECT d.descripcion,
       (SELECT codigo FROM codigo_conducta WHERE codigo = d.codigo),
       FALSE, d.requiere_observacion, d.orden, d.repetible, TRUE
FROM (
    SELECT 'Falta el día del examen' AS descripcion, NULL AS codigo, FALSE AS requiere_observacion, 1 AS orden, FALSE AS repetible
    UNION ALL SELECT 'Vapea (usa pod, cigarrillo electrónico)', NULL, FALSE, 2, TRUE
    UNION ALL SELECT 'Uniforme incompleto', 'N7', FALSE, 3, FALSE
    UNION ALL SELECT 'Sin uniforme correspondiente', 'N7', FALSE, 4, FALSE
    UNION ALL SELECT 'Utiliza palabras inapropiadas', 'N5', FALSE, 5, TRUE
    UNION ALL SELECT 'Comportamiento inapropiado (demostraciones de afecto en público)', NULL, FALSE, 6, FALSE
    UNION ALL SELECT 'Utiliza el sanitario que no le corresponde a su especialidad', NULL, FALSE, 7, TRUE
    UNION ALL SELECT 'Está fuera de clase en horario establecido', NULL, FALSE, 8, TRUE
    UNION ALL SELECT 'Otro...', NULL, TRUE, 9, FALSE
) AS d
ON DUPLICATE KEY UPDATE
    activo = TRUE,
    requiere_foto = FALSE,
    requiere_observacion = VALUES(requiere_observacion),
    orden = VALUES(orden),
    repetible = VALUES(repetible);
