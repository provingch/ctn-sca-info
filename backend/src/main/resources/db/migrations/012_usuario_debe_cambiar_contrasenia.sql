-- Migration: contraseña temporal. Cuando un administrador le restablece la contraseña a un usuario,
-- debe_cambiar_contrasenia queda en 1: hasta que el usuario elija una nueva, el sistema solo le deja
-- cambiarla (JwtAuthenticationFilter) y la web le muestra la pantalla de cambio.
--
-- Después de aplicarla, agregar también la columna a db-tables-properties.sql.

SET NAMES utf8mb4;

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS debe_cambiar_contrasenia TINYINT(1) NOT NULL DEFAULT 0;
