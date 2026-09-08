# Canonical Report Model

Status: **report-v2**

Developer Analytics separates **report content** from **output format**.

`CanonicalReport` is the single logical report representation. Export formats such as Markdown, PDF and future document formats render from this model instead of independently querying and assembling analytical content.

## Sections

The canonical model contains:

- summary,
- reporting period,
- data coverage,
- change scope,
- project categories,
- technology analysis,
- activity,
- significant projects,
- role / AI assessments,
- methodology,
- privacy scope.

## Change scope

`report-v2` adds an explicit `changeScope`:

- `allChanges=true` means the historical unfiltered contribution model is used;
- otherwise `changeKinds` contains one or more of `CODE`, `DOCUMENTATION`, `CI_CD`, `OTHER`.

Filtered report activity is based on persisted changed-file classifications. A commit touching more than one selected category is counted once. Non-commit contribution types are not included in a filtered change-kind scope because they do not have changed-file classification. The methodology section records this distinction explicitly.

For backwards compatibility, callers that do not provide a change-kind selection receive **All** behaviour. Legacy internal constructors also default to All.

## Privacy scopes

The model supports:

- `PUBLIC_ONLY`
- `PUBLIC_PLUS_PRIVATE_AGGREGATES`
- `FULL_PRIVATE_DETAIL`

The existing export choices map directly onto these report-model scopes. Hiding private repository names is a rendering/input option used while the model is built; private names are not exposed in the model when masking is requested.

Change scope and privacy scope are independent. Filtering by change kind never widens the set of repositories or private data authorised for an export.

## Separation of responsibilities

`CanonicalReportService`
: gathers user-scoped analytics and constructs `CanonicalReport`, using `ContributionFileChange` only when a filtered change scope is requested.

`CanonicalReport`
: contains report meaning and data, with no Markdown/PDF formatting.

`MarkdownReportRenderer` / `PdfReportRenderer`
: convert the completed canonical model to the selected format.

`ReportExportService`
: maps the user's explicit export privacy choices and optional change-kind scope to the canonical model and selects the renderer.

## Interpretation rules

Measured repository/contribution/evidence data and analytical inferences remain semantically distinct. Changed-file categories are deterministic measured classifications with stored classifier metadata; they are not AI interpretations.

AI-generated role/profile interpretation is explicitly represented as such and is omitted from a `PUBLIC_ONLY` report when its stored privacy provenance includes private evidence.

User corrections affect analytical views but never delete the source facts used to construct measured sections.
