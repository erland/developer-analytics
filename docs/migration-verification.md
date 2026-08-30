# Migration Verification

Step 78 makes Flyway migration correctness an explicit CI concern.

The verification has three layers:

1. static migration-sequence validation,
2. the backend persistence integration test,
3. startup against a completely fresh PostgreSQL volume.

## Static sequence validation

Run:

```bash
python3 scripts/check-flyway-migrations.py
```

The check rejects:

- malformed migration filenames,
- duplicate versions,
- missing versions in the `V1..Vn` sequence,
- a sequence that does not start at `V1`,
- empty migration files.

This catches migration-order mistakes before PostgreSQL is started.

## Fresh-database verification

Run:

```bash
export DB_PASSWORD='migration-test-password'
export CREDENTIAL_ENCRYPTION_KEY='AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8='
bash ./scripts/test-fresh-database-migrations.sh
```

The script uses an isolated Compose project and a new PostgreSQL volume. It:

- builds and starts the real backend,
- waits for backend startup,
- therefore requires Flyway migration-at-start to succeed,
- reads `flyway_schema_history`,
- fails if any migration is marked failed,
- compares the database migration order with the migration files in the repository,
- removes the test database volume afterwards.

A broken SQL migration or a migration that prevents application startup therefore
fails this dedicated CI job.

## Backend integration test

`FlywayMigrationTest` continues to verify the baseline schema and now also
asserts that all current migrations are present and successful.

When a migration is added, update the expected migration count in that test.
This intentional assertion makes schema-version changes visible in review.

## Upgrade verification

The development plan makes upgrade-from-release testing optional until releases
exist. No artificial previous-release schema is introduced in Step 78.

Once a stable released schema exists, the migration job can be extended to:

1. restore or construct the latest released schema,
2. start the new backend,
3. verify Flyway upgrades it to the current schema,
4. verify representative retained data.

That test should complement, not replace, the fresh-database verification.
