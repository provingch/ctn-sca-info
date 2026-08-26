CREATE TABLE IF NOT EXISTS curso_base (
    id INT AUTO_INCREMENT,
    especialidad_id INT NOT NULL,
    nivel TINYINT NOT NULL,
    seccion ENUM('A', 'B', 'C') NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_curso_base (especialidad_id, nivel, seccion),
    CONSTRAINT fk_curso_base_especialidad FOREIGN KEY (especialidad_id)
        REFERENCES especialidad (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO curso_base (id, especialidad_id, nivel, seccion)
SELECT c.id,
       c.especialidad_id,
       CASE
           WHEN c.promocion >= YEAR(CURDATE()) + 2 THEN 1
           WHEN c.promocion = YEAR(CURDATE()) + 1 THEN 2
           ELSE 3
       END AS nivel,
       c.seccion
FROM curso c;

ALTER TABLE asignacion DROP FOREIGN KEY fk_asig_curso;
ALTER TABLE asignacion CHANGE COLUMN curso_id curso_base_id INT NOT NULL;
ALTER TABLE asignacion ADD CONSTRAINT fk_asig_curso_base FOREIGN KEY (curso_base_id)
    REFERENCES curso_base (id)
    ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE horario_slot DROP FOREIGN KEY fk_horario_slot_curso;
ALTER TABLE horario_slot CHANGE COLUMN curso_id curso_base_id INT NOT NULL;
ALTER TABLE horario_slot ADD CONSTRAINT fk_horario_slot_curso FOREIGN KEY (curso_base_id)
    REFERENCES curso_base (id)
    ON UPDATE CASCADE ON DELETE CASCADE;
