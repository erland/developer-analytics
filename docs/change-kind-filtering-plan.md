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

Initial high-confidence rules include:

### Documentation

- `*.md`
- `*.mdx`
- `*.rst`
- `*.adoc`
- `*.asciidoc`
- `*.tex`
- `*.txt`
- clear documentation/book paths such as `docs/**`, `documentation/**`, `chapters/**`, `book/**`, `manus/**`, `manuscript/**`

Documentation-path classification intentionally takes precedence over source-code extensions so code examples living inside documentation trees count as documentation activity.

### CI/CD

Only high-confidence CI/CD locations/files are classified as CI/CD:

- `.github/workflows/**`
- `.github/actions/**`
- `.gitlab-ci.yml` / `.gitlab-ci.yaml`
- `.gitlab/**`
- `.circleci/**`
- `Jenkinsfile`
- `azure-pipelines.yml` / `azure-pipelines.yaml`

CI/CD has the highest precedence. Generic `scripts/*.sh`, `scripts/*.py`, `tools/**`, etc. are not promoted to CI/CD merely because they may be used by automation.

### Code

A conservative set of common source-code/script extensions is classified as `CODE`. Configuration/manifests such as `pom.xml`, `package.json`, and `docker-compose.yml` remain `OTHER` in this first version.

### Other

Unknown or missing paths fall back safely to `OTHER`.

The classifier normalizes case and Windows path separators and returns stable classifier metadata (`ruleKey`, confidence and classifier version). Focused unit tests cover positive and negative cases.

---

## Step 4 – Populate change-kind data during GitHub contribution ingestion

Extend GitHub commit discovery so changed-file metadata is collected and classified as contributions are ingested/refreshed.

Avoid cloning repositories.

Handle GitHub API pagination/rate-limit behaviour consistently with existing ingestion.

For existing historical commits, provide a bounded re-analysis/backfill mechanism rather than requiring destructive re-import.

**Acceptance criteria**

- Newly ingested commits receive per-file change-kind data.
- Mixed commits are represented correctly.
- Re-running ingestion is idempotent.
- Existing accounts can backfill the new dimension.
- Worker/job progress and failure handling remain visible.

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

The custom/advanced selection can expose:

- Code
- Documentation
- CI/CD
- Other

Preserve `All` as the default so current users see unchanged behaviour until they choose a filter.

Apply the control to all views where activity/change statistics are meaningful, including:

- Overview
- Activity
- Projects
- Project detail
- Technologies where activity metrics are shown
- Project types
- report/export configuration where relevant

Ensure the control is mobile-friendly.

**Acceptance criteria**

- One shared component/semantic is used instead of per-page bespoke filters.
- Filter state updates all affected metrics in a view consistently.
- Responsive/mobile tests cover the control.
- Existing privacy labels remain correct.

---

## Step 7 – Extend reports and External Analysis API

Update the canonical report model so report activity can be generated for:

- all change kinds
- code only
- documentation only
- selected combinations

Record the effective change-kind scope in report methodology/coverage so exported results remain interpretable.

Extend External Analysis API documentation and OpenAPI schemas with the same filtering concept.

**Acceptance criteria**

- Markdown and PDF remain aligned because both use `CanonicalReport`.
- Export clearly states which change kinds were included.
- Public/private privacy semantics remain unchanged.
- External API clients can request filtered activity without a new incompatible API family.

---

## Step 8 – Backfill, acceptance testing and documentation

Add an explicit migration/backfill operational path for existing installations.

Update relevant documentation:

- functional specification
- architecture specification if persistence/query flow changes materially
- operator/upgrade guidance
- External Analysis API docs
- README/development status as appropriate

Add end-to-end acceptance scenarios using repositories that model:

1. a code-centric application,
2. a Markdown-heavy textbook/novel repository,
3. a mixed code + documentation repository,
4. a repository with GitHub Actions and generic shell scripts.

Validate especially that:

- book/novel edits no longer inflate code-only activity,
- code-only views still include normal source-code work,
- CI/CD filtering only removes confidently classified workflow changes,
- “All” matches previous aggregate behaviour,
- private-data isolation and privacy provenance still hold,
- large-account processing remains bounded.

**Acceptance criteria**

- backend, frontend, privacy, migration, Compose smoke and mobile suites pass.
- representative historical data can be backfilled.
- change-kind filtering is documented and consistent across the product.

---

## Deferred refinements

The first implementation should intentionally defer lower-confidence categories and inference.

Potential later additions:

- `TEST`
- `CONFIGURATION`
- `GENERATED`
- `DATA`
- `ASSET`
- recognising scripts referenced directly by CI workflows
- repository-specific/user correction rules for misclassified paths
- activity composition visualisations by change kind over time

The persistence/API model should allow these additions without another fundamental redesign.

## Suggested implementation order

Execute Steps 1–8 sequentially. Each step should leave the repository buildable and tested. Avoid implementing UI filtering before the persisted per-file data and shared backend semantics are stable.
