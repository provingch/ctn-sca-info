#!/usr/bin/env bash
# Levanta el SCA completo en local para grabar tutoriales y sacar capturas sin tocar prod:
#   MariaDB descartable (puerto 3307) + backend (8080) + build de producción del frontend servida con vite preview (5173).
# Cada corrida REINICIA la base: schema + seed oficial + local-setup.py + demo-insert.sql (las grabaciones guardan cambios).
# Contraseña de todas las cuentas de demo: demo1234. Uso: manuales/datos-demo/levantar-local.sh [--solo-base]
# Requiere ~/Proyectos/database (schema y seed, fuera del repo) y el jar compilado (cd backend && ./mvnw -DskipTests package).
set -euo pipefail
RAIZ=$(cd "$(dirname "$0")/../.." && pwd)
DB_DIR=${DB_DIR:-$HOME/Proyectos/database}
L=/tmp/sca-local            # ruta corta: el socket de MariaDB no admite rutas largas
SOCK=$L/s.sock
m() { mariadb -uroot -S "$SOCK" "$@"; }
mkdir -p "$L"

if ! m -e 'select 1' >/dev/null 2>&1; then
  [ -d "$L/data/mysql" ] || mariadb-install-db --no-defaults --auth-root-authentication-method=normal --datadir="$L/data" > "$L/install.log" 2>&1
  nohup mariadbd --no-defaults $([ "$(id -u)" = 0 ] && echo --user=root) --datadir="$L/data" --socket="$SOCK" --port=3307 --bind-address=127.0.0.1 \
    --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci > "$L/mariadbd.log" 2>&1 &
  for _ in $(seq 30); do m -e 'select 1' >/dev/null 2>&1 && break; sleep 1; done
fi

echo "==> Base: schema + seed + preparación local + datos de ejemplo"
m < "$DB_DIR/db-tables-properties.sql" > "$L/schema.log" 2>&1
m ctndb < "$DB_DIR/ctn-official-seed.sql" > "$L/seed.log" 2>&1
python3 -I "$RAIZ/manuales/datos-demo/local-setup.py" | m ctndb
m --default-character-set=utf8mb4 ctndb < "$RAIZ/manuales/datos-demo/demo-insert.sql" > /dev/null
# Thiago (291) no entregó la prueba de julio: para mostrar el caso "No entregada" en la vista de padres
m ctndb -e "DELETE pu FROM puntaje pu JOIN registro r ON r.id = pu.registro_id JOIN tarea t ON t.id = pu.tarea_id WHERE r.alumno_id = 291 AND t.titulo = 'Prueba sumativa: variables y tipos de datos'"
# si el schema ya trae todas las migraciones del jar (llega hasta alumno_curso_historial), marcarlas como aplicadas
# para que el backend no las repita; si es un schema más viejo, que el backend aplique las que falten al arrancar
JAR=$(ls "$RAIZ"/backend/target/sca-*.jar | head -1)
[ -n "$(m ctndb -N -e "SHOW TABLES LIKE 'alumno_curso_historial'")" ] && { echo "INSERT IGNORE INTO schema_migrations (version) VALUES"
  unzip -l "$JAR" | grep -o 'db/migrations/[^ ]*\.sql' | sed 's|db/migrations/||' | sort -u | sed "s/.*/('&')/" | paste -sd,
  echo ";"; } | m ctndb
# las V0xx (formato viejo) ya vienen en el schema aunque no queden registradas: marcarlas siempre
{ echo "INSERT IGNORE INTO schema_migrations (version) VALUES"
  unzip -l "$JAR" | grep -o 'db/migrations/V[^ ]*\.sql' | sed 's|db/migrations/||' | sort -u | sed "s/.*/('&')/" | paste -sd,
  echo ";"; } | m ctndb
m -e "CREATE USER IF NOT EXISTS 'sca_local'@'127.0.0.1' IDENTIFIED BY 'local'; GRANT ALL ON ctndb.* TO 'sca_local'@'127.0.0.1';"
[ "${1:-}" = "--solo-base" ] && { echo "Base lista."; exit 0; }

# el backend crea algunas tablas al arrancar (refresh_token, …): tras reiniciar la base hay que reiniciarlo
PID=$(pgrep -f '^java -jar .*backend/target/sca-' || true)  # anclado: así no coincide con el shell que corre esto
[ -n "$PID" ] && { echo "==> Reiniciando backend"; kill $PID; while kill -0 $PID 2>/dev/null; do sleep 1; done; }
if ! curl -s -o /dev/null http://localhost:8080/; then
  echo "==> Backend en :8080"
  CTN_DB_HOST=127.0.0.1:3307 CTN_DB_NAME=ctndb CTN_DB_USER=sca_local CTN_DB_PASSWORD=local \
  SCA_DB_HOST=127.0.0.1 SCA_DB_PORT=3307 SCA_DB_NAME=ctndb SCA_DB_USER=sca_local SCA_DB_PASSWORD=local SCA_DB_TYPE=mariadb \
  SERVER_PORT=8080 nohup java -jar "$JAR" > "$L/backend.log" 2>&1 &
  for _ in $(seq 60); do grep -q 'Started ' "$L/backend.log" 2>/dev/null && break; sleep 1; done
fi

# anotaciones de ejemplo (sus tablas las crean las migraciones, así que va con el backend ya arrancado)
python3 -I "$RAIZ/manuales/datos-demo/demo-anotaciones.py" | m ctndb

if ! curl -s -o /dev/null http://localhost:5173/; then
  echo "==> Frontend (build de producción) en :5173"
  # build de producción: en modo dev, StrictMode duplica el refresh de sesión y la rotación de tokens cierra la sesión
  (cd "$RAIZ/frontend" && npx vite build --outDir "$L/dist" --emptyOutDir > "$L/vbuild.log" 2>&1)
  (cd "$RAIZ/frontend" && nohup npx vite preview --outDir "$L/dist" --port 5173 --strictPort > "$L/preview.log" 2>&1 &)
  for _ in $(seq 30); do curl -s -o /dev/null http://localhost:5173/ && break; sleep 1; done
fi
echo "Listo: http://localhost:5173 (usuarios de demo con contraseña demo1234)"
