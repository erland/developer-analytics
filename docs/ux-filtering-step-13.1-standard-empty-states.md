# UX filtering step 13.1 – standard empty states

## Goal

Make empty filtered results explain **why** the result is empty and give the user an immediate way to broaden the analysis, instead of ending in a generic `No data` message.

## Shared pattern

A new `AnalysisEmptyState` component is used for filter-driven empty results. It can show:

- a specific empty-result heading and explanation;
- the current `AnalysisScope` as a compact selection summary;
- `Clear period` when a time selection is active;
- `Clear all filters` when any analysis filter is active;
- an optional view-specific recovery action.

The current selection is descriptive rather than another filter editor. Normal filter editing remains in `AnalysisFilters`.

## Integrated views

### MatchingProjects

When no projects match, the result now shows the current analysis selection. When the parent view owns scope changes, the user can clear the period or all filters directly from the empty state.

This applies to Technologies, Project types and Contributions, which pass their shared scope controller to `MatchingProjects`.

### Projects

The inventory uses the same empty-state pattern. If the local `Activity` list option is also active, an additional `Clear activity option` action is shown because Activity is deliberately not part of `AnalysisScope`.

### Activity

When an inherited Explore scope returns zero commits, Activity now explains that the current selection has no matching activity and offers scope recovery actions rather than rendering an empty timeline as if data collection itself were missing.

## Small consistency fix

`AnalysisFilters` now treats a search-only scope as active when deciding whether `Clear all` should be enabled. Search already belongs to `AnalysisScope`; the previous helper accidentally omitted it.

## Behavioural rule

Filtered empty states answer three questions in order:

1. What happened?
2. What selection produced this result?
3. What is the smallest useful action to broaden the result?

Unfiltered data-availability states such as “No technology assessments yet” remain distinct because clearing filters cannot solve them.
