-- Migration: Add fecha_cierre_etapa2 and etapa2_confirmada to planilla
-- (espeja fecha_cierre_etapa1 / etapa1_confirmada de V016 para la segunda etapa).
-- Run-order: V021

ALTER TABLE planilla
    ADD COLUMN fecha_cierre_etapa2 DATE NULL AFTER etapa1_confirmada,
    ADD COLUMN etapa2_confirmada TINYINT(1) NOT NULL DEFAULT 0 AFTER fecha_cierre_etapa2;

-- end migration
