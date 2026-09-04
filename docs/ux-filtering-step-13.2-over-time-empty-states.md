# UX filtering step 13.2 – Over time empty states

## Goal

Make an empty **Over time** section explain what happened instead of looking like a missing or broken chart.

## Behaviour

Technology and Project type now use the shared `AnalysisEmptyState` when their filtered timeline contains no active periods.

The empty state:

- says that no recorded activity was found for the current selection,
- shows the active technology/project type and period,
- offers **Clear period** when a time filter is active,
- preserves the selected technology/project type when the period is cleared,
- only offers filter actions when there is an actual period filter to relax.

Examples:

```text
No activity over time for this selection.
No recorded activity was found in projects where Java has been observed during the selected period.

Current selection
Technology: Java
Period: 2025

[Clear period] [Clear all filters]
```

and:

```text
No activity over time for this selection.
No recorded activity was found in Game projects during the selected period.
```

## Non-goals

This step does not change:

- technology/project-type timeline calculations,
- matching-project filtering,
- data-collection empty states such as “No technology assessments yet”,
- the generic project-detail timeline, where no analysis filters are involved.

## Tests

`ExploreCurrentBehaviourFeature.test.tsx` now verifies that empty Technology and Project type timelines:

1. explain why no activity is shown,
2. display the current selection,
3. expose **Clear period**,
4. remove `year`/`month` while retaining `technology`/`projectType`.
