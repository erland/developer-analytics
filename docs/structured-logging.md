# Structured Logging and Correlation IDs

Step 72 establishes a structured operational logging contract.

## HTTP correlation IDs

Every HTTP request receives `X-Correlation-ID`.

- A caller-provided ID is reused only when it matches the safe identifier
  character/length rules.
- Otherwise Developer Analytics generates a UUID.
- The ID is placed in logging MDC for the request and returned in the response.
- It is removed from MDC after the response to prevent leakage between requests.

## Background jobs

Worker execution places `backgroundJobId` in MDC and logs structured events:

- `background_job_started`
- `background_job_completed`
- `background_job_failed`

Fields include job type, attempt and retry/access-loss classification without
logging job payload contents.

## Synchronisation IDs

Repository and contribution synchronisation services log their persisted
`syncId`. Contribution logs may also include the internal repository UUID, but
never repository source content.

Examples of event names:

- `repository_sync_started`
- `repository_sync_completed`
- `repository_sync_provider_error`
- `contribution_sync_started`
- `contribution_sync_completed`

## Error logs

`StructuredLog` uses stable `key=value` fields and records only:

- event name,
- correlation/job/sync identifiers,
- provider/model identifiers,
- HTTP status and counters where appropriate,
- exception class,
- bounded/sanitized exception message.

Common token/API-key query values in exception messages are redacted.

## Sensitive-data prohibition

Structured fields whose names indicate any of the following are discarded:

- tokens,
- credentials,
- secrets,
- authorization headers,
- private source content,
- AI prompts,
- source diffs.

Worker job payloads are not logged. Provider access tokens remain wrapped in
`ProviderAccessToken` and are never added to operational log fields.

The Gemini implementation continues to log request type/status/token *counts*
only; prompt bodies are not logged.

## Console format

Quarkus console logs expose timestamp, level, logger, correlation ID,
background-job ID and the structured message. This is suitable for ingestion by
log platforms while remaining readable in container logs.
