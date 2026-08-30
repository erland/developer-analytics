# Developer Analytics v1.0.0

Developer Analytics version 1 is the first self-hosted release for analysing a developer's GitHub/open-source history while keeping measured facts, inferred conclusions and private-data handling explicitly separated.

## Features

- GitHub sign-in and user-scoped private application data.
- Repository discovery and incremental/background synchronisation suitable for large accounts.
- Repository inventory with ownership, visibility, filtering and project detail.
- Commit/activity timelines and commit-size statistics.
- Pull request, review and issue contribution statistics.
- Technology catalogue, evidence strength and technology evolution timeline.
- Deterministic project-type classification and project-significance analysis.
- Identification of significant external/open-source projects.
- Explicit authorisation and selection of private repositories.
- Privacy provenance throughout analytics, reports and external API responses.
- Optional AI provider abstraction with Gemini support, project classification and user-level insights.
- User corrections/suppression of inferred classifications.
- Authenticated external `/me/...` analysis API with privacy scopes for GPT/API clients.
- Returned external AI assessments stored separately from measured facts.
- Canonical reports with Markdown and PDF export plus privacy preview/configuration.
- Disconnect, user-data deletion and synchronisation recovery workflows.
- Responsive desktop/mobile interface.
- Docker Compose deployment with PostgreSQL, web, backend and worker containers.
- Health/readiness checks, structured logging, backup/restore tooling and operator documentation.

## Published images

Version 1 publishes:

- `ghcr.io/erland/developer-analytics-web:1.0.0`
- `ghcr.io/erland/developer-analytics-backend:1.0.0`

The release workflow also publishes `1.0`, `1` and `latest` aliases. The worker uses the backend image.

Images are built with OCI source/revision/version metadata plus SBOM and provenance attestations. The workflow anonymously re-pulls and starts the published images before creating the GitHub release/tag.

## Required configuration

A production installation requires at least:

- a GitHub OAuth application/client ID and client secret,
- a callback URL matching the deployment,
- the public frontend URL,
- PostgreSQL credentials,
- a strong credential-encryption key and key version,
- an explicit application version/image tag for reproducible deployment.

For HTTPS deployments, keep secure session cookies enabled. See `docs/installation-v1.md` and `deploy/release.env.example` for the complete configuration.

AI is optional. To enable Gemini, configure the AI provider and Gemini API key/model. The application remains usable without an AI provider.

## Private repositories and privacy

Private repositories are not implicitly included. Users explicitly authorise/select them. Public exports and external clients are filtered according to their privacy scope, and private source content is not automatically sent to an external AI provider.

Operators should review the privacy and external-client settings before enabling private analysis for users.

## Known limitations

- Version 1 supports GitHub as the source-control provider; GitLab is not included yet.
- Historical GitHub coverage depends on what GitHub APIs expose and may not represent every historical event identically.
- Commit attribution can be affected by multiple Git identities/email addresses.
- AI conclusions are optional inference and are stored separately from factual analytics.
- Large histories are intentionally processed incrementally and can be constrained by GitHub API rate limits.
- PDF is generated from the same canonical report model as Markdown, but visual pagination can vary with content length.
- The deployment target for Version 1 is self-hosted Docker Compose; Kubernetes/managed-cloud deployment is not a Version 1 deliverable.

## Upgrade notes

For an existing pre-release installation:

1. Back up PostgreSQL before changing images.
2. Read `docs/postgres-backup-restore.md` and `docs/operator-v1.md`.
3. Set `APP_VERSION=1.0.0` (or the exact desired immutable release tag).
4. Pull the new web/backend images.
5. Start the Compose stack normally.
6. Allow Flyway to apply pending migrations automatically during backend startup.
7. Verify application health/readiness and worker operation before considering the upgrade complete.

Do not edit or remove historical Flyway migrations. Roll back using the documented database backup/restore process if a schema-level rollback is required.

## Validation performed for Version 1

The release pipeline requires the complete CI and acceptance suite to pass before publication, including backend/frontend tests, migrations, container runtime smoke, end-to-end Compose, backup/restore, privacy acceptance, mobile browser acceptance and the 200+ repository large-account acceptance scenario.
