# UX filtering step 15.1 – analysis model unit tests

## Goal

Add fast unit-level regression coverage for the shared analysis contracts before the final cross-view regression pass.

## Added coverage

### AnalysisScope combinations

The unit suite verifies that multiple technologies/project types can coexist with ownership, visibility, search and a composed time selection, and that URL round-tripping preserves the complete scope.

### Project facets

Facet-to-filter-option conversion is now a pure shared function. This makes it explicit that filter choices come from server facets and are independent of the currently paginated project rows.

### Non-empty timeline periods

Technology and Project type views now use one shared predicate for deciding whether a period contains observable activity. Unit tests cover commits, changed-line evidence, matching project counts and project-type `activeProjectCount` rows.

## Refactoring included

- Added `analysis/AnalysisTimeline.ts`.
- Added `analysis/ProjectFacetOptions.ts`.
- Technology and Project type timeline filtering now use `nonEmptyActivityPeriods(...)`.
- Projects facet option mapping now uses `projectFacetOptions(...)`.

No user-visible behaviour is intended to change in this step; the refactoring centralises already-established behaviour and locks it with unit tests.
