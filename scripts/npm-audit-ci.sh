#!/usr/bin/env bash
set -uo pipefail

audit_command=(npm audit --omit=dev --audit-level=high)

for attempt in 1 2; do
  set +e
  output="$(timeout 60s "${audit_command[@]}" 2>&1)"
  status=$?
  set -e

  printf '%s\n' "$output"

  if [[ $status -eq 0 ]]; then
    exit 0
  fi

  transient=false
  if [[ $status -eq 124 ]]; then
    transient=true
  elif grep -Eqi '503 Service Unavailable|502 Bad Gateway|504 Gateway Timeout|429 Too Many Requests|audit endpoint returned an error|ECONNRESET|ETIMEDOUT|ENOTFOUND|EAI_AGAIN|socket hang up' <<<"$output"; then
    transient=true
  fi

  if [[ "$transient" == false ]]; then
    # npm audit found a real policy failure (for example a high/critical
    # vulnerability) or another non-transient error. Keep failing CI.
    exit "$status"
  fi

  if [[ $attempt -lt 2 ]]; then
    echo "npm audit service unavailable; retrying once in 5 seconds..."
    sleep 5
    continue
  fi

  echo "::warning title=npm audit unavailable::The npm security audit service was unavailable after two attempts. Dependency Review and container scans still run; re-run CI later to obtain the npm audit result."
  exit 0
done
