#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Repository root (for git pull) and backend project directory (for Maven build).
REPO_DIR="${REPO_DIR:-$SCRIPT_DIR}"
PROJECT_DIR="${PROJECT_DIR:-}"

if [[ -z "$PROJECT_DIR" ]]; then
  if [[ -f "$REPO_DIR/backend/pom.xml" ]]; then
    PROJECT_DIR="$REPO_DIR/backend"
  elif [[ -f "$REPO_DIR/pom.xml" ]]; then
    PROJECT_DIR="$REPO_DIR"
  else
    # Keep the legacy default path in the error message context below.
    PROJECT_DIR="$REPO_DIR/backend"
  fi
fi

SERVICE_NAME="${SERVICE_NAME:-sca-backend}"
APP_USER="${APP_USER:-deploy}"
APP_GROUP="${APP_GROUP:-deploy}"
INSTALL_DIR="${INSTALL_DIR:-/opt/ctn-sca-info/backend}"
JAR_NAME="${JAR_NAME:-sca-backend.jar}"

APP_PORT="${APP_PORT:-8080}"
APP_URL="${APP_URL:-http://127.0.0.1:${APP_PORT}}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-90}"
HEALTHCHECK_RETRIES="${HEALTHCHECK_RETRIES:-45}"
HEALTHCHECK_DELAY_SECONDS="${HEALTHCHECK_DELAY_SECONDS:-2}"

DB_ENV_FILE="${DB_ENV_FILE:-/etc/ctn-sca-info-backend.env}"
DB_TYPE="${SCA_DB_TYPE:-mariadb}"
DB_NAME="${SCA_DB_NAME:-${CTN_DB_NAME:-ctndb}}"
DB_HOST="${SCA_DB_HOST:-${CTN_DB_HOST:-localhost}}"
DB_PORT="${SCA_DB_PORT:-${CTN_DB_PORT:-}}"
DB_USER="${SCA_DB_USER:-${CTN_DB_USER:-testadmin}}"
DB_PASSWORD_INPUT="${SCA_DB_PASSWORD:-${CTN_DB_PASSWORD:-}}"

GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-}"
GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-}"
GOOGLE_REDIRECT_URI="${GOOGLE_REDIRECT_URI:-}"

SYSTEMD_DROPIN_DIR="/etc/systemd/system/${SERVICE_NAME}.service.d"
SYSTEMD_DROPIN_FILE="${SYSTEMD_DROPIN_DIR}/ctn-sca-info.conf"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

normalize_db_type() {
  case "${DB_TYPE,,}" in
    mysql|mariadb|postgres|postgresql)
      DB_TYPE="${DB_TYPE,,}"
      ;;
    *)
      echo "Unsupported SCA_DB_TYPE: ${DB_TYPE}. Use mysql, mariadb, postgres or postgresql." >&2
      exit 1
      ;;
  esac
}

default_db_port() {
  if [[ -n "$DB_PORT" ]]; then
    printf "%s" "$DB_PORT"
    return
  fi

  case "$DB_TYPE" in
    mysql|mariadb)
      printf "3306"
      ;;
    postgres|postgresql)
      printf "5432"
      ;;
  esac
}

build_jdbc_url() {
  local port="$1"

  case "$DB_TYPE" in
    mysql)
      printf "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8" "$DB_HOST" "$port" "$DB_NAME"
      ;;
    mariadb)
      printf "jdbc:mariadb://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8" "$DB_HOST" "$port" "$DB_NAME"
      ;;
    postgres|postgresql)
      printf "jdbc:postgresql://%s:%s/%s" "$DB_HOST" "$port" "$DB_NAME"
      ;;
  esac
}

get_db_password() {
  if [[ -n "$DB_PASSWORD_INPUT" ]]; then
    printf "%s" "$DB_PASSWORD_INPUT"
    return
  fi

  if [[ -f "$DB_ENV_FILE" ]]; then
    return
  fi

  if [[ ! -t 0 ]]; then
    cat >&2 <<MSG
$DB_ENV_FILE does not exist and SCA_DB_PASSWORD/CTN_DB_PASSWORD was not provided.
Run once with:

  SCA_DB_PASSWORD='your_password' ./deploy.sh

Or run interactively to enter a hidden password.
MSG
    exit 1
  fi

  read -r -s -p "Database password for $DB_USER: " password
  echo >&2
  printf "%s" "$password"
}

write_db_env_file() {
  local password="$1"
  local db_port
  local jdbc_url

  db_port="$(default_db_port)"
  jdbc_url="$(build_jdbc_url "$db_port")"

  if [[ -f "$DB_ENV_FILE" && -z "$password" ]]; then
    return
  fi

  if [[ -z "$GOOGLE_CLIENT_ID" || -z "$GOOGLE_CLIENT_SECRET" || -z "$GOOGLE_REDIRECT_URI" ]]; then
    cat >&2 <<MSG
Google OAuth values are required to initialize or rewrite $DB_ENV_FILE.

Set these environment variables and run deploy again:
  GOOGLE_CLIENT_ID
  GOOGLE_CLIENT_SECRET
  GOOGLE_REDIRECT_URI

Example:
  GOOGLE_CLIENT_ID='...' GOOGLE_CLIENT_SECRET='...' GOOGLE_REDIRECT_URI='https://ctn-sca.ddns.net/google/callback' ./deploy.sh
MSG
    exit 1
  fi

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

    # Backward compatibility for code paths that still read CTN_* variables.
    printf 'CTN_DB_NAME=%q\n' "$DB_NAME"
    printf 'CTN_DB_HOST=%q\n' "${DB_HOST}:${db_port}"
    printf 'CTN_DB_USER=%q\n' "$DB_USER"
    printf 'CTN_DB_PASSWORD=%q\n' "$password"

    # Spring datasource values for newer code paths.
    printf 'SPRING_DATASOURCE_URL=%q\n' "$jdbc_url"
    printf 'SPRING_DATASOURCE_USERNAME=%q\n' "$DB_USER"
    printf 'SPRING_DATASOURCE_PASSWORD=%q\n' "$password"

    # Google OAuth values used by AppConfig.get("google.client.*").
    printf 'GOOGLE_CLIENT_ID=%q\n' "$GOOGLE_CLIENT_ID"
    printf 'GOOGLE_CLIENT_SECRET=%q\n' "$GOOGLE_CLIENT_SECRET"
    printf 'GOOGLE_REDIRECT_URI=%q\n' "$GOOGLE_REDIRECT_URI"
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

wait_for_service_active() {
  local waited=0
  while (( waited < STARTUP_TIMEOUT_SECONDS )); do
    if sudo systemctl is-active --quiet "$SERVICE_NAME"; then
      return 0
    fi
    sleep 1
    waited=$((waited + 1))
  done
  return 1
}

wait_for_app_response() {
  local retries="$HEALTHCHECK_RETRIES"
  local delay="$HEALTHCHECK_DELAY_SECONDS"
  local attempt=1

  while (( attempt <= retries )); do
    if curl -fsS -I "$APP_URL" >/dev/null; then
      return 0
    fi
    sleep "$delay"
    attempt=$((attempt + 1))
  done
  return 1
}

print_diagnostics() {
  echo "==> Service diagnostics ($SERVICE_NAME)" >&2
  sudo systemctl status "$SERVICE_NAME" --no-pager -l || true
  echo "==> Recent journal logs" >&2
  sudo journalctl -u "$SERVICE_NAME" -n 150 --no-pager || true
}

ensure_service_exists() {
  if sudo systemctl list-unit-files --type=service --no-legend | awk '{print $1}' | grep -Fxq "${SERVICE_NAME}.service"; then
    return 0
  fi

  echo "Systemd service not found: ${SERVICE_NAME}.service" >&2
  echo "Set SERVICE_NAME to your real unit name and run deploy again." >&2
  echo "Example: SERVICE_NAME=sca ./deploy.sh" >&2
  echo "" >&2
  echo "Possible related services on this host:" >&2
  sudo systemctl list-unit-files --type=service --no-legend | awk '{print $1}' | grep -Ei 'sca|ctn|backend|tomcat' || true
  exit 1
}

require_command git
require_command mvn
require_command find
require_command curl
require_command systemctl
require_command sudo

normalize_db_type

if [[ ! -d "$REPO_DIR/.git" ]]; then
  cat >&2 <<MSG
Repository directory is not a git repository: $REPO_DIR

Verify REPO_DIR points to your repository root.
MSG
  exit 1
fi

if [[ ! -f "$PROJECT_DIR/pom.xml" ]]; then
  echo "Cannot find Maven project (pom.xml) at: $PROJECT_DIR" >&2
  echo "Tip: set PROJECT_DIR explicitly, for example PROJECT_DIR=/home/deploy/ctn-sca-info/backend" >&2
  exit 1
fi

ensure_service_exists

echo "==> Pulling latest changes"
git -C "$REPO_DIR" pull --ff-only

configure_service_env

echo "==> Building Spring Boot JAR"
mvn -f "$PROJECT_DIR/pom.xml" clean package -DskipTests

BUILT_JAR="$(find "$PROJECT_DIR/target" -maxdepth 1 -type f -name "*.jar" ! -name "original-*.jar" | head -n 1)"
if [[ -z "$BUILT_JAR" ]]; then
  echo "No runnable .jar file found under $PROJECT_DIR/target" >&2
  exit 1
fi

echo "==> Installing artifact in $INSTALL_DIR"
sudo mkdir -p "$INSTALL_DIR"
sudo install -o "$APP_USER" -g "$APP_GROUP" -m 644 "$BUILT_JAR" "$INSTALL_DIR/$JAR_NAME"

echo "==> Restarting service"
sudo systemctl restart "$SERVICE_NAME"

echo "==> Waiting for service to become active"
if ! wait_for_service_active; then
  echo "Service did not become active within ${STARTUP_TIMEOUT_SECONDS}s" >&2
  print_diagnostics
  exit 1
fi

echo "==> Checking response on $APP_URL"
if ! wait_for_app_response; then
  echo "Application is not responding on $APP_URL" >&2
  print_diagnostics
  exit 1
fi

echo "Deploy completed successfully"
