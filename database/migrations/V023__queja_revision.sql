-- Las quejas existentes y nuevas quedan pendientes hasta registrar una revisión.
ALTER TABLE queja
    ADD COLUMN revisada_en DATETIME NULL,
    ADD COLUMN revisada_por INT NULL,
    ADD COLUMN conclusion TEXT NULL,
    ADD CONSTRAINT fk_queja_revisada_por FOREIGN KEY (revisada_por) REFERENCES usuario(id);
