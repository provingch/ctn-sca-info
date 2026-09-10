SET NAMES utf8mb4;

-- Vaciado de datos de todas las tablas
DELETE FROM puntaje;
DELETE FROM registro;
DELETE FROM tarea;
DELETE FROM instrumento;
DELETE FROM planilla;
DELETE FROM rasgo_asistencia_codigo;
DELETE FROM rasgo_asistencia;
DELETE FROM planilla_rasgo;
DELETE FROM horario_slot;
DELETE FROM hora_catedra;
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
ALTER TABLE hora_catedra AUTO_INCREMENT = 1;
ALTER TABLE horario_slot AUTO_INCREMENT = 1;
ALTER TABLE planilla AUTO_INCREMENT = 1;
ALTER TABLE tarea AUTO_INCREMENT = 1;
ALTER TABLE registro AUTO_INCREMENT = 1;
ALTER TABLE planilla_rasgo AUTO_INCREMENT = 1;
ALTER TABLE rasgo_asistencia AUTO_INCREMENT = 1;
ALTER TABLE rasgo_asistencia_codigo AUTO_INCREMENT = 1;

INSERT IGNORE INTO codigo_conducta (codigo, descripcion, activo) VALUES
('N1', 'Sale del aula sin autorización', TRUE),
('N2', 'No realiza la tarea asignada en clase', TRUE),
('N3', 'No dispone de los materiales necesarios', TRUE),
('N4', 'No presenta las tareas asignadas para la casa', TRUE),
('N5', 'Utiliza vocabulario indebido en clase', TRUE),
('N6', 'Charla mucho en clase', TRUE),
('N7', 'No utiliza el uniforme establecido', TRUE),
('N8', 'Ausente en clase, presente en la Institución', TRUE);

-- ========================================
-- HORAS CÁTEDRA
-- ========================================
INSERT INTO hora_catedra (numero, etiqueta, hora_inicio, hora_fin) VALUES
(1, 'M', '07:00', '07:35'),
(2, 'M', '07:35', '08:10'),
(3, 'M', '08:10', '08:45'),
(4, 'M', '08:45', '09:20'),
(5, 'M', '09:40', '10:15'),
(6, 'M', '10:15', '10:50'),
(7, 'M', '10:50', '11:25'),
(8, 'M', '11:25', '12:00'),
(9, 'T', '13:00', '13:35'),
(10, 'T', '13:35', '14:10'),
(11, 'T', '14:10', '14:45'),
(12, 'T', '14:45', '15:20'),
(13, 'T', '15:40', '16:15'),
(14, 'T', '16:15', '16:50'),
(15, 'T', '16:50', '17:25'),
(16, 'T', '17:25', '18:00');

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
-- Estos cursos son validos unicamente para el año 2025, 2026 y 2027. Para años posteriores se deben cargar dentro del sistema.
-- El programa deberia ser capaz de crear cursos automaticamente para años posteriores, pero por ahora se hace manualmente.
INSERT INTO curso (id, especialidad_id, promocion, seccion) VALUES
-- Construcciones Civiles (3 secciones)
(1, 1, 2026, 'A'),
(2, 1, 2026, 'B'),
(3, 1, 2026, 'C'),
(4, 1, 2027, 'A'),
(5, 1, 2027, 'B'),
(6, 1, 2027, 'C'),
(7, 1, 2028, 'A'),
(8, 1, 2028, 'B'),
(9, 1, 2028, 'C'),

-- Electricidad (2 secciones)
(10, 2, 2026, 'A'),
(11, 2, 2026, 'B'),
(12, 2, 2027, 'A'),
(13, 2, 2027, 'B'),
(14, 2, 2028, 'A'),
(15, 2, 2028, 'B'),

-- Electrónica (3 secciones)
(16, 3, 2026, 'A'),
(17, 3, 2026, 'B'),
(18, 3, 2026, 'C'),
(19, 3, 2027, 'A'),
(20, 3, 2027, 'B'),
(21, 3, 2027, 'C'),
(22, 3, 2028, 'A'),
(23, 3, 2028, 'B'),
(24, 3, 2028, 'C'),

-- Electromecánica (2 secciones)
(25, 4, 2026, 'A'),
(26, 4, 2026, 'B'),
(27, 4, 2027, 'A'),
(28, 4, 2027, 'B'),
(29, 4, 2028, 'A'),
(30, 4, 2028, 'B'),

-- Informática (2 secciones)
(31, 5, 2026, 'A'),
(32, 5, 2026, 'B'),
(33, 5, 2027, 'A'),
(34, 5, 2027, 'B'),
(35, 5, 2028, 'A'),
(36, 5, 2028, 'B'),

-- Mecánica General (2 secciones)
(37, 6, 2026, 'A'),
(38, 6, 2026, 'B'),
(39, 6, 2027, 'A'),
(40, 6, 2027, 'B'),
(41, 6, 2028, 'A'),
(42, 6, 2028, 'B'),

-- Mecánica Automotriz (2 secciones)
(43, 7, 2026, 'A'),
(44, 7, 2026, 'B'),
(45, 7, 2027, 'A'),
(46, 7, 2027, 'B'),
(47, 7, 2028, 'A'),
(48, 7, 2028, 'B'),

-- Química Industrial (3 secciones)
(49, 8, 2026, 'A'),
(50, 8, 2026, 'B'),
(51, 8, 2026, 'C'),
(52, 8, 2027, 'A'),
(53, 8, 2027, 'B'),
(54, 8, 2027, 'C'),
(55, 8, 2028, 'A'),
(56, 8, 2028, 'B'),
(57, 8, 2028, 'C');

INSERT INTO curso_base (id, especialidad_id, nivel, seccion) VALUES
-- Informática
(1, 5, 1, 'A'),
(2, 5, 1, 'B'),
(3, 5, 2, 'A'),
(4, 5, 2, 'B'),
(5, 5, 3, 'A'),
(6, 5, 3, 'B'),

-- Construcciones Civiles (especialidad_id 1)
(7, 1, 1, 'A'),
(8, 1, 1, 'B'),
(9, 1, 1, 'C'),
(10, 1, 2, 'A'),
(11, 1, 2, 'B'),
(12, 1, 2, 'C'),
(13, 1, 3, 'A'),
(14, 1, 3, 'B'),
(15, 1, 3, 'C'),

-- Electricidad (especialidad_id 2)
(16, 2, 1, 'A'),
(17, 2, 1, 'B'),
(18, 2, 2, 'A'),
(19, 2, 2, 'B'),
(20, 2, 3, 'A'),
(21, 2, 3, 'B'),

-- Electrónica (especialidad_id 3)
(22, 3, 1, 'A'),
(23, 3, 1, 'B'),
(24, 3, 1, 'C'),
(25, 3, 2, 'A'),
(26, 3, 2, 'B'),
(27, 3, 2, 'C'),
(28, 3, 3, 'A'),
(29, 3, 3, 'B'),
(30, 3, 3, 'C'),

-- Electromecánica (especialidad_id 4)
(31, 4, 1, 'A'),
(32, 4, 1, 'B'),
(33, 4, 2, 'A'),
(34, 4, 2, 'B'),
(35, 4, 3, 'A'),
(36, 4, 3, 'B'),

-- Mecánica General (especialidad_id 6)
(37, 6, 1, 'A'),
(38, 6, 1, 'B'),
(39, 6, 2, 'A'),
(40, 6, 2, 'B'),
(41, 6, 3, 'A'),
(42, 6, 3, 'B'),

-- Mecánica Automotriz (especialidad_id 7)
(43, 7, 1, 'A'),
(44, 7, 1, 'B'),
(45, 7, 2, 'A'),
(46, 7, 2, 'B'),
(47, 7, 3, 'A'),
(48, 7, 3, 'B'),

-- Química Industrial (especialidad_id 8)
(49, 8, 1, 'A'),
(50, 8, 1, 'B'),
(51, 8, 1, 'C'),
(52, 8, 2, 'A'),
(53, 8, 2, 'B'),
(54, 8, 2, 'C'),
(55, 8, 3, 'A'),
(56, 8, 3, 'B'),
(57, 8, 3, 'C');



-- ========================================
-- USUARIOS BASE
-- ========================================
-- 1 = profesor
-- 2 = evaluador
-- 3 = admin
-- 4 = padre
-- 5 = coordinador pedagógico

INSERT INTO usuario (
    id, nombre, apellido, usuario, contrasenia, ci, telefono, celular, correo,
    google_email, nivel, activity_log_path, especialidad_id
) VALUES
    -- Administrador global
    (1, 'Administrador', 'Global', 'global_admin', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1, null, null, null,
    null, 3, NULL, null),

    -- Administradores de especialidad
    (2, 'Administracion', 'Construcciones Civiles', 'admin_cc', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2, null, null, null,
    null, 3, NULL, 1),
    (3, 'Administracion', 'Electricidad', 'admin_electricidad', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3, null, null, null,
    null, 3, NULL, 2),
    (4, 'Administracion', 'Electronica', 'admin_electronica', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4, null, null, null,
    null, 3, NULL, 3),
    (5, 'Administracion', 'Electromecanica', 'admin_electromecanica', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 5, null, null, null,
    null, 3, NULL, 4),
    (6, 'Administracion', 'Informatica', 'admin_informatica', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 6, null, null, null,
    null, 3, NULL, 5),
    (7, 'Administracion', 'Mecanica General', 'admin_mecanica_general', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 7, null, null, null,
    null, 3, NULL, 6),
    (8, 'Administracion', 'Mecanica Automotriz', 'admin_mecanica_automotriz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 8, null, null, null,
    null, 3, NULL, 7),
    (9, 'Administracion', 'Quimica Industrial', 'admin_quimica_industrial', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 9, null, null, null,
    null, 3, NULL, 8),

    -- Profesores
    (10, 'Juan Nicolas', 'Acosta', 'juan.acosta', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4959582, '0992-398.653', null, 'ja8824317@gmail.com', null, 1, NULL, null),
    (11, 'Abel', 'Admen Oliveira', 'abel.admen', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2307477, '0992-315.360', null, 'abeloliveira14@hotmail.com', null, 1, NULL, null),
    (12, 'Romy Luz', 'Aguilera de Mongelos', 'romy.aguilera', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 748826, '0986-186.905', null, 'luzaguilera9696@gmail.com', null, 1, NULL, null),
    (13, 'Abner Constantino', 'Alcaraz Rojas', 'abner.alcaraz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 801245, '0961-410.579', null, 'abnercons@yahoo.es', null, 1, NULL, null),
    (14, 'Jorge Anibal', 'Alfonzo Vera', 'jorge.alfonzo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3538324, null, null, null, null, 1, NULL, null),
    (15, 'Liz Maria Gloria', 'Alfonzo Vera', 'liz.alfonzo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3538327, '0981-606.511', null, 'lizalfonzolui@gmail.com', null, 1, NULL, null),
    (16, 'Blanca Rosa', 'Almirón de Notario', 'blanca.almiron', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2870819, '0972-151.260', null, 'blancalmiron78@gmail.com', null, 1, NULL, null),
    (17, 'Susana Raquel', 'Alvarenga Cañete', 'susana.alvarenga', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2846055, '0994-463.222', null, 'susyalvarenga@gmail.com', null, 1, NULL, null),
    (18, 'Ana Maria', 'Andino Ramos', 'ana.andino', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 814644, '0982-978.848', null, 'anaandino1936@gmail.com', null, 1, NULL, null),
    (19, 'Edgar Sebastian', 'Aquino Ledezma', 'edgar.aquino', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4739025, '0991-591.087', null, 'edgarfilospoe@gmail.com', null, 1, NULL, null),
    (20, 'Jorge Alberto', 'Aquino Peralta', 'jorge.aquino', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2928412, '0982-707.177', null, 'j_aap@hotmail.com', null, 1, NULL, null),
    (21, 'Alba Concepción', 'Arrúa Sosa', 'alba.arrua', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3721761, '0981-432.639', null, 'albacarrua@gmail.com', null, 1, NULL, null),
    (22, 'Nathalia Soledad', 'Báez Pereira', 'nathalia.baez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3518186, '0961-483.777', null, 'nathaliabaez.nb@gmail.com', null, 1, NULL, null),
    (23, 'Alicia Celeste', 'Barrios de Báez', 'alicia.barrios', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2332755, '0981-382.161', null, 'aliciacelestebarrios@gmail.com', null, 1, NULL, null),
    (24, 'Irma', 'Benítez Fernández', 'irma.benitez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 503029, null, null, null, null, 1, NULL, null),
    (25, 'Victor Sebastián', 'Benítez Irala', 'victor.benitez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1348684, '0981-959.443', null, 'victorbirala@gmail.com', null, 1, NULL, null),
    (26, 'Norberto Alejandro', 'Benítez López', 'norberto.benitez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2152502, '0982-695.925', null, 'norbertobenitez1@gmail.com', null, 1, NULL, null),
    (27, 'Leticia Ester', 'Bogado Fariña', 'leticia.bogado', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 6570457, '0985-423.355', null, 'leticiabogado84@gmail.com', null, 1, NULL, null),
    (28, 'Víctor Hugo', 'Bogarin Martinez', 'victor.bogarin', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1241578, '0981-943.355', null, 'consultoravhbm@gmail.com', null, 1, NULL, null),
    (29, 'Claudia Marina', 'Burgos de Velázquez', 'claudia.burgos', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2069958, '0981-931.897', null, 'cmbv28@gmail.com', null, 1, NULL, null),
    (30, 'Robert Brigido', 'Caballero Vera', 'robert.caballero', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2412815, '0981-861.945', null, null, null, 1, NULL, null),
    (31, 'Arnaldo José', 'Cabrera Díaz', 'arnaldo.cabrera', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1291777, '0981-228.069', null, 'aldocabrera72@gmail.com', null, 1, NULL, null),
    (32, 'Lourdes Natalia', 'Caceres Alfonzo', 'lourdes.caceres', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3654427, '0981-263.318', null, 'lou.caceres263@gmail.com', null, 1, NULL, null),
    (33, 'Alcira Carolina', 'Cáceres de Ortellado', 'alcira.caceres', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2291288, '0982-669.375', null, 'caceresalcira06@gmail.com', null, 1, NULL, null),
    (34, 'Máximo Tito Simón', 'Cáceres Gini', 'maximo.caceres', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4325994, '0984-821.807', null, 'matisicagi@gmail.com', null, 1, NULL, null),
    (35, 'Irma Graciela', 'Cardozo', 'irma.cardozo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1295107, '0971-512.385', null, 'irma1295107@gmail.com', null, 1, NULL, null),
    (36, 'Tango Ottmar', 'Carrero Romero', 'tango.carrero', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1825471, '0972-910.073', null, 'tydestudio@hotmail.com', null, 1, NULL, null),
    (37, 'Gerardo Omar', 'Centurión Gómez', 'gerardo.centurion', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4337053, '0982-772.612', null, 'yerardcentu@gmail.com', null, 1, NULL, null),
    (38, 'Victor Amadeo', 'Cerquetti Cristaldo', 'victor.cerquetti', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 451439, '0981-429.826', null, 'ingcerquetti@hotmail.com', null, 1, NULL, null),
    (39, 'Luis Carlos', 'Chávez Zalazar', 'luis.chavez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1798067, '0961-985.917', null, 'luiscarloschavezzalazar@gmail.com', null, 1, NULL, null),
    (40, 'Luis Fernando', 'Codas Baade', 'luis.codas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1138010, '0981-275.626', null, 'luiscodas@gmail.com', null, 1, NULL, null),
    (41, 'Crispin', 'Coeffier Villalba', 'crispin.coeffier', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 694719, '0976-404.336', null, 'ccoeffierv@ing.una.py', null, 1, NULL, null),
    (42, 'Osvaldo Ramón', 'Cruz', 'osvaldo.cruz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 984558, '0992-723.302', null, 'ocruzinsaurralde@gmail.com', null, 1, NULL, null),
    (43, 'Cristian Humberto', 'Delgado Pereira', 'cristian.delgado', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1877110, '0986-375.505', null, 'cristianhdelgado@gmail.com', null, 1, NULL, null),
    (44, 'Cynthia Noelia', 'Díaz López', 'cynthia.diaz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4295031, '0981-560.137', null, 'cynthiadiaz560@gmail.com', null, 1, NULL, null),
    (45, 'Liz Mariza', 'Duarte de Delgado', 'liz.duarte', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1325786, '0981-995.663', null, 'lizmarizaduartededelgado@gmail.com', null, 1, NULL, null),
    (46, 'Helen Lilian', 'Duarte Ortiz', 'helen.duarte', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2868076, '0981-271.618', null, 'hlnd31@gmail.com', null, 1, NULL, null),
    (47, 'Jorge Guillermo', 'Echague Ramirez', 'jorge.echague', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4318501, '0961-930.640', null, 'jorgegechague@gmail.com', null, 1, NULL, null),
    (48, 'Maria del Rocio', 'Egusquiza de Schwarz', 'maria.egusquiza', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1349146, '0981-701.958', null, 'profe.rocioegusquiza@gmail.com', null, 1, NULL, null),
    (49, 'Lidubina', 'Escobar Garcete', 'lidubina.escobar', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2888556, '0981-602.173', null, 'lidueg14@gmail.com', null, 1, NULL, null),
    (50, 'Fernando Javier', 'Espinoza Correa', 'fernando.espinoza', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4193955, null, null, 'ferpele93@gmail.com', null, 1, NULL, null),
    (51, 'Ruth Ninfa', 'Estigarribia González', 'ruth.estigarribia', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1297160, '0981-628.724', null, 'rulitosctn@gmail.com', null, 1, NULL, null),
    (52, 'Nemesio', 'Fernandez Ferreira', 'nemesio.fernandez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1044470, '0981-564.528', null, 'nemesioff@hotmail.com', null, 1, NULL, null),
    (53, 'Ana Luciana', 'Fernández de Gómez', 'ana.fernandez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1711121, '0981-803.355', null, 'anafernandez552@gmail.com', null, 1, NULL, null),
    (54, 'Igor Alejandro', 'Fernández Ozuna', 'igor.fernandez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 6719007, '0981-935.535', null, 'ozunaigor@gmail.com', null, 1, NULL, null),
    (55, 'Ruth Johana', 'Ferrarino Chaparro', 'ruth.ferrarino', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4994558, '0972-460.362', null, 'joha.ferrarino@gmail.com', null, 1, NULL, null),
    (56, 'Richar', 'Ferreira Valenzuela', 'richar.ferreira', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1319513, '0981-288.329', null, 'richaralberto1971@gmail.com', null, 1, NULL, null),
    (57, 'Carmen Lilian', 'Franco', 'carmen.franco', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2252820, '0972-604.594', null, 'lylyfranco3079@gmail.com', null, 1, NULL, null),
    (58, 'Lourdes', 'Galeano de Viera', 'lourdes.galeano', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1788967, '0981-501.465', null, 'lourdesgaleano7498@gmail.com', null, 1, NULL, null),
    (59, 'Mirian Raquel', 'Galeano Zavala', 'mirian.galeano', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4089145, '0983-687.191', null, 'miriangaleano295@gmail.com', null, 1, NULL, null),
    (60, 'Ana Gabriela', 'Gallardo Alderete', 'ana.gallardo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3994217, '0981-478.903', null, 'ana_gabriela12@yahoo.com', null, 1, NULL, null),
    (61, 'Ismael Inocente', 'Garay Gonzalez', 'ismael.garay', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3455447, '0982-464.550', null, 'ismagar03@gmail.com', null, 1, NULL, null),
    (62, 'Pedro David', 'Garcete Gauto', 'pedro.garcete', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2110348, '0982-781.424', null, 'pedruspy@gmail.com', null, 1, NULL, null),
    (63, 'Anibal', 'Genes Boy', 'anibal.genes', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3471698, '0983-563.024', null, 'angeboy.986@gmail.com', null, 1, NULL, null),
    (64, 'Juan Angel', 'González Aguilera', 'juan.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 805420, '0991-308.855', null, 'juangonzalezctn@gmail.com', null, 1, NULL, null),
    (65, 'Bernarda Maria', 'González de Fleitas', 'bernarda.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1415391, '0981-253.809', null, 'bernigoflei@hotmail.com', null, 1, NULL, null),
    (66, 'Federico', 'Gónzalez Esteche', 'federico.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3655989, '0982-530.585', null, 'fgonza1985@gmail.com', null, 1, NULL, null),
    (67, 'Graciela Elizabeth', 'González Gimenez', 'graciela.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3480729, '0981-871.999', null, '3480729@mec.edu.py', null, 1, NULL, null),
    (68, 'Raquel', 'González Quintana', 'raquel.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1217787, '0984-908.406', null, 'gonzalezqraquel@gmail.com', null, 1, NULL, null),
    (69, 'Francisco Andres', 'Gónzalez Zaracho', 'francisco.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4793465, '0991-905.971', null, 'andresgonzalezdocente@gmail.com', null, 1, NULL, null),
    (70, 'Fátima Rocío', 'Grillón de Medina', 'fatima.grillon', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2281694, '0976-990.991', null, 'matematicas@gmail.com', null, 1, NULL, null),
    (71, 'Felix Fernando', 'Huerta Etcheverry', 'felix.huerta', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 939001, '0981-212.203', null, 'felix.fhuerta@gmail.com', null, 1, NULL, null),
    (72, 'Zulma Asunción', 'Ibarra Rodríguez', 'zulma.ibarra', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 757978, '0981-418.386', null, 'zulmaibarra@yahoo.es', null, 1, NULL, null),
    (73, 'Oscar', 'Ibarrola Diaz', 'oscar.ibarrola', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 621312, '0984-875.427', null, 'ibaoscar@hotmail.com', null, 1, NULL, null),
    (74, 'Emilce Beatriz', 'Jara Bogado', 'emilce.jara', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2045853, '0971-870.098', null, 'emilcejara14@gmail.com', null, 1, NULL, null),
    (75, 'Hernán', 'Jara Olmedo', 'hernan.jara', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1059166, '0976-990.995', null, 'hernanjara503@gmail.com', null, 1, NULL, null),
    (76, 'Rolando Daniel', 'Lenguaza Sosa', 'rolando.lenguaza', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1200350, '0971-292.398', null, 'rolandolenguaza@gmail.com', null, 1, NULL, null),
    (77, 'Javier Adolfo', 'López Benítez', 'javier.lopez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1638538, '0993-473.589', null, 'jbkuri007@gmail.com', null, 1, NULL, null),
    (78, 'Alice Amelia', 'López de Fretes', 'alice.lopez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1418878, '0992-288.682', null, 'alicelopez471@gmail.com', null, 1, NULL, null),
    (79, 'Humberto Manuel', 'López González', 'humberto.lopez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4203308, '0991-940.419', null, 'humbermlopez97@gmail.com', null, 1, NULL, null),
    (80, 'Maria Mercedes', 'López Lezcano', 'maria.lopez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4165186, '0994-342.027', null, 'mmercedesllezcano@gmail.com', null, 1, NULL, null),
    (81, 'Graciela Noemí', 'López Molinas', 'graciela.lopez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1722056, '0961-499.459', null, 'graceamarilla73@gmail.com', null, 1, NULL, null),
    (82, 'Graciela', 'Maidana Pinto', 'graciela.maidana', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1077876, '0981-198.202', null, 'grmaidanapinto@gmail.com', null, 1, NULL, null),
    (83, 'Alicia', 'Martinez López', 'alicia.martinez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2970412, '0982-492.514', null, 'aliciamdoctorado@gmail.com', null, 1, NULL, null),
    (84, 'Cristhian Agustin', 'Martinez Paredes', 'cristhian.martinez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 5928736, '0971-569.722', null, 'mart.par27@gmail.com', null, 1, NULL, null),
    (85, 'Cirilo David', 'Medina Cáceres', 'cirilo.medina', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2188244, '0991-975.299', null, 'cirilodmedina07@gmail.com', null, 1, NULL, null),
    (86, 'Juan Antonio', 'Medina Chávez', 'juan.medina', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1988072, null, null, null, null, 1, NULL, null),
    (87, 'Maria Adriana', 'Mequer Pfefferkorn', 'maria.mequer', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4261227, '0982-261.455', null, 'adrimequer@gmail.com', null, 1, NULL, null),
    (88, 'Milner Gabriel', 'Mercado Vera', 'milner.mercado', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1955257, '0971-290.023', null, 'milnermer@gmail.com', null, 1, NULL, null),
    (89, 'Maria Soledad', 'Mereles Barrios', 'maria.mereles', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1711193, '0972-437.476', null, 'marbarrios60@gmail.com', null, 1, NULL, null),
    (90, 'Marta Guadalupe', 'Mojoli Apthorpe', 'marta.mojoli', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1434365, '0971-546.434', null, '1434365@mec.edu.py', null, 1, NULL, null),
    (91, 'Francisco Javier', 'Molinas Ferreira', 'francisco.molinas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4329959, '0982-912.419', null, 'fran_molina@hotmail.com', null, 1, NULL, null),
    (92, 'Mirian Airini', 'Montanía Gónzalez', 'mirian.montania', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4311520, '0995-669.625', null, 'airinimirian86gonzalez@gmail.com', null, 1, NULL, null),
    (93, 'Liz Marina', 'Montiel de Bogarin', 'liz.montiel', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2196944, '0985-870.048', null, 'lizmontie@yahoo.com', null, 1, NULL, null),
    (94, 'Justo Enrique', 'Mora Caballero', 'justo.mora', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3189593, '0982-907.501', null, 'enriquemoracab@hotmail.com', null, 1, NULL, null),
    (95, 'Iván Gerardo', 'Núñez Genes', 'ivan.nunez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3578998, '0981-204.951', null, 'i.genu@mec.edu.py', null, 1, NULL, null),
    (96, 'Zully Antonia', 'Núñez Ramírez', 'zully.nunez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1794168, '0961-692.695', null, 'zullyanunez16@gmail.com', null, 1, NULL, null),
    (97, 'Benicio', 'Núñez Romero', 'benicio.nunez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3176523, '0972-448.450', null, 'benicionuez@gmail.com', null, 1, NULL, null),
    (98, 'Hugo de Jesús', 'Olmedo Chávez', 'hugo.olmedo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2817429, '0983-266.426', null, 'hugoolmedo20.j@gmail.com', null, 1, NULL, null),
    (99, 'Leticia Soledad', 'Olmedo Melgarejo', 'leticia.olmedo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3446998, '0981-841.646', null, 'letisolmedo_84@gmail.com', null, 1, NULL, null),
    (100, 'Aracely Macarena', 'Ortiz Ramirez', 'aracely.ortiz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 5132182, '0972-750.800', null, '5132182@mec.edu.py', null, 1, NULL, null),
    (101, 'José Edgar', 'Orué Alonso', 'jose.orue', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 979744, '0984-254.513', null, 'jose.oruealonso@gmail.com', null, 1, NULL, null),
    (102, 'Gerardo Raúl', 'Ovelar Fernández', 'gerardo.ovelar', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4614218, '0991-425.481', null, 'gerardoraul@outlook.es', null, 1, NULL, null),
    (103, 'Andrea Romina', 'Perez Benitez', 'andrea.perez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3420813, '0985-895.416', null, 'andyrominaperez@gmail.com', null, 1, NULL, null),
    (104, 'José Luis', 'Pino Meza', 'jose.pino', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3385220, '0981-460.604', null, 'joseluispinomeza@gmail.com', null, 1, NULL, null),
    (105, 'Zonia Inocencia', 'Ramirez de Torres', 'zonia.ramirez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1427160, '0971-287.942', null, 'zramirezrolon@gmail.com', null, 1, NULL, null),
    (106, 'Gustavo Adolfo', 'Ramírez Fernández', 'gustavo.ramirez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1393836, '0981-347.632', null, 'rgustavoadolfo128@gmail.com', null, 1, NULL, null),
    (107, 'Christian Javier', 'Ramos Santacruz', 'christian.ramos', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 5132801, '0961-244.205', null, 'crija88@gmail.com', null, 1, NULL, null),
    (108, 'Daniela Consuelo', 'Ratzlaff Galeano', 'daniela.ratzlaff', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1858451, '0961-910.559', null, 'dratzlaffg@gmail.com', null, 1, NULL, null),
    (109, 'Jorge Alberto', 'Recalde Espinoza', 'jorge.recalde', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1203846, '0983-308.740', null, 'jrecaldectn@gmail.com', null, 1, NULL, null),
    (110, 'Daniel', 'Rios Morales', 'daniel.rios', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1083166, '0971-133.393', null, 'dani.rios.m41@gmail.com', null, 1, NULL, null),
    (111, 'Laura Raquel', 'Rivas de López', 'laura.rivas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1587228, '0985-961.172', null, 'laurarivasdl@gmail.com', null, 1, NULL, null),
    (112, 'Claudia Irene', 'Riveros Weiler', 'claudia.riveros', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3528678, '0971-962.297', null, 'claudiariverosweiler@hotmail.com', null, 1, NULL, null),
    (113, 'Karill Aracelli', 'Rojas Díaz', 'karill.rojas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4562211, '0982-815.763', null, 'karillrojas85@gmail.com', null, 1, NULL, null),
    (114, 'Celso Ramón', 'Rojas Jara', 'celso.rojas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3669793, '0981-488.922', null, 'celsorojas.edu@gmail.com', null, 1, NULL, null),
    (115, 'Cesar Andres', 'Rojas Morel', 'cesar.rojas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4635720, '0981-357-378', null, 'cesarandres171096@gmail.com', null, 1, NULL, null),
    (116, 'María de Lurdes', 'Román de Rivaldi', 'maria.roman', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1225464, '0981-468.376', null, 'lurdesroman@gmail.com', null, 1, NULL, null),
    (117, 'Ruth Marlene', 'Román Gómez', 'ruth.roman', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2375989, '0981-505.785', null, 'ruth.roman.gomez@gmail.com', null, 1, NULL, null),
    (118, 'Angel José', 'Ruíz Diaz Antunez', 'angel.ruiz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3610653, '0983-432.751', null, 'angeljoseruiz1403@gmail.com', null, 1, NULL, null),
    (119, 'Guillermo', 'Salcedo', 'guillermo.salcedo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4797255, '0961-847.863', null, 'guillesal2010@gmail.com', null, 1, NULL, null),
    (120, 'Nidia Beatriz', 'Samudio de Torres', 'nidia.samudio', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3439931, '0984-261.157', null, 'nidiasamudio20@gmail.com', null, 1, NULL, null),
    (121, 'Silvio Gustavo', 'Sanchéz Montiel', 'silvio.sanchez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3685769, '0971-207.707', null, 'gustsanzlic@gmail.com', null, 1, NULL, null),
    (122, 'Hilda Ramona', 'Sánchez', 'hilda.sanchez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3701199, '0986-842.369', null, 'hilrasanchez100@gmail.com', null, 1, NULL, null),
    (123, 'Jean Michel', 'Sekatcheff Snead', 'jean.sekatcheff', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 577903, null, null, null, null, 1, NULL, null),
    (124, 'Nancy Catalina', 'Sosa de Franco', 'nancy.sosa', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3201310, '0991-853.663', null, 'nancy12sosa@gmail.com', null, 1, NULL, null),
    (125, 'Jorge Hilario', 'Szwako Montero', 'jorge.szwako', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 715659, '0971-964.090', null, '715659@mec.edu.py', null, 1, NULL, null),
    (126, 'Esperanza Lucia', 'Torales de Aquino', 'esperanza.torales', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2375521, '0981-251.884', null, 'toralesluciae@gmail.com', null, 1, NULL, null),
    (127, 'Genoveva De Jesús', 'Valdéz González', 'genoveva.valdez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 558994, '0981-411.531', null, 'gedejeva517@gmail.com', null, 1, NULL, null),
    (128, 'Maria Luz', 'Valiente de Angulo', 'maria.valiente', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1275005, '0971-944.194', null, 'luzvaliente2104@gmail.com', null, 1, NULL, null),
    (129, 'Gladys Carmen', 'Vallejos Ortiz', 'gladys.vallejos', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 758565, '0985-449.946', null, 'gladyscarmen1405@gmail.com', null, 1, NULL, null),
    (130, 'Mónica Elizabeth', 'Vera Vega', 'monica.vera', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1407630, '0961-534.224', null, 'moni_vera@hotmail.com', null, 1, NULL, null),
    (131, 'Oscar Adolfo', 'Villalba Ortiz', 'oscar.villalba', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 901079, '0983-346.840', null, 'oscarvillalbaortiz@hotmail.com', null, 1, NULL, null),
    (132, 'Oscar Daniel', 'Villalba Riveros', 'oscar.villalba2', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4912277, null, null, null, null, 1, NULL, null),
    (133, 'Oscar Ramón', 'Villasanti Cañete', 'oscar.villasanti', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2293109, '0971-119.798', null, 'os.villasanti@hotmail.com', null, 1, NULL, null),
    (134, 'Paolo Giovanni', 'Zucchini Cuevas', 'paolo.zucchini', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4337468, '0982-716.105', null, '4337468@mec.edu.py', null, 1, NULL, null),
    (145, 'Oscar Atilio', 'Azuaga Palacio', 'oscar.azuaga', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 607299, '0982-653.178', null, 'oscar.azuaga13@gmail.com', null, 1, NULL, null),

    -- Evaluadores
    (135, 'Evaluador', '1', 'evaluador1', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 11, null, null, null, null, 2, NULL, null),
    (136, 'Evaluador', '2', 'evaluador2', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 12, null, null, null, null, 2, NULL, null),
    (137, 'Evaluador', '3', 'evaluador3', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 13, null, null, null, null, 2, NULL, null),
    (138, 'Evaluador', '4', 'evaluador4', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 14, null, null, null, null, 2, NULL, null),
    (139, 'Evaluador', '5', 'evaluador5', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 15, null, null, null, null, 2, NULL, null),

    -- Coordinadores Pedagógicos
    (140, 'CoordinadorPedagogico', '1', 'cpdg1', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 16, null, null, null, null, 5, NULL, null),
    (141, 'CoordinadorPedagogico', '2', 'cpdg2', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 17, null, null, null, null, 5, NULL, null),
    (142, 'CoordinadorPedagogico', '3', 'cpdg3', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 18, null, null, null, null, 5, NULL, null),
    (143, 'CoordinadorPedagogico', '4', 'cpdg4', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 19, null, null, null, null, 5, NULL, null),
    (144, 'CoordinadorPedagogico', '5', 'cpdg5', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 20, null, null, null, null, 5, NULL, null)

    -- Padres (deben ser creados a traves del flujo de autenticacion de Google, por lo que no se insertan aca)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    apellido = VALUES(apellido),
    usuario = VALUES(usuario),
    contrasenia = VALUES(contrasenia),
    ci = VALUES(ci),
    telefono = VALUES(telefono),
    celular = VALUES(celular),
    correo = VALUES(correo),
    google_email = VALUES(google_email),
    nivel = VALUES(nivel),
    activity_log_path = VALUES(activity_log_path),
    especialidad_id = VALUES(especialidad_id);

UPDATE usuario
SET activity_log_path = CONCAT('usuario-', id, '.txt')
WHERE activity_log_path IS NOT NULL
  AND activity_log_path <> CONCAT('usuario-', id, '.txt');

-- Todas las contraseñas de los usuarios son "password" y estan encriptadas con BCrypt.

-- =====================================================================
-- MATERIAS COMUNES
-- =====================================================================

INSERT INTO materia (id, nombre, categoria) VALUES
(1, 'Antropología', 'comun'),
(2, 'Ciencias', 'comun'),
(3, 'Economía y Gestión', 'comun'),
(4, 'Educación Física', 'comun'),
(5, 'Educación Vial', 'comun'),
(6, 'Formacion Ética y Ciudadana', 'comun'),
(7, 'Física', 'comun'),
(8, 'Guaraní', 'comun'),
(9, 'Historia', 'comun'),
(10, 'Inglés', 'comun'),
(11, 'Matemática Común', 'comun'),
(12, 'Orientación', 'comun'),
(13, 'Psicología', 'comun'),
(14, 'Química', 'comun'),
(15, 'Administración Financiera', 'comun'),
(16, 'Literatura', 'comun'),
(17, 'Informática', 'comun'),
(18, 'Instalaciones Industriales', 'comun'),
(19, 'Taller de Mecánica', 'comun');

INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, e.id
FROM materia m
CROSS JOIN especialidad e
WHERE m.id IN (1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 16, 3)
  AND e.nombre IN (
    'Construcciones Civiles',
    'Mecánica General',
    'Electricidad',
    'Química Industrial',
    'Electromecánica',
    'Informática',
    'Electrónica',
    'Mecánica Automotriz'
  );

-- Química (comun, id 14) no se dicta como tal dentro de la especialidad
-- Química Industrial (ahí las materias son específicas: Química General,
-- Química Analítica, etc.) -> se linkea a las otras 7
INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT 14, e.id
FROM especialidad e
WHERE e.nombre IN (
  'Construcciones Civiles',
  'Mecánica General',
  'Electricidad',
  'Electromecánica',
  'Informática',
  'Electrónica',
  'Mecánica Automotriz'
);

-- Administración Financiera (comun, id 15) solo aparece en Informática
INSERT INTO materia_especialidad (materia_id, especialidad_id)
VALUES (15, (SELECT id FROM especialidad WHERE nombre = 'Informática'));

INSERT INTO materia_especialidad (materia_id, especialidad_id) VALUES
(17, (SELECT id FROM especialidad WHERE nombre = 'Electricidad')),
(17, (SELECT id FROM especialidad WHERE nombre = 'Electromecánica')),
(17, (SELECT id FROM especialidad WHERE nombre = 'Electrónica')),
(18, (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),
(18, (SELECT id FROM especialidad WHERE nombre = 'Electromecánica')),
(18, (SELECT id FROM especialidad WHERE nombre = 'Electricidad')),
(19, (SELECT id FROM especialidad WHERE nombre = 'Electricidad')),
(19, (SELECT id FROM especialidad WHERE nombre = 'Electromecánica')),
(19, (SELECT id FROM especialidad WHERE nombre = 'Mecánica General'));


-- =====================================================================
-- MATERIAS ESPECÍFICAS
-- =====================================================================

-- Informática (41-55)
INSERT INTO materia (id, nombre, categoria) VALUES
(41, 'Algorítmica', 'especifico'),
(42, 'Laboratorio Android', 'especifico'),
(43, 'Laboratorio Java', 'especifico'),
(44, 'Laboratorio Linux', 'especifico'),
(45, 'Laboratorio Python', 'especifico'),
(46, 'Laboratorio SQL', 'especifico'),
(47, 'Laboratorio Web', 'especifico'),
(48, 'Matemática Aplicada', 'especifico'),
(49, 'Plan de Lectura', 'especifico'),
(50, 'Laboratorio Redes', 'especifico'),
(51, 'Seguridad en Riesgos Eléctricos', 'especifico'),
(52, 'Dibujo Técnico', 'especifico'),
(53, 'Info General', 'especifico'),
(54, 'Laboratorio Hardware', 'especifico');

-- Construcciones Civiles (61-80)
INSERT INTO materia (id, nombre, categoria) VALUES
(61, 'Proyecto y Dibujo', 'especifico'),
(62, 'Laboratorio (Construcciones)', 'especifico'),
(63, 'Tecnología (Construcciones)', 'especifico'),
(64, 'Técnicas Instrumentales', 'especifico'),
(65, 'Taller (Construcciones)', 'especifico'),
(66, 'Resistencia de Materiales', 'especifico'),
(67, 'Topografía', 'especifico'),
(68, 'AutoCAD', 'especifico'),
(69, 'Proyecto Educativo (Construcciones)', 'especifico');

-- Mecánica General (81-100)
INSERT INTO materia (id, nombre, categoria) VALUES
(81, 'Laboratorio de Mecánica', 'especifico'),
(82, 'Mecánica Aplicada', 'especifico'),
(83, 'Electrotecnia (Mecánica General)', 'especifico'),
(84, 'Tecnología Mecánica', 'especifico'),
(85, 'Dibujo Técnico (Mecánica General)', 'especifico'),
(86, 'Diseño y Proyecto', 'especifico'),
(87, 'Máquinas CNC', 'especifico');

-- Electricidad (101-120)
INSERT INTO materia (id, nombre, categoria) VALUES
(101, 'Taller (Electricidad)', 'especifico'),
(102, 'Diseño (Electricidad)', 'especifico'),
(103, 'Dibujo Técnico (Electricidad)', 'especifico'),
(104, 'Laboratorio de Electrotecnia', 'especifico'),
(105, 'Electrónica (Electricidad)', 'especifico'),
(106, 'Proyecto (Electricidad)', 'especifico'),
(107, 'Optativa (Electricidad)', 'especifico'),
(108, 'Laboratorio (Electricidad)', 'especifico');

-- Química Industrial (121-140)
INSERT INTO materia (id, nombre, categoria) VALUES
(121, 'Química General', 'especifico'),
(122, 'Química Práctica', 'especifico'),
(123, 'Química Analítica', 'especifico'),
(124, 'Fisicoquímica', 'especifico'),
(125, 'Operaciones Unitarias', 'especifico'),
(126, 'Análisis Instrumental', 'especifico'),
(127, 'Recursos Naturales', 'especifico'),
(128, 'Seguridad e Higiene (Química)', 'especifico'),
(129, 'Taller (Química)', 'especifico'),
(130, 'Análisis Industrial', 'especifico'),
(131, 'Microbiología', 'especifico'),
(132, 'Tecnología y Análisis de Alimentos', 'especifico'),
(133, 'Energía', 'especifico'),
(134, 'Proyecto Industrial', 'especifico'),
(135, 'Plan Optativo', 'especifico'),
(136, 'Proyecto Educativo (Química)', 'especifico'),
(137, 'Tecnología (Química)', 'especifico'),
(138, 'Dibujo Técnico (Química Industrial)', 'especifico');

-- Electromecánica (141-160)
INSERT INTO materia (id, nombre, categoria) VALUES
(141, 'Refrigeración', 'especifico'),
(142, 'Neumática e Hidráulica', 'especifico'),
(143, 'PLC', 'especifico'),
(144, 'Diseño y Mantenimiento Industrial', 'especifico'),
(145, 'Electrotecnia (Electromecánica)', 'especifico'),
(146, 'Electrónica (Electromecánica)', 'especifico'),
(147, 'Dibujo Técnico (Electromecánica)', 'especifico');

-- Electrónica (161-180)
INSERT INTO materia (id, nombre, categoria) VALUES
(161, 'Electrónica Analógica', 'especifico'),
(162, 'Electrónica Digital', 'especifico'),
(163, 'Laboratorio de Electrónica', 'especifico'),
(164, 'Electrotecnia (Electrónica)', 'especifico'),
(165, 'Elementos', 'especifico'),
(166, 'Electrónica Industrial', 'especifico'),
(167, 'Dibujo Técnico (Electrónica)', 'especifico'),
(168, 'Seguridad e Higiene (Electrónica)', 'especifico'),
(169, 'Optativa (Electrónica)', 'especifico'),
(170, 'Proyecto (Electrónica)', 'especifico');

-- Mecánica Automotriz (181-200)
INSERT INTO materia (id, nombre, categoria) VALUES
(181, 'Tecnología Automotriz', 'especifico'),
(182, 'Electricidad y Electrónica del Automóvil', 'especifico'),
(183, 'Mecánica Aplicada (Automotriz)', 'especifico'),
(184, 'Laboratorio Diesel', 'especifico'),
(185, 'Taller Automotriz', 'especifico');


-- =====================================================================
-- VÍNCULOS ESPECÍFICA -> ESPECIALIDAD
-- =====================================================================

INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, (SELECT id FROM especialidad WHERE nombre = 'Informática')
FROM materia m WHERE m.id BETWEEN 41 AND 54;

INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')
FROM materia m WHERE m.id BETWEEN 61 AND 69;

INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, (SELECT id FROM especialidad WHERE nombre = 'Mecánica General')
FROM materia m WHERE m.id BETWEEN 81 AND 87;

INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, (SELECT id FROM especialidad WHERE nombre = 'Electricidad')
FROM materia m WHERE m.id BETWEEN 101 AND 108;

INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, (SELECT id FROM especialidad WHERE nombre = 'Química Industrial')
FROM materia m WHERE m.id BETWEEN 121 AND 138;

INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, (SELECT id FROM especialidad WHERE nombre = 'Electromecánica')
FROM materia m WHERE m.id BETWEEN 141 AND 147;

INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, (SELECT id FROM especialidad WHERE nombre = 'Electrónica')
FROM materia m WHERE m.id BETWEEN 161 AND 170;

INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, (SELECT id FROM especialidad WHERE nombre = 'Mecánica Automotriz')
FROM materia m WHERE m.id BETWEEN 181 AND 185;

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
-- Los alumnos corresponden solamente a informatica
-- los demas deben ser cargados luego
-- ¡¡TODOS LOS ALUMNOS CORRESPONDEN AL AÑO 2026!!

-- Informática
    -- 3º A (curso_id = 31)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('PAZ FIORELLA', 'ACUÑA RODRIGUEZ', 31, 6552138),
    ('GABRIELA ELIZABETH', 'ALEGRE ORTIZ', 31, 6520371),
    ('CESAR EZEQUIEL', 'AMARILLA ETTIENE', 31, 7011624),
    ('FERNANDO JOSE', 'BARRETO ROCHE', 31, 6271898),
    ('MARIA CECILIA', 'BENITEZ BARRIOS', 31, 7350265),
    ('SOFIA ESMERALDA', 'BENITEZ MARTINEZ', 31, 7290536),
    ('VALERIA ALEJANDRA', 'CACERES ACHUCARRO', 31, 7536039),
    ('CARLOS ANTONIO', 'CANDIA ROMERO', 31, 6895905),
    ('JONAS ALEXANDER', 'CUBILLA MORINIGO', 31, 7979695),
    ('ALICE GISSELLE', 'DIAZ AMARILLA', 31, 6274837),
    ('KEVIN MATIAS', 'DURE AQUINO', 31, 6711232),
    ('THIAGO DAVID', 'ESTIGARRIBIA DELGADILLO', 31, 6911572),
    ('GLORIA MILENA', 'FARIÑA NUÑEZ', 31, 6363114),
    ('LUCIO ALESSANDRO', 'GAMARRA AGUAYO', 31, 6216256),
    ('LUZ NAHIARA', 'GAYOZO AVALOS', 31, 6218519),
    ('THIAGO ALEXANDER', 'LEON CORONEL', 31, 6168091),
    ('LUCAS ABDIEL', 'MARTINEZ GONZALEZ', 31, 6219481),
    ('CHRISTOPHER IVAN', 'MARTINEZ INSFRAN', 31, 7449854),
    ('MARCOS DANIEL', 'MOLINAS LEON', 31, 6820120),
    ('JOSHUA FABRIZIO', 'MONGELOS CAMACHO', 31, 6656584),
    ('MIANE MARIA VERONICA', 'NOGUERA AVILA', 31, 6298042),
    ('ALAN ENRIQUE DAMIAN', 'OJEDA OLIVER', 31, 6840108),
    ('ALEXANDER AGUSTIN', 'OLMEDO RODRIGUEZ', 31, 6658507),
    ('SAMUEL JESUS', 'SCHMIDT SILVEIRA', 31, 6595852),
    ('JOSE FEDERICO', 'SOLER VAZQUEZ', 31, 7309281),
    ('MIKAHELA', 'SUAREZ ARZA', 31, 6711101),
    ('LEONARDO', 'VALINOTTI  PAREDES', 31, 6761746),
    ('FACUNDO BENJAMIN', 'VERA SALINAS', 31, 7007217);

    -- 3º B (curso_id = 32)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('JORGE JOAQUIN', 'GONZALEZ BAEZ', 32, 6300937),
    ('EMILIO ANDRES', 'ALMIRON RUIZ', 32, 8651544),
    ('JORGE DAVID', 'AVEIRO DURE', 32, 6763135),
    ('GABRIELA DENISSE', 'BENITEZ CAMPUZANO', 32, 6248031),
    ('PAMELA MONSERRAT', 'CABALLERO ZARACHO', 32, 6122730),
    ('FABRICIO NICOLAS', 'CUBAS VAZQUEZ', 32, 6299174),
    ('JESUS MARIA', 'DAVID RESQUIN', 32, 7112304),
    ('SANTIAGO DIDIER DAMASO', 'DELVALLE CABRAL', 32, 6323522),
    ('PAULO GASTON', 'DUARTE ORUE', 32, 6506158),
    ('ALBA MARIA ELIZABETH', 'FARIÑA MORAN', 32, 6682899),
    ('EVELYN CECILIA', 'GALEANO DUARTE', 32, 6254779),
    ('FRANCO GONZALO', 'GARCIA GARCIA', 32, 6378044),
    ('ANGELO GASTON', 'GONZALEZ AMARILLA', 32, 6306858),
    ('JUANA DAMARIS', 'HUACCA ALEJO', 32, 9132227),
    ('MILAGROS MICAELA', 'JIMENEZ ROJAS', 32, 6276848),
    ('LUCAS   MANUEL', 'LOPEZ ALDERETE', 32, 6709236),
    ('PABLO LEANDRO', 'LOPEZ PULLARES', 32, 6128349),
    ('LUNA MIA', 'MENDIETA', 32, 6521146),
    ('VICTOR MANUEL', 'MENDIETA PEREIRA', 32, 7965966),
    ('GAIA VIOLETA MARIA', 'MOREL AREVALOS', 32, 6315503),
    ('FACUNDO MATHIAS', 'PRIETO CACERES', 32, 7277773),
    ('AIDEE FIORELLA', 'RECALDE CASTILLO', 32, 7116092),
    ('YANARA AYELEN DOMINGA', 'RODAS VALDEZ', 32, 6337830),
    ('FIORELLA ANAHI', 'SOSA AMARILLA', 32, 7934035),
    ('SANTIAGO', 'SOSA OVELAR', 32, 6138828),
    ('ANA BELEN', 'VARGAS VALIENTE', 32, 6597209),
    ('HEATHER PATRICIA', 'WATTIEZ BAREIRO', 32, 6600003),
    ('ELIAS SEBASTIAN', 'ZORRILLA BENITEZ', 32, 6355776);

    -- 2º A (curso_id = 33)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci) VALUES
    ('MARIELA CHONG AH', 'ACOSTA POSADAS', 33, 6634030),
    ('PEDRO JOSÉ', 'ALDERETE PÁEZ', 33, 6599141),
    ('GUILLERMO MANUEL', 'APONTE RAMÍREZ', 33, 6375687),
    ('ARIEL MAXIMILIANO', 'ARAUJO SOSA', 33, 7868229),
    ('TYRA SELENE', 'BARBOZA CABRERA', 33, 6514004),
    ('JUAN GABRIEL', 'CORONEL VILLALBA', 33, 6780823),
    ('MICAELLA VALENTINA', 'ESPINOZA BELLOTO', 33, 6323591),
    ('IVAN ALEXANDER', 'FERNÁNDEZ MEZA', 33, 6674310),
    ('JUAN FABRICIO', 'FLEITAS IBÁÑEZ', 33, 7208277),
    ('LIA JAZMIN', 'FLEITAS PÉREZ', 33, 8177227),
    ('BRAYAN', 'GARCÍA FERNÁNDEZ', 33, 8563705),
    ('MARIANA EMILIA', 'GONZÁLEZ CASTRO', 33, 6738451),
    ('RAFFAELL', 'GONZÁLEZ LARREA', 33, 6623572),
    ('ÁNGEL JOSÉ IVAN', 'MACIEL RUÍZ DÍAZ', 33, 8079060),
    ('RICARDO GERMAN', 'MARTÍNEZ ROJAS', 33, 7488331),
    ('MOISES', 'MELGAREJO SAUCEDO', 33, 7230274),
    ('RODRIGO GABRIEL', 'MOREL MORENO', 33, 7383873),
    ('EMILIO JOSÉ', 'MORÍNIGO PEÑA', 33, 7071354),
    ('ANGÉLICA SUSANA', 'ORUÉ AYALA', 33, 6619509),
    ('TANIA GUADALUPE', 'PAIVA SOTELO', 33, 7209622),
    ('JUAN JOSÉ', 'PALMA RODRÍGUEZ', 33, 6813981),
    ('ALEJANDRO JOSÍAS', 'PÉREZ ÁVALOS', 33, 6534642),
    ('VINICIUS', 'RODRÍGUEZ DE OLIVEIRA', 33, 8758628),
    ('JOSÍAS ALEXANDER', 'SANTACRUZ OTAZU', 33, 6632204),
    ('ALESSANDRO JULIÁN', 'UNZAIN INSFRÁN', 33, 6599080),
    ('SOFÍA ARAMÍ', 'VERA MARTÍNEZ', 33, 6658849),
    ('FABIOLA LUJÁN', 'VERÓN MONGELÓS', 33, 6625127),
    ('WENDY AYELÉN', 'ZÁRATE ROJAS', 33, 6781794);

    -- 2º B (curso_id = 34)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('FELIX HERNAN', 'ALCARAZ MEZA', 34, 6549365),
    ('YAGO LAREN', 'AMARILLA LEGUIZAMON', 34, 6581374),
    ('MONSERRAT ANAHI', 'AYALA GAUTO', 34, 6693608),
    ('DYLAN VIRGILIO', 'BURGOS ROTELA', 34, 7401358),
    ('ISAAC ULISES', 'CUEVAS SAAVEDRA', 34, 6613266),
    ('ANGEL GABRIEL', 'DIAZ CAÑETE', 34, 7293215),
    ('FEDERICO AMIN', 'DOMINGUEZ SOSA', 34, 6538527),
    ('ALEJANDRA ANAHI', 'ESCOBAR OJEDA', 34, 6833279),
    ('EDEL JAZMIN', 'FRANCO MACIEL', 34, 6593803),
    ('ALEXIS DANIEL', 'FRETEZ VILLAMAYOR', 34, 6582254),
    ('SAULO EZEQUIEL', 'GALEANO RIVEROS', 34, 6704166),
    ('LUCAS GABRIEL', 'GAUTO NUÑEZ', 34, 6325567),
    ('ADRIAN', 'GRASSO RAMOS', 34, 6617987),
    ('MILAGROS MARGARITA YERUTI', 'GUPPI BORDON', 34, 8506321),
    ('AMILCAR ANDRES', 'JARA AGUILERA', 34, 7138719),
    ('DANAE ABIGAIL', 'JARA MARTINEZ', 34, 7551072),
    ('MATEO FERNANDO', 'LENCINA AREVALOS', 34, 6883337),
    ('MARTIN ALEJANDRO', 'LEZCANO MONTIEL', 34, 6626178),
    ('THIAGO VALENTINO', 'MARTINEZ FERNANDEZ', 34, 6727372),
    ('TOBIAS EZEQUIEL', 'MEDINA GONZALEZ', 34, 6512532),
    ('VALERIA NOEMI', 'MONTIEL TRIVERO', 34, 7337850),
    ('NATHALIA MARIELA', 'ORTIZ RODRIGUEZ', 34, 6532910),
    ('OSIAS BENJAMIN', 'RUBIO SAMUDIO', 34, 6971481),
    ('GIOVANNI JOSE', 'RUIZ ROMAN', 34, 7099638),
    ('SAMYRA ANAHI', 'SANCHEZ AGUILAR', 34, 7086918),
    ('ENZO SIMON', 'SANCHEZ VERON', 34, 6966829),
    ('MARIA TANIA', 'SOILAN SOSA', 34, 6634375),
    ('FIORELLA MAGALI', 'VILLAMAYOR VAZQUEZ', 34, 7225342);

    -- 1º A (curso_id = 35)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('EDGAR ARTURO', 'ALVAREZ BENITEZ', 35, 7090557),
    ('ANNA GABRIELA', 'ARAMBULO GONZALEZ', 35, 7357449),
    ('SEBASTIAN', 'BALBUENA GAONA', 35, 7159693),
    ('NAOMI ABIGAIL', 'BENITES NOGUERA', 35, 6763142),
    ('JOSÉ TOMÁS', 'BENÍTEZ BARRIOS', 35, 7370289),
    ('SANTINO RAÚL', 'BENÍTEZ ROA', 35, 7761410),
    ('LUCAS SEBASTIAN', 'CACERES CARDOZO', 35, 7726370),
    ('MATIAS DANIEL', 'CANDIA ALFONSO', 35, 6950228),
    ('ALEJANDRO NICOLAS', 'CENTURION CENTURION', 35, 6719867),
    ('SANTIAGO BENJAMIN', 'COCCO FRUTOS', 35, 6740205),
    ('SOFÍA', 'DA COSTA VALDEZ', 35, 7304888),
    ('JORGE ANDRES', 'DE LA BARRA ZOILAN', 35, 7399317),
    ('ELENA ISABELLA EDITH', 'DELGADILLO ESTIGARRIBIA', 35, 7211626),
    ('EDUARDO SEBASTIAN', 'DUARTE CASTILLO', 35, 6707283),
    ('ELIAN ANDRES', 'ESTIGARRIBIA UGARTE', 35, 6937627),
    ('KAREN GUADALUPE', 'FRUTOS MORALES', 35, 6926830),
    ('JUAN ENRIQUE', 'LECKIE ROLÓN', 35, 6751503),
    ('ANNA MEI', 'NOGUERA PENG', 35, 6870160),
    ('NATALIA BELEN', 'NUÑEZ VILLAMAYOR', 35, 7918425),
    ('NAHOMI BELÉN', 'OCAMPOS ACOSTA', 35, 6822731),
    ('RODRIGO MARTÍN', 'OLMEDO ZÁRATE', 35, 6977116),
    ('MARÍA LUJÁN', 'OVELAR CENTURIÓN', 35, 7045854),
    ('ALEXIA', 'OVELAR MARTÍNEZ', 35, 6766800),
    ('JOSE GIOVANNI', 'PORTILLO RIVEROS', 35, 6887445),
    ('JOSUE SEBASTIAN', 'QUINTANA BURGOS', 35, 6887564),
    ('ANIBAL', 'RAMIREZ ORTIZ', 35, 6741348),
    ('PEDRO DANIEL', 'RECALDE ROMERO', 35, 6864091),
    ('ANNELISE MARIA JOSÉ', 'SANABRIA DELPADRE', 35, 7047256);

    -- 1º B (curso_id = 36)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('ENRIQUE DAMIÁN', 'ACOSTA MEDINA', 36, 7286222),
    ('RODRIGO JAVIER', 'AYALA NAVARRO', 36, 7052595),
    ('GUILLERMO DANIEL', 'AYALA OCHIPINTTI', 36, 6803972),
    ('SANTIAGO DARIO', 'BÁEZ BORDON', 36, 6805076),
    ('JAVIER DE JESUS', 'BOGADO PERALTA', 36, 6754519),
    ('FABRIZIO BENJAMÍN', 'CABALLERO VILLANUEVA', 36, 6682833),
    ('JORGE BENJAMIN', 'DOMINGUEZ GALEANO', 36, 7088828),
    ('FERNANDA ISABEL', 'GALEANO RUIZ DÍAZ', 36, 7484939),
    ('HORACIO JOSE', 'GIMENEZ MEZA', 36, 6722653),
    ('MARIA JOSÉ', 'GIMENEZ TREVISON', 36, 6852970),
    ('LUCAS DANIEL', 'GÓMEZ MORENO', 36, 6698773),
    ('GUILLERMO FACUNDO', 'MARTÍNEZ BENÍTEZ', 36, 7313005),
    ('RODRIGO DANIEL', 'MARTINEZ MARTINO', 36, 7064807),
    ('MARTIN RAFAEL', 'MONGELOS BRITEZ', 36, 7037983),
    ('THIAGO ALEXANDER', 'OCAMPOS RIVAS', 36, 6752851),
    ('MARCELO JAVIER', 'PÉREZ VELÁZQUEZ', 36, 7228240),
    ('ALEXANDER DAVID', 'PORTILLO OLMEDO', 36, 6820649),
    ('ALEJANDRO ABEL', 'RIVELA TORALES', 36, 7565497),
    ('FABRIZIO ARIEL', 'RODAS CABRERA', 36, 6799915),
    ('MARIA ISABEL', 'RODRIGUEZ ACOSTA', 36, 6742018),
    ('FACUNDO DANIEL', 'RODRIGUEZ LIMA', 36, 6802446),
    ('DULCE MARIA GUADALUPE', 'SAUCEDO PARRA', 36, 6711084),
    ('IANN DANIEL', 'TOLEDO ARANDA', 36, 6818642),
    ('ISAAC ISMAEL', 'TORALES OVELAR', 36, 6923315),
    ('GIULIANNA ARAMI', 'VALDEZ FERNÁNDEZ', 36, 8187153),
    ('EZEQUIEL', 'VALENZUELA CABALLERO', 36, 7319899),
    ('MATEO RAFAEL', 'VELAZQUEZ AMADI', 36, 7138069),
    ('FRANCISCO RAFAEL', 'ZARZA MARTÍNEZ', 36, 6788083);

-- 1er Curso 2026 — resto de las especialidades (Informática 1º ya está más arriba).
-- Cargado desde 'LISTAS 1er CURSO 2026 SG.xls' (Secretaría General, ago-2026).
-- promocion = 2028 para todo 1er curso del período 2026.

-- Construcciones Civiles 1º A (curso_id = 7)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('YANINA MARIA PAULA', 'AGÜERO ENCIZO', 7, 6717156),
    ('MICAELA', 'BAEZ CORONEL', 7, 6848974),
    ('ALEXIA VALENTINA', 'BARBOZA DUARTE', 7, 7150185),
    ('BRUNO SEBASTIAN', 'CANDIA BARRETO', 7, 7436812),
    ('FABRIZIO JAVIER', 'CANDIA CACERES', 7, 6975857),
    ('ALEJANDRA SOLEDAD', 'CARDOZO SEGOVIA', 7, 6764124),
    ('PALOMA VALENTINA', 'CHAPARRO ACOSTA', 7, 7015051),
    ('GUSTAVO GABRIEL', 'CONTRERA ALARCON', 7, 7179928),
    ('DALMA SIRLENE', 'DELVALLE CACERES', 7, 7190512),
    ('MILAGROS YERUTI', 'FERREIRA CABRAL', 7, 7154033),
    ('RICHARD SAMUEL', 'FERREIRA MAIDANA', 7, 6841677),
    ('DANNA VIOLETA', 'GARCIA ALARCON', 7, 6975512),
    ('ARIANNA JAZMIN', 'GOMEZ FERLONI', 7, 6778522),
    ('FACUNDO LUIS', 'JARA CESPEDES', 7, NULL),  -- C.I. 6778522 duplicado en la fuente (Construcciones Civiles A GOMEZ FERLONI, ARIANNA JAZMIN)
    ('SANTINO EZEQUIEL', 'LOPEZ NOTARIO', 7, 6828778),
    ('GABRIELA', 'MARTINEZ GUERRERO', 7, 6747934),
    ('PABLO RAFAEL DE JESUS', 'MARTINEZ PAREDES', 7, 6637574),
    ('PABLO DARIO', 'MARTINEZ RIVEROS', 7, 7132171),
    ('ANNA PAULA', 'MENDEZ VERON', 7, 6944953),
    ('MICAELA BELEN', 'MIRANDA BORJA', 7, 7173452),
    ('ELENA BEATRIZ', 'ORTIZ CORONEL', 7, 6754258),
    ('ANGELICA BEATRIZ', 'PAREDES ALONSO', 7, 6886185),
    ('MIREIA ABIGAIL', 'RECALDE ACOSTA', 7, 6955012),
    ('FIORELLA ARAMI', 'ROA DUARTE', 7, 6776032),
    ('STEFANY AYLEM', 'RUIZ DIAZ PRIETO', 7, 7264123),
    ('ENZO ARIEL', 'VERA URAN', 7, 6819367),
    ('LUANA BELEN', 'VILLALBA RYAN', 7, 6922179),
    ('RYN MAIRA', 'YOSHIZAKI NISHIMURA', 7, 6985906);

-- Construcciones Civiles 1º B (curso_id = 8)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('FLORY ANAHI', 'AYALA BATTILANA', 8, 8181769),
    ('JORGE ALEJANDRO', 'BAREIRO AMARILLA', 8, 7587045),
    ('SANTIAGO EMANUEL', 'CERRANO BARRIOS', 8, 6750468),
    ('JOHANA ZUNILDA', 'DUARTE ACOSTA', 8, 7654238),
    ('FABRIZIO BENJAMIN', 'FARIÑA LOPEZ', 8, 6754566),
    ('AMIRA ABIGAIL', 'FERNANDEZ AQUINO', 8, 6703511),
    ('THIAGO JAVIER', 'FLEITAS OLAVARRIETA', 8, 7368879),
    ('AYELEN GUADALUPE', 'GARCIA CABRERA', 8, 7442884),
    ('ANTONELLA MERCEDES', 'GINI GUERRERO', 8, 6671521),
    ('LUANA ARAMI', 'GODOY LEZCANO', 8, 6683358),
    ('THIAGO JONAS', 'IBAÑEZ GONZALEZ', 8, 7018261),
    ('NATHALIA MILAGROS', 'JARA VALDEZ', 8, 6864212),
    ('ALMA ARAMI', 'LEZCANO VILLALBA', 8, 7836541),
    ('MAIRA MARIEL', 'LOPEZ AVALOS', 8, 7082718),
    ('RENATO SEBASTIAN', 'MARTINEZ FARIÑA', 8, 7312674),
    ('SOFIA ISABEL', 'MARTINEZ FERREIRA', 8, 6788430),
    ('CAMILA BELEN', 'MARTINEZ GAMARRA', 8, 6946279),
    ('RAMON', 'MORAY ESPINOZA', 8, 7262559),
    ('LUJAN MONSERRATH', 'NUÑEZ MENDEZ', 8, 7643816),
    ('MARCOS NAHUEL', 'OCAMPOS ZACARIAS', 8, 7218668),
    ('EMMA DAHIANE', 'PORTILLO MALDONADO', 8, 7685486),
    ('ANA PATRICIA', 'ROCA ROMAN', 8, 6822676),
    ('ALEJANDRA LISSET', 'RODRIGUEZ RAMIREZ', 8, 7358975),
    ('ARELI MILENA MICAELA', 'ROJAS BRIZUELA', 8, 7787476),
    ('MELINA BEATRIZ', 'SANABRIA FERNANDEZ', 8, 7032065),
    ('KASUMI', 'TSUBOI', 8, 7777875),
    ('TANYA GISSELLE', 'VESTER BAEZ', 8, 6828953);

-- Construcciones Civiles 1º C (curso_id = 9)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('SOL MARIA', 'AGÜERO DELGADO', 9, 7037265),
    ('MARIAM FIORELLA', 'AGUIRRE SANTACRUZ', 9, 6967407),
    ('KIMBERLY ARAMI', 'ARCE LEZCANO', 9, 7058434),
    ('MILAGROS LUJAN', 'BENITEZ MARECOS', 9, 6755762),
    ('CARMEN AZUCENA', 'BOBADILLA LOPEZ', 9, 7221051),
    ('SOFIA AURORA', 'BORBA BARBIERI', 9, 6887029),
    ('MARIA JAZMIN', 'CAMPOS SOSA', 9, 7054951),
    ('MELISSA GISSEL', 'CHAPARRO COLMAN', 9, 6975983),
    ('ADRIANA MAGALI', 'CHAPARRO ESCURRA', 9, 7614708),
    ('YULIANA JAZMIN', 'DUARTE GAONA', 9, 6969295),
    ('FABRICIO AMIN', 'FIGUEREDO RIVEROS', 9, 6733785),
    ('KATYA ABIGAIL', 'GENES RAMOS', 9, 7474771),
    ('PATRCIA ESTHER', 'GONZALEZ CANTERO', 9, 8879705),
    ('ROMINA AILEEN', 'GONZALEZ SIUDA', 9, 6885287),
    ('TIHANA AYLEN', 'MELGAREJO COLMAN', 9, 6918497),
    ('VIVIANA BELEN', 'MORENO ROJAS', 9, 6675391),
    ('VICTORIA ELIZABETH', 'NUÑEZ PRIETO', 9, 6900353),
    ('GIANNINA MONSERRATH', 'OJEDA IBARRA', 9, 7414532),
    ('SOL BEATRIZ', 'ORTEGA FERNANDEZ', 9, 7126227),
    ('SOFIA AYELEN', 'QUINTANA VILLALBA', 9, 6923317),
    ('SOFIA NICOLE', 'RAMIREZ LEZCANO', 9, 7803037),
    ('YERUTI ADRIANA', 'RAMIREZ MAQUEDA', 9, 6819381),
    ('SARA REBECA', 'RODRIGUES ORTEGA', 9, 6715107),
    ('SOFIA MAGALI', 'SANABRIA ORTIZ', 9, 6883226),
    ('AXEL EDUARDO', 'SOSA LEGUIZAMON', 9, 7014987),
    ('ALBERT BATISTA', 'VERA CACERES', 9, 7414958),
    ('BIA ANTONELLA', 'VILLALBA ARRUA', 9, 7295566),
    ('EMILIO EZEQUIEL', 'VILLASANTI DUARTE', 9, 6852619);

-- Electricidad 1º A (curso_id = 14)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('KATHERIN LUANA', 'ALVAREZ SANCHEZ', 14, 6876188),
    ('BELEN AGUSTINA', 'BARBOZA SANCHEZ', 14, 6646474),
    ('DANIEL ALCIDES', 'BENITEZ VERA', 14, 7688456),
    ('HORACIO ALEJANDRO', 'CABRERA ESCUDERO', 14, 6761025),
    ('JUANJO JONAS', 'CANTERO RECALDE', 14, 6243413),
    ('CAMILA MARIA BELEN', 'CHENA BRITEZ', 14, 6781795),
    ('KIARA YERUTI', 'CURIN OVELAR', 14, 6915873),
    ('VIOLETA NICOL', 'ESPINOLA NUÑEZ', 14, 7782200),
    ('ROCIO LIZETTE', 'ESPINOZA CANO', 14, 6699354),
    ('ISMAEL SEBASTIAN', 'GARCIA RAMIREZ', 14, 6820980),
    ('THIAGO DAVID', 'HERMOSILLA GONZALEZ', 14, 7560587),
    ('MARIA DE LOURDES', 'ISIDRE RAMIREZ', 14, 6926728),
    ('NICOLAS RENE', 'MACIEL SEGOVIA', 14, 6872763),
    ('GLADYS ELIZABETH', 'MAIDANA AQUINO', 14, 6805998),
    ('FIORELLA VALENTINA', 'MARTINEZ CANTERO', 14, 7736737),
    ('PAULA FLORENCIA', 'MARTINEZ RIVALDI', 14, 7767556),
    ('ISABELLA VIVIANA', 'MELGAREJO SAMANIEGO', 14, 7727046),
    ('JUAN DARIO DE JESUS', 'MORAN SEGOVIA', 14, 7303084),
    ('IVAN RAFAEL', 'NUÑEZ FERNANDEZ', 14, 6818954),
    ('MALENA NICOLE', 'OLAZAR JACQUET', 14, 7997098),
    ('SANTINO RAFAEL', 'PATIÑO ROLON', 14, 6923039),
    ('SOFIA ANABEL', 'PEREZ CARDOZO', 14, 6832656),
    ('KLAUS NATHANIEL', 'PINTOS GARCIA', 14, 6795642),
    ('BENJAMIN', 'RUSSO RAMIREZ', 14, 7221810),
    ('SELENA ALEJANDRA', 'SILVERO GIMENEZ', 14, 7372222),
    ('GUSTAVO EMANUEL', 'SOSA DOMINGUEZ', 14, 7045399),
    ('ESTEBAN', 'TAMAS MARTINEZ', 14, 6739158),
    ('JOAQUIN GONZALO', 'TORRES PAREDES', 14, 6872771);

-- Electricidad 1º B (curso_id = 15)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('MIA NAZARENA', 'ACOSTA GIMENEZ', 15, 6952444),
    ('ZAIRA FABIOLA', 'ALMIRON RAMIREZ', 15, 6773461),
    ('SEBASTIAN ENMANUEL', 'BAEZ MARTINEZ', 15, 7891094),
    ('NERY ABEL', 'BENITEZ LOVERA', 15, 6968821),
    ('GRACE VALENTINA', 'BOGARIN GOMEZ', 15, 7945223),
    ('DEVIN DANIEL', 'CANDIA RIOS', 15, 6995656),
    ('ALAN JOSUE', 'FERNANDEZ FIGUEREDO', 15, 7377801),
    ('FACUNDO NAHUEL', 'FERREIRA PAREDES', 15, 7001557),
    ('DANNA MARIA LUISA', 'GIMENEZ PEREIRA', 15, 7774066),
    ('JUAN SEBASTIAN', 'GOMEZ VERA', 15, 6879541),
    ('ANIBAL BENJAMIN', 'GOMEZ VILLALBA', 15, 7045543),
    ('ADRIAN GABRIEL', 'MARTINEZ VILLALBA', 15, 7163061),
    ('YAXENI AILEM', 'MENDIETA AGUIAR', 15, 7231495),
    ('LUCAS RAFAEL', 'MEZA RAMOS', 15, 7021434),
    ('ANDREA ISABELLA', 'MIERS MONGES', 15, 6645150),
    ('SANTIAGO', 'MONTIEL ACOSTA', 15, 6809993),
    ('CONSTANZA LUJAN', 'PEREZ ORTIZ', 15, 6960655),
    ('SANTIAGO SAMUEL', 'RIQUELME ROJAS', 15, 7602219),
    ('AGUSTIN', 'ROJAS DA SILVA MELLO', 15, 6708953),
    ('SANTIAGO JOSE', 'ROJAS DIAZ', 15, 7510143),
    ('ENZO PAOLO', 'ROLANDI VACCARI', 15, 6508573),
    ('MARIA JOSE', 'RUIZ ROMAN', 15, 7099709),
    ('MILAGROS ELENA', 'SALINAS DENIS', 15, 7003860),
    ('TOBIAS JOEL', 'SOSA VILLALBA', 15, 6714742),
    ('JOSE GASPAR', 'TORRES AQUINO', 15, 6931755),
    ('ENRIQUE GABRIEL', 'VARGAS FARIÑA', 15, 6888623),
    ('ENZO JOAQUIN', 'VILLALBA BENITEZ', 15, 6595379),
    ('FIORELLA ABIGAIL', 'VILLAMAYOR ALARCON', 15, 7084076);

-- Electrónica 1º A (curso_id = 22)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('ALEXANDER CESAR OSVALDO', 'ACOSTA GUZMAN', 22, 7945565),
    ('EMILI LUJAN', 'ARAUJO CACERES', 22, 7918502),
    ('EMMA SOPHIA', 'ARGUELLO MONTEGGIA', 22, 7237874),
    ('PEDRO ALCIDES', 'BENITEZ ROJAS', 22, '51540489L'),
    ('MARIANA LUJAN', 'BORDABERRY GONZALEZ', 22, 7210062),
    ('LUIS ELIAS', 'CASTILLO DUARTE', 22, 8009053),
    ('ANA FIORELLA', 'FERREIRA GUERRERO', 22, 6779801),
    ('VERONICA GISSELLE', 'GONZALEZ ORTIZ', 22, 6951028),
    ('VICTORIA MILAGROS', 'IMAS GALEANO', 22, 7300812),
    ('CRISTHIAN ARIEL', 'IRALA MENDOZA', 22, 7656913),
    ('REGINA YERUTI', 'JARA MANCUELLO', 22, 7070570),
    ('MARGED GUADALUPE', 'KAPPELER FLORES', 22, 7441361),
    ('GERARDO MIGUEL', 'LOPEZ ARELLANO', 22, 7012625),
    ('CARLOS MARINO DE JESUS', 'LOPEZ MIRANDA', 22, 7543713),
    ('DAMIAN EZEQUIEL', 'MARTINEZ DIAZ', 22, 6865557),
    ('ELIAS SEBASTIAN', 'MEDINA GONZALEZ', 22, 7070234),
    ('MATIAS JOSSUE', 'MONTIEL SANABRIA', 22, 8214428),
    ('GUILLERMO DAVID', 'MOURA VALDEZ', 22, 6999696),
    ('ALVARO SANTINO', 'PERALTA ARENA', 22, 7639731),
    ('ADOLFO MARTIN', 'RAMIREZ VILLALBA', 22, 6791815),
    ('MARCOS ADRIAN', 'RIQUELME ARZAMENDIA', 22, 6748509),
    ('FACUNDO ADRIAN', 'RODRIGUEZ ESCOBAR', 22, 7020273),
    ('JIMENA BELEN', 'RODRIGUEZ GALEANO', 22, 7061706),
    ('VALENTINA NOEMI', 'RODRIGUEZ GIMENEZ', 22, 7529213),
    ('SANTINO GASPAR', 'TORALES RUIZ', 22, 7083935),
    ('GIOVANNA MARIEL', 'VARGAS MORINIGO', 22, 7047499),
    ('ALEJANDRO RAFAEL', 'VERA LOPEZ', 22, 6996063),
    ('ELENA SOFIA', 'VILLALBA DUARTE', 22, 6700709);

-- Electrónica 1º B (curso_id = 23)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('DIEGO JAMAL', 'ACOSTA MEZA', 23, 6625954),
    ('MIA COSTANZA', 'ARIAS ALCARAZ', 23, 6784748),
    ('LUCAS FERNANDO', 'ARZAMENDIA BENITEZ', 23, 6545514),
    ('JONATHAN NEIL', 'AYALA GIMENEZ', 23, 7736647),
    ('JAZMIN ARACELI', 'BENITEZ ALVAREZ', 23, 6888492),
    ('JUAN CRUZ', 'BRITOS GOMPERTT', 23, 9200232),
    ('ALVARO MANUEL', 'CAMBRA RAMOS', 23, 7053206),
    ('MIGUEL ANGEL', 'CAREAGA ARANDA', 23, 6850845),
    ('MARTIN BENJAMIN', 'CESPEDES LOPEZ', 23, 7005488),
    ('AYLEN MAGALI', 'DOMINGUEZ MORENO', 23, 7196898),
    ('SANTIAGO EMANUEL', 'DURE CAMPUZANO', 23, 7725881),
    ('LUCAS SAMUEL', 'INSAURRALDE ROMERO', 23, 6735637),
    ('MIA NAYI', 'LEZCANO FERNANDEZ', 23, 6784849),
    ('DARA ABIGAIL', 'LOPEZ SAFFI', 23, 7284295),
    ('ENZO FACUNDO', 'MAYEREGGER MARTINEZ', 23, 6735892),
    ('FIORELLA AGOSTINA', 'MORAN PEREIRA', 23, 6808172),
    ('WISAN DANIEL', 'MOREL CABAÑAS', 23, 6982579),
    ('SOFIA ALEJANDRA', 'OJEDA JARA', 23, 7039284),
    ('FIORELLA NOEMI', 'ORTELLADO OLMEDO', 23, 6692072),
    ('MAX ELIEL', 'OVELAR ORUE', 23, 7549687),
    ('HUGO DANILO', 'PEÑA CASTRO', 23, 6744272),
    ('SOPHIA MONTSERRAT', 'ROJAS ESPINOZA', 23, 6839945),
    ('LUCIA AYMARA', 'ROMERO TESTI', 23, 6987155),
    ('JOSE FEDERICO', 'ROMERO VILLALBA', 23, 6994540),
    ('JUAN SEBASTIAN', 'SOTELO GONZALEZ', 23, 6974772),
    ('SEBASTHIAN', 'TORRADO ORTIZ', 23, 6911813),
    ('MAURICIO JOSE', 'VELAZQUEZ VILLALBA', 23, 7134137),
    ('DIEGO SEBASTIAN', 'VERA RODRIGUEZ', 23, 7218563);

-- Electrónica 1º C (curso_id = 24)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('ALAN EZEQUIEL', 'ALCARAZ PERALTA', 24, 7314702),
    ('KARIM MARIA', 'ARENAS ROJAS', 24, 7194609),
    ('GONZALO JESUS', 'AVALOS ESCOBAR', 24, 6907656),
    ('SHIRLEY LIZBETH', 'AVALOS MARTINEZ', 24, 7021859),
    ('LARA VALENTINA', 'BENITEZ AMARILLA', 24, 7567156),
    ('MAXIMO VALENTINO', 'CACERES SERAFINI', 24, 6688689),
    ('FIORELLA MONTSERRATH', 'CALDERON RAMIREZ', 24, 7471341),
    ('LUCIANO ANDRES', 'CAÑETE BOGADO', 24, 6946512),
    ('IVAN MOISES', 'CASTILLO VELAZQUEZ', 24, 7089056),
    ('JIMMY FABIAN', 'DIAZ RIOS', 24, 7751765),
    ('ELIAS ROBERTO', 'DINATALE MENDOZA', 24, 7282481),
    ('TADEO EZEQUIEL', 'DOMINGUEZ FRANCO', 24, 6827559),
    ('LUCAS AMIN', 'ESPINOLA AGÜERO', 24, 6362876),
    ('FERNANDO FABIAN', 'FRANCO MUÑOZ', 24, 6763188),
    ('MERCEDES ARAMI', 'GARAY AGÜERO', 24, 7125540),
    ('LUCAS DARNELL', 'GIMENEZ PIZURNO', 24, 6852129),
    ('FABRIZIO JOEL', 'GUTIERREZ LEZCANO', 24, 6952029),
    ('ELIEN ADRIANA', 'LEGUIZAMON', 24, 6766984),
    ('EDGAR ALEXANDER', 'MARTINEZ BERNAL', 24, 6817580),
    ('JOSIAS EMANUEL', 'MONELLO CUENCA', 24, 8051984),
    ('MARTINA MASAMI', 'NAGAI SATO', 24, 7199584),
    ('RODRIGO AGUSTIN', 'RIVAROLA DIAZ', 24, 6854460),
    ('MELANIE ANAHI', 'ROLON MARTINEZ', 24, 7490290),
    ('GIANPAULO SAMUEL', 'TUFARI CHAMORRO', 24, 7005672),
    ('SOFIA ELIZABETH', 'TUFARI FLEITAS', 24, 7189239),
    ('GABRIELA MONSERRATH', 'VELAZQUEZ DENIS', 24, 7772334),
    ('JOAN ANDRES', 'VENIALGO LEGUIZAMON', 24, 6710136),
    ('CARLOS ANDRES', 'VILLAMAYOR BARRETO', 24, 7876408);

-- Electromecánica 1º A (curso_id = 29)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('SANTIAGO ANDRES', 'AMARILLA PENAYO', 29, 7111290),
    ('CECILIA ABIGAIL', 'AMARILLA VELAZQUEZ', 29, 6654197),
    ('ALEXANDER GABRIEL', 'ARANDA MARTINEZ', 29, 7504485),
    ('JULIETA', 'BAEZ CORONEL', 29, 6848973),
    ('ALAN GABRIEL', 'BOGADO ACOSTA', 29, 6778334),
    ('JAVIER EZEQUIEL', 'BORDON CACERES', 29, 6978018),
    ('GONZALO SEBASTIAN', 'BRITEZ NUÑEZ', 29, 7650546),
    ('JOAQUIN DEJESUS', 'DIAZ RUIZ DIAZ', 29, 6844015),
    ('DIEGO ESTEBAN', 'DIAZ SANCHEZ', 29, 7001138),
    ('DIEGO HERNAN', 'DIAZ SANCHEZ', 29, 7004441),
    ('MIGUEL ANGEL', 'ESCOBAR MENDIETA', 29, 6894798),
    ('ANA LUCIA ALEJANDRA', 'ESPINOLA HERRERA', 29, 7007200),
    ('JEYMI LUJAN', 'GALEANO RODRIGUEZ', 29, 7760193),
    ('THOBIAS GABRIEL', 'GUZMAN CACERES', 29, 6611872),
    ('CRISTHIAN EDUARDO', 'MALDONADO FERNANDEZ', 29, 6910827),
    ('JOAQUIN EZEQUIEL', 'MARIN BAEZ', 29, 7009055),
    ('OCTAVIO DANIEL', 'MARTINEZ PRESENTADO', 29, 7383949),
    ('SANTIAGO RAFAEL', 'MEDINA GONZALEZ', 29, 7070214),
    ('ALEXANDER', 'NOCEDA OH', 29, 8173876),
    ('GONZALO SAMUEL', 'NUÑEZ BAREIRO', 29, 6872027),
    ('JUAN JOSE', 'OZORIO BRITEZ', 29, 6746572),
    ('FLORENCIA ARAMI', 'PENAYO GONZALEZ', 29, 6812274),
    ('SEBASTIAN MANUEL', 'PORTILLO CABRERA', 29, 7788249),
    ('MARIA VICTORIA', 'RAMUA GONZALEZ', 29, 7321875),
    ('ALVARO LUIS', 'ROA MARTINEZ', 29, 7012705),
    ('BRUNO ANDRES', 'RUIZ DIAZ VALDIVIA', 29, 6878230),
    ('FABRIZZIO VALENTINO', 'VEGA GIMENEZ', 29, 7142100),
    ('MARIA JOSE', 'ZALAZAR PAREDES', 29, 7054794);

-- Electromecánica 1º B (curso_id = 30)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('RUTH ANAHI', 'AGÜERO ROLON', 30, 7718527),
    ('LUCAS ANDRES', 'ALVARES PAREDES', 30, 7383656),
    ('SOFIA VALENTINA', 'ARRUA DELVALLE', 30, 7011964),
    ('MARIA PAZ', 'BAEZ ROMERO', 30, 7294760),
    ('CARLOS DANIEL', 'BORDON BELOTTO', 30, 6692364),
    ('AURORA LUCIA', 'BOVEDA SANCHEZ', 30, 6808130),
    ('GONZALO DANIEL', 'BRITEZ GAGO', 30, 7325300),
    ('MARIO MANUEL', 'CASTILLO BAZAN', 30, 7894915),
    ('GIOVANNI SEBASTIAN', 'DIAZ DENIS', 30, 6853580),
    ('ELIAS GABRIEL', 'DUARTE PORTILLO', 30, 6886183),
    ('IVAN MARCELO', 'ESPINDOLA AYALA', 30, 6692294),
    ('ARACELI YERUTI', 'FERNANDEZ RODRIGUEZ', 30, 6663803),
    ('GENARO JOSE', 'FLORENTIN CUENCA', 30, 6740177),
    ('ALEJANDRA MARIA JOSE', 'GALEANO MARTINEZ', 30, 7504426),
    ('LEONARDO ARIEL', 'GARAY LOPFE', 30, 7132189),
    ('MARA AYLEEN', 'GUILLEN AMARILLA', 30, 6760198),
    ('ANIBAL MAXIMILIANO', 'HULSKAMP ALONSO', 30, 7366522),
    ('DANILO BENJAMIN', 'LOPEZ GUERRERO', 30, 7656057),
    ('TANIA KIARA', 'MARTINEZ LUGO', 30, 6754238),
    ('FABRIZIO AGUSTIN', 'MAZACOTTE REGUNEGA', 30, 7291333),
    ('ENZO DAMIAN', 'MERELES VILLALBA', 30, 6805498),
    ('BLAS ANGEL MANUEL', 'ORTIZ GALEANO', 30, 6914822),
    ('LUCIA ABIGAIL', 'RIVEROS JIMENEZ', 30, 6987889),
    ('LUCIANO JOAQUIN', 'RUIZ DIAZ ALVARADO', 30, 6712171),
    ('MAURICIO JAVIER', 'SERVIN FERREIRA', 30, 6771163),
    ('DANTE DE JESUS', 'VERA MIERES', 30, 7070833),
    ('INGRID MONSERRATH', 'VERDUN GONZALEZ', 30, 6879393),
    ('EZEQUIEL', 'VILLALBA CHIRIANI', 30, 7551342);

-- Mecánica General 1º A (curso_id = 41)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('DIMAS DANIEL', 'ALEGRE BALBUENA', 41, 6852400),
    ('LARA ISABEL', 'ARECO ALEGRE', 41, 7086440),
    ('ALAN ARTURO', 'CABALLERO ALDERETE', 41, 6987644),
    ('EMERSON JAVIER', 'COLMAN LEZCANO', 41, 6781038),
    ('OSLIAN AARON', 'CORRO POSSAMAI', 41, 4141479),
    ('BARBARA XIMENA', 'ENCISO GIMENEZ', 41, 50704022),
    ('JOSE MAURICIO', 'ESCOBAR SERVIN', 41, 6854249),
    ('AUGUSTO RAMON', 'GALEANO BENITEZ', 41, 7201107),
    ('SAMIRA', 'GARCIA MELLO', 41, 8187103),
    ('RONALD JESUS MARIA', 'IRALA ARAUJO', 41, 7218735),
    ('CESAR IVAN', 'LEIVA LOPEZ', 41, 7521573),
    ('MATHIAS SANTIAGO', 'MARECO AVALOS', 41, 7058029),
    ('VICTOR ALEJANDRO', 'MARECO BENITEZ', 41, 7559969),
    ('FABIANA MAELI', 'MEDINA FLORENTIN', 41, 6761589),
    ('FRANCISCO NAHUEL', 'MENDIETA BAEZ', 41, 7045202),
    ('SANTIAGO NICOLAS', 'NARVAEZ RODRIGUES', 41, 7252528),
    ('SIMONE ARLETTE', 'ORTIZ SAMANIEGO', 41, 7376068),
    ('THIAGO BENJAMIN', 'OSORIO PEREIRA', 41, 6613692),
    ('FULGENCIO DE JESUS', 'PALACIOS PAREDES', 41, 7589654),
    ('MELISSA ARACELI', 'PEREZ PEREZ', 41, 6776096),
    ('TADEO DAVID', 'RECALDE OZORIO', 41, 7330269),
    ('RENATE LUJAN', 'RODRIGUEZ BRITEZ', 41, 7315472),
    ('LUCAS STEVEN', 'SANABRIA BRITOS', 41, 7084903),
    ('GIULIANA LISET', 'SILVESTRI NAVARRO', 41, 7620640),
    ('BIANCA GISSEL', 'SOSA CACERES', 41, 6786152),
    ('LUANA MAGALI', 'SOTELO FERNANDEZ', 41, 6634192),
    ('THIAGO ALBERTO', 'VERA PEREIRA', 41, 7439288),
    ('ALMA MARIA', 'ZARATE FRANCO', 41, 6819705);

-- Mecánica General 1º B (curso_id = 42)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('FABRICIO ARIEL', 'ACOSTA BENITEZ', 42, 8370995),
    ('NOEMI VIOLETA', 'AMARILLA NUÑEZ', 42, 7391135),
    ('MIA ARACELI', 'ARGAÑA ROJAS', 42, 7147366),
    ('ELIAS IMANOL', 'BELLINO FERREIRA', 42, 7172232),
    ('SANTIAGO DANIEL', 'CABALLERO', 42, 7981790),
    ('MAURICIO ISAAC', 'DUARTE CASCO', 42, 7134238),
    ('ALEJANDRO JAVIER', 'ESTIGARRIBIA RODRIGUEZ', 42, 7469688),
    ('ANDREA ELIZABETH', 'FIGUEREDO ROJAS', 42, 6819655),
    ('ESTEBAN ALEJANDRO', 'GAMARRA OCAMPOS', 42, 6784666),
    ('FRANCO', 'GIRET RIOS', 42, 8872594),
    ('AYLEN ABIGAIL', 'GODOY DUARTE', 42, 6626563),
    ('MAURICIO JOAQUIN', 'GOMEZ AGUERO', 42, 8181049),
    ('SANTIAGO FEDERICO', 'LEZCANO VILLALBA', 42, 6786365),
    ('SOFIA AYELEN', 'LOPEZ BENITEZ', 42, 7045473),
    ('BRIAN AUGUSTO', 'MARTINEZ GOMEZ', 42, 6783469),
    ('FERNANDO AGUSTIN', 'MARTINEZ SANABRIA', 42, 6779072),
    ('MAXIMILIANO JESUS', 'MASCAREÑO CABRERA', 42, 7100875),
    ('DULCE MARIA', 'NUNES GIMENEZ', 42, 6970343),
    ('ANGEL NICOLAS', 'PEDROZO RIVEROS', 42, 8161905),
    ('VICTOR ADRIAN NAIM', 'RAMIREZ MEDINA', 42, 51123871),
    ('LUCAS SAMUEL', 'ROCHE VERA', 42, 6730545),
    ('PAULA ALEXANDRA', 'RODRIGUEZ HAEDO', 42, 6929314),
    ('AARON FABIAN', 'RUIZ DIAZ NUÑEZ', 42, 6847418),
    ('LUZ FERNANDA', 'SANDOVAL BENITEZ', 42, 7219142),
    ('AITOR', 'SILVERA TORRES', 42, 7416251),
    ('VICTOR STEVEN', 'SOSA RIQUELME', 42, 6899217),
    ('SANTIAGO DANIEL', 'VAZQUEZ MARTINEZ', 42, 6812729),
    ('LEANDRO HYUMA', 'WATANABE ISHIZAKI', 42, 7531145);

-- Mecánica Automotriz 1º A (curso_id = 47)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('ALEX RICARDO', 'ACOSTA MONTIEL', 47, 7116088),
    ('JOEL EZEQUIEL', 'BAEL MEAURIO', 47, 6663091),
    ('ISAIAS ISMAEL', 'BENITEZ GOMEZ', 47, 7553594),
    ('JUAN ANDRES', 'CABRERA QUIÑONEZ', 47, 6988421),
    ('SANTINO DANIEL', 'CARDOZO IBARRA', 47, 7045506),
    ('RODNEY JESUS', 'COLMAN CARRERAS', 47, 6740313),
    ('FACUNDO JAVIER', 'ENCINA MEDINA', 47, 7335541),
    ('BENJAMIN JOSE', 'GARRIDO YAMAMOTO', 47, 7436586),
    ('LUCAS EZEQUIEL', 'GONZALEZ CARDOZO', 47, 7812825),
    ('FRANCO FABRICIO', 'MASI GHIRINGHELLI', 47, 7102068),
    ('JENNIFER LETICIA', 'MAZO AGUILERA', 47, 6871540),
    ('GERARDO NICOLAS', 'MENDIETA PEREIRA', 47, 7963092),
    ('MARIA JAZMIN', 'MENDOZA FERNANDEZ', 47, 7578415),
    ('EVER ISMAEL', 'MENDOZA ROTELA', 47, 6795975),
    ('YERUTI ANAHI', 'MORINIGO CANDIA', 47, 6697729),
    ('MATEO NICOLAS', 'ORIHUELA GUERRERO', 47, 7242872),
    ('ESTEBAN DAVID', 'ORREGO BAEZ', 47, 7008300),
    ('VICTORIA', 'ORUE ARGUELLO', 47, 7037487),
    ('ENZO RAUL', 'PACUA AZCONA', 47, 6886826),
    ('ISAIAS JOSE ALFREDO', 'PEREZ ACOSTA', 47, 6668527),
    ('JUAN NICOLAS', 'ROJAS SAMUDIO', 47, 6655453),
    ('ANGEL DANIEL', 'ROLON PAREDES', 47, 6963742),
    ('MARINA PILAR', 'ROSELLON OVELAR', 47, 8639409),
    ('GUSTAVO ADRIAN', 'RUIZ DIAZ OZUNA', 47, 6640211),
    ('KAREN YANINA', 'RUIZ FLEITAS', 47, 7715981),
    ('JOSHIAS CARLOS', 'SERVIN MEZA', 47, 6923630),
    ('JORGE SEBASTIAN', 'TORRES ESCURRA', 47, 6766584),
    ('MATIAS NICOLAS', 'VEGA VERA', 47, 6631655);

-- Mecánica Automotriz 1º B (curso_id = 48)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('MARCELO DIOSNEL', 'ALONSO BENITEZ', 48, 6978019),
    ('CRISTOPHER EMMANUEL', 'AMARILLA DUARTE', 48, 7874727),
    ('GAEL BENJAMIN', 'ANZOATEGUI LOPEZ', 48, 7683079),
    ('BERNARD TOBIAS', 'ARGUELLO ACOSTA', 48, 6677615),
    ('DYLAN GABRIEL', 'BOGARIN OJEDA', 48, 6918909),
    ('ALDER DAMIAN', 'BRITEZ AMARILLA', 48, 50494537),
    ('DANNA FIORELLA LUJAN', 'CARVALLO SANABRIA', 48, 7146895),
    ('MAURICIO LUIS', 'CASCO HERMOSA', 48, 6792469),
    ('SOFIA VALENTINA', 'FERNANDEZ RODRIGUEZ', 48, 6849334),
    ('ALAN MATIAS', 'FIGUEREDO JARA', 48, 7245061),
    ('DULCE MARIA', 'FLEITAS BORDON', 48, 7812876),
    ('ELIAS SAMUEL', 'FRAGUEDA OCAMPOS', 48, 6872008),
    ('LUIS SAMUEL', 'GIMENEZ LEZCANO', 48, 6976413),
    ('ANGEL JHOSIAS', 'GOMEZ OLAZAR', 48, 6766398),
    ('JEFFERSON LUIS', 'HUANACUNI CHAMBI', 48, 7086924),
    ('SAUL ANDRES', 'LOPEZ ROJAS', 48, 6855462),
    ('ALEJANDRO', 'MEDINA GRILLON', 48, 6936727),
    ('MATIAS EZEQUIEL', 'MENA MARTINEZ', 48, 7328797),
    ('VAHYOLET TATIANA', 'NUÑEZ MARTINEZ', 48, 6970611),
    ('THIAGO BENJAMIN', 'OJEDA ORTIGOZA', 48, 7274742),
    ('MATEO ADALBERTO', 'ORTIZ MORALES', 48, 7086120),
    ('ALEXANDER GABRIEL', 'PINTOS GARCIA', 48, 6795667),
    ('MATIAS MANUEL', 'RODRIGUEZ LOPEZ', 48, 6842347),
    ('LUCAS GABRIEL', 'ROJAS GALEANO', 48, 7059573),
    ('JOHAN SEBASTIAN', 'SANABRIA BORDON', 48, 7002087),
    ('DALILA ABIGAIL', 'SCHUBERT MALDONADO', 48, 7610593),
    ('RENATO LEONEL', 'VERA LACERNA', 48, 7024789),
    ('FELIPE AGUSTIN', 'YBARRA VILLAGRA', 48, 8183134);

-- Química Industrial 1º A (curso_id = 55)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('FLORENCIA ISABEL', 'CABRERA MEDINA', 55, 6886888),
    ('DALIA NICOLE', 'DELFINI RODRIGUEZ', 55, 7725955),
    ('FIORELLA ARAMI', 'FERNANDEZ MUÑOZ', 55, 7022326),
    ('DARA GIULIANA', 'GARCIA ALARCON', 55, 6985596),
    ('GERARDO RENATO', 'GAYOSO LEZCANO', 55, 7652747),
    ('EMILY DANIELA', 'GONZALEZ RAMIREZ', 55, 6886779),
    ('JOSELIN ABIGAIL', 'GONZALEZ RIOS', 55, 7019226),
    ('VALERIA YERUTI', 'LOPEZ SANCHEZ', 55, 7087469),
    ('BIANCA JAZMIN', 'MARTINEZ REYES', 55, 7135570),
    ('RACHEL MARICEL', 'MORALES TORRES', 55, 7357923),
    ('RENATO EMANUEL', 'NOCEDA PAREDES', 55, 7030305),
    ('ANGELICA SUSANA', 'ORUE AYALA', 55, NULL),  -- C.I. 6619509 ya asignado a ORUÉ AYALA, ANGÉLICA SUSANA en Informática 2º A (seed)
    ('SANTIAGO', 'ORUE ORTIZ', 55, 6924479),
    ('SOFIA ISABEL', 'PAREDES IBERBUDEN', 55, 7441769),
    ('JESSICA ISABEL', 'RIVAROLA ALFONZO', 55, 7144183),
    ('AMELIA ITATI', 'RODRIGUEZ CANO', 55, 7738907),
    ('THIAGO DAVID', 'ROJAS ALVAREZ', 55, 6813121),
    ('LUIS MARIA', 'ROLON GOMEZ', 55, 6632926),
    ('CECILIA MONTSERRAT', 'RUIZ FLORENTIN', 55, 7027907),
    ('SOFIA MAGALY', 'SILGUERO ROA', 55, 6814347),
    ('BIANCA ELIZABETH', 'TORALES DUARTE', 55, 6972300),
    ('DALVA CAROLINA', 'VALDERRAMA GALEANO', 55, 7721653),
    ('OSVALDO AMILCAR', 'VAZQUEZ RATZLAFF', 55, 6846647),
    ('ALEXANDER DAVID', 'VILLALBA CABRERA', 55, 7097811),
    ('NINNA IRENE', 'VILLALBA DAVALOS', 55, 7758926),
    ('ASLIN XIOMARA', 'VILLALBA SOLIS', 55, 6666955),
    ('KARIS MANA', 'YOSHIZAKI KUDO', 55, 7041615);

-- Química Industrial 1º B (curso_id = 56)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('ADAM ANDRES', 'AMARILLA GONZALEZ', 56, 6739801),
    ('FELIX SEBASTIAN', 'AYALA DUARTE', 56, 6993082),
    ('DIOGO SANTINO', 'BALBUENA FLORENTIN', 56, 6920559),
    ('MATEO VALENTIN', 'BENITEZ FLEITAS', 56, 7619116),
    ('ANA CELESTE', 'BERNAL GONZALEZ', 56, 7519907),
    ('VANIA JIMENA', 'BOGARIN ALFONSO', 56, 6731321),
    ('MARIA PAZ', 'BRITEZ BENITEZ', 56, 6812526),
    ('DANNA BRIGYT', 'CONDORI ILLACUTIPA', 56, 7152383),
    ('GABRIELA ELIZABETH', 'DIAZ SALINAS', 56, 7332610),
    ('STELLA YERUTI', 'DOMINGUEZ PEREZ', 56, 6888232),
    ('VALERIA', 'FERNANDEZ BAREIRO', 56, 6780576),
    ('FELIPE EDUARDO', 'FERNANDEZ MEDINA', 56, 6856873),
    ('LUCIANA ABIGAIL', 'GONZALEZ QUINTANA', 56, 7068151),
    ('FABRIZIO DANIEL', 'GONZALEZ VILLAVERDE', 56, 7533831),
    ('MIA ROSALBA', 'MARECO RAMIREZ', 56, 6862115),
    ('CESAR AUGUSTO', 'MARTINEZ ROLON', 56, 7638594),
    ('NOELIA BELEN', 'MENDOZA AYALA', 56, 6751727),
    ('LUZ CLARITA', 'NOTARIO RAMIREZ', 56, 6758752),
    ('VALENTINA ANAHI', 'PARRA ZARZA', 56, 6998897),
    ('ZAHIRA', 'PIOCH RIOS', 56, '77938320Z'),
    ('LUANA ABIGAIL', 'RIVAS JIMENEZ', 56, 6917869),
    ('MILENA ABIGAIL', 'ROLON NAVARRO', 56, 6825917),
    ('NAOMI MARINA', 'ROTELA AMARILLA', 56, 7134849),
    ('SERGIO SEBASTIAN', 'SALCEDO NOGUERA', 56, 6988606),
    ('LUANA FIORELLA', 'SILGUERO ALFONZO', 56, 7089457),
    ('THAIS JAZMIN', 'VILLALBA ARAUJO', 56, 6603248),
    ('LUCIA', 'ZARATE MELGAREJO', 56, 6894894);

-- Química Industrial 1º C (curso_id = 57)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci)
    VALUES
    ('JESUS DANIEL', 'ACOSTA ACOSTA', 57, 7984614),
    ('SAMYRA LUZ DARY', 'ADORNO ESTIGARRIBIA', 57, 9089606),
    ('HERNAN NICOLAS', 'BENITEZ VARGAS', 57, 9100855),
    ('YULIANA ABIGAIL', 'CARDOZO BAEZ', 57, 8342195),
    ('DANNA STEFANIA', 'COHENE GALEANO', 57, 6879375),
    ('CLAUDIA SOFIA', 'COSCO BRUSICH', 57, 7741132),
    ('BIANCA YERUTI', 'DE ARAUJO MARTINEZ', 57, 7783915),
    ('JOSE SEBASTIAN', 'ESCOBAR DELGADO', 57, 6888411),
    ('NICOLAS DAVID', 'FERNANDEZ BOGADO', 57, 7053046),
    ('MARIA ALHELI', 'FERNANDEZ JARA', 57, 6692314),
    ('AURORA ELIZABETH', 'FERREIRA MARTINEZ', 57, 7461128),
    ('VALENTINA ANAHI', 'GAYOSO LEZCANO', 57, 7652752),
    ('ANTONELLA', 'GIMENEZ RAMIREZ', 57, 6719724),
    ('RODRIGO NICOLAS', 'GONZALEZ CABALLERO', 57, 6834286),
    ('SERENA CHASMIN', 'MARTINEZ ORTIZ', 57, 7723334),
    ('CESAR AGUSTIN', 'OZORIO GAONA', 57, 7755238),
    ('LUCIA VERONICA', 'PAREDES GALEANO', 57, 7023145),
    ('JAZMIN MARIA ANTONELLA', 'QUIÑONEZ GONZALEZ', 57, 7509950),
    ('SOFIA MAGALI', 'ROA ESPINOLA', 57, 7090850),
    ('NAOMI ROMINA', 'ROJAS RODRIGUEZ', 57, 6730847),
    ('JACQUELINE SOLANGE', 'ROLON GONZALEZ', 57, 7379652),
    ('FIORELLA MAGALI', 'RUIZ DIAZ ORTIZ', 57, 7307860),
    ('FIORELLA LUJAN', 'SANTOS OVIEDO', 57, 6764578),
    ('XIMENA LUCIANA', 'SILVA PORTILLO', 57, 7105398),
    ('LUCIA JOSEFINA', 'TORRES OCAMPO', 57, 6743961),
    ('MAIIA', 'VASILEVA', 57, 9211875),
    ('KAVY DOMINIQUE', 'VELLACICH UDRIZAR', 57, 6964850),
    ('JULIA', 'VENIALGO STRAHM', 57, 7615856);

-- ========================================
-- SALAS
-- ========================================
INSERT INTO sala (nombre, especialidad_id) VALUES
-- Plan Comun
("PC 01", null),
("PC 02", null),
("PC 03", null),
("PC 04", null),
("PC 05", null),
("PC 06", null),
("PC 07", null),
("PC 08", null),
("PC 09", null),
("PC 10", null),

-- Informática
("Aula INF-1º", 5),
("Aula INF-2º", 5),
("Aula INF-3º", 5),
("Aula INF-N", 5),
("Aula INF-T", 5),
("Lab INF-S", 5),
("Lab INF-H", 5),
("INF-CDI", 5),
("INF-CDT", 5),
("INF-PEC", 5),

-- Construcciones Civiles
    ('PC-S1', (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),
    ('PC-S4', (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),
    ('S1', (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),
    ('S2', (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),
    ('S3', (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),
    ('S4', (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),
    ('S5', (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),
    ('S6', (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),
    ('S7', (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),
    ('S8', (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),
    ('SF', (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')),

-- Electrónica
    ('Comp.', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('ESP 1', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('ESP 2', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('ESP 3', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('GAB', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('LAB', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('Lab 3 & 2', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('S1', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('S3', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('S4', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('S7', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('S8', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('SCIEN', (SELECT id FROM especialidad WHERE nombre = 'Electrónica')), ('SFIS', (SELECT id FROM especialidad WHERE nombre = 'Electrónica'));

-- ========================================
-- RELACIONES COMPLEJAS
-- ========================================
-- Usuario - Materia
INSERT INTO asignacion (usuario_id, materia_id, curso_base_id) VALUES
    -- Informatica
        (16, 1, 1), (16, 1, 2),
        -- Antropologia 1ro A/B - Emilce Jara

        (24, 2, 1), (24, 2, 2), (24, 2, 3),
        (24, 2, 4), (24, 2, 5), (24, 2, 6),
        -- Ciencias 1ro, 2do, 3ro A/B - Laura Rivas

        (15, 3, 5), (15, 3, 6),
        -- Economia y Gestion 3ro A/B - Daniel Lenguaza

        (18, 4, 1), (18, 4, 3), (18, 4, 5),
        -- Educacion Fisica 1ro, 2do, 3ro A - Gerardo Ovelar

        (35, 4, 2),
        -- Educacion Fisica 1ro B - Chavez

        (36, 4, 4),
        -- Educacion Fisica 2do B - Mequer

        (29, 4, 6),
        -- Educacion Fisica 3ro B - Oscar Villasanti

        (27, 5, 3), (27, 5, 4),
        -- Educacion Vial 2do A/B - Mirian Montania

        (27, 6, 1), (27, 6, 2),
        -- Etica 1ro A/B - Mirian Montania

        (13, 7, 1), (13, 7, 2),
        (13, 7, 3), (13, 7, 4),
        -- Fisica 1ro, 2do A/B - Claudia Burgos

        (34, 8, 1), (34, 8, 3), (34, 8, 4),
        -- Guarani 1ro A, 2do A/B - Zully Nuñez

        (30, 8, 2),
        -- Guarani 1ro B - Romy Aguilera

        (27, 9, 1), (27, 9, 3), (27, 9, 5),
        -- Historia 1ro, 2do, 3ro A - Mirian Montania

        (21, 9, 2), (21, 9, 6),
        -- Historia 1ro, 3ro B - Gustavo Ramirez

        (10, 9, 4),
        -- Historia 2do B - Abner Alcaraz

        (11, 10, 1), (11, 10, 2),
        (11, 10, 3), (11, 10, 4),
        -- Ingles 1ro, 2do A/B - Alcira Caceres

        (12, 11, 1), (12, 11, 2), (12, 11, 3),
        (12, 11, 4), (12, 11, 5), (12, 11, 6),
        -- Matematica Comun 1ro, 2do, 3ro A/B - Andres Rojas

        (20, 12, 1),
        -- Orientacion 1ro A - Graciela Maidana

        (31, 12, 2), (31, 12, 5), (31, 12, 6),
        -- Orientacion 1ro B, 3ro A/B - Ruth Estigarribia

        (22, 13, 5),
        -- Psicologia 3ro A - Irma Cardozo

        (20, 13, 5),
        -- Psicologia 3ro B - Graciela Maidana

        (28, 14, 1), (28, 14, 3),
        -- Quimica 1ro, 2do A - Oscar Ibarrola

        (26, 14, 2), (26, 14, 4),
        -- Quimica 2do B - Luz Angulo

        (25, 15, 3), (25, 15, 4),
        -- Administracion Financiera 2do A/B - Lourdes Galeano

        (32, 15, 5), (32, 15, 6),
        -- Administracion Financiera 3ro A/B - Ruth Roman

        (33, 16, 1), (33, 16, 2), (33, 16, 3),
        (33, 16, 4), (33, 16, 5), (33, 16, 6),
        -- Literatura 1ro, 2do, 3ro A/B - Susana Alvarenga

        (19, 41, 1), (19, 41, 2), (19, 41, 3),
        (19, 41, 4), (19, 41, 5), (19, 41, 6),
        -- Algoritmica 1ro, 2do, 3ro A/B - Graciela Lopez

        (17, 42, 5), (17, 42, 6),
        -- Laboratorio Android 3ro A/B - Federico Gonzalez

        (17, 43, 5), (17, 43, 6),
        -- Laboratorio Java 3ro A/B - Federico Gonzalez

        (14, 44, 1), (14, 44, 2),
        -- Laboratorio Linux 1ro A/B - Cristian Delgado

        (17, 45, 3), (17, 45, 4),
        -- Laboratorio Python 2do A/B - Federico Gonzalez

        (17, 46, 3), (17, 46, 4),
        -- Laboratorio SQL 2do A/B - Federico Gonzalez

        (17, 47, 1), (17, 47, 2),
        -- Laboratorio Web 1ro A/B - Federico Gonzalez

        (12, 48, 1), (12, 48, 2), (12, 48, 3),
        (12, 48, 4), (12, 48, 5), (12, 48, 6),
        -- Matematica Aplicada 1ro, 2do, 3ro A/B - Andres Rojas

        (33, 49, 2),
        -- Plan de Lectura 1ro B - Susana Alvarenga

        (14, 50, 5), (14, 50, 6),
        -- Laboratorio Redes 3ro A/B - Cristian Delgado

        (14, 51, 5), (14, 51, 6),
        -- Seguridad en Riesgos Electricos 3ro A/B - Cristian Delgado

        (23, 52, 1), (23, 52, 2),
        -- Dibujo Tecnico 1ro A/B - Juan Acosta

        (14, 53, 1), (14, 53, 2),
        (14, 53, 3), (14, 53, 4),
        -- Info General 1ro, 2do A/B - Cristian Delgado

        (14, 54, 3), (14, 54, 4);
        -- Laboratorio Hardware 2do A/B - Cristian Delgado

UPDATE asignacion
   SET curso_base_id = 6
 WHERE usuario_id = 20 AND materia_id = 13 AND curso_base_id = 5;
 -- (usuario_id 20 = graciela.maidana; deja intacta la fila de usuario_id 22 = irma.cardozo)

-- 2) A Claudia Burgos (Física) le faltaba la asignación para 3er año
--    (A y B) — el PDF muestra "Física Aplicada" con ella en ambas
--    secciones de 3er año, pero asignacion solo la vinculaba a 1ro/2do.
INSERT IGNORE INTO asignacion (usuario_id, materia_id, curso_base_id) VALUES
    (13, 7, 5), (13, 7, 6);

-- ========================================
-- ASIGNACION — Electricidad (curso_base 16-21)
-- ========================================
-- Fuente: "Horario de Clases 2026" de Electricidad, versión 06/05/2026
-- ("Actualizado", la más reciente de las dos fechas disponibles para esta
-- especialidad), cruzado contra las fichas individuales de cada profesor
-- para confirmar especialidad. curso_base: 16=1roA, 17=1roB, 18=2doA,
-- 19=2doB, 20=3roA, 21=3roB.
INSERT INTO asignacion (usuario_id, materia_id, curso_base_id) VALUES
    (45, 11, 16), (45, 11, 17), (45, 11, 18), (45, 11, 19), (45, 11, 20), (45, 11, 21),
    -- Matemática 1ro,2do,3ro A/B - Liz Mariza Duarte

    (102, 4, 16), (102, 4, 17), (102, 4, 20),
    -- Educ. Física 1ro A/B, 3ro A - Gerardo Ovelar
    (133, 4, 18),
    -- Educ. Física 2do A - Oscar Villasanti
    (87, 4, 19),
    -- Educ. Física 2do B - Adriana Mequer
    (91, 4, 21),
    -- Educ. Física 3ro B - Francisco Molinas

    (28, 101, 16), (28, 101, 17), (28, 101, 18), (28, 101, 19),
    -- Taller 1ro, 2do A/B - Víctor Bogarín (incluye "Taller e Instalac." de 2do A)

    (67, 1, 16),
    -- Antropología 1ro A - Graciela González
    (83, 1, 17),
    -- Antropología 1ro B - Alicia Martínez

    (120, 8, 16),
    -- Guaraní 1ro A - Nidia Samudio
    (75, 8, 17), (75, 8, 19),
    -- Guaraní 1ro B, 2do B - Hernán Jara
    (21, 8, 18),
    -- Guaraní 2do A - Alba Arrúa

    (37, 17, 16),
    -- Informática 1ro A - Gerardo Centurión
    (130, 17, 17), (130, 17, 18), (130, 17, 19),
    -- Informática 1ro B, 2do A/B - Mónica Vera

    (11, 14, 16), (11, 14, 17), (11, 14, 18), (11, 14, 19),
    -- Química 1ro, 2do A/B - Abel Admen

    (107, 10, 16), (107, 10, 17), (107, 10, 18), (107, 10, 19),
    -- Inglés 1ro, 2do A/B - Christian Ramos

    (28, 102, 16), (28, 102, 17), (28, 102, 18), (28, 102, 19),
    -- Diseño 1ro, 2do A/B - Víctor Bogarín
    (145, 102, 20), (145, 102, 21),
    -- Diseño 3ro A/B - Oscar Azuaga

    (74, 12, 16),
    -- Orientación 1ro A - Emilce Jara
    (13, 12, 17),
    -- Orientación 1ro B - Abner Alcaraz
    (82, 12, 20),
    -- Orientación 3ro A - Graciela Maidana
    (19, 12, 21),
    -- Orientación 3ro B - Edgar Aquino

    (28, 103, 16), (28, 103, 17),
    -- Dibujo Técnico 1ro A/B - Víctor Bogarín

    (77, 104, 16), (77, 104, 17),
    -- Laboratorio de Electrotecnia 1ro A/B - Javier López
    (47, 104, 18),
    -- Laboratorio de Electrotecnia 2do A - Jorge Echagüe
    (28, 104, 19),
    -- Laboratorio de Electrotecnia 2do B - Víctor Bogarín
    (145, 104, 20), (145, 104, 21),
    -- Laboratorio de Electrotecnia 3ro A/B - Oscar Azuaga ("Laboratorio" en la grilla)

    (106, 9, 16), (106, 9, 17), (106, 9, 20),
    -- Historia 1ro A/B, 3ro A - Gustavo Ramírez
    (13, 9, 18),
    -- Historia 2do A - Abner Alcaraz
    (44, 9, 19),
    -- Historia 2do B - Cynthia Díaz ("Historia y G.")
    (83, 9, 21),
    -- Historia 3ro B - Alicia Martínez ("Historia y G.")

    (47, 105, 16), (47, 105, 17), (47, 105, 18), (47, 105, 20), (47, 105, 21),
    -- Electrónica 1ro, 2do A, 3ro A/B - Jorge Echagüe
    (28, 105, 19),
    -- Electrónica 2do B - Víctor Bogarín

    (114, 19, 16), (114, 19, 17),
    -- Taller de Mecánica 1ro A/B - Celso Rojas ("Taller Mecánico")

    (106, 6, 16), (106, 6, 17),
    -- Formación Ética y Ciudadana 1ro A/B - Gustavo Ramírez ("Ética")

    (96, 16, 16), (96, 16, 17), (96, 16, 18), (96, 16, 19), (96, 16, 20), (96, 16, 21),
    -- Literatura 1ro, 2do, 3ro A/B - Zully Núñez

    (77, 7, 16), (77, 7, 17),
    -- Física 1ro A/B - Javier López
    (128, 7, 18), (128, 7, 19),
    -- Física 2do A/B - Maria Luz Valiente de Angulo ("Luz Angulo")

    (98, 2, 16), (98, 2, 17), (98, 2, 19), (98, 2, 21),
    -- Ciencias 1ro A/B, 2do B, 3ro B - Hugo Olmedo ("Ciencias N.")
    (111, 2, 18), (111, 2, 20),
    -- Ciencias 2do A, 3ro A - Laura Rivas

    (76, 3, 20), (76, 3, 21),
    -- Economía y Gestión 3ro A/B - Rolando Daniel Lenguaza ("Economía y G.")

    (74, 13, 20),
    -- Psicología 3ro A - Emilce Jara
    (82, 13, 21),
    -- Psicología 3ro B - Graciela Maidana

    (11, 106, 20), (11, 106, 21),
    -- Proyecto 3ro A/B - Abel Admen

    (28, 107, 20), (28, 107, 21),
    -- Optativa 3ro A/B - Víctor Bogarín

    (145, 18, 20), (145, 18, 21),
    -- Instalaciones Industriales 3ro A/B - Oscar Azuaga ("Instalaciones")

    (92, 5, 18),
    -- Educación Vial 2do A - Mirian Montanía
    (67, 5, 19);
    -- Educación Vial 2do B - Graciela González

-- ========================================
-- ASIGNACION — Química Industrial (curso_base 49-57)
-- ========================================
-- Fuente: "Horario de Clases 2026" de Química Industrial, única
-- versión disponible (30/03/2026), cruzado contra las fichas
-- individuales de cada profesor para confirmar especialidad.
-- curso_base: 49=1roA, 50=1roB, 51=1roC, 52=2doA, 53=2doB, 54=2doC,
-- 55=3roA, 56=3roB, 57=3roC.
INSERT INTO asignacion (usuario_id, materia_id, curso_base_id) VALUES
    (118, 1, 49), (48, 1, 50), (74, 1, 51),
    -- Antropología
    (111, 2, 49), (111, 2, 50), (111, 2, 51), (40, 2, 52), (40, 2, 53), (40, 2, 54), (40, 2, 55), (40, 2, 56), (40, 2, 57),
    -- Ciencias N.
    (76, 3, 55), (76, 3, 56), (76, 3, 57),
    -- Economía y G.
    (91, 4, 49), (133, 4, 50), (102, 4, 51), (39, 4, 52), (133, 4, 53), (133, 4, 54), (102, 4, 55), (102, 4, 56), (102, 4, 57),
    -- Educ. Física
    (83, 5, 53), (92, 5, 54),
    -- Educación Vial
    (67, 6, 49), (83, 6, 50), (110, 6, 51),
    -- Formación Ética y Ciudadana (Ética)
    (25, 7, 49), (25, 7, 50), (25, 7, 51), (25, 7, 52), (25, 7, 53), (25, 7, 54),
    -- Física
    (120, 8, 49), (75, 8, 50), (120, 8, 51), (120, 8, 52), (75, 8, 53), (120, 8, 54),
    -- Guaraní
    (62, 9, 49), (62, 9, 50), (13, 9, 51), (62, 9, 52), (62, 9, 53), (62, 9, 54), (62, 9, 55), (83, 9, 56), (62, 9, 57),
    -- Historia / Historia y G.
    (65, 10, 49), (65, 10, 50), (65, 10, 51), (65, 10, 52), (33, 10, 53), (65, 10, 54),
    -- Inglés
    (53, 11, 49), (60, 11, 50), (53, 11, 51), (45, 11, 52), (53, 11, 53), (53, 11, 54), (45, 11, 55), (53, 11, 56), (53, 11, 57),
    -- Matemática Común
    (51, 12, 49), (51, 12, 50), (110, 12, 51), (51, 12, 56), (82, 12, 55), (82, 12, 57),
    -- Orientación
    (127, 13, 55), (127, 13, 56), (35, 13, 57),
    -- Psicología
    (27, 16, 49), (27, 16, 50), (27, 16, 51), (27, 16, 52), (27, 16, 53), (27, 16, 54), (27, 16, 55), (27, 16, 56), (27, 16, 57),
    -- Literatura
    (134, 121, 49), (129, 121, 50), (134, 121, 51),
    -- Química General
    (116, 122, 49), (116, 122, 50), (108, 122, 51),
    -- Química Práctica
    (108, 123, 52), (57, 123, 53), (54, 123, 54),
    -- Química Analítica
    (73, 124, 52), (73, 124, 53), (116, 124, 54),
    -- Fisicoquímica
    (73, 125, 52), (73, 125, 53),
    -- Operaciones Unitarias
    (73, 126, 52), (73, 126, 53), (73, 126, 54),
    -- Análisis Instrumental
    (40, 127, 49), (40, 127, 50), (40, 127, 51),
    -- Recursos Naturales
    (108, 128, 49), (108, 128, 50), (108, 128, 51),
    -- Seguridad e Higiene
    (64, 129, 52), (64, 129, 53), (64, 129, 54),
    -- Taller (Química)
    (116, 130, 55), (116, 130, 56), (116, 130, 57),
    -- Análisis Industrial
    (105, 131, 55), (54, 131, 56), (54, 131, 57),
    -- Microbiología
    (103, 132, 55), (103, 132, 56), (103, 132, 57),
    -- Tecnología y Análisis de Alimentos
    (64, 133, 55), (64, 133, 56), (64, 133, 57),
    -- Energía
    (93, 134, 55), (73, 134, 56), (93, 134, 57),
    -- Proyecto Industrial
    (93, 135, 55), (93, 135, 56), (93, 135, 57),
    -- Plan Optativo
    (93, 136, 55), (93, 136, 56), (93, 136, 57),
    -- Proyecto Educativo (Química)
    (93, 137, 55), (93, 137, 56), (93, 137, 57),
    -- Tecnología (Química)
    (100, 138, 49), (68, 138, 50), (38, 138, 51);
    -- Dibujo Técnico (Química Industrial)

-- ========================================
-- ASIGNACION — Construcciones Civiles (curso_base 7-15)
-- ========================================
-- Fuente: "Horario de Clases 2026" de Construcciones Civiles.
-- 1ro y 3ro: versión 12/05/2026 (más reciente disponible para esos
-- cursos). 2do: solo existe la versión 30/03/2026 (no se encontró
-- actualización posterior para 2do), es la única fuente y por lo
-- tanto la vigente. Cruzado contra fichas individuales.
-- curso_base: 7=1roA, 8=1roB, 9=1roC, 10=2doA, 11=2doB, 12=2doC,
-- 13=3roA, 14=3roB, 15=3roC.
INSERT INTO asignacion (usuario_id, materia_id, curso_base_id) VALUES
    (128, 14, 7), (122, 14, 8), (128, 14, 9), (128, 14, 10), (128, 14, 11), (128, 14, 12),
    -- Química
    (75, 8, 7), (21, 8, 8), (120, 8, 9), (120, 8, 10), (21, 8, 11), (75, 8, 12),
    -- Guaraní
    (126, 11, 7), (45, 11, 8), (126, 11, 9), (126, 11, 10), (45, 11, 11), (126, 11, 12), (126, 11, 13), (45, 11, 14), (126, 11, 15),
    -- Matemática Común
    (67, 6, 7), (83, 6, 8), (67, 6, 9),
    -- Ética / Formación Ética y Ciudadana
    (68, 62, 7), (100, 62, 8), (34, 62, 9), (34, 62, 10), (34, 62, 11), (34, 62, 12), (38, 62, 13), (34, 62, 14), (38, 62, 15),
    -- Laboratorio (Construcciones)
    (29, 7, 7), (29, 7, 8), (29, 7, 9), (29, 7, 10), (59, 7, 11), (77, 7, 12),
    -- Física
    (68, 63, 7), (100, 63, 8), (34, 63, 9), (34, 63, 10), (34, 63, 11), (34, 63, 12), (38, 63, 13), (34, 63, 14), (38, 63, 15),
    -- Tecnología (Construcciones)
    (36, 65, 7), (36, 65, 8), (36, 65, 9), (32, 65, 10), (32, 65, 11), (32, 65, 12), (94, 65, 13), (34, 65, 14), (34, 65, 15),
    -- Taller (Construcciones)
    (23, 61, 7), (100, 61, 8), (23, 61, 9), (23, 61, 10), (23, 61, 11), (23, 61, 12), (104, 61, 13), (100, 61, 14), (100, 61, 15),
    -- Proyecto y Dibujo
    (106, 9, 7), (106, 9, 8), (106, 9, 9), (106, 9, 10), (67, 9, 11), (13, 9, 12), (13, 9, 14),
    -- Historia
    (36, 64, 7), (68, 64, 8), (34, 64, 9), (72, 64, 10), (72, 64, 11), (100, 64, 12), (36, 64, 13), (100, 64, 14), (100, 64, 15),
    -- Técnicas Instrumentales
    (92, 12, 7), (110, 12, 8), (13, 12, 9), (18, 12, 13), (18, 12, 14), (18, 12, 15),
    -- Orientación
    (67, 1, 7), (106, 1, 8), (13, 1, 9),
    -- Antropología
    (61, 10, 7), (61, 10, 8), (61, 10, 9), (61, 10, 10), (61, 10, 11), (61, 10, 12),
    -- Inglés
    (102, 4, 7), (102, 4, 8), (102, 4, 9), (102, 4, 10), (39, 4, 11), (91, 4, 12), (102, 4, 13), (133, 4, 14),
    -- Educación Física
    (27, 16, 7), (27, 16, 8), (27, 16, 9), (27, 16, 10), (27, 16, 11), (27, 16, 12), (27, 16, 13), (27, 16, 14), (27, 16, 15),
    -- Literatura
    (77, 66, 10), (77, 66, 11), (77, 66, 12), (77, 66, 13), (77, 66, 14), (77, 66, 15),
    -- Resistencia de Materiales
    (97, 67, 10), (97, 67, 11),
    -- Topografía
    (32, 18, 10), (32, 18, 11), (32, 18, 12), (94, 18, 13), (94, 18, 15),
    -- Instalaciones Industriales
    (92, 5, 10), (13, 5, 11),
    -- Educación Vial
    (72, 68, 13), (72, 68, 14), (72, 68, 15),
    -- AutoCAD
    (32, 69, 13), (32, 69, 14), (32, 69, 15),
    -- Proyecto Educativo (Construcciones)
    (35, 13, 13), (35, 13, 14), (35, 13, 15),
    -- Psicología
    (78, 3, 13), (113, 3, 14), (76, 3, 15);
    -- Economía y Gestión

-- ========================================
-- ASIGNACION — Electromecánica (curso_base 31-36)
-- ========================================
-- Fuente: "Horario de Clases 2026" de Electromecánica. 1ro y 2do:
-- única versión disponible (9/4/2026 y 30/3/2026, sin actualización
-- posterior encontrada para esos cursos). 3ro: existen 3 versiones
-- (30/3, 7/5 y 22/5/2026); se usó la más reciente (22/5/2026), que
-- difiere de las anteriores en que Instalaciones Ind. de 3ro A/B pasó
-- de TSMI Fernando Espinoza a Lic. Robert Caballero. Cruzado contra
-- fichas individuales para confirmar especialidad.
-- curso_base: 31=1roA, 32=1roB, 33=2doA, 34=2doB, 35=3roA, 36=3roB.
INSERT INTO asignacion (usuario_id, materia_id, curso_base_id) VALUES
    (108, 14, 31), (108, 14, 32), (108, 14, 33), (108, 14, 34),
    -- Química
    (100, 147, 31), (100, 147, 32),
    -- Dibujo Técnico (Electromecánica)
    (109, 141, 31), (109, 141, 32), (109, 141, 33), (109, 141, 34), (109, 141, 35), (109, 141, 36),
    -- Refrigeración
    (130, 17, 31), (130, 17, 32), (130, 17, 33), (130, 17, 34),
    -- Informática
    (41, 7, 31), (41, 7, 32), (41, 7, 33), (41, 7, 34),
    -- Física
    (40, 2, 31), (98, 2, 32), (40, 2, 33), (98, 2, 34), (98, 2, 36),
    -- Ciencias
    (87, 4, 31), (133, 4, 32), (87, 4, 33), (91, 4, 34), (87, 4, 35), (102, 4, 36),
    -- Educación Física
    (67, 6, 31), (67, 6, 32),
    -- Formación Ética y Ciudadana (Ética)
    (67, 1, 31), (110, 1, 32),
    -- Antropología
    (82, 12, 31), (82, 12, 32), (74, 12, 33), (82, 12, 34),
    -- Orientación
    (60, 145, 31), (60, 145, 32), (60, 145, 33), (60, 145, 34), (60, 145, 35), (60, 145, 36),
    -- Electrotecnia (Electromecánica)
    (41, 11, 31), (45, 11, 32), (126, 11, 33), (126, 11, 34), (126, 11, 35), (126, 11, 36),
    -- Matemática Común
    (114, 19, 31), (114, 19, 32), (94, 19, 33), (94, 19, 34),
    -- Taller de Mecánica (Taller Mecánico)
    (94, 18, 31), (30, 18, 32), (50, 18, 33), (30, 18, 34), (30, 18, 35), (30, 18, 36),
    -- Instalaciones Industriales
    (106, 9, 32), (106, 9, 33), (106, 9, 34), (106, 9, 35), (106, 9, 36),
    -- Historia
    (96, 8, 32), (67, 8, 33), (12, 8, 34),
    -- Guaraní
    (96, 16, 32), (27, 16, 33), (96, 16, 34), (27, 16, 35), (96, 16, 36),
    -- Literatura
    (33, 10, 32), (65, 10, 33), (33, 10, 34),
    -- Inglés
    (95, 146, 33), (95, 146, 34), (95, 146, 35), (95, 146, 36),
    -- Electrónica (Electromecánica)
    (92, 5, 33), (110, 5, 34),
    -- Educación Vial
    (52, 142, 35), (52, 142, 36),
    -- Neumática e Hidráulica
    (30, 144, 35), (30, 144, 36),
    -- Diseño y Mantenimiento Industrial
    (52, 143, 35), (52, 143, 36),
    -- PLC
    (35, 13, 35), (35, 13, 36),
    -- Psicología
    (76, 3, 35), (76, 3, 36);
    -- Economía y Gestión

-- ========================================
-- ASIGNACION -- Electrónica (curso_base 22-30, 1ro/2do/3ro completos)
-- ========================================
-- Fuente: "Horario 2026" de Electrónica del Colegio Técnico Nacional
-- de la Asunción (= de la Capital), vigencia 18/05/2026 en adelante --
-- páginas 4-6 de 74e9282b-WhatsApp_Scan_20260908_at_11.11.08_compressed.pdf.
-- Reemplaza integramente la entrega anterior (que solo cubria 1ro/2do,
-- version 30/03/2026).
-- curso_base: 22=1roA, 23=1roB, 24=1roC, 25=2doA, 26=2doB, 27=2doC,
--             28=3roA, 29=3roB, 30=3roC.
INSERT INTO asignacion (usuario_id, materia_id, curso_base_id) VALUES
    (11, 14, 22), (11, 14, 24), (11, 14, 23), (11, 14, 25), (11, 14, 27), (11, 14, 26),
    -- Química
    (121, 16, 22), (27, 16, 24), (17, 16, 23), (27, 16, 25), (27, 16, 27), (27, 16, 26), (27, 16, 28), (17, 16, 30), (27, 16, 29),
    -- Literatura
    (124, 11, 22), (124, 11, 24), (124, 11, 23), (124, 11, 25), (124, 11, 27), (124, 11, 26), (124, 11, 28), (124, 11, 30), (124, 11, 29),
    -- Matemática Común
    (64, 168, 22), (64, 168, 24), (64, 168, 23),
    -- Seguridad e Higiene (Electrónica)
    (102, 4, 22), (91, 4, 24), (91, 4, 23), (87, 4, 25), (102, 4, 28), (102, 4, 30), (46, 4, 29),
    -- Educación Física
    (82, 12, 22), (51, 12, 24), (110, 12, 23), (92, 12, 28), (92, 12, 30), (92, 12, 29),
    -- Orientación
    (131, 161, 22), (131, 161, 24), (42, 161, 23), (95, 161, 25), (95, 161, 27), (125, 161, 26), (101, 161, 28), (101, 161, 30), (101, 161, 29),
    -- Electrónica Analógica (Eca. Analógica)
    (90, 162, 22), (90, 162, 24), (125, 162, 25), (95, 162, 27), (125, 162, 26), (88, 162, 28), (95, 162, 30), (88, 162, 29),
    -- Electrónica Digital (Eca. Digital)
    (131, 164, 22), (131, 164, 24), (131, 164, 23),
    -- Electrotecnia (Electrónica)
    (62, 6, 22), (13, 6, 24), (67, 6, 23),
    -- Formación Ética y Ciudadana (Ética)
    (90, 163, 22), (90, 163, 24), (90, 163, 23), (95, 163, 25), (95, 163, 27), (125, 163, 26), (132, 163, 28), (132, 163, 30), (132, 163, 29),
    -- Laboratorio de Electrónica
    (98, 2, 22), (98, 2, 24), (98, 2, 23), (98, 2, 25), (98, 2, 27), (98, 2, 28), (98, 2, 30), (98, 2, 29),
    -- Ciencias
    (65, 10, 22), (33, 10, 24), (33, 10, 23), (33, 10, 25), (33, 10, 27), (33, 10, 26),
    -- Inglés
    (38, 167, 22), (38, 167, 24), (38, 167, 23),
    -- Dibujo Técnico (Electrónica)
    (67, 9, 22), (67, 9, 24), (13, 9, 23), (83, 9, 25), (13, 9, 27), (13, 9, 26), (13, 9, 28), (83, 9, 30), (67, 9, 29),
    -- Historia
    (114, 7, 22), (71, 7, 24), (114, 7, 23), (29, 7, 25), (29, 7, 27), (29, 7, 26),
    -- Física
    (12, 8, 22), (12, 8, 24), (120, 8, 23), (120, 8, 25), (75, 8, 27), (12, 8, 26),
    -- Guaraní
    (74, 1, 22), (67, 1, 24), (74, 1, 23),
    -- Antropología
    (110, 5, 25), (13, 5, 27), (13, 5, 26),
    -- Educación Vial
    (90, 17, 25), (89, 17, 27), (90, 17, 26), (101, 17, 28), (119, 17, 30), (84, 17, 29),
    -- Informática
    (125, 166, 25), (95, 166, 27), (131, 166, 26), (101, 166, 28), (101, 166, 30), (101, 166, 29),
    -- Electrónica Industrial (Eca. Industrial)
    (42, 165, 25), (95, 165, 27),
    -- Elementos
    (74, 13, 28), (19, 13, 30), (74, 13, 29),
    -- Psicología
    (42, 169, 28), (42, 169, 30), (42, 169, 29),
    -- Optativa (Electrónica)
    (95, 170, 28), (95, 170, 30), (95, 170, 29),
    -- Proyecto (Electrónica)
    (76, 3, 28), (76, 3, 30), (83, 3, 29);
    -- Economía y Gestión

-- ========================================================================
-- Reemplazo de horario_slot para Informática
-- ========================================================================
DELETE hs FROM horario_slot hs
JOIN curso_base cb ON cb.id = hs.curso_base_id
WHERE cb.especialidad_id = 5;

CREATE TEMPORARY TABLE horario_slot_staging (
    profesor_usuario VARCHAR(45) NOT NULL,
    materia_id INT NOT NULL,
    nivel TINYINT NOT NULL,
    seccion ENUM('A', 'B', 'C') NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL,
    hora_numero TINYINT UNSIGNED NOT NULL,
    duracion TINYINT UNSIGNED NOT NULL
);

CREATE TEMPORARY TABLE horario_slot_span (
    n TINYINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO horario_slot_span (n) VALUES (1), (2), (3), (4);

INSERT INTO horario_slot_staging (profesor_usuario, materia_id, nivel, seccion, dia_semana, hora_numero, duracion) VALUES
    -- ============================================================
    -- SECCIÓN A — 1er año
    -- ============================================================
    ('oscar.ibarrola', 14, 1, 'A', 1, 1, 2),       -- Lunes: Química
    ('mirian.montania', 6, 1, 'A', 1, 3, 2),       -- Lunes: Ética
    ('andres.rojas', 11, 1, 'A', 1, 5, 4),         -- Lunes: Mate_Común
    ('federico.gonzalez', 47, 1, 'A', 2, 1, 4),    -- Martes: Laboratorio Web
    ('mirian.montania', 9, 1, 'A', 2, 5, 2),       -- Martes: Historia
    ('zully.nunez', 8, 1, 'A', 2, 7, 2),           -- Martes: Guaraní
    ('cristian.delgado', 53, 1, 'A', 2, 11, 2),    -- Martes tarde: Info Gral
    ('susana.alvarenga', 16, 1, 'A', 3, 1, 4),     -- Miércoles: Literatura
    ('claudia.burgos', 7, 1, 'A', 3, 5, 4),        -- Miércoles: Física
    ('juan.acosta', 52, 1, 'A', 4, 1, 2),          -- Jueves: D. Técnico
    ('laura.rivas', 52, 1, 'A', 4, 3, 2),          -- Jueves: Ciencias
    ('emilce.jara', 1, 1, 'A', 4, 5, 2),           -- Jueves: Antropología
    ('susana.alvarenga', 16, 1, 'A', 4, 7, 1),     -- Jueves: Literatura (1 período)
    ('graciela.lopez', 41, 1, 'A', 5, 1, 4),       -- Viernes: Algorítmica
    ('cristian.delgado', 44, 1, 'A', 5, 5, 4),     -- Viernes: Laboratorio Linux
    ('gerardo.ovelar', 4, 1, 'A', 1, 9, 2),        -- Lunes tarde: E. Física
    ('alcira.caceres', 10, 1, 'A', 2, 9, 2),       -- Martes tarde: Inglés
    ('andres.rojas', 48, 1, 'A', 3, 9, 2),         -- Miércoles tarde: Mate_Aplicada
    ('graciela.maidana', 12, 1, 'A', 3, 11, 2),    -- Miércoles tarde: Orientación

    -- ============================================================
    -- SECCIÓN A — 2do año
    -- ============================================================
    ('graciela.lopez', 41, 2, 'A', 1, 1, 4),
    ('claudia.burgos', 7, 2, 'A', 2, 1, 4),
    ('gerardo.ovelar', 4, 2, 'A', 3, 1, 2),
    ('alcira.caceres', 10, 2, 'A', 3, 3, 2),
    ('susana.alvarenga', 16, 2, 'A', 4, 1, 2),
    ('lourdes.galeano', 15, 2, 'A', 4, 3, 2),
    ('susana.alvarenga', 16, 2, 'A', 5, 1, 2),
    ('laura.rivas', 2, 2, 'A', 5, 3, 2),
    ('zully.nunez', 8, 2, 'A', 1, 5, 2),
    ('federico.gonzalez', 46, 2, 'A', 1, 7, 2),
    ('cristian.delgado', 53, 2, 'A', 2, 5, 2),
    ('andres.rojas', 11, 2, 'A', 2, 7, 4),
    ('oscar.ibarrola', 14, 2, 'A', 3, 5, 4),
    ('federico.gonzalez', 45, 2, 'A', 4, 5, 4),
    ('andres.rojas', 48, 2, 'A', 5, 5, 2),
    ('mirian.montania', 5, 2, 'A', 1, 9, 2),
    ('zully.nunez', 8, 2, 'A', 1, 11, 2),
    -- REVISAR CON LA FUENTE ORIGINAL: en el PDF, Martes 13:00-14:10 (horas 9-10)
    -- aparece con comillas de continuación sin materia visible arriba (posible
    -- error/arrastre del documento original). Lo dejo SIN cargar (antes había
    -- una fila falsa de "Plan de Lectura" ahí que no se ve en ningún lado del PDF).
    -- Si en la realidad SÍ hay clase ahí, avisame qué materia/profesor va.
    ('susana.alvarenga', 16, 2, 'A', 2, 11, 1),    -- Martes tarde: Literatura (1 período)
    ('alcira.caceres', 10, 2, 'A', 3, 9, 2),
    ('mirian.montania', 9, 2, 'A', 3, 11, 2),
    ('cristian.delgado', 54, 2, 'A', 4, 9, 4),     -- Jueves tarde: Laboratorio Hardware

    -- ============================================================
    -- SECCIÓN A — 3er año  (verificado contra el PDF, sin cambios)
    -- ============================================================
    ('andres.rojas', 11, 3, 'A', 1, 1, 4),
    ('ruth.roman', 15, 3, 'A', 2, 1, 2),
    ('mirian.montania', 9, 3, 'A', 2, 3, 2),
    ('federico.gonzalez', 42, 3, 'A', 3, 1, 4),
    ('federico.gonzalez', 43, 3, 'A', 4, 1, 4),
    ('ruth.roman', 15, 3, 'A', 5, 1, 2),
    ('susana.alvarenga', 16, 3, 'A', 5, 3, 2),
    ('graciela.lopez', 41, 3, 'A', 1, 5, 4),
    ('federico.gonzalez', 43, 3, 'A', 2, 5, 4),
    ('cristian.delgado', 50, 3, 'A', 3, 5, 4),
    ('irma.cardozo', 13, 3, 'A', 4, 5, 4),
    ('laura.rivas', 2, 3, 'A', 5, 5, 2),
    ('claudia.burgos', 7, 3, 'A', 5, 7, 2),
    ('daniel.lenguaza', 3, 3, 'A', 1, 9, 4),
    ('susana.alvarenga', 16, 3, 'A', 2, 9, 2),
    ('andres.rojas', 48, 3, 'A', 2, 11, 2),
    ('cristian.delgado', 51, 3, 'A', 3, 9, 4),
    ('ruth.estigarribia', 12, 3, 'A', 4, 9, 2),
    ('gerardo.ovelar', 4, 3, 'A', 5, 9, 2),

    -- ============================================================
    -- SECCIÓN B — 1er año
    -- ============================================================
    ('federico.gonzalez', 47, 1, 'B', 1, 1, 4),
    ('emilce.jara', 1, 1, 'B', 1, 5, 2),
    ('luz.angulo', 14, 1, 'B', 1, 7, 2),           -- Química
    ('andres.rojas', 11, 1, 'B', 1, 9, 4),         -- Mate_Común
    ('graciela.lopez', 41, 1, 'B', 2, 1, 4),
    ('susana.alvarenga', 16, 1, 'B', 2, 5, 2),
    ('mirian.montania', 6, 1, 'B', 2, 7, 2),
    ('alcira.caceres', 10, 1, 'B', 3, 1, 2),
    ('null.chavez', 4, 1, 'B', 3, 3, 2),           -- E. Física
    ('susana.alvarenga', 16, 1, 'B', 3, 5, 2),
    ('ruth.estigarribia', 12, 1, 'B', 3, 7, 2),
    ('gustavo.ramirez', 9, 1, 'B', 3, 9, 2),
    ('claudia.burgos', 7, 1, 'B', 4, 1, 4),
    ('susana.alvarenga', 49, 1, 'B', 4, 5, 2),
    ('cristian.delgado', 53, 1, 'B', 4, 7, 2),
    ('juan.acosta', 52, 1, 'B', 4, 9, 2),
    ('laura.rivas', 2, 1, 'B', 4, 11, 2),
    ('cristian.delgado', 44, 1, 'B', 5, 1, 4),
    ('romy.aguilera', 8, 1, 'B', 5, 5, 2),
    ('andres.rojas', 48, 1, 'B', 5, 7, 2),

    -- ============================================================
    -- SECCIÓN B — 2do año  (la más desordenada del seed anterior)
    -- ============================================================
    ('cristian.delgado', 54, 2, 'B', 1, 1, 4),
    ('federico.gonzalez', 46, 2, 'B', 1, 5, 4),    -- Laboratorio SQL
    ('zully.nunez', 46, 2, 'B', 1, 9, 2),
    ('laura.rivas', 2, 2, 'B', 1, 11, 2),
    ('andres.rojas', 48, 2, 'B', 2, 1, 2),         -- Mate_Aplicada
    ('abner.alcaraz', 9, 2, 'B', 2, 3, 2),         -- Historia
    ('zully.nunez', 8, 2, 'B', 2, 5, 2),           -- Guaraní
    ('null.mequer', 4, 2, 'B', 2, 7, 2),           -- E. Física
    ('cristian.delgado', 53, 2, 'B', 2, 9, 2),     -- Info Gral
    ('mirian.montania', 5, 2, 'B', 2, 11, 2),      -- Educación Vial
    ('claudia.burgos', 7, 2, 'B', 3, 1, 4),
    ('alcira.caceres', 10, 2, 'B', 3, 5, 2),       -- Inglés
    ('susana.alvarenga', 16, 2, 'B', 3, 7, 2),     -- Literatura
    ('federico.gonzalez', 45, 2, 'B', 3, 9, 4),
    ('lourdes.galeano', 15, 2, 'B', 4, 1, 2),
    ('luz.angulo', 14, 2, 'B', 4, 3, 2),           -- Química bloque 1
    ('graciela.lopez', 41, 2, 'B', 4, 5, 4),       -- Algorítmica
    ('alcira.caceres', 10, 2, 'B', 4, 9, 2),
    ('luz.angulo', 14, 2, 'B', 4, 11, 2),          -- Química bloque 2 (este ya estaba bien)
    ('andres.rojas', 11, 2, 'B', 5, 1, 4),         -- Mate_Común
    ('susana.alvarenga', 16, 2, 'B', 5, 5, 3),     -- Literatura, 3 períodos

    -- ============================================================
    -- SECCIÓN B — 3er año
    -- ============================================================
    ('daniel.lenguaza', 3, 3, 'B', 1, 1, 4),       -- Economía y Gestión
    ('cristian.delgado', 51, 3, 'B', 1, 5, 4),     -- Seguridad en Riesgos
    ('graciela.maidana', 13, 3, 'B', 1, 9, 2),     -- Psicología bloque 1
    ('susana.alvarenga', 16, 3, 'B', 2, 1, 4),     -- Literatura
    ('andres.rojas', 48, 3, 'B', 2, 5, 2),         -- Mate_Aplicada
    ('claudia.burgos', 7, 3, 'B', 2, 7, 2),        -- Física Aplicada
    ('federico.gonzalez', 43, 3, 'B', 2, 9, 4),    -- Laboratorio Java
    ('cristian.delgado', 50, 3, 'B', 3, 1, 4),
    ('federico.gonzalez', 43, 3, 'B', 3, 5, 4),    -- Laboratorio Java
    ('ruth.estigarribia', 12, 3, 'B', 3, 9, 2),
    ('gustavo.ramirez', 9, 3, 'B', 3, 11, 2),      -- Historia
    ('oscar.villasanti', 4, 3, 'B', 3, 13, 2),
    ('ruth.roman', 15, 3, 'B', 4, 1, 4),
    ('andres.rojas', 11, 3, 'B', 4, 5, 4),         -- Mate_Común
    ('federico.gonzalez', 42, 3, 'B', 4, 9, 4),    -- Laboratorio Android
    ('laura.rivas', 2, 3, 'B', 5, 1, 2),
    ('graciela.maidana', 13, 3, 'B', 5, 3, 2),     -- Psicología bloque 2
    ('graciela.lopez', 41, 3, 'B', 5, 5, 4);       -- Algorítmica

INSERT IGNORE INTO horario_slot (asignacion_id, usuario_id, curso_base_id, dia_semana, hora_catedra_id, sala_id)
SELECT DISTINCT
    a.id,
    u.id,
    cb.id,
    s.dia_semana,
    hc.id,
    NULL
FROM horario_slot_staging s
JOIN usuario u ON u.usuario = s.profesor_usuario
JOIN curso_base cb ON cb.especialidad_id = 5 AND cb.nivel = s.nivel AND cb.seccion = s.seccion
JOIN asignacion a ON a.usuario_id = u.id AND a.materia_id = s.materia_id AND a.curso_base_id = cb.id
JOIN horario_slot_span span ON span.n <= s.duracion
JOIN hora_catedra hc ON hc.numero = s.hora_numero + (span.n - 1)
ORDER BY cb.nivel, cb.seccion, s.dia_semana, hc.numero;

DROP TEMPORARY TABLE horario_slot_span;
DROP TEMPORARY TABLE horario_slot_staging;

-- ========================================================================
-- Verificación rápida sugerida después de correr esto:
--   SELECT cb.nivel, cb.seccion, COUNT(*) FROM horario_slot hs
--   JOIN curso_base cb ON cb.id = hs.curso_base_id
--   WHERE cb.especialidad_id = 5 GROUP BY cb.nivel, cb.seccion;
-- (comparar cantidad de slots contra lo esperado por el PDF)
-- ========================================================================

-- ========================================================================
-- horario_slot para Electricidad (curso_base 16-21)
-- ========================================================================
-- Fuente: mismos horarios usados para la asignacion de Electricidad (ver
-- 03_curso_base_y_asignacion_electricidad.sql), version 06/05/2026.
-- El documento fuente de Electricidad NO trae columna de aula/sala, asi que
-- sala_id queda NULL para todas las filas (no se inventa ninguna aula).
DELETE hs FROM horario_slot hs
JOIN curso_base cb ON cb.id = hs.curso_base_id
WHERE cb.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Electricidad');

CREATE TEMPORARY TABLE horario_slot_staging (
    profesor_usuario VARCHAR(45) NOT NULL,
    materia_id INT NOT NULL,
    nivel TINYINT NOT NULL,
    seccion ENUM('A', 'B', 'C') NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL,
    hora_numero TINYINT UNSIGNED NOT NULL,
    duracion TINYINT UNSIGNED NOT NULL
);

CREATE TEMPORARY TABLE horario_slot_span (
    n TINYINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO horario_slot_span (n) VALUES (1), (2), (3), (4);

INSERT INTO horario_slot_staging (profesor_usuario, materia_id, nivel, seccion, dia_semana, hora_numero, duracion) VALUES
    ('liz.duarte', 11, 1, 'A', 2, 1, 3),  -- Matematica
    ('gerardo.ovelar', 4, 1, 'A', 4, 1, 2),  -- Educ Fisica
    ('graciela.gonzalez', 1, 1, 'A', 4, 3, 2),  -- Antropologia
    ('victor.bogarin', 101, 1, 'A', 5, 1, 3),  -- Taller
    ('nidia.samudio', 8, 1, 'A', 1, 5, 2),  -- Guarani
    ('gerardo.centurion', 17, 1, 'A', 2, 5, 2),  -- Informatica
    ('christian.ramos', 10, 1, 'A', 4, 5, 2),  -- Ingles
    ('victor.bogarin', 102, 1, 'A', 5, 5, 2),  -- Diseno
    ('abel.admen', 14, 1, 'A', 1, 7, 2),  -- Quimica
    ('emilce.jara', 12, 1, 'A', 4, 7, 2),  -- Orientacion
    ('victor.bogarin', 103, 1, 'A', 5, 7, 2),  -- Dibujo Tecnico
    ('javier.lopez', 104, 1, 'A', 1, 9, 2),  -- Lab Electrotecnia
    ('javier.lopez', 104, 1, 'A', 2, 9, 2),  -- Lab Electrotecnia
    ('jorge.echague', 105, 1, 'A', 3, 9, 2),  -- Electronica
    ('celso.rojas', 19, 1, 'A', 5, 9, 3),  -- Taller Mecanico
    ('javier.lopez', 104, 1, 'A', 3, 11, 2),  -- Lab Electrotecnia
    ('gustavo.ramirez', 9, 1, 'A', 2, 11, 2),  -- Historia
    ('gustavo.ramirez', 6, 1, 'A', 1, 13, 2),  -- Etica
    ('zully.nunez', 16, 1, 'A', 2, 13, 2),  -- Literatura
    ('javier.lopez', 7, 1, 'A', 3, 13, 2),  -- Fisica
    ('hugo.olmedo', 2, 1, 'A', 1, 15, 2),  -- Ciencias N
    ('javier.lopez', 104, 1, 'B', 1, 1, 2),  -- Lab Electrotecnia
    ('jorge.echague', 105, 1, 'B', 1, 3, 2),  -- Electronica
    ('javier.lopez', 104, 1, 'B', 2, 1, 3),  -- Lab Electrotecnia
    ('victor.bogarin', 101, 1, 'B', 4, 1, 4),  -- Taller
    ('christian.ramos', 10, 1, 'B', 1, 5, 2),  -- Ingles
    ('hugo.olmedo', 2, 1, 'B', 1, 7, 2),  -- Ciencias N
    ('javier.lopez', 7, 1, 'B', 2, 5, 3),  -- Fisica
    ('liz.duarte', 11, 1, 'B', 2, 8, 1),  -- Matematica
    ('victor.bogarin', 102, 1, 'B', 4, 5, 2),  -- Diseno
    ('victor.bogarin', 103, 1, 'B', 4, 7, 2),  -- Dibujo Tecnico
    ('celso.rojas', 19, 1, 'B', 2, 9, 3),  -- Taller Mecanico
    ('monica.vera', 17, 1, 'B', 3, 9, 1),  -- Informatica
    ('liz.duarte', 11, 1, 'B', 4, 9, 3),  -- Matematica
    ('abner.alcaraz', 12, 1, 'B', 3, 10, 2),  -- Orientacion
    ('zully.nunez', 16, 1, 'B', 5, 11, 2),  -- Literatura
    ('christian.ramos', 10, 2, 'A', 1, 1, 2),  -- Ingles
    ('zully.nunez', 16, 2, 'A', 1, 3, 2),  -- Literatura
    ('victor.bogarin', 101, 2, 'A', 2, 1, 3),  -- Taller e Instalac
    ('abel.admen', 14, 2, 'A', 3, 1, 2),  -- Quimica
    ('abner.alcaraz', 9, 2, 'A', 3, 3, 2),  -- Historia
    ('laura.rivas', 2, 2, 'A', 4, 1, 2),  -- Ciencias N
    ('abel.admen', 14, 2, 'A', 4, 3, 2),  -- Quimica
    ('jorge.echague', 104, 2, 'A', 1, 5, 2),  -- Lab Electrotecnia
    ('jorge.echague', 105, 2, 'A', 2, 5, 3),  -- Electronica
    ('jorge.echague', 104, 2, 'A', 3, 5, 3),  -- Lab Electrotecnia
    ('monica.vera', 17, 2, 'A', 4, 5, 3),  -- Informatica
    ('liz.duarte', 11, 2, 'A', 5, 5, 2),  -- Matematica
    ('maria.valiente', 7, 2, 'A', 1, 9, 4),  -- Fisica
    ('victor.bogarin', 102, 2, 'A', 3, 9, 3),  -- Diseno
    ('alba.arrua', 8, 2, 'A', 5, 9, 3),  -- Guarani
    ('mirian.montania', 5, 2, 'A', 3, 13, 2),  -- Educacion Vial
    ('oscar.villasanti', 4, 2, 'A', 5, 13, 2),  -- Educ Fisica
    ('christian.ramos', 10, 2, 'A', 3, 15, 2),  -- Ingles
    ('victor.bogarin', 105, 2, 'B', 1, 2, 3),  -- Electronica
    ('christian.ramos', 10, 2, 'B', 2, 1, 2),  -- Ingles
    ('maria.mequer', 4, 2, 'B', 2, 3, 2),  -- Educ Fisica
    ('victor.bogarin', 104, 2, 'B', 3, 1, 3),  -- Lab Electrotecnia
    ('monica.vera', 17, 2, 'B', 1, 5, 2),  -- Informatica
    ('victor.bogarin', 104, 2, 'B', 2, 5, 2),  -- Lab Electrotecnia
    ('liz.duarte', 11, 2, 'B', 3, 5, 2),  -- Matematica
    ('hernan.jara', 8, 2, 'B', 4, 5, 3),  -- Guarani
    ('abel.admen', 14, 2, 'B', 3, 7, 2),  -- Quimica
    ('graciela.gonzalez', 5, 2, 'B', 1, 9, 2),  -- Educacion Vial
    ('abel.admen', 14, 2, 'B', 1, 11, 2),  -- Quimica
    ('maria.valiente', 7, 2, 'B', 2, 9, 3),  -- Fisica
    ('victor.bogarin', 102, 2, 'B', 4, 9, 3),  -- Diseno
    ('victor.bogarin', 101, 2, 'B', 5, 9, 3),  -- Taller
    ('liz.duarte', 11, 2, 'B', 1, 13, 2),  -- Matematica
    ('christian.ramos', 10, 2, 'B', 4, 13, 2),  -- Ingles
    ('hugo.olmedo', 2, 2, 'B', 4, 15, 2),  -- Ciencias N
    ('zully.nunez', 16, 2, 'B', 5, 13, 2),  -- Literatura
    ('cynthia.diaz', 9, 2, 'B', 5, 15, 2),  -- Historia y G
    ('oscar.azuaga', 18, 3, 'A', 1, 1, 2),  -- Instalaciones
    ('graciela.maidana', 12, 3, 'A', 2, 1, 2),  -- Orientacion
    ('laura.rivas', 2, 3, 'A', 3, 1, 2),  -- Ciencias N
    ('rolando.lenguaza', 3, 3, 'A', 4, 1, 2),  -- Economia y G
    ('abel.admen', 106, 3, 'A', 5, 1, 2),  -- Proyecto
    ('oscar.azuaga', 102, 3, 'A', 2, 3, 2),  -- Diseno
    ('rolando.lenguaza', 3, 3, 'A', 3, 3, 2),  -- Economia y G
    ('jorge.echague', 105, 3, 'A', 4, 3, 2),  -- Electronica
    ('emilce.jara', 13, 3, 'A', 5, 3, 2),  -- Psicologia
    ('oscar.azuaga', 18, 3, 'A', 1, 5, 2),  -- Instalaciones
    ('gerardo.ovelar', 4, 3, 'A', 3, 5, 2),  -- Educ Fisica
    ('zully.nunez', 16, 3, 'A', 1, 7, 2),  -- Literatura
    ('liz.duarte', 11, 3, 'A', 3, 7, 2),  -- Matematica
    ('oscar.azuaga', 104, 3, 'A', 1, 9, 4),  -- Laboratorio
    ('victor.bogarin', 107, 3, 'A', 2, 9, 2),  -- Optativa
    ('oscar.azuaga', 104, 3, 'A', 3, 9, 2),  -- Laboratorio
    ('gustavo.ramirez', 9, 3, 'A', 4, 15, 2),  -- Historia y G
    ('jorge.echague', 105, 3, 'B', 2, 1, 2),  -- Electronica
    ('oscar.azuaga', 18, 3, 'B', 3, 1, 2),  -- Instalaciones
    ('hugo.olmedo', 2, 3, 'B', 4, 1, 2),  -- Ciencias N
    ('oscar.azuaga', 104, 3, 'B', 5, 1, 2),  -- Laboratorio
    ('rolando.lenguaza', 3, 3, 'B', 4, 3, 2),  -- Economia y G
    ('edgar.aquino', 12, 3, 'B', 4, 5, 2),  -- Orientacion
    ('graciela.maidana', 13, 3, 'B', 2, 7, 2),  -- Psicologia
    ('francisco.molinas', 4, 3, 'B', 3, 7, 2),  -- Educ Fisica
    ('alicia.martinez', 9, 3, 'B', 4, 7, 2),  -- Historia y G
    ('liz.duarte', 11, 3, 'B', 5, 7, 2),  -- Matematica
    ('victor.bogarin', 107, 3, 'B', 1, 9, 2),  -- Optativa
    ('zully.nunez', 16, 3, 'B', 2, 9, 2),  -- Literatura
    ('rolando.lenguaza', 3, 3, 'B', 4, 9, 2),  -- Economia y G
    ('oscar.azuaga', 102, 3, 'B', 3, 11, 2),  -- Diseno
    ('oscar.azuaga', 104, 3, 'B', 4, 11, 2),  -- Laboratorio
    ('graciela.maidana', 13, 3, 'B', 1, 13, 2),  -- Psicologia
    ('abel.admen', 106, 3, 'B', 1, 15, 2);  -- Proyecto

INSERT IGNORE INTO horario_slot (asignacion_id, usuario_id, curso_base_id, dia_semana, hora_catedra_id, sala_id)
SELECT DISTINCT
    a.id,
    u.id,
    cb.id,
    s.dia_semana,
    hc.id,
    NULL
FROM horario_slot_staging s
JOIN usuario u ON u.usuario = s.profesor_usuario
JOIN curso_base cb ON cb.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Electricidad')
    AND cb.nivel = s.nivel AND cb.seccion = s.seccion
JOIN asignacion a ON a.usuario_id = u.id AND a.materia_id = s.materia_id AND a.curso_base_id = cb.id
JOIN horario_slot_span span ON span.n <= s.duracion
JOIN hora_catedra hc ON hc.numero = s.hora_numero + (span.n - 1)
ORDER BY cb.nivel, cb.seccion, s.dia_semana, hc.numero;

DROP TEMPORARY TABLE horario_slot_span;
DROP TEMPORARY TABLE horario_slot_staging;

-- ========================================================================
-- horario_slot para Química Industrial (curso_base 49-57)
-- ========================================================================
-- Fuente: "Horario de Clases 2026" de Química Industrial, unica version
-- disponible (30/03/2026), paginas 13-15 (1ro/2do/3ro, secciones A/B/C).
-- curso_base: 49=1roA, 50=1roB, 51=1roC, 52=2doA, 53=2doB, 54=2doC,
-- 55=3roA, 56=3roB, 57=3roC.
-- El documento fuente NO trae columna de aula/sala, asi que sala_id queda
-- NULL para todas las filas (no se inventa ninguna aula), igual que en
-- Electricidad.
DELETE hs FROM horario_slot hs
JOIN curso_base cb ON cb.id = hs.curso_base_id
WHERE cb.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Química Industrial');

CREATE TEMPORARY TABLE horario_slot_staging (
    profesor_usuario VARCHAR(45) NOT NULL,
    materia_id INT NOT NULL,
    nivel TINYINT NOT NULL,
    seccion ENUM('A', 'B', 'C') NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL,
    hora_numero TINYINT UNSIGNED NOT NULL,
    duracion TINYINT UNSIGNED NOT NULL
);

CREATE TEMPORARY TABLE horario_slot_span (
    n TINYINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO horario_slot_span (n) VALUES (1), (2), (3), (4), (5), (6), (7), (8);

INSERT INTO horario_slot_staging (profesor_usuario, materia_id, nivel, seccion, dia_semana, hora_numero, duracion) VALUES
    ('angel.ruiz', 1, 1, 'A', 1, 1, 2),  -- Antropologia
    ('ruth.estigarribia', 12, 1, 'A', 2, 1, 2),  -- Orientacion
    ('paolo.zucchini', 121, 1, 'A', 3, 1, 8),  -- Quimica General
    ('daniela.ratzlaff', 128, 1, 'A', 4, 1, 2),  -- Seguridad e Higiene
    ('ana.fernandez', 11, 1, 'A', 1, 3, 2),  -- Matematica
    ('luis.codas', 127, 1, 'A', 2, 3, 2),  -- Recursos Naturales
    ('ana.fernandez', 11, 1, 'A', 4, 3, 2),  -- Matematica
    ('aracely.ortiz', 138, 1, 'A', 1, 5, 4),  -- Dibujo Tecnico
    ('laura.rivas', 2, 1, 'A', 2, 5, 2),  -- Ciencias N
    ('nidia.samudio', 8, 1, 'A', 4, 5, 2),  -- Guarani
    ('graciela.gonzalez', 6, 1, 'A', 2, 7, 2),  -- Etica
    ('paolo.zucchini', 121, 1, 'A', 1, 9, 2),  -- Quimica General
    ('victor.benitez', 7, 1, 'A', 5, 9, 3),  -- Fisica
    ('leticia.bogado', 16, 1, 'A', 1, 11, 2),  -- Literatura
    ('bernarda.gonzalez', 10, 1, 'A', 5, 12, 1),  -- Ingles
    ('pedro.garcete', 9, 1, 'A', 1, 13, 2),  -- Historia
    ('maria.roman', 122, 1, 'A', 5, 13, 3),  -- Quimica Practica
    ('francisco.molinas', 4, 1, 'A', 1, 15, 2),  -- Educ Fisica
    ('luis.codas', 127, 1, 'C', 2, 1, 2),  -- Recursos Naturales
    ('daniel.rios', 6, 1, 'C', 4, 1, 2),  -- Etica
    ('daniel.rios', 12, 1, 'C', 2, 3, 2),  -- Orientacion
    ('daniela.ratzlaff', 128, 1, 'C', 4, 3, 2),  -- Seguridad e Higiene
    ('ana.fernandez', 11, 1, 'C', 2, 5, 3),  -- Matematica
    ('daniela.ratzlaff', 122, 1, 'C', 4, 5, 3),  -- Quimica Practica
    ('ana.fernandez', 11, 1, 'C', 1, 9, 2),  -- Matematica
    ('victor.cerquetti', 138, 1, 'C', 2, 9, 4),  -- Dibujo Tecnico
    ('gerardo.ovelar', 4, 1, 'C', 3, 9, 2),  -- Educ Fisica
    ('abner.alcaraz', 9, 1, 'C', 4, 9, 2),  -- Historia
    ('bernarda.gonzalez', 10, 1, 'C', 5, 9, 3),  -- Ingles
    ('paolo.zucchini', 121, 1, 'C', 1, 11, 6),  -- Quimica General
    ('laura.rivas', 2, 1, 'C', 3, 11, 2),  -- Ciencias Nat
    ('leticia.bogado', 16, 1, 'C', 4, 11, 2),  -- Literatura
    ('victor.benitez', 7, 1, 'C', 5, 12, 3),  -- Fisica
    ('paolo.zucchini', 121, 1, 'C', 3, 13, 4),  -- Quimica General
    ('emilce.jara', 1, 1, 'C', 4, 13, 2),  -- Antropologia
    ('nidia.samudio', 8, 1, 'C', 5, 15, 2),  -- Guarani
    ('victor.benitez', 7, 1, 'B', 1, 1, 3),  -- Fisica
    ('gladys.vallejos', 121, 1, 'B', 3, 1, 4),  -- Quimica General
    ('luis.codas', 127, 1, 'B', 1, 4, 1),  -- Recursos Naturales
    ('bernarda.gonzalez', 10, 1, 'B', 3, 5, 2),  -- Ingles
    ('laura.rivas', 2, 1, 'B', 1, 6, 2),  -- Ciencias Nat
    ('daniela.ratzlaff', 128, 1, 'B', 3, 7, 2),  -- Seguridad e Higiene
    ('pedro.garcete', 9, 1, 'B', 1, 9, 2),  -- Historia
    ('ana.gallardo', 11, 1, 'B', 2, 9, 5),  -- Matematica
    ('gladys.vallejos', 121, 1, 'B', 3, 9, 6),  -- Quimica General
    ('maria.egusquiza', 1, 1, 'B', 4, 9, 2),  -- Antropologia
    ('hernan.jara', 8, 1, 'B', 5, 9, 2),  -- Guarani
    ('alicia.martinez', 6, 1, 'B', 1, 11, 2),  -- Etica
    ('ruth.estigarribia', 12, 1, 'B', 4, 11, 2),  -- Orientacion
    ('raquel.gonzalez', 138, 1, 'B', 5, 11, 4),  -- Dibujo Tecnico
    ('oscar.villasanti', 4, 1, 'B', 1, 13, 2),  -- Educ Fisica
    ('maria.roman', 122, 1, 'B', 4, 13, 3),  -- Quimica Practica
    ('leticia.bogado', 16, 1, 'B', 3, 15, 2),  -- Literatura
    ('luis.codas', 2, 2, 'A', 1, 1, 2),  -- Ciencias Nat
    ('oscar.ibarrola', 125, 2, 'A', 1, 3, 5),  -- Operaciones Unitarias
    ('daniela.ratzlaff', 123, 2, 'A', 2, 1, 7),  -- Quimica Analitica
    ('oscar.ibarrola', 124, 2, 'A', 3, 1, 4),  -- Fisicoquimica
    ('luis.chavez', 4, 2, 'A', 3, 5, 2),  -- Educ Fisica
    ('bernarda.gonzalez', 10, 2, 'A', 3, 7, 2),  -- Ingles
    ('liz.duarte', 11, 2, 'A', 4, 1, 5),  -- Matematica
    ('juan.gonzalez', 129, 2, 'A', 4, 6, 3),  -- Taller
    ('daniela.ratzlaff', 123, 2, 'A', 5, 1, 6),  -- Quimica Analitica
    ('leticia.bogado', 16, 2, 'A', 5, 7, 2),  -- Literatura
    ('oscar.ibarrola', 126, 2, 'A', 1, 9, 4),  -- Analisis Instrumental
    ('alicia.martinez', 5, 2, 'A', 1, 13, 2),  -- Educacion Vial
    ('pedro.garcete', 9, 2, 'A', 1, 15, 2),  -- Historia
    ('victor.benitez', 7, 2, 'A', 3, 9, 4),  -- Fisica
    ('bernarda.gonzalez', 10, 2, 'A', 3, 13, 2),  -- Ingles
    ('nidia.samudio', 8, 2, 'A', 5, 9, 4),  -- Guarani
    ('juan.gonzalez', 129, 2, 'C', 1, 1, 3),  -- Taller
    ('victor.benitez', 7, 2, 'C', 1, 4, 2),  -- Fisica
    ('ana.fernandez', 11, 2, 'C', 1, 6, 3),  -- Matematica
    ('mirian.montania', 5, 2, 'C', 2, 1, 2),  -- Educacion Vial
    ('ana.fernandez', 11, 2, 'C', 2, 3, 2),  -- Matematica
    ('luis.codas', 2, 2, 'C', 4, 1, 2),  -- Ciencias N
    ('nidia.samudio', 8, 2, 'C', 4, 3, 2),  -- Guarani
    ('oscar.ibarrola', 126, 2, 'C', 4, 5, 4),  -- Analisis Instrumental
    ('leticia.bogado', 16, 2, 'C', 1, 9, 2),  -- Literatura
    ('pedro.garcete', 9, 2, 'C', 1, 11, 2),  -- Historia
    ('oscar.ibarrola', 125, 2, 'C', 1, 13, 4),  -- Operaciones Unitarias
    ('igor.fernandez', 123, 2, 'C', 2, 9, 5),  -- Quimica Analitica
    ('bernarda.gonzalez', 10, 2, 'C', 3, 9, 2),  -- Ingles
    ('oscar.villasanti', 4, 2, 'C', 3, 11, 2),  -- Educ Fisica
    ('oscar.ibarrola', 125, 2, 'C', 3, 13, 1),  -- Operaciones Unitarias
    ('victor.benitez', 7, 2, 'C', 3, 14, 3),  -- Fisica
    ('maria.roman', 124, 2, 'C', 4, 9, 4),  -- Fisicoquimica
    ('nidia.samudio', 8, 2, 'C', 4, 13, 2),  -- Guarani
    ('igor.fernandez', 123, 2, 'C', 5, 9, 7),  -- Quimica Analitica
    ('alcira.caceres', 10, 2, 'B', 1, 1, 4),  -- Ingles
    ('hernan.jara', 8, 2, 'B', 1, 5, 4),  -- Guarani
    ('oscar.ibarrola', 125, 2, 'B', 2, 1, 5),  -- Operaciones Unitarias
    ('carmen.franco', 123, 2, 'B', 2, 6, 3),  -- Quimica Analitica
    ('victor.benitez', 7, 2, 'B', 3, 1, 2),  -- Fisica
    ('ana.fernandez', 11, 2, 'B', 3, 3, 6),  -- Matematica
    ('oscar.ibarrola', 126, 2, 'B', 4, 1, 4),  -- Analisis Instrumental
    ('luis.codas', 2, 2, 'B', 4, 5, 2),  -- Ciencias N
    ('carmen.franco', 123, 2, 'B', 4, 7, 2),  -- Quimica Analitica
    ('leticia.bogado', 16, 2, 'B', 5, 1, 2),  -- Literatura
    ('juan.gonzalez', 129, 2, 'B', 5, 3, 3),  -- Taller
    ('alicia.martinez', 5, 2, 'B', 1, 9, 2),  -- Educacion Vial
    ('oscar.villasanti', 4, 2, 'B', 1, 11, 2),  -- Educ Fisica
    ('oscar.ibarrola', 124, 2, 'B', 3, 9, 4),  -- Fisicoquimica
    ('victor.benitez', 7, 2, 'B', 3, 13, 1),  -- Fisica
    ('carmen.franco', 123, 2, 'B', 4, 9, 4),  -- Quimica Analitica
    ('pedro.garcete', 9, 2, 'B', 4, 13, 4),  -- Historia
    ('liz.montiel', 135, 3, 'A', 1, 1, 3),  -- Plan Optativo
    ('maria.roman', 130, 3, 'A', 2, 1, 8),  -- Analisis Industrial
    ('genoveva.valdez', 13, 3, 'A', 3, 1, 4),  -- Psicologia
    ('zonia.ramirez', 131, 3, 'A', 4, 1, 4),  -- Microbiologia
    ('andrea.perez', 132, 3, 'A', 5, 1, 8),  -- Tecnologia y A de Alim
    ('juan.gonzalez', 133, 3, 'A', 1, 4, 3),  -- Energia
    ('liz.montiel', 134, 3, 'A', 3, 5, 2),  -- Proyecto Industrial
    ('rolando.lenguaza', 3, 3, 'A', 4, 5, 4),  -- Economia y G
    ('luis.codas', 2, 3, 'A', 1, 7, 2),  -- Ciencias N
    ('graciela.maidana', 12, 3, 'A', 3, 7, 2),  -- Orientacion
    ('gerardo.ovelar', 4, 3, 'A', 1, 9, 2),  -- Educ Fisica
    ('liz.duarte', 11, 3, 'A', 3, 9, 2),  -- Matematica
    ('andrea.perez', 132, 3, 'A', 5, 9, 2),  -- Tecnologia y A de Alim
    ('pedro.garcete', 9, 3, 'A', 3, 11, 2),  -- Historia y G
    ('leticia.bogado', 16, 3, 'A', 5, 11, 2),  -- Literatura
    ('liz.montiel', 136, 3, 'A', 3, 13, 2),  -- Proyecto Educ
    ('liz.montiel', 134, 3, 'A', 5, 13, 1),  -- Proyecto Industrial
    ('liz.montiel', 137, 3, 'A', 3, 15, 2),  -- Tecnologia
    ('liz.montiel', 137, 3, 'A', 5, 14, 2),  -- Tecnologia
    ('liz.montiel', 134, 3, 'C', 3, 1, 3),  -- Proyecto Industrial
    ('ana.fernandez', 11, 3, 'C', 4, 1, 2),  -- Matematica
    ('liz.montiel', 137, 3, 'C', 3, 4, 1),  -- Tecnologia
    ('juan.gonzalez', 133, 3, 'C', 4, 3, 3),  -- Energia
    ('luis.codas', 2, 3, 'C', 1, 5, 2),  -- Ciencias N
    ('irma.cardozo', 13, 3, 'C', 3, 5, 4),  -- Psicologia
    ('liz.montiel', 135, 3, 'C', 5, 5, 4),  -- Plan Optativo
    ('gerardo.ovelar', 4, 3, 'C', 1, 7, 2),  -- Educ Fisica
    ('maria.roman', 130, 3, 'C', 1, 9, 8),  -- Analisis Industrial
    ('andrea.perez', 132, 3, 'C', 2, 9, 8),  -- Tecnologia y A de Alim
    ('leticia.bogado', 16, 3, 'C', 3, 9, 2),  -- Literatura
    ('andrea.perez', 132, 3, 'C', 4, 9, 2),  -- Tecnologia y A de Alim
    ('liz.montiel', 137, 3, 'C', 5, 9, 2),  -- Tecnologia
    ('liz.montiel', 136, 3, 'C', 3, 11, 2),  -- Proyecto Educ
    ('rolando.lenguaza', 3, 3, 'C', 4, 11, 2),  -- Economia y G
    ('rolando.lenguaza', 3, 3, 'C', 3, 13, 2),  -- Economia y G
    ('igor.fernandez', 131, 3, 'C', 4, 13, 4),  -- Microbiologia
    ('pedro.garcete', 9, 3, 'C', 3, 15, 2),  -- Historia y G
    ('maria.roman', 130, 3, 'B', 1, 1, 7),  -- Analisis Industrial
    ('liz.montiel', 135, 3, 'B', 1, 8, 1),  -- Plan Optativo
    ('ana.fernandez', 11, 3, 'B', 2, 1, 2),  -- Matematica
    ('gerardo.ovelar', 4, 3, 'B', 2, 3, 2),  -- Educ Fisica
    ('oscar.ibarrola', 134, 3, 'B', 2, 6, 3),  -- Proyecto Industrial
    ('andrea.perez', 132, 3, 'B', 3, 1, 8),  -- Tecnologia y A de Alim
    ('genoveva.valdez', 13, 3, 'B', 4, 1, 2),  -- Psicologia
    ('luis.codas', 2, 3, 'B', 4, 3, 2),  -- Ciencias N
    ('alicia.martinez', 9, 3, 'B', 4, 5, 2),  -- Historia y G
    ('ruth.estigarribia', 12, 3, 'B', 4, 7, 2),  -- Orientacion
    ('liz.montiel', 137, 3, 'B', 5, 1, 4),  -- Tecnologia
    ('juan.gonzalez', 133, 3, 'B', 5, 6, 3),  -- Energia
    ('liz.montiel', 137, 3, 'B', 1, 9, 2),  -- Tecnologia
    ('liz.montiel', 136, 3, 'B', 1, 11, 2),  -- Proyecto Educ
    ('igor.fernandez', 131, 3, 'B', 1, 13, 4),  -- Microbiologia
    ('rolando.lenguaza', 3, 3, 'B', 3, 9, 4),  -- Economia y G
    ('genoveva.valdez', 13, 3, 'B', 3, 13, 2),  -- Psicologia
    ('leticia.bogado', 16, 3, 'B', 4, 9, 2),  -- Literatura
    ('andrea.perez', 132, 3, 'B', 4, 11, 2);  -- Tecnologia y A de Alim

INSERT IGNORE INTO horario_slot (asignacion_id, usuario_id, curso_base_id, dia_semana, hora_catedra_id, sala_id)
SELECT DISTINCT
    a.id,
    u.id,
    cb.id,
    s.dia_semana,
    hc.id,
    NULL
FROM horario_slot_staging s
JOIN usuario u ON u.usuario = s.profesor_usuario
JOIN curso_base cb ON cb.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Química Industrial')
    AND cb.nivel = s.nivel AND cb.seccion = s.seccion
JOIN asignacion a ON a.usuario_id = u.id AND a.materia_id = s.materia_id AND a.curso_base_id = cb.id
JOIN horario_slot_span span ON span.n <= s.duracion
JOIN hora_catedra hc ON hc.numero = s.hora_numero + (span.n - 1)
ORDER BY cb.nivel, cb.seccion, s.dia_semana, hc.numero;

DROP TEMPORARY TABLE horario_slot_span;
DROP TEMPORARY TABLE horario_slot_staging;

-- ========================================================================
-- horario_slot para Construcciones Civiles (curso_base 7-15)
-- ========================================================================
-- Fuente: "Horario de Clases 2026" de Construcciones Civiles (paginas 16-20
-- del escaneo). 1ro: version 12/05/2026 (identica a 30/03/2026 en todo lo
-- verificable). 2do: unica version disponible, 30/03/2026. 3ro: version
-- 12/05/2026 (mas reciente; difiere de 30/03/2026 en la distribucion de
-- Psicologia/Historia/Tecnicas Inst. de la SECCION A los jueves).
--
-- Aulas: el documento anota un codigo de aula junto al nombre de la materia
-- en casi todas las celdas (ej. 'QUIMICA S1'). Ninguno de esos codigos tiene
-- el formato compartido 'PC 0x' (con espacio, dos digitos) que ya existe en
-- el seed con especialidad_id NULL, asi que TODOS son aulas propias de
-- Construcciones Civiles (S1..S8, SF, y las variantes 'PC-S1'/'PC-S4' que,
-- pese al prefijo 'PC', tienen un formato distinto al de las salas de plan
-- comun del seed y se reutilizan en horarios distintos de la misma semana,
-- confirmando que son aulas fisicas propias, no una referencia a 'PC 0x').
--
-- NOTA sobre 'Ciencias': la materia 'Ciencias' aparece varias veces en el
-- documento (con Laura Rivas como profesora) pero NO tiene ninguna tripleta
-- en 06_asignacion_construcciones_civiles.sql para ninguna seccion de CC;
-- esas celdas se omiten integramente (no se fabrica una asignacion nueva).
-- Tambien se omiten: Topografia y Educacion Vial en 2do C, e Historia en
-- 3ro A y 3ro C (visibles en el horario pero sin tripleta en la asignacion
-- entregada), y un bloque de Tecnicas Inst. (1ro C) / Instalaciones (3ro B)
-- cuyo profesor visible en la imagen no coincide con el unico profesor ya
-- asignado a esa materia+curso.
--
-- Aulas 'PC 01'..'PC 10' (especialidad_id NULL) ya existen en el seed y NO
-- se duplican aqui.
DELETE hs FROM horario_slot hs
JOIN curso_base cb ON cb.id = hs.curso_base_id
WHERE cb.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles');

CREATE TEMPORARY TABLE horario_slot_staging (
    profesor_usuario VARCHAR(45) NOT NULL,
    materia_id INT NOT NULL,
    nivel TINYINT NOT NULL,
    seccion ENUM('A', 'B', 'C') NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL,
    hora_numero TINYINT UNSIGNED NOT NULL,
    duracion TINYINT UNSIGNED NOT NULL,
    sala_nombre VARCHAR(45) NULL
);

CREATE TEMPORARY TABLE horario_slot_span (
    n TINYINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO horario_slot_span (n) VALUES (1), (2), (3), (4), (5), (6), (7), (8);

INSERT INTO horario_slot_staging (profesor_usuario, materia_id, nivel, seccion, dia_semana, hora_numero, duracion, sala_nombre) VALUES
    ('maria.valiente', 14, 1, 'A', 1, 1, 2, 'S1'),  -- Quimica
    ('esperanza.torales', 11, 1, 'A', 1, 3, 4, 'S1'),  -- Matematica
    ('gustavo.ramirez', 9, 1, 'A', 1, 7, 2, 'S1'),  -- Historia
    ('hernan.jara', 8, 1, 'A', 2, 1, 2, 'S2'),  -- Guarani
    ('graciela.gonzalez', 6, 1, 'A', 2, 3, 2, 'S2'),  -- Etica
    ('alicia.barrios', 61, 1, 'A', 2, 5, 3, 'S3'),  -- Proyecto y Dibujo
    ('leticia.bogado', 16, 1, 'A', 3, 7, 2, 'S5'),  -- Literatura
    ('mirian.montania', 12, 1, 'A', 3, 9, 2, 'S5'),  -- Orientacion
    ('graciela.gonzalez', 1, 1, 'A', 3, 11, 2, 'S5'),  -- Antropologia
    ('gerardo.ovelar', 4, 1, 'A', 3, 13, 2, NULL),  -- Educ Fisica
    ('ismael.garay', 10, 1, 'A', 3, 15, 2, 'S5'),  -- Ingles
    ('raquel.gonzalez', 62, 1, 'A', 4, 1, 3, 'S2'),  -- Laboratorio
    ('raquel.gonzalez', 63, 1, 'A', 4, 4, 3, 'S2'),  -- Tecnologia
    ('tango.carrero', 64, 1, 'A', 4, 7, 1, 'S5'),  -- Tecnicas Instrumentales
    ('tango.carrero', 65, 1, 'A', 4, 9, 4, NULL),  -- Taller
    ('claudia.burgos', 7, 1, 'A', 5, 1, 2, 'SF'),  -- Fisica
    ('liz.duarte', 11, 1, 'B', 1, 1, 4, 'S4'),  -- Matematica
    ('leticia.bogado', 16, 1, 'B', 1, 5, 2, 'S4'),  -- Literatura
    ('daniel.rios', 12, 1, 'B', 1, 7, 2, 'S4'),  -- Orientacion
    ('raquel.gonzalez', 64, 1, 'B', 1, 10, 4, 'S4'),  -- Tecnicas Instrumentales
    ('alicia.martinez', 6, 1, 'B', 1, 15, 2, 'S4'),  -- Etica
    ('aracely.ortiz', 62, 1, 'B', 2, 9, 3, 'S5'),  -- Laboratorio
    ('alba.arrua', 8, 1, 'B', 2, 13, 2, 'S5'),  -- Guarani
    ('ismael.garay', 10, 1, 'B', 2, 15, 2, 'S5'),  -- Ingles
    ('aracely.ortiz', 61, 1, 'B', 3, 9, 3, 'S3'),  -- Proyecto y Dibujo
    ('aracely.ortiz', 63, 1, 'B', 3, 13, 2, 'S3'),  -- Tecnologia
    ('gerardo.ovelar', 4, 1, 'B', 4, 3, 2, NULL),  -- Educ Fisica
    ('claudia.burgos', 7, 1, 'B', 4, 5, 3, 'S4'),  -- Fisica
    ('tango.carrero', 65, 1, 'B', 4, 8, 1, 'S8'),  -- Taller
    ('gustavo.ramirez', 9, 1, 'B', 4, 11, 2, 'S5'),  -- Historia
    ('gustavo.ramirez', 1, 1, 'B', 4, 13, 2, 'S5'),  -- Antropologia
    ('hilda.sanchez', 14, 1, 'B', 4, 15, 2, 'S5'),  -- Quimica
    ('tango.carrero', 65, 1, 'B', 5, 9, 3, 'S8'),  -- Taller
    ('aracely.ortiz', 63, 1, 'B', 5, 13, 2, 'S4'),  -- Tecnologia
    ('claudia.burgos', 7, 1, 'C', 1, 1, 3, 'S2'),  -- Fisica
    ('maximo.caceres', 64, 1, 'C', 1, 4, 1, 'S2'),  -- Tecnicas Instrumentales
    ('maximo.caceres', 62, 1, 'C', 1, 5, 3, 'S2'),  -- Laboratorio
    ('abner.alcaraz', 1, 1, 'C', 1, 9, 2, 'S1'),  -- Antropologia
    ('abner.alcaraz', 12, 1, 'C', 1, 11, 2, 'S1'),  -- Orientacion
    ('alicia.barrios', 61, 1, 'C', 2, 1, 4, 'S3'),  -- Proyecto y Dibujo
    ('ismael.garay', 10, 1, 'C', 2, 5, 2, 'S2'),  -- Ingles
    ('leticia.bogado', 16, 1, 'C', 3, 1, 2, 'S5'),  -- Literatura
    ('maximo.caceres', 63, 1, 'C', 3, 5, 3, 'S6'),  -- Tecnologia
    ('graciela.gonzalez', 6, 1, 'C', 4, 1, 2, 'S1'),  -- Etica
    ('maria.valiente', 14, 1, 'C', 4, 5, 2, 'S1'),  -- Quimica
    ('gustavo.ramirez', 9, 1, 'C', 4, 7, 2, 'S1'),  -- Historia
    ('nidia.samudio', 8, 1, 'C', 4, 9, 2, 'S1'),  -- Guarani
    ('gerardo.ovelar', 4, 1, 'C', 4, 11, 2, NULL),  -- Educ Fisica
    ('tango.carrero', 65, 1, 'C', 5, 3, 5, NULL),  -- Taller
    ('esperanza.torales', 11, 1, 'C', 5, 9, 2, 'S3'),  -- Matematica
    ('tango.carrero', 65, 1, 'C', 5, 13, 4, 'S6'),  -- Taller
    ('maximo.caceres', 62, 2, 'A', 1, 1, 3, 'S6'),  -- Laboratorio
    ('claudia.burgos', 7, 2, 'A', 1, 4, 1, 'S5'),  -- Fisica
    ('lourdes.caceres', 65, 2, 'A', 1, 5, 4, 'S8'),  -- Taller
    ('esperanza.torales', 11, 2, 'A', 1, 9, 3, 'S7'),  -- Matematica
    ('mirian.montania', 5, 2, 'A', 1, 13, 2, 'S1'),  -- Educacion Vial
    ('gustavo.ramirez', 9, 2, 'A', 1, 15, 2, 'S1'),  -- Historia
    ('ismael.garay', 10, 2, 'A', 2, 1, 2, 'S1'),  -- Ingles
    ('maria.valiente', 14, 2, 'A', 2, 5, 4, 'S1'),  -- Quimica
    ('claudia.burgos', 7, 2, 'A', 2, 9, 3, 'S1'),  -- Fisica
    ('alicia.barrios', 61, 2, 'A', 3, 1, 3, 'S3'),  -- Proyecto y Dibujo
    ('maximo.caceres', 63, 2, 'A', 3, 9, 2, 'S4'),  -- Tecnologia
    ('ismael.garay', 10, 2, 'A', 3, 11, 2, 'S4'),  -- Ingles
    ('javier.lopez', 66, 2, 'A', 4, 1, 3, 'S5'),  -- Resistencia de Materiales
    ('benicio.nunez', 67, 2, 'A', 4, 5, 2, 'S5'),  -- Topografia
    ('lourdes.caceres', 18, 2, 'A', 4, 7, 2, 'S5'),  -- Instalaciones Industriales
    ('gerardo.ovelar', 4, 2, 'A', 4, 9, 2, NULL),  -- Educ Fisica
    ('nidia.samudio', 8, 2, 'A', 4, 11, 2, 'S5'),  -- Guarani
    ('leticia.bogado', 16, 2, 'A', 4, 13, 2, 'S5'),  -- Literatura
    ('zulma.ibarra', 64, 2, 'A', 5, 9, 3, 'PC-S4'),  -- Tecnicas Instrumentales
    ('nidia.samudio', 8, 2, 'A', 5, 13, 2, 'PC-S4'),  -- Guarani
    ('lourdes.caceres', 65, 2, 'B', 1, 1, 4, 'S8'),  -- Taller
    ('liz.duarte', 11, 2, 'B', 1, 5, 4, 'S7'),  -- Matematica
    ('maximo.caceres', 62, 2, 'B', 1, 9, 3, 'S6'),  -- Laboratorio
    ('maria.valiente', 14, 2, 'B', 2, 1, 4, 'S4'),  -- Quimica
    ('graciela.gonzalez', 9, 2, 'B', 2, 5, 2, 'S4'),  -- Historia
    ('alba.arrua', 8, 2, 'B', 2, 9, 2, 'S4'),  -- Guarani
    ('ismael.garay', 10, 2, 'B', 2, 11, 2, 'S4'),  -- Ingles
    ('alba.arrua', 8, 2, 'B', 2, 15, 2, 'S4'),  -- Guarani
    ('luis.chavez', 4, 2, 'B', 3, 1, 2, NULL),  -- Educ Fisica
    ('maximo.caceres', 63, 2, 'B', 3, 3, 2, 'S6'),  -- Tecnologia
    ('javier.lopez', 66, 2, 'B', 3, 5, 3, 'S7'),  -- Resistencia de Materiales
    ('abner.alcaraz', 5, 2, 'B', 3, 9, 2, 'S5'),  -- Educacion Vial
    ('leticia.bogado', 16, 2, 'B', 3, 11, 2, 'S1'),  -- Literatura
    ('zulma.ibarra', 64, 2, 'B', 4, 1, 3, 'S7'),  -- Tecnicas Instrumentales
    ('lourdes.caceres', 18, 2, 'B', 4, 5, 2, 'S3'),  -- Instalaciones Industriales
    ('benicio.nunez', 67, 2, 'B', 4, 7, 2, 'S7'),  -- Topografia
    ('mirian.galeano', 7, 2, 'B', 4, 9, 3, 'SF'),  -- Fisica
    ('alicia.barrios', 61, 2, 'B', 5, 1, 7, 'S3'),  -- Proyecto y Dibujo
    ('alicia.barrios', 61, 2, 'C', 1, 1, 6, 'S3'),  -- Proyecto y Dibujo
    ('aracely.ortiz', 64, 2, 'C', 1, 9, 2, 'S1'),  -- Tecnicas Instrumentales
    ('francisco.molinas', 4, 2, 'C', 1, 13, 2, NULL),  -- Educ Fisica
    ('ismael.garay', 10, 2, 'C', 2, 9, 2, 'S7'),  -- Ingles
    ('javier.lopez', 66, 2, 'C', 2, 11, 2, 'S7'),  -- Resistencia de Materiales
    ('maximo.caceres', 62, 2, 'C', 2, 14, 3, 'S6'),  -- Laboratorio
    ('maximo.caceres', 63, 2, 'C', 3, 1, 2, 'S8'),  -- Tecnologia
    ('leticia.bogado', 16, 2, 'C', 3, 3, 2, 'S8'),  -- Literatura
    ('abner.alcaraz', 9, 2, 'C', 3, 5, 2, 'S8'),  -- Historia
    ('lourdes.caceres', 65, 2, 'C', 3, 7, 2, 'S8'),  -- Taller
    ('esperanza.torales', 11, 2, 'C', 3, 9, 3, 'S7'),  -- Matematica
    ('maria.valiente', 14, 2, 'C', 4, 1, 2, 'S3'),  -- Quimica
    ('maria.valiente', 14, 2, 'C', 4, 7, 2, 'S3'),  -- Quimica
    ('hernan.jara', 8, 2, 'C', 5, 3, 2, 'S3'),  -- Guarani
    ('hernan.jara', 8, 2, 'C', 5, 5, 2, 'S2'),  -- Guarani
    ('javier.lopez', 7, 2, 'C', 5, 7, 2, 'S4'),  -- Fisica
    ('jose.pino', 61, 3, 'A', 1, 9, 8, 'S3'),  -- Proyecto y Dibujo
    ('victor.cerquetti', 62, 3, 'A', 2, 1, 3, 'S7'),  -- Laboratorio
    ('victor.cerquetti', 63, 3, 'A', 2, 5, 3, 'S7'),  -- Tecnologia
    ('zulma.ibarra', 68, 3, 'A', 2, 9, 2, 'S2'),  -- AutoCAD
    ('javier.lopez', 66, 3, 'A', 2, 13, 2, 'S7'),  -- Resistencia de Materiales
    ('alice.lopez', 3, 3, 'A', 3, 1, 3, 'S4'),  -- Economia y Gestion
    ('leticia.bogado', 16, 3, 'A', 3, 5, 2, 'S4'),  -- Literatura
    ('justo.mora', 18, 3, 'A', 3, 9, 2, 'S8'),  -- Instalaciones Industriales
    ('justo.mora', 65, 3, 'A', 3, 11, 1, 'S8'),  -- Taller
    ('irma.cardozo', 13, 3, 'A', 4, 1, 3, 'S7'),  -- Psicologia
    ('tango.carrero', 64, 3, 'A', 4, 5, 2, 'S8'),  -- Tecnicas Instrumentales
    ('esperanza.torales', 11, 3, 'A', 4, 9, 3, 'S7'),  -- Matematica
    ('lourdes.caceres', 69, 3, 'A', 5, 1, 2, 'S2'),  -- Proyecto Educativo
    ('javier.lopez', 66, 3, 'A', 5, 3, 4, 'S7'),  -- Resistencia de Materiales
    ('ana.andino', 12, 3, 'A', 5, 9, 2, 'S4'),  -- Orientacion
    ('gerardo.ovelar', 4, 3, 'A', 5, 11, 2, NULL),  -- Educ Fisica
    ('leticia.bogado', 16, 3, 'B', 1, 3, 2, 'S5'),  -- Literatura
    ('javier.lopez', 66, 3, 'B', 1, 5, 3, 'S5'),  -- Resistencia de Materiales
    ('maximo.caceres', 65, 3, 'B', 1, 8, 1, 'S5'),  -- Taller
    ('liz.duarte', 11, 3, 'B', 1, 9, 4, 'S5'),  -- Matematica
    ('maximo.caceres', 65, 3, 'B', 2, 1, 2, 'S6'),  -- Taller
    ('maximo.caceres', 63, 3, 'B', 2, 5, 4, 'S5'),  -- Tecnologia
    ('maximo.caceres', 62, 3, 'B', 2, 9, 3, 'S6'),  -- Laboratorio
    ('irma.cardozo', 13, 3, 'B', 3, 9, 2, 'S2'),  -- Psicologia
    ('zulma.ibarra', 68, 3, 'B', 3, 11, 3, 'S2'),  -- AutoCAD
    ('karill.rojas', 3, 3, 'B', 4, 1, 3, 'S4'),  -- Economia y Gestion
    ('aracely.ortiz', 64, 3, 'B', 4, 5, 3, 'S6'),  -- Tecnicas Instrumentales
    ('lourdes.caceres', 69, 3, 'B', 4, 9, 2, 'S3'),  -- Proyecto Educativo
    ('oscar.villasanti', 4, 3, 'B', 4, 11, 2, NULL),  -- Educ Fisica
    ('javier.lopez', 66, 3, 'B', 4, 13, 2, 'S4'),  -- Resistencia de Materiales
    ('irma.cardozo', 13, 3, 'B', 4, 15, 1, 'S7'),  -- Psicologia
    ('aracely.ortiz', 61, 3, 'B', 5, 1, 8, 'S2'),  -- Proyecto y Dibujo
    ('abner.alcaraz', 9, 3, 'B', 5, 9, 2, 'S1'),  -- Historia
    ('ana.andino', 12, 3, 'B', 5, 11, 2, 'S1'),  -- Orientacion
    ('victor.cerquetti', 63, 3, 'C', 1, 1, 4, 'S7'),  -- Tecnologia
    ('victor.cerquetti', 62, 3, 'C', 1, 5, 4, 'S6'),  -- Laboratorio
    ('maximo.caceres', 65, 3, 'C', 1, 12, 5, 'S8'),  -- Taller
    ('aracely.ortiz', 61, 3, 'C', 2, 1, 8, 'S3'),  -- Proyecto y Dibujo
    ('justo.mora', 18, 3, 'C', 2, 9, 3, 'S1'),  -- Instalaciones Industriales
    ('javier.lopez', 66, 3, 'C', 3, 1, 4, 'S1'),  -- Resistencia de Materiales
    ('esperanza.torales', 11, 3, 'C', 3, 5, 4, 'S1'),  -- Matematica
    ('lourdes.caceres', 69, 3, 'C', 3, 9, 2, 'S3'),  -- Proyecto Educativo
    ('irma.cardozo', 13, 3, 'C', 3, 11, 3, 'S3'),  -- Psicologia
    ('zulma.ibarra', 68, 3, 'C', 4, 9, 4, 'S2'),  -- AutoCAD
    ('rolando.lenguaza', 3, 3, 'C', 5, 1, 4, 'PC-S1'),  -- Economia y Gestion
    ('aracely.ortiz', 64, 3, 'C', 5, 9, 3, 'S2'),  -- Tecnicas Instrumentales
    ('javier.lopez', 66, 3, 'C', 5, 12, 1, 'S6'),  -- Resistencia de Materiales
    ('ana.andino', 12, 3, 'C', 5, 13, 2, 'S4');  -- Orientacion

INSERT IGNORE INTO horario_slot (asignacion_id, usuario_id, curso_base_id, dia_semana, hora_catedra_id, sala_id)
SELECT DISTINCT
    a.id,
    u.id,
    cb.id,
    s.dia_semana,
    hc.id,
    sa.id
FROM horario_slot_staging s
JOIN usuario u ON u.usuario = s.profesor_usuario
JOIN curso_base cb ON cb.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')
    AND cb.nivel = s.nivel AND cb.seccion = s.seccion
JOIN asignacion a ON a.usuario_id = u.id AND a.materia_id = s.materia_id AND a.curso_base_id = cb.id
JOIN horario_slot_span span ON span.n <= s.duracion
JOIN hora_catedra hc ON hc.numero = s.hora_numero + (span.n - 1)
LEFT JOIN sala sa ON sa.nombre = s.sala_nombre
    AND sa.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Construcciones Civiles')
ORDER BY cb.nivel, cb.seccion, s.dia_semana, hc.numero;

DROP TEMPORARY TABLE horario_slot_span;
DROP TEMPORARY TABLE horario_slot_staging;

-- ========================================================================
-- horario_slot para Electromecanica (curso_base 31-36)
-- ========================================================================
-- Fuente: mismos horarios usados para la asignacion de Electromecanica (ver
-- 08_asignacion_electromecanica.sql). 1ro y 2do: version 7/5/2026. 3ro:
-- version 22/5/2026 (Instalaciones Ind. de 3ro A/B con Robert Caballero).
-- El documento fuente NO trae columna de aula/sala, asi que sala_id queda
-- NULL para todas las filas (no se inventa ninguna aula).
DELETE hs FROM horario_slot hs
JOIN curso_base cb ON cb.id = hs.curso_base_id
WHERE cb.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Electromecánica');

CREATE TEMPORARY TABLE horario_slot_staging (
    profesor_usuario VARCHAR(45) NOT NULL,
    materia_id INT NOT NULL,
    nivel TINYINT NOT NULL,
    seccion ENUM('A', 'B', 'C') NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL,
    hora_numero TINYINT UNSIGNED NOT NULL,
    duracion TINYINT UNSIGNED NOT NULL
);

CREATE TEMPORARY TABLE horario_slot_span (
    n TINYINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO horario_slot_span (n) VALUES (1), (2), (3), (4), (5), (6);

INSERT INTO horario_slot_staging (profesor_usuario, materia_id, nivel, seccion, dia_semana, hora_numero, duracion) VALUES
    ('daniela.ratzlaff', 14, 1, 'A', 3, 1, 2),  -- Quimica
    ('aracely.ortiz', 147, 1, 'A', 3, 3, 3),  -- Dibujo Tecnico
    ('jorge.recalde', 141, 1, 'A', 3, 6, 1),  -- Refrigeracion
    ('monica.vera', 17, 1, 'A', 4, 1, 3),  -- Informatica
    ('crispin.coeffier', 7, 1, 'A', 4, 4, 3),  -- Fisica
    ('luis.codas', 2, 1, 'A', 4, 7, 2),  -- Ciencias
    ('celso.rojas', 19, 1, 'A', 4, 9, 2),  -- Taller Mecanico
    ('justo.mora', 18, 1, 'A', 4, 11, 3),  -- Instalaciones Industriales
    ('jorge.recalde', 141, 1, 'A', 4, 14, 3),  -- Refrigeracion
    ('maria.mequer', 4, 1, 'A', 5, 1, 2),  -- Educacion Fisica
    ('graciela.gonzalez', 6, 1, 'A', 5, 3, 2),  -- Etica
    ('graciela.gonzalez', 1, 1, 'A', 5, 5, 2),  -- Antropologia
    ('graciela.maidana', 12, 1, 'A', 5, 7, 2),  -- Orientacion
    ('ana.gallardo', 145, 1, 'A', 1, 9, 3),  -- Electrotecnia
    ('crispin.coeffier', 11, 1, 'A', 1, 12, 5),  -- Matematica Comun
    ('celso.rojas', 19, 1, 'A', 2, 13, 4),  -- Taller Mecanico
    ('liz.duarte', 11, 1, 'B', 3, 1, 3),  -- Matematica Comun
    ('daniela.ratzlaff', 14, 1, 'B', 3, 4, 2),  -- Quimica
    ('aracely.ortiz', 147, 1, 'B', 3, 6, 3),  -- Dibujo Tecnico
    ('daniel.rios', 1, 1, 'B', 2, 7, 2),  -- Antropologia
    ('jorge.recalde', 141, 1, 'B', 5, 1, 4),  -- Refrigeracion
    ('robert.caballero', 18, 1, 'B', 5, 5, 3),  -- Instalaciones Industriales
    ('hugo.olmedo', 2, 1, 'B', 1, 9, 2),  -- Ciencias
    ('graciela.gonzalez', 6, 1, 'B', 1, 11, 2),  -- Etica
    ('zully.nunez', 16, 1, 'B', 1, 13, 2),  -- Literatura
    ('alcira.caceres', 10, 1, 'B', 1, 15, 2),  -- Ingles
    ('gustavo.ramirez', 9, 1, 'B', 2, 9, 2),  -- Historia
    ('zully.nunez', 8, 1, 'B', 2, 11, 2),  -- Guarani
    ('ana.gallardo', 145, 1, 'B', 2, 14, 3),  -- Electrotecnia
    ('crispin.coeffier', 7, 1, 'B', 4, 9, 3),  -- Fisica
    ('oscar.villasanti', 4, 1, 'B', 4, 13, 2),  -- Educacion Fisica
    ('monica.vera', 17, 1, 'B', 5, 9, 3),  -- Informatica
    ('celso.rojas', 19, 1, 'B', 5, 12, 4),  -- Taller Mecanico
    ('graciela.maidana', 12, 1, 'B', 3, 13, 2),  -- Orientacion
    ('daniela.ratzlaff', 14, 2, 'A', 1, 1, 4),  -- Quimica
    ('ana.gallardo', 145, 2, 'A', 1, 5, 4),  -- Electrotecnia
    ('bernarda.gonzalez', 10, 2, 'A', 2, 1, 4),  -- Ingles
    ('maria.mequer', 4, 2, 'A', 2, 5, 2),  -- Educacion Fisica
    ('justo.mora', 19, 2, 'A', 3, 1, 4),  -- Taller Mecanico
    ('mirian.montania', 5, 2, 'A', 3, 5, 2),  -- Educacion Vial
    ('gustavo.ramirez', 9, 2, 'A', 3, 7, 2),  -- Historia
    ('emilce.jara', 12, 2, 'A', 5, 1, 2),  -- Orientacion
    ('ivan.nunez', 146, 2, 'A', 5, 3, 2),  -- Electronica
    ('esperanza.torales', 11, 2, 'A', 5, 5, 3),  -- Matematica Comun
    ('crispin.coeffier', 7, 2, 'A', 1, 9, 2),  -- Fisica
    ('luis.codas', 2, 2, 'A', 1, 11, 2),  -- Ciencias
    ('monica.vera', 17, 2, 'A', 1, 13, 3),  -- Informatica
    ('esperanza.torales', 11, 2, 'A', 2, 9, 2),  -- Matematica Comun
    ('graciela.gonzalez', 8, 2, 'A', 2, 11, 2),  -- Guarani
    ('graciela.gonzalez', 8, 2, 'A', 3, 9, 2),  -- Guarani
    ('leticia.bogado', 16, 2, 'A', 3, 13, 2),  -- Literatura
    ('jorge.recalde', 141, 2, 'A', 4, 9, 5),  -- Refrigeracion
    ('crispin.coeffier', 7, 2, 'A', 4, 14, 3),  -- Fisica
    ('fernando.espinoza', 18, 2, 'A', 5, 9, 6),  -- Instalaciones Industriales
    ('daniela.ratzlaff', 14, 2, 'B', 1, 7, 2),  -- Quimica
    ('esperanza.torales', 11, 2, 'B', 2, 1, 5),  -- Matematica Comun
    ('jorge.recalde', 141, 2, 'B', 2, 6, 1),  -- Refrigeracion
    ('ivan.nunez', 146, 2, 'B', 2, 7, 2),  -- Electronica
    ('jorge.recalde', 141, 2, 'B', 3, 1, 4),  -- Refrigeracion
    ('justo.mora', 19, 2, 'B', 3, 5, 4),  -- Taller Mecanico
    ('crispin.coeffier', 7, 2, 'B', 4, 1, 2),  -- Fisica
    ('alcira.caceres', 10, 2, 'B', 4, 3, 4),  -- Ingles
    ('daniel.rios', 5, 2, 'B', 4, 7, 2),  -- Educacion Vial
    ('romy.aguilera', 8, 2, 'B', 5, 1, 4),  -- Guarani
    ('graciela.maidana', 12, 2, 'B', 5, 5, 2),  -- Orientacion
    ('zully.nunez', 16, 2, 'B', 5, 7, 2),  -- Literatura
    ('daniela.ratzlaff', 14, 2, 'B', 1, 9, 2),  -- Quimica
    ('francisco.molinas', 4, 2, 'B', 1, 11, 2),  -- Educacion Fisica
    ('robert.caballero', 18, 2, 'B', 1, 13, 4),  -- Instalaciones Industriales
    ('crispin.coeffier', 7, 2, 'B', 3, 9, 3),  -- Fisica
    ('robert.caballero', 18, 2, 'B', 3, 15, 2),  -- Instalaciones Industriales
    ('gustavo.ramirez', 9, 2, 'B', 4, 9, 2),  -- Historia
    ('hugo.olmedo', 2, 2, 'B', 4, 11, 2),  -- Ciencias
    ('ana.gallardo', 145, 2, 'B', 4, 13, 4),  -- Electrotecnia
    ('nemesio.fernandez', 142, 3, 'A', 1, 1, 5),  -- Neumatica e Hidraulica
    ('nemesio.fernandez', 143, 3, 'A', 1, 6, 3),  -- PLC
    ('ana.gallardo', 145, 3, 'A', 2, 1, 5),  -- Electrotecnia
    ('robert.caballero', 144, 3, 'A', 3, 1, 4),  -- Diseno y Mantenimiento Industrial
    ('jorge.recalde', 141, 3, 'A', 3, 5, 1),  -- Refrigeracion
    ('robert.caballero', 18, 3, 'A', 4, 1, 4),  -- Instalaciones Industriales
    ('jorge.recalde', 141, 3, 'A', 4, 5, 4),  -- Refrigeracion
    ('esperanza.torales', 11, 3, 'A', 5, 1, 2),  -- Matematica Comun
    ('leticia.bogado', 16, 3, 'A', 5, 3, 2),  -- Literatura
    ('irma.cardozo', 13, 3, 'A', 5, 5, 4),  -- Psicologia
    ('ivan.nunez', 146, 3, 'A', 1, 9, 2),  -- Electronica
    ('maria.mequer', 4, 3, 'A', 1, 11, 2),  -- Educacion Fisica
    ('rolando.lenguaza', 3, 3, 'A', 2, 9, 4),  -- Economia y Gestion
    ('nemesio.fernandez', 143, 3, 'A', 4, 9, 2),  -- PLC
    ('ivan.nunez', 146, 3, 'A', 4, 11, 2),  -- Electronica
    ('robert.caballero', 18, 3, 'A', 4, 13, 2),  -- Instalaciones Industriales
    ('gustavo.ramirez', 9, 3, 'A', 4, 15, 2),  -- Historia
    ('zully.nunez', 16, 3, 'B', 1, 1, 2),  -- Literatura
    ('hugo.olmedo', 2, 3, 'B', 1, 3, 2),  -- Ciencias
    ('rolando.lenguaza', 3, 3, 'B', 1, 5, 4),  -- Economia y Gestion
    ('ivan.nunez', 146, 3, 'B', 2, 3, 4),  -- Electronica
    ('esperanza.torales', 11, 3, 'B', 2, 7, 2),  -- Matematica Comun
    ('ana.gallardo', 145, 3, 'B', 3, 3, 4),  -- Electrotecnia
    ('nemesio.fernandez', 143, 3, 'B', 1, 9, 5),  -- PLC
    ('nemesio.fernandez', 142, 3, 'B', 1, 14, 1),  -- Neumatica e Hidraulica
    ('gustavo.ramirez', 9, 3, 'B', 1, 15, 2),  -- Historia
    ('nemesio.fernandez', 142, 3, 'B', 2, 9, 4),  -- Neumatica e Hidraulica
    ('robert.caballero', 144, 3, 'B', 2, 13, 4),  -- Diseno y Mantenimiento Industrial
    ('robert.caballero', 18, 3, 'B', 3, 9, 6),  -- Instalaciones Industriales
    ('jorge.recalde', 141, 3, 'B', 3, 15, 1),  -- Refrigeracion
    ('irma.cardozo', 13, 3, 'B', 4, 9, 4),  -- Psicologia
    ('gerardo.ovelar', 4, 3, 'B', 4, 13, 2),  -- Educacion Fisica
    ('jorge.recalde', 141, 3, 'B', 5, 9, 4),  -- Refrigeracion
    ('robert.caballero', 144, 3, 'B', 5, 13, 4);  -- Diseno y Mantenimiento Industrial

INSERT IGNORE INTO horario_slot (asignacion_id, usuario_id, curso_base_id, dia_semana, hora_catedra_id, sala_id)
SELECT DISTINCT
    a.id,
    u.id,
    cb.id,
    s.dia_semana,
    hc.id,
    NULL
FROM horario_slot_staging s
JOIN usuario u ON u.usuario = s.profesor_usuario
JOIN curso_base cb ON cb.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Electromecánica')
    AND cb.nivel = s.nivel AND cb.seccion = s.seccion
JOIN asignacion a ON a.usuario_id = u.id AND a.materia_id = s.materia_id AND a.curso_base_id = cb.id
JOIN horario_slot_span span ON span.n <= s.duracion
JOIN hora_catedra hc ON hc.numero = s.hora_numero + (span.n - 1)
ORDER BY cb.nivel, cb.seccion, s.dia_semana, hc.numero;

DROP TEMPORARY TABLE horario_slot_span;
DROP TEMPORARY TABLE horario_slot_staging;

-- ========================================================================
-- horario_slot + sala para Electrónica (curso_base 22-30, 1ro/2do/3ro completos)
-- ========================================================================
-- Fuente: "Horario 2026" de Electrónica del Colegio Técnico Nacional de la
-- Asunción (= de la Capital), vigencia 18/05/2026 en adelante -- páginas 4-6
-- de 74e9282b-WhatsApp_Scan_20260908_at_11.11.08_compressed.pdf. Reemplaza
-- integramente la entrega anterior (solo 1ro/2do, version 30/03/2026).
--
-- Notacion de "Sala" de esta version: "Presencial" junto a un codigo de aula
-- real (ej. "ESP 1", "LAB") => se usa ese codigo como sala_nombre. "Virtual"
-- (sin codigo de aula emparejado) y "Asincrónico" => sala_nombre NULL (no hay
-- aula fisica). "Comp." (Laboratorio de Computacion) => codigo de aula real.
--
-- Catalogo de aulas: toda sala vista en el documento que no sea "PC 0x" (plan
-- comun, ya sembrado con especialidad_id NULL) se agrega como aula interna de
-- Electrónica. No aparecieron en estas paginas los codigos 'Biblio' ni 'Auto'.
DELETE hs FROM horario_slot hs
JOIN curso_base cb ON cb.id = hs.curso_base_id
WHERE cb.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Electrónica');

CREATE TEMPORARY TABLE horario_slot_staging (
    profesor_usuario VARCHAR(45) NOT NULL,
    materia_id INT NOT NULL,
    nivel TINYINT NOT NULL,
    seccion ENUM('A', 'B', 'C') NOT NULL,
    dia_semana TINYINT UNSIGNED NOT NULL,
    hora_numero TINYINT UNSIGNED NOT NULL,
    duracion TINYINT UNSIGNED NOT NULL,
    sala_nombre VARCHAR(45) NULL
);

CREATE TEMPORARY TABLE horario_slot_span (
    n TINYINT UNSIGNED NOT NULL PRIMARY KEY
);

INSERT INTO horario_slot_span (n) VALUES (1), (2), (3), (4), (5), (6);

INSERT INTO horario_slot_staging (profesor_usuario, materia_id, nivel, seccion, dia_semana, hora_numero, duracion, sala_nombre) VALUES
    ('abel.admen', 14, 1, 'A', 1, 1, 2, NULL),  -- Química
    ('silvio.sanchez', 16, 1, 'A', 1, 3, 2, NULL),  -- Literatura
    ('nancy.sosa', 11, 1, 'A', 1, 5, 2, NULL),  -- Matemática Común
    ('juan.gonzalez', 168, 1, 'A', 1, 7, 2, NULL),  -- Seguridad e Higiene (Electrónica)
    ('gerardo.ovelar', 4, 1, 'A', 2, 1, 2, NULL),  -- Educación Física
    ('graciela.maidana', 12, 1, 'A', 2, 3, 2, NULL),  -- Orientación
    ('oscar.villalba', 161, 1, 'A', 2, 5, 3, NULL),  -- Electrónica Analógica (Eca. Analógica)
    ('marta.mojoli', 162, 1, 'A', 3, 1, 2, NULL),  -- Electrónica Digital (Eca. Digital)
    ('oscar.villalba', 164, 1, 'A', 3, 3, 2, NULL),  -- Electrotecnia (Electrónica)
    ('nancy.sosa', 11, 1, 'A', 3, 5, 2, NULL),  -- Matemática Común
    ('pedro.garcete', 6, 1, 'A', 3, 9, 2, NULL),  -- Formación Ética y Ciudadana (Ética)
    ('marta.mojoli', 163, 1, 'A', 4, 1, 6, 'ESP 1'),  -- Laboratorio de Electrónica
    ('oscar.villalba', 164, 1, 'A', 4, 7, 2, 'ESP 1'),  -- Electrotecnia (Electrónica)
    ('hugo.olmedo', 2, 1, 'A', 4, 9, 2, NULL),  -- Ciencias
    ('oscar.villalba', 164, 1, 'A', 4, 11, 2, NULL),  -- Electrotecnia (Electrónica)
    ('bernarda.gonzalez', 10, 1, 'A', 4, 13, 2, NULL),  -- Inglés
    ('victor.cerquetti', 167, 1, 'A', 4, 15, 2, NULL),  -- Dibujo Técnico (Electrónica)
    ('graciela.gonzalez', 9, 1, 'A', 5, 1, 2, NULL),  -- Historia
    ('celso.rojas', 7, 1, 'A', 5, 3, 3, NULL),  -- Física
    ('romy.aguilera', 8, 1, 'A', 5, 6, 2, NULL),  -- Guaraní
    ('emilce.jara', 1, 1, 'A', 5, 9, 2, NULL),  -- Antropología
    ('marta.mojoli', 163, 1, 'C', 1, 1, 6, 'ESP 1'),  -- Laboratorio de Electrónica
    ('oscar.villalba', 164, 1, 'C', 1, 7, 2, 'ESP 1'),  -- Electrotecnia (Electrónica)
    ('ruth.estigarribia', 12, 1, 'C', 1, 9, 2, NULL),  -- Orientación
    ('victor.cerquetti', 167, 1, 'C', 1, 12, 2, NULL),  -- Dibujo Técnico (Electrónica)
    ('felix.huerta', 7, 1, 'C', 1, 14, 3, NULL),  -- Física
    ('oscar.villalba', 164, 1, 'C', 2, 9, 6, NULL),  -- Electrotecnia (Electrónica)
    ('alcira.caceres', 10, 1, 'C', 2, 15, 2, NULL),  -- Inglés
    ('marta.mojoli', 162, 1, 'C', 3, 9, 2, NULL),  -- Electrónica Digital (Eca. Digital)
    ('abel.admen', 14, 1, 'C', 3, 11, 2, NULL),  -- Química
    ('graciela.gonzalez', 9, 1, 'C', 3, 13, 2, NULL),  -- Historia
    ('graciela.gonzalez', 1, 1, 'C', 3, 15, 2, NULL),  -- Antropología
    ('nancy.sosa', 11, 1, 'C', 4, 9, 3, NULL),  -- Matemática Común
    ('hugo.olmedo', 2, 1, 'C', 4, 13, 2, NULL),  -- Ciencias
    ('juan.gonzalez', 168, 1, 'C', 5, 1, 2, NULL),  -- Seguridad e Higiene (Electrónica)
    ('nancy.sosa', 11, 1, 'C', 5, 3, 2, NULL),  -- Matemática Común
    ('oscar.villalba', 161, 1, 'C', 5, 5, 3, NULL),  -- Electrónica Analógica (Eca. Analógica)
    ('leticia.bogado', 16, 1, 'C', 5, 9, 2, NULL),  -- Literatura
    ('abner.alcaraz', 6, 1, 'C', 5, 11, 2, NULL),  -- Formación Ética y Ciudadana (Ética)
    ('romy.aguilera', 8, 1, 'C', 5, 13, 2, NULL),  -- Guaraní
    ('francisco.molinas', 4, 1, 'C', 5, 15, 2, NULL),  -- Educación Física
    ('nidia.samudio', 8, 1, 'B', 1, 3, 2, NULL),  -- Guaraní
    ('daniel.rios', 12, 1, 'B', 1, 5, 2, NULL),  -- Orientación
    ('emilce.jara', 1, 1, 'B', 1, 7, 2, NULL),  -- Antropología
    ('oscar.villalba', 164, 1, 'B', 1, 9, 2, 'ESP 1'),  -- Electrotecnia (Electrónica)
    ('marta.mojoli', 163, 1, 'B', 1, 11, 6, 'ESP 1'),  -- Laboratorio de Electrónica
    ('graciela.gonzalez', 6, 1, 'B', 2, 1, 2, NULL),  -- Formación Ética y Ciudadana (Ética)
    ('alcira.caceres', 10, 1, 'B', 2, 3, 2, NULL),  -- Inglés
    ('susana.alvarenga', 16, 1, 'B', 2, 5, 2, NULL),  -- Literatura
    ('abner.alcaraz', 9, 1, 'B', 3, 1, 2, NULL),  -- Historia
    ('nancy.sosa', 11, 1, 'B', 3, 3, 2, NULL),  -- Matemática Común
    ('osvaldo.cruz', 161, 1, 'B', 3, 6, 2, NULL),  -- Electrónica Analógica (Eca. Analógica)
    ('oscar.villalba', 164, 1, 'B', 3, 9, 4, NULL),  -- Electrotecnia (Electrónica)
    ('juan.gonzalez', 168, 1, 'B', 4, 1, 2, NULL),  -- Seguridad e Higiene (Electrónica)
    ('hugo.olmedo', 2, 1, 'B', 4, 3, 2, NULL),  -- Ciencias
    ('abel.admen', 14, 1, 'B', 4, 5, 2, NULL),  -- Química
    ('nancy.sosa', 11, 1, 'B', 4, 7, 2, NULL),  -- Matemática Común
    ('victor.cerquetti', 167, 1, 'B', 5, 1, 2, NULL),  -- Dibujo Técnico (Electrónica)
    ('francisco.molinas', 4, 1, 'B', 5, 3, 2, NULL),  -- Educación Física
    ('celso.rojas', 7, 1, 'B', 5, 6, 3, NULL),  -- Física
    ('nidia.samudio', 8, 2, 'A', 1, 1, 2, NULL),  -- Guaraní
    ('daniel.rios', 5, 2, 'A', 1, 3, 2, NULL),  -- Educación Vial
    ('claudia.burgos', 7, 2, 'A', 1, 5, 3, NULL),  -- Física
    ('abel.admen', 14, 2, 'A', 1, 9, 2, NULL),  -- Química
    ('ivan.nunez', 161, 2, 'A', 1, 11, 2, NULL),  -- Electrónica Analógica (Eca. Analógica)
    ('alcira.caceres', 10, 2, 'A', 1, 13, 2, NULL),  -- Inglés
    ('marta.mojoli', 17, 2, 'A', 2, 1, 2, NULL),  -- Informática
    ('nancy.sosa', 11, 2, 'A', 2, 3, 3, NULL),  -- Matemática Común
    ('jorge.szwako', 166, 2, 'A', 2, 6, 2, NULL),  -- Electrónica Industrial (Eca. Industrial)
    ('jorge.szwako', 166, 2, 'A', 2, 9, 2, NULL),  -- Electrónica Industrial (Eca. Industrial)
    ('maria.mequer', 4, 2, 'A', 2, 11, 2, NULL),  -- Educación Física
    ('jorge.szwako', 162, 2, 'A', 2, 13, 2, NULL),  -- Electrónica Digital (Eca. Digital)
    ('ivan.nunez', 163, 2, 'A', 3, 1, 6, 'LAB'),  -- Laboratorio de Electrónica
    ('marta.mojoli', 17, 2, 'A', 3, 7, 2, 'ESP 1'),  -- Informática
    ('alcira.caceres', 10, 2, 'A', 4, 1, 2, NULL),  -- Inglés
    ('alicia.martinez', 9, 2, 'A', 4, 3, 2, NULL),  -- Historia
    ('hugo.olmedo', 2, 2, 'A', 4, 5, 2, NULL),  -- Ciencias
    ('abel.admen', 14, 2, 'A', 4, 7, 2, NULL),  -- Química
    ('nancy.sosa', 11, 2, 'A', 5, 1, 2, NULL),  -- Matemática Común
    ('nidia.samudio', 8, 2, 'A', 5, 3, 2, NULL),  -- Guaraní
    ('claudia.burgos', 7, 2, 'A', 5, 5, 2, NULL),  -- Física
    ('osvaldo.cruz', 165, 2, 'A', 5, 7, 2, NULL),  -- Elementos
    ('ivan.nunez', 161, 2, 'A', 5, 9, 5, NULL),  -- Electrónica Analógica (Eca. Analógica)
    ('leticia.bogado', 16, 2, 'A', 5, 15, 2, NULL),  -- Literatura
    ('abner.alcaraz', 9, 2, 'C', 1, 1, 2, 'S3'),  -- Historia
    ('nancy.sosa', 11, 2, 'C', 1, 3, 2, 'ESP 3'),  -- Matemática Común
    ('ivan.nunez', 161, 2, 'C', 1, 5, 4, 'ESP 3'),  -- Electrónica Analógica (Eca. Analógica)
    ('claudia.burgos', 7, 2, 'C', 1, 9, 5, 'SFIS'),  -- Física
    ('ivan.nunez', 161, 2, 'C', 1, 14, 3, 'ESP 3'),  -- Electrónica Analógica (Eca. Analógica)
    ('ivan.nunez', 166, 2, 'C', 2, 9, 4, 'S4'),  -- Electrónica Industrial (Eca. Industrial)
    ('ivan.nunez', 162, 2, 'C', 2, 13, 2, 'S4'),  -- Electrónica Digital (Eca. Digital)
    ('maria.mereles', 17, 2, 'C', 2, 15, 2, 'Comp.'),  -- Informática
    ('nancy.sosa', 11, 2, 'C', 3, 9, 3, 'S1'),  -- Matemática Común
    ('ivan.nunez', 163, 2, 'C', 4, 1, 6, 'LAB'),  -- Laboratorio de Electrónica
    ('ivan.nunez', 165, 2, 'C', 4, 9, 2, 'S8'),  -- Elementos
    ('abel.admen', 14, 2, 'C', 4, 11, 2, 'S8'),  -- Química
    ('hernan.jara', 8, 2, 'C', 4, 13, 2, 'S4'),  -- Guaraní
    ('alcira.caceres', 10, 2, 'C', 4, 15, 2, 'S4'),  -- Inglés
    ('hugo.olmedo', 2, 2, 'C', 5, 9, 2, 'SCIEN'),  -- Ciencias
    ('hernan.jara', 8, 2, 'C', 5, 11, 2, 'S8'),  -- Guaraní
    ('leticia.bogado', 16, 2, 'C', 5, 13, 2, 'S8'),  -- Literatura
    ('abner.alcaraz', 5, 2, 'C', 5, 15, 2, 'S8'),  -- Educación Vial
    ('jorge.szwako', 162, 2, 'B', 1, 3, 2, NULL),  -- Electrónica Digital (Eca. Digital)
    ('jorge.szwako', 161, 2, 'B', 1, 5, 4, NULL),  -- Electrónica Analógica (Eca. Analógica)
    ('alcira.caceres', 10, 2, 'B', 1, 9, 2, NULL),  -- Inglés
    ('nancy.sosa', 11, 2, 'B', 1, 11, 2, NULL),  -- Matemática Común
    ('leticia.bogado', 16, 2, 'B', 1, 13, 2, NULL),  -- Literatura
    ('abner.alcaraz', 5, 2, 'B', 1, 15, 2, NULL),  -- Educación Vial
    ('jorge.szwako', 163, 2, 'B', 2, 1, 5, 'LAB'),  -- Laboratorio de Electrónica
    ('marta.mojoli', 17, 2, 'B', 2, 7, 2, 'ESP 1'),  -- Informática
    ('abner.alcaraz', 9, 2, 'B', 3, 1, 2, NULL),  -- Historia
    ('nancy.sosa', 11, 2, 'B', 3, 3, 2, NULL),  -- Matemática Común
    ('claudia.burgos', 7, 2, 'B', 3, 9, 5, NULL),  -- Física
    ('jorge.szwako', 161, 2, 'B', 3, 14, 3, NULL),  -- Electrónica Analógica (Eca. Analógica)
    ('abel.admen', 14, 2, 'B', 4, 9, 2, NULL),  -- Química
    ('alcira.caceres', 10, 2, 'B', 4, 11, 2, NULL),  -- Inglés
    ('oscar.villalba', 166, 2, 'B', 4, 13, 4, NULL),  -- Electrónica Industrial (Eca. Industrial)
    ('romy.aguilera', 8, 2, 'B', 5, 9, 4, NULL),  -- Guaraní
    ('leticia.bogado', 16, 3, 'A', 1, 1, 2, 'S8'),  -- Literatura
    ('oscar.villalba.lab', 163, 3, 'A', 1, 3, 6, 'Lab 3 & 2'),  -- Laboratorio de Electrónica
    ('jose.orue', 17, 3, 'A', 2, 1, 4, 'ESP 3'),  -- Informática
    ('jose.orue', 161, 3, 'A', 2, 5, 4, 'ESP 3'),  -- Electrónica Analógica (Eca. Analógica)
    ('emilce.jara', 13, 3, 'A', 2, 9, 4, 'S8'),  -- Psicología
    ('oscar.villalba.lab', 163, 3, 'A', 2, 13, 4, 'Lab 3 & 2'),  -- Laboratorio de Electrónica
    ('jose.orue', 166, 3, 'A', 3, 1, 4, 'ESP 3'),  -- Electrónica Industrial (Eca. Industrial)
    ('jose.orue', 161, 3, 'A', 3, 5, 4, 'ESP 3'),  -- Electrónica Analógica (Eca. Analógica)
    ('osvaldo.cruz', 169, 3, 'A', 3, 9, 5, 'GAB'),  -- Optativa (Electrónica)
    ('nancy.sosa', 11, 3, 'A', 4, 1, 2, 'S8'),  -- Matemática Común
    ('abner.alcaraz', 9, 3, 'A', 4, 3, 2, 'S8'),  -- Historia
    ('gerardo.ovelar', 4, 3, 'A', 4, 5, 2, 'S8'),  -- Educación Física
    ('mirian.montania', 12, 3, 'A', 4, 7, 2, 'S8'),  -- Orientación
    ('milner.mercado', 162, 3, 'A', 4, 9, 4, 'S8'),  -- Electrónica Digital (Eca. Digital)
    ('ivan.nunez', 170, 3, 'A', 5, 1, 2, 'ESP 3'),  -- Proyecto (Electrónica)
    ('hugo.olmedo', 2, 3, 'A', 5, 3, 2, 'S8'),  -- Ciencias
    ('rolando.lenguaza', 3, 3, 'A', 5, 5, 4, 'S8'),  -- Economía y Gestión
    ('oscar.villalba.lab', 163, 3, 'C', 1, 9, 4, 'Lab 3 & 2'),  -- Laboratorio de Electrónica
    ('oscar.villalba.lab', 163, 3, 'C', 1, 13, 2, 'Lab 3 & 2'),  -- Laboratorio de Electrónica
    ('jose.orue', 161, 3, 'C', 2, 9, 4, 'ESP 3'),  -- Electrónica Analógica (Eca. Analógica)
    ('jose.orue', 166, 3, 'C', 2, 13, 4, 'ESP 3'),  -- Electrónica Industrial (Eca. Industrial)
    ('mirian.montania', 12, 3, 'C', 3, 3, 2, 'S8'),  -- Orientación
    ('rolando.lenguaza', 3, 3, 'C', 3, 5, 4, 'ESP 2'),  -- Economía y Gestión
    ('edgar.aquino', 13, 3, 'C', 3, 9, 4, 'S7'),  -- Psicología
    ('jose.orue', 161, 3, 'C', 3, 13, 4, 'GAB'),  -- Electrónica Analógica (Eca. Analógica)
    ('susana.alvarenga', 16, 3, 'C', 4, 3, 2, 'ESP 2'),  -- Literatura
    ('nancy.sosa', 11, 3, 'C', 4, 5, 2, 'ESP 2'),  -- Matemática Común
    ('gerardo.ovelar', 4, 3, 'C', 4, 7, 2, 'ESP 2'),  -- Educación Física
    ('hugo.olmedo', 2, 3, 'C', 4, 9, 2, 'S7'),  -- Ciencias
    ('alicia.martinez', 9, 3, 'C', 4, 11, 2, 'S7'),  -- Historia
    ('ivan.nunez', 162, 3, 'C', 4, 13, 4, 'GAB'),  -- Electrónica Digital (Eca. Digital)
    ('osvaldo.cruz', 169, 3, 'C', 5, 1, 6, 'GAB'),  -- Optativa (Electrónica)
    ('ivan.nunez', 170, 3, 'C', 5, 7, 2, 'GAB'),  -- Proyecto (Electrónica)
    ('guillermo.salcedo', 17, 3, 'C', 5, 9, 4, 'S7'),  -- Informática
    ('ivan.nunez', 170, 3, 'B', 1, 1, 2, 'GAB'),  -- Proyecto (Electrónica)
    ('alicia.martinez', 3, 3, 'B', 1, 3, 4, 'S8'),  -- Economía y Gestión
    ('leticia.bogado', 16, 3, 'B', 1, 7, 2, 'S8'),  -- Literatura
    ('nancy.sosa', 11, 3, 'B', 1, 9, 2, 'S8'),  -- Matemática Común
    ('hugo.olmedo', 2, 3, 'B', 1, 11, 2, NULL),  -- Ciencias
    ('emilce.jara', 13, 3, 'B', 1, 14, 2, 'S8'),  -- Psicología
    ('oscar.villalba.lab', 163, 3, 'B', 2, 9, 4, 'Lab 3 & 2'),  -- Laboratorio de Electrónica
    ('osvaldo.cruz', 169, 3, 'B', 3, 1, 5, 'GAB'),  -- Optativa (Electrónica)
    ('mirian.montania', 12, 3, 'B', 3, 7, 2, 'S8'),  -- Orientación
    ('jose.orue', 161, 3, 'B', 3, 9, 4, 'ESP 3'),  -- Electrónica Analógica (Eca. Analógica)
    ('jose.orue', 161, 3, 'B', 3, 13, 4, 'ESP 3'),  -- Electrónica Analógica (Eca. Analógica)
    ('helen.duarte', 4, 3, 'B', 4, 7, 2, 'ESP 3'),  -- Educación Física
    ('jose.orue', 166, 3, 'B', 4, 9, 4, 'ESP 3'),  -- Electrónica Industrial (Eca. Industrial)
    ('oscar.villalba.lab', 163, 3, 'B', 5, 1, 2, 'Lab 3 & 2'),  -- Laboratorio de Electrónica
    ('oscar.villalba.lab', 163, 3, 'B', 5, 5, 2, 'Lab 3 & 2'),  -- Laboratorio de Electrónica
    ('graciela.gonzalez', 9, 3, 'B', 5, 7, 2, 'ESP 2'),  -- Historia
    ('milner.mercado', 162, 3, 'B', 5, 9, 4, 'S8'),  -- Electrónica Digital (Eca. Digital)
    ('cristhian.martinez', 17, 3, 'B', 5, 13, 4, 'S8');  -- Informática

INSERT IGNORE INTO horario_slot (asignacion_id, usuario_id, curso_base_id, dia_semana, hora_catedra_id, sala_id)
SELECT DISTINCT
    a.id,
    u.id,
    cb.id,
    s.dia_semana,
    hc.id,
    sa.id
FROM horario_slot_staging s
JOIN usuario u ON (
        (s.profesor_usuario <> 'oscar.villalba.lab' AND u.usuario = s.profesor_usuario)
        -- 'oscar.villalba.lab' es un marcador (no un usuario real) para distinguir
        -- a "Prof. Villalba" (Laboratorio de Electrónica, 3ro, id 132 = Oscar
        -- Daniel Villalba Riveros) de "Prof. Oscar" (Electrotecnia/Eca. Analógica
        -- de 1ro/2do, id 131 = Oscar Adolfo Villalba Ortiz) -- ambos comparten el
        -- mismo username 'oscar.villalba' en la tabla usuario, ver reporte.
        OR (s.profesor_usuario = 'oscar.villalba.lab' AND u.id = 132)
    )
JOIN curso_base cb ON cb.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Electrónica')
    AND cb.nivel = s.nivel AND cb.seccion = s.seccion
JOIN asignacion a ON a.usuario_id = u.id AND a.materia_id = s.materia_id AND a.curso_base_id = cb.id
JOIN horario_slot_span span ON span.n <= s.duracion
JOIN hora_catedra hc ON hc.numero = s.hora_numero + (span.n - 1)
LEFT JOIN sala sa ON sa.nombre = s.sala_nombre
    AND sa.especialidad_id = (SELECT id FROM especialidad WHERE nombre = 'Electrónica')
ORDER BY cb.nivel, cb.seccion, s.dia_semana, hc.numero;

DROP TEMPORARY TABLE horario_slot_span;
DROP TEMPORARY TABLE horario_slot_staging;
