-- Migration 011: plan curricular + temas

CREATE TABLE IF NOT EXISTS plan_curricular (
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

CREATE TABLE IF NOT EXISTS tema_plan_curricular (
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
    PRIMARY KEY (id),
    UNIQUE KEY uq_tema_plan_orden (plan_curricular_id, orden_mes, bloque),
    CONSTRAINT fk_tema_plan_curricular FOREIGN KEY (plan_curricular_id)
        REFERENCES plan_curricular (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
