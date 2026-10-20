-- Migration: sistema de enfermería (SHME) integrado a la base del colegio.
-- Agrega tablas nuevas con prefijo enf_ y NO modifica ninguna tabla existente del colegio.
-- Origen: V035__enfermeria.sql de la app de enfermería (shmeapi/), con un cambio:
-- las claves hacia alumno quedan ON DELETE RESTRICT (como anotacion y registro), para que
-- borrar un alumno desde el panel no borre en silencio su historial médico.
-- Depurar egresados es un proceso aparte, con respaldo previo.
-- Los usuarios de enfermería (enf_usuario) son independientes de usuario del colegio.

SET NAMES utf8mb4;

-- Ficha médica del alumno (1 a 1 con alumno del colegio).
-- Nombre, apellido, curso y sección se obtienen de alumno/curso del colegio.
CREATE TABLE IF NOT EXISTS enf_ficha (
    alumno_id INT NOT NULL PRIMARY KEY,
    enfermedadBase VARCHAR(255) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enf_ficha_alumno FOREIGN KEY (alumno_id)
        REFERENCES alumno (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Responsable (tutor) del alumno.
CREATE TABLE IF NOT EXISTS enf_tutor (
    id_tutor INT AUTO_INCREMENT PRIMARY KEY,
    alumno_id INT NOT NULL,
    nombre VARCHAR(100) NULL,
    apellido VARCHAR(100) NULL,
    correo VARCHAR(150) NULL,
    telefono VARCHAR(30) NULL,
    KEY idx_enf_tutor_alumno (alumno_id),
    CONSTRAINT fk_enf_tutor_alumno FOREIGN KEY (alumno_id)
        REFERENCES alumno (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Catálogo de alergias y alergias de cada alumno.
CREATE TABLE IF NOT EXISTS enf_alergia (
    id_alergia INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    UNIQUE KEY uq_enf_alergia_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS enf_detalle_alergia (
    id_detalle_alergia INT AUTO_INCREMENT PRIMARY KEY,
    id_alergia INT NOT NULL,
    alumno_id INT NOT NULL,
    UNIQUE KEY uq_enf_det_alergia (alumno_id, id_alergia),
    KEY idx_enf_det_alergia_alergia (id_alergia),
    CONSTRAINT fk_enf_det_alergia_alergia FOREIGN KEY (id_alergia)
        REFERENCES enf_alergia (id_alergia) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_enf_det_alergia_alumno FOREIGN KEY (alumno_id)
        REFERENCES alumno (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Usuarios del sistema de enfermería (independientes de los profesores del colegio).
-- contrasena guarda el hash, nunca texto plano.
CREATE TABLE IF NOT EXISTS enf_usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    usuario VARCHAR(50) NOT NULL,
    contrasena VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    UNIQUE KEY uq_enf_usuario_usuario (usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Inventario de insumos.
CREATE TABLE IF NOT EXISTS enf_insumo (
    id_insumo INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    stock_actual INT NOT NULL DEFAULT 0,
    stock_minimo INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_enf_insumo_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Cada visita a enfermería.
CREATE TABLE IF NOT EXISTS enf_consulta (
    idConsulta INT AUTO_INCREMENT PRIMARY KEY,
    motivo VARCHAR(150) NOT NULL,
    fecha DATE NOT NULL,
    tratamiento TEXT NULL,
    horaEntrada TIME NULL,
    horaSalida TIME NULL,
    observaciones TEXT NULL,
    alumno_id INT NOT NULL,
    idUsuario INT NULL,
    KEY idx_enf_consulta_alumno (alumno_id, fecha),
    KEY idx_enf_consulta_usuario (idUsuario),
    CONSTRAINT fk_enf_consulta_alumno FOREIGN KEY (alumno_id)
        REFERENCES alumno (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_enf_consulta_usuario FOREIGN KEY (idUsuario)
        REFERENCES enf_usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Insumos usados en cada consulta.
CREATE TABLE IF NOT EXISTS enf_detalle_consulta (
    id_detalle_consulta INT AUTO_INCREMENT PRIMARY KEY,
    idConsulta INT NOT NULL,
    id_insumo INT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    KEY idx_enf_det_consulta_consulta (idConsulta),
    KEY idx_enf_det_consulta_insumo (id_insumo),
    CONSTRAINT fk_enf_det_consulta_consulta FOREIGN KEY (idConsulta)
        REFERENCES enf_consulta (idConsulta) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_enf_det_consulta_insumo FOREIGN KEY (id_insumo)
        REFERENCES enf_insumo (id_insumo) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Pedidos de insumos.
CREATE TABLE IF NOT EXISTS enf_solicitud_insumo (
    id_solicitud INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    id_usuario INT NULL,
    KEY idx_enf_solicitud_usuario (id_usuario),
    CONSTRAINT fk_enf_solicitud_usuario FOREIGN KEY (id_usuario)
        REFERENCES enf_usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS enf_detalle_solicitud_insumo (
    id_detalle_solicitud INT AUTO_INCREMENT PRIMARY KEY,
    id_solicitud INT NOT NULL,
    id_insumo INT NOT NULL,
    cantidad INT NOT NULL,
    observacion VARCHAR(255) NULL,
    KEY idx_enf_det_solicitud_solicitud (id_solicitud),
    KEY idx_enf_det_solicitud_insumo (id_insumo),
    CONSTRAINT fk_enf_det_solicitud_solicitud FOREIGN KEY (id_solicitud)
        REFERENCES enf_solicitud_insumo (id_solicitud) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_enf_det_solicitud_insumo FOREIGN KEY (id_insumo)
        REFERENCES enf_insumo (id_insumo) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Registro de reportes generados.
CREATE TABLE IF NOT EXISTS enf_reporte (
    id_reporte INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    id_usuario INT NULL,
    KEY idx_enf_reporte_usuario (id_usuario),
    CONSTRAINT fk_enf_reporte_usuario FOREIGN KEY (id_usuario)
        REFERENCES enf_usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Catálogo inicial de alergias (genérico, sin datos de personas).
INSERT IGNORE INTO enf_alergia (nombre) VALUES
    ('Penicilina'), ('Ibuprofeno'), ('Maní'), ('Polen'), ('Lactosa');

-- end migration
