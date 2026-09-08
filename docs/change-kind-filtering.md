# Change-kind filtering

Status: implemented by `docs/change-kind-filtering-plan.md` Steps 1–8.

Developer Analytics classifies each changed file independently so measured activity can be viewed as code, documentation, CI/CD or other work without classifying an entire repository as one type.

## Change kinds

The initial taxonomy is:

- `CODE` — recognised source-code and scripting files.
- `DOCUMENTATION` — documentation extensions and high-confidence documentation/book/manuscript paths.
- `CI_CD` — only well-known CI/CD locations and filenames such as `.github/workflows/**`, `.github/actions/**`, `.circleci/**`, `.gitlab/**`, `Jenkinsfile` and Azure/GitLab pipeline files.
- `OTHER` — safe fallback when no deterministic rule matches.

Generic scripts are deliberately **not** treated as CI/CD merely because they may be used during delivery. For example `scripts/deploy.sh` remains `CODE` unless future evidence links it to a workflow.

## User-facing semantics

The shared filter offers **All**, **Code**, **Documentation** and **Custom…**. Custom selection exposes all four categories.

Omitting `changeKinds` means **All** and preserves the historical aggregate path. This is important because non-commit contribution types such as pull requests, reviews and issues do not have changed-file classification.

When a filter is active, commit activity is derived from classified changed-file rows. A mixed commit may touch several kinds, but it is counted once in a combined query. Additions/deletions are summed only from matching files, avoiding double counting.

A documentation-only book or novel edit therefore appears in Documentation activity but does not inflate Code-only activity.

## Persistence and explainability

`contribution_file_change` stores one row per changed path with:

- contribution and repository references,
- path,
- additions and deletions,
- change kind,
- classifier confidence,
- classifier rule key and classifier version,
- occurrence time,
- privacy provenance.

The unique `(contribution_id, path)` constraint makes replacement idempotent. Indexes support user/repository + time + change-kind queries.

No patch bodies or source-file contents are stored for this feature.

## Ingestion and backfill

GitHub commit-detail ingestion fetches changed-file metadata and classifies each file with classifier version `1`. Contribution scope version `3` is the historical re-analysis boundary for existing repositories.

Backfill uses the existing contribution-sync/background-job mechanism rather than an unbounded one-shot database migration. Existing rows remain usable while repositories are progressively refreshed. Provider failures are fetch-before-replace: previously stored file-change data is not erased merely because a later provider request fails.

Operationally:

1. upgrade normally and let Flyway apply migration `V36`,
2. allow background contribution refresh to populate historical file changes,
3. monitor the existing sync/job status views for progress, rate limiting and failures,
4. retry/recover through the normal worker/job mechanisms rather than manually editing classification rows.

## Privacy

Change-kind filtering never widens repository access. Filtered queries remain user-scoped and use the same repository inclusion/privacy boundaries as their unfiltered counterparts.

External Analysis API tokens still enforce their independent privacy scope. `changeKinds` narrows activity inside that authorised scope; it cannot grant access to private repositories or private detail.

## Reports and external API

Canonical reports use model `report-v2` and include an explicit `changeScope`. Markdown/PDF metadata and methodology state the effective scope. Report preview is still required before generation, and export responses include `X-Change-Kinds`.

External Analysis API `GET /api/me/activity` accepts repeated or comma-separated `changeKinds` values. Omitting the parameter retains the original v1 activity behaviour; the addition is backward-compatible within the existing media type.

## Performance and large accounts

Filtered aggregation is backed by the change-kind/time indexes introduced in `V36`. The existing CI large-account acceptance scenario seeds 240 repositories, enforces bounded pagination, checks API requests against a five-second ceiling, exercises partial enrichment, worker recovery and surfaced provider rate limiting.

The change-kind implementation deliberately reuses those bounded repository/job mechanisms. It does not introduce repository cloning, patch storage or an unbounded synchronous historical scan.

## Acceptance scenarios

The automated suite covers these feature-specific cases:

| Scenario | Expected result |
| --- | --- |
| Markdown book/manuscript paths | Documentation, not Code |
| Java/TypeScript/Python application files | Code |
| Commit touching source + docs + workflow | May contribute to Code, Documentation and CI/CD; combined commit count remains one |
| `.github/workflows/**` / `Jenkinsfile` | CI/CD |
| Generic `scripts/deploy.sh` or release Python script | Code, not CI/CD |
| Omitted `changeKinds` | All / historical behaviour |
| Report filter | Explicit `changeScope` in report-v2 |
| External activity filter | Same selection syntax, privacy still enforced |
| Fresh database | `V36` present and all Flyway migrations successful |
| Large account | Existing 240-repository bounded acceptance flow remains green |

## Deferred refinements

The following remain intentionally outside the initial implementation: separate TEST/CONFIGURATION/GENERATED/DATA/ASSET categories, inference that a generic script is CI/CD because a workflow references it, repository-specific correction rules and composition visualisations over time.
