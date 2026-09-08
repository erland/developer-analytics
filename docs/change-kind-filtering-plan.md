# Implementation Plan – Change Kind Filtering

**Repository:** `erland/developer-analytics`  
**Scope:** Add evidence-based classification of changed files so activity can be filtered by code, documentation, CI/CD, or combinations of these.  
**Status:** Implementation in progress

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

Very-large-history operational tuning remains part of Step 8 acceptance/backfill validation.

---

## Step 5 – Add change-kind-aware backend aggregations and API filtering

Introduce a common API filter semantic such as:

`changeKinds=CODE,DOCUMENTATION`

Apply it to activity-oriented APIs while preserving existing output when omitted.

At minimum cover:

- overview activity metrics
- activity timeline/statistics
- project activity summaries/details
- project type/category activity
- technology-associated activity where activity is derived from commits
- report input/canonical report model
- External Analysis API activity-oriented payloads

Define commit-count semantics explicitly:

- a mixed commit may count as “touching code” and “touching documentation”
- additions/deletions are attributed only to their file/change kind and must not be double-counted

**Acceptance criteria**

- Omitted filter reproduces current all-activity behaviour.
- `CODE` excludes documentation line changes.
- `DOCUMENTATION` excludes code line changes.
- combined filters equal the sum of their underlying line-based activity.
- mixed-commit semantics are covered by tests.

---

## Step 6 – Add reusable frontend filter controls

Introduce one reusable change-kind filter component and use it consistently.

Recommended default UX:

- **All**
- **Code**
- **Documentation**
- **Custom…**

The custom/advanced selection can expose Code, Documentation, CI/CD and Other.

---

## Step 7 – Extend reports and External Analysis API

Update the canonical report model and External Analysis API with the same change-kind scope while preserving backwards compatibility and privacy semantics.

---

## Step 8 – Backfill, acceptance testing and documentation

Finalize operational backfill behaviour, performance/rate-limit validation, end-to-end acceptance scenarios and product documentation.

Validate especially that book/novel edits no longer inflate code-only activity, CI/CD remains conservatively classified, “All” matches previous aggregate behaviour, privacy holds, and large-account processing remains bounded.

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

Execute Steps 1–8 sequentially. Each step should leave the repository buildable and tested. Avoid implementing UI filtering before the persisted per-file data and shared backend semantics are stable.
