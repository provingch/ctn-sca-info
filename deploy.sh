#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="${REPO_DIR:-$SCRIPT_DIR}"
PROJECT_DIR="${PROJECT_DIR:-}"
first_run_wizard() {
  # Only run when called with no args, interactive TTY, and no existing user config
  if [[ -n "${1:-}" ]]; then
    return 0
  fi
  if [[ ! -t 0 ]]; then
    return 0
  fi
  if [[ -f "$USER_CONFIG_FILE" ]]; then
    return 0
  fi

  echo
  echo "Este asistente va a crear un archivo de configuración de usuario y un alias 'deploy'."
  echo "Los valores de configuración no incluirán la contraseña de la base de datos."
  echo
  echo "Nota: podés dejar en blanco las credenciales de Google o VAPID si no las tenés ahora."
  if ! confirm "¿Querés configurar el alias 'deploy' con tus datos ahora?"; then
    return 0
  fi

  local def_service def_port def_domain def_email def_db_name def_db_host def_db_port def_db_user inp
  def_service="${SERVICE_NAME:-sca-backend}"
  def_port="${APP_PORT:-8080}"
  def_domain="${DOMAIN_NAME:-}"
  def_email="${CERTBOT_EMAIL:-}"
  def_db_name="${SCA_DB_NAME:-${CTN_DB_NAME:-ctndb}}"
  def_db_host="${SCA_DB_HOST:-${CTN_DB_HOST:-localhost}}"
  def_db_port="${SCA_DB_PORT:-}"
  def_db_user="${SCA_DB_USER:-${CTN_DB_USER:-testadmin}}"

  read -r -p "SERVICE_NAME [${def_service}]: " inp
  SERVICE_NAME="${inp:-$def_service}"
  read -r -p "APP_PORT [${def_port}]: " inp
  APP_PORT="${inp:-$def_port}"
  read -r -p "DOMAIN_NAME [${def_domain}]: " inp
  DOMAIN_NAME="${inp:-$def_domain}"
  read -r -p "CERTBOT_EMAIL [${def_email}]: " inp
  CERTBOT_EMAIL="${inp:-$def_email}"
  read -r -p "SCA_DB_NAME [${def_db_name}]: " inp
  SCA_DB_NAME="${inp:-$def_db_name}"
  read -r -p "SCA_DB_HOST [${def_db_host}]: " inp
  SCA_DB_HOST="${inp:-$def_db_host}"
  read -r -p "SCA_DB_PORT [${def_db_port}]: " inp
  SCA_DB_PORT="${inp:-$def_db_port}"
  read -r -p "SCA_DB_USER [${def_db_user}]: " inp
  SCA_DB_USER="${inp:-$def_db_user}"

  # Opcionales: credenciales de Google OAuth y claves VAPID. Enter para dejar en blanco.
  read -r -p "GOOGLE_CLIENT_ID [${GOOGLE_CLIENT_ID:-}]: " inp
  GOOGLE_CLIENT_ID="${inp:-${GOOGLE_CLIENT_ID:-}}"
  read -r -p "GOOGLE_CLIENT_SECRET [${GOOGLE_CLIENT_SECRET:-}]: " inp
  GOOGLE_CLIENT_SECRET="${inp:-${GOOGLE_CLIENT_SECRET:-}}"
  read -r -p "GOOGLE_REDIRECT_URI [${GOOGLE_REDIRECT_URI:-}]: " inp
  GOOGLE_REDIRECT_URI="${inp:-${GOOGLE_REDIRECT_URI:-}}"

  read -r -p "CTN_VAPID_PUBLIC_KEY [${CTN_VAPID_PUBLIC_KEY:-}]: " inp
  CTN_VAPID_PUBLIC_KEY="${inp:-${CTN_VAPID_PUBLIC_KEY:-}}"
  read -r -p "CTN_VAPID_PRIVATE_KEY [${CTN_VAPID_PRIVATE_KEY:-}]: " inp
  CTN_VAPID_PRIVATE_KEY="${inp:-${CTN_VAPID_PRIVATE_KEY:-}}"

  mkdir -p "$USER_CONFIG_DIR"
  local tmpcfg
  tmpcfg="$(mktemp)"
  chmod 600 "$tmpcfg"
  printf 'SERVICE_NAME=%q\n' "$SERVICE_NAME" >> "$tmpcfg"
  printf 'APP_PORT=%q\n' "$APP_PORT" >> "$tmpcfg"
  printf 'DOMAIN_NAME=%q\n' "$DOMAIN_NAME" >> "$tmpcfg"
  printf 'CERTBOT_EMAIL=%q\n' "$CERTBOT_EMAIL" >> "$tmpcfg"
  printf 'SCA_DB_NAME=%q\n' "$SCA_DB_NAME" >> "$tmpcfg"
  printf 'SCA_DB_HOST=%q\n' "$SCA_DB_HOST" >> "$tmpcfg"
  printf 'SCA_DB_PORT=%q\n' "$SCA_DB_PORT" >> "$tmpcfg"
  printf 'SCA_DB_USER=%q\n' "$SCA_DB_USER" >> "$tmpcfg"
  printf 'GOOGLE_CLIENT_ID=%q\n' "$GOOGLE_CLIENT_ID" >> "$tmpcfg"
  printf 'GOOGLE_CLIENT_SECRET=%q\n' "$GOOGLE_CLIENT_SECRET" >> "$tmpcfg"
  printf 'GOOGLE_REDIRECT_URI=%q\n' "$GOOGLE_REDIRECT_URI" >> "$tmpcfg"
  printf 'CTN_VAPID_PUBLIC_KEY=%q\n' "$CTN_VAPID_PUBLIC_KEY" >> "$tmpcfg"
  printf 'CTN_VAPID_PRIVATE_KEY=%q\n' "$CTN_VAPID_PRIVATE_KEY" >> "$tmpcfg"
  install -m 600 "$tmpcfg" "$USER_CONFIG_FILE"
  rm -f "$tmpcfg"
  log_ok "Wrote user config to $USER_CONFIG_FILE"

  # Idempotent alias block in the user's actual shell rc file (zsh or bash)
  local rc_file rc_shell
  rc_shell="$(basename "${SHELL:-bash}")"
  case "$rc_shell" in
    zsh) rc_file="$HOME/.zshrc" ;;
    *)   rc_file="$HOME/.bashrc" ;;
  esac
  local marker_start="# >>> ctn-sca-deploy alias >>>"
  local marker_end="# <<< ctn-sca-deploy alias <<<"
  local tmpbash
  tmpbash="$(mktemp)"
  if [[ -f "$rc_file" ]]; then
    awk -v s="$marker_start" -v e="$marker_end" 'BEGIN{skip=0} $0==s{skip=1; next} $0==e{skip=0; next} !skip{print}' "$rc_file" > "$tmpbash"
  else
    : > "$tmpbash"
  fi
  printf '%s\n' "$marker_start" >> "$tmpbash"
  printf 'alias deploy="%s"\n' "$SCRIPT_DIR/deploy.sh" >> "$tmpbash"
  printf '%s\n' "$marker_end" >> "$tmpbash"
  install -m 644 "$tmpbash" "$rc_file"
  rm -f "$tmpbash"

  echo
  echo "Alias configurado. Corré 'source ${rc_file}' (o abrí una terminal nueva) y después podés usar 'deploy' en vez de './deploy.sh'."
  exit 0
}

FRONTEND_DIR="${FRONTEND_DIR:-$REPO_DIR/frontend}"

# Per-user deploy convenience config (not the runtime env file)
USER_CONFIG_DIR="${USER_CONFIG_DIR:-$HOME/.config/ctn-sca-deploy}"
USER_CONFIG_FILE="${USER_CONFIG_FILE:-$USER_CONFIG_DIR/config.env}"

# If a user config exists, load it but do not overwrite already-exported env vars.
if [[ -f "$USER_CONFIG_FILE" ]]; then
  while IFS= read -r line || [[ -n "$line" ]]; do
    # skip comments and blank lines
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ -z "$line" ]] && continue
    key="${line%%=*}"
    val="${line#*=}"
    # Evaluate the right-hand side which was written with %q to recover proper quoting
    if eval "parsed_val=$val" 2>/dev/null; then
      # only set if variable is not already set in the environment
      if [[ -z "${!key:-}" ]]; then
        printf -v "$key" '%s' "$parsed_val"
        export "$key"
      fi
    fi
  done < "$USER_CONFIG_FILE"
fi

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
SERVICE_NAME="$(echo -n "${SERVICE_NAME}" | xargs)"
DOMAIN_NAME="${DOMAIN_NAME:-}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"
NGINX_SITE_PATH="${NGINX_SITE_PATH:-/etc/nginx/sites-available/${SERVICE_NAME}}"
APP_USER="${APP_USER:-$(id -un)}"
APP_GROUP="${APP_GROUP:-$(id -gn)}"
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

ACTIVITY_LOGS_DIR="${ACTIVITY_LOGS_DIR:-/var/lib/ctn/activity-logs}"
LOG_DIR="${LOG_DIR:-/var/log/ctn-sca-info}"
mkdir -p "$LOG_DIR" 2>/dev/null || true
if [[ ! -w "$LOG_DIR" ]]; then
  LOG_DIR="${HOME}/.local/state/ctn-sca-deploy"
  mkdir -p "$LOG_DIR" 2>/dev/null || true
fi
DEPLOY_LOG="${DEPLOY_LOG:-${LOG_DIR}/deploy.log}"

C_RESET='\033[0m'
C_BOLD='\033[1m'
C_DIM='\033[2m'
C_RED='\033[38;5;196m'
C_GREEN='\033[38;5;77m'
C_YELLOW='\033[38;5;220m'
C_BLUE='\033[38;5;33m'
C_CYAN='\033[38;5;51m'
C_MAGENTA='\033[38;5;201m'
C_ORANGE='\033[38;5;208m'
C_WHITE='\033[38;5;15m'
C_GRAY='\033[38;5;244m'

_log_to_file() { printf "%b\n" "$1" 2>/dev/null >> "$DEPLOY_LOG" || true; }

log()     { printf "%b\n" "  ${C_GRAY}[$(date '+%H:%M:%S')]${C_RESET} $*"; _log_to_file "  [$(date '+%H:%M:%S')] $*"; }
log_ok()  { printf "%b\n" "  ${C_GREEN}✔${C_RESET} $*"; _log_to_file "  OK $*"; }
log_err() { printf "%b\n" "  ${C_RED}✘${C_RESET} $*"; _log_to_file "  ERROR $*"; }
log_warn(){ printf "%b\n" "  ${C_YELLOW}▲${C_RESET} $*"; _log_to_file "  WARN $*"; }
log_info(){ printf "%b\n" "  ${C_CYAN}ℹ${C_RESET} $*"; _log_to_file "  INFO $*"; }

ensure_activity_logs_dir() {
  log_info "Verificando directorio de logs de actividad ($ACTIVITY_LOGS_DIR)..."
  sudo mkdir -p "$ACTIVITY_LOGS_DIR"
  sudo chown "$APP_USER:$APP_GROUP" "$ACTIVITY_LOGS_DIR"
  log_ok "Directorio de logs de actividad listo"
}

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

terminal_cols() {
  local cols="${COLUMNS:-}"
  if [[ -z "$cols" ]]; then
    cols="$(tput cols 2>/dev/null || printf '80')"
  fi
  [[ "$cols" =~ ^[0-9]+$ ]] || cols=80
  printf '%s' "$cols"
}

banner_art_path() {
  local local_path="${SCRIPT_DIR}/ascii-art.html"
  if [[ -f "$local_path" ]]; then
    printf '%s' "$local_path"
    return 0
  fi

  local downloads_path="${HOME}/Descargas/ascii-art.html"
  if [[ -f "$downloads_path" ]]; then
    printf '%s' "$downloads_path"
    return 0
  fi

  return 1
}

render_color_banner() {
  local cols="$1"
  local art_path
  if ! art_path="$(banner_art_path)"; then
    return 1
  fi

  perl - "$cols" "$art_path" <<'PERL'
use strict;
use warnings;

my ($cols, $path) = @ARGV;
$cols = 80 if !defined($cols) || $cols !~ /^\d+$/ || $cols < 1;

open my $fh, '<', $path or die "Unable to read $path: $!";
local $/;
my $html = <$fh>;
close $fh;

$html =~ s{(?is)<style.*?</style>}{}g;
$html =~ s{(?i)<br\s*/?>}{\n}g;
$html =~ s{(?i)<span style="color:#([0-9a-f]{6})">}{
  my $hex = $1;
  my ($r, $g, $b) = map { hex($_) } ($hex =~ /(..)(..)(..)/);
  sprintf("\e[38;2;%d;%d;%dm", $r, $g, $b);
}ge;
$html =~ s{</span>}{\e[0m}gi;
$html =~ s{(?is)<[^>]+>}{}g;

my @lines = split /\n/, $html, -1;
shift @lines while @lines && $lines[0] =~ /^\s*$/;
pop @lines while @lines && $lines[-1] =~ /^\s*$/;

for my $line (@lines) {
  my $plain = $line;
  $plain =~ s/\e\[[0-9;]*m//g;
  my $len = length($plain);
  my $pad = $cols > $len ? int(($cols - $len) / 2) : 0;
  print ' ' x $pad, $line, "\n";
}
PERL
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

# ---------------------------------------------------------------------------
# Requisitos del sistema — detección de gestor de paquetes e instalación
# ---------------------------------------------------------------------------

REQUIRED_BINARIES=(git mvn curl java openssl node npm)

detect_pkg_manager() {
  if command -v pacman >/dev/null 2>&1; then
    printf 'pacman'
  elif command -v apt-get >/dev/null 2>&1; then
    printf 'apt'
  elif command -v dnf >/dev/null 2>&1; then
    printf 'dnf'
  else
    printf 'unknown'
  fi
}

check_requirements_status() {
  local all_ok=true
  local bin ver
  printf "  ${C_BOLD}%-12s %-8s %s${C_RESET}\n" "Herramienta" "Estado" "Versión"
  printf "  ${C_GRAY}%s${C_RESET}\n" "────────────────────────────────────────────"
  for bin in "${REQUIRED_BINARIES[@]}"; do
    if command -v "$bin" >/dev/null 2>&1; then
      case "$bin" in
        git)     ver="$(git --version 2>/dev/null | awk '{print $3}')" ;;
        mvn)     ver="$(mvn -v 2>/dev/null | head -n1 | awk '{print $3}')" ;;
        java)    ver="$(java -version 2>&1 | head -n1 | awk -F'"' '{print $2}')" ;;
        node)    ver="$(node -v 2>/dev/null)" ;;
        npm)     ver="$(npm -v 2>/dev/null)" ;;
        curl)    ver="$(curl --version 2>/dev/null | head -n1 | awk '{print $2}')" ;;
        openssl) ver="$(openssl version 2>/dev/null | awk '{print $2}')" ;;
        *)       ver="" ;;
      esac
      printf "  %-12s ${C_GREEN}%-8s${C_RESET} %s\n" "$bin" "OK" "${ver:-—}"
    else
      printf "  %-12s ${C_RED}%-8s${C_RESET} %s\n" "$bin" "FALTA" "-"
      all_ok=false
    fi
  done
  printf '\n'
  [[ "$all_ok" == true ]]
}

install_requirements() {
  section "🔧  Requisitos del sistema"

  if check_requirements_status; then
    log_ok "Todos los requisitos ya están instalados"
    return 0
  fi

  local pm
  pm="$(detect_pkg_manager)"
  log_warn "Faltan dependencias — instalando con ${C_BOLD}${pm}${C_RESET}${C_YELLOW}..."

  case "$pm" in
    pacman)
      # "mariadb" en Arch incluye servidor (mysqld) + cliente
      sudo pacman -Sy --needed --noconfirm \
        git maven curl jdk-openjdk openssl mariadb nodejs npm
      ;;
    apt)
      sudo apt-get update -y
      sudo apt-get install -y \
        git maven curl default-jdk openssl mariadb-server mariadb-client nodejs npm
      ;;
    dnf)
      sudo dnf install -y \
        git maven curl java-17-openjdk openssl mariadb-server nodejs npm
      ;;
    *)
      log_err "Gestor de paquetes no reconocido; instalá manualmente: ${REQUIRED_BINARIES[*]}"
      return 1
      ;;
  esac

  echo
  if check_requirements_status; then
    log_ok "Requisitos instalados correctamente"
  else
    log_err "Algunos requisitos siguen faltando — revisá la instalación manual"
    return 1
  fi
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
  local persisted_google_client_id=""
  local persisted_google_client_secret=""
  local persisted_google_redirect_uri=""
  if [[ -f "$DB_ENV_FILE" ]]; then
    persisted_jwt_secret="$(read_env_file_value 'JWT_SECRET')"
    persisted_demo_data="$(read_env_file_value 'SCA_LOAD_DEMO_DATA')"
    persisted_vapid_public_key="$(read_env_file_value 'CTN_VAPID_PUBLIC_KEY')"
    persisted_vapid_private_key="$(read_env_file_value 'CTN_VAPID_PRIVATE_KEY')"
    persisted_google_client_id="$(read_env_file_value 'GOOGLE_CLIENT_ID')"
    persisted_google_client_secret="$(read_env_file_value 'GOOGLE_CLIENT_SECRET')"
    persisted_google_redirect_uri="$(read_env_file_value 'GOOGLE_REDIRECT_URI')"
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
  local google_client_id="${GOOGLE_CLIENT_ID:-$persisted_google_client_id}"
  local google_client_secret="${GOOGLE_CLIENT_SECRET:-$persisted_google_client_secret}"
  local google_redirect_uri="${GOOGLE_REDIRECT_URI:-$persisted_google_redirect_uri}"

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
    [[ -n "$google_client_id" ]] && printf 'GOOGLE_CLIENT_ID=%q\n' "$google_client_id"
    [[ -n "$google_client_secret" ]] && printf 'GOOGLE_CLIENT_SECRET=%q\n' "$google_client_secret"
    [[ -n "$google_redirect_uri" ]] && printf 'GOOGLE_REDIRECT_URI=%q\n' "$google_redirect_uri"
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

configure_nginx() {
  require_command sudo
  # Ask for DOMAIN_NAME if not provided and we have a TTY
  if [[ -z "$DOMAIN_NAME" ]]; then
    if [[ ! -t 0 ]]; then
      log_err "DOMAIN_NAME is not set and no TTY available; cannot configure nginx.";
      return 1
    fi
    read -r -p "Dominio público para el sitio (example.com): " DOMAIN_NAME
    DOMAIN_NAME="$(echo -n "$DOMAIN_NAME" | xargs)"
    if [[ -z "$DOMAIN_NAME" ]]; then
      log_err "DOMAIN_NAME required."; return 1
    fi
  fi

  log_info "Configuring nginx for ${DOMAIN_NAME} -> proxy to 127.0.0.1:${APP_PORT}"

  # Install nginx and certbot con el gestor de paquetes que corresponda
  if ! command -v nginx >/dev/null 2>&1 || ! command -v certbot >/dev/null 2>&1; then
    local pm
    pm="$(detect_pkg_manager)"
    log_info "Installing nginx and certbot (${pm})..."
    case "$pm" in
      pacman)
        sudo pacman -Sy --needed --noconfirm nginx certbot certbot-nginx || true
        ;;
      apt)
        sudo apt-get update -y || true
        sudo apt-get install -y nginx python3-certbot-nginx || true
        ;;
      dnf)
        sudo dnf install -y nginx certbot python3-certbot-nginx || true
        ;;
      *)
        log_err "Gestor de paquetes no reconocido; instalá nginx y certbot manualmente."
        ;;
    esac
    sudo systemctl enable --now nginx 2>/dev/null || true
  fi

  # Arch/Fedora no traen sites-available/sites-enabled por defecto (eso es
  # convención de Debian/Ubuntu) — usan /etc/nginx/conf.d/*.conf, incluido
  # directamente por nginx.conf. Detectamos cuál corresponde.
  local site_path="$NGINX_SITE_PATH"
  local use_sites_enabled=true
  if [[ "$site_path" == "/etc/nginx/sites-available/${SERVICE_NAME}" && ! -d /etc/nginx/sites-available ]]; then
    site_path="/etc/nginx/conf.d/${SERVICE_NAME}.conf"
    use_sites_enabled=false
    log_info "Convención Debian (sites-available) no detectada — usando ${site_path}"
  elif [[ "$use_sites_enabled" == true && ! -d /etc/nginx/sites-enabled ]]; then
    # sites-available existe pero sites-enabled no: instalación a medias/no
    # estándar. Si nginx.conf igual lo incluye, lo creamos; si no, caemos a
    # conf.d en vez de dejar que el symlink falle más adelante.
    if sudo grep -qE '^[[:space:]]*include[[:space:]]+/etc/nginx/sites-enabled/' /etc/nginx/nginx.conf 2>/dev/null; then
      log_warn "/etc/nginx/sites-enabled no existe pero nginx.conf lo incluye — creándolo"
      sudo mkdir -p /etc/nginx/sites-enabled
    else
      site_path="/etc/nginx/conf.d/${SERVICE_NAME}.conf"
      use_sites_enabled=false
      log_warn "sites-enabled no existe y nginx.conf no lo incluye — usando ${site_path}"
    fi
  fi

  local server_block tmpfile existing
  tmpfile="$(mktemp)"
  cat > "$tmpfile" <<NGINXCONF
server {
    listen 80;
    server_name ${DOMAIN_NAME};

    location / {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 90;
    }

    access_log /var/log/nginx/${SERVICE_NAME}.access.log;
    error_log /var/log/nginx/${SERVICE_NAME}.error.log;
}
NGINXCONF

  sudo mkdir -p "$(dirname "$site_path")"

  if sudo test -f "$site_path"; then
    existing="$(sudo cat "$site_path")"
    if printf '%s' "$existing" | cmp -s - "$tmpfile" 2>/dev/null; then
      log_ok "Nginx site file at $site_path already up-to-date."
      rm -f "$tmpfile"
    else
      local bak="${site_path}.$(date +%Y%m%d-%H%M%S).bak"
      log_warn "Backing up existing nginx site file to $bak"
      sudo cp -a "$site_path" "$bak"
      sudo install -o root -g root -m 644 "$tmpfile" "$site_path"
      rm -f "$tmpfile"
      log_ok "Wrote new nginx site file to $site_path"
    fi
  else
    sudo install -o root -g root -m 644 "$tmpfile" "$site_path"
    rm -f "$tmpfile"
    log_ok "Created nginx site file at $site_path"
  fi

  # Symlink a sites-enabled solo aplica bajo la convención Debian/Ubuntu
  if [[ "$use_sites_enabled" == true ]] && [[ ! -L "/etc/nginx/sites-enabled/$(basename "$site_path")" ]]; then
    sudo ln -sf "$site_path" "/etc/nginx/sites-enabled/$(basename "$site_path")"
  fi

  # Test and reload nginx
  if sudo nginx -t; then
    sudo systemctl reload nginx || sudo systemctl restart nginx || true
    log_ok "nginx configuration test OK and reloaded"
  else
    log_err "nginx configuration test failed; check /var/log/nginx/error.log"
    return 1
  fi

  # "nginx -t" solo valida sintaxis de lo que YA está incluido; un site file
  # que nadie referencia desde nginx.conf pasa el test igual y nginx nunca lo
  # carga (por eso certbot después no encuentra el server_name). Confirmamos
  # con nginx -T (config activa real) y, si no aparece, agregamos el include
  # que falta en nginx.conf y reintentamos.
  if ! sudo nginx -T 2>/dev/null | grep -qE "server_name[[:space:]]+.*\b${DOMAIN_NAME}\b"; then
    local inc_dir
    if [[ "$use_sites_enabled" == true ]]; then
      inc_dir="/etc/nginx/sites-enabled/*"
    else
      inc_dir="/etc/nginx/conf.d/*.conf"
    fi
    log_warn "$site_path no está siendo cargado por nginx.conf (server_name ${DOMAIN_NAME} ausente en config activa)"
    if sudo grep -qF "include ${inc_dir};" /etc/nginx/nginx.conf 2>/dev/null; then
      log_err "nginx.conf ya incluye ${inc_dir} pero el server_name sigue sin aparecer; revisá manualmente con 'nginx -T'"
      return 1
    fi
    local ncbak="/etc/nginx/nginx.conf.$(date +%Y%m%d-%H%M%S).bak"
    log_warn "Agregando 'include ${inc_dir};' al bloque http de nginx.conf (backup: $ncbak)"
    sudo cp -a /etc/nginx/nginx.conf "$ncbak"
    sudo sed -i "0,/http[[:space:]]*{/s//http {\n    include ${inc_dir};/" /etc/nginx/nginx.conf
    if sudo nginx -t; then
      sudo systemctl reload nginx || sudo systemctl restart nginx || true
      if sudo nginx -T 2>/dev/null | grep -qE "server_name[[:space:]]+.*\b${DOMAIN_NAME}\b"; then
        log_ok "nginx.conf corregido; ${DOMAIN_NAME} ahora está en la config activa"
      else
        log_err "Se agregó el include pero ${DOMAIN_NAME} sigue sin aparecer; revisá nginx.conf manualmente"
        return 1
      fi
    else
      log_err "nginx -t falló tras editar nginx.conf; revirtiendo backup"
      sudo cp -a "$ncbak" /etc/nginx/nginx.conf
      sudo nginx -t && (sudo systemctl reload nginx || sudo systemctl restart nginx || true)
      return 1
    fi
  fi

  # Try to obtain TLS cert if email provided
  if [[ -n "$CERTBOT_EMAIL" ]]; then
    log_info "Requesting Let's Encrypt cert for $DOMAIN_NAME (certbot)"
    sudo certbot --nginx -d "$DOMAIN_NAME" -m "$CERTBOT_EMAIL" --non-interactive --agree-tos --redirect || {
      log_err "certbot failed; certificate not installed"
    }
  else
    log_warn "CERTBOT_EMAIL not set; skipping certbot. Site will remain on HTTP."
  fi
}

service_exists() {
  if ! command -v systemctl >/dev/null 2>&1; then
    return 1
  fi

  # Try to list known unit files and match exact service name. Do not hide stderr
  # so failures are visible to the caller for debugging.
  local list_out
  if list_out="$(systemctl list-unit-files --type=service --no-legend 2>&1)"; then
    printf '%s' "$list_out" | awk '{print $1}' | grep -Fxq "${SERVICE_NAME}.service" && return 0 || true
  else
    # If systemctl itself failed, print the captured error for diagnostics and continue
    printf '%s\n' "$list_out" >&2
  fi

  # Fallback: if a unit file exists on disk, treat the service as existing.
  if sudo test -f "$SERVICE_UNIT_PATH"; then
    return 0
  fi

  return 1
}

ensure_service_exists() {
  if service_exists; then
    if sudo test -f "$SERVICE_UNIT_PATH" && ! sudo grep -q "^User=${APP_USER}$" "$SERVICE_UNIT_PATH" 2>/dev/null; then
      log_warn "El servicio existe pero corre con otro usuario — regenerando la unit para usar '${APP_USER}'"
    else
      return 0
    fi
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
  require_command find
  require_command systemctl
  require_command sudo
  normalize_db_type
  validate_project_layout

  install_requirements || return 1

  require_command git
  require_command mvn
  require_command curl
  require_command java
  require_command openssl

  ensure_service_exists
  ensure_database_initialized || return 1

  section "⬇️   Código fuente"
  log_info "Descargando últimos cambios..."
  git -C "$REPO_DIR" pull --ff-only
  log_ok "Repositorio actualizado"

  configure_service_env
  build_frontend

  section "🏗️   Compilación"
  log_info "Compilando el backend (Spring Boot)..."
  mvn -f "$PROJECT_DIR/pom.xml" clean package -DskipTests
  log_ok "JAR compilado correctamente"

  local built_jar
  built_jar="$(find "$PROJECT_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)"
  if [[ -z "$built_jar" ]]; then
    log_err "No se encontró un .jar ejecutable en $PROJECT_DIR/target"
    return 1
  fi

  section "🚀  Despliegue"
  log_info "Instalando artefacto en $INSTALL_DIR"
  sudo mkdir -p "$INSTALL_DIR"
  ensure_activity_logs_dir
  sudo install -o "$APP_USER" -g "$APP_GROUP" -m 644 "$built_jar" "$INSTALL_DIR/$JAR_NAME"
  sudo systemctl restart "$SERVICE_NAME"
  log_ok "Servicio reiniciado"

  log_info "Esperando a que el servicio quede activo..."
  local waited=0
  while (( waited < STARTUP_TIMEOUT_SECONDS )); do
    if sudo systemctl is-active --quiet "$SERVICE_NAME"; then
      break
    fi
    sleep 1
    waited=$((waited + 1))
  done
  if ! sudo systemctl is-active --quiet "$SERVICE_NAME"; then
    log_err "El servicio no quedó activo dentro de ${STARTUP_TIMEOUT_SECONDS}s"
    sudo systemctl status "$SERVICE_NAME" --no-pager -l || true
    sudo journalctl -u "$SERVICE_NAME" -n 150 --no-pager || true
    return 1
  fi
  log_ok "Servicio activo"

  local attempt=1
  while (( attempt <= HEALTHCHECK_RETRIES )); do
    if curl -fsS --max-time 5 "$APP_URL" >/dev/null; then
      log_ok "Health check OK — sistema actualizado con éxito"
      return 0
    fi
    log_warn "Health check falló (intento ${attempt}/${HEALTHCHECK_RETRIES}), reintentando en ${HEALTHCHECK_DELAY_SECONDS}s..."
    sleep "$HEALTHCHECK_DELAY_SECONDS"
    attempt=$((attempt + 1))
  done

  log_err "Health check falló tras ${HEALTHCHECK_RETRIES} intentos"
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

mariadb_service_name() {
  local list
  list="$(systemctl list-unit-files --type=service --no-legend 2>/dev/null | awk '{print $1}')"
  if grep -Fxq 'mariadb.service' <<<"$list"; then
    printf 'mariadb'
  elif grep -Fxq 'mysqld.service' <<<"$list"; then
    printf 'mysqld'
  else
    printf 'mariadb'
  fi
}

ensure_database_server_running() {
  # Only manage a local server; a remote DB_HOST is someone else's responsibility.
  if [[ "$DB_HOST" != "localhost" && "$DB_HOST" != "127.0.0.1" ]]; then
    return 0
  fi
  if ! command -v systemctl >/dev/null 2>&1; then
    log_warn "systemctl no disponible; se omite el arranque automático del servidor de DB"
    return 0
  fi

  section "🐬  Servidor de base de datos"

  local svc
  svc="$(mariadb_service_name)"

  if ! sudo test -d /var/lib/mysql/mysql; then
    log_warn "Datadir de MariaDB no inicializado — ejecutando mariadb-install-db"
    if command -v mariadb-install-db >/dev/null 2>&1; then
      sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql >/dev/null
      log_ok "Datadir inicializado"
    else
      log_err "mariadb-install-db no encontrado; instalá el paquete del servidor (mariadb / mariadb-server) primero"
      return 1
    fi
  fi

  if ! sudo systemctl is-active --quiet "$svc" 2>/dev/null; then
    log_info "Iniciando servicio ${svc}..."
    sudo systemctl enable --now "$svc"
  fi

  local waited=0
  while (( waited < 20 )); do
    if sudo systemctl is-active --quiet "$svc" 2>/dev/null; then
      log_ok "Servidor de base de datos activo (${svc})"
      return 0
    fi
    sleep 1
    waited=$((waited + 1))
  done

  log_err "El servicio ${svc} no llegó a estar activo a tiempo"
  sudo systemctl status "$svc" --no-pager -l || true
  return 1
}

ensure_database_user_exists() {
  local password client config
  password="$(get_db_password)"
  if [[ -z "$password" && -f "$DB_ENV_FILE" ]]; then
    password="$(read_env_file_value 'SCA_DB_PASSWORD')"
    [[ -z "$password" ]] && password="$(read_env_file_value 'CTN_DB_PASSWORD')"
  fi

  client="$(database_client 2>/dev/null || true)"
  [[ -z "$client" ]] && return 1

  # ¿Ya podemos conectar con el usuario configurado? Si sí, no hay nada que hacer.
  config="$(mktemp)"
  create_database_client_config "$config" "$password"
  if "$client" --defaults-extra-file="$config" -e "SELECT 1;" >/dev/null 2>&1; then
    rm -f "$config"
    return 0
  fi
  rm -f "$config"

  log_warn "El usuario de base de datos '${DB_USER}' no existe todavía — creándolo vía root local"

  if [[ -z "$password" ]]; then
    log_err "No hay contraseña definida para '${DB_USER}'. Definí SCA_DB_PASSWORD o corré el script en modo interactivo."
    return 1
  fi

  if ! sudo "$client" -e \
    "CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${password}';
     GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
     FLUSH PRIVILEGES;" 2>/tmp/ctn-db-user-err; then
    log_err "No se pudo crear el usuario '${DB_USER}' vía root (¿root de MariaDB no usa auth por socket?)"
    cat /tmp/ctn-db-user-err >&2 2>/dev/null || true
    rm -f /tmp/ctn-db-user-err
    return 1
  fi
  rm -f /tmp/ctn-db-user-err
  log_ok "Usuario '${DB_USER}' creado con privilegios sobre '${DB_NAME}'"
}

database_exists() {
  local client config password rc
  client="$(database_client 2>/dev/null || true)"
  [[ -z "$client" ]] && return 1

  password="$(get_db_password)"
  if [[ -z "$password" && -f "$DB_ENV_FILE" ]]; then
    password="$(read_env_file_value 'SCA_DB_PASSWORD')"
    [[ -z "$password" ]] && password="$(read_env_file_value 'CTN_DB_PASSWORD')"
  fi

  config="$(mktemp)"
  create_database_client_config "$config" "$password"
  "$client" --defaults-extra-file="$config" -N -e \
    "SELECT 1 FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='${DB_NAME}';" 2>/dev/null \
    | grep -q '^1$'
  rc=$?
  rm -f "$config"
  return $rc
}

ensure_database_initialized() {
  normalize_db_type
  ensure_database_server_running || return 1
  ensure_database_user_exists || return 1

  section "🗄️   Base de datos"

  local client
  client="$(database_client 2>/dev/null || true)"
  if [[ -z "$client" ]]; then
    log_warn "No hay cliente mysql/mariadb disponible; se omite la verificación de la base de datos"
    return 0
  fi

  if database_exists; then
    log_ok "La base de datos '${DB_NAME}' ya existe"
    return 0
  fi

  log_warn "No se encontró la base de datos '${DB_NAME}' — primera instalación detectada"

  local schema_file="$REPO_DIR/database/db-tables-properties.sql"
  local seed_file="$REPO_DIR/database/ctn-official-seed.sql"
  if [[ ! -f "$schema_file" || ! -f "$seed_file" ]]; then
    log_err "No se encontró el esquema/seed oficial en $REPO_DIR/database"
    return 1
  fi

  log_info "Creando y cargando la base de datos por primera vez..."
  if SCA_CONFIRM_DB_RESET=RESET load_default_database; then
    log_ok "Base de datos '${DB_NAME}' creada y cargada"
  else
    log_err "No se pudo crear/cargar la base de datos"
    return 1
  fi
}

edit_linux_service() {
  require_command systemctl
  require_command sudo
  if ! service_exists; then
    echo "Linux service ${SERVICE_NAME}.service does not exist." >&2
    echo "==> Diagnostic information:" >&2
    echo "SERVICE_NAME='${SERVICE_NAME}'" >&2
    echo "Output of 'systemctl list-unit-files --type=service --no-legend | grep -i sca':" >&2
    systemctl list-unit-files --type=service --no-legend 2>/dev/null | grep -i sca || true
    if sudo test -f "$SERVICE_UNIT_PATH"; then
      echo "Service unit file exists on disk at $SERVICE_UNIT_PATH" >&2
    else
      echo "Service unit file does NOT exist at $SERVICE_UNIT_PATH" >&2
    fi
    if command -v git >/dev/null 2>&1 && [[ -d "$REPO_DIR/.git" ]]; then
      echo "Git status for $REPO_DIR:" >&2
      git -C "$REPO_DIR" status --porcelain 2>/dev/null || true
      echo "Last commit in $REPO_DIR:" >&2
      git -C "$REPO_DIR" log -1 --oneline 2>/dev/null || true
    fi
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
  local cols
  cols="$(terminal_cols)"

  printf '\n'
  if ! render_color_banner "$cols"; then
    printf '%b\n' "${C_ORANGE}${C_BOLD}CTN - SCA${C_RESET}"
    printf '%b\n' "${C_CYAN}Sistema de Carpetas Academicas${C_RESET}"
  fi
  printf '\n'
}

main_menu() {
  while true; do
    printf '\033[2J\033[H'
    show_banner
    cat <<MENU
  1) 🔧 Instalar requisitos del sistema
  2) 🗄️  Inicializar/verificar base de datos (primera instalación)
  3) 🔄 Actualizar sistema (completo: requisitos + DB + build + deploy)
  4) 📦 Cargar base de datos por default
     (db-tables-properties.sql + ctn-official-seed.sql)
  5) ⚙️  Editar servicio Linux
  6) ❤️  Health monitor
  7) 🌐 Configurar nginx + SSL
  8) 🚪 Salir
MENU
    echo
    read -r -p "Select an action [1-8]: " choice
    case "$choice" in
      1) if ! install_requirements; then echo "Requirement installation failed."; fi; pause_menu ;;
      2) if ! ensure_database_initialized; then echo "Database initialization failed."; fi; pause_menu ;;
      3) if ! update_system; then echo "System update failed."; fi; pause_menu ;;
      4) if ! load_default_database; then echo "Database load failed."; fi; pause_menu ;;
      5) if ! edit_linux_service; then echo "Service editor is unavailable."; fi; pause_menu ;;
      6) if ! health_monitor; then echo "Health monitor failed."; pause_menu; fi ;;
      7) if ! configure_nginx; then echo "nginx configuration failed."; fi; pause_menu ;;
      8) echo "Bye."; return 0 ;;
      *) echo "Invalid option."; sleep 1 ;;
    esac
  done
}

show_help() {
  cat <<HELP
Usage: ./deploy.sh [action]

  --menu                  Open the interactive CTN console
  --install-requirements  Detect and install missing system requirements
  --init-db               Create and load the database on first install
                          (no-op if it already exists)
  --update                Full flow: requirements + DB init + build + deploy
  --load-default-db       Replace the database with schema + official seed
  --edit-service          Open the systemd override editor when the service exists
  --health                Start the health monitor (single snapshot without a TTY)
  --configure-nginx       Configure nginx site and obtain TLS via certbot
  --help                  Show this help

Without arguments, an interactive terminal opens the menu. Non-interactive
execution keeps the historical behavior and updates the system (which now
also installs missing requirements and initializes the database on first run).
HELP
}

case "${1:-}" in
  --menu) main_menu ;;
  --install-requirements) install_requirements ;;
  --init-db) ensure_database_initialized ;;
  --update) update_system ;;
  --load-default-db) load_default_database ;;
  --edit-service) edit_linux_service ;;
  --health) health_monitor ;;
  --configure-nginx) configure_nginx ;;
  --help|-h) show_help ;;
  "") if [[ -t 0 ]]; then first_run_wizard ""; main_menu; else update_system; fi ;;
  *) echo "Unknown action: $1" >&2; show_help >&2; exit 2 ;;
esac