-- Catálogo de horas cátedra (bloques de 35 minutos) y horario semanal
-- por asignación (profesor + materia + curso). No modela turno como
-- restricción de negocio: "etiqueta" es solo informativa para agrupar
-- en pantalla. No valida sala: es un campo de texto libre opcional.

START TRANSACTION;

CREATE TABLE hora_catedra (
    id INT AUTO_INCREMENT,
    numero SMALLINT UNSIGNED NOT NULL,
    etiqueta VARCHAR(20) NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_hora_catedra_numero (numero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE horario_slot (
    id INT AUTO_INCREMENT,
    asignacion_id INT NOT NULL,
    usuario_id INT NOT NULL,
    curso_id INT NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL COMMENT '1=Lunes ... 6=Sabado',
    hora_catedra_id INT NOT NULL,
    sala VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_horario_profesor (dia_semana, hora_catedra_id, usuario_id),
    UNIQUE KEY uq_horario_curso (dia_semana, hora_catedra_id, curso_id),
    CONSTRAINT fk_horario_slot_asignacion FOREIGN KEY (asignacion_id)
        REFERENCES asignacion (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_horario_slot_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_horario_slot_curso FOREIGN KEY (curso_id)
        REFERENCES curso (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_horario_slot_hora_catedra FOREIGN KEY (hora_catedra_id)
        REFERENCES hora_catedra (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Seed inicial del catálogo, en base al horario impreso de referencia
-- (turno mañana = 1-8, turno T.O. = 9-16). AJUSTAR horarios reales de
-- la tarde si difieren — estos son los observados en el ejemplo.
INSERT INTO hora_catedra (numero, etiqueta, hora_inicio, hora_fin) VALUES
(1,  'M', '07:00', '07:35'),
(2,  'M', '07:35', '08:10'),
(3,  'M', '08:10', '08:45'),
(4,  'M', '08:45', '09:20'),
(5,  'M', '09:40', '10:15'),
(6,  'M', '10:15', '10:50'),
(7,  'M', '10:50', '11:25'),
(8,  'M', '11:25', '12:00'),
(9,  'T', '13:00', '13:35'),
(10, 'T', '13:35', '14:10'),
(11, 'T', '14:10', '14:45'),
(12, 'T', '14:45', '15:20'),
(13, 'T', '15:40', '16:15'),
(14, 'T', '16:15', '16:50'),
(15, 'T', '16:50', '17:25'),
(16, 'T', '17:25', '18:00');

COMMIT;
