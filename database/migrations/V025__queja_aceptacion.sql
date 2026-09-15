-- Coordinación acepta o rechaza antes de revisar. Rechazada es un estado terminal.
ALTER TABLE queja
    ADD COLUMN aceptada_en DATETIME NULL,
    ADD COLUMN aceptada_por INT NULL,
    ADD COLUMN rechazada_en DATETIME NULL,
    ADD COLUMN rechazada_por INT NULL,
    ADD COLUMN motivo_rechazo TEXT NULL,
    ADD CONSTRAINT fk_queja_aceptada_por FOREIGN KEY (aceptada_por) REFERENCES usuario(id),
    ADD CONSTRAINT fk_queja_rechazada_por FOREIGN KEY (rechazada_por) REFERENCES usuario(id);

-- Backfill: toda queja ya revisada bajo el flujo viejo fue de hecho aceptada.
UPDATE queja SET aceptada_en = revisada_en, aceptada_por = revisada_por
WHERE revisada_en IS NOT NULL AND aceptada_en IS NULL;
