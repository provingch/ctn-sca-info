-- Vaciado de datos de todas las tablas
DELETE FROM puntaje;
DELETE FROM registro;
DELETE FROM tarea;
DELETE FROM instrumento;
DELETE FROM planilla;
DELETE FROM rasgo_asistencia;
DELETE FROM planilla_rasgo;
DELETE FROM materia_especialidad;
DELETE FROM materia;
DELETE FROM alumno_usuario;
DELETE FROM alumno;
DELETE FROM curso;
DELETE FROM usuario;
DELETE FROM especialidad;

-- Reinicio de los contadores de AUTO_INCREMENT
ALTER TABLE alumno AUTO_INCREMENT = 1;
ALTER TABLE usuario AUTO_INCREMENT = 1;
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

-- ========================================
-- CURSOS
-- ========================================
-- Estos cursos son validos unicamente para el año 2025, 2026 y 2027. Para años posteriores se deben crear nuevos cursos.
-- El programa deberia ser capaz de crear cursos automaticamente para años posteriores, pero por ahora se hace manualmente.
-- ESTADO: No terminado. Se debe crear un script que genere los cursos automaticamente para años posteriores al 2027.
INSERT INTO curso (id, especialidad_id, promocion, seccion) VALUES
(1, 5, 2027, 'A'),
(2, 5, 2027, 'B'),
(3, 5, 2026, 'A'),
(4, 5, 2026, 'B'),
(5, 5, 2025, 'A'),
(6, 5, 2025, 'B'),
(7, 4, 2027, 'A'),
(8, 4, 2027, 'B'),
(9, 4, 2026, 'A'),
(10, 4, 2026, 'B'),
(11, 4, 2025, 'A'),
(12, 4, 2025, 'B'),
(13, 2, 2026, 'A'),
(14, 2, 2026, 'B'),
(15, 2, 2025, 'A'),
(16, 2, 2025, 'B'),
(17, 1, 2027, 'A'),
(18, 1, 2027, 'C'),
(19, 1, 2027, 'B'),
(20, 1, 2026, 'A'),
(21, 1, 2026, 'C'),
(22, 1, 2026, 'B'),
(23, 1, 2025, 'A'),
(24, 1, 2025, 'C'),
(25, 1, 2025, 'B'),
(26, 2, 2027, 'A'),
(27, 2, 2027, 'B'),
(28, 3, 2027, 'A'),
(29, 3, 2027, 'B'),
(30, 3, 2027, 'C'),
(31, 3, 2026, 'A'),
(32, 3, 2026, 'B'),
(33, 3, 2026, 'C'),
(34, 3, 2025, 'A'),
(35, 3, 2025, 'B'),
(36, 3, 2025, 'C'),
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
-- Se remueve la insersion de profesores para evitar conflictos con la
-- autenticacion de Google, ya que los usuarios deben ser creados a traves del flujo de autenticacion.

-- ========================================
-- MATERIAS
-- ========================================
-- Se remueve la insersion de materias para evitar conflictos con la
-- autenticacion de Google, ya que los usuarios deben ser creados a traves del flujo de autenticacion.

-- ========================================
-- INSTRUMENTOS
-- ========================================
INSERT INTO instrumento (id, nombre) VALUES
(1, 'Cuaderno/portafolio'),
(2, 'Fichas de trabajo/biblioteca/laboratorio'),
(3, 'Presentaciones Orales'),
(4, 'Prueba de cierre de etapa'),
(5, 'Prueba Sumativa'),
(6, 'Pruebas Orales'),
(7, 'Socio Afectivo'),
(8, 'Trabajo de investigación grupal'),
(9, 'Trabajo de Investigación individual'),
(10, 'Trabajos en clase'),
(11, 'Trabajos en DECECI'),
(12, 'Trabajos en forma Virtual');

-- ========================================
-- ALUMNOS
-- ========================================
-- Se remueve la insersion de alumnos para evitar conflictos con la
-- autenticacion de Google, ya que los usuarios deben ser creados a traves del flujo de autenticacion.

-- ========================================
-- ADMINISTRADORES
-- ========================================
-- Solo deberia haber un admin para todo el sistema.
INSERT INTO usuario (nombre, apellido, usuario, rol) VALUES
('Administrador', 'Principal', 'admin', );