#!/usr/bin/env bash
set -Eeuo pipefail

APP_NAME="${APP_NAME:-rent-wise}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/${APP_NAME}}"
JAR_SOURCE="${JAR_SOURCE:-${1:-}}"
DOMAIN="${DOMAIN:-x.com}"
SHARED_DIR="${DEPLOY_DIR}/shared"
RELEASES_DIR="${DEPLOY_DIR}/releases"
LOG_DIR="${DEPLOY_DIR}/logs"
PID_FILE="${DEPLOY_DIR}/${APP_NAME}.pid"
CURRENT_JAR="${DEPLOY_DIR}/current.jar"
ENV_FILE="${ENV_FILE:-${SHARED_DIR}/${APP_NAME}.env}"
JAVA_BIN="${JAVA_BIN:-java}"
PORT="${PORT:-8080}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${PORT}/api/catalog/states}"
JAVA_VERSION="${JAVA_VERSION:-25}"
SIGNAL_CLI_VERSION="${SIGNAL_CLI_VERSION:-0.14.2}"
SIGNAL_CLI_PATH="${SIGNAL_CLI_PATH:-/usr/local/bin/signal-cli}"

log() {
  printf '[deploy] %s\n' "$*"
}

ensure_java_installed() {
  local required_version="${JAVA_VERSION}"
  local installed_version=0

  # Check if Java is installed and get version
  if command -v java &>/dev/null; then
    installed_version=$(java -version 2>&1 | grep -oP '(?<=version ")?\d+' | head -1 || echo "0")
    log "Java version $installed_version is installed"

    if [[ "$installed_version" == "$required_version" ]]; then
      log "Java version $installed_version matches required version $required_version"
      return 0
    fi

    log "Java version $installed_version does not match required version $required_version. Uninstalling..."
    # Uninstall current Java
    if command -v apt-get &>/dev/null; then
      sudo apt-get remove -y "openjdk-*" "*openjdk*" 2>&1 | tail -3 || true
    elif command -v yum &>/dev/null; then
      sudo yum remove -y "java-*openjdk*" 2>&1 | tail -3 || true
    elif command -v dnf &>/dev/null; then
      sudo dnf remove -y "java-*openjdk*" 2>&1 | tail -3 || true
    fi
  fi

  log "Installing Java ${required_version}..."

  # Try apt-get (Debian/Ubuntu)
  if command -v apt-get &>/dev/null; then
    log "Using apt-get to install Java ${required_version}"
    sudo apt-get update -qq
    sudo apt-get install -y "openjdk-${required_version}-jre-headless" 2>&1 | tail -5
    return $?
  fi

  # Try yum (RHEL/CentOS)
  if command -v yum &>/dev/null; then
    log "Using yum to install Java ${required_version}"
    sudo yum install -y "java-${required_version}-openjdk-headless" 2>&1 | tail -5
    return $?
  fi

  # Try dnf (Fedora)
  if command -v dnf &>/dev/null; then
    log "Using dnf to install Java ${required_version}"
    sudo dnf install -y "java-${required_version}-openjdk-headless" 2>&1 | tail -5
    return $?
  fi

  echo "[deploy] ERROR: Java not found and no package manager available (apt-get, yum, or dnf)" >&2
  exit 1
}

ensure_nginx_installed() {
  if command -v nginx &>/dev/null; then
    log "nginx is already installed"
    return 0
  fi

  log "Installing nginx..."

  # Try apt-get (Debian/Ubuntu)
  if command -v apt-get &>/dev/null; then
    log "Using apt-get to install nginx"
    sudo apt-get update -qq
    sudo apt-get install -y nginx 2>&1 | tail -5
    sudo systemctl enable nginx
    return $?
  fi

  # Try yum (RHEL/CentOS)
  if command -v yum &>/dev/null; then
    log "Using yum to install nginx"
    sudo yum install -y nginx 2>&1 | tail -5
    sudo systemctl enable nginx
    return $?
  fi

  # Try dnf (Fedora)
  if command -v dnf &>/dev/null; then
    log "Using dnf to install nginx"
    sudo dnf install -y nginx 2>&1 | tail -5
    sudo systemctl enable nginx
    return $?
  fi

  echo "[deploy] ERROR: nginx not found and no package manager available" >&2
  exit 1
}

ensure_certbot_installed() {
  if command -v certbot &>/dev/null; then
    log "certbot is already installed"
    return 0
  fi

  log "Installing certbot..."

  # Try apt-get (Debian/Ubuntu)
  if command -v apt-get &>/dev/null; then
    log "Using apt-get to install certbot"
    sudo apt-get update -qq
    sudo apt-get install -y certbot python3-certbot-nginx 2>&1 | tail -5
    return $?
  fi

  # Try yum (RHEL/CentOS)
  if command -v yum &>/dev/null; then
    log "Using yum to install certbot"
    sudo yum install -y certbot python3-certbot-nginx 2>&1 | tail -5
    return $?
  fi

  # Try dnf (Fedora)
  if command -v dnf &>/dev/null; then
    log "Using dnf to install certbot"
    sudo dnf install -y certbot python3-certbot-nginx 2>&1 | tail -5
    return $?
  fi

  echo "[deploy] ERROR: certbot not found and no package manager available" >&2
  exit 1
}

ensure_signal_cli_installed() {
  if command -v signal-cli &>/dev/null; then
    log "signal-cli is already installed"
    return 0
  fi

  local version="${SIGNAL_CLI_VERSION}"
  local install_dir="/opt/signal-cli"
  local archive="/tmp/signal-cli-${version}-Linux-native.tar.gz"
  local url="https://github.com/AsamK/signal-cli/releases/download/v${version}/signal-cli-${version}-Linux-native.tar.gz"

  log "Installing signal-cli ${version} from ${url}"
  curl -fL "${url}" -o "${archive}"
  sudo mkdir -p /opt
  sudo tar -xzf "${archive}" -C /opt
  local extracted_dir
  extracted_dir="$(find /opt -maxdepth 1 -type d -name 'signal-cli*' | sort | tail -1)"
  if [[ -z "${extracted_dir}" ]]; then
    echo "[deploy] ERROR: signal-cli archive did not extract a directory under /opt" >&2
    exit 1
  fi
  sudo ln -sfn "${extracted_dir}" "${install_dir}"
  sudo ln -sfn "${install_dir}/bin/signal-cli" "${SIGNAL_CLI_PATH}"
  sudo chmod +x "${SIGNAL_CLI_PATH}"
}

configure_nginx() {
  local domain="${DOMAIN:-x.com}"
  local nginx_conf="/etc/nginx/sites-available/${APP_NAME}"

  log "Configuring nginx for domain ${domain}"

  # Create nginx site configuration
  sudo tee "${nginx_conf}" > /dev/null <<EOF
server {
    listen 80;
    server_name ${domain};

    location / {
        proxy_pass http://127.0.0.1:${PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

  # Enable site
  sudo ln -sf "${nginx_conf}" "/etc/nginx/sites-enabled/${APP_NAME}"
  sudo rm -f /etc/nginx/sites-enabled/default 2>/dev/null || true

  # Test configuration
  if sudo nginx -t; then
    sudo systemctl reload nginx
    log "nginx configured and reloaded"
  else
    echo "[deploy] ERROR: nginx configuration test failed" >&2
    exit 1
  fi
}

configure_ssl() {
  local domain="${DOMAIN:-x.com}"

  log "Obtaining SSL certificate for ${domain}"

  # Check if nginx is running
  if sudo systemctl is-active --quiet nginx; then
    log "nginx is running, stopping it for certbot standalone"
    sudo systemctl stop nginx
    # Wait for port 80 to be free
    sleep 5
  fi

  # Check if certificate already exists
  if sudo certbot certificates 2>/dev/null | grep -q "Certificate Name: ${domain}"; then
    log "SSL certificate for ${domain} already exists, skipping certificate generation"
  else
    # Obtain certificate using standalone
    if sudo certbot certonly --standalone --agree-tos --email admin@${domain} -d ${domain} --non-interactive; then
      log "SSL certificate obtained successfully"
    else
      echo "[deploy] ERROR: Failed to obtain SSL certificate" >&2
      # Restart nginx if it was running
      sudo systemctl start nginx 2>/dev/null || true
      exit 1
    fi
  fi

  # Restart nginx
  sudo systemctl start nginx

  # Configure nginx for HTTPS
  local nginx_conf="/etc/nginx/sites-available/${APP_NAME}"

  sudo tee "${nginx_conf}" > /dev/null <<EOF
server {
    listen 80;
    server_name ${domain};
    return 301 https://\$server_name\$request_uri;
}

server {
    listen 443 ssl http2;
    server_name ${domain};

    ssl_certificate /etc/letsencrypt/live/${domain}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${domain}/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:${PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

  # Test and reload
  if sudo nginx -t; then
    sudo systemctl reload nginx
    log "nginx SSL configured and reloaded"
  else
    echo "[deploy] ERROR: nginx configuration test failed" >&2
    exit 1
  fi
}

tail_application_log() {
  if [[ -f "${LOG_DIR}/application.log" ]]; then
    echo "[deploy] Last 200 lines of ${LOG_DIR}/application.log:" >&2
    tail -n 200 "${LOG_DIR}/application.log" >&2 || true
  else
    echo "[deploy] No application log found at ${LOG_DIR}/application.log" >&2
  fi
}

on_error() {
  local exit_code=$?
  local line_no="$1"
  local command="$2"
  echo "[deploy] ERROR on line ${line_no}: ${command}" >&2
  tail_application_log
  exit "${exit_code}"
}

trap 'on_error "${LINENO}" "${BASH_COMMAND}"' ERR

# Verify sudo is available without password
if ! sudo -n true 2>/dev/null; then
  echo "[deploy] ERROR: sudo access required without password. Please configure passwordless sudo." >&2
  exit 1
fi

if [[ -z "${JAR_SOURCE}" || ! -f "${JAR_SOURCE}" ]]; then
  echo "Usage: JAR_SOURCE=/path/to/rent-wise.jar $0" >&2
  exit 1
fi

# Ensure Java is installed before proceeding
ensure_java_installed

# Ensure nginx is installed
ensure_nginx_installed

# Ensure certbot is installed
ensure_certbot_installed

# Ensure signal-cli is installed for Signal OTP delivery
ensure_signal_cli_installed

mkdir -p "${SHARED_DIR}" "${RELEASES_DIR}" "${LOG_DIR}"
log "Deploying ${APP_NAME} into ${DEPLOY_DIR}"

# Create environment file with secrets
log "Creating environment file with secrets"
cat > "${ENV_FILE}" <<EOF
GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID:-}
GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET:-}
SPRING_MAIL_HOST=${SPRING_MAIL_HOST:-}
SPRING_MAIL_PORT=${SPRING_MAIL_PORT:-587}
SPRING_MAIL_USERNAME=${SPRING_MAIL_USERNAME:-}
SPRING_MAIL_PASSWORD=${SPRING_MAIL_PASSWORD:-}
APP_MAIL_FROM=${APP_MAIL_FROM:-rentwise@zeeroiq.com}
APP_TOTP_ISSUER=${APP_TOTP_ISSUER:-RentWise}
APP_TOTP_PERIOD_SECONDS=${APP_TOTP_PERIOD_SECONDS:-30}
APP_TOTP_DIGITS=${APP_TOTP_DIGITS:-6}
TWILIO_ACCOUNT_SID=${TWILIO_ACCOUNT_SID:-}
TWILIO_AUTH_TOKEN=${TWILIO_AUTH_TOKEN:-}
TWILIO_FROM_NUMBER=${TWILIO_FROM_NUMBER:-}
TELEGRAM_BOT_TOKEN=${TELEGRAM_BOT_TOKEN:-}
SIGNAL_ACCOUNT=${SIGNAL_ACCOUNT:-}
SIGNAL_CLI_PATH=${SIGNAL_CLI_PATH:-/usr/local/bin/signal-cli}
EOF

if [[ -f "${ENV_FILE}" ]]; then
  log "Loading environment from ${ENV_FILE}"
  set -a
  # shellcheck disable=SC1090
  . "${ENV_FILE}"
  set +a
fi

timestamp="$(date +%Y%m%d%H%M%S)"
release_jar="${RELEASES_DIR}/${APP_NAME}-${timestamp}.jar"
cp "${JAR_SOURCE}" "${release_jar}"
ln -sfn "${release_jar}" "${CURRENT_JAR}"
log "Prepared release artifact ${release_jar}"

if [[ -f "${PID_FILE}" ]] && kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
  log "Stopping existing ${APP_NAME} process $(cat "${PID_FILE}")"
  kill "$(cat "${PID_FILE}")"
  for _ in $(seq 1 30); do
    if kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
      sleep 1
    else
      break
    fi
  done

  if kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
    echo "Existing ${APP_NAME} process did not stop cleanly." >&2
    exit 1
  fi
fi

java_opts=()
if [[ -n "${JAVA_OPTS:-}" ]]; then
  # shellcheck disable=SC2206
  java_opts=(${JAVA_OPTS})
fi

log "Starting ${APP_NAME} on port ${PORT} with profile ${SPRING_PROFILES_ACTIVE}"
nohup "${JAVA_BIN}" \
  "${java_opts[@]}" \
  -Dserver.port="${PORT}" \
  -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE}" \
  -Dapp.frontend-base-url="${FRONTEND_BASE_URL:-https://${DOMAIN:-x.com}}" \
  -jar "${CURRENT_JAR}" >> "${LOG_DIR}/application.log" 2>&1 &

echo $! > "${PID_FILE}"

for _ in $(seq 1 30); do
  if curl -fsS "${HEALTH_URL}" >/dev/null; then
    log "Deployment complete. ${APP_NAME} is healthy at ${HEALTH_URL}"
    # Configure nginx reverse proxy
    configure_nginx
    # Configure SSL
    configure_ssl
    log "App is now accessible at https://${DOMAIN:-x.com}"
    exit 0
  fi
  sleep 2
done

echo "Application started, but health check failed at ${HEALTH_URL}" >&2
tail_application_log
exit 1
