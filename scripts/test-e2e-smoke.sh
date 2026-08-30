#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-deploy/compose.yaml}"
LOCAL_BUILD_COMPOSE="${LOCAL_BUILD_COMPOSE:-deploy/compose.local-build.yaml}"
BASE_URL="${E2E_BASE_URL:-http://localhost:${WEB_PORT:-8080}}"
DB_NAME="${DB_NAME:-developer_analytics}"
DB_USERNAME="${DB_USERNAME:-developer_analytics}"
SESSION_TOKEN="${E2E_SESSION_TOKEN:-developer-analytics-e2e-session}"
USER_ID="77000000-0000-0000-0000-000000000001"
IDENTITY_ID="77000000-0000-0000-0000-000000000002"
SESSION_ID="77000000-0000-0000-0000-000000000003"

fail() {
  echo "E2E smoke test failed: $*" >&2
  exit 1
}

wait_for_url() {
  local url="$1"
  local attempts="${2:-90}"
  for _ in $(seq 1 "${attempts}"); do
    if curl --fail --silent --show-error "${url}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  fail "Timed out waiting for ${url}"
}

echo "Waiting for frontend through Nginx..."
wait_for_url "${BASE_URL}/"

html="$(curl --fail --silent --show-error "${BASE_URL}/")"
grep -q "<title>Developer Analytics</title>" <<<"${html}" \
  || fail "Frontend HTML did not contain the expected title"

echo "Verifying API proxy through Nginx..."
health="$(curl --fail --silent --show-error "${BASE_URL}/api/health/application")"
python3 - "${health}" <<'PY'
import json, sys
payload = json.loads(sys.argv[1])
if payload.get("service") != "developer-analytics-backend":
    raise SystemExit("Unexpected backend health payload")
if payload.get("database") != "UP":
    raise SystemExit("Database is not UP through application health")
PY

echo "Verifying Flyway migration state in PostgreSQL..."
migration_count="$(
  docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db \
    psql --username="${DB_USERNAME}" --dbname="${DB_NAME}" \
    --tuples-only --no-align --set=ON_ERROR_STOP=1 \
    --command="SELECT count(*) FROM flyway_schema_history WHERE success=true;"
)"
[[ "${migration_count}" =~ ^[1-9][0-9]*$ ]] \
  || fail "No successful Flyway migrations were found"

failed_count="$(
  docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db \
    psql --username="${DB_USERNAME}" --dbname="${DB_NAME}" \
    --tuples-only --no-align --set=ON_ERROR_STOP=1 \
    --command="SELECT count(*) FROM flyway_schema_history WHERE success=false;"
)"
[[ "${failed_count}" == "0" ]] \
  || fail "Flyway contains failed migrations"

echo "Creating deterministic authenticated smoke-test session..."
token_hash="$(python3 scripts/hash-auth-token.py "${SESSION_TOKEN}")"

docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db \
  psql --username="${DB_USERNAME}" --dbname="${DB_NAME}" \
  --set=ON_ERROR_STOP=1 --set=token_hash="${token_hash}" <<SQL
DELETE FROM app_user WHERE id='${USER_ID}'::uuid;

INSERT INTO app_user(id)
VALUES ('${USER_ID}'::uuid);

INSERT INTO provider_identity(
  id,user_id,provider,external_user_id,login,display_name
) VALUES (
  '${IDENTITY_ID}'::uuid,
  '${USER_ID}'::uuid,
  'github',
  'e2e-smoke-user',
  'e2e-smoke',
  'E2E Smoke User'
);

INSERT INTO user_session(
  id,user_id,token_hash,expires_at
) VALUES (
  '${SESSION_ID}'::uuid,
  '${USER_ID}'::uuid,
  :'token_hash',
  CURRENT_TIMESTAMP + INTERVAL '30 minutes'
);
SQL

echo "Verifying representative authenticated flow through Nginx..."
session="$(
  curl --fail --silent --show-error \
    --cookie "developer_analytics_session=${SESSION_TOKEN}" \
    "${BASE_URL}/api/auth/session"
)"
python3 - "${session}" <<'PY'
import json, sys
payload = json.loads(sys.argv[1])
assert payload["authenticated"] is True
assert payload["provider"] == "github"
assert payload["login"] == "e2e-smoke"
assert payload["displayName"] == "E2E Smoke User"
PY

me="$(
  curl --fail --silent --show-error \
    --cookie "developer_analytics_session=${SESSION_TOKEN}" \
    "${BASE_URL}/api/me"
)"
python3 - "${me}" <<'PY'
import json, sys
payload = json.loads(sys.argv[1])
assert payload["provider"] == "github"
assert payload["login"] == "e2e-smoke"
assert payload["displayName"] == "E2E Smoke User"
PY

echo "E2E smoke verification passed:"
echo "- frontend loaded through Nginx"
echo "- backend API responded through Nginx"
echo "- PostgreSQL/Flyway migrations succeeded (${migration_count} migrations)"
echo "- authenticated session flow succeeded without live GitHub"
