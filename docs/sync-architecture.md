# Sync architecture

This document describes the responsibility split between GitHub REST, local Git and the main synchronization services after the Git-history and sync refactoring.

## Design goals

- Use GitHub REST where GitHub-specific metadata or semantics are required.
- Use local Git where repository history/tree data can be read more efficiently without one REST request per commit/file.
- Keep Git and REST transport concerns outside technology-classification and contribution-persistence logic.
- Keep the normal incremental contribution sync bounded and API-based.
- Use Git primarily for historical/bulk repository inspection, with REST fallback where correctness requires it.
- Preserve existing rate-limit, retry and background-job semantics.

## Transport responsibility

### GitHub REST

GitHub REST remains the source for:

- repository discovery and GitHub repository metadata,
- pull requests,
- issues,
- reviews,
- repository languages,
- contributor statistics and weekly contributor activity,
- current GitHub user identity when no persisted provider login is available,
- normal incremental contribution listing,
- incremental commit details when enrichment is needed,
- repository snapshot fallback if Git snapshot collection fails with a provider-level error.

Contributor statistics and weekly activity are fetched from `/stats/contributors` once per repository sync and reused for both aggregate statistics and weekly activity.

The persisted provider login is reused when available. `/user` is only a fallback when the persisted identity does not contain a login.

### Local Git

Local Git is used for:

- historical change-kind backfill,
- commit-to-file numstat analysis for historical commits,
- repository tree inspection for file/manifest evidence,
- reading bounded manifest/configuration file content for technology evidence.

The implementation uses temporary bare/blobless repositories. No permanent Git cache or working tree is maintained.

Git is an optimization layer, not a replacement for GitHub-specific metadata APIs.

## Git infrastructure

Common Git infrastructure is under:

`io.github.developeranalytics.provider.git`

### `GitWorkspaceService`

Owns temporary clone lifecycle and clone-level resource controls:

- bare/blobless clone,
- credential-safe Git environment,
- clone timeout,
- temporary disk-size limit,
- minimum free-space reserve,
- at most one concurrent clone per worker JVM,
- cleanup of temporary repositories.

Configuration uses the `developer-analytics.git.*` namespace. Existing `GIT_HISTORY_*` environment variables remain supported for deployment compatibility.

### `GitCommandRunner`

Owns execution of Git commands against an already-cloned bare repository:

- `--git-dir` handling,
- authentication environment for lazy object fetches,
- command timeout,
- process termination,
- stdout/error handling,
- credential sanitization,
- thread-interrupt preservation.

Domain readers must not implement their own production `ProcessBuilder` logic for Git commands.

### `GitLocalHistoryReader`

Reads requested historical commits and returns provider-neutral per-file additions/deletions.

Merge commits are compared against their first parent. Rename detection is intentionally disabled for stable, deterministic semantics.

### `GitRepositorySnapshotService`

Reads a bounded repository tree and selected manifest/configuration file contents from Git.

File selection and limits are defined by the shared `RepositorySnapshotFilePolicy`.

## Repository snapshot abstraction

`RepositorySnapshotProvider` owns transport selection for file/manifest snapshots:

1. try `GitRepositorySnapshotService`,
2. fall back to `GitHubProviderAdapter.fetchRepositorySnapshot(...)` on `ProviderException`,
3. do not hide unexpected programming errors behind REST fallback,
4. do not continue to REST when the thread is interrupted.

`FileManifestEvidenceService` therefore depends only on `RepositorySnapshotProvider`; it does not know whether Git or REST supplied the snapshot.

## Repository model mapping

`ProviderRepositoryMapper` is the single production mapping from persisted `SourceRepository` to provider-neutral `ProviderRepository`.

It preserves:

- external repository and owner IDs,
- owner login,
- owner type,
- visibility,
- fork/archive flags,
- activity timestamp,
- description,
- topics,
- repository size.

Services should not construct equivalent `ProviderRepository` instances independently.

## Contribution synchronization

### `GitHubContributionDiscoveryService`

Acts as the orchestration layer. Its responsibilities are intentionally limited to:

- resolving sync context,
- starting and completing the sync run,
- paging through provider contributions,
- delegating per-contribution ingestion,
- updating sync progress/rate-limit state,
- fetching and delegating contributor snapshot persistence,
- marking repository sync/scope state,
- handling provider and runtime failures at orchestration level.

It should not contain detailed persistence logic or Git process handling.

### `GitHubContributionSyncContextResolver`

Resolves the stable context for one contribution sync:

- `ProviderSession`,
- access token,
- persisted provider login,
- `/user` fallback when login is missing,
- provider-neutral repository mapping through `ProviderRepositoryMapper`.

### `ProviderSession`

Represents credentials and persisted provider identity obtained from one provider-connection lookup.

This avoids separately loading the same provider connection for token and login during contribution sync.

### `GitHubContributionIngestionService`

Owns processing of one `ProviderContribution`:

- provider type/state mapping,
- create/update of `Contribution`,
- cached classification checks,
- commit-detail enrichment when required,
- file-statistics updates.

Pagination and sync-run lifecycle do not belong here.

### Contributor statistics

`GitHubContributorSnapshotService` fetches one contributor-statistics payload and derives:

- contributor totals,
- human/bot counts,
- user/repository commit counts,
- user additions/deletions,
- weekly user activity.

`ContributorSnapshotPersistenceService` persists the aggregate repository statistics and delegates weekly persistence to `GitHubWeeklyActivityService`.

`GitHubWeeklyActivityService` is persistence-focused; it does not perform its own duplicate `/stats/contributors` network request in the main sync flow.

## Historical change-kind backfill

`GitHubChangeKindBackfillJobHandler` keeps historical backfill separate from normal incremental contribution sync.

For each bounded job:

1. select up to 1,000 commits missing current classification,
2. fetch Git history once for the whole bounded work set,
3. process results in logical groups of 100,
4. persist usable Git results locally,
5. fall back to REST commit-detail only for missing/invalid Git results or provider-level Git failure,
6. queue continuation when work remains,
7. mark contribution scope current only when classification is complete.

Unexpected runtime/programming errors are not silently converted into REST fallback.

## File and manifest evidence

`RepositorySnapshotFilePolicy` is the single source of truth for repository snapshot selection:

- which paths are relevant,
- maximum 40 relevant files,
- maximum 1 MiB per readable file.

Both Git and REST snapshot transports use the same policy.

`FileManifestEvidenceService` is responsible only for applying technology evidence rules to the returned snapshot and persisting evidence.

## Language evidence

`LanguageEvidenceService` continues to use GitHub's language endpoint rather than attempting to recreate GitHub Linguist semantics locally.

This is intentional: Git is used where repository data is the source of truth, while GitHub REST remains appropriate where GitHub provides higher-level semantics.

## Error and fallback rules

- Git provider/transport failure may fall back to REST where explicitly defined.
- Unexpected `RuntimeException` must not be hidden by a fallback path.
- Interrupted threads must remain interrupted and must not start additional fallback network work.
- GitHub 403/429 handling continues to use the existing provider rate-limit/retry semantics.
- Temporary Git repositories must always be cleaned up.

## Guidance for new sync functionality

When adding a new synchronization feature, prefer:

- **GitHub REST** for GitHub-specific entities, identity, permissions, statistics and provider semantics.
- **Local Git** for repository history, tree structure and bounded file inspection where REST would otherwise require many per-object calls.
- **A provider abstraction** when a domain service should not care which transport is used.
- **`ProviderRepositoryMapper`** rather than constructing `ProviderRepository` manually.
- **`GitCommandRunner`** rather than introducing new production Git process handling.

Do not move functionality to Git solely because Git can expose the data. The deciding factor is whether local Git materially reduces API cost/complexity without losing GitHub-specific semantics.

## Current validation

The refactored architecture has passed the full CI and acceptance matrix, including:

- backend validation,
- frontend validation,
- mobile acceptance,
- 200+ repository large-account acceptance,
- migration verification,
- privacy acceptance,
- end-to-end smoke tests,
- PostgreSQL backup/restore verification,
- container build/runtime validation,
- dependency review.
