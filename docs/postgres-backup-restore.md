# PostgreSQL Backup and Restore

Step 74 defines the self-hosted backup/restore procedure for Developer Analytics.

The PostgreSQL database is the authoritative persistent store for accounts,
repository inventory, measured contributions and aggregates, derived technology
and project analysis, AI metadata, external-client configuration, synchronisation
state and queued background work.

## Backup format

Use PostgreSQL's **custom dump format** (`pg_dump --format=custom`). It supports
checks, compressed storage and selective inspection while retaining the full
schema and data needed by Developer Analytics.

The repository provides:

```text
scripts/postgres-backup.sh
scripts/postgres-restore.sh
scripts/test-postgres-backup-restore.sh
```

The scripts operate through `deploy/compose.yaml`, so the host does not need
PostgreSQL client tools installed.

## Create a backup

From the repository root, with `deploy/.env` configured:

```bash
bash ./scripts/postgres-backup.sh
```

By default this writes:

```text
backups/developer-analytics-<UTC timestamp>.dump
backups/developer-analytics-<UTC timestamp>.dump.sha256
```

The backup includes Flyway history. Preserve that history: it lets a restored
database retain its exact migration state.

### Backup security

Treat database backups as at least as sensitive as the live database. Depending
on the user's choices they can contain:

- GitHub account identifiers,
- private repository names and metadata,
- contribution history,
- derived analysis,
- AI assessment metadata,
- encrypted provider credentials.

Store backups encrypted at rest, restrict access, and never commit them to Git.
The `.gitignore` excludes the repository-local `backups/` directory.

`CREDENTIAL_ENCRYPTION_KEY` is **not** stored in PostgreSQL. If encrypted
provider credentials should remain usable after disaster recovery, separately
back up the encryption key/version using a secret-management system. Do not put
the key in the database dump.

## Restore

Restoring replaces the current Developer Analytics database.

```bash
bash ./scripts/postgres-restore.sh   backups/developer-analytics-20260830T120000Z.dump
```

The restore script:

1. verifies the SHA-256 file when present,
2. stops backend and worker writers,
3. terminates remaining sessions to the application database,
4. drops and recreates the database,
5. restores the custom dump using `pg_restore --exit-on-error`,
6. starts backend and worker again.

After restore, verify:

```bash
docker compose -f deploy/compose.yaml exec -T db   pg_isready -U developer_analytics -d developer_analytics

curl -fsS http://localhost:8080/q/health/ready
```

Also verify the account/repository dashboard and, when configured, reconnect any
provider whose external grant has changed since the backup was taken.

## Restore into a fresh host

For disaster recovery to a different host:

1. install Docker/Compose,
2. check out the same Developer Analytics release as the backup,
3. recreate `deploy/.env` and secrets,
4. start PostgreSQL only,
5. copy the `.dump` and optional `.sha256` files to the host,
6. run the restore script,
7. start the remaining services.

When restoring an old database into newer application code, start the backend
after the restore and allow Flyway to apply only migrations newer than those
recorded in the restored `flyway_schema_history`.

Do not manually delete Flyway history or rerun old migrations.

## Recovery point and retention guidance

The appropriate schedule depends on acceptable data loss. A practical
self-hosted starting point is:

- daily PostgreSQL backup,
- retain 7 daily copies,
- retain 4 weekly copies,
- keep at least one copy outside the Docker host.

More frequent backups can be used when synchronisation/analysis would be costly
to reproduce. Periodically run the restore verification described below;
unverified backups should not be treated as a complete recovery strategy.

## Automated restore verification

Run:

```bash
bash ./scripts/test-postgres-backup-restore.sh
```

against a running, migrated Compose database.

The verification test inserts deterministic realistic data representing:

- an application account/provider identity,
- repository inventory,
- user and repository activity aggregates,
- persisted user-level AI analysis metadata.

It then:

1. creates a custom-format backup with `pg_dump`,
2. restores it into a separate temporary PostgreSQL database,
3. queries the restored database,
4. verifies the account,
5. verifies the repository inventory,
6. verifies activity aggregates,
7. verifies AI analysis metadata,
8. verifies that Flyway migration history was restored,
9. drops the temporary verification database.

The live application database is not replaced by this test.

This is deliberately an actual PostgreSQL dump/restore test rather than a
serialization/unit test, because backup correctness depends on PostgreSQL schema,
foreign keys, JSONB values and migration history.

## CI workflow placement

The `backup-restore` verification is a normal GitHub Actions job and must remain
nested under the top-level `jobs:` mapping in `.github/workflows/ci.yml`.
A malformed top-level `backup-restore:` key causes GitHub to reject the workflow
before any jobs are created.
