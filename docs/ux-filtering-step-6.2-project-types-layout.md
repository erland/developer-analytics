# UX filtering step 6.2 – Project types layout alignment

## Goal

Make Project types follow the same interaction hierarchy as Technologies so users do not need to learn a separate navigation model.

## Changes

`ProjectTypeViews` now uses the shared `AnalysisFilters` component with Project type as the visible filter. The previous sticky category list has been removed.

The selected project type is presented in this order:

1. Project type filter
2. Compact project type summary
3. Over time
4. Projects matching this selection
5. Secondary classification statistics

The project list is now the shared `MatchingProjects` component and therefore uses the complete `AnalysisScope`, including inherited technology/time/ownership filters when present.

The previous `representativeProjects` list and local project-detail state are no longer part of the Project types page. Project detail navigation is owned by `MatchingProjects`, as it is for Technologies.

Statistics that previously occupied three large metric cards are represented compactly in the summary and repeated in a closed `Classification statistics` disclosure for users who want the detail.

## Intentionally deferred

Step 6.2 does not yet make Project type timeline drill-down alter `AnalysisScope`. It also does not change which timeline periods the Project types API returns. Those semantics belong to the following Project type timeline step.

## Regression coverage

The Explore feature regression test now verifies that:

- the shared Project type filter changes URL-backed `AnalysisScope`;
- Over time is shown before the common matching-project result;
- `MatchingProjects` receives the selected project type;
- classification statistics are secondary and collapsed by default.
