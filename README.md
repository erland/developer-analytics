# Developer Analytics

Developer Analytics is a service for analysing a user's own software-development and open-source history, initially using GitHub as the primary data source.

The service is intended to provide a private, evidence-based view of:

- repositories and project history,
- activity over time,
- commits and contribution statistics,
- technologies and their evolution,
- project categories,
- significant self-owned and external projects,
- optional AI-assisted interpretation,
- exportable reports that the user can explicitly choose to share.

Version 1 is focused on self-analysis rather than public profiling of other people.


## Install Developer Analytics v1

The supported v1 self-hosted installation uses published GHCR images and Docker
Compose. The host only needs Docker/Compose plus GitHub application
configuration and deployment secrets; Java, Node.js, PostgreSQL and Nginx run
inside containers.

Start here:

- [`docs/installation-v1.md`](docs/installation-v1.md) — complete installation,
  first-login, private-repository, backup, upgrade and troubleshooting guide.
- [`docs/operator-v1.md`](docs/operator-v1.md) — container, health, logs,
  jobs, recovery, backup, migrations and upgrade operations.
- [`deploy/compose.release.example.yaml`](deploy/compose.release.example.yaml) —
  end-user release stack.
- [`deploy/release.env.example`](deploy/release.env.example) — deployment
  environment template.

## Planned architecture

The current architecture direction is:

- React + TypeScript frontend,
- Java + Quarkus backend,
- PostgreSQL persistence,
- Flyway database migrations,
- Docker packaging,
- Docker Compose reference deployment,
- Nginx as the external web entry point,
- a separate background worker process for long-running repository ingestion and analysis,
- GitHub as the initial authentication and source-code data provider,
- optional AI enrichment through a provider abstraction,
- an authenticated external API for complementary GPT/AI analysis.

## Repository structure

```text
.
├── .github/
│   └── workflows/      # GitHub Actions workflows
├── backend/            # Quarkus backend (added in later steps)
├── deploy/             # Docker Compose and deployment configuration
├── docs/               # Project specifications and development plan
├── frontend/           # React/TypeScript frontend (added in later steps)
├── scripts/            # Repository/build helper scripts
├── .dockerignore
├── .editorconfig
├── .gitignore
├── LICENSE
└── README.md
```

## Documentation baseline

The following documents are the current functional and architectural baseline for implementation. Changes that materially alter scope or architecture should update the relevant document together with the implementation.

- [Functional specification](docs/functional-specification.md) — version 1 functionality, user flows, privacy, analysis and export requirements.
- [Architecture specification](docs/architecture-specification.md) — target architecture, technology choices, component boundaries, persistence and deployment model.
- [Development plan](docs/development-plan.md) — numbered implementation sequence and acceptance criteria used to build the service incrementally.

## CI baseline fix after first GitHub run

The first GitHub Actions run after Step 15 exposed a frontend TypeScript configuration issue: Vitest's `test` configuration was declared in `vite.config.ts` while `defineConfig` was imported from `vite`. The configuration now imports `defineConfig` from `vitest/config`, allowing TypeScript to recognise the `test` section. The backend validation and Docker Compose validation were already successful in that run.

## Development status

Completed development-plan steps:

1. Initial repository skeleton.
2. Root `.gitignore` and repository hygiene rules.
3. Docker build-context ignore rules.
4. Specifications stored and established as the implementation baseline.
5. React + TypeScript + Vite frontend scaffold.
6. Quarkus/Java backend scaffold.
7. PostgreSQL configuration and first Flyway migration.
8. Initial Nginx configuration for SPA hosting and `/api/` reverse proxying.
9. Production-oriented Dockerfiles for the web/frontend and Quarkus backend images.
10. Initial Docker Compose stack with Nginx/web, Quarkus backend and persistent PostgreSQL.
11. Initial GitHub Actions validation workflow for frontend and backend.
12. Docker and Compose build validation in CI.
13. GHCR publication workflow for release container images.
14. SBOM and provenance attestations for published container images.
15. CI, release and GHCR publication behaviour documented.

Step 15 is complete. The repository now has a documented CI/CD baseline covering pull-request validation, `main` validation, release image publication, GHCR package naming, image tagging and supply-chain metadata.

The next planned step is **Step 16 – Define core domain identifiers**.

## Continuous integration

The repository has two distinct GitHub Actions workflows so source validation and release publication remain separate concerns.

### CI validation – `.github/workflows/ci.yml`

The validation workflow runs for:

- every pull request,
- every push to `main`,
- manual `workflow_dispatch`.

It contains three independent jobs:

| Job | Validation | Publishes images? |
|---|---|---|
| Frontend validation | dependency installation, ESLint, TypeScript typecheck, Vitest, Vite production build | No |
| Backend validation | Java 21 setup and Maven `verify`, including Quarkus tests | No |
| Container build validation | Docker Compose configuration plus web/backend image builds with Buildx | No |

PR and `main` CI therefore prove that the source, tests and distributable container definitions build successfully without modifying GHCR. Concurrency cancellation is enabled so superseded CI runs for the same branch/ref do not waste capacity.

The frontend currently has no committed `package-lock.json` because the development environment used for the initial scaffold could not reach the npm registry. CI therefore uses `npm install` temporarily. Once a lockfile can be generated from the pinned direct dependencies, it should be committed and the workflow changed to `npm ci` as required by the development plan.

### Release publication – `.github/workflows/publish-images.yml`

Container publication is deliberately separate from pull-request validation. The workflow runs when a GitHub Release is published and may also be run manually for publication testing.

For the intended repository `github.com/erland/developer-analytics`, the package names resolve dynamically to:

```text
ghcr.io/erland/developer-analytics-web
ghcr.io/erland/developer-analytics-backend
```

No owner name is hard-coded in the workflow; `${{ github.repository_owner }}` is used so forks remain internally consistent.

A release publishes:

- the complete semantic version, for example `1.0.0`,
- the major/minor version, for example `1.0`,
- `latest` for stable releases only,
- a `sha-...` tag for source traceability.

Prereleases do not update `latest`. Manual workflow runs publish the SHA-based tag and can therefore be used to verify GHCR publication before a formal release.

The workflow authenticates to GHCR using the GitHub-provided `GITHUB_TOKEN` with `packages: write`. A separate long-lived container-registry password is not required.

Each release image also receives OCI metadata, BuildKit SBOM information and build provenance (`mode=max`). These attestations travel with the published image and provide traceability from the image back to its build inputs.

### Expected status before merge/release

Before merging a normal change, the expected quality gate is that all three CI jobs are green. Before considering a release successful, both GHCR images must build and publish successfully. Later development-plan steps add runtime smoke verification of the published images.

## Frontend development

The initial React + TypeScript + Vite application lives in `frontend/`. See `frontend/README.md` for commands.


## Docker Compose baseline

The current stack can be started from the repository root with:

```bash
cp deploy/env.example deploy/.env
docker compose --env-file deploy/.env -f deploy/compose.yaml pull
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d
```

Replace the example database password before starting the stack. The application is then exposed through Nginx on `http://localhost:8080` by default. PostgreSQL and the backend are not published directly to the host. See [`deploy/README.md`](deploy/README.md) for details.


## Development status – Step 22

Step 22 is complete: GitHub sign-in, PKCE/state handling, service-owned browser sessions and logout are implemented. During this step the cumulative Step 16–21 backend package model and Docker Compose worker topology were also rebuilt from the last green CI baseline to remove inconsistencies discovered before authentication was added.

Next: **Step 23 – Enforce `/me`-style user scoping**.


## Development status – Step 23

Step 23 is complete: `/api/me/...` is now the primary user-scoping boundary. The backend derives the internal user from the server-side session and repository lookups include the current user ID, preventing cross-user object access.

Next: **Step 24 – Account connection management**.


## Development status – Step 24

Step 24 is complete: authenticated users can inspect and manage their provider connections under `/api/me/connections`, with provider-neutral lifecycle handling and cross-user isolation.

Next: **Step 25 – GitHub provider adapter**.


## Development status – Step 25

Step 25 is complete: a provider-neutral source-control adapter boundary is available, with GitHub as the first implementation and normalized repository, user, pagination and rate-limit data.

Next: **Step 26 – Store GitHub connection credentials securely**.


## Development status – Step 26

Step 26 is complete: provider access credentials are encrypted at rest with AES-256-GCM, keyed from deployment secrets, versioned for future rotation and cleared on provider disconnect.

Next: **Step 27 – Initial GitHub repository discovery job**.


## Development status – Step 27

Step 27 is complete: GitHub repository discovery now runs as a persistent worker job, follows provider paging and upserts the authenticated user's repository inventory.

Next: **Step 28 – Repository discovery progress and sync-state handling**.


## Development status – Step 28

Step 28 is complete: repository discovery now persists progress, sync status, counters, errors and provider rate-limit state, with `/api/me/sync-runs` endpoints for frontend status views.

Next: **Step 29 – Initial contribution discovery**.


## Development status – Step 29

Step 29 is complete: GitHub commit and pull-request discovery can now be queued per repository and normalized into the persistent contribution model with deduplication.

Next: **Step 30 – Extend contribution discovery with reviews and issues**.


## Development status – Step 30

Step 30 is complete: GitHub contribution discovery now includes pull-request reviews and issues in addition to commits and pull requests, normalized into the common contribution model.

Next: **Step 31 – Contribution discovery orchestration across repositories**.


## Development status – Step 31

Step 31 is complete: contribution discovery can now be orchestrated across the repository inventory in bounded, deduplicated batches, and repository discovery automatically seeds the first batch.

Next: **Step 32 – Contribution sync progress and completion state**.


## CI correction after Step 31

Two CI-only configuration problems were corrected:

- backend tests now use non-production GitHub OAuth test values so Quarkus can boot without repository secrets,
- Docker Compose validation receives a deterministic CI-only credential encryption key.

No real GitHub App client secret or production credential-encryption key is required for pull-request validation. Real values are only required when running/deploying the application with GitHub sign-in and repository access enabled.


## CI correction – Hibernate JSON mapping

Backend CI reached Flyway successfully but Quarkus stopped while building the Hibernate SessionFactory because REST/Jackson formatting was being reused for JSON database columns.

The backend now sets:

`quarkus.hibernate-orm.mapping.format.global=ignore`

This follows Quarkus' recommended behavior when the application does not need REST-specific JSON customization for Hibernate JSON columns, keeping persistence serialization independent from the REST ObjectMapper.


## CI correction – committed REST test fixtures

After the Hibernate JSON fix, Quarkus started correctly and all Flyway migrations succeeded. Three REST tests still returned `401` because their session fixtures were created in an uncommitted test transaction while the HTTP request executed in a separate transaction.

A dedicated `TestFixtureService` now creates authentication/repository/connection fixtures in `REQUIRES_NEW` transactions, committing them before RestAssured calls the API. This matches real request visibility and preserves the intended `/me` authentication tests.


## CI correction – provider identity fixture reuse

The final failing backend test created two GitHub `ProviderIdentity` rows for the same application user, conflicting with the model's unique `(user_id, provider)` constraint.

The connection test fixture now reuses the user's existing GitHub identity and only creates one when none exists. This matches the production identity model.


## Development status – Step 32

Step 32 is complete: contribution discovery now persists per-repository progress, completion/error state and provider rate-limit information, exposed through `/api/me/contribution-sync-runs`.

Next: **Step 33** according to the development plan.


## Development status – Step 33

Step 33 is complete: the backend now has a provider-neutral technology catalogue with stable keys, categories, aliases and evidence definitions for language-, file- and manifest-based detection.

Next: **Step 34 – Implement Language-Based Evidence**.


## Development status – Step 34

Step 34 is complete: GitHub repository language statistics are now collected as measured `LANGUAGE` evidence and mapped deterministically to the technology catalogue. The system records observations, not expertise claims.

Next: **Step 35 – Implement File/Manifest-Based Detection**.


## Development status – Step 35

Step 35 is complete: selected repository files and manifests are now inspected deterministically and stored as separate `FILE` and `MANIFEST` technology evidence. Raw source repositories are not cloned or persisted for this analysis.

Next: **Step 36 – Calculate Technology Evidence Strength**.


## Development status – Step 36

Step 36 is complete: repository technology evidence can now be aggregated into transparent user-level labels `EXPOSURE`, `LIMITED`, `MODERATE` and `STRONG`, with an explainable numerical score and rationale.

Next: **Step 37** according to the development plan.


## Development status – Step 37

Step 37 is complete: technology evidence and contribution history are now combined into chart-ready timelines with first/latest observation, yearly project/activity counts and separate public/private repository evidence.

Next: **Phase 9 – Project Classification and Significance**, beginning with Step 38.


## Development status – Step 38

Step 38 is complete: project classification now has a data-driven multi-category taxonomy rather than a fixed enum. The schema also supports multiple category assignments per repository with source, confidence and rationale.

Next: **Step 39** according to the development plan.


## Development status – Step 39

Step 39 is complete: repositories can now be classified deterministically from topics, technology evidence, known files/structure and repository metadata. Every assignment is multi-category capable and stores its score, confidence and exact supporting signals.

A missing `enqueueFileManifestEvidence` method from the earlier step-35 implementation was also restored while consolidating the job service.

Next: **Step 40** according to the development plan.


## Development status – Step 40

Step 40 is complete: project significance and user involvement are now calculated and stored as two separate, explainable assessments. Ranking can use both dimensions without collapsing them into a single opaque score.

Next: **Step 41** according to the development plan.


## Development status – Step 41

Step 41 is complete: significant external projects can now be queried directly when project significance, user involvement or both are high. API responses preserve the full evidence for each dimension and explain which condition caused the project to be surfaced.

Next: **Phase 10 – Dashboard and Responsive UX**, beginning with Step 42.


## Development status – Step 42

Step 42 is complete: the frontend now has a responsive authenticated application shell with session state, loading/error handling, data freshness status, desktop navigation and a mobile navigation drawer.

Next: **Step 43** according to the development plan.


## Development status – Step 43

Step 43 is complete: the authenticated Overview now presents repository, activity, technology, project-category and significant-project summaries using the existing API surface, with responsive cards designed for phone-sized screens.

Next: **Step 44** according to the development plan.


## Development status – Step 44

Step 44 is complete: Activity now provides period-filtered commit trends, active projects, average/median commit size and additions/deletions through a user-scoped API and responsive dashboard views.

Next: **Step 45** according to the development plan.


## Development status – Step 45

Step 45 is complete: Projects now provides a paged, searchable and filterable inventory supporting own/external, public/private, active/inactive, project category and technology filters, with a mobile-adaptive card layout.

Next: **Step 46** according to the development plan.


## Development status – Step 46

Step 46 is complete: the Projects section now opens a consolidated project detail view covering metadata, activity, technologies/evidence, categories, user involvement, project significance and synchronisation state.

Next: **Step 47** according to the development plan.


## Development status – Step 47

Step 47 is complete: Technologies now provides a consolidated evidence-based view with project counts, first/latest use, activity timeline and representative projects, while retaining the existing non-proficiency semantics.

Next: **Step 48** according to the development plan.


## Development status – Step 48

Step 48 is complete: Project types now shows project counts per category, contribution activity, category evolution over time and representative projects through a responsive category view.

Next: **Phase 11 – Private Repository Support**, beginning with Step 49.


## Development status – Step 49

Step 49 is complete: private GitHub repository access is opt-in, separately authorised, recorded as an explicit provider-connection permission and enforced server-side during repository discovery. Normal sign-in remains public-data-only.

Next: **Step 50** according to the development plan.

## Development status – Step 50

Step 50 is complete: derived statistics and assessments retain privacy provenance with propagation tests and API/UI exposure.

Next: **Step 51** according to the development plan.

## Development status – Step 51

Step 51 is complete: authorised private repositories are inspectable and individually opt-in for analysis, with refresh and remove-from-analysis controls. Newly discovered private repositories default to excluded.

Next: **Step 52** according to the development plan.


## Development status – Step 52

Step 52 is complete: report export now requires explicit per-export controls for excluding private data, including private aggregates, including full private project detail, and hiding or showing private repository names. The backend rejects requests that omit either privacy setting.

Next: **Phase 12 – AI-Assisted Analysis**, beginning with Step 53.


## Development status – Step 53

Step 53 is complete: a vendor-independent AI provider abstraction and application gateway now exist for project classification, project summaries, technology normalisation, role inference and technology-history summaries. The default provider is disabled and returns no AI result, preserving all core deterministic functionality without external AI configuration.

Next: **Step 54** according to the development plan.


## Development status – Step 54

Step 54 is complete: Gemini is available as the first optional `AiProvider` implementation. It is enabled only through environment configuration, uses structured JSON outputs for all five AI operations, records request type/result/token usage without logging prompt content, and degrades to the disabled provider when no Gemini secret is configured.

Next: **Step 55** according to the development plan.


## Development status – Step 55

Step 55 is complete: every AI request now requires explicit sensitivity metadata and passes a three-way privacy decision using source sensitivity, per-user consent and deployment-level provider policy. Private repository content is never automatically sent to Gemini; private metadata requires explicit user consent plus provider permission.

Next: **Step 56** according to the development plan.


## Development status – Step 56

Step 56 is complete: AI project classification now complements deterministic classification, persists classification/confidence/explanation/version/provider/model/privacy metadata, fingerprints its inputs and reuses unchanged results rather than repeating AI calls.

Next: **Step 57** according to the development plan.


## Development status – Step 57

Step 57 is complete: Developer Analytics can now generate optional user-level AI insights covering likely roles, technical focus, breadth/depth, technology evolution and open-source engagement. Insights are persisted, fingerprinted/reused when unchanged, privacy-gated and clearly labelled as AI-generated rather than measured facts.

Next: **Step 58** according to the development plan.


## Development status – Step 58

Step 58 is complete: user corrections are stored as a separate reversible layer, allowing incorrect project categories to be rejected, technology inferences to be suppressed and projects to be excluded from AI profile conclusions without altering source facts.

Next: **Phase 13 – External GPT Integration**, beginning with Step 59.


## Development status – Step 59

Step 59 is complete: the External Analysis API v1 contract defines compact LLM-oriented `/api/me/profile`, `/projects`, `/activity`, `/technologies`, `/project-types`, `/contributions` and `/evidence` representations. The contract uses an explicit versioned media type so dashboard payloads remain independent.

Next: **Step 60** according to the development plan.


## Development status – Step 60

Step 60 is complete: users can create scoped external-client bearer tokens, see their metadata, revoke them, and use them independently of browser sessions and GitHub credentials. External Analysis API v1 now requires a dedicated token with the endpoint-specific read scope.

Next: **Step 61** according to the development plan.


## Development status – Step 61

Step 61 is complete: external GPT/API tokens now carry an explicit server-enforced privacy scope (`PUBLIC_ONLY`, `PUBLIC_PLUS_PRIVATE_AGGREGATES` or `FULL_AUTHORISED_ANALYSIS`) in addition to endpoint read scopes. Public-only is the default and private project detail requires the full authorised scope.

Next: **Step 62** according to the development plan.


## Development status – Step 62

Step 62 is complete: external clients can return structured AI assessments through a dedicated scoped write permission. Assessments retain analysis type, authenticated source/client, server timestamp, token data/privacy scope, structured content and an explicit private-data indication. Users can list and delete returned assessments without changing source facts.

Next: **Step 63 – Publish GPT/API Documentation**.


## GPT/API documentation

Phase 13 integration documentation is published in-repository:

- [`docs/external-analysis-api.md`](docs/external-analysis-api.md) — compact API contract and privacy semantics.
- [`docs/openapi/external-analysis-v1.yaml`](docs/openapi/external-analysis-v1.yaml) — machine-readable OpenAPI 3.1 contract.
- [`docs/gpt-api-integration.md`](docs/gpt-api-integration.md) — authentication, scopes, privacy rules and integration workflow.
- [`docs/openapi/custom-gpt-action-example.yaml`](docs/openapi/custom-gpt-action-example.yaml) — sample Custom GPT Action schema.
- [`docs/examples/external-analysis-calls.sh`](docs/examples/external-analysis-calls.sh) — runnable curl examples.
- [`docs/examples/ai-assessment-example.json`](docs/examples/ai-assessment-example.json) — example returned assessment payload.

## Development status – Step 63

Step 63 is complete: the External Analysis API now has published OpenAPI 3.1 documentation, authentication and privacy-scope instructions, copyable API examples and a sample Custom GPT Action schema.

Next: **Phase 14 – Reports and Export**, beginning with Step 64.


## Canonical report model

Report content is now defined independently of output format. See
[`docs/canonical-report-model.md`](docs/canonical-report-model.md).

## Development status – Step 64

Step 64 is complete: report content now flows through the versioned
`CanonicalReport` model containing summary, period, coverage, project
categories, technology analysis, activity, significant projects, role/AI
assessment, methodology and privacy scope. Markdown is a renderer of that model
rather than an independent report implementation.

Next: **Step 65** according to the development plan.


## Markdown report exports

The first canonical output format now supports public OSS, full developer,
technology-profile and activity reports. See
[`docs/markdown-reports.md`](docs/markdown-reports.md).

## Development status – Step 65

Step 65 is complete: Markdown export now offers four canonical report variants,
all rendered from `CanonicalReport`, with methodology and data-coverage sections
in every variant. The public OSS report is forced to public-only data server-side.

Next: **Step 66** according to the development plan.


## Development status – Step 66

Step 66 is complete: report generation now uses a mandatory privacy-preview
stage showing private repository inclusion, private-name inclusion, AI
assessment inclusion, effective privacy scope, analysed time range and coverage.
The preview creates no file; export requires a separate explicit generation
action and server-side `generationConfirmed=true`.

Next: **Step 67** according to the development plan.


## Development status – Step 67

Step 67 is complete: PDF export now renders from the same `CanonicalReport` and
shared section plan as Markdown. The A4 renderer uses wrapping card layouts for
wide analytical tables, a print-native activity chart, independent print layout
rather than dashboard CSS, and persistent privacy markings on every page.

Next: **Step 68** according to the development plan.


## Development status – Step 68

Step 68 is complete: export tests now verify public-only private-name isolation,
aggregate-private masking/no-detail behavior, full-private inclusion only for
repositories explicitly included in analysis, and core-content parity between
Markdown and rendered PDF including PDF privacy markings.

Next: **Phase 15 – Data Lifecycle and Operational Hardening**, beginning with
Step 69.


## Development status – Step 69

Step 69 is complete: GitHub disconnect now cancels queued provider jobs, removes
the stored provider credential and private-repository authorisation, blocks
future credential retrieval, and requires an explicit choice to preserve or
remove previously analysed GitHub data.

Next: **Step 70** according to the development plan.


## Development status – Step 70

Step 70 is complete: users can permanently delete all Developer Analytics
account data through one explicit, transactional deletion boundary rooted at
`app_user`. Database cascades remove provider connections, sessions,
repositories, contributions, aggregates, evidence, classifications, AI data,
external-client tokens and background jobs. A realistic relational integration
test verifies the cascade graph.

Next: **Step 71** according to the development plan.


## Development status – Step 71

Step 71 is complete: persistent jobs now recover stale `RUNNING` locks after
worker/backend interruption, transient GitHub/network failures retry with bounded
exponential backoff, lost provider permission becomes an explicit
`ACCESS_REVOKED`/connection-error state, and optional AI outages degrade to no AI
result instead of breaking deterministic analysis. Manual recovery/retry actions
are also available.

Next: **Step 72** according to the development plan.


## Development status – Step 72

Step 72 is complete: HTTP requests now have propagated correlation IDs,
background-worker logs carry job IDs, repository/contribution sync logs carry
persisted sync IDs, and operational failures use sanitized structured
`key=value` events. Tokens, credentials, authorization values, private source
content, prompts and diffs are explicitly excluded from the structured log
contract.

Next: **Step 73** according to the development plan.


## Development status – Step 73

Step 73 is complete: SmallRye liveness/readiness now covers the backend process,
database connectivity and Flyway migration state. Worker processes persist a
heartbeat for operational status, while optional Gemini availability is
reported separately and never makes the service unhealthy by itself.

Next: **Step 74** according to the development plan.


## Development status – Step 74

Step 74 is complete: self-hosted PostgreSQL backup/restore is documented and
supported by repository scripts for custom-format `pg_dump`, checksum-verified
restore, and a real restore verification that checks account data, repository
inventory, aggregates, AI analysis metadata and Flyway history.

Next: **Step 75** according to the development plan.


## Development status – Step 75

Step 75 is complete: backend tests are now explicitly classified with JUnit 5
tags into unit, persistence, GitHub-adapter, authorization, worker/job and
privacy layers. Maven profiles can run each layer independently, while the
default `mvn verify` still runs the complete suite. CI also rejects new untagged
backend tests or an accidentally empty required layer.

Next: **Step 76** according to the development plan.


## Development status – Step 76

Step 76 is complete: frontend tests now have explicit component, feature/page,
responsive, API-error-state and privacy layers. Each layer has focused tests and
can be run independently, while the existing `npm test` command remains the
authoritative complete frontend suite. CI verifies that every required layer
continues to contain tests.

Next: **Step 77** according to the development plan.


## Development status – Step 77

Step 77 is complete: CI now runs a Compose end-to-end smoke path across the
built frontend, Nginx, backend and PostgreSQL. It verifies frontend delivery,
proxied API health, successful Flyway migrations and a representative
authenticated session flow without requiring live GitHub OAuth.

Next: **Step 78** according to the development plan.


## Development status – Step 78

Step 78 is complete: CI now has dedicated Flyway migration verification. It
checks migration naming/order/continuity, verifies the full migration history in
the backend persistence test, and boots the real backend against an isolated
fresh PostgreSQL volume so broken migrations and fresh-database startup failures
fail explicitly.

Next: **Step 79** according to the development plan.


## Development status – Step 79

Step 79 is complete: the container CI job now loads the exact frontend/backend
images it builds, starts the full Compose runtime with `--no-build`, waits for
health, verifies the proxied application API and static frontend, confirms
db/backend/worker/web are running, and shuts the stack down cleanly.

Next: **Step 80** according to the development plan.


## Development status – Step 80

Step 80 is complete: the project now has a pragmatic automated security
baseline with GitHub dependency review, production npm auditing, Trivy scanning
of the exact CI-built container images, weekly Dependabot updates for npm,
Maven, Docker and GitHub Actions, plus documented GitHub-native secret-scanning
and push-protection settings.

Next: **Step 81** according to the development plan.


## Development status – Step 81

Step 81 is complete: `deploy/compose.yaml` is now the production reference and
uses version-selectable GHCR images for web/backend/worker rather than requiring
source builds on the deployment host. A separate
`deploy/compose.local-build.yaml` override preserves source-build workflows for
development and CI.

Next: **Step 82** according to the development plan.


## Development status – Step 82

Step 82 is complete: the repository now includes a copy-and-configure release
Compose example plus an end-user environment template and quickstart. A release
host only needs Docker/Compose, GitHub application configuration, required
secrets and optional Gemini configuration; Java, Node.js, PostgreSQL and Nginx
remain containerized.

Next: **Step 83** according to the development plan.


## Development status – Step 83

Step 83 is complete: every published release now has a post-publication GHCR
verification job that removes cached images, logs out of GHCR, performs
anonymous clean pulls, checks semantic version tags, confirms the worker uses
the backend image, and inspects image configuration/history for accidentally
embedded runtime secrets. The intended GHCR package visibility is explicitly
documented as public.

Next: **Step 84** according to the development plan.


## Development status – Step 84

Step 84 is complete: after GHCR publication and clean-pull verification, the
release workflow now starts the actual published versioned images using the
end-user release Compose file with `--no-build`. It waits for health, verifies
the frontend, proxied backend/API, PostgreSQL/Flyway state and worker image, then
requires a clean shutdown. A release now fails if its published images cannot
actually run together.

Next: **Step 85** according to the development plan.

## Development status – Step 85

Step 85 is complete: Developer Analytics v1 now has a single installation guide
covering prerequisites, GitHub application setup, environment and optional AI
configuration, Compose startup, first login, private repository authorisation,
backup, upgrades and troubleshooting. The root README points new installers
directly to this v1 path.

Next: **Step 86** according to the development plan.


## Development status – Step 86

Step 86 is complete: Developer Analytics v1 now has a dedicated operator guide
covering containers, volumes, ports, health checks, logs, persistent jobs,
synchronisation recovery, database backup, Flyway migration behaviour and image
upgrades/rollback considerations.

Next: **Step 87** according to the development plan.


## Version 1 large-account acceptance

The v1 release gate includes a deterministic 240-repository acceptance scenario
covering incremental enrichment, API responsiveness, paged/filterable project
inventory, worker restart recovery and rate-limit presentation. See
[`docs/large-account-acceptance-v1.md`](docs/large-account-acceptance-v1.md).


## Development status – Step 87

Step 87 is complete: v1 now has a mandatory Docker/Compose acceptance scenario
with 240 repositories, partial enrichment, bounded API response checks,
pagination/filtering, frontend availability before enrichment completion,
worker-restart recovery and explicit rate-limit state verification.

Next: **Step 88** according to the development plan.


## Version 1 privacy acceptance

The v1 release gate includes an end-to-end privacy matrix covering cross-user
isolation, public/private/excluded repositories, private-provenance AI,
public/private reports and three external GPT privacy scopes. See
[`docs/privacy-acceptance-v1.md`](docs/privacy-acceptance-v1.md).


## Development status – Step 88

Step 88 is complete: v1 now has a mandatory Compose privacy acceptance scenario
that verifies no private information crosses into another user's view, public
exports, unauthenticated APIs or insufficiently scoped GPT clients, while still
allowing explicitly authorised private report/GPT paths.

Next: **Step 89** according to the development plan.


## Version 1 mobile acceptance

The v1 release gate includes a real Chromium/iPhone-sized acceptance scenario
covering login, dashboard navigation, activity charts, projects and filters,
technologies, AI insights, report privacy configuration, privacy/data sources
and account controls. See
[`docs/mobile-acceptance-v1.md`](docs/mobile-acceptance-v1.md).


## Development status – Step 89

Step 89 is complete: v1 now has a Playwright/Chromium phone-sized acceptance
test that exercises every primary mobile flow and checks that essential
functionality does not depend on wide tables or page-level horizontal scrolling.



## Release process

Developer Analytics uses ordinary **GitHub Releases** as the single release history and
release trigger. CI runs for every pull request and every push to `main`. After the exact
release commit has passed the complete `main` CI suite, create a GitHub Release with a
`vMAJOR.MINOR.PATCH` tag from that commit and use GitHub **Generate release notes**.
Publishing the release automatically validates the tag and successful `main` CI, builds and
publishes versioned GHCR web/backend images, and verifies a clean installation from them.
See [`docs/release-process.md`](docs/release-process.md).

## Development status – Step 91

Step 91 completed the original Version 1 implementation plan. The release mechanism has
since been simplified: the dedicated Version 1/RC workflows and checked-in release notes
have been replaced by the GitHub Release driven process described above.

## Repository analysis orchestration

A successful GitHub sign-in now queues repository discovery automatically. After discovery,
Developer Analytics queues the full deterministic analysis pipeline for every repository that
is included in analysis: contributions, language evidence, file/manifest evidence and project
classification, followed by technology strength, technology timeline and project significance
recalculation.

Private repository access remains opt-in. Once authorised, the Privacy/data sources view can
include or exclude all private repositories at once, or apply the selection to repositories
whose name/full name starts with a prefix. Including a private repository queues its analysis.
Each project detail page also provides **Refresh repository analysis** for a targeted re-analysis.

Provider recovery is repository-safe: only GitHub HTTP 401 is treated as global credential loss.
A missing repository (404) or a repository-specific/temporary forbidden response (403) no longer
marks every repository as `ACCESS_REVOKED`. A successful GitHub discovery validates the provider
connection again and restores discovered repositories to `SYNCED`.
