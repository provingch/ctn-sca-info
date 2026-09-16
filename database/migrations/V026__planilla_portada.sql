-- Portada decorativa por planilla, cargada por el profesor dueño.
ALTER TABLE planilla
    ADD COLUMN portada LONGTEXT NULL,
    ADD COLUMN portada_actualizada_en DATETIME NULL;
