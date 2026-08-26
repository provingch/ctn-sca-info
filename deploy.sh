#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="${REPO_DIR:-$SCRIPT_DIR}"
PROJECT_DIR="${PROJECT_DIR:-}"
FRONTEND_DIR="${FRONTEND_DIR:-$REPO_DIR/frontend}"

if [[ -z "$PROJECT_DIR" ]]; then
  if [[ -f "$REPO_DIR/backend/pom.xml" ]]; then
    PROJECT_DIR="$REPO_DIR/backend"
  elif [[ -f "$REPO_DIR/pom.xml" ]]; then
    PROJECT_DIR="$REPO_DIR"
  else
    PROJECT_DIR="$REPO_DIR/backend"
  fi
fi

REPO_URL="${REPO_URL:-https://github.com/provingch/ctn-sca-info.git}"
SERVICE_NAME="${SERVICE_NAME:-sca-backend}"
APP_USER="${APP_USER:-deploy}"
APP_GROUP="${APP_GROUP:-deploy}"
INSTALL_DIR="${INSTALL_DIR:-/opt/ctn-sca-info/backend}"
JAR_NAME="${JAR_NAME:-sca-backend.jar}"
SERVICE_UNIT_PATH="${SERVICE_UNIT_PATH:-/etc/systemd/system/${SERVICE_NAME}.service}"

APP_PORT="${APP_PORT:-8080}"
APP_URL="${APP_URL:-http://127.0.0.1:${APP_PORT}/api/health}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-90}"
HEALTHCHECK_RETRIES="${HEALTHCHECK_RETRIES:-10}"
HEALTHCHECK_DELAY_SECONDS="${HEALTHCHECK_DELAY_SECONDS:-2}"

DB_ENV_FILE="${DB_ENV_FILE:-/etc/ctn-sca-info-backend.env}"
DB_TYPE="${SCA_DB_TYPE:-mariadb}"
DB_NAME="${SCA_DB_NAME:-${CTN_DB_NAME:-ctndb}}"
DB_HOST="${SCA_DB_HOST:-${CTN_DB_HOST:-localhost}}"
DB_PORT="${SCA_DB_PORT:-${CTN_DB_PORT:-}}"
DB_USER="${SCA_DB_USER:-${CTN_DB_USER:-testadmin}}"
DB_PASSWORD_INPUT="${SCA_DB_PASSWORD:-${CTN_DB_PASSWORD:-}}"
LOAD_DEMO_DATA_INPUT="${SCA_LOAD_DEMO_DATA:-}"

GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-}"
GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-}"
GOOGLE_REDIRECT_URI="${GOOGLE_REDIRECT_URI:-}"
VAPID_PUBLIC_KEY="${CTN_VAPID_PUBLIC_KEY:-}"
VAPID_PRIVATE_KEY="${CTN_VAPID_PRIVATE_KEY:-}"

SYSTEMD_DROPIN_DIR="/etc/systemd/system/${SERVICE_NAME}.service.d"
SYSTEMD_DROPIN_FILE="${SYSTEMD_DROPIN_DIR}/ctn-sca-info.conf"

LOG_DIR="${LOG_DIR:-/var/log/ctn-sca-info}"
DEPLOY_LOG="${DEPLOY_LOG:-${LOG_DIR}/deploy.log}"
mkdir -p "$LOG_DIR" 2>/dev/null || true

C_RESET='\033[0m'
C_BOLD='\033[1m'
C_DIM='\033[2m'
C_RED='\033[38;5;196m'
C_GREEN='\033[38;5;77m'
C_YELLOW='\033[38;5;220m'
C_BLUE='\033[38;5;33m'
C_CYAN='\033[38;5;51m'
C_GRAY='\033[38;5;244m'

log()     { printf "%b\n" "  ${C_GRAY}[$(date '+%H:%M:%S')]${C_RESET} $*" | tee -a "$DEPLOY_LOG" 2>/dev/null || printf "%b\n" "  $*"; }
log_ok()  { printf "%b\n" "  ${C_GREEN}✔${C_RESET} $*" | tee -a "$DEPLOY_LOG" 2>/dev/null || printf "%b\n" "  ✔ $*"; }
log_err() { printf "%b\n" "  ${C_RED}✘${C_RESET} $*" | tee -a "$DEPLOY_LOG" 2>/dev/null || printf "%b\n" "  ✘ $*"; }
log_warn(){ printf "%b\n" "  ${C_YELLOW}▲${C_RESET} $*" | tee -a "$DEPLOY_LOG" 2>/dev/null || printf "%b\n" "  ▲ $*"; }
log_info(){ printf "%b\n" "  ${C_CYAN}ℹ${C_RESET} $*" | tee -a "$DEPLOY_LOG" 2>/dev/null || printf "%b\n" "  ℹ $*"; }

section() {
  local title="$1"
  local width=64
  printf "\n${C_BOLD}${C_BLUE}┏"; printf '━%.0s' $(seq 1 "$width"); printf "┓${C_RESET}\n"
  printf "${C_BOLD}${C_BLUE}┃${C_RESET}  %-*s${C_BOLD}${C_BLUE}┃${C_RESET}\n" $((width - 2)) "$title"
  printf "${C_BOLD}${C_BLUE}┗"; printf '━%.0s' $(seq 1 "$width"); printf "┛${C_RESET}\n\n"
}

confirm() {
  local prompt="${1:-¿Confirmás?}"
  local resp
  read -r -p "$(printf "${C_YELLOW}%s${C_RESET} [s/N]: " "$prompt")" resp
  [[ "$resp" =~ ^([sS][iI]?|[yY])$ ]]
}

press_enter() {
  printf "\n${C_DIM}Presioná ENTER para continuar...${C_RESET}"
  read -r
}

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    log_err "Falta el comando '$1' en el PATH."
    return 1
  fi
}

as_root_or_sudo() {
  if [[ $EUID -eq 0 ]]; then
    "$@"
  else
    sudo "$@"
  fi
}

validate_project_layout() {
  if [[ ! -d "$REPO_DIR/.git" ]]; then
    log_err "No encuentro un repo git en ${REPO_DIR}."
    return 1
  fi
  if [[ ! -f "$PROJECT_DIR/pom.xml" ]]; then
    log_err "No encuentro Maven en ${PROJECT_DIR}."
    return 1
  fi
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

normalize_db_type() {
  case "${DB_TYPE,,}" in
    mysql|mariadb) DB_TYPE="${DB_TYPE,,}" ;;
    *) echo "Unsupported SCA_DB_TYPE: ${DB_TYPE}. Use mysql or mariadb." >&2; exit 1 ;;
  esac
}

default_db_port() {
  if [[ -n "$DB_PORT" ]]; then
    printf '%s' "$DB_PORT"
    return
  fi
  printf '3306'
}

build_jdbc_url() {
  local port="$1"
  case "$DB_TYPE" in
    mysql) printf 'jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8' "$DB_HOST" "$port" "$DB_NAME" ;;
    mariadb) printf 'jdbc:mariadb://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8' "$DB_HOST" "$port" "$DB_NAME" ;;
  esac
}

read_env_file_value() {
  local key="$1"
  if ! sudo test -f "$DB_ENV_FILE"; then
    return
  fi
  local line value
  line="$(sudo grep -E "^${key}=" "$DB_ENV_FILE" 2>/dev/null | tail -n 1 || true)"
  [[ -z "$line" ]] && return
  value="${line#${key}=}"
  value="${value%\"}"
  value="${value#\"}"
  value="${value%\'}"
  value="${value#\'}"
  printf '%s' "$value"
}

get_db_password() {
  if [[ -n "$DB_PASSWORD_INPUT" ]]; then
    printf '%s' "$DB_PASSWORD_INPUT"
    return
  fi
  if [[ -f "$DB_ENV_FILE" ]]; then
    return
  fi
  if [[ ! -t 0 ]]; then
    echo "$DB_ENV_FILE does not exist and SCA_DB_PASSWORD/CTN_DB_PASSWORD was not provided." >&2
    exit 1
  fi
  read -r -s -p "Database password for $DB_USER: " password
  echo >&2
  printf '%s' "$password"
}

write_db_env_file() {
  local password="$1"
  local db_port jdbc_url
  db_port="$(default_db_port)"
  jdbc_url="$(build_jdbc_url "$db_port")"

  local persisted_jwt_secret=""
  local persisted_demo_data=""
  local persisted_vapid_public_key=""
  local persisted_vapid_private_key=""
  if [[ -f "$DB_ENV_FILE" ]]; then
    persisted_jwt_secret="$(read_env_file_value 'JWT_SECRET')"
    persisted_demo_data="$(read_env_file_value 'SCA_LOAD_DEMO_DATA')"
    persisted_vapid_public_key="$(read_env_file_value 'CTN_VAPID_PUBLIC_KEY')"
    persisted_vapid_private_key="$(read_env_file_value 'CTN_VAPID_PRIVATE_KEY')"
    if [[ -z "$password" ]]; then
      password="$(read_env_file_value 'SCA_DB_PASSWORD')"
      [[ -z "$password" ]] && password="$(read_env_file_value 'CTN_DB_PASSWORD')"
    fi
  fi

  local load_demo_data="$LOAD_DEMO_DATA_INPUT"
  [[ -z "$load_demo_data" ]] && load_demo_data="${persisted_demo_data:-false}"

  local jwt_secret=""
  if [[ -n "${SCA_JWT_SECRET:-}" ]]; then
    jwt_secret="$SCA_JWT_SECRET"
  elif [[ -n "${JWT_SECRET:-}" ]]; then
    jwt_secret="$JWT_SECRET"
  elif [[ -n "$persisted_jwt_secret" ]]; then
    jwt_secret="$persisted_jwt_secret"
  elif command -v openssl >/dev/null 2>&1; then
    jwt_secret="$(openssl rand -base64 48)"
    echo "==> Generated a new JWT secret to persist in $DB_ENV_FILE"
  fi

  local vapid_public_key="${VAPID_PUBLIC_KEY:-$persisted_vapid_public_key}"
  local vapid_private_key="${VAPID_PRIVATE_KEY:-$persisted_vapid_private_key}"

  echo "==> Writing runtime config to $DB_ENV_FILE"
  local tmp_env
  tmp_env="$(mktemp)"
  chmod 600 "$tmp_env"
  {
    printf 'APP_PORT=%q\n' "$APP_PORT"
    printf 'SERVER_PORT=%q\n' "$APP_PORT"
    printf 'SCA_DB_TYPE=%q\n' "$DB_TYPE"
    printf 'SCA_DB_NAME=%q\n' "$DB_NAME"
    printf 'SCA_DB_HOST=%q\n' "$DB_HOST"
    printf 'SCA_DB_PORT=%q\n' "$db_port"
    printf 'SCA_DB_USER=%q\n' "$DB_USER"
    printf 'SCA_DB_PASSWORD=%q\n' "$password"
    printf 'CTN_DB_NAME=%q\n' "$DB_NAME"
    printf 'CTN_DB_HOST=%q\n' "${DB_HOST}:${db_port}"
    printf 'CTN_DB_USER=%q\n' "$DB_USER"
    printf 'CTN_DB_PASSWORD=%q\n' "$password"
    printf 'SPRING_DATASOURCE_URL=%q\n' "$jdbc_url"
    printf 'SPRING_DATASOURCE_USERNAME=%q\n' "$DB_USER"
    printf 'SPRING_DATASOURCE_PASSWORD=%q\n' "$password"
    printf 'SCA_LOAD_DEMO_DATA=%q\n' "$load_demo_data"
    [[ -n "$GOOGLE_CLIENT_ID" ]] && printf 'GOOGLE_CLIENT_ID=%q\n' "$GOOGLE_CLIENT_ID"
    [[ -n "$GOOGLE_CLIENT_SECRET" ]] && printf 'GOOGLE_CLIENT_SECRET=%q\n' "$GOOGLE_CLIENT_SECRET"
    [[ -n "$GOOGLE_REDIRECT_URI" ]] && printf 'GOOGLE_REDIRECT_URI=%q\n' "$GOOGLE_REDIRECT_URI"
    [[ -n "$vapid_public_key" ]] && printf 'CTN_VAPID_PUBLIC_KEY=%q\n' "$vapid_public_key"
    [[ -n "$vapid_private_key" ]] && printf 'CTN_VAPID_PRIVATE_KEY=%q\n' "$vapid_private_key"
    if [[ -n "$jwt_secret" ]]; then
      printf 'JWT_SECRET=%q\n' "$jwt_secret"
      printf 'SCA_JWT_SECRET=%q\n' "$jwt_secret"
    fi
  } > "$tmp_env"
  sudo install -o root -g root -m 600 "$tmp_env" "$DB_ENV_FILE"
  rm -f "$tmp_env"
}

configure_service_env() {
  local password
  password="$(get_db_password)"
  write_db_env_file "$password"

  echo "==> Configuring environment for service $SERVICE_NAME"
  sudo mkdir -p "$SYSTEMD_DROPIN_DIR"
  local tmp_dropin
  tmp_dropin="$(mktemp)"
  cat > "$tmp_dropin" <<DROPIN
[Service]
EnvironmentFile=$DB_ENV_FILE
DROPIN
  sudo install -o root -g root -m 644 "$tmp_dropin" "$SYSTEMD_DROPIN_FILE"
  rm -f "$tmp_dropin"
  sudo systemctl daemon-reload
}

service_exists() {
  command -v systemctl >/dev/null 2>&1 \
    && systemctl list-unit-files --type=service --no-legend 2>/dev/null | awk '{print $1}' | grep -Fxq "${SERVICE_NAME}.service"
}

ensure_service_exists() {
  if service_exists; then
    return 0
  fi

  echo "==> Service ${SERVICE_NAME}.service not found; creating a systemd unit for this JAR"
  local java_bin
  java_bin="$(command -v java || true)"
  if [[ -z "$java_bin" ]]; then
    echo "Cannot find java in PATH. Install Java and run deploy again." >&2
    exit 1
  fi

  local tmp_unit
  tmp_unit="$(mktemp)"
  cat > "$tmp_unit" <<UNIT
[Unit]
Description=CTN SCA Backend
After=network.target

[Service]
Type=simple
User=$APP_USER
Group=$APP_GROUP
WorkingDirectory=$INSTALL_DIR
EnvironmentFile=$DB_ENV_FILE
ExecStart=$java_bin -jar $INSTALL_DIR/$JAR_NAME
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT

  sudo install -o root -g root -m 644 "$tmp_unit" "$SERVICE_UNIT_PATH"
  rm -f "$tmp_unit"
  sudo systemctl daemon-reload
  sudo systemctl enable "$SERVICE_NAME" >/dev/null
}

build_frontend() {
  if [[ ! -f "$FRONTEND_DIR/package.json" ]]; then
    echo "==> No frontend/ found at $FRONTEND_DIR; skipping"
    return 0
  fi

  require_command npm
  echo "==> Building frontend into backend/src/main/resources/static"
  if [[ -f "$FRONTEND_DIR/package-lock.json" ]]; then
    npm --prefix "$FRONTEND_DIR" ci
  else
    npm --prefix "$FRONTEND_DIR" install
  fi
  npm --prefix "$FRONTEND_DIR" run build
}

update_system() {
  require_command git
  require_command mvn
  require_command find
  require_command curl
  require_command systemctl
  require_command sudo
  require_command java
  require_command openssl
  normalize_db_type
  validate_project_layout
  ensure_service_exists

  echo "==> Pulling latest changes"
  git -C "$REPO_DIR" pull --ff-only
  configure_service_env
  build_frontend

  echo "==> Building Spring Boot JAR"
  mvn -f "$PROJECT_DIR/pom.xml" clean package -DskipTests

  local built_jar
  built_jar="$(find "$PROJECT_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)"
  if [[ -z "$built_jar" ]]; then
    echo "No runnable .jar file found under $PROJECT_DIR/target" >&2
    return 1
  fi

  echo "==> Installing artifact in $INSTALL_DIR"
  sudo mkdir -p "$INSTALL_DIR"
  sudo install -o "$APP_USER" -g "$APP_GROUP" -m 644 "$built_jar" "$INSTALL_DIR/$JAR_NAME"
  sudo systemctl restart "$SERVICE_NAME"

  echo "==> Waiting for service to become active"
  local waited=0
  while (( waited < STARTUP_TIMEOUT_SECONDS )); do
    if sudo systemctl is-active --quiet "$SERVICE_NAME"; then
      break
    fi
    sleep 1
    waited=$((waited + 1))
  done
  if ! sudo systemctl is-active --quiet "$SERVICE_NAME"; then
    echo "Service did not become active within ${STARTUP_TIMEOUT_SECONDS}s" >&2
    sudo systemctl status "$SERVICE_NAME" --no-pager -l || true
    sudo journalctl -u "$SERVICE_NAME" -n 150 --no-pager || true
    return 1
  fi

  local attempt=1
  while (( attempt <= HEALTHCHECK_RETRIES )); do
    if curl -fsS --max-time 5 "$APP_URL" >/dev/null; then
      echo "==> System updated successfully"
      return 0
    fi
    echo "==> Health check failed on attempt ${attempt}/${HEALTHCHECK_RETRIES}, retrying in ${HEALTHCHECK_DELAY_SECONDS}s..."
    sleep "$HEALTHCHECK_DELAY_SECONDS"
    attempt=$((attempt + 1))
  done

  echo "ERROR: Health check failed after ${HEALTHCHECK_RETRIES} attempts." >&2
  sudo systemctl status "$SERVICE_NAME" --no-pager -l || true
  sudo journalctl -u "$SERVICE_NAME" -n 150 --no-pager || true
  return 1
}

database_client() {
  if command -v mariadb >/dev/null 2>&1; then
    command -v mariadb
  else
    command -v mysql
  fi
}

database_dump_client() {
  if command -v mariadb-dump >/dev/null 2>&1; then
    command -v mariadb-dump
  else
    command -v mysqldump
  fi
}

escape_option_value() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  printf '%s' "$value"
}

create_database_client_config() {
  local path="$1"
  local password="$2"
  local port
  port="$(default_db_port)"
  chmod 600 "$path"
  {
    echo '[client]'
    printf 'host="%s"\n' "$(escape_option_value "$DB_HOST")"
    printf 'port="%s"\n' "$(escape_option_value "$port")"
    printf 'user="%s"\n' "$(escape_option_value "$DB_USER")"
    printf 'password="%s"\n' "$(escape_option_value "$password")"
    echo 'default-character-set=utf8mb4'
  } > "$path"
}

backup_database_if_possible() {
  local config_file="$1"
  local dump_client
  dump_client="$(database_dump_client 2>/dev/null || true)"
  if [[ -z "$dump_client" ]]; then
    echo "==> No dump utility found; skipping automatic backup"
    return 0
  fi

  local backup_dir="${BACKUP_DIR:-/var/backups/ctn-sca-info}"
  local temporary_backup
  temporary_backup="$(mktemp)"
  if "$dump_client" --defaults-extra-file="$config_file" --single-transaction --routines --events "$DB_NAME" > "$temporary_backup" 2>/dev/null; then
    local backup_name="${DB_NAME}-$(date +%Y%m%d-%H%M%S).sql"
    sudo mkdir -p "$backup_dir"
    sudo install -m 600 "$temporary_backup" "$backup_dir/$backup_name"
    echo "==> Backup saved to $backup_dir/$backup_name"
  else
    echo "==> Existing database could not be backed up (it may not exist yet)"
  fi
  rm -f "$temporary_backup"
}

load_default_database() {
  normalize_db_type
  require_command sudo
  require_command awk
  require_command mktemp

  if [[ ! "$DB_NAME" =~ ^[A-Za-z0-9_]+$ ]]; then
    echo "Invalid database name: $DB_NAME" >&2
    return 1
  fi

  local schema_file="$REPO_DIR/database/db-tables-properties.sql"
  local seed_file="$REPO_DIR/database/ctn-official-seed.sql"
  if [[ ! -f "$schema_file" || ! -f "$seed_file" ]]; then
    echo "Default schema or official seed is missing under $REPO_DIR/database" >&2
    return 1
  fi

  if [[ "${SCA_CONFIRM_DB_RESET:-}" != "RESET" ]]; then
    if [[ ! -t 0 ]]; then
      echo "Database reset requires SCA_CONFIRM_DB_RESET=RESET in non-interactive mode." >&2
      return 1
    fi
    echo "WARNING: this will replace database '$DB_NAME' with the official CTN dataset."
    read -r -p "Type RESET to continue: " confirmation
    if [[ "$confirmation" != "RESET" ]]; then
      echo "Database load cancelled."
      return 0
    fi
  fi

  local password
  password="$(get_db_password)"
  if [[ -z "$password" && -f "$DB_ENV_FILE" ]]; then
    password="$(read_env_file_value 'SCA_DB_PASSWORD')"
    [[ -z "$password" ]] && password="$(read_env_file_value 'CTN_DB_PASSWORD')"
  fi

  local client
  client="$(database_client 2>/dev/null || true)"
  if [[ -z "$client" ]]; then
    echo "Install the mysql or mariadb command-line client first." >&2
    return 1
  fi

  local client_config transformed_schema
  client_config="$(mktemp)"
  transformed_schema="$(mktemp)"
  create_database_client_config "$client_config" "$password"
  awk -v db="$DB_NAME" '
    NR == 1 && /^##/ { next }
    tolower($0) == "drop database if exists ctndb;" { print "DROP DATABASE IF EXISTS `" db "`;"; next }
    tolower($0) == "create database ctndb;" { print "CREATE DATABASE `" db "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"; next }
    tolower($0) == "use ctndb;" { print "USE `" db "`;"; next }
    { print }
  ' "$schema_file" > "$transformed_schema"

  backup_database_if_possible "$client_config"
  local restart_service=false
  if service_exists && sudo systemctl is-active --quiet "$SERVICE_NAME"; then
    restart_service=true
    echo "==> Stopping $SERVICE_NAME during database load"
    sudo systemctl stop "$SERVICE_NAME"
  fi

  echo "==> Loading database structure"
  if ! "$client" --defaults-extra-file="$client_config" < "$transformed_schema"; then
    rm -f "$client_config" "$transformed_schema"
    if [[ "$restart_service" == true ]]; then sudo systemctl start "$SERVICE_NAME"; fi
    return 1
  fi
  echo "==> Loading official CTN seed"
  if ! "$client" --defaults-extra-file="$client_config" "$DB_NAME" < "$seed_file"; then
    rm -f "$client_config" "$transformed_schema"
    if [[ "$restart_service" == true ]]; then sudo systemctl start "$SERVICE_NAME"; fi
    return 1
  fi

  rm -f "$client_config" "$transformed_schema"
  if [[ "$restart_service" == true ]]; then
    sudo systemctl start "$SERVICE_NAME"
  fi
  echo "==> Default database loaded successfully"
}

edit_linux_service() {
  require_command systemctl
  require_command sudo
  if ! service_exists; then
    echo "Linux service ${SERVICE_NAME}.service does not exist." >&2
    return 1
  fi

  echo "==> Current effective service definition"
  sudo systemctl cat "$SERVICE_NAME"
  if [[ ! -t 0 ]]; then
    return 0
  fi
  read -r -p "Open the systemd override editor? [y/N]: " answer
  [[ "$answer" =~ ^[Yy]$ ]] || return 0
  sudo systemctl edit "$SERVICE_NAME"
  sudo systemctl daemon-reload
  if [[ -t 0 ]]; then
    read -r -p "Restart ${SERVICE_NAME}.service now? [y/N]: " answer
    if [[ "$answer" =~ ^[Yy]$ ]]; then
      sudo systemctl restart "$SERVICE_NAME"
    fi
  fi
}

health_snapshot() {
  local service_state="not-installed"
  if service_exists; then
    service_state="$(systemctl is-active "$SERVICE_NAME" 2>/dev/null || true)"
  fi
  local http_state="unreachable"
  if command -v curl >/dev/null 2>&1; then
    http_state="$(curl -fsS --max-time 3 "$APP_URL" 2>/dev/null || printf 'unreachable')"
  fi
  printf 'Service: %-14s  HTTP: %s\n' "$service_state" "$http_state"
  printf 'Endpoint: %s\n' "$APP_URL"
  echo
  command -v free >/dev/null 2>&1 && free -h | sed -n '1,2p'
  df -h "$INSTALL_DIR" 2>/dev/null | sed -n '1,2p' || true
  if service_exists; then
    echo
    echo "Recent service logs:"
    journalctl -u "$SERVICE_NAME" -n 8 --no-pager 2>/dev/null || true
  fi
}

health_monitor() {
  require_command systemctl
  local interval="${HEALTH_MONITOR_INTERVAL:-5}"
  if [[ ! -t 0 ]]; then
    health_snapshot
    return 0
  fi
  local monitoring=true
  trap 'monitoring=false' INT
  while [[ "$monitoring" == true ]]; do
    printf '\033[2J\033[H'
    echo "Health monitor - refresh every ${interval}s - Ctrl+C to return"
    echo "----------------------------------------------------------------"
    health_snapshot
    sleep "$interval" || true
  done
  trap - INT
}

pause_menu() {
  if [[ -t 0 ]]; then
    echo
    read -r -p "Press Enter to return to the menu..." _
  fi
}

show_banner() {
  cat <<'BANNER'

              .-----------------------.
             /        C T N            \
            /   COLEGIO TECNICO         \
           |        NACIONAL             |
           |      +-----------+           |
           |      |  S C A    |           |
           |      +-----------+           |
            \                           /
             '-------------------------'

          Sistema de Carpetas Academicas
BANNER
}

main_menu() {
  while true; do
    printf '\033[2J\033[H'
    show_banner
    cat <<MENU
  1) Actualizar sistema
  2) Cargar base de datos por default
     (db-tables-properties.sql + ctn-official-seed.sql)
  3) Editar servicio Linux
  4) Health monitor
  5) Salir
MENU
    echo
    read -r -p "Select an action [1-5]: " choice
    case "$choice" in
      1) if ! update_system; then echo "System update failed."; fi; pause_menu ;;
      2) if ! load_default_database; then echo "Database load failed."; fi; pause_menu ;;
      3) if ! edit_linux_service; then echo "Service editor is unavailable."; fi; pause_menu ;;
      4) if ! health_monitor; then echo "Health monitor failed."; pause_menu; fi ;;
      5) echo "Bye."; return 0 ;;
      *) echo "Invalid option."; sleep 1 ;;
    esac
  done
}

show_help() {
  cat <<HELP
Usage: ./deploy.sh [action]

  --menu              Open the interactive CTN console
  --update            Pull, build, install and verify the system
  --load-default-db   Replace the database with schema + official seed
  --edit-service      Open the systemd override editor when the service exists
  --health            Start the health monitor (single snapshot without a TTY)
  --help              Show this help

Without arguments, an interactive terminal opens the menu. Non-interactive
execution keeps the historical behavior and updates the system.
HELP
}

case "${1:-}" in
  --menu) main_menu ;;
  --update) update_system ;;
  --load-default-db) load_default_database ;;
  --edit-service) edit_linux_service ;;
  --health) health_monitor ;;
  --help|-h) show_help ;;
  "") if [[ -t 0 ]]; then main_menu; else update_system; fi ;;
  *) echo "Unknown action: $1" >&2; show_help >&2; exit 2 ;;
esac
