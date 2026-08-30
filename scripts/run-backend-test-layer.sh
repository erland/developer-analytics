#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: bash ./scripts/run-backend-test-layer.sh <layer>" >&2
  echo "Layers: all unit persistence github-adapter authorization worker-job privacy" >&2
  exit 2
fi

layer="$1"
case "${layer}" in
  all)
    profile=""
    ;;
  unit|persistence|github-adapter|authorization|worker-job|privacy)
    profile="-Ptest-${layer}"
    ;;
  *)
    echo "Unknown backend test layer: ${layer}" >&2
    exit 2
    ;;
esac

cd backend
if [[ -n "${profile}" ]]; then
  mvn --batch-mode --no-transfer-progress "${profile}" test
else
  mvn --batch-mode --no-transfer-progress test
fi
