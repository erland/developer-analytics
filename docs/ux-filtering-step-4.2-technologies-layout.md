# UX filtering step 4.2 – Technologies analysis flow

## Goal

Reorder the Technologies view around the shared Explore interaction model:

1. analysis filters,
2. selected technology summary,
3. over-time analysis,
4. projects matching the current AnalysisScope,
5. secondary statistics and advanced correction controls.

## Changes

- Replaced the technology-specific sticky selector list with the shared `AnalysisFilters` Technology selector.
- The filter is controlled by the `AnalysisScope` introduced in step 4.1 and writes the full scope back to the URL.
- Reused `MatchingProjects` so the project result is driven by the same scope as the selected technology and any inherited scope parameters.
- Moved the six metric cards below the timeline and matching projects. They are intentionally retained for now; metric compression belongs to step 4.3.
- Moved suppression/correction controls to the end of the primary analysis flow and labelled the section `Advanced`.
- Removed the Technologies component's own project-detail state because project navigation is now owned by `MatchingProjects`.

## Behaviour after this step

A Technologies page is read top-to-bottom as:

```text
Technology filter
Selected technology summary
Over time
Projects matching this selection
Statistics
Advanced correction
```

The selected technology continues to be URL-backed, and browser back/forward remains supported.

## Deliberate limitation

The technology-specific timeline still comes from the existing `/api/me/technologies` aggregate. Additional inherited AnalysisScope dimensions (for example a year or project type) are already applied to `MatchingProjects`, but are not yet applied to that technology timeline. The later timeline filtering steps will close that gap; this step is specifically the structural/layout migration.
