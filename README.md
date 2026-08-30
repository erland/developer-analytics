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
docker compose --env-file deploy/.env -f deploy/compose.yaml up --build
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
