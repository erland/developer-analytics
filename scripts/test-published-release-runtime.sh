#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-}"
if [[ -z "${VERSION}" ]]; then
  echo "Usage: bash ./scripts/test-published-release-runtime.sh <release-version>" >&2
  exit 2
fi

VERSION="${VERSION#v}"
OWNER="${GHCR_OWNER:-erland}"
WEB_IMAGE="${WEB_IMAGE:-ghcr.io/${OWNER}/developer-analytics-web}"
BACKEND_IMAGE="${BACKEND_IMAGE:-ghcr.io/${OWNER}/developer-analytics-backend}"
COMPOSE_FILE="${RELEASE_COMPOSE_FILE:-deploy/compose.release.example.yaml}"
PROJECT_NAME="${RELEASE_VERIFY_PROJECT:-developer-analytics-release-verify}"
WEB_PORT="${WEB_PORT:-18082}"
BASE_URL="${RELEASE_VERIFY_BASE_URL:-http://localhost:${WEB_PORT}}"

export APP_VERSION="${VERSION}"
export WEB_IMAGE
export BACKEND_IMAGE
export WEB_PORT

# Runtime-only deterministic configuration. No live GitHub calls are made.
export DB_NAME="${DB_NAME:-developer_analytics}"
export DB_USERNAME="${DB_USERNAME:-developer_analytics}"
export DB_PASSWORD="${DB_PASSWORD:-release-verify-db-password}"
export CREDENTIAL_ENCRYPTION_KEY="${CREDENTIAL_ENCRYPTION_KEY:-AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=}"
export CREDENTIAL_KEY_VERSION="${CREDENTIAL_KEY_VERSION:-release-verify-v1}"
export GITHUB_CLIENT_ID="${GITHUB_CLIENT_ID:-release-verify-client-id}"
export GITHUB_CLIENT_SECRET="${GITHUB_CLIENT_SECRET:-release-verify-client-secret}"
export GITHUB_CALLBACK_URL="${GITHUB_CALLBACK_URL:-http://localhost:${WEB_PORT}/api/auth/github/callback}"
export FRONTEND_URL="${FRONTEND_URL:-http://localhost:${WEB_PORT}/}"
export SESSION_COOKIE_SECURE="${SESSION_COOKIE_SECURE:-false}"
export AI_PROVIDER="${AI_PROVIDER:-disabled}"
export GEMINI_API_KEY="${GEMINI_API_KEY:-}"
export AI_PRIVATE_DATA_POLICY="${AI_PRIVATE_DATA_POLICY:-PUBLIC_ONLY}"

compose() {
  docker compose -p "${PROJECT_NAME}" -f "${COMPOSE_FILE}" "$@"
}

cleanup() {
  compose down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

fail() {
  echo "Published release runtime verification failed: $*" >&2
  compose ps >&2 || true
  compose logs --no-color db backend worker web >&2 || true
  exit 1
}

echo "Ensuring published release images are present..."
docker image inspect "${WEB_IMAGE}:${VERSION}" >/dev/null \
  || docker pull "${WEB_IMAGE}:${VERSION}" >/dev/null \
  || fail "cannot obtain ${WEB_IMAGE}:${VERSION}"
docker image inspect "${BACKEND_IMAGE}:${VERSION}" >/dev/null \
  || docker pull "${BACKEND_IMAGE}:${VERSION}" >/dev/null \
  || fail "cannot obtain ${BACKEND_IMAGE}:${VERSION}"

echo "Starting release Compose using published images only..."
cleanup
compose up -d --no-build db backend worker web

echo "Waiting for release services to become healthy/running..."
for _ in $(seq 1 90); do
  web_health="$(
    compose ps --format json web 2>/dev/null \
      | python3 -c 'import json,sys; rows=[json.loads(line) for line in sys.stdin if line.strip()]; print(rows[0].get("Health","") if rows else "")' \
      || true
  )"
  backend_health="$(
    compose ps --format json backend 2>/dev/null \
      | python3 -c 'import json,sys; rows=[json.loads(line) for line in sys.stdin if line.strip()]; print(rows[0].get("Health","") if rows else "")' \
      || true
  )"
  worker_running="$(
    compose ps --status running --services 2>/dev/null \
      | grep -x worker || true
  )"

  if [[ "${web_health}" == "healthy" &&
        "${backend_health}" == "healthy" &&
        "${worker_running}" == "worker" ]]; then
    break
  fi

  sleep 2
done

[[ "$(
  compose ps --format json web \
    | python3 -c 'import json,sys; rows=[json.loads(line) for line in sys.stdin if line.strip()]; print(rows[0].get("Health","") if rows else "")'
)" == "healthy" ]] || fail "published web image did not become healthy"

[[ "$(
  compose ps --format json backend \
    | python3 -c 'import json,sys; rows=[json.loads(line) for line in sys.stdin if line.strip()]; print(rows[0].get("Health","") if rows else "")'
)" == "healthy" ]] || fail "published backend image did not become healthy"

[[ "$(compose ps --status running --services | grep -x worker || true)" == "worker" ]] \
  || fail "worker using published backend image is not running"

echo "Checking frontend/static response..."
html="$(curl --fail --silent --show-error "${BASE_URL}/")"
grep -q "<title>Developer Analytics</title>" <<<"${html}" \
  || fail "release frontend response is unexpected"

echo "Checking application API through published web image/Nginx..."
health="$(curl --fail --silent --show-error "${BASE_URL}/api/health/application")"
python3 - "${health}" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])
if payload.get("service") != "developer-analytics-backend":
    raise SystemExit("unexpected application health service")
if payload.get("database") != "UP":
    raise SystemExit("database is not UP")
PY

echo "Checking Flyway migrations in release database..."
failed_migrations="$(
  compose exec -T db \
    psql \
      --username="${DB_USERNAME}" \
      --dbname="${DB_NAME}" \
      --tuples-only \
      --no-align \
      --set=ON_ERROR_STOP=1 \
      --command="SELECT count(*) FROM flyway_schema_history WHERE success=false;"
)"
[[ "${failed_migrations}" == "0" ]] \
  || fail "release startup produced failed Flyway migrations"

migration_count="$(
  compose exec -T db \
    psql \
      --username="${DB_USERNAME}" \
      --dbname="${DB_NAME}" \
      --tuples-only \
      --no-align \
      --set=ON_ERROR_STOP=1 \
      --command="SELECT count(*) FROM flyway_schema_history WHERE success=true;"
)"
[[ "${migration_count}" =~ ^[1-9][0-9]*$ ]] \
  || fail "release database has no successful Flyway migrations"

echo "Verifying actual running image identities..."
backend_running="$(
  compose ps --format json backend \
    | python3 -c 'import json,sys; rows=[json.loads(line) for line in sys.stdin if line.strip()]; print(rows[0].get("Image","") if rows else "")'
)"
worker_running_image="$(
  compose ps --format json worker \
    | python3 -c 'import json,sys; rows=[json.loads(line) for line in sys.stdin if line.strip()]; print(rows[0].get("Image","") if rows else "")'
)"
web_running="$(
  compose ps --format json web \
    | python3 -c 'import json,sys; rows=[json.loads(line) for line in sys.stdin if line.strip()]; print(rows[0].get("Image","") if rows else "")'
)"

[[ "${backend_running}" == "${BACKEND_IMAGE}:${VERSION}" ]] \
  || fail "backend is not running the published release image"
[[ "${worker_running_image}" == "${BACKEND_IMAGE}:${VERSION}" ]] \
  || fail "worker is not running the published backend release image"
[[ "${web_running}" == "${WEB_IMAGE}:${VERSION}" ]] \
  || fail "web is not running the published release image"

echo "Stopping verified release deployment cleanly..."
compose down -v --remove-orphans
trap - EXIT

remaining="$(
  docker ps -a \
    --filter "label=com.docker.compose.project=${PROJECT_NAME}" \
    --format '{{.ID}}'
)"
[[ -z "${remaining}" ]] \
  || fail "release verification containers remained after shutdown"

echo "Published release runtime verification passed:"
echo "- published web/backend tags started without rebuilding"
echo "- backend and web became healthy"
echo "- worker started from the backend release image"
echo "- frontend and API responded through the release Nginx path"
echo "- Flyway migrations completed successfully"
echo "- release Compose shut down cleanly"
