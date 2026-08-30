# PDF Reports

Step 67 adds PDF as a second renderer of the same canonical report model used by
Markdown.

## Shared content path

Both Markdown and PDF consume:

1. `CanonicalReportService`
2. `CanonicalReport`
3. `ReportSectionPlanner`

The section planner decides which canonical sections belong to each report type.
There is no separately authored PDF analysis path.

## Print layout

PDF uses an A4-specific backend layout. It deliberately does not reuse the
responsive dashboard DOM/CSS.

Design rules:

- long text is wrapped to the printable width,
- technology and significant-project data are rendered as wrapping cards rather
  than wide seven-column tables,
- monthly activity uses a compact horizontal print chart,
- new pages are created before content would cross the footer,
- every page carries the canonical privacy scope in the header and footer,
- AI interpretation retains its explicit `AI-generated interpretation` marking.

The PDF renderer therefore remains readable regardless of mobile/desktop
dashboard layout.

## API

The existing preview flow is unchanged. Report generation accepts
`outputFormat`:

```json
{
  "outputFormat": "PDF",
  "reportType": "FULL_DEVELOPER_REPORT",
  "privateDataMode": "EXCLUDE_PRIVATE",
  "hidePrivateRepositoryNames": true,
  "generationConfirmed": true
}
```

`MARKDOWN` remains supported through the same endpoint. PDF responses use
`application/pdf` and `.pdf` filenames; Markdown uses
`text/markdown; charset=utf-8`.

Changing output format in the UI invalidates the existing privacy preview and
requires previewing again before generation.
