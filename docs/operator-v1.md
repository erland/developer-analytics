# Developer Analytics v1 — Operator Guide

This guide is for the person operating a self-hosted Developer Analytics v1
installation after initial setup.

The reference deployment uses Docker Compose and published GHCR images.

## Containers

The standard release deployment contains four services:

| Service | Purpose | Image |
| --- | --- | --- |
| `web` | Nginx + built frontend; proxies `/api/*` to backend | `ghcr.io/erland/developer-analytics-web:<version>` |
| `backend` | Quarkus API, authentication, migrations and synchronous application services | `ghcr.io/erland/developer-analytics-backend:<version>` |
| `worker` | Background jobs and synchronisation work | same backend image |
| `db` | PostgreSQL persistent store | `postgres:17-alpine` |

Inspect current state:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   ps
```

The backend and worker intentionally use the same application image with
different `APP_RUNTIME_ROLE` values.

## Volumes

The release Compose file defines:

```text
postgres-data
```

This volume contains the PostgreSQL data directory and is the primary persistent
application state.

Normal shutdown:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   down
```

Do **not** add `-v` during routine shutdown because that removes the database
volume.

Backups must be treated as the recovery mechanism; the Docker volume itself is
not a substitute for backups.

## Ports and network exposure

The `web` service is the only application service published to the host by
default:

```text
WEB_PORT -> web:80
```

Typical value:

```text
WEB_PORT=8080
```

The backend and PostgreSQL services are not directly published to the host in
the reference Compose file.

Internal communication:

```text
web -> backend:8080
backend/worker -> db:5432
```

The `data` network is internal.

For Internet-facing deployments, terminate HTTPS in front of the Compose
service or otherwise ensure the externally visible application URL uses HTTPS.

## Health checks

Expected steady state:

- `db`: healthy,
- `backend`: healthy,
- `web`: healthy,
- `worker`: running.

Check:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   ps
```

Application health endpoint:

```bash
curl -fsS http://localhost:8080/api/health/application
```

The backend also exposes Quarkus health endpoints used by operational checks.

A backend that does not become healthy often indicates:

- database connectivity failure,
- migration failure,
- invalid required configuration,
- credential-encryption-key configuration problem.

## Logs

Show all service logs:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   logs
```

Follow logs:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   logs -f web backend worker db
```

Useful focused commands:

```bash
docker compose --env-file deploy/release.env -f deploy/compose.release.example.yaml logs backend
docker compose --env-file deploy/release.env -f deploy/compose.release.example.yaml logs worker
docker compose --env-file deploy/release.env -f deploy/compose.release.example.yaml logs db
```

Application logs include structured correlation identifiers where available.
Credentials, tokens and private content should not be logged.

When investigating a failed request, start with backend logs. For delayed or
missing synchronisation, inspect worker logs as well.

## Background jobs

Repository discovery, contribution collection and related processing are handled
through persistent background jobs stored in PostgreSQL.

The `worker` service polls and processes these jobs.

Operational implications:

- restarting the worker does not discard queued jobs,
- interrupted running jobs are recoverable,
- transient failures can be retried,
- the database is the source of truth for job state.

Check the worker first when:

- repository discovery appears stuck,
- contribution data stops updating,
- synchronisation remains queued,
- recovery controls report repeated failures.

A worker container that repeatedly exits should be treated as an application or
configuration fault rather than solved by deleting job data.

## Synchronisation recovery

Developer Analytics contains recovery logic for interrupted and failed
synchronisation.

Users can trigger recovery from **Privacy/data sources** through:

- **Recover interrupted jobs**
- **Retry GitHub synchronisation**

The system also performs automatic stale/interrupted-job recovery.

Operational workflow for a synchronisation incident:

1. inspect `worker` and `backend` logs,
2. confirm GitHub access has not been revoked,
3. confirm the worker is running,
4. use the recovery action,
5. observe whether new jobs progress,
6. only restart containers if recovery remains stalled.

Provider authentication failures may require the user to reconnect GitHub rather
than repeated retries.

Rate-limit-related delays should not be treated as lost data.

## Database backup

Create regular PostgreSQL backups with:

```bash
bash ./scripts/postgres-backup.sh
```

The backup includes application data and Flyway migration history.

Also protect separately:

```text
CREDENTIAL_ENCRYPTION_KEY
CREDENTIAL_KEY_VERSION
```

The encryption key is not stored in PostgreSQL.

Recommended baseline:

- daily backup,
- retain at least 7 daily copies,
- retain at least 4 weekly copies,
- keep at least one copy outside the Docker host,
- periodically run restore verification.

See:

- `docs/postgres-backup-restore.md`

## Database migration behaviour

Flyway is the schema authority.

When a new backend image starts:

1. it connects to PostgreSQL,
2. Flyway reads `flyway_schema_history`,
3. pending migrations are applied in version order,
4. application startup continues only if migrations succeed.

Hibernate does not generate the production schema.

Operational consequences:

- do not manually edit or delete Flyway history,
- do not rename already-released migration files,
- do not modify an already-applied migration in place,
- create a backup before upgrading to a release containing schema changes.

If migration startup fails:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   logs backend db
```

Do not repeatedly remove/recreate the database as a troubleshooting shortcut.

The CI pipeline separately verifies fresh-database migration correctness.

## Upgrading images

The release is selected through:

```text
APP_VERSION=<version>
```

Recommended upgrade procedure:

1. read release notes,
2. create a verified database backup,
3. record the current `APP_VERSION`,
4. change `APP_VERSION` in `deploy/release.env`,
5. pull the new images,
6. recreate services,
7. verify health and logs.

Commands:

```bash
docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   pull

docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   up -d

docker compose   --env-file deploy/release.env   -f deploy/compose.release.example.yaml   ps
```

Then verify:

```bash
curl -fsS https://developer-analytics.example.com/api/health/application
```

The worker automatically uses the same backend version because both services
reference the same `BACKEND_IMAGE` and `APP_VERSION`.

### Rollback

An image-only rollback is possible by restoring the previous `APP_VERSION`.

However, database migrations may not be reversible. If the new release applied
a schema change that older code cannot use, restore the database backup created
before the upgrade.

## Operational checklist

Routine checks:

- containers are running,
- `db`, `backend` and `web` are healthy,
- worker remains running,
- application health endpoint responds,
- disk usage for PostgreSQL/backups is acceptable,
- backups are completing and being copied off-host,
- GitHub synchronisation is progressing,
- no repeated worker/job failures are visible,
- current `APP_VERSION` is documented.

Before maintenance:

- create a backup,
- record current image version,
- preserve encryption-key material,
- avoid deleting volumes unless performing an intentional restore/rebuild.

## Related documentation

- `docs/installation-v1.md`
- `docs/postgres-backup-restore.md`
- `docs/migration-verification.md`
- `docs/production-compose.md`
- `docs/container-runtime-smoke-tests.md`
- `docs/automated-release-verification.md`
