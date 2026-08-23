-- Códigos conductuales por alumno y clase. V/P/A siguen viviendo en estado.
CREATE TABLE IF NOT EXISTS rasgo_asistencia_codigo (
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

-- Conserva únicamente el historial conductual existente. V/P/A continúan
-- representados exclusivamente por rasgo_asistencia.estado.
INSERT IGNORE INTO rasgo_asistencia_codigo (rasgo_asistencia_id, codigo)
SELECT id, UPPER(TRIM(falta_codigo))
FROM rasgo_asistencia
WHERE UPPER(TRIM(falta_codigo)) IN ('N1', 'N2', 'N3', 'N4', 'N5', 'N6', 'N7', 'N8');