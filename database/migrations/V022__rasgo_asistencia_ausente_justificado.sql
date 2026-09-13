-- Agrega el estado `ausente_justificado` al ENUM de rasgo_asistencia.estado
-- para permitir que el profesor marque una ausencia previa como justificada
-- después de que el alumno presente el justificativo.
ALTER TABLE rasgo_asistencia
    MODIFY estado ENUM('pendiente', 'presente', 'ausente', 'ausente_justificado')
        NOT NULL DEFAULT 'pendiente';
