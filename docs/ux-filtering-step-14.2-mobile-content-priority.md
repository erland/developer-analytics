# UX filtering step 14.2 – mobile content priority

## Goal

Keep the useful analysis and matching work ahead of secondary statistics, especially on narrow screens where vertical space is scarce.

The target order is:

1. current selection / controls
2. over-time analysis
3. matching projects
4. compact statistics
5. secondary details

## Changes

### Activity

The activity timeline/drill-down now comes before the compact Activity statistics summary in the semantic DOM order. The changed-line coverage warning stays directly with the timeline because it explains the selected measure.

New order:

1. inherited AnalysisScope filters, when present
2. Activity window / Measure / Colour by controls
3. changed-line coverage note, when relevant
4. year → month → week activity analysis and projects for a selected period
5. Activity statistics summary

### Contributions

`MatchingProjects` now comes before the compact Contribution statistics summary.

New order:

1. AnalysisScope filters
2. matching projects
3. Contribution statistics

This makes the project set—the evidence behind the selected analysis—visible before aggregate counts.

### Technologies and Project types

No structural change was required. Their existing order already follows the intended hierarchy:

1. AnalysisFilters
2. selected technology/project-type context summary
3. Over time
4. MatchingProjects
5. collapsed statistics/details

The selected-item hero is retained before Over time because it identifies what is being analysed rather than acting as a KPI dashboard.

## Accessibility

The priority is implemented in DOM order rather than only with CSS `order`. Screen readers, keyboard navigation, desktop and mobile therefore encounter the same information hierarchy.

## Behaviour deliberately unchanged

- AnalysisScope semantics and URL state
- Activity drill-down behaviour
- Contribution calculations
- MatchingProjects pagination/detail navigation
- technology/project-type evidence semantics
