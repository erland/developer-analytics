# UX filtering step 5.1 – Technology Over time semantics

## Goal

Make the meaning of **Technology → Over time** explicit before changing its filtering and drill-down behaviour.

## Semantic contract

For a selected technology, the timeline means:

> **Activity in projects where the selected technology has been observed.**

A repository is associated with a technology through deterministic repository technology evidence. User contribution/weekly activity for that repository is then attributed to the technology timeline.

The timeline therefore does **not** claim that every commit or changed line in a displayed month was itself made in that technology.

For example, if a repository has evidence for Java and the user was active in that repository in March 2026, that activity can appear in the Java timeline. This is a repository-level association, not commit-level language attribution.

## Implementation evidence

`TechnologyTimelineRepository.calculateMetricActivity(...)` joins repository technology evidence with contribution and weekly repository activity for the same repository. A code comment now records this contract next to the calculation so future refactoring does not silently strengthen the claim.

## UI wording

The Technology view now uses the heading:

```text
Activity in projects using Java
```

and explicitly explains that the timeline shows activity in projects where Java has been observed and does not imply that every commit or changed line in the period used Java.

The empty state uses the same semantics.

## Regression coverage

`ExploreCurrentBehaviourFeature.test.tsx` now verifies that the Technology view exposes both the semantic heading and the non-commit-level interpretation.

## Deliberately not changed in this step

- Timeline calculation.
- Which months are returned.
- Filtering of empty periods.
- Year/month/week selection.
- Application of other `AnalysisScope` dimensions to the technology timeline.

Those behavioural changes belong to the following Technology Over time steps.
