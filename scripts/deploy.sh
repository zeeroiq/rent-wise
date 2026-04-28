#!/usr/bin/env bash
set -Eeuo pipefail

APP_NAME="${APP_NAME:-rent-wise}"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/${APP_NAME}}"
JAR_SOURCE="${JAR_SOURCE:-${1:-}}"
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

if [[ -z "${JAR_SOURCE}" || ! -f "${JAR_SOURCE}" ]]; then
  echo "Usage: JAR_SOURCE=/path/to/rent-wise.jar $0" >&2
  exit 1
fi

# Ensure Java is installed before proceeding
ensure_java_installed

mkdir -p "${SHARED_DIR}" "${RELEASES_DIR}" "${LOG_DIR}"
log "Deploying ${APP_NAME} into ${DEPLOY_DIR}"

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
  -Dapp.frontend-base-url="${FRONTEND_BASE_URL:-http://127.0.0.1:${PORT}}" \
  -jar "${CURRENT_JAR}" >> "${LOG_DIR}/application.log" 2>&1 &

echo $! > "${PID_FILE}"

for _ in $(seq 1 30); do
  if curl -fsS "${HEALTH_URL}" >/dev/null; then
    log "Deployment complete. ${APP_NAME} is healthy at ${HEALTH_URL}"
    exit 0
  fi
  sleep 2
done

echo "Application started, but health check failed at ${HEALTH_URL}" >&2
tail_application_log
exit 1
