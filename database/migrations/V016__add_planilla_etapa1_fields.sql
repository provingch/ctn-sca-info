-- Migration: Add fecha_cierre_etapa1 and etapa1_confirmada to planilla
-- Run-order: V016

ALTER TABLE planilla
    ADD COLUMN fecha_cierre_etapa1 DATE NULL AFTER google_course_id,
    ADD COLUMN etapa1_confirmada TINYINT(1) NOT NULL DEFAULT 0 AFTER fecha_cierre_etapa1;

-- end migration
