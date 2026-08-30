#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-deploy/compose.yaml}"
BACKUP_DIR="${BACKUP_DIR:-backups}"
DB_NAME="${DB_NAME:-developer_analytics}"
DB_USERNAME="${DB_USERNAME:-developer_analytics}"

mkdir -p "${BACKUP_DIR}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup="${BACKUP_DIR}/developer-analytics-${timestamp}.dump"

docker compose -f "${COMPOSE_FILE}" exec -T db \
  pg_dump \
    --username="${DB_USERNAME}" \
    --dbname="${DB_NAME}" \
    --format=custom \
    --compress=6 \
    --no-owner \
    --no-privileges \
  > "${backup}"

sha256sum "${backup}" > "${backup}.sha256"

echo "Backup created: ${backup}"
echo "Checksum created: ${backup}.sha256"
