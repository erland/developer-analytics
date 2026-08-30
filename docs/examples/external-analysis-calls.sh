#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${DEVELOPER_ANALYTICS_URL:-https://developer-analytics.example.com}"
TOKEN="${DEVELOPER_ANALYTICS_TOKEN:?Set DEVELOPER_ANALYTICS_TOKEN}"
ANALYSIS_ACCEPT="application/vnd.developer-analytics.analysis.v1+json"

curl_analysis() {
  curl --fail --silent --show-error \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Accept: ${ANALYSIS_ACCEPT}" \
    "$@"
}

echo "== Profile =="
curl_analysis "${BASE_URL}/api/me/profile"
echo

echo "== Technologies =="
curl_analysis "${BASE_URL}/api/me/technologies?limit=10"
echo

echo "== Project types =="
curl_analysis "${BASE_URL}/api/me/project-types?limit=10"
echo

echo "== Activity =="
curl_analysis "${BASE_URL}/api/me/activity?months=24"
echo
