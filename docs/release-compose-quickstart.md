# Release Compose Quickstart

This is the end-user deployment path for Developer Analytics. No source build
toolchain is required.

## Requirements

Install only:

- Docker,
- Docker Compose.

You do **not** need to install:

- Java,
- Maven,
- Node.js/npm,
- PostgreSQL,
- Nginx.

Those run inside containers.

## 1. Download the release deployment files

Use:

- `deploy/compose.release.example.yaml`
- `deploy/release.env.example`

Copy the environment example:

```bash
cp deploy/release.env.example deploy/release.env
```

## 2. Configure the release version

Set an explicit published version:

```text
APP_VERSION=1.0.0
```

The default image repositories are:

```text
ghcr.io/erland/developer-analytics-web
ghcr.io/erland/developer-analytics-backend
```

Pinning a version is recommended instead of using `latest`.


The published Developer Analytics GHCR packages are intended to be public, so
normal release installation does not require a registry login. The release
workflow verifies this with an anonymous clean pull after publication.


## 3. Configure GitHub sign-in

Create/configure the GitHub application used by Developer Analytics and set:

```text
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
GITHUB_CALLBACK_URL=https://your-host/api/auth/github/callback
FRONTEND_URL=https://your-host/
```

The callback URL configured in GitHub must exactly match the deployment value.

## 4. Configure required secrets

Set:

```text
DB_PASSWORD=...
CREDENTIAL_ENCRYPTION_KEY=...
```

`CREDENTIAL_ENCRYPTION_KEY` must be a Base64-encoded 32-byte key.

One way to generate it is:

```bash
openssl rand -base64 32
```

Keep this key backed up separately from the PostgreSQL database. Stored provider
credentials cannot be decrypted after restore without the same key/version.

## 5. Optional Gemini configuration

AI is disabled by default:

```text
AI_PROVIDER=disabled
```

To enable Gemini:

```text
AI_PROVIDER=gemini
GEMINI_API_KEY=...
GEMINI_MODEL=gemini-2.5-flash-lite
```

Private-data AI behavior remains separately constrained by:

```text
AI_PRIVATE_DATA_POLICY=PUBLIC_ONLY
```

Do not widen that policy without reviewing the AI privacy documentation.

## 6. Pull and start

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   pull

docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   up -d
```

The application is exposed on `WEB_PORT`, default `8080`.

## 7. Verify

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   ps
```

The `db`, `backend` and `web` services should become healthy. The `worker`
service should remain running.

For application health:

```bash
curl http://localhost:8080/api/health/application
```

Use your externally published HTTPS URL instead when a reverse proxy/load
balancer sits in front of the Compose host.

## 8. Stop

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   down
```

Do not add `-v` during normal shutdown because that removes the PostgreSQL
volume.

## Upgrade

Change only `APP_VERSION`, then run `pull` and `up -d` again.

Take a database backup before schema-changing upgrades. See:

- `docs/postgres-backup-restore.md`
- `docs/production-compose.md`

## What runs where

| Component | Runtime |
| --- | --- |
| Frontend + Nginx | web container |
| Quarkus API | backend container |
| Background jobs | worker container using the backend image |
| PostgreSQL | upstream PostgreSQL container |
| Gemini | optional external provider |

The deployment host therefore remains a container host rather than an
application build server.
