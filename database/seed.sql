-- Seed data for ctndb
-- Limpia las tablas y recarga los datos de ejemplo

use ctndb;

CREATE TABLE IF NOT EXISTS planilla_rasgo (
	id INT AUTO_INCREMENT PRIMARY KEY,
	curso_id INT NOT NULL,
	profesor_id INT NOT NULL,
	tema VARCHAR(150) NOT NULL,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	KEY idx_planilla_rasgo_curso (curso_id),
	KEY idx_planilla_rasgo_profesor (profesor_id),
	CONSTRAINT fk_planilla_rasgo_curso FOREIGN KEY (curso_id)
		REFERENCES curso (id)
		ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT fk_planilla_rasgo_profesor FOREIGN KEY (profesor_id)
		REFERENCES profesor (id)
		ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rasgo_asistencia (
	id INT AUTO_INCREMENT PRIMARY KEY,
	planilla_rasgo_id INT NOT NULL,
	alumno_id INT NOT NULL,
	alumno_nombre VARCHAR(80) NOT NULL,
	alumno_apellido VARCHAR(80) NOT NULL,
	alumno_email VARCHAR(255) NOT NULL,
	estado ENUM('pendiente', 'presente', 'ausente') NOT NULL DEFAULT 'pendiente',
	falta_codigo VARCHAR(4) NULL,
	falta_observacion VARCHAR(500) NULL,
	responded_at TIMESTAMP NULL,
	UNIQUE KEY uq_rasgo_asistencia_alumno (planilla_rasgo_id, alumno_id),
	KEY idx_rasgo_asistencia_alumno (alumno_id),
	CONSTRAINT fk_rasgo_asistencia_planilla FOREIGN KEY (planilla_rasgo_id)
		REFERENCES planilla_rasgo (id)
		ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT fk_rasgo_asistencia_alumno FOREIGN KEY (alumno_id)
		REFERENCES alumno (id)
		ON UPDATE CASCADE ON DELETE CASCADE
);

DELETE FROM puntaje;
DELETE FROM registro;
DELETE FROM tarea;
DELETE FROM instrumento;
DELETE FROM planilla;
DELETE FROM rasgo_asistencia;
DELETE FROM planilla_rasgo;
DELETE FROM profesor_materia;
DELETE FROM materia_especialidad;
DELETE FROM materia;
DELETE FROM alumno_padre;
DELETE FROM padre;
DELETE FROM alumno;
DELETE FROM curso;
DELETE FROM profesor;
DELETE FROM especialidad;

-- Reset AUTO_INCREMENT
ALTER TABLE alumno AUTO_INCREMENT = 1;
ALTER TABLE profesor AUTO_INCREMENT = 1;
ALTER TABLE padre AUTO_INCREMENT = 1;
ALTER TABLE materia AUTO_INCREMENT = 1;
ALTER TABLE planilla AUTO_INCREMENT = 1;
ALTER TABLE tarea AUTO_INCREMENT = 1;
ALTER TABLE registro AUTO_INCREMENT = 1;
ALTER TABLE planilla_rasgo AUTO_INCREMENT = 1;
ALTER TABLE rasgo_asistencia AUTO_INCREMENT = 1;

-- ========================================
-- ESPECIALIDADES
-- ========================================
INSERT INTO especialidad (id, nombre) VALUES
(1, 'Construcciones Civiles'),
(2, 'Electricidad'),
(3, 'Electrónica'),
(4, 'Electromecánica'),
(5, 'Informática'),
(6, 'Mecánica General'),
(7, 'Mecánica Automotriz'),
(8, 'Química Industrial');

-- (rest omitted for brevity in this copy)
