-- Migration: alumno.ci pasa de INT a VARCHAR para admitir cédulas con letra
-- (p. ej. DNI español/argentino como '51540489L'). El índice UNIQUE existente
-- sobre la columna se conserva al cambiar el tipo.
-- Run-order: V019

ALTER TABLE alumno MODIFY ci VARCHAR(20) NULL;

-- end migration
