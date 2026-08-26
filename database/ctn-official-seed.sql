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

-- ========================================
-- USUARIOS BASE
-- ========================================
-- Tipo de usuario (nivel):
-- 1 = profesor
-- 2 = evaluador
-- 3 = admin
-- 4 = padre

INSERT INTO usuario (
    id, nombre, apellido, usuario, contrasenia, ci, telefono, celular, correo,
    google_email, nivel, activity_log_path, especialidad_id
) VALUES
    -- Administrador global
    (1, 'Administrador', 'Global', 'global_admin', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 1, null, null, null,
    null, 3, 'admin_global.txt', null),
    
    -- Administradores de especialidad
    (2, 'Administracion', 'Construcciones Civiles', 'admin_cc', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 2, null, null, null,
    null, 3, 'admin_construcciones_civiles.txt', 1),
    (3, 'Administracion', 'Electricidad', 'admin_electricidad', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 3, null, null, null,
    null, 3, 'admin_electricidad.txt', 2),
    (4, 'Administracion', 'Electronica', 'admin_electronica', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 4, null, null, null,
    null, 3, 'admin_electronica.txt', 3),
    (5, 'Administracion', 'Electromecanica', 'admin_electromecanica', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 5, null, null, null,
    null, 3, 'admin_electromecanica.txt', 4),
    (6, 'Administracion', 'Informatica', 'admin_informatica', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 6, null, null, null,
    null, 3, 'admin_informatica.txt', 5),
    (7, 'Administracion', 'Mecanica General', 'admin_mecanica_general', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 7, null, null, null,
    null, 3, 'admin_mecanica_general.txt', 6),
    (8, 'Administracion', 'Mecanica Automotriz', 'admin_mecanica_automotriz', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 8, null, null, null,
    null, 3, 'admin_mecanica_automotriz.txt', 7),
    (9, 'Administracion', 'Quimica Industrial', 'admin_quimica_industrial', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 9, null, null, null,
    null, 3, 'admin_quimica_industrial.txt', 8),
    
    -- Profesores
    -- Hasta ahora solo disponemos los profes que van en informatica, pero se pueden agregar los de las otras especialidades si se desea.
    (10, 'Abner', 'Alcaraz', 'abner.alcaraz', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'abner_alcaraz.txt', 5),
    (11, 'Alcira', 'Cáceres', 'alcira.caceres', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'alcira_caceres.txt', 5),
    (12, 'Andres', 'Rojas', 'andres.rojas', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'andres_rojas.txt', 5),
    (13, 'Claudia', 'Burgos', 'claudia.burgos', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'claudia_burgos.txt', 5),
    (14, 'Cristian', 'Delgado', 'cristian.delgado', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'cristian_delgado.txt', 5),
    (15, 'Daniel', 'Lenguaza', 'daniel.lenguaza', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'daniel_lenguaza.txt', 5),
    (16, 'Emilce', 'Jara', 'emilce.jara', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'emilce_jara.txt', 5),
    (17, 'Federico', 'González', 'federico.gonzalez', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'federico_gonzalez.txt', 5),
    (18, 'Gerardo', 'Ovelar', 'gerardo.ovelar', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'gerardo_ovelar.txt', 5),
    (19, 'Graciela', 'López', 'graciela.lopez', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'graciela_lopez.txt', 5),
    (20, 'Graciela', 'Maidana', 'graciela.maidana', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'graciela_maidana.txt', 5),
    (21, 'Gustavo', 'Ramirez', 'gustavo.ramirez', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'gustavo_ramirez.txt', 5),
    (22, 'Irma', 'Cardozo', 'irma.cardozo', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'irma_cardozo.txt', 5),
    (23, 'Juan', 'Acosta', 'juan.acosta', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'juan_acosta.txt', 5),
    (24, 'Laura', 'Rivas', 'laura.rivas', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'laura_rivas.txt', 5),
    (25, 'Lourdes', 'Galeano', 'lourdes.galeano', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'lourdes_galeano.txt', 5),
    (26, 'Luz', 'Angulo', 'luz.angulo', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'luz_angulo.txt', 5),
    (27, 'Mirian', 'Montania', 'mirian.montania', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'mirian_montania.txt', 5),
    (28, 'Oscar', 'Ibarrola', 'oscar.ibarrola', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'oscar_ibarrola.txt', 5),
    (29, 'Oscar', 'Villasanti', 'oscar.villasanti', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'oscar_villasanti.txt', 5),
    (30, 'Romy', 'Aguilera', 'romy.aguilera', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'romy_aguilera.txt', 5),
    (31, 'Ruth', 'Estigarribia', 'ruth.estigarribia', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'ruth_estigarribia.txt', 5),
    (32, 'Ruth', 'Román', 'ruth.roman', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'ruth_roman.txt', 5),
    (33, 'Susana', 'Alvarenga', 'susana.alvarenga', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'susana_alvarenga.txt', 5),
    (34, 'Zully', 'Nuñez', 'zully.nunez', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 10, null, null, null, null, 1, 'zully_nunez.txt', 5),
    -- Reservado hasta 90...

    -- Evaluadores
    (91, 'Evaluador', '1', 'evaluador1', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 11, null, null, null, null, 2, 'evaluador_1.txt', null),
    (92, 'Evaluador', '2', 'evaluador2', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 12, null, null, null, null, 2, 'evaluador_2.txt', null),
    (93, 'Evaluador', '3', 'evaluador3', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 13, null, null, null, null, 2, 'evaluador_3.txt', null),
    (94, 'Evaluador', '4', 'evaluador4', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 14, null, null, null, null, 2, 'evaluador_4.txt', null),
    (95, 'Evaluador', '5', 'evaluador5', '$2a$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 15, null, null, null, null, 2, 'evaluador_5.txt', null)

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

-- Especificas
    -- Informática (ID 41-55)
    (41, 'Administración Financiera', 'especifica'),
    (42, 'Algorítmica', 'especifica'),
    (43, 'Laboratorio Android', 'especifica'),
    (44, 'Laboratorio Java', 'especifica'),
    (45, 'Laboratorio Linux', 'especifica'),
    (46, 'Laboratorio Python', 'especifica'),
    (47, 'Laboratorio SQL', 'especifica'),
    (48, 'Laboratorio Web', 'especifica'),
    (49, 'Literatura', 'especifica'),
    (50, 'Matemática Aplicada', 'especifica'),
    (51, 'Plan de Lectura', 'especifica'),
    (52, 'Laboratorio Redes', 'especifica'),
    (53, 'Seguridad en Riesgos Eléctricos', 'especifica'),
    (54, 'Dibujo Técnico', 'especifica'),
    (55, 'Info General', 'especifica');

    -- Las demas quedan pendientes a cargar...

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

-- Commit message: "Seed data for users, subjects, instruments, and students inserted into the database."