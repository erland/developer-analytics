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


## Contribution sync progress and completion state

Step 32 adds persistent contribution-sync state per repository.

Each run records status (`QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`, `RATE_LIMITED`), contributions seen/created/updated, pages processed, rate-limit information, timestamps and the last error.

Authenticated endpoints:

- `GET /api/me/contribution-sync-runs`
- `GET /api/me/contribution-sync-runs?repositoryId=<uuid>`
- `GET /api/me/contribution-sync-runs/{id}`

The frontend can now follow repository discovery and contribution discovery without reading internal background-job records.


## Technology catalogue

Step 33 introduces a provider-neutral technology catalogue.

Each technology has:

- a stable key,
- display name,
- category,
- aliases,
- language evidence,
- file/path evidence,
- manifest/dependency evidence,
- active/inactive state.

The built-in catalogue is stored in `src/main/resources/technology-catalogue.json`, while the database table allows the catalogue to become configurable later without changing repository-analysis code.

Initial entries cover common languages, frameworks, databases, containers, CI/CD and infrastructure tooling. The next detection steps consume the evidence fields rather than hard-coding technology names into scanners.

Endpoint:

- `GET /api/technology-catalogue`


## Language-based technology evidence

Step 34 uses GitHub's repository language breakdown as deterministic technology evidence.

For every observed GitHub language that matches a catalogue entry, the backend stores:

- repository,
- technology,
- evidence type `LANGUAGE`,
- strength `OBSERVED`,
- provider language name,
- measured byte count,
- observation timestamp.

This intentionally records evidence rather than declaring that a user is "skilled" or an "expert" in a technology.

Endpoints:

- `POST /api/me/sync/github/repositories/{repositoryId}/language-evidence`
- `GET /api/me/repositories/{repositoryId}/technology-evidence`

Language evidence jobs are user-scoped and deduplicated per repository.


## File- and manifest-based technology evidence

Step 35 adds deterministic detection from repository structure and selected text manifests.

The GitHub adapter:

- reads the default branch tree,
- selects relevant technology files,
- fetches textual content for a bounded number of manifests,
- never clones the full repository or stores raw source code as part of normal analysis.

Evidence types:

- `FILE` — presence/path of a relevant file or directory,
- `MANIFEST` — deterministic token/dependency match inside a selected manifest.

Examples include `pom.xml`, `package.json`, `Dockerfile`, Compose, GitHub Actions workflows, Kubernetes/Helm files, Terraform and Swift package files.

Endpoint:

- `POST /api/me/sync/github/repositories/{repositoryId}/file-manifest-evidence`

Results remain available through:

- `GET /api/me/repositories/{repositoryId}/technology-evidence`


## Technology evidence strength

Step 36 converts accumulated evidence into a transparent user-level technology assessment.

The model considers:

- number of repositories,
- number of independent evidence types,
- total evidence volume,
- duration between first and latest observation,
- recent repository activity.

Labels:

- `EXPOSURE`
- `LIMITED`
- `MODERATE`
- `STRONG`

The score and its point breakdown are stored in `rationale` so the result is explainable. The label explicitly describes evidence strength and must not be presented as formal skill proficiency.

Endpoints:

- `POST /api/me/technology-assessments/recalculate`
- `GET /api/me/technology-assessments`


## Technology timeline

Step 37 builds chart-ready technology timelines from deterministic evidence and contribution activity.

For each observed technology the API exposes:

- first observed use,
- latest observed use,
- total observed repository count,
- public repository count,
- private repository count,
- projects per year,
- contribution activity per year.

Monthly activity is stored in the existing `technology_activity_month` aggregate table. Activity is attributed to a technology when a contribution occurred in a repository where that technology has observed evidence.

Endpoints:

- `POST /api/me/technology-timeline/recalculate`
- `GET /api/me/technology-timeline`

The API keeps public/private counts separate so later report modes can omit private repository identity while still using aggregated private evidence.


## Project category taxonomy

Step 38 introduces a data-driven project taxonomy. Categories are stored as rows, not as a Java enum, so the taxonomy can evolve without a code deployment.

Built-in categories include web application, mobile application, game, backend service, API, library, framework, developer tooling, automation, infrastructure/platform, DevOps/CI/CD, security, observability, data/database, integration, AI/ML, architecture/modelling, documentation/education and experiment/prototype.

Repositories may have multiple category assignments. Assignments keep their source (`DETERMINISTIC`, `AI`, `MANUAL`), confidence and rationale separately.

Endpoint:

- `GET /api/project-category-taxonomy`


## Deterministic project classification

Step 39 classifies repositories before any AI provider is involved. Signals include GitHub topics, detected technologies, known files/repository structure, and repository name/description metadata.

Each category receives an explicit point score from independent signals. Assignments are stored with source `DETERMINISTIC`, confidence (`LOW`, `MEDIUM`, `HIGH`) and a rationale containing the exact supporting signals. Repositories can have multiple categories, and deterministic assignments are recalculated rather than appended indefinitely.

Endpoints:

- `POST /api/me/sync/repositories/{repositoryId}/classification`
- `GET /api/me/repositories/{repositoryId}/project-categories`

Repository discovery now persists GitHub description and topics. This step also restores the missing step-35 enqueue method for file/manifest evidence so the job service remains internally consistent.


## Project significance and user involvement

Step 40 deliberately keeps two concepts separate.

### Project significance

The project-side assessment uses explainable sub-scores for:

- popularity proxy,
- observed contribution volume/contributor breadth proxy,
- longevity,
- organisation/ecosystem context,
- current activity.

The current V1 does not yet collect GitHub stars/watchers, so the popularity component explicitly records that it is using repository-state proxies rather than pretending those metrics are available.

### User involvement

The user-side assessment independently uses:

- contribution count,
- duration of involvement,
- recency,
- relative share of the observed project contributions.

Both sides receive their own score, level and rationale. The ranking endpoint sorts by project significance first and involvement second, but it returns both complete breakdowns rather than a single opaque global score.

Endpoints:

- `POST /api/me/project-significance/recalculate`
- `GET /api/me/project-significance`


## Significant external projects

Step 41 identifies repositories not owned directly by the user where at least one of the two step-40 dimensions is high:

- project significance is `HIGH` or `VERY_HIGH`,
- user involvement is `HIGH` or `VERY_HIGH`,
- or both.

The result keeps the dimensions separate and adds a `matchReason`:

- `PROJECT_SIGNIFICANCE`
- `USER_INVOLVEMENT`
- `BOTH`

Each API result includes both complete rationale maps so the user can see exactly why the project surfaced.

Endpoint:

- `GET /api/me/significant-external-projects`

Projects directly owned by the user are excluded; organisation-owned and otherwise external repositories remain eligible.


## Activity views

Step 44 adds a user-scoped activity endpoint:

- `GET /api/me/activity?from=YYYY-MM-DD&to=YYYY-MM-DD`

The endpoint calculates commit counts per year and month, active projects, average/median commit size and additions/deletions from measured contribution records. Date filters are optional and inclusive by calendar date.


## Project inventory

Step 45 adds a paged project inventory endpoint with server-side filtering:

- `GET /api/me/project-inventory`

Supported query parameters:

- `page`
- `pageSize`
- `search`
- `ownership=own|external`
- `visibility=public|private`
- `activity=active|inactive`
- `category=<category-key>`
- `technology=<technology-key>`

Inventory rows include category and technology metadata for display and filtering.


## Project detail

Step 46 adds a consolidated authenticated endpoint:

- `GET /api/me/projects/{repositoryId}`

It returns repository metadata, contribution/activity timeline, technology evidence, project categories, the separate project-significance and user-involvement assessments, and repository synchronisation state.


## Technology views

Step 47 adds a consolidated authenticated technology endpoint:

- `GET /api/me/technologies`

Each technology includes its evidence level and score, project/evidence counts, first/latest observation, recent-project count, monthly activity timeline and up to five representative projects. The response preserves the explicit distinction between evidence strength and formal proficiency.


## Project-type views

Step 48 adds an authenticated project-type analytics endpoint:

- `GET /api/me/project-types`

Each category returns the number of classified projects, total observed contribution activity, monthly category evolution with active-project counts, and up to five representative projects. Repositories may appear in multiple categories by design.


## Explicit private repository authorisation

Step 49 keeps normal GitHub sign-in public-data-only. Private repository analysis requires a separate authenticated OAuth authorisation flow that explicitly requests GitHub's `repo` scope.

The provider connection stores an application-level `private_repository_access` flag. Repository discovery checks this flag before accepting any private repository, so a token with broader permissions is never treated as implicit consent.

## Privacy provenance

Step 50 adds `PUBLIC_ONLY`, `INCLUDES_PRIVATE`, and `PRIVATE_AGGREGATE` provenance to derived statistics and assessments. Repository-level evidence inherits repository visibility; cross-repository technology aggregates calculate provenance from contributing public/private repositories.

## Private repository selection

Step 51 adds `source_repository.included_in_analysis`. Newly discovered private repositories default to excluded. The authenticated `/api/me/private-repositories` API lists authorised private repositories, changes per-repository selection, queues a GitHub discovery refresh, and removes a private repository from analysis by excluding it without revoking GitHub permission. Contribution sync candidates and primary analysis inventory queries respect the selection flag.


## Private export controls

Step 52 adds `POST /api/me/reports/export`. The request must explicitly contain both `privateDataMode` and `hidePrivateRepositoryNames`; missing values are rejected with HTTP 400.

Supported modes are:

- `EXCLUDE_PRIVATE`
- `INCLUDE_PRIVATE_AGGREGATES`
- `INCLUDE_FULL_PRIVATE_DETAIL`

Aggregate mode never emits private repository names or per-project private detail. Full-private mode can independently hide private repository names. The current export format is Markdown.


## AI provider abstraction

Step 53 introduces the vendor-neutral `AiProvider` interface with the initial operations `classifyProject`, `summariseProject`, `normaliseTechnologies`, `inferRoles`, and `summariseTechnologyHistory`.

`AiAnalysisGateway` is the application-facing boundary. The default `DisabledAiProvider` is a Quarkus `@DefaultBean`, so the application starts and all deterministic analytics remain available without any AI credentials or vendor configuration. A future vendor integration can replace the default bean without changing core domain services.


## Gemini provider

Step 54 adds Gemini behind `AiProvider`. Enable it with:

- `AI_PROVIDER=gemini`
- `GEMINI_API_KEY=<secret>`
- optional `GEMINI_MODEL` (default `gemini-2.5-flash-lite`)
- optional `GEMINI_BASE_URL`

The API key is never committed and has no non-secret fallback. If the provider is disabled or the key is missing, `DisabledAiProvider` is selected and deterministic analytics continue normally.

Gemini requests use structured JSON output schemas. Logging records only the operation type, success/failure, HTTP status when relevant, exception class, and available token usage metadata. Prompt/request content is deliberately not logged.


## AI privacy policy enforcement

Step 55 requires every external AI call to carry an `AiRequestContext` with explicit data sensitivity. The gateway evaluates that context before invoking the configured provider.

Supported user/provider policies are:

- `PUBLIC_ONLY`
- `PRIVATE_METADATA_ALLOWED`
- `PRIVATE_AI_DISABLED`

Private source/content is blocked unconditionally in this phase. Private metadata is permitted only when both the user has explicitly selected `PRIVATE_METADATA_ALLOWED` and the deployment-level provider policy (`AI_PRIVATE_DATA_POLICY`) allows it. User consent can restrict provider policy but never widen it.

The per-user default is `PRIVATE_AI_DISABLED`. Blocked requests are logged by operation type, sensitivity and denial reason, without prompt data.


## AI project classification

Step 56 adds persisted AI-assisted project classification as a complement to deterministic project classification.

Each result stores:

- classification labels,
- confidence,
- explanation,
- analysis version,
- creation time,
- provider and model metadata,
- privacy provenance,
- a SHA-256 fingerprint of the repository metadata, observed technologies and deterministic classifications used as input.

`POST /api/me/projects/{repositoryId}/ai-classification` generates a result only when the configured provider and AI privacy policy permit the request. An unchanged repository with the same analysis version/provider/model reuses the existing result instead of making another external AI request. `GET` returns the latest persisted result.

Private repositories are sent only as `PRIVATE_METADATA` requests and therefore remain subject to the Step 55 consent/provider-policy gate. No source file content is included.


## User-level AI insights

Step 57 adds optional, persisted user-level AI insights. The AI receives aggregate technology, project-category, repository and contribution signals and returns:

- likely roles with confidence/rationale,
- technical focus,
- breadth/depth observation,
- technology evolution summary,
- open-source engagement summary.

Results are stored separately from measured/deterministic analytics and are explicitly marked AI-generated in the API/UI. The input is SHA-256 fingerprinted; unchanged data with the same analysis version/provider/model reuses the stored result.

Privacy remains enforced by Step 55. With private-metadata consent disabled, the service builds the AI input from public evidence only. With private metadata allowed, private repository metadata may contribute, but source content is never included.


## User correction feedback

Step 58 introduces a separate `user_analysis_correction` layer. Corrections never delete or rewrite source facts, contribution records, technology evidence or project-category assignments.

Supported corrections are:

- reject a project category for a repository,
- suppress a technology inference for the user,
- exclude a repository from user-level AI profile conclusions.

Technology suppression filters the inferred technology from analysis/UI and AI profile input while retaining its evidence rows. Category rejection is stored separately and is excluded from user-level AI category signals. Project exclusion removes that repository only from user-level AI profile aggregation; measured project/activity data remains intact.

Corrections can be reversed by disabling the corresponding correction.


## External Analysis API contract

Step 59 introduces a versioned compact representation for external GPT/LLM clients. Clients request `application/vnd.developer-analytics.analysis.v1+json` on the standard `/api/me/...` URLs. This leaves the dashboard's existing `application/json` resources unchanged while providing an independent contract that avoids frontend-specific payloads.

The v1 endpoints are documented in `docs/external-analysis-api.md`.


## External client authentication

Step 60 adds user-controlled `da_ext_...` bearer tokens for the versioned External Analysis API. Raw tokens are returned once and only their SHA-256 hashes are persisted. Tokens are user-specific, explicitly scoped, revocable and independent of both browser sessions and GitHub OAuth/provider credentials.

Management uses the authenticated browser API at `/api/me/external-clients`. The external analysis resources require the dedicated bearer token and enforce a scope per endpoint.


## External client privacy scopes

Step 61 adds a second, independent permission dimension to external-client tokens:

- `PUBLIC_ONLY`
- `PUBLIC_PLUS_PRIVATE_AGGREGATES`
- `FULL_AUTHORISED_ANALYSIS`

The backend enforces the privacy scope on every compact external analysis endpoint. `PUBLIC_ONLY` excludes private evidence. Aggregate scope permits private data only in aggregate analytical forms, while `/projects` still returns public repositories only. Full scope permits private project detail only for repositories already authorised and included in analysis.


## Returned AI assessment API

Step 62 adds a write-back channel for external GPT/API clients. A token with `AI_ASSESSMENTS_WRITE` may `POST /api/me/ai-assessments`. The source client name, timestamp and privacy/data scope are derived server-side from the authenticated external token; only analysis type, structured content and the private-data indication come from the caller.

The signed-in user can list assessments with `GET /api/me/ai-assessments` and delete one with `DELETE /api/me/ai-assessments/{id}`. Returned assessments are stored separately from measured source facts.
