# UX filtering step 5.3 – Technology time selection in AnalysisScope

## Goal

Make Technology `Over time` drill-down part of the same `AnalysisScope` that drives the matching project result.

## Behaviour

- Selecting a year replaces any previous time constraint with `year=<YYYY>`.
- Selecting a month keeps the selected technology and sets both `year=<YYYY>` and `month=<YYYY-MM>`.
- `from`, `to` and `week` are cleared when a year/month is selected so time constraints cannot accidentally conflict.
- `MatchingProjects` receives the updated scope immediately, so the list below the chart represents exactly the selected technology and period.
- `Back to years` clears the timeline year/month constraint in one history update while preserving non-time filters such as technology and project type.
- URL deep links and browser back/forward restore the same year/month selection.

Example:

```text
?technology=java&year=2026&month=2026-08
```

means: projects where Java has been observed and where the user has activity overlapping August 2026.

## DrilldownTimeChart

`DrilldownTimeChart` remains backwards-compatible and can still manage year selection internally. Explore views that need shared filter state can now provide `year`, `month`, `onYearChange` and `onMonthChange` to make the chart controlled by their analysis scope.

## Verification

Regression coverage verifies that:

1. selecting 2026 updates the URL and matching-project scope;
2. selecting August 2026 adds the month constraint;
3. the period chip reflects the selected month;
4. a deep-linked month is restored as the active timeline selection;
5. browser navigation can change the time scope without dropping other analysis filters.
