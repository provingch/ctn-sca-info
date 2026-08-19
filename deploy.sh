#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Repository root (for git pull) and backend project directory (for Maven build).
REPO_DIR="${REPO_DIR:-$SCRIPT_DIR}"
PROJECT_DIR="${PROJECT_DIR:-}"
FRONTEND_DIR="${FRONTEND_DIR:-$REPO_DIR/frontend}"

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
APP_URL="${APP_URL:-http://127.0.0.1:${APP_PORT}/api/health}"
STARTUP_TIMEOUT_SECONDS="${STARTUP_TIMEOUT_SECONDS:-90}"
# Reasonable defaults: try 10 times with 2s delay (≈20s) — app needs ~8-11s to boot
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
SYSTEMD_UNIT_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

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

read_env_file_value() {
  local key="$1"
  local line

  if ! sudo test -f "$DB_ENV_FILE"; then
    return
  fi

  line="$(sudo grep -E "^${key}=" "$DB_ENV_FILE" 2>/dev/null | tail -n 1 || true)"
  if [[ -z "$line" ]]; then
    return
  fi

  local value="${line#${key}=}"
  value="${value%\"}"
  value="${value#\"}"
  value="${value%\'}"
  value="${value#\'}"
  printf '%s' "$value"
}

write_db_env_file() {
  local password="$1"
  local db_port
  local jdbc_url

  db_port="$(default_db_port)"
  jdbc_url="$(build_jdbc_url "$db_port")"

  local persisted_jwt_secret=""
  local persisted_password=""
  local persisted_demo_data=""
  local persisted_vapid_public_key=""
  local persisted_vapid_private_key=""
  if [[ -f "$DB_ENV_FILE" ]]; then
    persisted_jwt_secret="$(read_env_file_value 'JWT_SECRET')"
    persisted_demo_data="$(read_env_file_value 'SCA_LOAD_DEMO_DATA')"
    persisted_vapid_public_key="$(read_env_file_value 'CTN_VAPID_PUBLIC_KEY')"
    persisted_vapid_private_key="$(read_env_file_value 'CTN_VAPID_PRIVATE_KEY')"
    if [[ -z "$password" ]]; then
      persisted_password="$(read_env_file_value 'SCA_DB_PASSWORD')"
      if [[ -z "$persisted_password" ]]; then
        persisted_password="$(read_env_file_value 'CTN_DB_PASSWORD')"
      fi
      password="$persisted_password"
    fi
  fi

  local vapid_public_key="${VAPID_PUBLIC_KEY:-$persisted_vapid_public_key}"
  local vapid_private_key="${VAPID_PRIVATE_KEY:-$persisted_vapid_private_key}"

  local load_demo_data="$LOAD_DEMO_DATA_INPUT"
  if [[ -z "$load_demo_data" ]]; then
    load_demo_data="${persisted_demo_data:-false}"
  fi

  # Determine or generate JWT secret to persist in the env file. Priority:
  # 1) explicit env vars SCA_JWT_SECRET / JWT_SECRET passed by operator
  # 2) existing value already persisted in $DB_ENV_FILE (if present)
  # 3) generate a strong random secret using openssl
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

  if [[ -f "$DB_ENV_FILE" && -z "$password" && -z "${SCA_JWT_SECRET:-}" && -z "${JWT_SECRET:-}" && -z "$GOOGLE_CLIENT_ID" && -z "$GOOGLE_CLIENT_SECRET" && -z "$GOOGLE_REDIRECT_URI" && -z "$VAPID_PUBLIC_KEY" && -z "$VAPID_PRIVATE_KEY" && -z "$LOAD_DEMO_DATA_INPUT" ]]; then
    return
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
    printf 'SCA_LOAD_DEMO_DATA=%q\n' "$load_demo_data"

    # Google OAuth values used by AppConfig.get("google.client.*").
    if [[ -n "$GOOGLE_CLIENT_ID" ]]; then
      printf 'GOOGLE_CLIENT_ID=%q\n' "$GOOGLE_CLIENT_ID"
    fi
    if [[ -n "$GOOGLE_CLIENT_SECRET" ]]; then
      printf 'GOOGLE_CLIENT_SECRET=%q\n' "$GOOGLE_CLIENT_SECRET"
    fi
    if [[ -n "$GOOGLE_REDIRECT_URI" ]]; then
      printf 'GOOGLE_REDIRECT_URI=%q\n' "$GOOGLE_REDIRECT_URI"
    fi
    if [[ -n "$vapid_public_key" ]]; then
      printf 'CTN_VAPID_PUBLIC_KEY=%q\n' "$vapid_public_key"
    fi
    if [[ -n "$vapid_private_key" ]]; then
      printf 'CTN_VAPID_PRIVATE_KEY=%q\n' "$vapid_private_key"
    fi
    # Persist JWT secret for the application (SPRING / direct property mapping)
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
    # Use a small per-request timeout to avoid long hangs and allow retries
    if curl -fsS --max-time 5 "$APP_URL" >/dev/null; then
      return 0
    fi
    echo "==> Health check failed on attempt ${attempt}/${retries}, retrying in ${delay}s..."
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

build_frontend() {
  if [[ ! -f "$FRONTEND_DIR/package.json" ]]; then
    echo "==> No frontend/ found at $FRONTEND_DIR (nothing to build yet); skipping"
    return 0
  fi

  require_command npm

  echo "==> Building frontend (Vite) into backend/src/main/resources/static"
  if [[ -f "$FRONTEND_DIR/package-lock.json" ]]; then
    npm --prefix "$FRONTEND_DIR" ci
  else
    npm --prefix "$FRONTEND_DIR" install
  fi
  npm --prefix "$FRONTEND_DIR" run build
}

ensure_service_exists() {
  if sudo systemctl list-unit-files --type=service --no-legend | awk '{print $1}' | grep -Fxq "${SERVICE_NAME}.service"; then
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

  sudo install -o root -g root -m 644 "$tmp_unit" "$SYSTEMD_UNIT_FILE"
  rm -f "$tmp_unit"
  sudo systemctl daemon-reload
  sudo systemctl enable "$SERVICE_NAME" >/dev/null
}

require_command git
require_command mvn
require_command find
require_command curl
require_command systemctl
require_command sudo
require_command java
require_command openssl

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
# Clean known frontend build residues that may be untracked and block pull
clean_frontend_residues() {
  # Assets generated by Vite into the backend resources folder
  local assets_dir="$REPO_DIR/backend/src/main/resources/static/assets"
  if [[ -d "$assets_dir" ]]; then
    echo "==> Removing generated frontend assets in $assets_dir"
    # Remove common hashed bundle files that Vite emits (index-*.js/css and their maps)
    find "$assets_dir" -maxdepth 1 -type f \( -name 'index-*.js' -o -name 'index-*.css' -o -name 'index-*.js.map' -o -name 'index-*.css.map' -o -name 'index-*.js.gz' -o -name 'index-*.css.gz' \) -print0 | xargs -0 -r rm -f --
  fi

  # Also remove any top-level index-*.{js,css} files in static root
  local static_root="$REPO_DIR/backend/src/main/resources/static"
  if [[ -d "$static_root" ]]; then
    find "$static_root" -maxdepth 1 -type f \( -name 'index-*.js' -o -name 'index-*.css' -o -name 'index-*.js.map' -o -name 'index-*.css.map' \) -print0 | xargs -0 -r rm -f --
  fi

  # Clean frontend dist folder (if present) to avoid leftover built files
  local frontend_dist="$FRONTEND_DIR/dist"
  if [[ -d "$frontend_dist" ]]; then
    echo "==> Removing frontend dist directory $frontend_dist"
    rm -rf "$frontend_dist"
  fi

  # Also clean bundled assets that sometimes land in backend/target/classes/static during build
  local target_assets="$PROJECT_DIR/target/classes/static/assets"
  if [[ -d "$target_assets" ]]; then
    echo "==> Removing generated assets in $target_assets"
    find "$target_assets" -maxdepth 1 -type f \( -name 'index-*.js' -o -name 'index-*.css' -o -name 'index-*.js.map' -o -name 'index-*.css.map' \) -print0 | xargs -0 -r rm -f --
  fi

  # Remove untracked backend build residues (target, classes, generated-sources) only
  remove_untracked_under() {
    local abs_path="$1"
    # Derive repository-relative path (git expects paths relative to repo root)
    local rel_path
    rel_path="${abs_path#$REPO_DIR/}"
    # Ask git for untracked files under this path
    local untracked
    untracked=$(git -C "$REPO_DIR" ls-files --others --exclude-standard -- "$rel_path" 2>/dev/null || true)
    if [[ -n "$untracked" ]]; then
      echo "==> Removing untracked files under $rel_path"
      # Remove each untracked path (safe: only removes untracked files)
      printf '%s\n' "$untracked" | while IFS= read -r f; do
        rm -rf -- "$REPO_DIR/$f" || true
      done
    fi
  }

  # Candidate backend/build paths to scan for untracked files
  for p in "$PROJECT_DIR/target" "$REPO_DIR/target" "$PROJECT_DIR/classes" "$PROJECT_DIR/generated-sources" "$PROJECT_DIR/src/main/resources/static"; do
    if [[ -e "$p" ]]; then
      remove_untracked_under "$p"
    fi
  done
}

clean_frontend_residues

git -C "$REPO_DIR" pull --ff-only

configure_service_env

build_frontend

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
  echo "ERROR: Health check failed after ${HEALTHCHECK_RETRIES} attempts." >&2
  echo "       Review the service with: journalctl -u ${SERVICE_NAME}.service" >&2
  print_diagnostics
  exit 1
fi

echo "Deploy completed successfully"
