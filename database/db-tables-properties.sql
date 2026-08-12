##information_schema|mysql|performance_schema|phpmyadmin

drop database if exists ctndb;
create database ctndb;
use ctndb;

CREATE TABLE especialidad (
    id INT,
    nombre VARCHAR(45) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE curso (
    id INT AUTO_INCREMENT,
    especialidad_id INT NOT NULL,
    promocion SMALLINT UNSIGNED NOT NULL,
    seccion enum('A', 'B', 'C') NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (especialidad_id , promocion , seccion),
    CONSTRAINT fk_curso_especialidad FOREIGN KEY (especialidad_id)
        REFERENCES especialidad (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE alumno (
    id INT AUTO_INCREMENT,
    ci INT UNIQUE,
    nombre VARCHAR(45) NOT NULL,
    apellido VARCHAR(45) NOT NULL,
    curso_id INT NOT NULL,
    correo_encargado VARCHAR(45),
    correo_encargado2 VARCHAR(45),
    google_user_id VARCHAR(255) NULL,
    google_email VARCHAR(255) NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (curso_id)
        REFERENCES curso (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE profesor (
    id INT AUTO_INCREMENT,
    nombre VARCHAR(45),
    apellido VARCHAR(45),
    usuario VARCHAR(45) NOT NULL UNIQUE,
    contrasenia VARCHAR(255) DEFAULT 'password' NOT NULL,
    ci int UNIQUE,
    telefono int,
    celular int,
    correo VARCHAR(45),
    google_email VARCHAR(255),
    google_access_token TEXT NULL,
    google_refresh_token TEXT NULL,
    google_token_expiry BIGINT NULL,
    materias_manual TEXT NULL,
    totp_secret VARCHAR(255) NULL,
    especialidad_id INT NULL,
    nivel TINYINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_profesor_especialidad FOREIGN KEY (especialidad_id)
        REFERENCES especialidad (id)
        ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE TABLE materia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(45) NOT NULL,
    categoria ENUM('comun', 'especifico') NOT NULL,
    UNIQUE KEY uq_materia_nombre (nombre)
);

CREATE TABLE profesor_materia (
    profesor_id INT NOT NULL,
    materia_id INT NOT NULL,
    PRIMARY KEY (profesor_id, materia_id),
    CONSTRAINT fk_pm_profesor FOREIGN KEY (profesor_id)
        REFERENCES profesor (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_pm_materia FOREIGN KEY (materia_id)
        REFERENCES materia (id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE asignacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    profesor_id INT NOT NULL,
    materia_id INT NOT NULL,
    curso_id INT NOT NULL,
    UNIQUE KEY uq_asignacion (profesor_id, materia_id, curso_id),
    CONSTRAINT fk_asig_profesor FOREIGN KEY (profesor_id)
        REFERENCES profesor (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_asig_materia FOREIGN KEY (materia_id)
        REFERENCES materia (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_asig_curso FOREIGN KEY (curso_id)
        REFERENCES curso (id) ON UPDATE CASCADE ON DELETE CASCADE
);

-- Relación N:M: una materia 'comun' puede pertenecer a varias especialidades,
-- una 'especifico' típicamente a una sola.
CREATE TABLE materia_especialidad (
    materia_id INT NOT NULL,
    especialidad_id INT NOT NULL,
    PRIMARY KEY (materia_id, especialidad_id),
    CONSTRAINT fk_me_materia FOREIGN KEY (materia_id)
        REFERENCES materia (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_me_especialidad FOREIGN KEY (especialidad_id)
        REFERENCES especialidad (id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE planilla (
    id INT AUTO_INCREMENT,
    curso_id INT NOT NULL,
    materia_id INT NOT NULL,
    periodo SMALLINT UNSIGNED NOT NULL,
    etapa ENUM('primera', 'segunda') NOT NULL,
    profesor_id INT NOT NULL,
    google_course_id VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE (curso_id, materia_id, periodo, etapa),
    FOREIGN KEY (curso_id)
        REFERENCES curso (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (materia_id)
        REFERENCES materia (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (profesor_id)
        REFERENCES profesor (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE instrumento (
    id INT,
    nombre VARCHAR(45) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE tarea (
    id INT AUTO_INCREMENT PRIMARY KEY,
    planilla_id INT NOT NULL,
    instrumento_id INT NOT NULL,
    fecha DATE NOT NULL,
    total SMALLINT UNSIGNED NOT NULL,
    titulo VARCHAR(100) NOT NULL,
    google_coursework_id VARCHAR(255) NULL,
    google_coursework_url VARCHAR(500) NULL,
    fecha_inicio DATE NULL,
    fecha_limite DATE NULL,
    FOREIGN KEY (planilla_id)
        REFERENCES planilla (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (instrumento_id)
        REFERENCES instrumento (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE registro (
    id INT AUTO_INCREMENT PRIMARY KEY,
    planilla_id INT NOT NULL,
    alumno_id INT NOT NULL,
    FOREIGN KEY (planilla_id)
        REFERENCES planilla (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (alumno_id)
        REFERENCES alumno (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE puntaje (
    registro_id INT,
    tarea_id INT,
    puntos SMALLINT UNSIGNED,
    PRIMARY KEY (registro_id , tarea_id),
    FOREIGN KEY (registro_id)
        REFERENCES registro (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (tarea_id)
        REFERENCES tarea (id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE padre (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ci` int(11) NOT NULL,
  `nombre` varchar(45) DEFAULT NULL,
  `apellido` varchar(45) DEFAULT NULL,
  `usuario` varchar(45) NOT NULL,
  `contrasenia` varchar(255) NOT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `correo` varchar(45) DEFAULT NULL,
  `totp_secret` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `usuario_UNIQUE` (`usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE push_subscription (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  user_type VARCHAR(20) NOT NULL DEFAULT 'profesor',
  endpoint TEXT NOT NULL,
  p256dh VARCHAR(255) NOT NULL,
  auth VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_push_endpoint (endpoint(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE planilla_rasgo (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE rasgo_asistencia (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
