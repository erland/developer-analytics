# Implementation Plan – Change Kind Filtering

**Repository:** `erland/developer-analytics`  
**Scope:** Add evidence-based classification of changed files so activity can be filtered by code, documentation, CI/CD, or combinations of these.  
**Status:** Implemented; final CI/acceptance verification in PR #72

## Goal

Developer Analytics currently treats changed lines as one undifferentiated activity stream. This works for code-centric repositories, but distorts the picture for repositories containing books, long-form documentation, generated material, or CI/CD definitions.

The goal is to introduce a reusable `change kind` dimension so every activity-oriented view can distinguish at least:

- `CODE`
- `DOCUMENTATION`
- `CI_CD`
- `OTHER`

The model must be extensible for later categories such as `TEST`, `CONFIGURATION`, `DATA`, `ASSET`, and `GENERATED`.

Classification should happen at changed-file level, not repository level or commit level. A single commit may therefore contain several change kinds.

## Design principles

1. Preserve the current “all activity” behaviour as the default.
2. Classify changed files deterministically from observable evidence.
3. Prefer conservative classification over speculative classification.
4. Treat CI/CD conservatively: known workflow paths/files are CI/CD; ordinary shell/Python scripts remain code/other unless stronger evidence exists.
5. Keep classification explainable by storing the rule/version used.
6. Do not collapse mixed commits into one category.
7. Avoid double-counting changed lines when aggregating.
8. Use one shared filter semantic across dashboards, reports and the External Analysis API.
9. Preserve privacy provenance and existing user scoping.
10. Keep the design backwards-compatible and migration-safe.

---

## Step 1 – Establish the change-kind domain model ✅

Implemented.

---

## Step 2 – Persist per-file contribution/change statistics ✅

Implemented.

---

## Step 3 – Implement deterministic file classification ✅

Implemented with a central `ChangeKindClassifier`.

Initial high-confidence rules include documentation, high-confidence CI/CD, common source-code/script extensions and safe `OTHER` fallback. Classification is deterministic, explainable and path-normalized.

---

## Step 4 – Populate change-kind data during GitHub contribution ingestion ✅

Implemented.

GitHub commit detail is fetched for commit contributions so changed-file paths and line statistics can be classified and persisted.

Key properties:

- only commit contributions receive file-level change rows,
- GitHub `files[]` is paginated at 100 files per page,
- owning commit additions/deletions/changed-files are refreshed from commit detail,
- all remote pages are fetched before existing file rows are replaced,
- re-running sync is idempotent,
- contribution scope version is now `3`, forcing one historical scan for existing installations,
- existing contribution rows are retained and upserted rather than destructively rebuilt,
- current weekly activity remains available until Step 5 switches filtered activity to file-level aggregation.

---

## Step 5 – Add change-kind-aware backend aggregations and API filtering ✅

Implemented for the product-facing backend activity APIs.

Shared query semantics:

`changeKinds=CODE,DOCUMENTATION`

The same parser also accepts repeated query parameters. Omitted/empty `changeKinds` means **All** and preserves the pre-existing aggregation path.

Implemented coverage:

- overview/activity metrics and timelines,
- project lifecycle/activity summaries in the activity response,
- project detail activity,
- project type/category activity,
- technology-associated activity where activity is derived from commits.

Filtered line statistics are derived from `ContributionFileChange`, while the unfiltered path intentionally retains the existing weekly-statistics behaviour for backwards compatibility.

Commit semantics are explicit:

- a mixed commit touching several selected kinds counts once for the combined selection,
- the same mixed commit may count in both independent `CODE` and `DOCUMENTATION` queries,
- additions/deletions are summed from matching file rows only and are never double-counted inside one query,
- invalid change-kind values fail with HTTP 400 instead of being silently ignored.

---

## Step 6 – Add reusable frontend filter controls ✅

Implemented with one shared change-kind selection and reusable filter component.

Default UX:

- **All**
- **Code**
- **Documentation**
- **Custom…**

Custom selection exposes Code, Documentation, CI/CD and Other. The selection is reused across the relevant analytical views and Reports. Legacy/standalone consumers default safely to **All**.

---

## Step 7 – Extend reports and External Analysis API ✅

Implemented.

The canonical report model is `report-v2` with explicit `changeScope`. Markdown/PDF reports, preview/export, and External Analysis API activity use the same optional `changeKinds` semantics while preserving historical **All** behaviour and independent privacy enforcement.

---

## Step 8 – Backfill, acceptance testing and documentation ✅

Implemented.

Operational backfill remains bounded by the existing contribution-sync/background-job architecture rather than a synchronous migration. Contribution scope version `3` triggers historical re-analysis, while Flyway `V36` only creates the file-change persistence/index structures.

Final acceptance/documentation coverage includes:

- Markdown book/manuscript edits classify as Documentation rather than Code,
- Java/TypeScript/Python/source edits classify as Code,
- mixed source + documentation + workflow commits retain multiple kinds,
- CI/CD classification remains conservative and generic deployment/release scripts remain Code,
- omitted `changeKinds` remains **All**,
- `report-v2` legacy construction still resolves to **All**,
- fresh-database migration validation includes `contribution_file_change` and all 36 migrations,
- existing privacy acceptance remains the boundary for filtered data as well,
- the existing 240-repository large-account CI scenario remains the bounded performance/rate-limit/recovery gate,
- product/operator semantics are documented in `docs/change-kind-filtering.md`.

---

## Deferred refinements

Potential later additions:

- `TEST`
- `CONFIGURATION`
- `GENERATED`
- `DATA`
- `ASSET`
- recognising scripts referenced directly by CI workflows
- repository-specific/user correction rules for misclassified paths
- activity composition visualisations by change kind over time

## Suggested implementation order

Steps 1–8 are now implemented sequentially. The resulting design keeps the persisted per-file evidence and shared backend semantics as the foundation for UI, reports and external API filtering.
