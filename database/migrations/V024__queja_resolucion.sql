-- Una revisión previa no implica que la queja esté resuelta.
ALTER TABLE queja
    ADD COLUMN proceso_revision TEXT NULL,
    ADD COLUMN solucion_aplicada TEXT NULL,
    ADD COLUMN corregida_por_nombre VARCHAR(200) NULL,
    ADD COLUMN resuelta_en DATETIME NULL,
    ADD COLUMN resolucion_registrada_por INT NULL,
    ADD CONSTRAINT fk_queja_resolucion_usuario FOREIGN KEY (resolucion_registrada_por) REFERENCES usuario(id);
