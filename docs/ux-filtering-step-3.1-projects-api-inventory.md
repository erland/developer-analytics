# UX filtering step 3.1 – Projects API and filter responsibility inventory

## Purpose

This step records the current Projects filtering contract before the shared `AnalysisScope` is connected to backend queries and before facets are introduced.

No runtime behaviour is changed in this step.

## Current request flow

The current Projects view has three distinct layers:

1. `ProjectInventoryView` owns local `InventoryFilters` state.
2. `useProjectInventory` converts that state into query parameters.
3. `GET /api/me/project-inventory` applies most filters server-side in `ProjectInventoryRepository`.

Current endpoint:

```text
GET /api/me/project-inventory
```

## Current filter matrix

| Filter | Frontend state | Sent to backend | Server-side filtering | Notes |
|---|---:|---:|---:|---|
| `page` | Yes | Yes | Yes | Zero-based; backend clamps to >= 0. |
| `pageSize` | Yes | Yes | Yes | Backend clamps to 1–100. |
| `search` | Yes | Yes | Yes | Matches repository name or description case-insensitively. |
| `ownership` | Yes | Yes | Yes | `own` means `OWNED_BY_USER`; `external` means everything else. |
| `visibility` | Yes | Yes | Yes | Supports `public` and `private`. |
| `activity` | Yes | Yes | Yes | `active` means last activity within one year of current UTC time. |
| `category` | Yes | Yes | Yes | Implemented with an `exists` query on `RepositoryProjectCategory`. |
| `technology` | Yes | Yes | Yes | Implemented with an `exists` query on `RepositoryTechnologyEvidence`. |
| `from` / `to` | No | No | No | Not supported by project inventory today. |
| `year` | No | No | No | Not supported by project inventory today. |
| `month` | No | No | No | Not supported by project inventory today. |
| `week` | No | No | No | Not supported by project inventory today. |
| multiple technologies | No | No | No | Current API accepts a single `technology` key. |
| multiple project types | No | No | No | Current API accepts a single `category` key. |

## Important finding: most current Projects filtering is already server-side

The existing implementation already avoids client-side filtering of the project result set. `ProjectInventoryRepository.find(...)` constructs the complete query and a matching count query, then applies pagination after filtering.

This means the upcoming refactoring does **not** need to redesign the basic project-inventory query from scratch.

The existing query can be evolved into the canonical backend query for `AnalysisScope`.

## Important finding: Technology and Category filter choices are page-local

`ProjectInventoryView` currently derives `knownCategories` and `knownTechnologies` from:

```text
inventory.data.items
```

Those items are only the current page of the paginated response (25 by default).

Consequences:

- a technology can disappear from the filter dropdown merely because no project on the current page contains it;
- changing sorting or pagination can change which filter choices are visible;
- the dropdown does not represent the complete user's analyzable project set;
- counts cannot be shown reliably;
- this behaviour cannot support the planned faceted filtering model.

This is the primary reason step 3.3 needs backend-provided facets.

## Important finding: time is split across APIs

Project inventory has no time-range filter.

`GET /api/me/activity` already supports:

```text
from=YYYY-MM-DD
to=YYYY-MM-DD
```

but that date constraint is applied to contribution/activity aggregation, not to `GET /api/me/project-inventory`.

Therefore the desired interaction:

```text
Technology = Java
Year = 2025
→ matching projects
```

cannot currently be expressed as one Project inventory query.

A later step must define what it means for a project to match a time scope. The recommended semantic is:

> A project matches a selected period when the user has observed activity for that project during the selected period.

This should be implemented against activity/contribution data, rather than merely comparing `SourceRepository.lastActivityAt`.

## Important finding: `activity=active|inactive` is not an analysis time filter

The current Projects `activity` filter means:

```text
active   = lastActivityAt >= now - 1 year
inactive = no lastActivityAt or lastActivityAt < now - 1 year
```

This is a repository recency classification.

It is **not** equivalent to an `AnalysisScope` period such as:

```text
2025
March 2025
Week of 2025-03-10
```

The two concepts should remain separate during migration.

## Important finding: current API supports one technology and one project type

`AnalysisScope` was intentionally defined with arrays:

```text
technologies: string[]
projectTypes: string[]
```

The current Projects API accepts only:

```text
technology=<key>
category=<key>
```

Before multiple selections are exposed in UI, backend semantics need to be explicit.

Recommended semantics for values within the same dimension:

```text
technology=java + technology=typescript
```

should be **OR** within the Technology dimension:

> projects using Java OR TypeScript

while dimensions are combined with **AND**:

```text
(Java OR TypeScript)
AND
(Backend OR CLI)
AND
Ownership = Own
AND
Period = 2025
```

This matches normal faceted-search behaviour and is easier for users to understand.

## Existing data returned by project inventory

Each item already contains enough metadata for a reusable matching-project list:

- repository id;
- name;
- description;
- GitHub URL;
- ownership relation;
- visibility;
- latest activity;
- all returned project categories;
- all returned technologies.

This is a good base for the planned `MatchingProjects` component.

## Current response limitations

The response currently contains:

```text
items
total
page
pageSize
totalPages
```

It does not contain:

- technology facets;
- project-type facets;
- ownership facets;
- visibility facets;
- time facets;
- counts per facet value.

Therefore frontend cannot construct stable filter choices from the response without loading the complete project set.

Loading the complete project set client-side should **not** be used as the solution.

## Mapping from `AnalysisScope` to current Projects API

| `AnalysisScope` field | Current Projects equivalent | Migration status |
|---|---|---|
| `technologies[]` | `technology` | Partial: one value only. |
| `projectTypes[]` | `category` | Partial: one value only. |
| `ownership` | `ownership` | Supported. |
| `visibility` | `visibility` | Supported. |
| `search` | `search` | Supported. |
| `from` / `to` | none | Missing. |
| `year` | none | Missing. |
| `month` | none | Missing. |
| `week` | none | Missing. |

Pagination (`page`, `pageSize`) is list state and is deliberately not part of `AnalysisScope`.

The legacy `activity=active|inactive` filter is also not part of the current shared scope because it represents repository recency rather than a selected analysis period.

## Recommended implementation direction

### Step 3.2

Evolve the backend query so it can represent the portions of `AnalysisScope` that affect matching projects.

Priority order:

1. retain all existing filters;
2. add selected-period filtering based on actual user project activity;
3. support repeated Technology values;
4. support repeated Project type/category values;
5. preserve pagination and total-count correctness.

The endpoint can initially remain:

```text
GET /api/me/project-inventory
```

There is no immediate need to introduce a parallel `/api/me/projects` collection endpoint solely for this UX refactoring.

### Step 3.3

Add backend facets to the same response or a tightly related endpoint.

Recommended response shape:

```json
{
  "items": [],
  "total": 23,
  "page": 0,
  "pageSize": 25,
  "totalPages": 1,
  "facets": {
    "technologies": [
      {"key": "java", "name": "Java", "count": 17}
    ],
    "projectTypes": [
      {"key": "backend", "name": "Backend", "count": 12}
    ],
    "ownership": [
      {"key": "own", "count": 19},
      {"key": "external", "count": 4}
    ]
  }
}
```

Facet counts must be computed from the complete matching dataset, never from the current page.

## Facet-count semantics to decide in step 3.3

For a conventional faceted UI, each facet should normally retain all *other* active dimensions while ignoring its own dimension when calculating available choices.

Example active scope:

```text
Technology = Java
Ownership = Own
```

Technology facets should answer:

> Among own projects matching all non-Technology filters, which technologies could be selected and how many projects would they match?

Ownership facets should answer the corresponding question while retaining Java.

This avoids facet choices collapsing unnecessarily after a value is selected.

If implementation complexity is too high for the first facet version, a simpler all-active-filters count can be introduced temporarily, but that semantic must be explicit in tests and documentation.

## Tests needed when behaviour changes

The current regression test that confirms page-local Technology/Category choices is intentionally a baseline test. It must be changed when facets are introduced.

New backend tests should cover at minimum:

- search + ownership combination;
- technology + project type combination;
- multiple Technology OR semantics;
- multiple Project type OR semantics;
- cross-dimension AND semantics;
- period + Technology;
- period + Project type;
- pagination count after filtering;
- facets are independent of current page.

Frontend tests should verify that filter options come from facets rather than `items`.

## Conclusion

The Projects backend is already a suitable foundation for the shared analysis model. The main missing capabilities are not basic filtering, but:

1. **period-aware project matching**;
2. **multi-value dimension support**;
3. **backend-provided facets and counts**.

The first concrete behavioural backend change should therefore be step 3.2, extending `ProjectInventoryRepository` and the resource contract to accept the shared analysis dimensions without changing pagination or privacy semantics.
