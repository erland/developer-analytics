# UX filtering step 3.3 – project facets

## Goal

Make Project filter choices describe the complete filtered result set rather than the current paginated page.

## API response

`GET /api/me/project-inventory` now includes:

```json
{
  "items": [],
  "total": 42,
  "page": 0,
  "pageSize": 25,
  "totalPages": 2,
  "facets": {
    "technologies": [
      { "key": "java", "name": "Java", "count": 23 }
    ],
    "projectTypes": [
      { "key": "backend-service", "name": "Backend service", "count": 17 }
    ],
    "ownership": [
      { "key": "own", "name": "Own", "count": 19 },
      { "key": "external", "name": "External", "count": 4 }
    ]
  }
}
```

Facet counts are repository counts, not evidence-row counts.

## Semantics

Facets are calculated from all repositories that match the current query **before pagination**. This means:

- page and pageSize never change facet counts;
- all active query filters, including AnalysisScope time filters from step 3.2, affect the facets;
- technologies count distinct repositories even when several evidence rows exist for the same technology;
- project types count distinct repositories even when several classification sources exist;
- ownership maps `OWNED_BY_USER` to `own`; organization-owned and other external repositories are grouped as `external` to match the existing UI filter semantics.

This is intentionally the first facet contract. A later UX iteration may choose self-excluding facets (where a dimension is calculated without its own active filter) if multi-select exploration requires it.

## Frontend

`ProjectInventoryView` now takes Technology and Category/Project type options from `response.facets` rather than deriving them from `response.items`.

Counts are shown in the options, for example:

```text
Java (23)
Backend service (17)
```

This removes the previous pagination-dependent behaviour.

## Compatibility

The existing `items`, `total`, `page`, `pageSize` and `totalPages` fields are unchanged. The `facets` field is additive.

The existing legacy `category` query parameter continues to work alongside `projectType` as established in step 3.2.

## Tests

Updated coverage verifies:

- the inventory response model carries facets;
- the frontend renders facet values that do not occur on the current page;
- existing project-filter and project-detail/back behaviour remains covered.
