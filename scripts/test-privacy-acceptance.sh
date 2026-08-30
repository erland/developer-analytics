#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-deploy/compose.yaml}"
LOCAL_BUILD_COMPOSE="${LOCAL_BUILD_COMPOSE:-deploy/compose.local-build.yaml}"
PROJECT_NAME="${PRIVACY_PROJECT_NAME:-developer-analytics-privacy-acceptance}"
BASE_URL="${PRIVACY_BASE_URL:-http://localhost:${WEB_PORT:-18088}}"
DB_NAME="${DB_NAME:-developer_analytics}"
DB_USERNAME="${DB_USERNAME:-developer_analytics}"

USER_A="88000000-0000-0000-0000-000000000001"
USER_B="88000000-0000-0000-0000-000000000002"
IDENTITY_A="88000000-0000-0000-0000-000000000011"
IDENTITY_B="88000000-0000-0000-0000-000000000012"
SESSION_A_ID="88000000-0000-0000-0000-000000000021"
SESSION_B_ID="88000000-0000-0000-0000-000000000022"
PUBLIC_REPO="88000000-0000-0001-0000-000000000001"
PRIVATE_REPO="88000000-0000-0001-0000-000000000002"
EXCLUDED_PRIVATE_REPO="88000000-0000-0001-0000-000000000003"
OTHER_REPO="88000000-0000-0002-0000-000000000001"
SESSION_A="${PRIVACY_SESSION_A:-privacy-user-a-session}"
SESSION_B="${PRIVACY_SESSION_B:-privacy-user-b-session}"

PUBLIC_NAME="public-oss-visible"
PRIVATE_NAME="private-secret-project"
EXCLUDED_NAME="excluded-private-ultra-secret"
OTHER_NAME="other-user-private-project"
PRIVATE_AI_SECRET="PRIVATE_AI_ONLY_INTERNAL_PATTERN_88"

compose() {
  docker compose -p "${PROJECT_NAME}" -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" "$@"
}

cleanup() {
  compose down -v --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

fail() {
  echo "Privacy acceptance test failed: $*" >&2
  compose ps >&2 || true
  compose logs --no-color db backend worker web >&2 || true
  exit 1
}

expect_absent() {
  local needle="$1"
  local file="$2"
  if grep -F "${needle}" "${file}" >/dev/null; then
    echo "Unexpected private marker '${needle}' found in ${file}" >&2
    sed -n '1,220p' "${file}" >&2 || true
    fail "private information crossed a privacy boundary"
  fi
}

expect_present() {
  local needle="$1"
  local file="$2"
  grep -F "${needle}" "${file}" >/dev/null \
    || fail "expected marker '${needle}' missing from ${file}"
}

echo "Starting privacy acceptance stack..."
cleanup
compose up -d --build db backend worker web

for _ in $(seq 1 90); do
  if curl --fail --silent "${BASE_URL}/api/health/application" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
curl --fail --silent "${BASE_URL}/api/health/application" >/dev/null \
  || fail "application did not become ready"

HASH_A="$(python3 scripts/hash-auth-token.py "${SESSION_A}")"
HASH_B="$(python3 scripts/hash-auth-token.py "${SESSION_B}")"

echo "Seeding two users, public/private/excluded repositories and private AI analysis..."
compose exec -T db psql \
  --username="${DB_USERNAME}" \
  --dbname="${DB_NAME}" \
  --set=ON_ERROR_STOP=1 \
  --set=hash_a="${HASH_A}" \
  --set=hash_b="${HASH_B}" <<SQL
DELETE FROM app_user WHERE id IN ('${USER_A}'::uuid,'${USER_B}'::uuid);

INSERT INTO app_user(id) VALUES ('${USER_A}'::uuid),('${USER_B}'::uuid);

INSERT INTO provider_identity(id,user_id,provider,external_user_id,login,display_name)
VALUES
('${IDENTITY_A}'::uuid,'${USER_A}'::uuid,'github','privacy-a','privacy-a','Privacy User A'),
('${IDENTITY_B}'::uuid,'${USER_B}'::uuid,'github','privacy-b','privacy-b','Privacy User B');

INSERT INTO user_session(id,user_id,token_hash,expires_at)
VALUES
('${SESSION_A_ID}'::uuid,'${USER_A}'::uuid,:'hash_a',CURRENT_TIMESTAMP + INTERVAL '2 hours'),
('${SESSION_B_ID}'::uuid,'${USER_B}'::uuid,:'hash_b',CURRENT_TIMESTAMP + INTERVAL '2 hours');

INSERT INTO source_repository(
  id,user_id,provider,external_repository_id,owner_external_id,owner_login,
  owner_type,ownership_relation,name,full_name,html_url,description,topics,
  visibility,is_fork,is_archived,first_activity_at,last_activity_at,
  sync_status,discovered_at,last_seen_at,included_in_analysis
) VALUES
('${PUBLIC_REPO}'::uuid,'${USER_A}'::uuid,'github','privacy-public','a','privacy-a',
 'USER','OWNED_BY_USER','${PUBLIC_NAME}','privacy-a/${PUBLIC_NAME}',
 'https://example.invalid/${PUBLIC_NAME}','public repository','[]'::jsonb,
 'PUBLIC',false,false,CURRENT_TIMESTAMP-INTERVAL '100 days',CURRENT_TIMESTAMP,
 'SYNCED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,true),
('${PRIVATE_REPO}'::uuid,'${USER_A}'::uuid,'github','privacy-private','a','privacy-a',
 'USER','OWNED_BY_USER','${PRIVATE_NAME}','privacy-a/${PRIVATE_NAME}',
 'https://example.invalid/${PRIVATE_NAME}','included private repository','[]'::jsonb,
 'PRIVATE',false,false,CURRENT_TIMESTAMP-INTERVAL '90 days',CURRENT_TIMESTAMP,
 'SYNCED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,true),
('${EXCLUDED_PRIVATE_REPO}'::uuid,'${USER_A}'::uuid,'github','privacy-excluded','a','privacy-a',
 'USER','OWNED_BY_USER','${EXCLUDED_NAME}','privacy-a/${EXCLUDED_NAME}',
 'https://example.invalid/${EXCLUDED_NAME}','excluded private repository','[]'::jsonb,
 'PRIVATE',false,false,CURRENT_TIMESTAMP-INTERVAL '80 days',CURRENT_TIMESTAMP,
 'SYNCED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,false),
('${OTHER_REPO}'::uuid,'${USER_B}'::uuid,'github','privacy-other','b','privacy-b',
 'USER','OWNED_BY_USER','${OTHER_NAME}','privacy-b/${OTHER_NAME}',
 'https://example.invalid/${OTHER_NAME}','other user private repository','[]'::jsonb,
 'PRIVATE',false,false,CURRENT_TIMESTAMP-INTERVAL '70 days',CURRENT_TIMESTAMP,
 'SYNCED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,true);

INSERT INTO contribution(
 id,user_id,source_repository_id,provider,provider_contribution_id,
 contribution_type,title,occurred_at,state
) VALUES
('88000000-0000-0010-0000-000000000001'::uuid,'${USER_A}'::uuid,'${PUBLIC_REPO}'::uuid,
 'github','privacy-c-public','COMMIT','public commit',CURRENT_TIMESTAMP-INTERVAL '2 days','UNKNOWN'),
('88000000-0000-0010-0000-000000000002'::uuid,'${USER_A}'::uuid,'${PRIVATE_REPO}'::uuid,
 'github','privacy-c-private','COMMIT','private commit',CURRENT_TIMESTAMP-INTERVAL '1 day','UNKNOWN'),
('88000000-0000-0010-0000-000000000003'::uuid,'${USER_A}'::uuid,'${EXCLUDED_PRIVATE_REPO}'::uuid,
 'github','privacy-c-excluded','COMMIT','excluded secret commit',CURRENT_TIMESTAMP,'UNKNOWN');

INSERT INTO project_significance_assessment(
 id,user_id,repository_id,
 significance_level,significance_score,popularity_score,contributor_score,
 longevity_score,ecosystem_score,activity_score,significance_rationale,
 involvement_level,involvement_score,contribution_score,involvement_duration_score,
 involvement_recency_score,relative_contribution_score,involvement_rationale,
 calculated_at,privacy_provenance
) VALUES
('88000000-0000-0020-0000-000000000001'::uuid,'${USER_A}'::uuid,'${PUBLIC_REPO}'::uuid,
 'HIGH',80,10,10,10,10,10,'{}'::jsonb,'HIGH',80,10,10,10,10,'{}'::jsonb,
 CURRENT_TIMESTAMP,'PUBLIC_ONLY'),
('88000000-0000-0020-0000-000000000002'::uuid,'${USER_A}'::uuid,'${PRIVATE_REPO}'::uuid,
 'HIGH',90,10,10,10,10,10,'{}'::jsonb,'HIGH',90,10,10,10,10,'{}'::jsonb,
 CURRENT_TIMESTAMP,'PRIVATE_AGGREGATE'),
('88000000-0000-0020-0000-000000000003'::uuid,'${USER_A}'::uuid,'${EXCLUDED_PRIVATE_REPO}'::uuid,
 'HIGH',99,10,10,10,10,10,'{}'::jsonb,'HIGH',99,10,10,10,10,'{}'::jsonb,
 CURRENT_TIMESTAMP,'PRIVATE_AGGREGATE');

INSERT INTO user_ai_insight(
 id,user_id,input_fingerprint,likely_roles,technical_focus,
 breadth_depth_observation,technology_evolution_summary,
 open_source_engagement_summary,analysis_version,provider_id,model_id,
 privacy_provenance,created_at
) VALUES (
 '88000000-0000-0030-0000-000000000001'::uuid,
 '${USER_A}'::uuid,
 repeat('8',64),
 '[{"role":"Internal platform engineer","confidence":0.91,"rationale":"${PRIVATE_AI_SECRET}"}]'::jsonb,
 '${PRIVATE_AI_SECRET}',
 'private breadth signal',
 'private evolution signal',
 'private engagement signal',
 'v1','acceptance-ai','acceptance-model','PRIVATE_AGGREGATE',CURRENT_TIMESTAMP
);
SQL

echo "Creating external GPT tokens through the normal authenticated API..."
create_token() {
  local privacy_scope="$1"
  local name="$2"
  curl --fail --silent --show-error \
    --cookie "developer_analytics_session=${SESSION_A}" \
    -H 'Content-Type: application/json' \
    -X POST "${BASE_URL}/api/me/external-clients" \
    --data "{\"name\":\"${name}\",\"scopes\":[\"PROFILE_READ\",\"PROJECTS_READ\",\"ACTIVITY_READ\",\"CONTRIBUTIONS_READ\"],\"privacyScope\":\"${privacy_scope}\"}"
}

create_token PUBLIC_ONLY privacy-public-client > /tmp/privacy-token-public.json
create_token PUBLIC_PLUS_PRIVATE_AGGREGATES privacy-aggregate-client > /tmp/privacy-token-aggregate.json
create_token FULL_AUTHORISED_ANALYSIS privacy-full-client > /tmp/privacy-token-full.json

PUBLIC_TOKEN="$(python3 -c 'import json; print(json.load(open("/tmp/privacy-token-public.json"))["token"])')"
AGG_TOKEN="$(python3 -c 'import json; print(json.load(open("/tmp/privacy-token-aggregate.json"))["token"])')"
FULL_TOKEN="$(python3 -c 'import json; print(json.load(open("/tmp/privacy-token-full.json"))["token"])')"

echo "1/7 Another user cannot read user A repository detail..."
status="$(
  curl --silent --output /tmp/privacy-other-user.json --write-out '%{http_code}' \
    --cookie "developer_analytics_session=${SESSION_B}" \
    "${BASE_URL}/api/me/projects/${PRIVATE_REPO}"
)"
[[ "${status}" == "404" ]] || fail "cross-user repository lookup returned HTTP ${status}"
expect_absent "${PRIVATE_NAME}" /tmp/privacy-other-user.json

echo "2/7 Public report cannot expose private repositories or private AI..."
curl --fail --silent --show-error \
  --cookie "developer_analytics_session=${SESSION_A}" \
  -H 'Content-Type: application/json' \
  -X POST "${BASE_URL}/api/me/reports/export" \
  --data '{"outputFormat":"MARKDOWN","reportType":"PUBLIC_OSS_REPORT","privateDataMode":"INCLUDE_FULL_PRIVATE_DETAIL","hidePrivateRepositoryNames":false,"generationConfirmed":true}' \
  > /tmp/privacy-public-report.md

expect_present "${PUBLIC_NAME}" /tmp/privacy-public-report.md
expect_absent "${PRIVATE_NAME}" /tmp/privacy-public-report.md
expect_absent "${EXCLUDED_NAME}" /tmp/privacy-public-report.md
expect_absent "${PRIVATE_AI_SECRET}" /tmp/privacy-public-report.md

echo "3/7 Private report may include authorised private data but never excluded private data..."
curl --fail --silent --show-error \
  --cookie "developer_analytics_session=${SESSION_A}" \
  -H 'Content-Type: application/json' \
  -X POST "${BASE_URL}/api/me/reports/export" \
  --data '{"outputFormat":"MARKDOWN","reportType":"FULL_DEVELOPER_REPORT","privateDataMode":"INCLUDE_FULL_PRIVATE_DETAIL","hidePrivateRepositoryNames":false,"generationConfirmed":true}' \
  > /tmp/privacy-private-report.md

expect_present "${PUBLIC_NAME}" /tmp/privacy-private-report.md
expect_present "${PRIVATE_NAME}" /tmp/privacy-private-report.md
expect_present "${PRIVATE_AI_SECRET}" /tmp/privacy-private-report.md
expect_absent "${EXCLUDED_NAME}" /tmp/privacy-private-report.md
expect_absent "${OTHER_NAME}" /tmp/privacy-private-report.md

echo "4/7 Unauthenticated/public API path does not expose user data..."
status="$(
  curl --silent --output /tmp/privacy-no-token.json --write-out '%{http_code}' \
    -H 'Accept: application/vnd.developer-analytics.analysis.v1+json' \
    "${BASE_URL}/api/me/projects"
)"
[[ "${status}" == "401" ]] || fail "external projects without token returned HTTP ${status}"
expect_absent "${PRIVATE_NAME}" /tmp/privacy-no-token.json

echo "5/7 PUBLIC_ONLY GPT token receives public projects only..."
curl --fail --silent --show-error \
  -H 'Accept: application/vnd.developer-analytics.analysis.v1+json' \
  -H "Authorization: Bearer ${PUBLIC_TOKEN}" \
  "${BASE_URL}/api/me/projects?limit=50" > /tmp/privacy-gpt-public.json

expect_present "${PUBLIC_NAME}" /tmp/privacy-gpt-public.json
expect_absent "${PRIVATE_NAME}" /tmp/privacy-gpt-public.json
expect_absent "${EXCLUDED_NAME}" /tmp/privacy-gpt-public.json

echo "6/7 Aggregate-only GPT token still cannot receive private project detail..."
curl --fail --silent --show-error \
  -H 'Accept: application/vnd.developer-analytics.analysis.v1+json' \
  -H "Authorization: Bearer ${AGG_TOKEN}" \
  "${BASE_URL}/api/me/projects?limit=50" > /tmp/privacy-gpt-aggregate-projects.json

expect_present "${PUBLIC_NAME}" /tmp/privacy-gpt-aggregate-projects.json
expect_absent "${PRIVATE_NAME}" /tmp/privacy-gpt-aggregate-projects.json
expect_absent "${EXCLUDED_NAME}" /tmp/privacy-gpt-aggregate-projects.json

curl --fail --silent --show-error \
  -H 'Accept: application/vnd.developer-analytics.analysis.v1+json' \
  -H "Authorization: Bearer ${AGG_TOKEN}" \
  "${BASE_URL}/api/me/profile" > /tmp/privacy-gpt-aggregate-profile.json

python3 - /tmp/privacy-gpt-aggregate-profile.json <<'PY'
import json,sys
payload=json.load(open(sys.argv[1], encoding="utf-8"))
assert payload["privateRepositoryCount"] == 1
assert payload["repositoryCount"] == 2
PY
expect_absent "${PRIVATE_NAME}" /tmp/privacy-gpt-aggregate-profile.json
expect_absent "${EXCLUDED_NAME}" /tmp/privacy-gpt-aggregate-profile.json

echo "7/7 FULL_AUTHORISED_ANALYSIS token gets included private detail, never excluded private..."
curl --fail --silent --show-error \
  -H 'Accept: application/vnd.developer-analytics.analysis.v1+json' \
  -H "Authorization: Bearer ${FULL_TOKEN}" \
  "${BASE_URL}/api/me/projects?limit=50" > /tmp/privacy-gpt-full.json

expect_present "${PUBLIC_NAME}" /tmp/privacy-gpt-full.json
expect_present "${PRIVATE_NAME}" /tmp/privacy-gpt-full.json
expect_absent "${EXCLUDED_NAME}" /tmp/privacy-gpt-full.json
expect_absent "${OTHER_NAME}" /tmp/privacy-gpt-full.json

echo "Privacy v1 acceptance test passed:"
echo "- cross-user isolation held"
echo "- public export excluded private repositories and private AI"
echo "- private export included only explicitly included private data"
echo "- unauthenticated external API exposed no user data"
echo "- PUBLIC_ONLY and aggregate-only GPT tokens could not access private project detail"
echo "- fully authorised GPT token saw included private detail but never excluded private data"
