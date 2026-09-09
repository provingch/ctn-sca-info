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

INSERT INTO curso_base (id, especialidad_id, nivel, seccion)
VALUES
(1, 5, 1, 'A'),
(2, 5, 1, 'B'),
(3, 5, 2, 'A'),
(4, 5, 2, 'B'),
(5, 5, 3, 'A'),
(6, 5, 3, 'B');

-- ========================================
-- USUARIOS BASE
-- ========================================
-- Tipo de usuario (nivel):
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
    (10, 'Juan Nicolas', 'Acosta', 'juan.acosta', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4959582, null, null, null, null, 1, NULL, null), --[cite: 1]
    (11, 'Abel', 'Admen Oliveira', 'abel.admen', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2307477, null, null, null, null, 1, NULL, null), --[cite: 1]
    (12, 'Romy Luz', 'Aguilera de Mongelos', 'romy.aguilera', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 748826, null, null, null, null, 1, NULL, null), --[cite: 1]
    (13, 'Abner Constantino', 'Alcaraz Rojas', 'abner.alcaraz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 801245, null, null, null, null, 1, NULL, null), --[cite: 1]
    (14, 'Jorge Anibal', 'Alfonzo Vera', 'jorge.alfonzo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3538324, null, null, null, null, 1, NULL, null), --[cite: 1]
    (15, 'Liz Maria Gloria', 'Alfonzo Vera', 'liz.alfonzo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3538327, null, null, null, null, 1, NULL, null), --[cite: 1]
    (16, 'Blanca Rosa', 'Almirón de Notario', 'blanca.almiron', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2870819, null, null, null, null, 1, NULL, null), --[cite: 1]
    (17, 'Susana Raquel', 'Alvarenga Cañete', 'susana.alvarenga', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2846055, null, null, null, null, 1, NULL, null), --[cite: 1]
    (18, 'Ana Maria', 'Andino Ramos', 'ana.andino', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 814644, null, null, null, null, 1, NULL, null), --[cite: 1]
    (19, 'Edgar Sebastian', 'Aquino Ledezma', 'edgar.aquino', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4739025, null, null, null, null, 1, NULL, null), --[cite: 1]
    (20, 'Jorge Alberto', 'Aquino Peralta', 'jorge.aquino', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2928412, null, null, null, null, 1, NULL, null), --[cite: 1]
    (21, 'Alba Concepción', 'Arrúa Sosa', 'alba.arrua', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3721761, null, null, null, null, 1, NULL, null), --[cite: 1]
    (22, 'Nathalia Soledad', 'Báez Pereira', 'nathalia.baez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3518186, null, null, null, null, 1, NULL, null), --[cite: 1]
    (23, 'Alicia Celeste', 'Barrios de Báez', 'alicia.barrios', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2332755, null, null, null, null, 1, NULL, null), --[cite: 1]
    (24, 'Irma', 'Benítez Fernández', 'irma.benitez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 503029, null, null, null, null, 1, NULL, null), --[cite: 1]
    (25, 'Victor Sebastián', 'Benítez Irala', 'victor.benitez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1348684, null, null, null, null, 1, NULL, null), --[cite: 1]
    (26, 'Norberto Alejandro', 'Benítez López', 'norberto.benitez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2152502, null, null, null, null, 1, NULL, null), --[cite: 1]
    (27, 'Leticia Ester', 'Bogado Fariña', 'leticia.bogado', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 6570457, null, null, null, null, 1, NULL, null), --[cite: 1]
    (28, 'Víctor Hugo', 'Bogarin Martinez', 'victor.bogarin', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1241578, null, null, null, null, 1, NULL, null), --[cite: 1]
    (29, 'Claudia Marina', 'Burgos de Velázquez', 'claudia.burgos', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2069958, null, null, null, null, 1, NULL, null), --[cite: 1]
    (30, 'Robert Brigido', 'Caballero Vera', 'robert.caballero', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1044470, null, null, null, null, 1, NULL, null), --[cite: 1]
    (31, 'Arnaldo José', 'Cabrera Díaz', 'arnaldo.cabrera', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1291777, null, null, null, null, 1, NULL, null), --[cite: 1]
    (32, 'Lourdes Natalia', 'Caceres Alfonzo', 'lourdes.caceres', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3654427, null, null, null, null, 1, NULL, null), --[cite: 1]
    (33, 'Alcira Carolina', 'Cáceres de Ortellado', 'alcira.caceres', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2291288, null, null, null, null, 1, NULL, null), --[cite: 1]
    (34, 'Máximo Tito Simón', 'Cáceres Gini', 'maximo.caceres', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4325994, null, null, null, null, 1, NULL, null), --[cite: 1]
    (35, 'Irma Graciela', 'Cardozo', 'irma.cardozo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1295107, null, null, null, null, 1, NULL, null), --[cite: 1]
    (36, 'Tango Ottmar', 'Carrero Romero', 'tango.carrero', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1825471, null, null, null, null, 1, NULL, null), --[cite: 1]
    (37, 'Gerardo Omar', 'Centurión Gómez', 'gerardo.centurion', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4337053, null, null, null, null, 1, NULL, null), --[cite: 1]
    (38, 'Victor Amadeo', 'Cerquetti Cristaldo', 'victor.cerquetti', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 451439, null, null, null, null, 1, NULL, null), --[cite: 1]
    (39, 'Luis Carlos', 'Chávez Zalazar', 'luis.chavez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1798067, null, null, null, null, 1, NULL, null), --[cite: 1]
    (40, 'Luis Fernando', 'Codas Baade', 'luis.codas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1138010, null, null, null, null, 1, NULL, null), --[cite: 1]
    (41, 'Crispin', 'Coeffier Villalba', 'crispin.coeffier', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 694719, null, null, null, null, 1, NULL, null), --[cite: 1]
    (42, 'Osvaldo Ramón', 'Cruz', 'osvaldo.cruz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1955257, null, null, null, null, 1, NULL, null), --[cite: 1]
    (43, 'Cristian Humberto', 'Delgado Pereira', 'cristian.delgado', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1877110, null, null, null, null, 1, NULL, null), --[cite: 1]
    (44, 'Cynthia Noelia', 'Díaz López', 'cynthia.diaz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4295031, null, null, null, null, 1, NULL, null), --[cite: 1]
    (45, 'Liz Mariza', 'Duarte de Delgado', 'liz.duarte', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1325786, null, null, null, null, 1, NULL, null), --[cite: 1]
    (46, 'Helen Lilian', 'Duarte Ortiz', 'helen.duarte', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2868076, null, null, null, null, 1, NULL, null), --[cite: 1]
    (47, 'Jorge Guillermo', 'Echague Ramirez', 'jorge.echague', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4318501, null, null, null, null, 1, NULL, null), --[cite: 1]
    (48, 'Maria del Rocio', 'Egusquiza de Schwarz', 'maria.egusquiza', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1349146, null, null, null, null, 1, NULL, null), --[cite: 1]
    (49, 'Lidubina', 'Escobar Garcete', 'lidubina.escobar', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2888556, null, null, null, null, 1, NULL, null), --[cite: 1]
    (50, 'Fernando Javier', 'Espinoza Correa', 'fernando.espinoza', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4912277, null, null, null, null, 1, NULL, null), --[cite: 1]
    (51, 'Ruth Ninfa', 'Estigarribia González', 'ruth.estigarribia', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1297160, null, null, null, null, 1, NULL, null), --[cite: 1]
    (52, 'Nemesio', 'Fernandez Ferreira', 'nemesio.fernandez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4193955, null, null, null, null, 1, NULL, null), --[cite: 1]
    (53, 'Ana Luciana', 'Fernández de Gómez', 'ana.fernandez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1711121, null, null, null, null, 1, NULL, null), --[cite: 1]
    (54, 'Igor Alejandro', 'Fernández Ozuna', 'igor.fernandez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 6719007, null, null, null, null, 1, NULL, null), --[cite: 1]
    (55, 'Ruth Johana', 'Ferrarino Chaparro', 'ruth.ferrarino', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4994558, null, null, null, null, 1, NULL, null), --[cite: 1]
    (56, 'Richar', 'Ferreira Valenzuela', 'richar.ferreira', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1319513, null, null, null, null, 1, NULL, null), --[cite: 1]
    (57, 'Carmen Lilian', 'Franco', 'carmen.franco', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 758565, null, null, null, null, 1, NULL, null), --[cite: 1]
    (58, 'Lourdes', 'Galeano de Viera', 'lourdes.galeano', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1788967, null, null, null, null, 1, NULL, null), --[cite: 1]
    (59, 'Mirian Raquel', 'Galeano Zavala', 'mirian.galeano', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4089145, null, null, null, null, 1, NULL, null), --[cite: 1]
    (60, 'Ana Gabriela', 'Gallardo Alderete', 'ana.gallardo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3994217, null, null, null, null, 1, NULL, null), --[cite: 1]
    (61, 'Ismael Inocente', 'Garay Gonzalez', 'ismael.garay', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3455447, null, null, null, null, 1, NULL, null), --[cite: 1]
    (62, 'Pedro David', 'Garcete Gauto', 'pedro.garcete', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2110348, null, null, null, null, 1, NULL, null), --[cite: 1]
    (63, 'Anibal', 'Genes Boy', 'anibal.genes', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3471698, null, null, null, null, 1, NULL, null), --[cite: 1]
    (64, 'Juan Angel', 'González Aguilera', 'juan.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 805420, null, null, null, null, 1, NULL, null), --[cite: 1]
    (65, 'Bernarda Maria', 'González de Fleitas', 'bernarda.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1415391, null, null, null, null, 1, NULL, null), --[cite: 1]
    (66, 'Federico', 'Gónzalez Esteche', 'federico.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3176523, null, null, null, null, 1, NULL, null), --[cite: 1]
    (67, 'Graciela Elizabeth', 'González Gimenez', 'graciela.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3480729, null, null, null, null, 1, NULL, null), --[cite: 1]
    (68, 'Raquel', 'González Quintana', 'raquel.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1217787, null, null, null, null, 1, NULL, null), --[cite: 1]
    (69, 'Francisco Andres', 'Gónzalez Zaracho', 'francisco.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4793465, null, null, null, null, 1, NULL, null), --[cite: 1]
    (70, 'Fátima Rocío', 'Grillón de Medina', 'fatima.grillon', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2281694, null, null, null, null, 1, NULL, null), --[cite: 1]
    (71, 'Felix Fernando', 'Huerta Etcheverry', 'felix.huerta', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 939001, null, null, null, null, 1, NULL, null), --[cite: 1]
    (72, 'Zulma Asunción', 'Ibarra Rodríguez', 'zulma.ibarra', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 757978, null, null, null, null, 1, NULL, null), --[cite: 1]
    (73, 'Oscar', 'Ibarrola Diaz', 'oscar.ibarrola', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 621312, null, null, null, null, 1, NULL, null), --[cite: 1]
    (74, 'Emilce Beatriz', 'Jara Bogado', 'emilce.jara', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2045853, null, null, null, null, 1, NULL, null), --[cite: 1]
    (75, 'Hernán', 'Jara Olmedo', 'hernan.jara', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1059166, null, null, null, null, 1, NULL, null), --[cite: 1]
    (76, 'Rolando Daniel', 'Lenguaza Sosa', 'rolando.lenguaza', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1200350, null, null, null, null, 1, NULL, null), --[cite: 1]
    (77, 'Javier Adolfo', 'López Benítez', 'javier.lopez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1638538, null, null, null, null, 1, NULL, null), --[cite: 1]
    (78, 'Alice Amelia', 'López de Fretes', 'alice.lopez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1418878, null, null, null, null, 1, NULL, null), --[cite: 1]
    (79, 'Humberto Manuel', 'López González', 'humberto.lopez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4203308, null, null, null, null, 1, NULL, null), --[cite: 1]
    (80, 'Maria Mercedes', 'López Lezcano', 'maria.lopez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4165186, null, null, null, null, 1, NULL, null), --[cite: 1]
    (81, 'Graciela Noemí', 'López Molinas', 'graciela.lopez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1722056, null, null, null, null, 1, NULL, null), --[cite: 1]
    (82, 'Graciela', 'Maidana Pinto', 'graciela.maidana', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1077876, null, null, null, null, 1, NULL, null), --[cite: 1]
    (83, 'Alicia', 'Martinez López', 'alicia.martinez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2970412, null, null, null, null, 1, NULL, null), --[cite: 1]
    (84, 'Cristhian Agustin', 'Martinez Paredes', 'cristhian.martinez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 5928736, null, null, null, null, 1, NULL, null), --[cite: 1]
    (85, 'Cirilo David', 'Medina Cáceres', 'cirilo.medina', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2188244, null, null, null, null, 1, NULL, null), --[cite: 1]
    (86, 'Juan Antonio', 'Medina Chávez', 'juan.medina', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1988072, null, null, null, null, 1, NULL, null), --[cite: 1]
    (87, 'Maria Adriana', 'Mequer Pfefferkorn', 'maria.mequer', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4261227, null, null, null, null, 1, NULL, null), --[cite: 1]
    (88, 'Milner Gabriel', 'Mercado Vera', 'milner.mercado', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 979744, null, null, null, null, 1, NULL, null), --[cite: 1]
    (89, 'Maria Soledad', 'Mereles Barrios', 'maria.mereles', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1711193, null, null, null, null, 1, NULL, null), --[cite: 1]
    (90, 'Marta Guadalupe', 'Mojoli Apthorpe', 'marta.mojoli', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1434365, null, null, null, null, 1, NULL, null), --[cite: 1]
    (91, 'Francisco Javier', 'Molinas Ferreira', 'francisco.molinas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4329959, null, null, null, null, 1, NULL, null), --[cite: 1]
    (92, 'Mirian Airini', 'Montanía Gónzalez', 'mirian.montania', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4311520, null, null, null, null, 1, NULL, null), --[cite: 1]
    (93, 'Liz Marina', 'Montiel de Bogarin', 'liz.montiel', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2196944, null, null, null, null, 1, NULL, null), --[cite: 1]
    (94, 'Justo Enrique', 'Mora Caballero', 'justo.mora', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3189593, null, null, null, null, 1, NULL, null), --[cite: 1]
    (95, 'Iván Gerardo', 'Núñez Genes', 'ivan.nunez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3578998, null, null, null, null, 1, NULL, null), --[cite: 1]
    (96, 'Zully Antonia', 'Núñez Ramírez', 'zully.nunez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1794168, null, null, null, null, 1, NULL, null), --[cite: 1]
    (97, 'Genicio', 'Núñez Romero', 'genicio.nunez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3176523, null, null, null, null, 1, NULL, null), --[cite: 1]
    (98, 'Hugo de Jesús', 'Olmedo Chávez', 'hugo.olmedo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2817429, null, null, null, null, 1, NULL, null), --[cite: 1]
    (99, 'Leticia Soledad', 'Olmedo Melgarejo', 'leticia.olmedo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3446998, null, null, null, null, 1, NULL, null), --[cite: 1]
    (100, 'Aracely Macarena', 'Ortiz Ramirez', 'aracely.ortiz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 5132182, null, null, null, null, 1, NULL, null), --[cite: 1]
    (101, 'José Edgar', 'Orué Alonso', 'jose.orue', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 901079, null, null, null, null, 1, NULL, null), --[cite: 1]
    (102, 'Gerardo Raúl', 'Ovelar Fernández', 'gerardo.ovelar', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4614218, null, null, null, null, 1, NULL, null), --[cite: 1]
    (103, 'Andrea Romina', 'Perez Benitez', 'andrea.perez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3420813, null, null, null, null, 1, NULL, null), --[cite: 1]
    (104, 'José Luis', 'Pino Meza', 'jose.pino', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3385220, null, null, null, null, 1, NULL, null), --[cite: 1]
    (105, 'Zonia Inocencia', 'Ramirez de Torres', 'zonia.ramirez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1427160, null, null, null, null, 1, NULL, null), --[cite: 1]
    (106, 'Gustavo Adolfo', 'Ramírez Fernández', 'gustavo.ramirez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1393836, null, null, null, null, 1, NULL, null), --[cite: 1]
    (107, 'Christian Javier', 'Ramos Santacruz', 'christian.ramos', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 5132801, null, null, null, null, 1, NULL, null), --[cite: 1]
    (108, 'Daniela Consuelo', 'Ratzlaff Galeano', 'daniela.ratzlaff', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1858451, null, null, null, null, 1, NULL, null), --[cite: 1]
    (109, 'Jorge Alberto', 'Recalde Espinoza', 'jorge.recalde', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1203846, null, null, null, null, 1, NULL, null), --[cite: 1]
    (110, 'Daniel', 'Rios Morales', 'daniel.rios', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1083166, null, null, null, null, 1, NULL, null), --[cite: 1]
    (111, 'Laura Raquel', 'Rivas de López', 'laura.rivas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1587228, null, null, null, null, 1, NULL, null), --[cite: 1]
    (112, 'Claudia Irene', 'Riveros Weiler', 'claudia.riveros', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3528678, null, null, null, null, 1, NULL, null), --[cite: 1]
    (113, 'Karill Aracelli', 'Rojas Díaz', 'karill.rojas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4562211, null, null, null, null, 1, NULL, null), --[cite: 1]
    (114, 'Celso Ramón', 'Rojas Jara', 'celso.rojas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3669793, null, null, null, null, 1, NULL, null), --[cite: 1]
    (115, 'Cesar Andres', 'Rojas Morel', 'cesar.rojas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4635720, null, null, null, null, 1, NULL, null), --[cite: 1]
    (116, 'María de Lurdes', 'Román de Rivaldi', 'maria.roman', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1225464, null, null, null, null, 1, NULL, null), --[cite: 1]
    (117, 'Ruth Marlene', 'Román Gómez', 'ruth.roman', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2375989, null, null, null, null, 1, NULL, null), --[cite: 1]
    (118, 'Angel José', 'Ruíz Diaz Antunez', 'angel.ruiz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3610653, null, null, null, null, 1, NULL, null), --[cite: 1]
    (119, 'Guillermo', 'Salcedo', 'guillermo.salcedo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4797255, null, null, null, null, 1, NULL, null), --[cite: 1]
    (120, 'Nidia Beatriz', 'Samudio de Torres', 'nidia.samudio', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3439931, null, null, null, null, 1, NULL, null), --[cite: 1]
    (121, 'Silvio Gustavo', 'Sanchéz Montiel', 'silvio.sanchez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3685769, null, null, null, null, 1, NULL, null), --[cite: 1]
    (122, 'Hilda Ramona', 'Sánchez', 'hilda.sanchez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3701199, null, null, null, null, 1, NULL, null), --[cite: 1]
    (123, 'Jean Michel', 'Sekatcheff Snead', 'jean.sekatcheff', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 577903, null, null, null, null, 1, NULL, null), --[cite: 1]
    (124, 'Nancy Catalina', 'Sosa de Franco', 'nancy.sosa', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3201310, null, null, null, null, 1, NULL, null), --[cite: 1]
    (125, 'Jorge Hilario', 'Szwako Montero', 'jorge.szwako', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 715659, null, null, null, null, 1, NULL, null), --[cite: 1]
    (126, 'Esperanza Lucia', 'Torales de Aquino', 'esperanza.torales', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2375521, null, null, null, null, 1, NULL, null), --[cite: 1]
    (127, 'Genoveva De Jesús', 'Valdéz González', 'genoveva.valdez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 558994, null, null, null, null, 1, NULL, null), --[cite: 1]
    (128, 'Maria Luz', 'Valiente de Angulo', 'maria.valiente', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1275005, null, null, null, null, 1, NULL, null), --[cite: 1]
    (129, 'Gladys Carmen', 'Vallejos Ortiz', 'gladys.vallejos', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 758565, null, null, null, null, 1, NULL, null), --[cite: 1]
    (130, 'Mónica Elizabeth', 'Vera Vega', 'monica.vera', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1407630, null, null, null, null, 1, NULL, null), --[cite: 1]
    (131, 'Oscar Adolfo', 'Villalba Ortiz', 'oscar.villalba', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 1722056, null, null, null, null, 1, NULL, null), --[cite: 1]
    (132, 'Oscar Daniel', 'Villalba Riveros', 'oscar.villalba', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4912277, null, null, null, null, 1, NULL, null), --[cite: 1]
    (133, 'Oscar Ramón', 'Villasanti Cañete', 'oscar.villasanti', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2293109, null, null, null, null, 1, NULL, null), --[cite: 1]
    (134, 'Paolo Giovanni', 'Zucchini Cuevas', 'paolo.zucchini', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4337468, null, null, null, null, 1, NULL, null); --[cite: 1]

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
    (144,'CoordinadorPedagogico', '5', 'cpdg5', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 20, null, null, null, null, 5, NULL, null)

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
    'Electrónica'
  );

-- Química (comun, id 14) no se dicta como tal dentro de la especialidad
-- Química Industrial (ahí las materias son específicas: Química General,
-- Química Analítica, etc.) -> se linkea a las otras 6
INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT 14, e.id
FROM especialidad e
WHERE e.nombre IN (
  'Construcciones Civiles',
  'Mecánica General',
  'Electricidad',
  'Electromecánica',
  'Informática',
  'Electrónica'
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
(137, 'Tecnología (Química)', 'especifico');

-- Electromecánica (141-160)
INSERT INTO materia (id, nombre, categoria) VALUES
(141, 'Refrigeración', 'especifico'),
(142, 'Neumática e Hidráulica', 'especifico'),
(143, 'PLC', 'especifico'),
(144, 'Diseño y Mantenimiento Industrial', 'especifico'),
(145, 'Electrotecnia (Electromecánica)', 'especifico'),
(146, 'Electrónica (Electromecánica)', 'especifico');

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
FROM materia m WHERE m.id BETWEEN 121 AND 137;

INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, (SELECT id FROM especialidad WHERE nombre = 'Electromecánica')
FROM materia m WHERE m.id BETWEEN 141 AND 146;

INSERT INTO materia_especialidad (materia_id, especialidad_id)
SELECT m.id, (SELECT id FROM especialidad WHERE nombre = 'Electrónica')
FROM materia m WHERE m.id BETWEEN 161 AND 170;

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
    (nombre, apellido, curso_id, ci, correo_encargado, correo_encargado2)
    VALUES
    ('PAZ FIORELLA', 'ACUÑA RODRIGUEZ', 31, 6552138, NULL, NULL),
    ('GABRIELA ELIZABETH', 'ALEGRE ORTIZ', 31, 6520371, NULL, NULL),
    ('CESAR EZEQUIEL', 'AMARILLA ETTIENE', 31, 7011624, NULL, NULL),
    ('FERNANDO JOSE', 'BARRETO ROCHE', 31, 6271898, NULL, NULL),
    ('MARIA CECILIA', 'BENITEZ BARRIOS', 31, 7350265, NULL, NULL),
    ('SOFIA ESMERALDA', 'BENITEZ MARTINEZ', 31, 7290536, NULL, NULL),
    ('VALERIA ALEJANDRA', 'CACERES ACHUCARRO', 31, 7536039, NULL, NULL),
    ('CARLOS ANTONIO', 'CANDIA ROMERO', 31, 6895905, NULL, NULL),
    ('JONAS ALEXANDER', 'CUBILLA MORINIGO', 31, 7979695, NULL, NULL),
    ('ALICE GISSELLE', 'DIAZ AMARILLA', 31, 6274837, NULL, NULL),
    ('KEVIN MATIAS', 'DURE AQUINO', 31, 6711232, NULL, NULL),
    ('THIAGO DAVID', 'ESTIGARRIBIA DELGADILLO', 31, 6911572, NULL, NULL),
    ('GLORIA MILENA', 'FARIÑA NUÑEZ', 31, 6363114, NULL, NULL),
    ('LUCIO ALESSANDRO', 'GAMARRA AGUAYO', 31, 6216256, NULL, NULL),
    ('LUZ NAHIARA', 'GAYOZO AVALOS', 31, 6218519, NULL, NULL),
    ('THIAGO ALEXANDER', 'LEON CORONEL', 31, 6168091, NULL, NULL),
    ('LUCAS ABDIEL', 'MARTINEZ GONZALEZ', 31, 6219481, NULL, NULL),
    ('CHRISTOPHER IVAN', 'MARTINEZ INSFRAN', 31, 7449854, NULL, NULL),
    ('MARCOS DANIEL', 'MOLINAS LEON', 31, 6820120, NULL, NULL),
    ('JOSHUA FABRIZIO', 'MONGELOS CAMACHO', 31, 6656584, NULL, NULL),
    ('MIANE MARIA VERONICA', 'NOGUERA AVILA', 31, 6298042, NULL, NULL),
    ('ALAN ENRIQUE DAMIAN', 'OJEDA OLIVER', 31, 6840108, NULL, NULL),
    ('ALEXANDER AGUSTIN', 'OLMEDO RODRIGUEZ', 31, 6658507, NULL, NULL),
    ('SAMUEL JESUS', 'SCHMIDT SILVEIRA', 31, 6595852, NULL, NULL),
    ('JOSE FEDERICO', 'SOLER VAZQUEZ', 31, 7309281, NULL, NULL),
    ('MIKAHELA', 'SUAREZ ARZA', 31, 6711101, NULL, NULL),
    ('LEONARDO', 'VALINOTTI  PAREDES', 31, 6761746, NULL, NULL),
    ('FACUNDO BENJAMIN', 'VERA SALINAS', 31, 7007217, NULL, NULL);

    -- 3º B (curso_id = 32)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci, correo_encargado, correo_encargado2)
    VALUES
    ('JORGE JOAQUIN', 'GONZALEZ BAEZ', 32, 6300937, 'NULL@null.com', ''),
    ('EMILIO ANDRES', 'ALMIRON RUIZ', 32, 8651544, NULL, NULL),
    ('JORGE DAVID', 'AVEIRO DURE', 32, 6763135, NULL, NULL),
    ('GABRIELA DENISSE', 'BENITEZ CAMPUZANO', 32, 6248031, NULL, NULL),
    ('PAMELA MONSERRAT', 'CABALLERO ZARACHO', 32, 6122730, NULL, NULL),
    ('FABRICIO NICOLAS', 'CUBAS VAZQUEZ', 32, 6299174, NULL, NULL),
    ('JESUS MARIA', 'DAVID RESQUIN', 32, 7112304, NULL, NULL),
    ('SANTIAGO DIDIER DAMASO', 'DELVALLE CABRAL', 32, 6323522, NULL, NULL),
    ('PAULO GASTON', 'DUARTE ORUE', 32, 6506158, NULL, NULL),
    ('ALBA MARIA ELIZABETH', 'FARIÑA MORAN', 32, 6682899, NULL, NULL),
    ('EVELYN CECILIA', 'GALEANO DUARTE', 32, 6254779, NULL, NULL),
    ('FRANCO GONZALO', 'GARCIA GARCIA', 32, 6378044, NULL, NULL),
    ('ANGELO GASTON', 'GONZALEZ AMARILLA', 32, 6306858, NULL, NULL),
    ('JUANA DAMARIS', 'HUACCA ALEJO', 32, 9132227, NULL, NULL),
    ('MILAGROS MICAELA', 'JIMENEZ ROJAS', 32, 6276848, NULL, NULL),
    ('LUCAS   MANUEL', 'LOPEZ ALDERETE', 32, 6709236, NULL, NULL),
    ('PABLO LEANDRO', 'LOPEZ PULLARES', 32, 6128349, NULL, NULL),
    ('LUNA MIA', 'MENDIETA', 32, 6521146, NULL, NULL),
    ('VICTOR MANUEL', 'MENDIETA PEREIRA', 32, 7965966, NULL, NULL),
    ('GAIA VIOLETA MARIA', 'MOREL AREVALOS', 32, 6315503, NULL, NULL),
    ('FACUNDO MATHIAS', 'PRIETO CACERES', 32, 7277773, NULL, NULL),
    ('AIDEE FIORELLA', 'RECALDE CASTILLO', 32, 7116092, NULL, NULL),
    ('YANARA AYELEN DOMINGA', 'RODAS VALDEZ', 32, 6337830, NULL, NULL),
    ('FIORELLA ANAHI', 'SOSA AMARILLA', 32, 7934035, NULL, NULL),
    ('SANTIAGO', 'SOSA OVELAR', 32, 6138828, NULL, NULL),
    ('ANA BELEN', 'VARGAS VALIENTE', 32, 6597209, NULL, NULL),
    ('HEATHER PATRICIA', 'WATTIEZ BAREIRO', 32, 6600003, NULL, NULL),
    ('ELIAS SEBASTIAN', 'ZORRILLA BENITEZ', 32, 6355776, NULL, NULL);

    -- 2º A (curso_id = 33)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci, correo_encargado, correo_encargado2) VALUES
    ('MARIELA CHONG AH', 'ACOSTA POSADAS', 33, 6634030, 'posadasmiriam7@gmail.com', 'aacosta352@gmail.com'),
    ('PEDRO JOSÉ', 'ALDERETE PÁEZ', 33, 6599141, 'dograpaez82@gmail.com', 'serafinialderete@hotmail.com'),
    ('GUILLERMO MANUEL', 'APONTE RAMÍREZ', 33, 6375687, 'deinyraq@hotmail.com', 'juanaponte1981@gmail.com'),
    ('ARIEL MAXIMILIANO', 'ARAUJO SOSA', 33, 7868229, 'imbso73@gmail.com', 'araujodionisio03@gmail.com'),
    ('TYRA SELENE', 'BARBOZA CABRERA', 33, 6514004, 'ocacabrera@gmail.com', 'ribarboz@hotmail.com'),
    ('JUAN GABRIEL', 'CORONEL VILLALBA', 33, 6780823, NULL, 'franciscocoronel753@gmail.com'),
    ('MICAELLA VALENTINA', 'ESPINOZA BELLOTO', 33, 6323591, 'isabelbelotto@gmail.com', 'espinozacelso@gmail.com'),
    ('IVAN ALEXANDER', 'FERNÁNDEZ MEZA', 33, 6674310, NULL, 'hugoconsulramon705@gmail.com'),
    ('JUAN FABRICIO', 'FLEITAS IBÁÑEZ', 33, 7208277, 'ibanezmariaestela86@gmail.com', 'fleitasj277@gmail.com'),
    ('LIA JAZMIN', 'FLEITAS PÉREZ', 33, 8177227, 'daidahipy@gmail.com', 'buysellpy@gmail.com'),
    ('BRAYAN', 'GARCÍA FERNÁNDEZ', 33, 8563705, 'janetfernandez192@gmail.com', 'javiergarciameijide78@gmail.com'),
    ('MARIANA EMILIA', 'GONZÁLEZ CASTRO', 33, 6738451, 'marta.caso415@gmail.com', 'domingoaquiles@gmail.com'),
    ('RAFFAELL', 'GONZÁLEZ LARREA', 33, 6623572, 'na-la-ote@hotmail.com', 'judivepa@gmail.com'),
    ('ÁNGEL JOSÉ IVAN', 'MACIEL RUÍZ DÍAZ', 33, 8079060, 'ruizdiazbarriosepifania@gmail.com', NULL),
    ('RICARDO GERMAN', 'MARTÍNEZ ROJAS', 33, 7488331, 'digracie10@gmail.com', NULL),
    ('MOISES', 'MELGAREJO SAUCEDO', 33, 7230274, 'candidasaucedo4@gmail.com', 'callomelgarejo@gmail.com'),
    ('RODRIGO GABRIEL', 'MOREL MORENO', 33, 7383873, 'morelu71@gmail.com', NULL),
    ('EMILIO JOSÉ', 'MORÍNIGO PEÑA', 33, 7071354, 'emiliomorinigo@hotmail.com', NULL),
    ('ANGÉLICA SUSANA', 'ORUÉ AYALA', 33, 6619509, NULL, 'oscarorue346@gmail.com'),
    ('TANIA GUADALUPE', 'PAIVA SOTELO', 33, 7209622, 'estelamary198@gmail.com', 'faustopaivacolman@gmail.com'),
    ('JUAN JOSÉ', 'PALMA RODRÍGUEZ', 33, 6813981, 'marlenerodriguez0076@gmail.com', 'jdipalma033@gmail.com'),
    ('ALEJANDRO JOSÍAS', 'PÉREZ ÁVALOS', 33, 6534642, 'avalosblancogisellecorina@gmail.com', NULL),
    ('VINICIUS', 'RODRÍGUEZ DE OLIVEIRA', 33, 8758628, 'oliveiraclauder@hotmail.com', NULL),
    ('JOSÍAS ALEXANDER', 'SANTACRUZ OTAZU', 33, 6632204, 'lisandraotazulopez05@gmail.com', 'fidelafhemirsantacruzrojas@gmail.com'),
    ('ALESSANDRO JULIÁN', 'UNZAIN INSFRÁN', 33, 6599080, 'lilainsfrand@gmail.com', 'junzain@gmail.com'),
    ('SOFÍA ARAMÍ', 'VERA MARTÍNEZ', 33, 6658849, 'katiayissel@hotmail.com', 'diegogavin83@gmail.com'),
    ('FABIOLA LUJÁN', 'VERÓN MONGELÓS', 33, 6625127, 'fabiluueron@gmail.com', 'funcargo2020@gmail.com'),
    ('WENDY AYELÉN', 'ZÁRATE ROJAS', 33, 6781794, 'alice.rojas22@gmail.com', 'nelsonzarate3185@gmail.com');

    -- 2º B (curso_id = 34)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci, correo_encargado, correo_encargado2)
    VALUES
    ('FELIX HERNAN', 'ALCARAZ MEZA', 34, 6549365, 'deliameza2015@gmail.com', NULL),
    ('YAGO LAREN', 'AMARILLA LEGUIZAMON', 34, 6581374, 'yanine.leguizamon@gmail.com', 'ajamarilla@gmail.com'),
    ('MONSERRAT ANAHI', 'AYALA GAUTO', 34, 6693608, 'cynthiagauto84@gmail.com', 'cucumelero2008@hotmail.com'),
    ('DYLAN VIRGILIO', 'BURGOS ROTELA', 34, 7401358, 'lilianarotelag@gmail.com', NULL),
    ('ISAAC ULISES', 'CUEVAS SAAVEDRA', 34, 6613266, 'gloriaelizabethsaavedra@gmail.com', 'gustavocuevasvazquez@gmail.com'),
    ('ANGEL GABRIEL', 'DIAZ CAÑETE', 34, 7293215, 'michelscanete90@gmail.com', NULL),
    ('FEDERICO AMIN', 'DOMINGUEZ SOSA', 34, 6538527, 'paolasosa1982@gmail.com', 'cadconstrucciones@gmail.com'),
    ('ALEJANDRA ANAHI', 'ESCOBAR OJEDA', 34, 6833279, 'gracielaescobar86@gmail.com', NULL),
    ('EDEL JAZMIN', 'FRANCO MACIEL', 34, 6593803, 'edelsita09@gmail.com', 'seralber86@gmail.com'),
    ('ALEXIS DANIEL', 'FRETEZ VILLAMAYOR', 34, 6582254, 'porfi.ac@gmail.com', 'david-fretes83@hotmail.com'),
    ('SAULO EZEQUIEL', 'GALEANO RIVEROS', 34, 6704166, 'criveros@vet.una.py', 'arielgaleanobaez@gmail.com'),
    ('LUCAS GABRIEL', 'GAUTO NUÑEZ', 34, 6325567, 'lucriszv@gmail.com', 'jose19gauto@gmail.com'),
    ('ADRIAN', 'GRASSO RAMOS', 34, 6617987, 'lramos@grupofaviola.com.py', 'sergio-grasso2011@hotmail.com'),
    ('MILAGROS MARGARITA YERUTI', 'GUPPI BORDON', 34, 8506321, 'sanibordon@gmail.com', NULL),
    ('AMILCAR ANDRES', 'JARA AGUILERA', 34, 7138719, 'lorenamap84@gmail.com', NULL),
    ('DANAE ABIGAIL', 'JARA MARTINEZ', 34, 7551072, 'paokarina03@gmail.com', 'carlosjarabaez@gmail.com'),
    ('MATEO FERNANDO', 'LENCINA AREVALOS', 34, 6883337, 'silcah@gmail.com', 'silvio-lencina@hotmail.com'),
    ('MARTIN ALEJANDRO', 'LEZCANO MONTIEL', 34, 6626178, 'benimabel85@gmail.com', NULL),
    ('THIAGO VALENTINO', 'MARTINEZ FERNANDEZ', 34, 6727372, 'canolafernandezduarte@gmail.com', 'gasparmartinez06@gmail.com'),
    ('TOBIAS EZEQUIEL', 'MEDINA GONZALEZ', 34, 6512532, 'justialegria@gmail.com', NULL),
    ('VALERIA NOEMI', 'MONTIEL TRIVERO', 34, 7337850, 'natasha.triverofreyre@gmail.com', NULL),
    ('NATHALIA MARIELA', 'ORTIZ RODRIGUEZ', 34, 6532910, 'rodriguezramirezmariela@gmail.com', 'ortizariashector@gmail.com'),
    ('OSIAS BENJAMIN', 'RUBIO SAMUDIO', 34, 6971481, 'patysam1515@gmail.com', 'frubio9987@hotmail.com'),
    ('GIOVANNI JOSE', 'RUIZ ROMAN', 34, 7099638, 'lizipauz@gmail.com', NULL),
    ('SAMYRA ANAHI', 'SANCHEZ AGUILAR', 34, 7086918, 'sameyve1234@gmail.com', NULL),
    ('ENZO SIMON', 'SANCHEZ VERON', 34, 6966829, NULL, NULL),
    ('MARIA TANIA', 'SOILAN SOSA', 34, 6634375, NULL, 'miguel.soilan@rieder.com.py'),
    ('FIORELLA MAGALI', 'VILLAMAYOR VAZQUEZ', 34, 7225342, 'carinafio78@gmail.com', NULL);

    -- 1º A (curso_id = 35)
    -- IMPORTANTE: falta la CI y el correo de cada alumno, no
    -- figuran en las fotos. Reemplazar los numeros ya cargados antes de ejecutar.
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci, correo_encargado, correo_encargado2)
    VALUES
    ('EDGAR ARTURO', 'ALVAREZ BENITEZ', 35, 0000001, NULL, NULL),
    ('ANNA GABRIELA', 'ARAMBULO GONZALEZ', 35, 0000002, NULL, NULL),
    ('SEBASTIAN', 'BALBUENA GAONA', 35, 0000003, NULL, NULL),
    ('NAOMI ABIGAIL', 'BENITES NOGUERA', 35, 0000004, NULL, NULL),
    ('JOSÉ TOMÁS', 'BENÍTEZ BARRIOS', 35, 0000005, NULL, NULL),
    ('SANTINO RAÚL', 'BENÍTEZ ROA', 35, 0000006, NULL, NULL),
    ('LUCAS SEBASTIAN', 'CACERES CARDOZO', 35, 0000007, NULL, NULL),
    ('MATIAS DANIEL', 'CANDIA ALFONSO', 35, 0000008, NULL, NULL),
    ('ALEJANDRO NICOLAS', 'CENTURION CENTURION', 35, 0000009, NULL, NULL),
    ('SANTIAGO BENJAMIN', 'COCCO FRUTOS', 35, 0000010, NULL, NULL),
    ('SOFÍA', 'DA COSTA VALDEZ', 35, 0000011, NULL, NULL),
    ('JORGE ANDRES', 'DE LA BARRA ZOILAN', 35, 0000012, NULL, NULL),
    ('ELENA ISABELLA EDITH', 'DELGADILLO ESTIGARRIBIA', 35, 0000013, NULL, NULL),
    ('EDUARDO SEBASTIAN', 'DUARTE CASTILLO', 35, 0000014, NULL, NULL),
    ('ELIAN ANDRES', 'ESTIGARRIBIA UGARTE', 35, 0000015, NULL, NULL),
    ('KAREN GUADALUPE', 'FRUTOS MORALES', 35, 0000016, NULL, NULL),
    ('JUAN ENRIQUE', 'LECKIE ROLÓN', 35, 0000017, NULL, NULL),
    ('ANNA MEI', 'NOGUERA PENG', 35, 0000018, NULL, NULL),
    ('NATALIA BELEN', 'NUÑEZ VILLAMAYOR', 35, 0000019, NULL, NULL),
    ('NAHOMI BELÉN', 'OCAMPOS ACOSTA', 35, 0000020, NULL, NULL),
    ('RODRIGO MARTÍN', 'OLMEDO ZÁRATE', 35, 0000021, NULL, NULL),
    ('MARÍA LUJÁN', 'OVELAR CENTURIÓN', 35, 0000022, NULL, NULL),
    ('ALEXIA', 'OVELAR MARTÍNEZ', 35, 0000023, NULL, NULL),
    ('JOSE GIOVANNI', 'PORTILLO RIVEROS', 35, 0000024, NULL, NULL),
    ('JOSUE SEBASTIAN', 'QUINTANA BURGOS', 35, 0000025, NULL, NULL),
    ('ANIBAL', 'RAMIREZ ORTIZ', 35, 0000026, NULL, NULL),
    ('PEDRO DANIEL', 'RECALDE ROMERO', 35, 0000027, NULL, NULL),
    ('ANNELISE MARIA JOSÉ', 'SANABRIA DELPADRE', 35, 0000028, NULL, NULL);

    -- 1º B (curso_id = 36)
    INSERT INTO alumno
    (nombre, apellido, curso_id, ci, correo_encargado, correo_encargado2)
    VALUES
    ('ENRIQUE DAMIÁN', 'ACOSTA MEDINA', 36, 0000029, NULL, NULL),
    ('RODRIGO JAVIER', 'AYALA NAVARRO', 36, 0000030, NULL, NULL),
    ('GUILLERMO DANIEL', 'AYALA OCHIPINTTI', 36, 0000031, NULL, NULL),
    ('SANTIAGO DARIO', 'BÁEZ BORDON', 36, 0000032, NULL, NULL),
    ('JAVIER DE JESUS', 'BOGADO PERALTA', 36, 0000033, NULL, NULL),
    ('FABRIZIO BENJAMÍN', 'CABALLERO VILLANUEVA', 36, 0000034, NULL, NULL),
    ('JORGE BENJAMIN', 'DOMINGUEZ GALEANO', 36, 0000035, NULL, NULL),
    ('FERNANDA ISABEL', 'GALEANO RUIZ DÍAZ', 36, 0000036, NULL, NULL),
    ('HORACIO JOSE', 'GIMENEZ MEZA', 36, 0000037, NULL, NULL),
    ('MARIA JOSÉ', 'GIMENEZ TREVISON', 36, 0000038, NULL, NULL),
    ('LUCAS DANIEL', 'GÓMEZ MORENO', 36, 0000039, NULL, NULL),
    ('GUILLERMO FACUNDO', 'MARTÍNEZ BENÍTEZ', 36, 0000040, NULL, NULL),
    ('RODRIGO DANIEL', 'MARTINEZ MARTINO', 36, 0000041, NULL, NULL),
    ('MARTIN RAFAEL', 'MONGELOS BRITEZ', 36, 0000042, NULL, NULL),
    ('THIAGO ALEXANDER', 'OCAMPOS RIVAS', 36, 0000043, NULL, NULL),
    ('MARCELO JAVIER', 'PÉREZ VELÁZQUEZ', 36, 0000044, NULL, NULL),
    ('ALEXANDER DAVID', 'PORTILLO OLMEDO', 36, 0000045, NULL, NULL),
    ('ALEJANDRO ABEL', 'RIVELA TORALES', 36, 0000046, NULL, NULL),
    ('FABRIZIO ARIEL', 'RODAS CABRERA', 36, 0000047, NULL, NULL),
    ('MARIA ISABEL', 'RODRIGUEZ ACOSTA', 36, 0000048, NULL, NULL),
    ('FACUNDA DANIEL', 'RODRIGUEZ LIMA', 36, 0000049, NULL, NULL),
    ('DULCE MARIA GUADALUPE', 'SAUCEDO PARRA', 36, 0000050, NULL, NULL),
    ('IANN DANIEL', 'TOLEDO ARANDA', 36, 0000051, NULL, NULL),
    ('ISAAC ISMAEL', 'TORALES OVELAR', 36, 0000052, NULL, NULL),
    ('GIULIANNA ARAMI', 'VALDEZ FERNÁNDEZ', 36, 0000053, NULL, NULL),
    ('EZEQUIEL', 'VALENZUELA CABALLERO', 36, 0000054, NULL, NULL),
    ('MATEO RAFAEL', 'VELAZQUEZ AMADI', 36, 0000055, NULL, NULL),
    ('FRANCISCO RAFAEL', 'ZARZA MARTÍNEZ', 36, 0000056, NULL, NULL);

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
("INF-PEC", 5);

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
    ('cristian.delgado', 53, 1, 'A', 2, 11, 2),    -- Martes tarde: Info Gral  [AGREGADO, faltaba]
    ('susana.alvarenga', 16, 1, 'A', 3, 1, 4),     -- Miércoles: Literatura
    ('claudia.burgos', 7, 1, 'A', 3, 5, 4),        -- Miércoles: Física        [FIX dur 2->4]
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
    ('susana.alvarenga', 16, 2, 'A', 2, 11, 1),    -- Martes tarde: Literatura (1 período) [FIX dur 2->1; se elimina fila falsa de Plan de Lectura en hora 9]
    ('alcira.caceres', 10, 2, 'A', 3, 9, 2),
    ('mirian.montania', 9, 2, 'A', 3, 11, 2),
    ('cristian.delgado', 54, 2, 'A', 4, 9, 4),     -- Jueves tarde: Laboratorio Hardware [FIX dur 2->4]

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
    ('luz.angulo', 14, 1, 'B', 1, 7, 2),           -- Química               [AGREGADO, faltaba]
    ('andres.rojas', 11, 1, 'B', 1, 9, 4),         -- Mate_Común            [FIX dur 2->4]
    ('graciela.lopez', 41, 1, 'B', 2, 1, 4),
    ('susana.alvarenga', 16, 1, 'B', 2, 5, 2),
    ('mirian.montania', 6, 1, 'B', 2, 7, 2),
    ('alcira.caceres', 10, 1, 'B', 3, 1, 2),
    ('null.chavez', 4, 1, 'B', 3, 3, 2),           -- E. Física             [AGREGADO, faltaba]
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
    ('federico.gonzalez', 46, 2, 'B', 1, 5, 4),    -- Laboratorio SQL       [FIX: antes decía Guaraní en este slot]
    ('zully.nunez', 46, 2, 'B', 1, 9, 2),
    ('laura.rivas', 2, 2, 'B', 1, 11, 2),
    ('andres.rojas', 48, 2, 'B', 2, 1, 2),         -- Mate_Aplicada         [FIX dur 4->2]
    ('abner.alcaraz', 9, 2, 'B', 2, 3, 2),         -- Historia              [AGREGADO, faltaba]
    ('zully.nunez', 8, 2, 'B', 2, 5, 2),           -- Guaraní               [FIX: día 1->2]
    ('null.mequer', 4, 2, 'B', 2, 7, 2),           -- E. Física             [AGREGADO, faltaba]
    ('cristian.delgado', 53, 2, 'B', 2, 9, 2),     -- Info Gral             [FIX: hora 11->9]
    ('mirian.montania', 5, 2, 'B', 2, 11, 2),      -- Educación Vial        [FIX: hora 9->11]
    ('claudia.burgos', 7, 2, 'B', 3, 1, 4),
    ('alcira.caceres', 10, 2, 'B', 3, 5, 2),       -- Inglés                [FIX: día 4->3]
    ('susana.alvarenga', 16, 2, 'B', 3, 7, 2),     -- Literatura            [AGREGADO, faltaba]
    ('federico.gonzalez', 45, 2, 'B', 3, 9, 4),
    ('lourdes.galeano', 15, 2, 'B', 4, 1, 2),
    ('luz.angulo', 14, 2, 'B', 4, 3, 2),           -- Química bloque 1      [FIX: estaba en día 1 / hora 7]
    ('graciela.lopez', 41, 2, 'B', 4, 5, 4),       -- Algorítmica           [FIX: día 5->4, dur 2->4]
    ('alcira.caceres', 10, 2, 'B', 4, 9, 2),
    ('luz.angulo', 14, 2, 'B', 4, 11, 2),          -- Química bloque 2 (este ya estaba bien)
    ('andres.rojas', 11, 2, 'B', 5, 1, 4),         -- Mate_Común            [FIX dur 2->4]
    ('susana.alvarenga', 16, 2, 'B', 5, 5, 3),     -- Literatura, 3 períodos [FIX: día/hora/dur]

    -- ============================================================
    -- SECCIÓN B — 3er año
    -- ============================================================
    ('daniel.lenguaza', 3, 3, 'B', 1, 1, 4),       -- Economía y Gestión    [FIX dur 2->4]
    ('cristian.delgado', 51, 3, 'B', 1, 5, 4),     -- Seguridad en Riesgos  [AGREGADO, faltaba]
    ('graciela.maidana', 13, 3, 'B', 1, 9, 2),     -- Psicología bloque 1   [FIX: era Literatura/Susana por error]
    ('susana.alvarenga', 16, 3, 'B', 2, 1, 4),     -- Literatura            [FIX dur 2->4]
    ('andres.rojas', 48, 3, 'B', 2, 5, 2),         -- Mate_Aplicada         [FIX: hora 9->5]
    ('claudia.burgos', 7, 3, 'B', 2, 7, 2),        -- Física Aplicada       [AGREGADO, faltaba]
    ('federico.gonzalez', 43, 3, 'B', 2, 9, 4),    -- Laboratorio Java      [FIX: hora 5->9]
    ('cristian.delgado', 50, 3, 'B', 3, 1, 4),
    ('federico.gonzalez', 43, 3, 'B', 3, 5, 4),    -- Laboratorio Java      [FIX: era Redes/Delgado por error]
    ('ruth.estigarribia', 12, 3, 'B', 3, 9, 2),
    ('gustavo.ramirez', 9, 3, 'B', 3, 11, 2),      -- Historia              [AGREGADO, faltaba]
    ('oscar.villasanti', 4, 3, 'B', 3, 13, 2),
    ('ruth.roman', 15, 3, 'B', 4, 1, 4),
    ('andres.rojas', 11, 3, 'B', 4, 5, 4),         -- Mate_Común            [FIX: era Psicología/Irma Cardozo por error]
    ('federico.gonzalez', 42, 3, 'B', 4, 9, 4),    -- Laboratorio Android   [FIX dur 2->4]
    ('laura.rivas', 2, 3, 'B', 5, 1, 2),
    ('graciela.maidana', 13, 3, 'B', 5, 3, 2),     -- Psicología bloque 2   [AGREGADO, faltaba]
    ('graciela.lopez', 41, 3, 'B', 5, 5, 4);       -- Algorítmica           [FIX dur 2->4]

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
