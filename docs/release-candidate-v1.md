# Version 1 Release Candidate Gate

Step 90 defines the mandatory gate before creating a Version 1 release-candidate
tag.

A release candidate is ready only after the GitHub Actions workflow
**Version 1 Release Candidate Readiness** is green for the exact commit that will
be tagged.

## What the gate requires

The workflow first invokes the complete reusable `CI` workflow. Therefore a
candidate cannot proceed unless all existing CI and acceptance jobs pass,
including:

- frontend lint/typecheck/unit/feature/responsive/privacy tests,
- backend compile and complete test suite,
- Docker image build and runtime smoke test,
- Flyway migration verification against a fresh database,
- end-to-end Compose smoke test,
- backup/restore verification,
- large-account acceptance with 240 repositories,
- privacy acceptance matrix,
- Playwright/Chromium mobile acceptance.

## GHCR and clean-install proof

After the full CI suite succeeds, the RC workflow creates immutable temporary
candidate tags:

```text
rc-<first 12 characters of commit SHA>
```

for:

```text
ghcr.io/<owner>/developer-analytics-web
ghcr.io/<owner>/developer-analytics-backend
```

Both images are built with SBOM and provenance enabled.

The workflow then:

1. logs out of GHCR after publication,
2. removes cached candidate images,
3. anonymously pulls both candidate tags,
4. starts the end-user release Compose file with `--no-build`,
5. verifies web/backend health,
6. verifies the frontend and proxied application API,
7. verifies worker uses the backend candidate image,
8. verifies Flyway has no failed migrations,
9. shuts the clean installation down.

This proves GHCR publication and clean installation for the exact candidate
commit before an RC tag exists.

## Documentation gate

`scripts/check-release-candidate-docs.py` requires the current v1 documentation
set, including:

- installation guide,
- operator guide,
- backup/restore,
- privacy acceptance,
- mobile acceptance,
- large-account acceptance,
- release Compose quickstart,
- automated release verification.

## Tagging rule

Only tag a release candidate from the exact commit whose final
**Version 1 RC ready** job is green.

Do not move the tag to another commit without rerunning the readiness workflow.

A typical tag after the gate is:

```text
v1.0.0-rc.1
```

The actual tagging/release operation belongs to Step 91.

## Status interpretation

The repository contains all gates required by Step 90. Their runtime result is
authoritative in GitHub Actions; local structural validation cannot substitute
for a green hosted run.
