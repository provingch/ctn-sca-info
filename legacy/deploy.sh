#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="/home/deploy/ctn-sca-info"
TOMCAT_SERVICE="${TOMCAT_SERVICE:-tomcat10}"
TOMCAT_WEBAPPS="${TOMCAT_WEBAPPS:-/var/lib/tomcat10/webapps}"
TOMCAT_USER="${TOMCAT_USER:-tomcat10}"
TOMCAT_GROUP="${TOMCAT_GROUP:-tomcat10}"
WAR_NAME="${WAR_NAME:-ROOT.war}"
APP_URL="${APP_URL:-http://127.0.0.1:8080}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-60}"
HEALTHCHECK_RETRIES="${HEALTHCHECK_RETRIES:-30}"
HEALTHCHECK_DELAY_SECONDS="${HEALTHCHECK_DELAY_SECONDS:-2}"
CATALINA_BASE="${CATALINA_BASE:-/var/lib/tomcat10}"

DB_ENV_FILE="${DB_ENV_FILE:-/etc/ctn-sca-info.env}"

# New SCA variables with CTN fallback for compatibility.
DB_NAME="${SCA_DB_NAME:-${CTN_DB_NAME:-ctndb}}"
DB_HOST="${SCA_DB_HOST:-${CTN_DB_HOST:-localhost:3306}}"
DB_USER="${SCA_DB_USER:-${CTN_DB_USER:-testadmin}}"
DB_PASSWORD_INPUT="${SCA_DB_PASSWORD:-${CTN_DB_PASSWORD:-}}"

SYSTEMD_DROPIN_DIR="/etc/systemd/system/${TOMCAT_SERVICE}.service.d"
SYSTEMD_DROPIN_FILE="${SYSTEMD_DROPIN_DIR}/ctn-sca-info.conf"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
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

  read -r -s -p "MariaDB password for $DB_USER: " password
  echo >&2
  printf "%s" "$password"
}

write_db_env_file() {
  local password="$1"

  if [[ -f "$DB_ENV_FILE" && -z "$password" ]]; then
    return
  fi

  echo "==> Writing DB config to $DB_ENV_FILE"
  local tmp_env
  tmp_env="$(mktemp)"
  chmod 600 "$tmp_env"
  {
    printf 'SCA_DB_NAME=%q\n' "$DB_NAME"
    printf 'SCA_DB_HOST=%q\n' "$DB_HOST"
    printf 'SCA_DB_USER=%q\n' "$DB_USER"
    printf 'SCA_DB_PASSWORD=%q\n' "$password"
    # Compatibility with code that still reads CTN_*.
    printf 'CTN_DB_NAME=%q\n' "$DB_NAME"
    printf 'CTN_DB_HOST=%q\n' "$DB_HOST"
    printf 'CTN_DB_USER=%q\n' "$DB_USER"
    printf 'CTN_DB_PASSWORD=%q\n' "$password"
  } > "$tmp_env"

  sudo install -o root -g root -m 600 "$tmp_env" "$DB_ENV_FILE"
  rm -f "$tmp_env"
}

configure_tomcat_env() {
  local password
  password="$(get_db_password)"
  write_db_env_file "$password"

  echo "==> Configuring environment for $TOMCAT_SERVICE"
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

require_command git
require_command mvn
require_command find
require_command curl
require_command systemctl
require_command sudo

print_tomcat_diagnostics() {
  echo "==> Tomcat diagnostics ($TOMCAT_SERVICE)" >&2
  sudo systemctl status "$TOMCAT_SERVICE" --no-pager -l || true
  echo "==> Recent journal logs" >&2
  sudo journalctl -u "$TOMCAT_SERVICE" -n 120 --no-pager || true
  echo "==> Recent Tomcat app logs" >&2
  for log_file in "$CATALINA_BASE"/logs/localhost*.log "$CATALINA_BASE"/logs/catalina*.log; do
    if [[ -f "$log_file" ]]; then
      echo "---- $log_file (tail -n 120)" >&2
      sudo tail -n 120 "$log_file" || true
    fi
  done
}

wait_for_tomcat_active() {
  local waited=0
  while (( waited < STARTUP_TIMEOUT_SECONDS )); do
    if sudo systemctl is-active --quiet "$TOMCAT_SERVICE"; then
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

if [[ ! -d "$PROJECT_DIR" ]]; then
  echo "Project directory does not exist: $PROJECT_DIR" >&2
  exit 1
fi

if [[ ! -d "$PROJECT_DIR/.git" ]]; then
  cat >&2 <<MSG
Project directory is not a git repository: $PROJECT_DIR

Verify the fixed project path exists and contains the repository.
MSG
  exit 1
fi

cd "$PROJECT_DIR"

echo "==> Pulling latest changes"
git -C "$PROJECT_DIR" pull --ff-only

configure_tomcat_env

echo "==> Building WAR"
mvn -f "$PROJECT_DIR/pom.xml" clean package

BUILT_WAR="$(find "$PROJECT_DIR/target" -maxdepth 1 -type f -name "*.war" | head -n 1)"
if [[ -z "$BUILT_WAR" ]]; then
  echo "No .war file found under target/" >&2
  exit 1
fi

echo "==> Stopping Tomcat"
sudo systemctl stop "$TOMCAT_SERVICE"

echo "==> Deploying as $WAR_NAME"
sudo rm -rf "$TOMCAT_WEBAPPS/ROOT"
sudo rm -f "$TOMCAT_WEBAPPS/$WAR_NAME"
sudo install -o "$TOMCAT_USER" -g "$TOMCAT_GROUP" -m 644 "$BUILT_WAR" "$TOMCAT_WEBAPPS/$WAR_NAME"

echo "==> Starting Tomcat"
sudo systemctl start "$TOMCAT_SERVICE"

echo "==> Waiting for Tomcat service to become active"
if ! wait_for_tomcat_active; then
  echo "Tomcat service did not become active within ${STARTUP_TIMEOUT_SECONDS}s." >&2
  print_tomcat_diagnostics
  exit 1
fi

echo "==> Checking local response"
if ! wait_for_app_response; then
  echo "Application is not responding at $APP_URL." >&2
  print_tomcat_diagnostics
  exit 1
fi

echo "SCA deploy completed"
