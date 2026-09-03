# UX filtering redesign – final cleanup and regression review

## Purpose

Complete the step-by-step UX filtering plan with a conservative cleanup pass after the navigation, filtering, timeline, project-list and mobile changes have been implemented.

## Final architecture check

The Explore experience now uses one consistent model:

1. `AnalysisScope` describes what data is selected.
2. URL query parameters are the source of truth for shared Explore scope.
3. `useAnalysisScope` synchronises scope consumers and browser history.
4. `AnalysisFilters` renders shared analysis controls and active filter chips.
5. Project queries support multi-value technology/project type and time scope.
6. Project facets are calculated before pagination.
7. `MatchingProjects` is the shared project-result component.
8. Technologies and Project types use `selection -> over time -> matching projects -> secondary details`.
9. Activity is the single time-analysis view; the separate Timeline page has been removed.
10. Contributions and Projects use the same scope semantics.
11. Project detail navigation preserves scope through URL/history.
12. Secondary statistics use compact summaries/disclosures.
13. Empty states and mobile filter behaviour follow shared patterns.

## Cleanup performed

### Removed generated compiler artefacts

Deleted the accidental root files:

- `javac.20260901_165445.args`
- `javac.20260901_165705.args`

Added `javac.*.args` to `.gitignore` so local Java compiler argument dumps do not return.

### Removed obsolete Technology-list CSS

The old sticky Technology selector/list was removed from the UI in step 4.2, but its styles remained.

Removed unused selectors for:

- `.technology-layout`
- `.technology-list`
- `.technology-list-item`
- `.technology-list-item-active`
- `.technology-list-meta`
- their obsolete responsive variants

Styles still used by the current Technology/Project type detail views are retained.

### Timeline cleanup verified

The former `TimelineView.tsx` wrapper and navigation destination are absent.

Remaining names containing `timeline` are intentional shared concepts, for example:

- `ActivityTimelineAnalysis`
- `DrilldownTimeChart`
- technology/project-type timeline data
- `timeline.css`

These are not remnants of the removed navigation page.

### Local selection-state cleanup verified

The old page-local `selectedKey` state has been removed from Technology and Project type flows. Current `selectedKey` variables are derived values from `AnalysisScope`, not independent React state.

`selectedProjectId` remains intentionally encapsulated by `useProjectDetailNavigation`; it mirrors the URL `project` parameter and is part of the current navigation model rather than legacy hidden state.

### Compatibility fields retained intentionally

Technology and Project type APIs still expose `representativeProjects` even though the redesigned UI uses `MatchingProjects` instead.

They are retained during this cleanup pass because removing response fields would be an API-contract change with no UX benefit. They can be deprecated separately if desired.

## Regression coverage at completion

The redesign now has coverage for:

- AnalysisScope construction and URL round-trip
- AnalysisViewOptions separation
- shared filter component and active chips
- project facets independent of pagination
- matching-project query and pagination behaviour
- Technology selection/time/project consistency
- Project type selection/time/project consistency
- Activity scope and time drill-down
- Contributions scope and matching projects
- Explore cross-view scope persistence
- project-detail browser history
- compact mobile filters
- mobile content priority and time drill-down
- empty states
- summary/disclosure behaviour

## Validation notes

Repository-level frontend/backend test-layer structure checks pass in this workspace.

A full local `npm install` could not complete within the execution limit available to this environment, so the complete lint/typecheck/Vitest/Playwright suite should still be run by the repository's normal GitHub Actions workflow. No partial `node_modules` or generated npm lockfile is included in the delivery.

## Plan status

The UX filtering/navigation redesign plan is complete after this cleanup pass.

Future work should be treated as follow-up refinement rather than an unfinished step from the original plan.
