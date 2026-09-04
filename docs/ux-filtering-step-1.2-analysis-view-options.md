# UX filtering step 1.2 – AnalysisViewOptions

## Purpose

Step 1.2 introduces a shared representation for **presentation state** in Explore views. It complements `AnalysisScope`, which is reserved for state that changes which projects and observations belong to the analysis.

No existing view has been migrated to the model in this step, so there should be no visible behaviour change.

## Added model

`frontend/src/analysis/AnalysisViewOptions.ts`

The initial shared dimensions are:

- `metric`: `changed-lines` or `commits`
- `colourBy`: `none`, `technology` or `project-type`
- `sort`: key plus ascending/descending direction
- `groupBy`: optional presentation grouping

The model intentionally does **not** contain filters such as technology, project type, ownership, time period or search. Those remain in `AnalysisScope`.

## Why the separation matters

Today, Timeline mixes controls such as `Colour by` with drill-down selections such as year/month/week. The redesign needs to distinguish two different questions:

1. **What data am I analysing?** → `AnalysisScope`
2. **How should that data be displayed?** → `AnalysisViewOptions`

For example:

```text
FILTERS
Technology: Java
Year: 2025

VIEW
Measure: Changed lines
Colour by: Project type
```

Changing `Technology` or `Year` changes the matching project set. Changing `Measure` or `Colour by` does not.

## Tests

`frontend/src/test-layers/feature/AnalysisViewOptionsFeature.test.ts` verifies that:

- the empty view options object has no implicit choices,
- common presentation dimensions can be represented,
- analysis-filter fields are not part of the model,
- nested sort state is copied rather than shared.

## Next step

Step 1.3 will introduce URL serialization/parsing for `AnalysisScope`, preparing filter state to survive refresh, browser history and navigation between Explore views.
