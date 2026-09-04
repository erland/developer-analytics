# UX filtering step 3.2 – Projects query supports AnalysisScope

## Goal

Extend the project inventory backend so the shared `AnalysisScope` can be represented without changing the current frontend yet.

## Added query semantics

`GET /api/me/project-inventory` now supports:

- repeated `technology=` values
- repeated `projectType=` values
- legacy repeated `category=` values for backwards compatibility
- `from=YYYY-MM-DD`
- `to=YYYY-MM-DD`
- `year=YYYY`
- `month=YYYY-MM`
- `week=YYYY-MM-DD` or `YYYY-Www`

Multiple values inside one dimension use OR semantics. Different dimensions are combined with AND semantics.

Example:

```text
/api/me/project-inventory?technology=java&technology=typescript&projectType=backend-service&year=2026
```

means:

```text
(Java OR TypeScript) AND Backend service AND active during 2026
```

## Time semantics

Time filtering is based on `repository_user_activity_week`, not merely `source_repository.last_activity_at`.

A project therefore matches a period when one of its weekly user-activity buckets overlaps the requested inclusive date range.

This is intentionally different from the existing `activity=active|inactive` filter:

- `activity=active|inactive` remains a recency/status filter based on `lastActivityAt`.
- AnalysisScope time fields represent historical activity during a selected period.

If year, month, week and/or explicit from/to are combined, their ranges are intersected. Non-overlapping combinations return HTTP 400 instead of silently producing a misleading query.

## Compatibility

The existing frontend currently sends `category=<key>` and `technology=<key>`. Both remain supported, so this change is additive.

## Not included

This step does not yet:

- add facets to the response
- migrate frontend views to `AnalysisScope`
- change filter controls
- change pagination or project rendering

Those are later steps in the plan.
