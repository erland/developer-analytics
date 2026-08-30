# End-to-End Smoke Tests

Step 77 adds a lightweight end-to-end CI environment that verifies the deployed
request path:

```text
client -> web container -> Nginx -> backend -> PostgreSQL
```

Normal CI does not call live GitHub.

## Verified behavior

`scripts/test-e2e-smoke.sh` verifies:

- the built frontend is served through Nginx,
- `/api/health/application` reaches the backend through Nginx,
- PostgreSQL is reachable by the backend,
- Flyway has successful migrations and no failed migrations,
- a deterministic database-seeded session is accepted by normal authentication,
- `/api/auth/session` and `/api/me` work through Nginx.

## Authentication strategy

Production authentication remains GitHub OAuth. The smoke test does not add a
test-login endpoint or authentication bypass.

Instead, CI creates the minimum records for an already authenticated account:

- `app_user`,
- `provider_identity`,
- `user_session`.

The known smoke-test session token is SHA-256 hashed before being inserted,
matching the application's session-storage model.

## Run locally

```bash
export DB_PASSWORD='local-e2e-password'
export CREDENTIAL_ENCRYPTION_KEY='AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8='
export WEB_PORT=18080

docker compose -f deploy/compose.yaml up -d --build db backend web
bash ./scripts/test-e2e-smoke.sh
docker compose -f deploy/compose.yaml down -v
```

`E2E_BASE_URL` can override the default URL.

## CI

The `End-to-end smoke test` job builds and starts the actual `db`, `backend` and
`web` Compose services, runs the smoke checks through port `18080`, prints logs
on failure and removes containers and volumes afterwards.

This complements the frontend/backend test layers, container-build validation
and PostgreSQL backup/restore verification.
