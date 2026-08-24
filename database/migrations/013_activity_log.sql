-- Migration 013: activity log user path support

ALTER TABLE usuario
  ADD COLUMN IF NOT EXISTS activity_log_path VARCHAR(255) NULL AFTER nivel;
