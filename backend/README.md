# Backend

Quarkus/Java backend for Developer Analytics.

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 16+ for deployed environments
- Docker when using Quarkus Dev Services for local/test PostgreSQL

## Commands

```bash
mvn test
mvn verify
mvn quarkus:dev
```

## Database

PostgreSQL is the application's persistence store. Flyway owns the schema and migrations live in:

```text
src/main/resources/db/migration/
```

The first baseline migration creates `application_metadata`. Hibernate schema generation is disabled; application mappings must match the Flyway-managed schema.

When no JDBC URL is configured in development/test, Quarkus Dev Services may start a PostgreSQL container automatically when Docker is available.

For an externally managed database, configure for example:

```text
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/developer_analytics
DB_USERNAME=developer_analytics
DB_PASSWORD=change-me
```

## Smoke endpoint

```text
GET /api/health/application
```

## Production container

Build the backend image from the repository root:

```bash
docker build -f backend/Dockerfile -t developer-analytics-backend .
```

The Dockerfile performs a Maven/Quarkus build in a builder stage and copies the Quarkus fast-jar runtime into a Java 21 JRE image. The runtime process uses a non-root user and listens on port `8080`.

The same backend image is intentionally suitable for reuse by the future background-worker service; worker-specific startup behaviour will be added when the worker runtime mode is implemented.


## GitHub sign-in

Step 22 introduces GitHub App user authentication using GitHub's OAuth web application flow with:

- server-generated `state`,
- PKCE (`S256`),
- ten-minute persisted login attempts,
- one-time callback consumption,
- transient GitHub access token usage only to resolve the GitHub identity,
- service-owned opaque browser sessions stored as token hashes,
- HttpOnly + SameSite=Lax session cookies,
- configurable Secure cookie flag,
- logout and session status endpoints.

Authentication is deliberately separate from later GitHub repository permissions. The sign-in token is not stored for repository synchronisation.

Endpoints:

- `GET /api/auth/github/login`
- `GET /api/auth/github/callback`
- `GET /api/auth/session`
- `POST /api/auth/logout`


## `/me` security boundary

Step 23 establishes `/api/me/...` as the authenticated user boundary.

Rules:

- browser/API clients never provide an application `userId`,
- the backend derives the current user from the opaque server-side session,
- all user-owned queries require the resolved internal user ID,
- object lookup uses `(object id, current user id)` rather than object id alone,
- an object owned by another user is returned as `404` rather than exposing its existence,
- unauthenticated calls are rejected with `401`.

Initial endpoints:

- `GET /api/me`
- `GET /api/me/repositories`
- `GET /api/me/repositories/{repositoryId}`

Future user-scoped dashboard, analysis and GPT endpoints should follow the same pattern.


## Account connection management

Step 24 adds provider-neutral connection management under the authenticated `/api/me` boundary.

Endpoints:

- `GET /api/me/connections`
- `GET /api/me/connections/{provider}`
- `POST /api/me/connections/{provider}/disconnect`
- `POST /api/me/connections/{provider}/validate`

Connection mutations are always scoped by the current internal user ID. Disconnecting a provider does not delete the user's historical analysis data; it only changes the connection state. A future provider adapter can decide whether validation performs a remote token/API check before marking a connection healthy.


## Source-control provider adapter

Step 25 introduces a provider-neutral source-control API and the first GitHub implementation.

Core abstractions:

- `SourceControlProvider`
- `ProviderUser`
- `ProviderRepository`
- `PagedResult`
- `ProviderRateLimit`
- `ProviderAccessToken`

`GitHubProviderAdapter` currently supports current-user lookup, authenticated repository discovery via `/user/repos`, visibility/owner normalization, fork/archive state, timestamps, paging, and GitHub rate-limit headers.

Provider access tokens are value objects whose string representation is always redacted. The adapter consumes tokens but does not persist them; secure credential storage belongs to the connection layer added in the following step.


## Provider credential storage

Step 26 adds encrypted provider credential storage.

Security properties:

- GitHub access tokens are encrypted before database persistence.
- AES-256-GCM is used with a fresh random 96-bit nonce per encryption.
- the encryption key is supplied through `CREDENTIAL_ENCRYPTION_KEY` and is never stored in PostgreSQL,
- the key is versioned through `CREDENTIAL_KEY_VERSION`,
- plaintext tokens are exposed only transiently as `ProviderAccessToken` values when a provider API call needs them,
- `ProviderAccessToken.toString()` remains redacted,
- disconnecting a provider removes the encrypted credential while preserving historical analysis data.

`CREDENTIAL_ENCRYPTION_KEY` must decode from Base64 to exactly 32 bytes. In production it should be generated randomly and stored in a deployment secret manager or protected environment configuration.


## Initial GitHub repository discovery

Step 27 adds the first real worker job: `GITHUB_REPOSITORY_DISCOVERY`.

Flow:

1. an authenticated user queues discovery through `POST /api/me/sync/github/repositories`,
2. the persistent PostgreSQL job queue stores the job,
3. a worker claims it,
4. the handler decrypts the user's GitHub access token only for the duration of the provider call,
5. `GitHubProviderAdapter` fetches current-user data and walks all repository pages,
6. repositories are upserted by `(user, provider, external repository id)`,
7. ownership, visibility, fork/archive state, URLs and last activity are normalized into the domain model,
8. `discovered_at` and `last_seen_at` record inventory coverage.

Repeated discovery is idempotent with respect to repository identity and updates existing rows rather than creating duplicates.


## Repository discovery progress and sync state

Step 28 adds persistent repository sync-run state.

Each GitHub discovery run records:

- queued/running/completed/failed/rate-limited status,
- repositories seen, created and updated,
- pages processed,
- latest observed GitHub rate-limit remaining/reset values,
- start/completion timestamps,
- last error.

Repositories transition through `SYNCING` to `SYNCED`, with `sync_error` available for future per-repository failures.

Authenticated status endpoints:

- `GET /api/me/sync-runs`
- `GET /api/me/sync-runs/{id}`

This gives the frontend enough state to show discovery progress without querying raw worker/job internals.


## Initial contribution discovery

Step 29 adds the first contribution sync path for GitHub repositories.

Implemented contribution types:

- commits,
- pull requests.

Flow:

- `POST /api/me/sync/github/repositories/{repositoryId}/contributions` queues a worker job,
- the worker resolves the repository under the current user,
- the GitHub adapter fetches commit pages and recent pull requests,
- contributions are normalized into the provider-neutral `Contribution` model,
- deduplication uses `(user, provider, external contribution id, contribution type)`,
- existing contributions are updated rather than duplicated.

The initial implementation uses an incremental window derived from repository activity and keeps review/issue discovery for the next iteration.


## Reviews and issues

Step 30 extends GitHub contribution discovery with pull-request reviews and issues.

GitHub's issues endpoint also returns pull requests, so those entries are explicitly filtered to avoid double counting. Reviews and issues use stable provider IDs with type prefixes and still flow through the common contribution deduplication rule.

The normalized contribution model now covers:

- `COMMIT`
- `PULL_REQUEST`
- `REVIEW`
- `ISSUE`

Commit pagination remains the cursor driver in this first implementation. Recent pull requests, reviews and issues are refreshed once per repository contribution-sync run.


## Contribution sync orchestration

Step 31 adds orchestration across the user's repository inventory.

After repository discovery completes, the worker now seeds the first contribution-sync batch automatically. Additional batches can be queued through:

- `POST /api/me/sync/github/contributions?offset=0&batchSize=25`

Important safeguards:

- contribution jobs use a stable deduplication key per user/repository,
- a partial unique PostgreSQL index prevents duplicate queued/running jobs,
- batching defaults to 25 repositories and is capped at 100,
- access-revoked repositories are excluded,
- the API returns `nextOffset` so the frontend or a later orchestration job can continue through large inventories.

This keeps repository inventories with hundreds of projects from creating an uncontrolled burst of duplicate worker jobs.
