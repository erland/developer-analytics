#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-deploy/compose.yaml}"
LOCAL_BUILD_COMPOSE="${LOCAL_BUILD_COMPOSE:-deploy/compose.local-build.yaml}"
PROJECT_NAME="${LARGE_ACCOUNT_PROJECT:-developer-analytics-large-account}"
BASE_URL="${LARGE_ACCOUNT_BASE_URL:-http://localhost:${WEB_PORT:-18087}}"
DB_NAME="${DB_NAME:-developer_analytics}"
DB_USERNAME="${DB_USERNAME:-developer_analytics}"
REPOSITORY_COUNT="${LARGE_ACCOUNT_REPOSITORY_COUNT:-240}"
SESSION_TOKEN="${LARGE_ACCOUNT_SESSION_TOKEN:-developer-analytics-large-account-session}"

USER_ID="87000000-0000-0000-0000-000000000001"
IDENTITY_ID="87000000-0000-0000-0000-000000000002"
CONNECTION_ID="87000000-0000-0000-0000-000000000003"
SESSION_ID="87000000-0000-0000-0000-000000000004"
SYNC_RUN_ID="87000000-0000-0000-0000-000000000005"
STALE_JOB_ID="87000000-0000-0000-0000-000000000006"

compose() {
  docker compose \
    -p "${PROJECT_NAME}" \
    -f "${COMPOSE_FILE}" \
    -f "${LOCAL_BUILD_COMPOSE}" \
    "$@"
}

cleanup() {
  compose down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

fail() {
  echo "Large-account acceptance test failed: $*" >&2
  compose ps >&2 || true
  compose logs --no-color db backend worker web >&2 || true
  exit 1
}

timed_get() {
  local url="$1"
  local output="$2"
  local max_seconds="${3:-5}"
  local elapsed
  elapsed="$(
    curl --fail --silent --show-error \
      --cookie "developer_analytics_session=${SESSION_TOKEN}" \
      --output "${output}" \
      --write-out '%{time_total}' \
      "${url}"
  )"
  python3 - "${elapsed}" "${max_seconds}" <<'PY'
import sys
elapsed=float(sys.argv[1]); limit=float(sys.argv[2])
if elapsed > limit:
    raise SystemExit(f"API request took {elapsed:.3f}s, limit is {limit:.3f}s")
PY
}

echo "Starting large-account acceptance stack..."
cleanup
compose up -d --build db backend worker web

echo "Waiting for application..."
for _ in $(seq 1 90); do
  if curl --fail --silent "${BASE_URL}/api/health/application" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
curl --fail --silent "${BASE_URL}/api/health/application" >/dev/null \
  || fail "application did not become ready"

token_hash="$(python3 scripts/hash-auth-token.py "${SESSION_TOKEN}")"

echo "Seeding ${REPOSITORY_COUNT} realistic repositories with partial enrichment..."
compose exec -T db psql \
  --username="${DB_USERNAME}" \
  --dbname="${DB_NAME}" \
  --set=ON_ERROR_STOP=1 \
  --set=token_hash="${token_hash}" \
  --set=repo_count="${REPOSITORY_COUNT}" <<SQL
DELETE FROM app_user WHERE id='${USER_ID}'::uuid;

INSERT INTO app_user(id)
VALUES ('${USER_ID}'::uuid);

INSERT INTO provider_identity(
  id,user_id,provider,external_user_id,login,display_name
) VALUES (
  '${IDENTITY_ID}'::uuid,
  '${USER_ID}'::uuid,
  'github',
  'large-account-user',
  'large-account',
  'Large Account Acceptance'
);

INSERT INTO provider_connection(
  id,user_id,provider_identity_id,provider,status,connected_at,
  private_repository_access
) VALUES (
  '${CONNECTION_ID}'::uuid,
  '${USER_ID}'::uuid,
  '${IDENTITY_ID}'::uuid,
  'github',
  'CONNECTED',
  CURRENT_TIMESTAMP,
  'AUTHORISED'
);

INSERT INTO user_session(
  id,user_id,token_hash,expires_at
) VALUES (
  '${SESSION_ID}'::uuid,
  '${USER_ID}'::uuid,
  :'token_hash',
  CURRENT_TIMESTAMP + INTERVAL '2 hours'
);

INSERT INTO source_repository(
  id,user_id,provider,external_repository_id,
  owner_external_id,owner_login,owner_type,ownership_relation,
  name,full_name,html_url,description,topics,visibility,
  is_fork,is_archived,first_activity_at,last_activity_at,
  sync_status,last_synced_at,discovered_at,last_seen_at,
  included_in_analysis
)
SELECT
  ('87000000-0000-0001-0000-' || lpad(to_hex(n),12,'0'))::uuid,
  '${USER_ID}'::uuid,
  'github',
  'large-' || n,
  'large-owner',
  CASE WHEN n % 5 = 0 THEN 'external-org' ELSE 'large-account' END,
  CASE WHEN n % 5 = 0 THEN 'ORGANIZATION' ELSE 'USER' END,
  CASE WHEN n % 5 = 0 THEN 'CONTRIBUTED_TO' ELSE 'OWNED_BY_USER' END,
  CASE
    WHEN n % 20 = 0 THEN 'search-target-' || lpad(n::text,3,'0')
    ELSE 'repository-' || lpad(n::text,3,'0')
  END,
  CASE
    WHEN n % 5 = 0 THEN 'external-org/repository-' || lpad(n::text,3,'0')
    ELSE 'large-account/repository-' || lpad(n::text,3,'0')
  END,
  'https://github.example.invalid/repository-' || n,
  CASE
    WHEN n % 3 = 0 THEN 'Partially enriched repository with measured activity'
    ELSE 'Discovered repository awaiting enrichment'
  END,
  '[]'::jsonb,
  CASE WHEN n % 10 = 0 THEN 'PRIVATE' ELSE 'PUBLIC' END,
  (n % 7 = 0),
  (n % 17 = 0),
  CURRENT_TIMESTAMP - ((700 + n) || ' days')::interval,
  CURRENT_TIMESTAMP - ((n % 900) || ' days')::interval,
  CASE
    WHEN n <= 40 THEN 'SYNCED'
    WHEN n <= 120 THEN 'SYNCING'
    ELSE 'NOT_SYNCED'
  END,
  CASE WHEN n <= 40 THEN CURRENT_TIMESTAMP - INTERVAL '1 hour' ELSE NULL END,
  CURRENT_TIMESTAMP - INTERVAL '2 hours',
  CURRENT_TIMESTAMP - INTERVAL '5 minutes',
  CASE WHEN n % 10 = 0 THEN FALSE ELSE TRUE END
FROM generate_series(1, :repo_count::int) AS n;

INSERT INTO repository_sync_run(
  id,user_id,provider,status,repositories_seen,repositories_created,
  repositories_updated,pages_processed,rate_limit_remaining,
  rate_limit_reset_at,started_at,completed_at,last_error
) VALUES (
  '${SYNC_RUN_ID}'::uuid,
  '${USER_ID}'::uuid,
  'github',
  'RATE_LIMITED',
  :repo_count::int,
  :repo_count::int,
  0,
  CEIL(:repo_count::numeric / 100)::int,
  0,
  CURRENT_TIMESTAMP + INTERVAL '15 minutes',
  CURRENT_TIMESTAMP - INTERVAL '10 minutes',
  CURRENT_TIMESTAMP,
  'GitHub rate limit reached; retry after reset'
);

INSERT INTO background_job(
  id,user_id,job_type,status,priority,payload,
  attempt_count,max_attempts,next_execution_at,
  locked_at,locked_by,last_error,deduplication_key
) VALUES (
  '${STALE_JOB_ID}'::uuid,
  '${USER_ID}'::uuid,
  'ACCEPTANCE_RECOVERY_SENTINEL',
  'RUNNING',
  100,
  '{"provider":"acceptance"}'::jsonb,
  1,
  3,
  CURRENT_TIMESTAMP - INTERVAL '20 minutes',
  CURRENT_TIMESTAMP - INTERVAL '20 minutes',
  'worker-before-restart',
  NULL,
  'large-account-recovery-sentinel'
);
SQL

echo "1/8 Repository discovery remains usable..."
timed_get "${BASE_URL}/api/me/project-inventory?page=0&pageSize=25" /tmp/large-page0.json
python3 - /tmp/large-page0.json "${REPOSITORY_COUNT}" <<'PY'
import json, math, sys
payload=json.load(open(sys.argv[1], encoding="utf-8"))
total=int(sys.argv[2]) - int(sys.argv[2])//10
assert payload["total"] == total
assert payload["page"] == 0
assert payload["pageSize"] == 25
assert payload["totalPages"] == math.ceil(total/25)
assert len(payload["items"]) == 25
PY

echo "2/8 Initial analysis progresses incrementally and dashboard/API work before enrichment completes..."
timed_get "${BASE_URL}/api/me/activity?period=all" /tmp/large-activity.json
timed_get "${BASE_URL}/api/me/technologies" /tmp/large-technologies.json
html="$(curl --fail --silent --show-error "${BASE_URL}/")"
grep -q "<title>Developer Analytics</title>" <<<"${html}" \
  || fail "frontend did not load during partial enrichment"

sync_counts="$(
  compose exec -T db psql \
    --username="${DB_USERNAME}" \
    --dbname="${DB_NAME}" \
    --tuples-only --no-align \
    --command="
      SELECT
        count(*) FILTER (WHERE sync_status='SYNCED') || ':' ||
        count(*) FILTER (WHERE sync_status='SYNCING') || ':' ||
        count(*) FILTER (WHERE sync_status='NOT_SYNCED')
      FROM source_repository
      WHERE user_id='${USER_ID}'::uuid;"
)"
[[ "${sync_counts}" == "40:80:$((REPOSITORY_COUNT-120))" ]] \
  || fail "partial enrichment fixture was not preserved: ${sync_counts}"

echo "3/8 API remains responsive under hundreds of repositories..."
for page in 0 1 2 3 4; do
  timed_get "${BASE_URL}/api/me/project-inventory?page=${page}&pageSize=25" "/tmp/large-page-${page}.json" 5
done

echo "4/8 UI inventory remains bounded by pagination..."
python3 - /tmp/large-page-0.json /tmp/large-page-1.json <<'PY'
import json,sys
first=json.load(open(sys.argv[1], encoding="utf-8"))
second=json.load(open(sys.argv[2], encoding="utf-8"))
assert len(first["items"]) <= 25
assert len(second["items"]) <= 25
ids1={x["id"] for x in first["items"]}
ids2={x["id"] for x in second["items"]}
assert ids1.isdisjoint(ids2)
PY

echo "5/8 Pagination and filtering work..."
timed_get "${BASE_URL}/api/me/project-inventory?page=0&pageSize=10&search=search-target" /tmp/large-search.json
timed_get "${BASE_URL}/api/me/project-inventory?page=0&pageSize=20&ownership=external" /tmp/large-external.json
timed_get "${BASE_URL}/api/me/project-inventory?page=0&pageSize=20&visibility=public" /tmp/large-public.json
python3 - /tmp/large-search.json /tmp/large-external.json /tmp/large-public.json <<'PY'
import json,sys
search=json.load(open(sys.argv[1], encoding="utf-8"))
external=json.load(open(sys.argv[2], encoding="utf-8"))
public=json.load(open(sys.argv[3], encoding="utf-8"))
assert search["total"] > 0
assert all("search-target" in item["name"] for item in search["items"])
assert external["total"] > 0
assert all(item["ownershipRelation"] != "OWNED_BY_USER" for item in external["items"])
assert public["total"] > 0
assert all(item["visibility"] == "PUBLIC" for item in public["items"])
PY

echo "6/8 Background jobs recover after worker restart..."
compose restart worker >/dev/null
for _ in $(seq 1 45); do
  state="$(
    compose exec -T db psql \
      --username="${DB_USERNAME}" \
      --dbname="${DB_NAME}" \
      --tuples-only --no-align \
      --command="
        SELECT status || ':' || coalesce(locked_by,'')
        FROM background_job
        WHERE id='${STALE_JOB_ID}'::uuid;"
  )"
  if [[ "${state}" != RUNNING:* ]]; then
    break
  fi
  sleep 2
done
[[ "${state}" != RUNNING:* ]] \
  || fail "stale RUNNING job was not recovered after worker restart"

echo "7/8 Rate limiting is surfaced cleanly..."
timed_get "${BASE_URL}/api/me/sync-runs" /tmp/large-sync-runs.json
python3 - /tmp/large-sync-runs.json <<'PY'
import json,sys
runs=json.load(open(sys.argv[1], encoding="utf-8"))
rate=[r for r in runs if r["status"]=="RATE_LIMITED"]
assert rate, "RATE_LIMITED run missing"
item=rate[0]
assert item["rateLimitRemaining"] == 0
assert item["rateLimitResetAt"] is not None
assert "rate limit" in (item["lastError"] or "").lower()
PY

echo "8/8 Large-account data remains queryable after worker restart..."
timed_get "${BASE_URL}/api/me/project-inventory?page=8&pageSize=25" /tmp/large-post-restart.json
python3 - /tmp/large-post-restart.json <<'PY'
import json,sys
payload=json.load(open(sys.argv[1], encoding="utf-8"))
assert payload["page"] == 8
assert len(payload["items"]) > 0
PY

echo "Large-account v1 acceptance test passed:"
echo "- ${REPOSITORY_COUNT} repositories seeded"
echo "- partial enrichment remained usable"
echo "- paged/filterable inventory remained responsive"
echo "- frontend loaded before enrichment completion"
echo "- stale job recovered after worker restart"
echo "- rate-limit state was exposed with reset metadata"
