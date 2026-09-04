# UX filtering step 1.1 – shared AnalysisScope

Step 1.1 introduces the shared frontend model for the data set a user is currently analysing. It is intentionally a foundation-only change: no existing Explore view has been migrated to the model yet and no visible behaviour changes in this step.

## Added model

`frontend/src/analysis/AnalysisScope.ts` defines `AnalysisScope` with the common selection dimensions identified during the baseline inventory:

- technologies
- project types
- ownership
- visibility
- date range
- year
- month
- week
- search

Technology and project-type selections are arrays from the beginning so the model does not lock the later UI into single-select behaviour.

## Explicit boundary

`AnalysisScope` describes **what data is included in the analysis**. It deliberately does not contain presentation options such as:

- metric (`commits` / `changed lines`)
- colour mode
- sort order
- grouping

Those belong to view options and are introduced separately in step 1.2.

## Defaults

`emptyAnalysisScope` represents an unfiltered scope. `createAnalysisScope()` creates a new scope with independent technology/project-type collections and optional overrides.

## Regression coverage

`AnalysisScopeFeature.test.ts` verifies:

1. the empty scope has only selection state;
2. all planned common selection dimensions can be represented;
3. factory-created scopes do not share array instances.

## Deliberately not changed yet

Existing local state remains in:

- `TechnologyViews`
- `ProjectTypeViews`
- `TimelineView`
- `ActivityView`
- `ProjectInventoryView`

Migrating those views starts only after the common model and view-option boundary are established.

## Next step

Step 1.2 introduces a separate `AnalysisViewOptions` model for presentation choices such as metric and colour-by.
