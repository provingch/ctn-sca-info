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

CREATE TABLE usuario (
    id INT AUTO_INCREMENT,
    nombre VARCHAR(45) DEFAULT NULL,
    apellido VARCHAR(45) DEFAULT NULL,
    usuario VARCHAR(45) NOT NULL UNIQUE,
    contrasenia VARCHAR(255) DEFAULT 'password' NOT NULL,
    ci INT DEFAULT NULL,
    telefono VARCHAR(45) DEFAULT NULL,
    celular VARCHAR(45) DEFAULT NULL,
    correo VARCHAR(255) DEFAULT NULL,
    google_email VARCHAR(255) DEFAULT NULL,
    google_access_token TEXT NULL,
    google_refresh_token TEXT NULL,
    google_token_expiry BIGINT NULL,
    materias_manual TEXT NULL,
    totp_secret VARCHAR(255) NULL,
    firma_imagen LONGTEXT NULL,
    foto_perfil LONGTEXT NULL,
    -- `nivel` es la única fuente de verdad para el rol del usuario
    nivel TINYINT NOT NULL DEFAULT 0,
    activity_log_path VARCHAR(255) NULL,
    especialidad_id INT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE materia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(45) NOT NULL,
    categoria ENUM('comun', 'especifico') NOT NULL,
    UNIQUE KEY uq_materia_nombre (nombre)
);

CREATE TABLE usuario_materia (
    usuario_id INT NOT NULL,
    materia_id INT NOT NULL,
    PRIMARY KEY (usuario_id, materia_id),
    CONSTRAINT fk_um_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_um_materia FOREIGN KEY (materia_id)
        REFERENCES materia (id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE asignacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    materia_id INT NOT NULL,
    curso_id INT NOT NULL,
    UNIQUE KEY uq_asignacion (usuario_id, materia_id, curso_id),
    CONSTRAINT fk_asig_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_asig_materia FOREIGN KEY (materia_id)
        REFERENCES materia (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_asig_curso FOREIGN KEY (curso_id)
        REFERENCES curso (id) ON UPDATE CASCADE ON DELETE CASCADE
);

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
    usuario_id INT NOT NULL,
    google_course_id VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE (curso_id, materia_id, periodo, etapa),
    FOREIGN KEY (curso_id)
        REFERENCES curso (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (materia_id)
        REFERENCES materia (id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
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

-- Nota: la tabla `padre` se ha consolidado dentro de `usuario`.
-- Si necesita mantener datos históricos, migre los registros de `padre` a `usuario` antes de aplicar el DDL en producción.

CREATE TABLE push_subscription (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    -- indicar el tipo/rol del usuario en el momento de la suscripción (ej: 'profesor','padre','admin')
    user_type VARCHAR(20) NOT NULL DEFAULT 'usuario',
    endpoint TEXT NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_push_endpoint (endpoint(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE planilla_rasgo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    curso_id INT NOT NULL,
    usuario_id INT NOT NULL,
    tema VARCHAR(150) NOT NULL,
    fecha_clase DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_planilla_rasgo_curso (curso_id),
    KEY idx_planilla_rasgo_usuario (usuario_id),
    CONSTRAINT fk_planilla_rasgo_curso FOREIGN KEY (curso_id)
        REFERENCES curso (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_planilla_rasgo_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE plan_curricular (
    id INT AUTO_INCREMENT,
    asignacion_id INT NOT NULL,
    etapa VARCHAR(10) NOT NULL,
    anio_lectivo SMALLINT UNSIGNED NOT NULL,
    archivo_nombre VARCHAR(255) NOT NULL,
    archivo_contenido LONGBLOB NOT NULL,
    estado ENUM('PENDIENTE','APROBADO','RECHAZADO') NOT NULL DEFAULT 'PENDIENTE',
    fecha_subida TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_revision TIMESTAMP NULL,
    evaluador_id INT NULL,
    observaciones_evaluador TEXT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_plan_curricular_asignacion_etapa_anio (asignacion_id, etapa, anio_lectivo),
    CONSTRAINT fk_plan_curricular_asignacion FOREIGN KEY (asignacion_id)
        REFERENCES asignacion (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_plan_curricular_evaluador FOREIGN KEY (evaluador_id)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Add columns to planilla_rasgo for plan curricular linkage and estado_verificacion_tema
ALTER TABLE planilla_rasgo
    ADD COLUMN asignacion_id INT NULL,
    ADD COLUMN estado_verificacion_tema ENUM('OK','DUDOSO','NO_COINCIDE','SIN_PLAN') NOT NULL DEFAULT 'SIN_PLAN',
    ADD COLUMN tema_plan_curricular_id INT NULL;

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

CREATE TABLE rasgo_asistencia_codigo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rasgo_asistencia_id INT NOT NULL,
    codigo VARCHAR(2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_rasgo_asistencia_codigo (rasgo_asistencia_id, codigo),
    KEY idx_rasgo_asistencia_codigo (rasgo_asistencia_id),
    CONSTRAINT fk_rasgo_asistencia_codigo_asistencia
        FOREIGN KEY (rasgo_asistencia_id) REFERENCES rasgo_asistencia (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_rasgo_asistencia_codigo
        CHECK (codigo IN ('N1', 'N2', 'N3', 'N4', 'N5', 'N6', 'N7', 'N8'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Tabla de relación entre alumno y usuario (antes: alumno_padre)
CREATE TABLE alumno_usuario (
    alumno_id INT NOT NULL,
    usuario_id INT NOT NULL,
    parentesco VARCHAR(45) DEFAULT 'padre',
    PRIMARY KEY (alumno_id, usuario_id),
    CONSTRAINT fk_au_alumno FOREIGN KEY (alumno_id)
        REFERENCES alumno (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_au_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- tema_plan_curricular depends on plan_curricular; we'll create it after plan_curricular

CREATE TABLE tema_plan_curricular (
    id INT AUTO_INCREMENT,
    plan_curricular_id INT NOT NULL,
    mes VARCHAR(20) NOT NULL,
    orden_mes TINYINT UNSIGNED NOT NULL,
    bloque TINYINT UNSIGNED NOT NULL,
    capacidades TEXT NULL,
    temas_contenidos TEXT NOT NULL,
    actividades TEXT NULL,
    instrumentos_evaluacion TEXT NULL,
    indicador_conceptual TEXT NULL,
    indicador_procedimental TEXT NULL,
    indicador_actitudinal TEXT NULL,
    estado_cobertura ENUM('PENDIENTE','CUBIERTO') NOT NULL DEFAULT 'PENDIENTE',
    fecha_cobertura TIMESTAMP NULL,
    planilla_rasgo_id INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_tema_plan_orden (plan_curricular_id, orden_mes, bloque),
    CONSTRAINT fk_tema_plan_curricular FOREIGN KEY (plan_curricular_id)
        REFERENCES plan_curricular (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_tema_plan_planilla_rasgo FOREIGN KEY (planilla_rasgo_id)
        REFERENCES planilla_rasgo (id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Now add foreign key from planilla_rasgo to tema_plan_curricular (added after creation to avoid FK ordering issues)
ALTER TABLE planilla_rasgo
    ADD CONSTRAINT fk_planilla_rasgo_tema FOREIGN KEY (tema_plan_curricular_id)
        REFERENCES tema_plan_curricular (id) ON UPDATE CASCADE ON DELETE SET NULL;

-- classroom_sync_log
CREATE TABLE classroom_sync_log (
    id BIGINT AUTO_INCREMENT,
    planilla_id INT NOT NULL,
    usuario_id INT NOT NULL,
    tareas_creadas INT NOT NULL DEFAULT 0,
    calificaciones_actualizadas INT NOT NULL DEFAULT 0,
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_classroom_sync_log_planilla_id (planilla_id),
    KEY idx_classroom_sync_log_usuario_id (usuario_id),
    KEY idx_classroom_sync_log_synced_at (synced_at),
    CONSTRAINT fk_classroom_sync_log_planilla FOREIGN KEY (planilla_id)
        REFERENCES planilla (id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_classroom_sync_log_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
