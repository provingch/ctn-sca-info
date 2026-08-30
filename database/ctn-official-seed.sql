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
    null, 3, 'admin_global.txt', null),
    
    -- Administradores de especialidad
    (2, 'Administracion', 'Construcciones Civiles', 'admin_cc', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 2, null, null, null,
    null, 3, 'admin_construcciones_civiles.txt', 1),
    (3, 'Administracion', 'Electricidad', 'admin_electricidad', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 3, null, null, null,
    null, 3, 'admin_electricidad.txt', 2),
    (4, 'Administracion', 'Electronica', 'admin_electronica', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 4, null, null, null,
    null, 3, 'admin_electronica.txt', 3),
    (5, 'Administracion', 'Electromecanica', 'admin_electromecanica', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 5, null, null, null,
    null, 3, 'admin_electromecanica.txt', 4),
    (6, 'Administracion', 'Informatica', 'admin_informatica', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 6, null, null, null,
    null, 3, 'admin_informatica.txt', 5),
    (7, 'Administracion', 'Mecanica General', 'admin_mecanica_general', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 7, null, null, null,
    null, 3, 'admin_mecanica_general.txt', 6),
    (8, 'Administracion', 'Mecanica Automotriz', 'admin_mecanica_automotriz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 8, null, null, null,
    null, 3, 'admin_mecanica_automotriz.txt', 7),
    (9, 'Administracion', 'Quimica Industrial', 'admin_quimica_industrial', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 9, null, null, null,
    null, 3, 'admin_quimica_industrial.txt', 8),
    
    -- Profesores
    -- Hasta ahora solo disponemos los profes que van en informatica, pero se pueden agregar los de las otras especialidades si se desea.
    (10, 'Abner', 'Alcaraz', 'abner.alcaraz', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'abner_alcaraz.txt', null),
    (11, 'Alcira', 'Cáceres', 'alcira.caceres', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'alcira_caceres.txt', null),
    (12, 'Andres', 'Rojas', 'andres.rojas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'andres_rojas.txt', null),
    (13, 'Claudia', 'Burgos', 'claudia.burgos', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'claudia_burgos.txt', null),
    (14, 'Cristian', 'Delgado', 'cristian.delgado', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'cristian_delgado.txt', null),
    (15, 'Daniel', 'Lenguaza', 'daniel.lenguaza', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'daniel_lenguaza.txt', null),
    (16, 'Emilce', 'Jara', 'emilce.jara', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'emilce_jara.txt', null),
    (17, 'Federico', 'González', 'federico.gonzalez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'federico_gonzalez.txt', null),
    (18, 'Gerardo', 'Ovelar', 'gerardo.ovelar', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'gerardo_ovelar.txt', null),
    (19, 'Graciela', 'López', 'graciela.lopez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'graciela_lopez.txt', null),
    (20, 'Graciela', 'Maidana', 'graciela.maidana', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'graciela_maidana.txt', null),
    (21, 'Gustavo', 'Ramirez', 'gustavo.ramirez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'gustavo_ramirez.txt', null),
    (22, 'Irma', 'Cardozo', 'irma.cardozo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'irma_cardozo.txt', null),
    (23, 'Juan', 'Acosta', 'juan.acosta', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'juan_acosta.txt', null),
    (24, 'Laura', 'Rivas', 'laura.rivas', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'laura_rivas.txt', null),
    (25, 'Lourdes', 'Galeano', 'lourdes.galeano', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'lourdes_galeano.txt', null),
    (26, 'Luz', 'Angulo', 'luz.angulo', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'luz_angulo.txt', null),
    (27, 'Mirian', 'Montania', 'mirian.montania', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'mirian_montania.txt', null),
    (28, 'Oscar', 'Ibarrola', 'oscar.ibarrola', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'oscar_ibarrola.txt', null),
    (29, 'Oscar', 'Villasanti', 'oscar.villasanti', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'oscar_villasanti.txt', null),
    (30, 'Romy', 'Aguilera', 'romy.aguilera', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'romy_aguilera.txt', null),
    (31, 'Ruth', 'Estigarribia', 'ruth.estigarribia', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'ruth_estigarribia.txt', null),
    (32, 'Ruth', 'Román', 'ruth.roman', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'ruth_roman.txt', null),
    (33, 'Susana', 'Alvarenga', 'susana.alvarenga', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'susana_alvarenga.txt', null),
    (34, 'Zully', 'Nuñez', 'zully.nunez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'zully_nunez.txt', null),
    (35, null, 'Chavez', 'null.chavez', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'null_chavez.txt', null),
    (36, null, 'Mequer', 'null.mequer', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 10, null, null, null, null, 1, 'null_mequer.txt', null),
    -- Reservado hasta 90...

    -- Evaluadores
    (91, 'Evaluador', '1', 'evaluador1', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 11, null, null, null, null, 2, 'evaluador_1.txt', null),
    (92, 'Evaluador', '2', 'evaluador2', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 12, null, null, null, null, 2, 'evaluador_2.txt', null),
    (93, 'Evaluador', '3', 'evaluador3', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 13, null, null, null, null, 2, 'evaluador_3.txt', null),
    (94, 'Evaluador', '4', 'evaluador4', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 14, null, null, null, null, 2, 'evaluador_4.txt', null),
    (95, 'Evaluador', '5', 'evaluador5', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 15, null, null, null, null, 2, 'evaluador_5.txt', null),

    (96, 'CoordinadorPedagogico', '1', 'cpdg1', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 16, null, null, null, null, 5, 'coordinadorpedagogico_1.txt', null),
    (97, 'CoordinadorPedagogico', '2', 'cpdg2', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 17, null, null, null, null, 5, 'coordinadorpedagogico_2.txt', null),
    (98, 'CoordinadorPedagogico', '3', 'cpdg3', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 18, null, null, null, null, 5, 'coordinadorpedagogico_3.txt', null),
    (99, 'CoordinadorPedagogico', '4', 'cpdg4', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 19, null, null, null, null, 5, 'coordinadorpedagogico_4.txt', null),
    (100,'CoordinadorPedagogico', '5', 'cpdg5', '$2a$12$RPIBll3ykfHDr1h1qqPBb.89ekEfpsDjOVV8ehqR9yTrVMVRzcEEq', 20, null, null, null, null, 5, 'coordinadorpedagogico_5.txt', null)

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

-- Todas las contraseñas de los usuarios son "password" y estan encriptadas con BCrypt.

-- ========================================
-- MATERIAS
-- ========================================
INSERT INTO materia (id, nombre, categoria) VALUES
-- Comunes (reservado hasta ID 40)
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


-- Especificas
    -- Informática (ID 41-55)
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

    -- Las demas quedan pendientes a cargar...

INSERT INTO materia_especialidad (materia_id, especialidad_id) VALUES
    -- Comunes (ID 1-16)
    (1, 1),  (1, 2),  (1, 3),  (1, 4),  (1, 5),  (1, 6),  (1, 7),  (1, 8),
    (2, 1),  (2, 2),  (2, 3),  (2, 4),  (2, 5),  (2, 6),  (2, 7),  (2, 8),
    (3, 1),  (3, 2),  (3, 3),  (3, 4),  (3, 5),  (3, 6),  (3, 7),  (3, 8),
    (4, 1),  (4, 2),  (4, 3),  (4, 4),  (4, 5),  (4, 6),  (4, 7),  (4, 8),
    (5, 1),  (5, 2),  (5, 3),  (5, 4),  (5, 5),  (5, 6),  (5, 7),  (5, 8),
    (6, 1),  (6, 2),  (6, 3),  (6, 4),  (6, 5),  (6, 6),  (6, 7),  (6, 8),
    (7, 1),  (7, 2),  (7, 3),  (7, 4),  (7, 5),  (7, 6),  (7, 7),  (7, 8),
    (8, 1),  (8, 2),  (8, 3),  (8, 4),  (8, 5),  (8, 6),  (8, 7),  (8, 8),
    (9, 1),  (9, 2),  (9, 3),  (9, 4),  (9, 5),  (9, 6),  (9, 7),  (9, 8),
    (10, 1), (10, 2), (10, 3), (10, 4), (10, 5), (10, 6), (10, 7), (10, 8),
    (11, 1), (11, 2), (11, 3), (11, 4), (11, 5), (11, 6), (11, 7), (11, 8),
    (12, 1), (12, 2), (12, 3), (12, 4), (12, 5), (12, 6), (12, 7), (12, 8),
    (13, 1), (13, 2), (13, 3), (13, 4), (13, 5), (13, 6), (13, 7), (13, 8),
    (14, 1), (14, 2), (14, 3), (14, 4), (14, 5), (14, 6), (14, 7), (14, 8),
    (15, 1), (15, 2), (15, 3), (15, 4), (15, 5), (15, 6), (15, 7), (15, 8),
    (16, 1), (16, 2), (16, 3), (16, 4), (16, 5), (16, 6), (16, 7), (16, 8),
    
    -- Especificas Informatica (ID 41-54)
    (41, 5), (42, 5), (43, 5), (44, 5), (45, 5), (46, 5), (47, 5), (48, 5),
    (49, 5), (50, 5), (51, 5), (52, 5), (53, 5), (54, 5);

    

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
