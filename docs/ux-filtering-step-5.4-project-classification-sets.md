# UX filtering step 5.4 – complete project technology/type sets

## Goal

Remove the activity/timeline data-model assumption that a repository has only one technology or one project type.

## Change

`GET /api/me/activity` keeps the existing scalar fields:

- `technology`
- `projectType`

These remain the ranked primary labels for backwards compatibility and compact timeline colouring.

The project lifecycle now also exposes:

- `technologies: string[]`
- `projectTypes: string[]`

The arrays contain every distinct technology/project type observed for the repository.

## Why

A repository can contain Java, Quarkus, PostgreSQL and Docker simultaneously. It must still match a future `Technology = Java` AnalysisScope even if Quarkus (or another label) is chosen as the primary timeline label.

The same principle applies to project types when multiple classifications exist.

## Frontend

`useActivityView` normalises both arrays and remains compatible with older API responses by falling back to the scalar primary label.

Timeline project details display all returned labels. Timeline colour grouping still uses the primary scalar label so one project's activity is not double-counted across several coloured segments. Future AnalysisScope filtering must use the arrays rather than the primary scalar fields.

## Compatibility

No existing JSON field is removed or renamed. Existing clients that only consume `technology` and `projectType` continue to work.
