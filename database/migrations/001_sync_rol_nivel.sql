-- Sincronización segura de nivel/rol. Es tolerante a bases donde rol ya fue
-- eliminado por una ejecución manual previa de la migración 002.

SET @sync_nivel_from_rol = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuario' AND column_name = 'rol')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuario' AND column_name = 'nivel'),
    'UPDATE usuario SET nivel = 4 WHERE rol = ''padre'' AND nivel <> 4',
    'SELECT 1'
  )
);
PREPARE sync_nivel_from_rol_statement FROM @sync_nivel_from_rol;
EXECUTE sync_nivel_from_rol_statement;
DEALLOCATE PREPARE sync_nivel_from_rol_statement;

SET @sync_rol_from_nivel = (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuario' AND column_name = 'rol')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuario' AND column_name = 'nivel'),
    'UPDATE usuario SET rol = ''padre'' WHERE nivel = 4 AND (rol IS NULL OR rol <> ''padre'')',
    'SELECT 1'
  )
);
PREPARE sync_rol_from_nivel_statement FROM @sync_rol_from_nivel;
EXECUTE sync_rol_from_nivel_statement;
DEALLOCATE PREPARE sync_rol_from_nivel_statement;
