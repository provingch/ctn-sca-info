-- Migration 014: classroom sync audit log

CREATE TABLE IF NOT EXISTS classroom_sync_log (
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
