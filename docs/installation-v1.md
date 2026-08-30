# Developer Analytics v1 — Installation Guide

This guide describes the supported self-hosted installation path for Developer
Analytics v1.

The recommended deployment uses published container images from GitHub
Container Registry together with Docker Compose. The host does not need Java,
Maven, Node.js, npm, PostgreSQL or Nginx installed separately.

## 1. Prerequisites

You need:

- a host capable of running Docker,
- Docker Engine,
- Docker Compose v2,
- a DNS name and HTTPS termination for production use,
- permission to create/configure the GitHub application used for sign-in,
- access to the Developer Analytics release deployment files.

Recommended host preparation:

```bash
docker version
docker compose version
```

For production, also make sure:

- the host has persistent storage for Docker volumes,
- backups can be copied off-host,
- outbound HTTPS access is available to GitHub,
- outbound HTTPS access to Gemini is available only if Gemini is enabled.

You do not need host installations of:

- Java,
- Maven,
- Node.js/npm,
- PostgreSQL,
- Nginx.

## 2. GitHub application setup

Developer Analytics uses GitHub OAuth for interactive sign-in and GitHub API
access.

Create or configure the GitHub application/OAuth configuration for the
installation and record:

```text
GITHUB_CLIENT_ID
GITHUB_CLIENT_SECRET
```

Set the callback URL to the externally reachable Developer Analytics URL:

```text
https://developer-analytics.example.com/api/auth/github/callback
```

The same URL must be configured as:

```text
GITHUB_CALLBACK_URL
```

Set:

```text
FRONTEND_URL=https://developer-analytics.example.com/
```

The callback URL configured in GitHub must match the deployed callback URL
exactly, including scheme, hostname, port when non-standard, and path.

Normal sign-in does not automatically grant access to private repositories.
Private-repository access is a separate explicit user action inside Developer
Analytics.

## 3. Environment configuration

Start from the release environment template:

```bash
cp deploy/release.env.example deploy/release.env
```

### Release selection

Set an explicit published version:

```text
APP_VERSION=1.0.0
```

The default registry images are:

```text
WEB_IMAGE=ghcr.io/erland/developer-analytics-web
BACKEND_IMAGE=ghcr.io/erland/developer-analytics-backend
```

Pin an explicit release version for production rather than relying on `latest`.

### Database

Configure:

```text
DB_NAME=developer_analytics
DB_USERNAME=developer_analytics
DB_PASSWORD=<strong-random-password>
```

PostgreSQL runs in the Compose stack and stores data in a persistent Docker
volume.

### Credential encryption

Developer Analytics encrypts stored provider credentials. Configure a
Base64-encoded 32-byte key:

```bash
openssl rand -base64 32
```

Then set:

```text
CREDENTIAL_ENCRYPTION_KEY=<generated-value>
CREDENTIAL_KEY_VERSION=v1
```

Back up the encryption key outside the database and outside the repository.
Restored encrypted GitHub credentials cannot be decrypted without the same key.

### Session configuration

For HTTPS production deployments:

```text
SESSION_HOURS=8
SESSION_COOKIE_SECURE=true
```

For local HTTP-only testing, `SESSION_COOKIE_SECURE=false` may be needed.

### Public port

The release example exposes Nginx through:

```text
WEB_PORT=8080
```

If a reverse proxy or load balancer is used in front of the Compose host, it
should forward HTTPS traffic to that port.

## 4. Optional AI configuration

Developer Analytics works without AI.

Default:

```text
AI_PROVIDER=disabled
```

To enable Gemini:

```text
AI_PROVIDER=gemini
GEMINI_API_KEY=<api-key>
GEMINI_MODEL=gemini-2.5-flash-lite
GEMINI_BASE_URL=https://generativelanguage.googleapis.com/v1beta
```

The deployment-level private-data policy is configured separately:

```text
AI_PRIVATE_DATA_POLICY=PUBLIC_ONLY
```

Keep `PUBLIC_ONLY` unless private metadata has been explicitly reviewed and
approved for the selected provider/deployment.

Private source content is not intended to be sent automatically to external AI
providers.

## 5. Start with Docker Compose

The end-user release Compose file is:

```text
deploy/compose.release.example.yaml
```

Pull the selected images:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   pull
```

Start the application:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   up -d
```

Check status:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   ps
```

Expected result:

- `db` becomes healthy,
- `backend` becomes healthy,
- `web` becomes healthy,
- `worker` remains running.

Verify application health:

```bash
curl -fsS http://localhost:8080/api/health/application
```

Use the externally published HTTPS URL instead when applicable.

## 6. First login

Open the Developer Analytics URL in a browser.

Choose GitHub sign-in and complete the GitHub OAuth flow.

After a successful first login:

- the application creates the local account,
- the GitHub identity is associated with that account,
- the provider credential is stored encrypted,
- the initial repository discovery can run through the worker,
- the dashboard becomes the primary application view.

If login fails, verify the GitHub callback URL and the configured
`GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` first.

## 7. Private repository authorisation

Private repository access is intentionally separate from normal login.

In Developer Analytics:

1. open **Privacy/data sources**,
2. choose the GitHub private-repository authorisation action,
3. approve the requested GitHub permission,
4. return to Developer Analytics,
5. refresh/discover the authorised private repositories,
6. explicitly select which private repositories may be included in analysis.

Authorising GitHub access does not automatically include every private
repository in analysis.

Newly discovered private repositories remain excluded until explicitly selected.

Private-data export and AI behavior are controlled separately from repository
authorisation.

## 8. Backup

The PostgreSQL database is the authoritative persistent store.

Before upgrades and on a regular schedule, run:

```bash
bash ./scripts/postgres-backup.sh
```

Backups are created in custom PostgreSQL dump format together with a checksum.

Recommended starting retention:

- 7 daily backups,
- 4 weekly backups,
- at least one copy outside the Docker host.

Also back up separately:

```text
CREDENTIAL_ENCRYPTION_KEY
CREDENTIAL_KEY_VERSION
```

Do not store those values inside the database backup.

See:

- `docs/postgres-backup-restore.md`

## 9. Upgrade

Before upgrading:

1. read the release notes,
2. create and verify a database backup,
3. retain the currently deployed `APP_VERSION`,
4. change `APP_VERSION` to the new release.

Then run:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   pull

docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   up -d
```

Flyway applies required schema migrations when the backend starts.

Verify:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   ps
```

and:

```bash
curl -fsS https://developer-analytics.example.com/api/health/application
```

Do not assume database migrations are reversible. If a release introduces an
incompatible schema change, restoring a previous application image alone may
not be sufficient; use the documented database restore procedure when needed.

## 10. Troubleshooting

### Images cannot be pulled

Check the selected version:

```text
APP_VERSION
```

and verify the release exists.

The Developer Analytics GHCR packages are intended to be public. If Docker asks
for credentials unexpectedly, check GHCR package visibility.

Inspect:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   pull
```

### Backend does not become healthy

Show logs:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   logs backend db
```

Common causes:

- incorrect database password,
- invalid `CREDENTIAL_ENCRYPTION_KEY`,
- failed Flyway migration,
- database volume/storage problem.

### GitHub login fails

Check:

- `GITHUB_CLIENT_ID`,
- `GITHUB_CLIENT_SECRET`,
- `GITHUB_CALLBACK_URL`,
- `FRONTEND_URL`,
- the callback URL configured in GitHub.

For production, verify that the externally visible URL uses HTTPS and
`SESSION_COOKIE_SECURE=true`.

### User signs in but repositories do not appear

Check worker status:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   ps worker
```

Then inspect:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   logs worker backend
```

Repository discovery can also be affected by GitHub rate limits or revoked
provider access.

### Private repositories are missing

Confirm that:

1. private repository access was authorised separately,
2. the GitHub grant still permits access,
3. repository permissions were refreshed,
4. the specific private repositories were selected for analysis.

### Gemini is not used

Confirm:

```text
AI_PROVIDER=gemini
GEMINI_API_KEY=<non-empty>
```

Then inspect backend/worker logs for AI provider configuration or privacy-policy
decisions.

Core analytics should continue to work when Gemini is unavailable or disabled.

### Application returns 502/503

Check all service states:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   ps
```

Then inspect:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   logs web backend db worker
```

### Database restore is needed

Follow:

- `docs/postgres-backup-restore.md`

Do not delete the PostgreSQL volume before confirming that a usable backup
exists.

## Installation reference files

The v1 installation path uses:

- `deploy/compose.release.example.yaml`
- `deploy/release.env.example`
- `docs/release-compose-quickstart.md`
- `docs/production-compose.md`
- `docs/postgres-backup-restore.md`
- `docs/ghcr-release-verification.md`
- `docs/automated-release-verification.md`
