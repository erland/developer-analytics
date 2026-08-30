#!/usr/bin/env bash
set -euo pipefail

TAG="${1:-}"
if [[ -z "${TAG}" ]]; then
  echo "Usage: bash ./scripts/verify-release-candidate-images.sh <candidate-tag>" >&2
  exit 2
fi

OWNER="${GHCR_OWNER:-erland}"
WEB_IMAGE="${WEB_IMAGE:-ghcr.io/${OWNER}/developer-analytics-web}"
BACKEND_IMAGE="${BACKEND_IMAGE:-ghcr.io/${OWNER}/developer-analytics-backend}"
COMPOSE_FILE="${RELEASE_COMPOSE_FILE:-deploy/compose.release.example.yaml}"
PROJECT_NAME="${RC_VERIFY_PROJECT:-developer-analytics-rc-verify}"
WEB_PORT="${WEB_PORT:-18090}"
BASE_URL="${RC_VERIFY_BASE_URL:-http://localhost:${WEB_PORT}}"

fail() {
  echo "Release-candidate image verification failed: $*" >&2
  exit 1
}

echo "Clearing local candidate images and logging out of GHCR..."
docker image rm -f "${WEB_IMAGE}:${TAG}" "${BACKEND_IMAGE}:${TAG}" >/dev/null 2>&1 || true
docker logout ghcr.io >/dev/null 2>&1 || true

echo "Verifying clean anonymous pulls..."
docker pull "${WEB_IMAGE}:${TAG}" >/dev/null \
  || fail "cannot anonymously pull ${WEB_IMAGE}:${TAG}"
docker pull "${BACKEND_IMAGE}:${TAG}" >/dev/null \
  || fail "cannot anonymously pull ${BACKEND_IMAGE}:${TAG}"

export APP_VERSION="${TAG}"
export WEB_IMAGE
export BACKEND_IMAGE
export WEB_PORT
export DB_NAME="${DB_NAME:-developer_analytics}"
export DB_USERNAME="${DB_USERNAME:-developer_analytics}"
export DB_PASSWORD="${DB_PASSWORD:-rc-verify-db-password}"
export CREDENTIAL_ENCRYPTION_KEY="${CREDENTIAL_ENCRYPTION_KEY:-AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=}"
export CREDENTIAL_KEY_VERSION="${CREDENTIAL_KEY_VERSION:-rc-v1}"
export GITHUB_CLIENT_ID="${GITHUB_CLIENT_ID:-rc-client-id}"
export GITHUB_CLIENT_SECRET="${GITHUB_CLIENT_SECRET:-rc-client-secret}"
export GITHUB_CALLBACK_URL="${GITHUB_CALLBACK_URL:-http://localhost:${WEB_PORT}/api/auth/github/callback}"
export FRONTEND_URL="${FRONTEND_URL:-http://localhost:${WEB_PORT}/}"
export SESSION_COOKIE_SECURE=false
export AI_PROVIDER=disabled
export GEMINI_API_KEY=
export AI_PRIVATE_DATA_POLICY=PUBLIC_ONLY

compose() {
  docker compose -p "${PROJECT_NAME}" -f "${COMPOSE_FILE}" "$@"
}
cleanup() {
  compose down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Starting candidate deployment from published images only..."
cleanup
compose up -d --no-build db backend worker web

for _ in $(seq 1 90); do
  if curl --fail --silent "${BASE_URL}/api/health/application" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

curl --fail --silent "${BASE_URL}/api/health/application" >/tmp/rc-health.json \
  || {
    compose logs --no-color db backend worker web >&2 || true
    fail "candidate deployment did not become healthy"
  }

python3 - /tmp/rc-health.json <<'PY'
import json,sys
payload=json.load(open(sys.argv[1],encoding="utf-8"))
assert payload["service"]=="developer-analytics-backend"
assert payload["database"]=="UP"
PY

html="$(curl --fail --silent "${BASE_URL}/")"
grep -q "<title>Developer Analytics</title>" <<<"${html}" \
  || fail "candidate frontend did not serve expected application"

backend_image="$(compose ps --format json backend | python3 -c 'import json,sys; rows=[json.loads(x) for x in sys.stdin if x.strip()]; print(rows[0].get("Image","") if rows else "")')"
worker_image="$(compose ps --format json worker | python3 -c 'import json,sys; rows=[json.loads(x) for x in sys.stdin if x.strip()]; print(rows[0].get("Image","") if rows else "")')"
web_image="$(compose ps --format json web | python3 -c 'import json,sys; rows=[json.loads(x) for x in sys.stdin if x.strip()]; print(rows[0].get("Image","") if rows else "")')"

[[ "${backend_image}" == "${BACKEND_IMAGE}:${TAG}" ]] || fail "backend tag mismatch"
[[ "${worker_image}" == "${BACKEND_IMAGE}:${TAG}" ]] || fail "worker is not using candidate backend image"
[[ "${web_image}" == "${WEB_IMAGE}:${TAG}" ]] || fail "web tag mismatch"

failed_migrations="$(
  compose exec -T db psql \
    --username="${DB_USERNAME}" --dbname="${DB_NAME}" \
    --tuples-only --no-align --set=ON_ERROR_STOP=1 \
    --command="SELECT count(*) FROM flyway_schema_history WHERE success=false;"
)"
[[ "${failed_migrations}" == "0" ]] || fail "candidate has failed Flyway migrations"

echo "Candidate published-image verification passed."
