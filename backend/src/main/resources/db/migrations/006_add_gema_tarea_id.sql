-- Fase 2 de la integración con GEMA: idempotencia de POST/PUT de tareas por gema_tarea_id.
-- Nullable: solo las tareas creadas desde GEMA la tienen; MySQL permite múltiples NULL bajo UNIQUE.
ALTER TABLE tarea
    ADD COLUMN gema_tarea_id VARCHAR(64) NULL,
    ADD UNIQUE KEY uq_tarea_gema_tarea_id (gema_tarea_id);
