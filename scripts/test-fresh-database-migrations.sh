#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-deploy/compose.yaml}"
LOCAL_BUILD_COMPOSE="${LOCAL_BUILD_COMPOSE:-deploy/compose.local-build.yaml}"
DB_NAME="${DB_NAME:-developer_analytics}"
DB_USERNAME="${DB_USERNAME:-developer_analytics}"
PROJECT_NAME="${MIGRATION_COMPOSE_PROJECT:-developer-analytics-migration-test}"

compose() {
  docker compose -p "${PROJECT_NAME}" -f "${COMPOSE_FILE}" -f "${LOCAL_BUILD_COMPOSE}" "$@"
}

cleanup() {
  compose down -v >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Starting an empty PostgreSQL volume and backend..."
cleanup
compose up -d --build db backend

echo "Waiting for backend readiness..."
for _ in $(seq 1 90); do
  if compose exec -T backend \
      bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

compose exec -T backend \
  bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' >/dev/null 2>&1 \
  || {
    echo "Backend did not start successfully on a fresh database." >&2
    compose logs --no-color db backend >&2
    exit 1
  }

echo "Checking Flyway history..."
history="$(
  compose exec -T db \
    psql \
      --username="${DB_USERNAME}" \
      --dbname="${DB_NAME}" \
      --tuples-only \
      --no-align \
      --set=ON_ERROR_STOP=1 \
      --command="SELECT version || ':' || success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank;"
)"

[[ -n "${history}" ]] || {
  echo "Flyway history is empty after backend startup." >&2
  exit 1
}

if grep -q ':f$' <<<"${history}"; then
  echo "Flyway history contains a failed migration:" >&2
  echo "${history}" >&2
  exit 1
fi

db_versions="$(cut -d: -f1 <<<"${history}" | paste -sd, -)"
file_versions="$(
  find backend/src/main/resources/db/migration -maxdepth 1 -type f -name 'V*.sql' -printf '%f\n' \
    | sed -E 's/^V([0-9]+)__.*/\1/' \
    | sort -n \
    | paste -sd, -
)"

if [[ "${db_versions}" != "${file_versions}" ]]; then
  echo "Database migration history does not match repository migration order." >&2
  echo "Database:   ${db_versions}" >&2
  echo "Repository: ${file_versions}" >&2
  exit 1
fi

echo "Fresh-database migration verification passed: ${db_versions}"
