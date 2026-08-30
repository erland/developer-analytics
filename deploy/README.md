
> For the complete v1 installation procedure, see
> [`../docs/installation-v1.md`](../docs/installation-v1.md).

# Deployment

`deploy/compose.yaml` is the reference **release deployment**. It runs published
container images and does not require application source code to be built on the
target host.

## Release topology

```text
ghcr.io/erland/developer-analytics-web:<version>
                  |
                Nginx
                  |
ghcr.io/erland/developer-analytics-backend:<version>   (API)
                  |
ghcr.io/erland/developer-analytics-backend:<version>   (worker)
                  |
            postgres:17-alpine
```

The backend and worker intentionally use the same published backend image with
different runtime roles.

## Select a release version

Copy the example environment file:

```bash
cp deploy/env.example deploy/.env
```

Set a version explicitly:

```text
APP_VERSION=1.2.3
```

Then start the release deployment:

```bash
docker compose --env-file deploy/.env -f deploy/compose.yaml pull
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d
```

By default the image repositories are:

```text
WEB_IMAGE=ghcr.io/erland/developer-analytics-web
BACKEND_IMAGE=ghcr.io/erland/developer-analytics-backend
```

The defaults can be overridden for forks, mirrors or another registry without
editing the Compose file.

`APP_VERSION=latest` is supported, but pinning an explicit release version is
recommended for reproducible production deployments and rollbacks.

## Upgrade

To deploy a new release:

```bash
# edit APP_VERSION in deploy/.env
docker compose --env-file deploy/.env -f deploy/compose.yaml pull
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d
```

Flyway migrations run when the backend starts. Follow the backup/restore guide
before schema-changing upgrades:

- [`../docs/postgres-backup-restore.md`](../docs/postgres-backup-restore.md)

## Rollback

For an application-only rollback, restore the previous `APP_VERSION` and run
`pull` + `up -d` again.

Do not assume a database migration is reversible. If a newer release has changed
the schema incompatibly, use the documented database backup/restore procedure.

## Local source builds

The production Compose file intentionally contains no source-build requirement.
For local development or CI scenarios that need to build current source, apply
the local override:

```bash
docker compose   --env-file deploy/.env   -f deploy/compose.yaml   -f deploy/compose.local-build.yaml   up --build
```

This builds:

- `developer-analytics-web:local`
- `developer-analytics-backend:local`

while retaining the same networking, PostgreSQL, environment and runtime-role
configuration as the release topology.

## GHCR authentication

Public GHCR packages can be pulled without registry credentials. If package
visibility is private, authenticate Docker with a GitHub credential that has
`read:packages` before `docker compose pull`.

## Required deployment secrets

At minimum configure a non-example:

- `DB_PASSWORD`
- `CREDENTIAL_ENCRYPTION_KEY`

Real GitHub sign-in also requires the GitHub OAuth/App configuration described
in the project documentation.

Do not store production secret values in `deploy/.env` in source control.


## End-user release example

For a copy-and-configure deployment intended for end users, see:

- `compose.release.example.yaml`
- `release.env.example`
- [`../docs/release-compose-quickstart.md`](../docs/release-compose-quickstart.md)

That mode requires Docker/Compose plus GitHub application configuration and the
documented environment values. Java, Node.js, PostgreSQL and Nginx are supplied
by containers.


## Operator guide

For ongoing operation after installation, see:

- [`../docs/operator-v1.md`](../docs/operator-v1.md)

It covers container state, volumes, ports, health checks, logs, worker/jobs,
synchronisation recovery, backups, Flyway migration behavior and image upgrades.
