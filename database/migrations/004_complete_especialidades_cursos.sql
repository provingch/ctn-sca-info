-- Completa el catalogo institucional sin duplicar datos existentes.
-- Promocion = anio de egreso: 2028 corresponde a 1er curso en 2026.

INSERT INTO especialidad (id, nombre)
SELECT ids.id, ids.nombre
FROM (
    SELECT 1 AS id, 'Construcciones Civiles' AS nombre
    UNION ALL SELECT 2, 'Electricidad'
    UNION ALL SELECT 3, 'Electrónica'
    UNION ALL SELECT 4, 'Electromecánica'
    UNION ALL SELECT 5, 'Informática'
    UNION ALL SELECT 6, 'Mecánica General'
    UNION ALL SELECT 7, 'Mecánica Automotriz'
    UNION ALL SELECT 8, 'Química Industrial'
) ids
WHERE NOT EXISTS (
    SELECT 1
    FROM especialidad e
    WHERE e.id = ids.id OR e.nombre = ids.nombre
);

INSERT IGNORE INTO curso (especialidad_id, promocion, seccion)
SELECT e.id, anios.promocion, secciones.seccion
FROM especialidad e
JOIN (
    SELECT 2025 AS promocion
    UNION ALL SELECT 2026
    UNION ALL SELECT 2027
    UNION ALL SELECT 2028
) anios
JOIN (
    SELECT 'A' AS seccion
    UNION ALL SELECT 'B'
) secciones;

-- Construcciones Civiles, Electrónica y Química Industrial mantienen
-- las secciones A, B y C en los tres cursos.
INSERT IGNORE INTO curso (especialidad_id, promocion, seccion)
SELECT e.id, 2028, 'C'
FROM especialidad e
WHERE e.id IN (1, 3, 8);