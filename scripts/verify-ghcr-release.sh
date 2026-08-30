#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-}"
if [[ -z "${VERSION}" ]]; then
  echo "Usage: bash ./scripts/verify-ghcr-release.sh <release-version>" >&2
  exit 2
fi

VERSION="${VERSION#v}"
OWNER="${GHCR_OWNER:-erland}"
WEB_IMAGE="${WEB_IMAGE:-ghcr.io/${OWNER}/developer-analytics-web}"
BACKEND_IMAGE="${BACKEND_IMAGE:-ghcr.io/${OWNER}/developer-analytics-backend}"
COMPOSE_FILE="${RELEASE_COMPOSE_FILE:-deploy/compose.release.example.yaml}"

fail() {
  echo "GHCR release verification failed: $*" >&2
  exit 1
}

echo "Removing any locally cached release images..."
docker image rm -f \
  "${WEB_IMAGE}:${VERSION}" \
  "${BACKEND_IMAGE}:${VERSION}" >/dev/null 2>&1 || true

echo "Logging out of GHCR to verify intended public visibility..."
docker logout ghcr.io >/dev/null 2>&1 || true

echo "Pulling release images anonymously from a clean local state..."
docker pull "${WEB_IMAGE}:${VERSION}" >/dev/null \
  || fail "web image ${WEB_IMAGE}:${VERSION} cannot be pulled anonymously"
docker pull "${BACKEND_IMAGE}:${VERSION}" >/dev/null \
  || fail "backend image ${BACKEND_IMAGE}:${VERSION} cannot be pulled anonymously"

echo "Verifying semantic-version tags..."
minor_version="$(
  python3 - "${VERSION}" <<'PY'
import re, sys
version = sys.argv[1]
match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)(?:[-+].*)?", version)
if not match:
    raise SystemExit("Release version is not semantic x.y.z")
print(f"{match.group(1)}.{match.group(2)}")
PY
)"

docker manifest inspect "${WEB_IMAGE}:${VERSION}" >/dev/null \
  || fail "web full-version tag is missing"
docker manifest inspect "${BACKEND_IMAGE}:${VERSION}" >/dev/null \
  || fail "backend full-version tag is missing"
docker manifest inspect "${WEB_IMAGE}:${minor_version}" >/dev/null \
  || fail "web major/minor tag ${minor_version} is missing"
docker manifest inspect "${BACKEND_IMAGE}:${minor_version}" >/dev/null \
  || fail "backend major/minor tag ${minor_version} is missing"

echo "Verifying release Compose uses the backend image for both API and worker..."
APP_VERSION="${VERSION}" \
GITHUB_CLIENT_ID=test \
GITHUB_CLIENT_SECRET=test \
GITHUB_CALLBACK_URL=https://example.invalid/api/auth/github/callback \
FRONTEND_URL=https://example.invalid/ \
DB_PASSWORD=test \
CREDENTIAL_ENCRYPTION_KEY=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8= \
docker compose -f "${COMPOSE_FILE}" config --format json \
  > /tmp/developer-analytics-release-compose.json

python3 - /tmp/developer-analytics-release-compose.json "${BACKEND_IMAGE}:${VERSION}" <<'PY'
import json, sys
path, expected = sys.argv[1], sys.argv[2]
with open(path, encoding="utf-8") as handle:
    data = json.load(handle)
backend = data["services"]["backend"]["image"]
worker = data["services"]["worker"]["image"]
if backend != expected:
    raise SystemExit(f"backend image mismatch: {backend} != {expected}")
if worker != expected:
    raise SystemExit(f"worker does not use backend image: {worker} != {expected}")
PY

echo "Inspecting image config/history for embedded repository secrets..."
check_image_for_secrets() {
  local image="$1"
  local tmp
  tmp="$(mktemp)"

  {
    docker image inspect "${image}"
    docker history --no-trunc "${image}"
  } > "${tmp}"

  # These values must only arrive at runtime through Compose/environment.
  # Any occurrence with a concrete assignment in image metadata/history is a failure.
  if grep -E \
    '(GITHUB_CLIENT_SECRET|GEMINI_API_KEY|CREDENTIAL_ENCRYPTION_KEY|DB_PASSWORD)=[^"[:space:]]+' \
    "${tmp}" >/dev/null; then
    echo "Potential embedded secret material found in ${image}:" >&2
    grep -E \
      '(GITHUB_CLIENT_SECRET|GEMINI_API_KEY|CREDENTIAL_ENCRYPTION_KEY|DB_PASSWORD)=[^"[:space:]]+' \
      "${tmp}" >&2 || true
    rm -f "${tmp}"
    return 1
  fi

  rm -f "${tmp}"
}

check_image_for_secrets "${WEB_IMAGE}:${VERSION}" \
  || fail "web image contains secret-like build metadata"
check_image_for_secrets "${BACKEND_IMAGE}:${VERSION}" \
  || fail "backend image contains secret-like build metadata"

echo "Verifying source OCI label is present..."
for image in "${WEB_IMAGE}:${VERSION}" "${BACKEND_IMAGE}:${VERSION}"; do
  source_label="$(
    docker image inspect \
      --format '{{ index .Config.Labels "org.opencontainers.image.source" }}' \
      "${image}"
  )"
  [[ -n "${source_label}" && "${source_label}" != "<no value>" ]] \
    || fail "${image} is missing org.opencontainers.image.source"
done

echo "GHCR release verification passed:"
echo "- anonymous clean pulls succeeded"
echo "- full and major/minor release tags exist"
echo "- worker uses the published backend image"
echo "- no runtime secret assignments were found in image config/history"
echo "- OCI source metadata is present"
