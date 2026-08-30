# Synchronisation Recovery

Step 71 makes background synchronisation safely retriable across service and
provider failures.

## Worker/backend restart

Background jobs are persistent PostgreSQL records. A worker marks a claimed job
`RUNNING` with a lock timestamp. Every worker performs a recovery sweep every
60 seconds.

A `RUNNING` job whose lock is older than 10 minutes is treated as interrupted,
its worker lock is cleared and it moves to `WAITING` for another attempt.

This covers worker process termination and backend/worker container restarts
without requiring an in-memory queue.

## Temporary GitHub/network errors

The shared `JobFailureClassifier` classifies:

- HTTP 408, 409, 425, 429 and 5xx as retriable,
- connection/timeout/I/O failures as retriable,
- HTTP 401, 403 and 404 as lost provider/repository access.

Retriable jobs use bounded exponential backoff and the existing `maxAttempts`
limit. Handlers remain idempotent/deduplicated through repository upserts,
contribution uniqueness constraints and job deduplication keys.

## Lost repository permission

A GitHub 401/403/404 is not retried indefinitely. Developer Analytics:

- marks GitHub repositories `ACCESS_REVOKED`,
- marks the GitHub provider connection `ERROR`,
- cancels queued/waiting GitHub jobs,
- makes the current job terminal.

The user can restore permission/reconnect and queue synchronisation again.

## AI outage

AI remains optional. `AiAnalysisGateway` catches provider runtime outages and
returns `Optional.empty()` after logging only operation/provider/error metadata.
Deterministic analysis therefore continues to work and an AI outage does not
invalidate repository synchronisation or measured statistics.

## Manual recovery

Authenticated users can request:

```text
POST /api/me/sync-recovery/recover
POST /api/me/sync-recovery/github/retry
```

The first immediately performs the same stale-lock recovery check used by the
worker. The second queues a fresh, deduplicated GitHub repository discovery
cycle.

Automatic recovery remains the normal path; these endpoints exist for explicit
operational recovery and troubleshooting.
