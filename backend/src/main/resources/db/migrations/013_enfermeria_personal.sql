-- Migration: consultas de enfermería a funcionarios del colegio (usuario nivel 1, 2, 3 y 5).
-- Tablas nuevas con prefijo enf_; NO modifica enf_consulta ni ninguna otra tabla existente.
-- El funcionario no se copia: personal_id apunta a usuario.id y nombre, CI y rol se leen de usuario.
-- El historial queda atado a usuario.id aunque después le cambien el nivel.
-- ON DELETE RESTRICT (como anotacion): borrar un funcionario con consultas falla en lugar de
-- borrar en silencio su historial médico.
-- El stock es el mismo enf_insumo que usan las consultas de alumnos.

SET NAMES utf8mb4;

-- Cada visita de un funcionario a enfermería. Mismos campos que enf_consulta.
CREATE TABLE IF NOT EXISTS enf_consulta_personal (
    idConsulta INT AUTO_INCREMENT PRIMARY KEY,
    motivo VARCHAR(150) NOT NULL,
    fecha DATE NOT NULL,
    tratamiento TEXT NULL,
    horaEntrada TIME NULL,
    horaSalida TIME NULL,
    observaciones TEXT NULL,
    personal_id INT NOT NULL,
    idUsuario INT NULL,
    KEY idx_enf_consulta_personal_personal (personal_id, fecha),
    KEY idx_enf_consulta_personal_usuario (idUsuario),
    CONSTRAINT fk_enf_consulta_personal_usuario_colegio FOREIGN KEY (personal_id)
        REFERENCES usuario (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_enf_consulta_personal_enf_usuario FOREIGN KEY (idUsuario)
        REFERENCES enf_usuario (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Insumos usados en cada consulta de funcionario.
CREATE TABLE IF NOT EXISTS enf_detalle_consulta_personal (
    id_detalle_consulta INT AUTO_INCREMENT PRIMARY KEY,
    idConsulta INT NOT NULL,
    id_insumo INT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    KEY idx_enf_det_consulta_personal_consulta (idConsulta),
    KEY idx_enf_det_consulta_personal_insumo (id_insumo),
    CONSTRAINT fk_enf_det_consulta_personal_consulta FOREIGN KEY (idConsulta)
        REFERENCES enf_consulta_personal (idConsulta) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_enf_det_consulta_personal_insumo FOREIGN KEY (id_insumo)
        REFERENCES enf_insumo (id_insumo) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- end migration
