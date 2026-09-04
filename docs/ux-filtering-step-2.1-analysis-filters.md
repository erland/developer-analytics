# UX filtering step 2.1 – shared AnalysisFilters component

This step introduces the first reusable filter UI for Explore views without
migrating any existing view to it yet.

## Added

- `frontend/src/components/AnalysisFilters.tsx`
- component-layer regression tests
- compact desktop and mobile styling

## Supported dimensions

The first version can be configured independently to show:

- Technology
- Project type
- Ownership
- Period
- Clear all

The component is controlled: callers own the `AnalysisScope` and receive the
complete next scope through `onChange`.

Technology and project type controls are single-selection UI in this first
iteration, while the underlying `AnalysisScope` deliberately remains plural so
multi-selection can be added later without changing the shared data model.

Period options are supplied by the consuming view and map directly to the
shared time fields (`from`, `to`, `year`, `month`, `week`). Selecting a new
period clears stale lower-level time drill-down state before applying the new
period.

## Not changed yet

No existing Explore view renders `AnalysisFilters` in this step. Technologies,
Project types, Projects, Activity and Timeline therefore retain their existing
behaviour until later migration steps.

## Next step

Step 2.2 adds visible active-filter chips so individual filters can be removed
without clearing the complete scope.
