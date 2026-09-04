# UX filtering step 8.2 – Matching projects in Contributions

## Goal

Make the Contributions view follow the same Explore rule as Technologies and Project types:

> The project list must show exactly which projects the analysis above is based on.

## Changes

- Replaced the contribution-specific `Recently active projects` summary with the shared `MatchingProjects` component.
- `MatchingProjects` receives the exact same `AnalysisScope` as `useContributions`.
- The project result therefore respects Technology, Project type, Ownership and Period filters consistently.
- The shared result provides total count, pagination, empty/error/loading states and project-detail/back navigation.
- `ContributionsView` no longer owns a separate project-navigation callback; project detail is handled by `MatchingProjects`, as in the other migrated Explore views.

## Behavioural consequence

Before this step, Contributions displayed a limited `recentProjects` summary returned by the contribution endpoint. That list was not a full, paginated representation of the current analysis selection.

After this step, the visible project list is the canonical Projects query for the same AnalysisScope used by contribution statistics.

## Regression coverage

`ContributionsAnalysisScopeFeature.test.tsx` now verifies that:

- contribution statistics and MatchingProjects receive the same scope after filter changes;
- matching projects are visible in Contributions;
- project detail can be opened from the matching result;
- returning from project detail restores the matching-project result.

## Deferred work

The four contribution KPI cards are intentionally left unchanged in this step. Statistics compaction is handled by the later visual-simplification phase.
