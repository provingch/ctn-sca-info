# Codificación de textos

El código fuente, la lectura de migraciones y JDBC usan UTF-8. Los archivos SQL de carga
no fijaban la codificación del cliente y el esquema creado manualmente heredaba la
codificación del servidor. Esto permitía importar bytes UTF-8 como Latin-1 y guardar
textos como `ElectrÃ³nica`. El nombre dañado tampoco coincidía con el catálogo de íconos.

Las cargas ahora declaran `SET NAMES utf8mb4`, el cliente de despliegue usa
`--default-character-set=utf8mb4` y la base nueva declara `utf8mb4` explícitamente.
No ejecutar de nuevo los seeds para reparar una instalación existente: borran datos.

## Instalaciones existentes

Al iniciar el backend actualizado, después de las migraciones SQL, se ejecuta una vez
`V017__repair_display_text_utf8`. Corrige únicamente secuencias conocidas de tildes,
ñ, diéresis y puntuación en la lista explícita de campos de `TextEncodingMigrationDao`.
No convierte cadenas completas ni modifica contraseñas, usuarios de acceso, correos,
tokens, archivos o JSON. El texto correcto o irreparable (`�`) queda intacto.

La reparación guarda cada valor original y corregido como bytes UTF-8 en
`text_encoding_repair_backup`. Los cambios y la marca de migración se confirman en una
misma transacción. Ante un error se revierten y el arranque falla, como las otras
migraciones. La tabla de respaldo se crea antes de la transacción porque el DDL de
MySQL confirma implícitamente. El registro de aplicación informa cantidades, no textos.

Para verificar en la base de destino (consultas de solo lectura):

```sql
SELECT @@character_set_client, @@character_set_connection, @@character_set_results;
SELECT table_name, column_name, character_set_name, collation_name
FROM information_schema.columns
WHERE table_schema = DATABASE() AND character_set_name IS NOT NULL;
SELECT version, applied_at FROM schema_migrations
WHERE version = 'V017__repair_display_text_utf8';
SELECT table_name, column_name, COUNT(*) AS campos_reparados
FROM text_encoding_repair_backup GROUP BY table_name, column_name;
```

La corrección no depende de volver a importar datos ni de renombrar especialidades a
mano. Revisar por separado las cadenas que contengan `�`: esos bytes ya se perdieron
y se necesita la fuente original para reconstruirlas. El respaldo permite recuperar
un valor concreto si hiciera falta; no restaurar masivamente encima de ediciones nuevas.
