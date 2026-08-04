-- Seed de prueba completa para ctndb
-- Cubre login, perfil, PWA/push, home, planillas, evaluación, exportación y portal de padres.

USE ctndb;

DELETE FROM push_subscription;
DELETE FROM puntaje;
DELETE FROM registro;
DELETE FROM tarea;
DELETE FROM instrumento;
DELETE FROM planilla;
DELETE FROM asignacion;
DELETE FROM profesor_materia;
DELETE FROM materia_especialidad;
DELETE FROM materia;
DELETE FROM alumno_padre;
DELETE FROM padre;
DELETE FROM alumno;
DELETE FROM curso;
DELETE FROM profesor;
DELETE FROM especialidad;

ALTER TABLE push_subscription AUTO_INCREMENT = 1;
ALTER TABLE puntaje AUTO_INCREMENT = 1;
ALTER TABLE registro AUTO_INCREMENT = 1;
ALTER TABLE tarea AUTO_INCREMENT = 1;
ALTER TABLE planilla AUTO_INCREMENT = 1;
ALTER TABLE materia AUTO_INCREMENT = 1;
ALTER TABLE alumno AUTO_INCREMENT = 1;
ALTER TABLE curso AUTO_INCREMENT = 1;
ALTER TABLE profesor AUTO_INCREMENT = 1;
ALTER TABLE padre AUTO_INCREMENT = 1;
ALTER TABLE especialidad AUTO_INCREMENT = 1;
ALTER TABLE asignacion AUTO_INCREMENT = 1;

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

-- ========================================
-- CURSOS
-- ========================================
INSERT INTO curso (id, especialidad_id, promocion, seccion) VALUES
(1, 5, 2027, 'A'),
(2, 5, 2026, 'A'),
(3, 5, 2025, 'A'),
(4, 2, 2027, 'A'),
(5, 2, 2026, 'B'),
(6, 2, 2025, 'A'),
(7, 1, 2027, 'C'),
(8, 1, 2026, 'C'),
(9, 1, 2025, 'B'),
(10, 1, 2027, 'A'),
(11, 1, 2027, 'B'),
(12, 1, 2026, 'A'),
(13, 1, 2026, 'B'),
(14, 1, 2025, 'A'),
(15, 1, 2025, 'C'),
(16, 2, 2027, 'B'),
(17, 2, 2026, 'A'),
(18, 2, 2025, 'B'),
(19, 3, 2027, 'A'),
(20, 3, 2027, 'B'),
(21, 3, 2027, 'C'),
(22, 3, 2026, 'A'),
(23, 3, 2026, 'B'),
(24, 3, 2026, 'C'),
(25, 3, 2025, 'A'),
(26, 3, 2025, 'B'),
(27, 3, 2025, 'C'),
(28, 4, 2027, 'A'),
(29, 4, 2027, 'B'),
(30, 4, 2026, 'A'),
(31, 4, 2026, 'B'),
(32, 4, 2025, 'A'),
(33, 4, 2025, 'B'),
(34, 5, 2027, 'B'),
(35, 5, 2026, 'B'),
(36, 5, 2025, 'B'),
(37, 6, 2027, 'A'),
(38, 6, 2027, 'B'),
(39, 6, 2026, 'A'),
(40, 6, 2026, 'B'),
(41, 6, 2025, 'A'),
(42, 6, 2025, 'B'),
(43, 7, 2027, 'A'),
(44, 7, 2027, 'B'),
(45, 7, 2026, 'A'),
(46, 7, 2026, 'B'),
(47, 7, 2025, 'A'),
(48, 7, 2025, 'B'),
(49, 8, 2027, 'A'),
(50, 8, 2027, 'B'),
(51, 8, 2027, 'C'),
(52, 8, 2026, 'A'),
(53, 8, 2026, 'B'),
(54, 8, 2026, 'C'),
(55, 8, 2025, 'A'),
(56, 8, 2025, 'B'),
(57, 8, 2025, 'C');

-- ========================================
-- PROFESORES
-- ========================================
INSERT INTO profesor
(id, nombre, apellido, usuario, contrasenia, ci, telefono, celular, correo, google_email, google_access_token, google_refresh_token, google_token_expiry, materias_manual, totp_secret, especialidad_id, nivel)
VALUES
(1, 'Laura', 'Rojas', 'profe.informatica', 'ctn2025', 4010101, 0981111111, 0981111111, 'laura.rojas@ctn.local', NULL, NULL, NULL, NULL, 'Programación I, Redes y Comunicaciones, Base de Datos', 'JBSWY3DPEHPK3PXP', 5, 1),
(2, 'Diego', 'Mora', 'profe.electricidad', 'ctn2025', 4020202, 0982222222, 0982222222, 'diego.mora@ctn.local', NULL, NULL, NULL, NULL, 'Instalaciones Eléctricas, Automatización', 'JBSWY3DPEHPK3PXP', 2, 1),
(3, 'Admin', 'Sistema', 'admin', 'ctn2025', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 2),
(4, 'Integración', 'Informática', 'informatica-itg', 'ctn2025', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 5, 3),
(5, 'Integración', 'Electricidad', 'electricidad-itg', 'ctn2025', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 2, 3),
(6, 'María', 'Benítez', 'profe.construcciones', 'ctn2025', 4030303, 0983333333, 0983333333, 'maria.benitez@ctn.local', NULL, NULL, NULL, NULL, 'Dibujo Técnico, Seguridad e Higiene', 'JBSWY3DPEHPK3PXP', 1, 1);

-- ========================================
-- MATERIAS
-- ========================================
INSERT INTO materia (id, nombre, categoria) VALUES
(1, 'Programación I', 'especifico'),
(2, 'Redes y Comunicaciones', 'especifico'),
(3, 'Base de Datos', 'especifico'),
(4, 'Instalaciones Eléctricas', 'especifico'),
(5, 'Automatización', 'especifico'),
(6, 'Dibujo Técnico', 'especifico'),
(7, 'Seguridad e Higiene', 'comun');

-- ========================================
-- RELACIÓN MATERIA-ESPECIALIDAD
-- ========================================
INSERT INTO materia_especialidad (materia_id, especialidad_id) VALUES
(1, 5),
(2, 5),
(3, 5),
(4, 2),
(5, 2),
(6, 1),
(7, 1),
(7, 2),
(7, 5);

-- ========================================
-- RELACIÓN PROFESOR-MATERIA
-- ========================================
INSERT INTO profesor_materia (profesor_id, materia_id) VALUES
(1, 1),
(1, 2),
(1, 3),
(2, 4),
(2, 5),
(6, 6),
(6, 7);

-- ========================================
-- ASIGNACIONES
-- ========================================
INSERT INTO asignacion (id, profesor_id, materia_id, curso_id) VALUES
(1, 1, 1, 1),
(2, 1, 2, 2),
(3, 1, 3, 3),
(4, 2, 4, 4),
(5, 2, 5, 5),
(6, 6, 6, 7),
(7, 6, 7, 9);

-- ========================================
-- INSTRUMENTOS
-- ========================================
INSERT INTO instrumento (id, nombre) VALUES
(1, 'Cuaderno/portafolio'),
(2, 'Trabajo práctico'),
(3, 'Prueba sumativa'),
(4, 'Prueba de cierre de etapa'),
(5, 'Trabajo de investigación'),
(6, 'Presentación oral');

-- ========================================
-- PADRES
-- ========================================
INSERT INTO padre (id, ci, nombre, apellido, usuario, contrasenia, telefono, correo, totp_secret) VALUES
(1, 5010101, 'Carlos', 'Pérez', 'padre.carlos', 'ctn2025', '0971000001', 'carlos.perez@example.com', 'JBSWY3DPEHPK3PXP'),
(2, 5020202, 'María', 'González', 'madre.maria', 'ctn2025', '0971000002', 'maria.gonzalez@example.com', NULL);

-- ========================================
-- ALUMNOS
-- ========================================
INSERT INTO alumno (id, nombre, apellido, curso_id, ci, correo_encargado, correo_encargado2, google_user_id, google_email) VALUES
(1, 'LUCAS', 'ALMEIDA', 1, 6010001, 'carlos.perez@example.com', NULL, NULL, NULL),
(2, 'MARTINA', 'BENÍTEZ', 1, 6010002, 'carlos.perez@example.com', 'maria.gonzalez@example.com', NULL, NULL),
(3, 'SANTIAGO', 'CABALLERO', 1, 6010003, NULL, NULL, NULL, NULL),
(4, 'VALERIA', 'DÍAZ', 2, 6020001, NULL, NULL, NULL, NULL),
(5, 'MATEO', 'ESCOBAR', 2, 6020002, NULL, NULL, NULL, NULL),
(6, 'SOFÍA', 'FRANCO', 2, 6020003, NULL, NULL, NULL, NULL),
(7, 'BRUNO', 'GALEANO', 4, 6030001, 'maria.gonzalez@example.com', NULL, NULL, NULL),
(8, 'LUCÍA', 'HERRERA', 4, 6030002, NULL, NULL, NULL, NULL),
(9, 'DIEGO', 'IBÁÑEZ', 5, 6040001, NULL, NULL, NULL, NULL),
(10, 'EMILIA', 'JIMÉNEZ', 7, 6050001, NULL, NULL, NULL, NULL),
(11, 'FRANCO', 'KLEIN', 7, 6050002, NULL, NULL, NULL, NULL);

-- ========================================
-- RELACIÓN ALUMNO-PADRE
-- ========================================
INSERT INTO alumno_padre (alumno_id, padre_id, parentesco) VALUES
(1, 1, 'padre'),
(2, 1, 'padre'),
(2, 2, 'madre'),
(7, 2, 'madre');

-- ========================================
-- PLANILLAS
-- ========================================
INSERT INTO planilla (id, curso_id, materia_id, periodo, etapa, profesor_id, google_course_id) VALUES
(1, 1, 1, 2025, 'primera', 1, NULL),
(2, 2, 2, 2025, 'segunda', 1, NULL),
(3, 4, 4, 2025, 'primera', 2, NULL),
(4, 7, 6, 2025, 'segunda', 6, NULL);

-- ========================================
-- TAREAS
-- ========================================
INSERT INTO tarea
(id, planilla_id, instrumento_id, fecha, total, titulo, google_coursework_id, google_coursework_url, fecha_inicio, fecha_limite)
VALUES
(1, 1, 2, '2025-03-10', 10, 'TP 1: Variables y tipos', NULL, NULL, '2025-03-01', '2025-03-10'),
(2, 1, 3, '2025-04-05', 20, 'Prueba 1: Fundamentos', NULL, NULL, '2025-04-01', '2025-04-05'),
(3, 2, 2, '2025-05-15', 15, 'TP 2: Redes básicas', NULL, NULL, '2025-05-01', '2025-05-15'),
(4, 2, 4, '2025-06-10', 25, 'Cierre de etapa: Redes', NULL, NULL, '2025-06-01', '2025-06-10'),
(5, 3, 2, '2025-03-20', 12, 'TP 1: Circuitos', NULL, NULL, '2025-03-10', '2025-03-20'),
(6, 3, 3, '2025-04-18', 18, 'Prueba 1: Medición', NULL, NULL, '2025-04-10', '2025-04-18'),
(7, 4, 5, '2025-07-01', 20, 'Trabajo final: Planos', NULL, NULL, '2025-06-15', '2025-07-01'),
(8, 4, 6, '2025-07-15', 20, 'Exposición final', NULL, NULL, '2025-07-01', '2025-07-15');

-- ========================================
-- REGISTROS
-- ========================================
INSERT INTO registro (id, planilla_id, alumno_id) VALUES
(1, 1, 1),
(2, 1, 2),
(3, 1, 3),
(4, 2, 4),
(5, 2, 5),
(6, 2, 6),
(7, 3, 7),
(8, 3, 8),
(9, 4, 10),
(10, 4, 11);

-- ========================================
-- PUNTAJES
-- ========================================
INSERT INTO puntaje (registro_id, tarea_id, puntos) VALUES
(1, 1, 8),
(1, 2, 16),
(2, 1, 9),
(2, 2, 18),
(3, 1, 6),
(3, 2, 12),
(4, 3, 14),
(4, 4, 20),
(5, 3, 11),
(5, 4, 17),
(6, 3, 10),
(6, 4, 16),
(7, 5, 9),
(7, 6, 15),
(8, 5, 7),
(8, 6, 14),
(9, 7, 18),
(9, 8, 19),
(10, 7, 16),
(10, 8, 17);

-- ========================================
-- PWA / PUSH
-- ========================================
INSERT INTO push_subscription (id, user_id, user_type, endpoint, p256dh, auth, created_at) VALUES
(1, 1, 'profesor', 'https://push.example.local/subscription/1', 'demo-p256dh', 'demo-auth', NOW());
