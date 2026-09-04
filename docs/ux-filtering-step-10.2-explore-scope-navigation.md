# UX filtering step 10.2 – preserve AnalysisScope across Explore navigation

## Goal

Keep the active analysis selection when the user changes Explore perspective, for example:

`Technologies → Activity → Projects`.

The selection must remain visible and must continue to constrain the data, not merely survive as unused query parameters.

## Implementation

- The authenticated shell continues to switch sections without replacing `location.search`, so the AnalysisScope query is retained.
- `ActivityView` now reads the shared `useAnalysisScope()` controller.
- When a scope is active, Activity shows its inherited filter chips and allows individual removal / Clear all.
- The existing Activity period preset is labelled **Activity window** to distinguish it from the shared AnalysisScope period.
- `useActivityView(period, scope)` serializes the same AnalysisScope contract used by the other Explore views.
- `/api/me/activity` now accepts Technology, Project type, Ownership, Visibility, Search and hierarchical time scope.
- Activity uses `ProjectInventoryRepository` to resolve the complete set of matching repository IDs, ensuring the same project-selection semantics as Projects.
- Commit rows, weekly line statistics and project lifecycle output are restricted to those repositories and the effective period.
- The local Activity window and an inherited AnalysisScope date range are intersected rather than one silently replacing the other.

## Behaviour

With `Technology = Java` and `Year = 2026` selected:

1. Navigate from Technologies to Activity.
2. Java and 2026 remain visible as active filters.
3. Activity data is restricted to Java projects in the effective 2026/activity-window intersection.
4. Navigate to Projects.
5. The same Java + 2026 scope remains active there.

## Tests

- `ActivityAnalysisScopeFeature.test.tsx` verifies inherited scope display, use by Activity loading and individual chip removal.
- `AuthenticatedShellResponsive.test.tsx` verifies that Explore section navigation preserves the query string.
- `MeActivityResourceQueryTest` locks the backend use of the shared project-filter semantics and repository restriction.
