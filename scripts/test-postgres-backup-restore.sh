#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-deploy/compose.yaml}"
LOCAL_BUILD_COMPOSE="${LOCAL_BUILD_COMPOSE:-deploy/compose.local-build.yaml}"
DB_NAME="${DB_NAME:-developer_analytics}"
DB_USERNAME="${DB_USERNAME:-developer_analytics}"
RESTORE_DB="${RESTORE_DB:-developer_analytics_restore_test}"

tmp="$(mktemp -d)"
trap 'rm -rf "${tmp}"' EXIT

echo "Waiting for migrated source database..."
for _ in $(seq 1 90); do
  if docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db \
       psql --username="${DB_USERNAME}" --dbname="${DB_NAME}" \
       --tuples-only --no-align \
       --command="SELECT to_regclass('public.user_ai_insight') IS NOT NULL;" \
       2>/dev/null | grep -qx 't'; then
    break
  fi
  sleep 2
done

docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db \
  psql --username="${DB_USERNAME}" --dbname="${DB_NAME}" \
  --set=ON_ERROR_STOP=1 <<'SQL'
DO $$
DECLARE
  u UUID := '74000000-0000-0000-0000-000000000001';
  r UUID := '74000000-0000-0000-0000-000000000002';
  a UUID := '74000000-0000-0000-0000-000000000003';
BEGIN
  DELETE FROM app_user WHERE id=u;

  INSERT INTO app_user(id) VALUES (u);

  INSERT INTO provider_identity(
    id,user_id,provider,external_user_id,login,display_name
  ) VALUES (
    '74000000-0000-0000-0000-000000000010',
    u,'github','backup-test-user','backup-test','Backup Test'
  );

  INSERT INTO source_repository(
    id,user_id,provider,external_repository_id,owner_login,
    owner_type,ownership_relation,name,full_name,visibility,
    is_fork,is_archived,sync_status,included_in_analysis
  ) VALUES (
    r,u,'github','backup-test-repo','backup-test',
    'USER','OWNED_BY_USER','backup-repository',
    'backup-test/backup-repository','PUBLIC',
    false,false,'SYNCED',true
  );

  INSERT INTO user_activity_month(
    id,user_id,year_month,commit_count,active_repository_count
  ) VALUES (
    '74000000-0000-0000-0000-000000000020',
    u,DATE '2026-08-01',17,1
  );

  INSERT INTO repository_activity_month(
    id,user_id,source_repository_id,year_month,commit_count
  ) VALUES (
    '74000000-0000-0000-0000-000000000021',
    u,r,DATE '2026-08-01',17
  );

  INSERT INTO user_ai_insight(
    id,user_id,input_fingerprint,likely_roles,
    technical_focus,breadth_depth_observation,
    technology_evolution_summary,open_source_engagement_summary,
    analysis_version,provider_id,model_id,privacy_provenance,created_at
  ) VALUES (
    a,u,
    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    '[{"role":"Backend developer","confidence":0.91,"rationale":"backup test"}]'::jsonb,
    'Java and backend systems',
    'Broad with backend depth',
    'Stable Java usage',
    'Regular OSS activity',
    'backup-test-v1','gemini','backup-test-model',
    'PUBLIC_ONLY',CURRENT_TIMESTAMP
  );
END $$;
SQL

backup="${tmp}/backup.dump"
docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db \
  pg_dump \
    --username="${DB_USERNAME}" \
    --dbname="${DB_NAME}" \
    --format=custom \
    --no-owner \
    --no-privileges > "${backup}"

docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db \
  dropdb --username="${DB_USERNAME}" --if-exists "${RESTORE_DB}"
docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db \
  createdb --username="${DB_USERNAME}" "${RESTORE_DB}"

cat "${backup}" | docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db \
  pg_restore \
    --username="${DB_USERNAME}" \
    --dbname="${RESTORE_DB}" \
    --no-owner \
    --no-privileges \
    --exit-on-error

assert_sql() {
  local sql="$1"
  local expected="$2"
  local actual
  actual="$(docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db \
    psql --username="${DB_USERNAME}" --dbname="${RESTORE_DB}" \
    --tuples-only --no-align --set=ON_ERROR_STOP=1 --command="${sql}")"
  if [[ "${actual}" != "${expected}" ]]; then
    echo "Assertion failed." >&2
    echo "SQL: ${sql}" >&2
    echo "Expected: ${expected}" >&2
    echo "Actual: ${actual}" >&2
    exit 1
  fi
}

user="'74000000-0000-0000-0000-000000000001'::uuid"
repo="'74000000-0000-0000-0000-000000000002'::uuid"

assert_sql "SELECT count(*) FROM app_user WHERE id=${user};" "1"
assert_sql "SELECT name FROM source_repository WHERE id=${repo};" "backup-repository"
assert_sql "SELECT commit_count FROM user_activity_month WHERE user_id=${user} AND year_month=DATE '2026-08-01';" "17"
assert_sql "SELECT technical_focus FROM user_ai_insight WHERE user_id=${user};" "Java and backend systems"
assert_sql "SELECT count(*) FROM flyway_schema_history WHERE success=true;" \
  "$(docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db psql --username="${DB_USERNAME}" --dbname="${DB_NAME}" --tuples-only --no-align --command="SELECT count(*) FROM flyway_schema_history WHERE success=true;")"

docker compose -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" exec -T db \
  dropdb --username="${DB_USERNAME}" --if-exists "${RESTORE_DB}"

echo "Backup/restore verification passed:"
echo "- account restored"
echo "- repository inventory restored"
echo "- aggregates restored"
echo "- AI analysis metadata restored"
