# Release process

Developer Analytics uses GitHub Releases as the single release history and release trigger. There is no separate checked-in release-notes file.

## Normal development validation

The reusable CI workflow runs automatically for:

- every pull request,
- every push to `main`,
- manual workflow dispatches.

The `main` push therefore validates the exact merged commit before it can be used for a release. The suite includes frontend/backend verification, container builds and vulnerability scanning, runtime smoke tests, migrations, backup/restore, end-to-end, large-account, privacy and mobile acceptance coverage.

## Create a release

1. Merge the intended changes to `main`.
2. Wait for the **CI** workflow for that exact `main` commit to finish successfully.
3. Open **GitHub → Releases → Draft a new release**.
4. Create a new tag from the intended `main` commit using `vMAJOR.MINOR.PATCH`, for example `v1.0.1`.
5. Use GitHub **Generate release notes** and edit the generated text if useful.
6. Publish the GitHub Release.

The published release is the source of truth for the human-readable release description.

## Automated publication

Publishing the GitHub Release triggers `.github/workflows/publish-images.yml`.

Before publishing images, the workflow verifies that:

- the tag is an exact semantic version in `vMAJOR.MINOR.PATCH` form,
- the tag's commit is contained in `main`,
- a successful push-to-`main` CI run exists for that exact commit.

If any validation fails, no versioned container images are published.

For a release such as `v1.2.3`, the workflow publishes web and backend images with these aliases:

```text
1.2.3
1.2
1
latest
```

`latest` is omitted for prereleases. Each image includes SBOM and provenance attestations.

After publication, the workflow performs a clean GHCR pull and starts the published release with the release Compose example. It verifies the frontend, backend, worker, Flyway migrations and image identities.

## Manual workflow dispatch

`Publish container images` can still be run manually for troubleshooting. A manual dispatch publishes SHA-tagged images only; it does not create versioned release tags. Versioned images are produced only by a published GitHub Release.

## Failure after publishing a GitHub Release

The GitHub Release exists before the image workflow finishes. If validation, build, publication or runtime verification fails, the release remains visible while the Actions run is red. Correct the underlying problem and publish a new version rather than silently moving an existing immutable release tag.
