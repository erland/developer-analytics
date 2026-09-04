# UX filtering step 4.4 – Technology evidence as secondary detail

## Goal

Keep the primary Technology analysis focused on the current selection, activity over time and matching projects. Evidence/statistical detail should remain available without occupying permanent vertical space.

## Change

`TechnologyViews` now renders an initially collapsed **Evidence and statistics** disclosure after `MatchingProjects` and before **Advanced**.

The disclosure contains:

- evidence level
- evidence score
- project count
- evidence item count
- independent evidence type count
- recent project count
- first and latest observation
- data provenance

The compact summary introduced in step 4.3 remains visible at the top of the Technology detail. The disclosure therefore provides detail and verification rather than becoming another primary dashboard block.

## Interaction order

The Technology view now follows:

1. Analysis filters
2. Compact Technology summary
3. Over time
4. Matching projects
5. Evidence and statistics (collapsed by default)
6. Advanced correction controls

## Regression coverage

The Technology feature regression test verifies that:

- the old large Technology statistics section is absent;
- Evidence and statistics is collapsed initially;
- it can be expanded;
- evidence metadata and privacy provenance are available inside it.
