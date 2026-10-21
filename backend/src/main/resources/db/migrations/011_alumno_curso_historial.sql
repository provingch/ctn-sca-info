-- Migration: historial de cambios de curso de cada alumno (traslados entre especialidades o secciones).
-- Se escribe una fila cada vez que el panel de admin le cambia el curso a un alumno. Con ella:
--  * las planillas del curso viejo siguen mostrando al alumno (en gris, "Trasladado a ...") con sus notas
--    de solo lectura, en vez de esconderlo;
--  * la vista de padres muestra las notas del curso anterior, aclarando hasta cuándo estuvo.
-- Borrar al alumno borra su historial (ON DELETE CASCADE). Borrar un curso con historial no se permite.
--
-- Después de aplicarla, agregar también la tabla a db-tables-properties.sql.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS alumno_curso_historial (
    id INT AUTO_INCREMENT PRIMARY KEY,
    alumno_id INT NOT NULL,
    curso_anterior_id INT NOT NULL,
    curso_nuevo_id INT NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id INT NULL COMMENT 'admin que hizo el cambio',
    KEY idx_ach_alumno (alumno_id, fecha),
    KEY idx_ach_curso_anterior (curso_anterior_id),
    KEY idx_ach_curso_nuevo (curso_nuevo_id),
    KEY idx_ach_usuario (usuario_id),
    CONSTRAINT fk_ach_alumno FOREIGN KEY (alumno_id)
        REFERENCES alumno (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ach_curso_anterior FOREIGN KEY (curso_anterior_id)
        REFERENCES curso (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_ach_curso_nuevo FOREIGN KEY (curso_nuevo_id)
        REFERENCES curso (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_ach_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- end migration
