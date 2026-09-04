# Step 14.1 – Compact mobile filter panel

## Goal

Reduce vertical space used by Explore filters on small screens without hiding the current analysis context.

## Behaviour

`AnalysisFilters` now has a compact mobile header:

```text
Filters · 2 active                 Edit filters
Java ×   2026 ×
```

Active filter chips remain visible at all times. The select controls and `Clear all` action are collapsed by default below 720 px and are shown when the user chooses **Edit filters**. The action changes to **Done** while the controls are expanded.

Desktop behaviour is unchanged: all configured filter controls remain visible.

## Accessibility

The toggle uses `aria-expanded` and `aria-controls` to expose the state and relationship to the filter controls.

## Scope

This step changes only the responsive presentation of the shared `AnalysisFilters` component. Filter semantics, URL state and backend queries are unchanged.
