# UX filtering step 7.2 – Activity becomes the shared time analysis view

## Goal

Start consolidating Activity and Timeline without removing navigation yet.

## Changes

- Extracted the rich year → month → week drill-down into `ActivityTimelineAnalysis`.
- Activity now uses the same project-aware timeline analysis as Timeline.
- Activity adds `Colour by` alongside Period and Measure.
- Period presets remain Activity-specific and constrain the `/api/me/activity` data before drill-down.
- Period detail shows the projects that make up the selected year/month/week.
- Project rows in Activity can open the normal project detail view.
- The existing Timeline page is temporarily retained as a thin wrapper around the exact same shared analysis component.

## Why a shared component

Keeping two copies of the drill-down logic during migration would make the two views diverge again. Step 7.2 therefore establishes one implementation before Timeline navigation is removed in step 7.3.

## Behaviour preserved

- Changed lines / Commits
- Colour by project type / technology / none
- Year → month → week drill-down
- Project-level period breakdown
- Changed-line coverage notice
- Timeline navigation remains available during the parity period

## Next step

Step 7.3 verifies parity and removes the separate Timeline navigation/page.
