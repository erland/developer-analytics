# Container Runtime Smoke Tests

Step 79 extends container validation from "the Dockerfile builds" to "the exact
CI-built images can actually run together".

The existing `Container build validation` job now:

1. builds the frontend image,
2. loads it into the GitHub Actions Docker daemon as
   `developer-analytics-web:ci`,
3. builds and loads the backend image as
   `developer-analytics-backend:ci`,
4. starts PostgreSQL, backend, worker and web with Compose using `--no-build`,
5. waits for runtime health,
6. calls the application API through Nginx,
7. verifies the static frontend response,
8. verifies all expected services are running,
9. shuts the stack down and confirms no Compose containers remain.

## Why `--no-build` matters

The runtime check must test the images produced by the preceding CI build steps.
Allowing Compose to rebuild would only prove that another build can run.

`deploy/compose.ci-runtime.yaml` pins:

- `backend` -> `developer-analytics-backend:ci`,
- `worker` -> `developer-analytics-backend:ci`,
- `web` -> `developer-analytics-web:ci`.

The base Compose file still defines normal `build:` configuration for local and
deployment use. During the smoke test, `docker compose up --no-build` guarantees
that the locally loaded CI images are used.

## Run locally

Build the images with the same tags:

```bash
docker build -f frontend/Dockerfile -t developer-analytics-web:ci .
docker build -f backend/Dockerfile -t developer-analytics-backend:ci .
```

Then:

```bash
export DB_PASSWORD='runtime-smoke-password'
export CREDENTIAL_ENCRYPTION_KEY='AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8='
export WEB_PORT=18081

bash ./scripts/test-container-runtime.sh
```

## Relationship to Step 77

The tests are intentionally complementary:

- **Step 77 E2E smoke** validates a representative authenticated request flow.
- **Step 79 runtime smoke** proves that the exact images built in the container
  CI job start successfully and shut down cleanly.

This separation makes failures easier to diagnose while avoiding live GitHub
dependencies.
