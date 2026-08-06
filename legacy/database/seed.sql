-- Seed data for ctndb
-- Limpia las tablas y recarga los datos de ejemplo

use ctndb;

CREATE TABLE IF NOT EXISTS planilla_rasgo (
	id INT AUTO_INCREMENT PRIMARY KEY,
	curso_id INT NOT NULL,
	profesor_id INT NOT NULL,
	tema VARCHAR(150) NOT NULL,
	fecha_clase DATE NOT NULL,
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

-- ========================================
-- CURSOS
-- ========================================
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
-- Todas las contraseñas de profesor usan BCrypt. Los usuarios que no especifican contraseña en
-- el INSERT inicial reciben el valor por defecto "password", que aquí se hashea explícitamente.
INSERT INTO profesor (id, nombre, apellido, usuario, contrasenia, nivel, ci, telefono, celular) VALUES
(1, 'Graciela', 'López', 'glopez', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9jtuemcwvEuWwPq16/n8.q', 1, 1234567, 0999123456, 0999123456),
(2, 'Daniel', 'Lenguaza', 'dlenguaza', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(3, 'Mirian', 'Montania', 'mmontania', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(4, 'Federico', 'González', 'fgonzalez', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(5, 'Irma', 'Cardozo', 'icardozo', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(6, 'Cristian', 'Delgado', 'cdelgado', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(7, 'Susana', 'Alvarenga', 'salvarenga', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(8, 'Ruth', 'Estigarribia', 'restigarribia', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(9, 'Ruth', 'Roman', 'rroman', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(10, 'Laura', 'Rivas', 'lrivas', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(11, 'Claudia', 'Burgos', 'cburgos', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(12, 'Gerardo', 'Ovelar', 'govelar', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(13, 'Andres', 'Rojas', 'arojas', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 1, NULL, NULL, NULL),
(14, NULL, NULL, 'admin', '$2b$12$Mx8sHmuXDLnNPxb65J.0fObQGnStITg9tuemcwvEuWwPq16/n8.q', 2, NULL, NULL, NULL),
(15, 'Integración', 'Informática', 'informatica-itg', '$2b$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', 3, NULL, NULL, NULL),
(16, 'Integración', 'Electricidad', 'electricidad-itg', '$2b$12$o6GzQT.DgSo9ia7uEtTTCO6twr/1tJ0.YdINSv.LZGS2qwzIIjKjO', 3, NULL, NULL, NULL);

-- ========================================
-- MATERIAS
-- ========================================
-- Las materias ya no se insertan en el seed inicial.
-- El catálogo se construye desde el perfil del profesor mediante
-- el formulario de integración y la carga manual de materias.

-- ========================================
-- RELACIÓN MATERIA-ESPECIALIDAD
-- ========================================
-- Las relaciones de especialidad se resuelven en la UI de perfil
-- al guardar una materia nueva, sin depender del seed fijo.

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
-- PADRES
-- ========================================
-- Password de ejemplo: ctn2025
INSERT INTO padre (id, ci, nombre, apellido, usuario, contrasenia, telefono, correo) VALUES
(1, 1111111, 'Carlos', 'González', 'padre1', '$2b$12$dY1Gt90541sE0Z2H0Rq2ZOuGo4RyzE3iY9jSE6uPPR/gBfx1ClK2y', '0971000000', 'padre1@example.com');

-- ========================================
-- PLANILLAS
-- ========================================
-- Las planillas se crean desde la UI del profesor y la integración con
-- Classroom/curso, en lugar de insertarse de forma fija aquí.

-- ========================================
-- ALUMNOS
-- ========================================
INSERT INTO `alumno` (`nombre`, `apellido`, `curso_id`, `ci`, `correo_encargado`, `correo_encargado2`) VALUES
('LUCAS MATHIAS', 'ALCARAZ PEREIRA', 5, 6041149, 'diegoalcarazm@hotmail.com', ''),
('AMIRA ABIGAIL', 'BALBUENA AYALA', 5, 6020173, 'juan05.balbuena@gmail.com', ''),
('CRISTIAN ARIEL DAVID', 'BENITEZ MORALES', 5, 6194716, 'JABENITEZ76@gmail.com', ''),
('BRUNO GABRIEL', 'BERNAL GONZÁLEZ', 5, 7525296, 'jabernalh@hotmail.com', ''),
('SOFÍA NATHALIA', 'ACOSTA FIGUEREDO', 5, 5871401, 'baldofs@gmail.com', ''),
('JONATHAN ANTHONY', 'BRAY CRISTALDO', 5, 5769689, '56tmb78@gmail.com', ''),
('NICOLÁS MANUEL', 'DIAZ BENITEZ', 5, 6141269, 'jdiazganadero@gmail.com', ''),
('ISAAC NICOLÁS', 'FELTES GIMÉNEZ', 5, 5798259, 'nfeltesquenhan@gmail.com', ''),
('YU JU ', 'HUANG PAN', 5, 6615720, 'mirasolhuang@gm\ail.com', ''),
('FABRIZIO SEBASTIAN', 'IRALA ACUÑA', 5, 6069402, 'iralav@hotmail.com', ''),
('MARLEY IVÁN JAVIER', 'LEZCANO CASTRO', 5, 6039182, 'kikolez@gmail.com', ''),
('BIANCA MARÍA', 'LEZCANO VILLALBA', 5, 7826727, 'villalbairma17@gmail.com', ''),
('ASTRID MILENA', 'MARTINEZ ZYUMANSCKI', 5, 7334549, 'hugomarcelomartinez@gmail.com', ''),
('RODNEY MAURICIO', 'MAZIER PAIVA', 5, 6132693, 'maziermouricio@gmail.com', ''),
('LUCAS DAMIÁN ', 'MELGAREJO TABOADA', 5, 6050835, 'aletaboada.1983@gmail.com', ''),
('ALEXANDER', 'MENCIA MELGAREJO', 5, 6290996, 'omat16@hotmail.com', ''),
('ELIAM', 'MENDOZA ESTIGARRIBIA', 5, 6047128, 'rulitosctn@gmail.com', ''),
('LARISSA JAZMÍN', 'OLMEDO CANO', 5, 5986397, 'patricia_elizabeth@live.com', ''),
('EVER MOÍSES', 'ORTEGA PORTILLO', 5, 6564444, 'sunildaportillo74@gmail.com', ''),
('CRISTIAN ALEJANDRO', 'PAREDES GALEANO', 5, 6185976, 'm.Veronica.galeano@gmail.com', ''),
('MATIAS FEDERICO', 'PEREIRA PINTOS', 5, 6070993, 'freddypereira4475@hotmail.com', ''),
('MARÍA LUJAN', 'RAMÍREZ RAMOS', 5, 5965357, 'ramosnaty1@hotmail.com', ''),
('ALFREDO DAICHI', 'UESUGUI ISAWA', 5, 6508732, 'yunvesugui@gmail.com', ''),
('MARCELO JAVIER', 'VÁZQUEZ AMARILLA', 5, 7012592, 'orlandomarcelo.vazquez@gmail.com', ''),
('IAN NAHUEL', 'VILLALBA CUENCA', 5, 5922541, 'arillalba@gmail.com', ''),
('MARCELO RYOMA', 'WATANABE ISHIZAKI', 5, 7531146, 'el.japo84@gmail.com', ''),
('HORACIO ADRÍAN', 'ZÁRATE ALVARENGA', 5, 6109942, 'susyalvarenga@gmail.com', ''),
('CHING WEN', 'YANG', 5, 7326156, 's0985222555@gmail.com', ''),
('IRIS SABRINA', 'ACOSTA MAÍZ', 6, 6566764, 'maizcoronel@gmail.com', ''),
('ABIGAIL', 'TEIXIDO LÓPEZ FONSECA', 5, 5933046, 'joesys@gmail.com', ''),
('MARÍA AGUSTINA', 'AGUILERA ALIMIRÓN', 6, 7236177, 'cesarbrumado@hotmail.com', ''),
('AUGUSTO FABRICIO', 'ARGAÑA BÁEZ', 6, 5998083, 'anyi198192@gmail.com', ''),
('DIEGO MATEO ', 'AYALA ALARCON', 6, 6796006, 'netdiego81@gmail.com', ''),
('JOHAN AARON', 'BARRAL PEREZ', 6, 8224633, 'Noeliapema@gmail.com', ''),
('FABRIZIO BENJAMIN', 'BENITEZ ROLON', 6, 6124217, 'pichado27@gmail.com', ''),
('ENZO BENJAMÍN', 'BRESANOVICH VILLALBA', 6, 6541823, 'Abelbresa21@hotmail.com', ''),
('MATHIAS SAUL', 'CABRAL CABALLERO', 6, 6296417, 'hogowawe@gmail.com', ''),
('EVER MARCELO', 'CENTURIÓN MAQUEDA', 6, 6139872, 'ever_richi@hotmail.com', ''),
('ESTEBAN DANIEL', 'CHAMORRO VILLAMAYOR', 6, 6254309, 'mearmenvilla76@gmail.com', ''),
('ESTEBAN NICOLÁS', 'CORONEL COLMÁN', 6, 6153765, 'Jbccoronel@gmail.com', ''),
('CAMILO RAFAEL', 'CUBILLA IBARRA', 6, 5892895, 'cubillalidio1973@gmail.com', ''),
('NATHALIA YERUTI', 'DENIS TRINIDAD', 6, 6326385, 'patriciatrinidad@gmail.com', ''),
('HUMBERTO DAMIAN', 'DIAZ RICCIARDI', 6, 6752131, 'NULL@null.com', ''),
('ERICK DAMIÁN', 'GONZÁLEZ CORONEL', 6, 6308749, 'auroragonzalezcoronel@gmail.com', ''),
('MATHIAS HERNAN', 'MEDINA ALDERETE', 6, 6141116, 'gustavomedin@hotmail.com', ''),
('MARIANGEL VALENTINA', 'MEZA GARCETE', 6, 6500202, 'carmengarcetec@gmail.com', ''),
('MIGUEL ANGEL', 'MOLINAS ESPINOLA', 6, 7015595, 'antonio.molinas@gmail.com', ''),
('FIORELLA ARAMI', 'PALACIOS BORDON', 6, 7284953, 'NULL@null.com', ''),
('JHONATAN EMANUEL', 'PALACIOS ZORRILLA', 6, 6122647, 'markpalaciosm@gmail.com', ''),
('OLIVER', 'PASOTTINI CACERES', 6, 6066191, 'NULL@null.com', ''),
('RAMON JUAN SEBASTIAN', 'PERALTA MERCADO', 6, 6292642, 'Mmercado1975@gmail.com', ''),
('FRANCO GIOVANNI', 'RODRÍGUEZ BRITEZ', 6, 6102476, 'ceram.rod@gmail.com', ''),
('NELSON ELÍAS BENJAMÍN', 'RUIZ DIAZ VILLAGRA', 6, 5889119, 'brauruizdidz@gmail.com', ''),
('JAZMÍN', 'SALINAS PRESENTADO', 6, 5888187, 'NULL@null.com', ''),
('ISAIAS DANIEL', 'ZELADA MARTINEZ', 6, 6100013, 'ismael_zelada@gmail.com', ''),
('JENNIFER AMIRA', 'FLORES ARIAS', 6, 5985027, 'jenniferaflores12147@gmail.com', ''),
('MARIELA CHONG AH', 'ACOSTA POSADAS', 1, 6634030, 'posadasmiriam7@gmail.com', 'aacosta352@gmail.com'),
('PEDRO JOSÉ', 'ALDERETE PÁEZ', 1, 6599141, 'dograpaez82@gmail.com', 'serafinialderete@hotmail.com'),
('GUILLERMO MANUEL', 'APONTE RAMÍREZ', 1, 6375687, 'deinyraq@hotmail.com', 'juanaponte1981@gmail.com'),
('ARIEL MAXIMILIANO', 'ARAUJO SOSA', 1, 7868229, 'imbso73@gmail.com', 'araujodionisio03@gmail.com'),
('TYRA SELENE', 'BARBOZA CABRERA', 1, 6514004, 'ocacabrera@gmail.com', 'ribarboz@hotmail.com'),
('JUAN GABRIEL', 'CORONEL VILLALBA', 1, 6780823, NULL, 'franciscocoronel753@gmail.com'),
('MICAELLA VALENTINA', 'ESPINOZA BELLOTO', 1, 6323591, 'isabelbelotto@gmail.com', 'espinozacelso@gmail.com'),
('IVAN ALEXANDER', 'FERNÁNDEZ MEZA', 1, 6674310, NULL, 'hugoconsulramon705@gmail.com'),
('JUAN FABRICIO', 'FLEITAS IBÁÑEZ', 1, 7208277, 'ibanezmariaestela86@gmail.com', 'fleitasj277@gmail.com'),
('LIA JAZMIN', 'FLEITAS PÉREZ', 1, 8177227, 'daidahipy@gmail.com', 'buysellpy@gmail.com'),
('BRAYAN', 'GARCÍA FERNÁNDEZ', 1, 8563705, 'janetfernandez192@gmail.com', 'javiergarciameijide78@gmail.com'),
('MARIANA EMILIA', 'GONZÁLEZ CASTRO', 1, 6738451, 'marta.caso415@gmail.com', 'domingoaquiles@gmail.com'),
('RAFFAELL', 'GONZÁLEZ LARREA', 1, 6623572, 'na-la-ote@hotmail.com', 'judivepa@gmail.com'),
('ÁNGEL JOSÉ IVAN', 'MACIEL RUÍZ DÍAZ', 1, 8079060, 'ruizdiazbarriosepifania@gmail.com', NULL),
('RICARDO GERMAN', 'MARTÍNEZ ROJAS', 1, 7488331, 'digracie10@gmail.com', NULL),
('MOISES', 'MELGAREJO SAUCEDO', 1, 7230274, 'candidasaucedo4@gmail.com', 'callomelgarejo@gmail.com'),
('RODRIGO GABRIEL', 'MOREL MORENO', 1, 7383873, 'morelu71@gmail.com', NULL),
('EMILIO JOSÉ', 'MORÍNIGO PEÑA', 1, 7071354, 'emiliomorinigo@hotmail.com', NULL),
('ANGÉLICA SUSANA', 'ORUÉ AYALA', 1, 6619509, NULL, 'oscarorue346@gmail.com'),
('TANIA GUADALUPE', 'PAIVA SOTELO', 1, 7209622, 'estelamary198@gmail.com', 'faustopaivacolman@gmail.com'),
('JUAN JOSÉ', 'PALMA RODRÍGUEZ', 1, 6813981, 'marlenerodriguez0076@gmail.com', 'jdipalma033@gmail.com'),
('ALEJANDRO JOSÍAS', 'PÉREZ ÁVALOS', 1, 6534642, 'avalosblancogisellecorina@gmail.com', NULL),
('VINICIUS', 'RODRÍGUEZ DE OLIVEIRA', 1, 8758628, 'oliveiraclauder@hotmail.com', NULL),
('JOSÍAS ALEXANDER', 'SANTACRUZ OTAZU', 1, 6632204, 'lisandraotazulopez05@gmail.com', 'fidelafhemirsantacruzrojas@gmail.com'),
('ALESSANDRO JULIÁN', 'UNZAIN INSFRÁN', 1, 6599080, 'lilainsfrand@gmail.com', 'junzain@gmail.com'),
('SOFÍA ARAMÍ', 'VERA MARTÍNEZ', 1, 6658849, 'katiayissel@hotmail.com', 'diegogavin83@gmail.com'),
('FABIOLA LUJÁN', 'VERÓN MONGELÓS', 1, 6625127, 'fabiluueron@gmail.com', 'funcargo2020@gmail.com'),
('WENDY AYELÉN', 'ZÁRATE ROJAS', 1, 6781794, 'alice.rojas22@gmail.com', 'nelsonzarate3185@gmail.com'),

('FELIX HERNAN', 'ALCARAZ MEZA', 2, 6549365, 'deliameza2015@gmail.com', NULL),
('YAGO LAREN', 'AMARILLA LEGUIZAMON', 2, 6581374, 'yanine.leguizamon@gmail.com', 'ajamarilla@gmail.com'),
('MONSERRAT ANAHI', 'AYALA GAUTO', 2, 6693608, 'cynthiagauto84@gmail.com', 'cucumelero2008@hotmail.com'),
('DYLAN VIRGILIO', 'BURGOS ROTELA', 2, 7401358, 'lilianarotelag@gmail.com', NULL),
('ISAAC ULISES', 'CUEVAS SAAVEDRA', 2, 6613266, 'gloriaelizabethsaavedra@gmail.com', 'gustavocuevasvazquez@gmail.com'),
('ANGEL GABRIEL', 'DIAZ CAÑETE', 2, 7293215, 'michelscanete90@gmail.com', NULL),
('FEDERICO AMIN', 'DOMINGUEZ SOSA', 2, 6538527, 'paolasosa1982@gmail.com', 'cadconstrucciones@gmail.com'),
('ALEJANDRA ANAHI', 'ESCOBAR OJEDA', 2, 6833279, 'gracielaescobar86@gmail.com', NULL),
('EDEL JAZMIN', 'FRANCO MACIEL', 2, 6593803, 'edelsita09@gmail.com', 'seralber86@gmail.com'),
('ALEXIS DANIEL', 'FRETEZ VILLAMAYOR', 2, 6582254, 'porfi.ac@gmail.com', 'david-fretes83@hotmail.com'),
('SAULO EZEQUIEL', 'GALEANO RIVEROS', 2, 6704166, 'criveros@vet.una.py', 'arielgaleanobaez@gmail.com'),
('LUCAS GABRIEL', 'GAUTO NUÑEZ', 2, 6325567, 'lucriszv@gmail.com', 'jose19gauto@gmail.com'),
('ADRIAN', 'GRASSO RAMOS', 2, 6617987, 'lramos@grupofaviola.com.py', 'sergio-grasso2011@hotmail.com'),
('MILAGROS MARGARITA YERUTI', 'GUPPI BORDON', 2, 8506321, 'sanibordon@gmail.com', NULL),
('AMILCAR ANDRES', 'JARA AGUILERA', 2, 7138719, 'lorenamap84@gmail.com', NULL),
('DANAE ABIGAIL', 'JARA MARTINEZ', 2, 7551072, 'paokarina03@gmail.com', 'carlosjarabaez@gmail.com'),
('MATEO FERNANDO', 'LENCINA AREVALOS', 2, 6883337, 'silcah@gmail.com', 'silvio-lencina@hotmail.com'),
('MARTIN ALEJANDRO', 'LEZCANO MONTIEL', 2, 6626178, 'benimabel85@gmail.com', NULL),
('THIAGO VALENTINO', 'MARTINEZ FERNANDEZ', 2, 6727372, 'canolafernandezduarte@gmail.com', 'gasparmartinez06@gmail.com'),
('TOBIAS EZEQUIEL', 'MEDINA GONZALEZ', 2, 6512532, 'justialegria@gmail.com', NULL),
('VALERIA NOEMI', 'MONTIEL TRIVERO', 2, 7337850, 'natasha.triverofreyre@gmail.com', NULL),
('NATHALIA MARIELA', 'ORTIZ RODRIGUEZ', 2, 6532910, 'rodriguezramirezmariela@gmail.com', 'ortizariashector@gmail.com'),
('OSIAS BENJAMIN', 'RUBIO SAMUDIO', 2, 6971481, 'patysam1515@gmail.com', 'frubio9987@hotmail.com'),
('GIOVANNI JOSE', 'RUIZ ROMAN', 2, 7099638, 'lizipauz@gmail.com', NULL),
('SAMYRA ANAHI', 'SANCHEZ AGUILAR', 2, 7086918, 'sameyve1234@gmail.com', NULL),
('ENZO SIMON', 'SANCHEZ VERON', 2, 6966829, NULL, NULL),
('MARIA TANIA', 'SOILAN SOSA', 2, 6634375, NULL, 'miguel.soilan@rieder.com.py'),
('FIORELLA MAGALI', 'VILLAMAYOR VAZQUEZ', 2, 7225342, 'carinafio78@gmail.com', NULL),
('JORGE JOAQUIN', 'GONZALEZ BAEZ', 4, 6300937, 'NULL@null.com', ''),
('PAZ FIORELLA', 'ACUÑA RODRIGUEZ', 3, 6552138, NULL, NULL),
('GABRIELA ELIZABETH', 'ALEGRE ORTIZ', 3, 6520371, NULL, NULL),
('CESAR EZEQUIEL', 'AMARILLA ETTIENE', 3, 7011624, NULL, NULL),
('FERNANDO JOSE', 'BARRETO ROCHE', 3, 6271898, NULL, NULL),
('MARIA CECILIA', 'BENITEZ BARRIOS', 3, 7350265, NULL, NULL),
('SOFIA ESMERALDA', 'BENITEZ MARTINEZ', 3, 7290536, NULL, NULL),
('VALERIA ALEJANDRA', 'CACERES ACHUCARRO', 3, 7536039, NULL, NULL),
('CARLOS ANTONIO', 'CANDIA ROMERO', 3, 6895905, NULL, NULL),
('JONAS ALEXANDER', 'CUBILLA MORINIGO', 3, 7979695, NULL, NULL),
('ALICE GISSELLE', 'DIAZ AMARILLA', 3, 6274837, NULL, NULL),
('KEVIN MATIAS', 'DURE AQUINO', 3, 6711232, NULL, NULL),
('THIAGO DAVID', 'ESTIGARRIBIA DELGADILLO', 3, 6911572, NULL, NULL),
('GLORIA MILENA', 'FARIÑA NUÑEZ', 3, 6363114, NULL, NULL),
('LUCIO ALESSANDRO', 'GAMARRA AGUAYO', 3, 6216256, NULL, NULL),
('LUZ NAHIARA', 'GAYOZO AVALOS', 3, 6218519, NULL, NULL),
('THIAGO ALEXANDER', 'LEON CORONEL', 3, 6168091, NULL, NULL),
('LUCAS ABDIEL', 'MARTINEZ GONZALEZ', 3, 6219481, NULL, NULL),
('CHRISTOPHER IVAN', 'MARTINEZ INSFRAN', 3, 7449854, NULL, NULL),
('MARCOS DANIEL', 'MOLINAS LEON', 3, 6820120, NULL, NULL),
('JOSHUA FABRIZIO', 'MONGELOS CAMACHO', 3, 6656584, NULL, NULL),
('MIANE MARIA VERONICA', 'NOGUERA AVILA', 3, 6298042, NULL, NULL),
('ALAN ENRIQUE DAMIAN', 'OJEDA OLIVER', 3, 6840108, NULL, NULL),
('ALEXANDER AGUSTIN', 'OLMEDO RODRIGUEZ', 3, 6658507, NULL, NULL),
('SAMUEL JESUS', 'SCHMIDT SILVEIRA', 3, 6595852, NULL, NULL),
('JOSE FEDERICO', 'SOLER VAZQUEZ', 3, 7309281, NULL, NULL),
('MIKAHELA', 'SUAREZ ARZA', 3, 6711101, NULL, NULL),
('LEONARDO', 'VALINOTTI  PAREDES', 3, 6761746, NULL, NULL),
('FACUNDO BENJAMIN', 'VERA SALINAS', 3, 7007217, NULL, NULL),

('EMILIO ANDRES', 'ALMIRON RUIZ', 4, 8651544, NULL, NULL),
('JORGE DAVID', 'AVEIRO DURE', 4, 6763135, NULL, NULL),
('GABRIELA DENISSE', 'BENITEZ CAMPUZANO', 4, 6248031, NULL, NULL),
('PAMELA MONSERRAT', 'CABALLERO ZARACHO', 4, 6122730, NULL, NULL),
('FABRICIO NICOLAS', 'CUBAS VAZQUEZ', 4, 6299174, NULL, NULL),
('JESUS MARIA', 'DAVID RESQUIN', 4, 7112304, NULL, NULL),
('SANTIAGO DIDIER DAMASO', 'DELVALLE CABRAL', 4, 6323522, NULL, NULL),
('PAULO GASTON', 'DUARTE ORUE', 4, 6506158, NULL, NULL),
('ALBA MARIA ELIZABETH', 'FARIÑA MORAN', 4, 6682899, NULL, NULL),
('EVELYN CECILIA', 'GALEANO DUARTE', 4, 6254779, NULL, NULL),
('FRANCO GONZALO', 'GARCIA GARCIA', 4, 6378044, NULL, NULL),
('ANGELO GASTON', 'GONZALEZ AMARILLA', 4, 6306858, NULL, NULL),
('JUANA DAMARIS', 'HUACCA ALEJO', 4, 9132227, NULL, NULL),
('MILAGROS MICAELA', 'JIMENEZ ROJAS', 4, 6276848, NULL, NULL),
('LUCAS   MANUEL', 'LOPEZ ALDERETE', 4, 6709236, NULL, NULL),
('PABLO LEANDRO', 'LOPEZ PULLARES', 4, 6128349, NULL, NULL),
('LUNA MIA', 'MENDIETA', 4, 6521146, NULL, NULL),
('VICTOR MANUEL', 'MENDIETA PEREIRA', 4, 7965966, NULL, NULL),
('GAIA VIOLETA MARIA', 'MOREL AREVALOS', 4, 6315503, NULL, NULL),
('FACUNDO MATHIAS', 'PRIETO CACERES', 4, 7277773, NULL, NULL),
('AIDEE FIORELLA', 'RECALDE CASTILLO', 4, 7116092, NULL, NULL),
('YANARA AYELEN DOMINGA', 'RODAS VALDEZ', 4, 6337830, NULL, NULL),
('FIORELLA ANAHI', 'SOSA AMARILLA', 4, 7934035, NULL, NULL),
('SANTIAGO', 'SOSA OVELAR', 4, 6138828, NULL, NULL),
('ANA BELEN', 'VARGAS VALIENTE', 4, 6597209, NULL, NULL),
('HEATHER PATRICIA', 'WATTIEZ BAREIRO', 4, 6600003, NULL, NULL),
('ELIAS SEBASTIAN', 'ZORRILLA BENITEZ', 4, 6355776, NULL, NULL);

INSERT INTO alumno_padre (alumno_id, padre_id, parentesco) VALUES
(1, 1, 'padre');

-- Las planillas, tareas y registros se crean desde la UI del profesor.
-- El seed solo deja la base relacional mínima para que el portal de padres
-- y la carga inicial del sistema no dependan de filas fijas en SQL.