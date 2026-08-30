# Report Privacy Preview

Step 66 introduces a mandatory two-stage report workflow.

## 1. Preview

`POST /api/me/reports/preview` receives the report type and explicit export
privacy choices. It builds the same canonical report model used by the final
export but does **not** create a downloadable file.

The preview returns:

- whether private repositories are included,
- whether private repository names are included,
- whether an AI assessment is included,
- the effective public/private report scope,
- first and last analysed activity timestamps,
- repository and contribution coverage,
- canonical report-model version.

The values are based on the **effective server-side policy**. For example a
`PUBLIC_OSS_REPORT` is always previewed as `PUBLIC_ONLY` even if the request
contains a broader private-data selection.

## 2. Explicit generation

After reviewing the preview, the UI exposes a separate **Generate Markdown
report** action.

`POST /api/me/reports/export` now requires:

```json
{
  "reportType": "FULL_DEVELOPER_REPORT",
  "privateDataMode": "EXCLUDE_PRIVATE",
  "hidePrivateRepositoryNames": true,
  "generationConfirmed": true
}
```

Requests without `generationConfirmed=true` are rejected. The preview endpoint
does not accept or imply generation confirmation.

Changing report type or either privacy selection invalidates the current preview
in the frontend and requires a new preview before generation.
