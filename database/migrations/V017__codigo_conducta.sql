-- Catálogo administrable de rasgos conductuales.
CREATE TABLE IF NOT EXISTS codigo_conducta (
    id INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO codigo_conducta (codigo, descripcion, activo) VALUES
    ('N1', 'Sale del aula sin autorización', TRUE),
    ('N2', 'No realiza la tarea asignada en clase', TRUE),
    ('N3', 'No dispone de los materiales necesarios', TRUE),
    ('N4', 'No presenta las tareas asignadas para la casa', TRUE),
    ('N5', 'Utiliza vocabulario indebido en clase', TRUE),
    ('N6', 'Charla mucho en clase', TRUE),
    ('N7', 'No utiliza el uniforme establecido', TRUE),
    ('N8', 'Ausente en clase, presente en la Institución', TRUE);

-- MariaDB no soporta `DROP CHECK`; usa `DROP CONSTRAINT`. Se separan los
-- ALTER y se hace drop-then-add del FK para que la migración sea reintentable.
ALTER TABLE rasgo_asistencia_codigo MODIFY codigo VARCHAR(10) NOT NULL;

ALTER TABLE rasgo_asistencia_codigo DROP CONSTRAINT IF EXISTS chk_rasgo_asistencia_codigo;

ALTER TABLE rasgo_asistencia_codigo DROP FOREIGN KEY IF EXISTS fk_rasgo_asistencia_codigo_conducta;
ALTER TABLE rasgo_asistencia_codigo
    ADD CONSTRAINT fk_rasgo_asistencia_codigo_conducta
        FOREIGN KEY (codigo) REFERENCES codigo_conducta (codigo)
        ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE rasgo_asistencia
    MODIFY falta_codigo VARCHAR(10) NULL;
