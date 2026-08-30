# Automated Release Verification

Step 84 makes a published release fail its workflow if the published images
cannot actually start.

This verification runs after Step 83's clean GHCR pull/tag/security checks.

## Release workflow sequence

For a published GitHub Release:

1. build web and backend images,
2. publish both images to GHCR,
3. verify clean anonymous pulls and release tags,
4. start `deploy/compose.release.example.yaml` with the **published tags**,
5. run runtime smoke checks,
6. stop and remove the verification deployment.

The runtime verification uses:

```bash
bash ./scripts/test-published-release-runtime.sh <release-tag>
```

## What is verified

The script confirms that:

- no source rebuild is performed (`docker compose up --no-build`),
- the published web image becomes healthy,
- the published backend image becomes healthy,
- the worker starts from the same published backend image,
- the frontend HTML is served,
- `/api/health/application` works through Nginx,
- PostgreSQL is reachable,
- Flyway migrations complete without failed entries,
- the actual running container image names match the requested release version,
- Compose shuts down cleanly.

A release workflow therefore becomes red if the registry contains images that
can be pulled but cannot operate together.

## No live GitHub dependency

The verification supplies deterministic placeholder GitHub application values
only to satisfy normal runtime configuration. It does not execute OAuth or make
live GitHub API calls.

Gemini remains disabled.

## Difference from other smoke tests

- Step 77 tests the application flow from current source-built images.
- Step 79 tests the exact images built during pull-request/main container CI.
- Step 83 verifies GHCR pullability, tags, visibility and image hygiene.
- **Step 84 tests the actual published release images from GHCR in the end-user
  release Compose topology.**

Together these distinguish source, build, registry and release-runtime failures.
