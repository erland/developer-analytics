# Version 1 Release Procedure

The final Version 1 release is created through the GitHub Actions workflow **Version 1 Release**.

## Preconditions

1. The exact commit intended for release has passed **Version 1 Release Candidate Readiness**.
2. All pull-request CI jobs are green.
3. GHCR packages are intended to be publicly pullable as documented in the installation guide.
4. The operator has reviewed `RELEASE_NOTES.md`.

## Run the release

From GitHub Actions, choose **Version 1 Release** and run it from the exact commit/branch containing the approved release candidate.

Inputs:

- `version`: normally `v1.0.0` for the first Version 1 release.
- `confirmation`: exactly `RELEASE_V1`.

The workflow refuses non-`v1.x.y` tags and refuses to overwrite an existing tag/release.

## Release sequence

The workflow deliberately creates the GitHub tag/release last:

1. validate requested version and confirmation,
2. execute the complete reusable CI/acceptance workflow,
3. build the web and backend containers,
4. publish full, minor, major and `latest` GHCR tags,
5. attach OCI source/revision/version metadata,
6. publish SBOM and provenance attestations,
7. anonymously pull and verify the published images,
8. start the published release via the end-user Compose example,
9. verify frontend, backend, worker, Flyway and container identities,
10. create the GitHub tag and release from the exact tested commit using the checked-in release notes,
11. verify the created tag resolves to `GITHUB_SHA`.

If any step before tag creation fails, no GitHub Version 1 release is created.

## Result

For `v1.0.0`, the primary immutable images are:

```text
ghcr.io/<owner>/developer-analytics-web:1.0.0
ghcr.io/<owner>/developer-analytics-backend:1.0.0
```

Aliases are also published as `1.0`, `1` and `latest`.

The worker runs the same backend image.

## Existing generic publish workflow

`.github/workflows/publish-images.yml` remains available for ordinary/manual image publishing. Its publish job explicitly skips `v1.*` GitHub release events so it cannot rebuild and replace a Version 1 image digest after the dedicated workflow has verified it. The dedicated Version 1 workflow is authoritative for v1 releases because it places the full Version 1 acceptance suite and clean published-image verification before tag creation.
