# UX filtering step 9.1 – Projects uses shared AnalysisScope

## Goal

Move the Projects view onto the same shared analysis-selection model already used by Technologies, Project types and Contributions.

## Changed behaviour

The following Projects selections are now represented by `AnalysisScope` and the URL:

- Technology
- Project type
- Ownership
- Visibility
- Search
- Existing time parameters (`from`, `to`, `year`, `month`, `week`) when inherited from another Explore view

The Projects inventory request serialises that scope using the same `AnalysisScopeUrl` contract as the other Explore views.

`Activity = active|inactive` intentionally remains a project-list option. It describes recency relative to now and is not equivalent to an AnalysisScope time period. Pagination also remains local list state.

## Navigation behaviour

- Changing a shared filter resets pagination to page 1.
- Shared filter changes update the URL.
- Search updates use `replaceState` so typing does not create one browser-history entry per character.
- Browser back/forward reparses `AnalysisScope` from the URL and resets pagination.
- Opening and closing project detail retains the current scope.

## API behaviour

`useProjectInventory` now accepts:

```ts
{
  page,
  pageSize,
  activity,
  scope
}
```

It uses `analysisScopeToSearchParams(scope)` before adding pagination and the local Activity list option. This keeps project inventory queries aligned with `MatchingProjects` and other Explore queries.

## Facets

Technology and Project type options continue to come from server facets, so they are based on the complete matching result set rather than the current page.

## Deferred to step 9.2

The next step should make the visual distinction between **Analysis filters** and **Project list options** even clearer and decide whether Visibility/Search should remain visually grouped with project-list options despite being persisted in AnalysisScope.
