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
