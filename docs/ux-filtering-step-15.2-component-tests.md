# UX filtering step 15.2 – component tests for Explore selection consistency

## Goal

Lock the user-visible contract that an Explore selection affects both the time view and the matching project list in the same way.

## Added coverage

`frontend/src/test-layers/component/ExploreSelectionConsistency.test.tsx` now verifies three representative component flows:

1. **Technology = Java**
   - the Technology view renders Java's Over time analysis;
   - zero-activity 2025 is not rendered;
   - active 2024 and 2026 remain visible;
   - the matching project list receives `technologies: ['java']` and shows the Java result.

2. **Technology = Java + Year = 2026**
   - selecting 2026 updates URL/AnalysisScope;
   - the matching project list receives both the technology and year;
   - the rendered project changes to the result for that combined selection.

3. **Project type = Game + Year = 2026**
   - zero-activity periods are removed from Over time;
   - the initial matching project list uses `projectTypes: ['game']`;
   - selecting 2026 passes the same combined project-type/time scope to matching projects.

## Why this is separate from the broader feature tests

The existing Explore feature tests exercise longer navigation and regression flows. These component tests deliberately stay narrow and focus on the central consistency rule:

> The selection used to explain activity over time must be the same selection used to produce the project list below it.

This gives faster, more local failures if a future UI refactor accidentally lets the timeline and project query drift apart.

## Runtime impact

None. This step adds test coverage and documentation only.
