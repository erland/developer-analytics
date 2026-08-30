# Markdown Reports

Status: **Step 65**

Markdown is the first canonical export format. Every Markdown report is rendered
from the same `CanonicalReport` (`report-v1`) built in Step 64. Report variants do
not independently query the database or maintain separate analytical logic.

## Supported report types

### `PUBLIC_OSS_REPORT`

Public open-source profile intended for sharing outside the private dashboard.
The backend always forces this report to `PUBLIC_ONLY`, regardless of any
private-data option sent by a client.

Includes:

- summary and report metadata,
- data coverage,
- project categories,
- technology analysis,
- activity,
- significant public projects,
- methodology.

AI role/profile interpretation is deliberately omitted from this share-oriented
variant.

### `FULL_DEVELOPER_REPORT`

The broadest Markdown report. It can use any explicitly selected export privacy
mode and includes:

- data coverage,
- project categories,
- technology analysis,
- activity,
- significant projects,
- optional AI-generated role/profile assessment,
- methodology.

### `TECHNOLOGY_PROFILE`

Focused on technology history/evidence and, when privacy-compatible, the optional
AI-generated role/profile interpretation. Data coverage and methodology are
always included.

### `ACTIVITY_REPORT`

Focused on contribution totals and monthly activity. Data coverage and
methodology are always included.

## Privacy

The existing explicit per-export privacy choices remain mandatory. The public OSS
report is additionally protected server-side by forcing private data exclusion.
For the other variants, export privacy maps onto the canonical model scopes:

- `EXCLUDE_PRIVATE` → `PUBLIC_ONLY`
- `INCLUDE_PRIVATE_AGGREGATES` → `PUBLIC_PLUS_PRIVATE_AGGREGATES`
- `INCLUDE_FULL_PRIVATE_DETAIL` → `FULL_PRIVATE_DETAIL`

Private repository-name masking continues to apply when full private detail is
selected.

## API

`POST /api/me/reports/export` now requires:

```json
{
  "reportType": "FULL_DEVELOPER_REPORT",
  "privateDataMode": "EXCLUDE_PRIVATE",
  "hidePrivateRepositoryNames": true
}
```

The response is `text/markdown`, has a report-specific filename in
`Content-Disposition`, and exposes `X-Report-Type` and `X-Report-Model-Version`
headers.
