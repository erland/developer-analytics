# Canonical Report Model

Status: **report-v1, Step 64**

Developer Analytics separates **report content** from **output format**.

`CanonicalReport` is the single logical report representation. Export formats
such as Markdown, PDF and future document formats must render from this model
instead of independently querying and assembling analytical content.

## Sections

The canonical model contains:

- summary,
- reporting period,
- data coverage,
- project categories,
- technology analysis,
- activity,
- significant projects,
- role / AI assessments,
- methodology,
- privacy scope.

## Privacy scopes

The model supports:

- `PUBLIC_ONLY`
- `PUBLIC_PLUS_PRIVATE_AGGREGATES`
- `FULL_PRIVATE_DETAIL`

The existing export choices map directly onto these report-model scopes. Hiding
private repository names is a rendering/input option used while the model is
built; private names are not exposed in the model when masking is requested.

## Separation of responsibilities

`CanonicalReportService`
: gathers user-scoped analytics and constructs `CanonicalReport`.

`CanonicalReport`
: contains report meaning and data, with no Markdown/PDF formatting.

`MarkdownReportRenderer`
: converts a completed canonical model to Markdown.

`ReportExportService`
: maps the user's explicit export privacy choices to the canonical privacy
scope and selects the renderer.

This boundary allows future PDF and other formats to reuse exactly the same
report contents and privacy decisions.

## Interpretation rules

Measured repository/contribution/evidence data and analytical inferences remain
semantically distinct. AI-generated role/profile interpretation is explicitly
represented as such and is omitted from a `PUBLIC_ONLY` report when its stored
privacy provenance includes private evidence.

User corrections affect analytical views but never delete the source facts used
to construct measured sections.
