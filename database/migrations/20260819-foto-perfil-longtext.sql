-- Add foto_perfil column to profesor if not exists
ALTER TABLE profesor ADD COLUMN IF NOT EXISTS foto_perfil LONGTEXT NULL;
