# Health and Readiness

Step 73 exposes standard SmallRye Health endpoints plus an application-level
operational summary.

## Standard endpoints

Quarkus SmallRye Health exposes:

- `/q/health/live` — backend process liveness
- `/q/health/ready` — readiness
- `/q/health` — combined health

Readiness includes:

- PostgreSQL connectivity (`SELECT 1`),
- Flyway migration state (`flyway_schema_history`),
- process-role worker check.

A failed migration or unavailable database makes readiness fail.

## Worker status

Worker processes write a database heartbeat every 30 seconds to
`worker_heartbeat`. `/api/health/application` reports the newest heartbeat as:

- `UP` when seen within two minutes,
- `STALE` when older,
- `UNKNOWN` before a heartbeat exists.

The API process does not declare itself unready merely because a separate worker
heartbeat is stale; this keeps interactive/read-only functionality available
while still exposing worker degradation explicitly. A worker process itself has
its own readiness endpoint.

## Optional AI

Gemini/AI configuration is reported by `/api/health/application`, but
`requiredForServiceHealth=false`.

Developer Analytics therefore remains healthy/readable when Gemini is
unconfigured or temporarily unavailable. AI outage handling remains the
degraded optional path implemented in Step 71.

## Application operational summary

`GET /api/health/application` reports:

- backend service/process status,
- database status,
- latest worker heartbeat,
- optional AI provider/model/configuration status.

No tokens, credentials, prompts or private source content are included.
