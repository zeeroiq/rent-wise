#!/usr/bin/env bash
set -euo pipefail

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

if [[ -z "${JAR_SOURCE}" || ! -f "${JAR_SOURCE}" ]]; then
  echo "Usage: JAR_SOURCE=/path/to/rent-wise.jar $0" >&2
  exit 1
fi

mkdir -p "${SHARED_DIR}" "${RELEASES_DIR}" "${LOG_DIR}"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  . "${ENV_FILE}"
  set +a
fi

timestamp="$(date +%Y%m%d%H%M%S)"
release_jar="${RELEASES_DIR}/${APP_NAME}-${timestamp}.jar"
cp "${JAR_SOURCE}" "${release_jar}"
ln -sfn "${release_jar}" "${CURRENT_JAR}"

if [[ -f "${PID_FILE}" ]] && kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
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

nohup "${JAVA_BIN}" \
  "${java_opts[@]}" \
  -Dserver.port="${PORT}" \
  -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE}" \
  -Dapp.frontend-base-url="${FRONTEND_BASE_URL:-http://127.0.0.1:${PORT}}" \
  -jar "${CURRENT_JAR}" >> "${LOG_DIR}/application.log" 2>&1 &

echo $! > "${PID_FILE}"

for _ in $(seq 1 30); do
  if curl -fsS "${HEALTH_URL}" >/dev/null; then
    echo "Deployment complete. ${APP_NAME} is healthy at ${HEALTH_URL}"
    exit 0
  fi
  sleep 2
done

echo "Application started, but health check failed at ${HEALTH_URL}" >&2
exit 1
