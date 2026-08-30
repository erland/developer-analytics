#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: bash ./scripts/run-frontend-test-layer.sh <layer>" >&2
  echo "Layers: all component feature responsive api-error privacy" >&2
  exit 2
fi

layer="$1"
cd frontend

case "${layer}" in
  all)
    npm test
    ;;
  component|feature|responsive|api-error|privacy)
    npm run "test:${layer}"
    ;;
  *)
    echo "Unknown frontend test layer: ${layer}" >&2
    exit 2
    ;;
esac
