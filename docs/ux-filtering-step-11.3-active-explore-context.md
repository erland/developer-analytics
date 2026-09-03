# Step 11.3 – Active Explore context in navigation

The Explore navigation group now shows a small count badge whenever the URL-backed `AnalysisScope` contains active filters. The badge is deliberately compact: it communicates that context is being carried between Explore views without repeating every filter in the sidebar.

## Counting rules

- each selected technology counts once
- each selected project type counts once
- ownership, visibility and search count once when active
- time counts as one period filter even when represented by both `year` and `month` (or `from`/`to`)

For example, `technology=java&year=2026&month=2026-08` renders an Explore badge of `2`, not `3`.

## Shared URL state notification

`useAnalysisScope` now emits an internal `developer-analytics:analysis-scope-change` browser event after `pushState`/`replaceState`. Other mounted consumers of the hook (including the application shell) listen for that event as well as `popstate`, so the badge updates immediately when a child Explore view changes scope.

This keeps the URL as the source of truth while avoiding a separate global React store.
