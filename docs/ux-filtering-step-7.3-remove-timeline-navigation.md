# UX filtering step 7.3 – remove separate Timeline navigation

## Goal

Complete the Activity/Timeline consolidation started in step 7.2 by removing the duplicate Timeline page and primary-navigation entry once Activity has functional parity.

## Parity verified before removal

The Activity regression coverage introduced in step 7.2 verifies that Activity now provides the behaviours that previously justified the separate Timeline page:

- Changed lines / Commits metric selection
- Colour by Project type / Technology / None
- year → month → week drill-down
- period-specific project rows
- opening project detail from a selected period
- changed-line coverage messaging through the shared analysis implementation

Both pages used the same `ActivityTimelineAnalysis` component during the transition, so keeping a second page no longer added analytical capability.

## Changes

- Removed `Timeline` from `AuthenticatedShell` primary navigation.
- Removed the `section === 'Timeline'` rendering branch.
- Removed the transitional `TimelineView.tsx` wrapper.
- Removed the duplicate Timeline-specific regression test; Activity remains the canonical regression path for the shared timeline behaviour.
- Extended responsive navigation coverage to assert that Activity is present and Timeline is no longer exposed as a separate navigation item.

## Runtime result

There is now one time-analysis destination in the main application navigation: **Activity**.

This reduces navigation ambiguity while preserving the richer timeline analysis introduced in step 7.2.

## Next step

The next planned phase is step 8.1: align Contributions with `AnalysisScope` where relevant, followed by a matching-project result list.
