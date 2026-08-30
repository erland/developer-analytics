#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <backup.dump>" >&2
  exit 2
fi

backup="$1"
COMPOSE_FILE="${COMPOSE_FILE:-deploy/compose.yaml}"
DB_NAME="${DB_NAME:-developer_analytics}"
DB_USERNAME="${DB_USERNAME:-developer_analytics}"

if [[ ! -f "${backup}" ]]; then
  echo "Backup does not exist: ${backup}" >&2
  exit 2
fi

if [[ -f "${backup}.sha256" ]]; then
  sha256sum --check "${backup}.sha256"
fi

echo "Stopping application services that write to PostgreSQL..."
docker compose -f "${COMPOSE_FILE}" stop backend worker

echo "Terminating existing database sessions..."
docker compose -f "${COMPOSE_FILE}" exec -T db \
  psql --username="${DB_USERNAME}" --dbname=postgres \
  --set=ON_ERROR_STOP=1 \
  --command="SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${DB_NAME}' AND pid <> pg_backend_pid();"

echo "Recreating ${DB_NAME}..."
docker compose -f "${COMPOSE_FILE}" exec -T db \
  dropdb --username="${DB_USERNAME}" --if-exists "${DB_NAME}"
docker compose -f "${COMPOSE_FILE}" exec -T db \
  createdb --username="${DB_USERNAME}" "${DB_NAME}"

echo "Restoring backup..."
cat "${backup}" | docker compose -f "${COMPOSE_FILE}" exec -T db \
  pg_restore \
    --username="${DB_USERNAME}" \
    --dbname="${DB_NAME}" \
    --no-owner \
    --no-privileges \
    --exit-on-error

echo "Starting application services..."
docker compose -f "${COMPOSE_FILE}" start backend worker

echo "Restore complete."
