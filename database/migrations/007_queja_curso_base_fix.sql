-- Corregir quejas legacy para que apunten a curso_base y no a curso.
-- Esto elimina registros rotos que quedaron con un curso_id que no existe en curso_base y re-sincroniza la especialidad.

ALTER TABLE queja DROP FOREIGN KEY IF EXISTS fk_queja_curso;

-- Eliminar registros inválidos que quedaron apuntando a un curso que no existe en curso_base.
DELETE q
FROM queja q
LEFT JOIN curso_base cb ON cb.id = q.curso_id
WHERE cb.id IS NULL;

-- Mantener la especialidad coherente con el curso_base asociado.
UPDATE queja q
JOIN curso_base cb ON cb.id = q.curso_id
SET q.especialidad_id = cb.especialidad_id
WHERE q.especialidad_id <> cb.especialidad_id;

ALTER TABLE queja
    ADD CONSTRAINT fk_queja_curso
    FOREIGN KEY (curso_id) REFERENCES curso_base(id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT;
