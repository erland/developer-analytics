#!/usr/bin/env bash
set -euo pipefail

BASE_COMPOSE="${BASE_COMPOSE:-deploy/compose.yaml}"
RUNTIME_COMPOSE="${RUNTIME_COMPOSE:-deploy/compose.ci-runtime.yaml}"
PROJECT_NAME="${RUNTIME_PROJECT_NAME:-developer-analytics-runtime-smoke}"
BASE_URL="${RUNTIME_BASE_URL:-http://localhost:${WEB_PORT:-18081}}"

compose() {
  docker compose \
    -p "${PROJECT_NAME}" \
    -f "${BASE_COMPOSE}" \
    -f "${RUNTIME_COMPOSE}" \
    "$@"
}

cleanup() {
  compose down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

fail() {
  echo "Container runtime smoke test failed: $*" >&2
  compose ps >&2 || true
  compose logs --no-color db backend worker web >&2 || true
  exit 1
}

echo "Verifying that CI-built images exist locally..."
docker image inspect developer-analytics-web:ci >/dev/null \
  || fail "developer-analytics-web:ci is not loaded"
docker image inspect developer-analytics-backend:ci >/dev/null \
  || fail "developer-analytics-backend:ci is not loaded"

echo "Starting Compose stack from pre-built images only..."
cleanup
compose up -d --no-build db backend worker web

echo "Waiting for web and backend health..."
for _ in $(seq 1 90); do
  web_health="$(
    compose ps --format json web 2>/dev/null \
      | python3 -c 'import json,sys; rows=[json.loads(x) for x in sys.stdin if x.strip()]; print(rows[0].get("Health","") if rows else "")' \
      || true
  )"
  backend_health="$(
    compose ps --format json backend 2>/dev/null \
      | python3 -c 'import json,sys; rows=[json.loads(x) for x in sys.stdin if x.strip()]; print(rows[0].get("Health","") if rows else "")' \
      || true
  )"

  if [[ "${web_health}" == "healthy" && "${backend_health}" == "healthy" ]]; then
    break
  fi
  sleep 2
done

[[ "$(compose ps --format json web | python3 -c 'import json,sys; rows=[json.loads(x) for x in sys.stdin if x.strip()]; print(rows[0].get("Health","") if rows else "")')" == "healthy" ]] \
  || fail "web container did not become healthy"
[[ "$(compose ps --format json backend | python3 -c 'import json,sys; rows=[json.loads(x) for x in sys.stdin if x.strip()]; print(rows[0].get("Health","") if rows else "")')" == "healthy" ]] \
  || fail "backend container did not become healthy"

echo "Checking application endpoint through the runtime web image..."
health="$(
  curl --fail --silent --show-error \
    "${BASE_URL}/api/health/application"
)"
python3 - "${health}" <<'PY'
import json, sys
payload = json.loads(sys.argv[1])
assert payload["service"] == "developer-analytics-backend"
assert payload["database"] == "UP"
PY

echo "Checking static frontend response from the runtime web image..."
html="$(curl --fail --silent --show-error "${BASE_URL}/")"
grep -q "<title>Developer Analytics</title>" <<<"${html}" \
  || fail "unexpected frontend HTML"

echo "Checking that all expected services are running..."
for service in db backend worker web; do
  running="$(
    compose ps --status running --services \
      | grep -x "${service}" || true
  )"
  [[ "${running}" == "${service}" ]] \
    || fail "${service} is not running"
done

echo "Stopping Compose stack cleanly..."
compose down -v --remove-orphans
trap - EXIT

remaining="$(
  docker ps -a \
    --filter "label=com.docker.compose.project=${PROJECT_NAME}" \
    --format '{{.ID}}'
)"
[[ -z "${remaining}" ]] \
  || fail "containers remained after docker compose down"

echo "Container runtime smoke verification passed:"
echo "- CI-built images were used with --no-build"
echo "- web and backend became healthy"
echo "- application API responded through Nginx"
echo "- frontend static HTML responded"
echo "- db/backend/worker/web were running"
echo "- Compose stack shut down cleanly"
