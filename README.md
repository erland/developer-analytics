# Developer Analytics

Developer Analytics is a self-hosted service for analysing a user's own software-development and open-source history, initially using GitHub as the primary data source.

It provides a private, evidence-based view of:

- repositories and project history,
- activity over time,
- commits and contribution statistics,
- technologies and their evolution,
- project categories and project significance,
- significant self-owned and external projects,
- optional AI-assisted interpretation,
- exportable reports that the user can explicitly choose to share,
- an external analysis API for complementary GPT/AI workflows.

Version 1 is implemented and the original numbered Version 1 implementation plan is complete.

## Install Developer Analytics v1

The supported self-hosted installation uses published GHCR images and Docker Compose. The host only needs Docker/Compose plus GitHub application configuration and deployment secrets; Java, Node.js, PostgreSQL and Nginx run inside containers.

Start here:

- [`docs/installation-v1.md`](docs/installation-v1.md) — installation, first login, private repositories, backup, upgrades and troubleshooting.
- [`docs/operator-v1.md`](docs/operator-v1.md) — containers, health, logs, jobs, recovery, backup, migrations and upgrades.
- [`deploy/compose.release.example.yaml`](deploy/compose.release.example.yaml) — end-user release stack.
- [`deploy/release.env.example`](deploy/release.env.example) — deployment environment template.
- [`deploy/coolify/README.md`](deploy/coolify/README.md) — Coolify-specific deployment guidance.

## Architecture

The current architecture uses:

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

Private repository access is opt-in and privacy provenance is retained through analysis, reporting and external API access.

## Repository structure

```text
.
├── .github/workflows/  # CI, dependency review, Coolify validation and image publication
├── backend/            # Quarkus backend and worker runtime
├── deploy/             # Compose, release and Coolify deployment configuration
├── docs/               # Specifications, API docs, installation and operations
├── frontend/           # React/TypeScript/Vite frontend
├── scripts/            # Build, migration, runtime and acceptance-test helpers
├── LICENSE
└── README.md
```

## Documentation

The main project documentation is:

- [Functional specification](docs/functional-specification.md)
- [Architecture specification](docs/architecture-specification.md)
- [Version 1 development plan](docs/development-plan.md)
- [Installation guide](docs/installation-v1.md)
- [Operator guide](docs/operator-v1.md)
- [Release process](docs/release-process.md)
- [External Analysis API](docs/external-analysis-api.md)
- [GPT/API integration](docs/gpt-api-integration.md)
- [Canonical report model](docs/canonical-report-model.md)

## Continuous integration

`.github/workflows/ci.yml` runs for pull requests, pushes to `main`, manual dispatch and reusable workflow calls.

The current CI baseline includes:

- frontend dependency installation with the committed lockfile and `npm ci`,
- frontend lint, type checking, tests and production build,
- backend Java 21 / Maven verification,
- backend and frontend container builds,
- production dependency auditing,
- Trivy scanning of CI-built images,
- Docker Compose validation,
- runtime smoke testing of built images,
- PostgreSQL backup/restore verification,
- Flyway migration verification,
- end-to-end application smoke testing,
- large-account acceptance testing,
- privacy acceptance testing,
- mobile acceptance coverage.

`.github/workflows/dependency-review.yml` provides dependency review for pull requests.

`.github/workflows/validate-coolify.yml` separately validates the production-relevant Coolify Compose topology, including the external PostgreSQL boundary and internal backend service naming.

## Releases and container images

GitHub Releases are the release trigger and release history.

A published `vMAJOR.MINOR.PATCH` release must point to a commit on `main` that has a successful push CI run. The release workflow then builds and publishes:

```text
ghcr.io/erland/developer-analytics-web
ghcr.io/erland/developer-analytics-backend
```

The worker uses the backend image with a different runtime role.

Published images include OCI metadata, SBOM and provenance. Release verification performs clean GHCR pulls and starts the actual published versioned images through the release Compose stack before the release is considered successful.

See [`docs/release-process.md`](docs/release-process.md) for the authoritative process.

## Deployment

### Standard Docker Compose

The production reference stack is `deploy/compose.yaml`. Local source-build development uses the corresponding local-build override.

For end-user installation, prefer:

- `deploy/compose.release.example.yaml`
- `deploy/release.env.example`

### Coolify

The Coolify deployment lives under `deploy/coolify/` and uses:

- published web/backend GHCR images,
- a separate worker using the backend image,
- an external PostgreSQL service referenced through `DB_HOST`,
- the unique internal backend service name `developer-analytics-backend`.

See [`deploy/coolify/README.md`](deploy/coolify/README.md).

## External analysis and reports

Developer Analytics exposes a scoped External Analysis API for complementary GPT/AI workflows. Access tokens have explicit endpoint scopes and privacy scopes so public-only, aggregate-private and fully authorised analysis can be separated server-side.

Report generation uses a canonical report model shared by Markdown and PDF renderers and requires an explicit privacy-preview/confirmation step before file generation.

Relevant documentation:

- [`docs/external-analysis-api.md`](docs/external-analysis-api.md)
- [`docs/openapi/external-analysis-v1.yaml`](docs/openapi/external-analysis-v1.yaml)
- [`docs/gpt-api-integration.md`](docs/gpt-api-integration.md)
- [`docs/canonical-report-model.md`](docs/canonical-report-model.md)

## Development status

The original numbered Version 1 implementation plan was completed through Step 91. Subsequent changes are normal maintenance, fixes and product evolution rather than an unfinished continuation of Step 15/16-era implementation.

For historical implementation detail, use [`docs/development-plan.md`](docs/development-plan.md) and the Git history rather than this README.
