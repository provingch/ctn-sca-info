-- Migration: Registra horario, modalidad, observaciones e instrumento del
-- registro de clase. El formulario de "Iniciar clase" ya recolectaba estos
-- datos pero create-rasgo-planilla nunca los mandaba y planilla_rasgo no
-- tenía dónde persistirlos: se escribían en pantalla y se perdían.
-- Sin backfill: las clases ya registradas no tienen este dato y quedan NULL.
-- Run-order: V027

ALTER TABLE planilla_rasgo
    ADD COLUMN hora_inicio TIME NULL,
    ADD COLUMN horas_catedra TINYINT UNSIGNED NULL,
    ADD COLUMN hora_fin TIME NULL,
    ADD COLUMN modalidad VARCHAR(20) NULL,
    ADD COLUMN observaciones TEXT NULL,
    ADD COLUMN instrumento_id INT NULL,
    ADD CONSTRAINT fk_planilla_rasgo_instrumento FOREIGN KEY (instrumento_id)
        REFERENCES instrumento (id) ON UPDATE CASCADE ON DELETE SET NULL;

-- end migration
