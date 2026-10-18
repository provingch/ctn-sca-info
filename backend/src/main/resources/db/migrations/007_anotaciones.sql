-- Migration: anotaciones registradas por profesores desde la app Android
-- (conducta, uniforme, etc.). La foto de evidencia es opcional y depende del tipo.
-- Agrega 3 tablas nuevas y NO modifica ninguna tabla existente del colegio.
-- Origen: V033__anotaciones.sql de la app de anotaciones (ceciapi/).

SET NAMES utf8mb4;

-- Catálogo de tipos de anotación: es la lista que va a definir la coordinadora.
-- Para agregar o quitar tipos solo se hace INSERT/UPDATE acá, sin cambiar la app ni la API.
-- requiere_foto = TRUE -> la app obliga a sacar foto; FALSE -> la foto no se pide.
-- codigo_conducta enlaza (opcional) con el catálogo existente codigo_conducta (N1..N8).
CREATE TABLE IF NOT EXISTS tipo_anotacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descripcion VARCHAR(255) NOT NULL UNIQUE,
    codigo_conducta VARCHAR(10) NULL,
    requiere_foto BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tipo_anotacion_codigo FOREIGN KEY (codigo_conducta)
        REFERENCES codigo_conducta (codigo)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Carga inicial (provisoria, hasta tener la lista oficial de la coordinadora):
-- 1) los códigos de conducta que ya existen, sin foto (salvo el de uniforme, que se detalla abajo)
INSERT IGNORE INTO tipo_anotacion (descripcion, codigo_conducta, requiere_foto)
SELECT descripcion, codigo, FALSE
FROM codigo_conducta
WHERE activo = TRUE AND codigo <> 'N7';

-- 2) los tres motivos de uniforme, con foto obligatoria
-- (si el código N7 no existiera en codigo_conducta, el tipo se crea igual, sin enlace)
INSERT IGNORE INTO tipo_anotacion (descripcion, codigo_conducta, requiere_foto)
SELECT d.descripcion, (SELECT codigo FROM codigo_conducta WHERE codigo = 'N7'), TRUE
FROM (
    SELECT 'No trae corbata' AS descripcion
    UNION ALL SELECT 'No utiliza el zapato correspondiente'
    UNION ALL SELECT 'No utiliza el uniforme correspondiente al día de hoy'
) AS d;

-- Cada anotación registrada por un profesor.
CREATE TABLE IF NOT EXISTS anotacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    alumno_id INT NOT NULL,
    usuario_id INT NOT NULL COMMENT 'profesor (usuario.nivel = 1) que registra la anotación',
    tipo_id INT NOT NULL,
    materia_id INT NULL COMMENT 'materia elegida en la app (opcional)',
    fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacion VARCHAR(500) NULL,
    KEY idx_anotacion_alumno (alumno_id, fecha_hora),
    KEY idx_anotacion_usuario (usuario_id),
    KEY idx_anotacion_tipo (tipo_id),
    CONSTRAINT fk_anotacion_alumno FOREIGN KEY (alumno_id)
        REFERENCES alumno (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_anotacion_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_anotacion_tipo FOREIGN KEY (tipo_id)
        REFERENCES tipo_anotacion (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_anotacion_materia FOREIGN KEY (materia_id)
        REFERENCES materia (id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Foto de evidencia (solo existe para las anotaciones que la llevan).
-- En tabla aparte para que el historial no cargue las imágenes hasta que se pidan.
CREATE TABLE IF NOT EXISTS anotacion_foto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    anotacion_id INT NOT NULL,
    imagen MEDIUMBLOB NOT NULL COMMENT 'hasta 16 MB, la app la reduce a ~300-500 KB',
    tipo_mime VARCHAR(50) NOT NULL DEFAULT 'image/jpeg',
    tamano_bytes INT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_anotacion_foto_anotacion (anotacion_id),
    CONSTRAINT fk_anotacion_foto_anotacion FOREIGN KEY (anotacion_id)
        REFERENCES anotacion (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- end migration
