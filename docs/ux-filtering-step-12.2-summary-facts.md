# UX filtering redesign – step 12.2: compact summary facts

## Goal

Reduce scroll cost from secondary KPI statistics without hiding information. Large `metric-grid` / `metric-card` blocks are replaced by one shared compact presentation so primary analysis content appears earlier.

## Shared component

`frontend/src/components/SummaryFacts.tsx` renders labelled values as an accessible definition list. It is:

- inline and wrapping on desktop;
- two-column on normal mobile widths;
- single-column on very narrow screens;
- intentionally normal-sized typography rather than dashboard-style oversized numbers.

## Migrated views

### Activity

The previous five KPI cards are replaced by one `Activity summary` containing:

- commits;
- active projects;
- average commit size;
- additions;
- deletions;
- activity period.

The separate Activity period section after the timeline has been removed because the same information now lives in the summary. The changed-line coverage warning remains immediately visible when relevant.

### Contributions

The four KPI cards are replaced by `Contribution summary` containing commits, pull requests, reviews and issues. `MatchingProjects` remains immediately after the contribution result.

### Overview

The six repository/activity cards are replaced by one compact `Repository overview` summary. Overview still exposes all summary facts because summarisation is its purpose, but no longer spends six card-heights doing so.

### Project detail

The six activity cards are replaced by `Project activity summary`. Contributor totals are also rendered as an embedded `Contributor summary` rather than three additional cards.

## Cleanup

No component uses the generic `.metric-grid` / `.metric-card` pattern after this migration, so those legacy styles and local Metric/MetricCard helper functions were removed.

## Behaviour

This is a presentation-only change. It does not modify:

- analysis/filter semantics;
- API requests;
- metric calculations;
- privacy provenance;
- timeline/drill-down behaviour;
- project navigation.

## Verification

A component regression test verifies that `SummaryFacts` exposes labels/values accessibly and does not emit the legacy metric-card markup. Existing application tests continue to assert the same statistic labels and values.
