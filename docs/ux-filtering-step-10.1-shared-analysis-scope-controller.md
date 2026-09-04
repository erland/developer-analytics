# UX filtering step 10.1 – shared AnalysisScope controller

## Goal

Make the browser URL the single implementation point for Explore analysis state instead of letting each view implement its own `location.search`, `pushState`, `replaceState` and `popstate` handling.

## Implementation

A shared `useAnalysisScope()` hook now owns the URL/state bridge.

It provides:

- `scope` – parsed from the current URL;
- `pushScope(scope)` – records a new analysis selection in browser history;
- `replaceScope(scope)` – replaces the current history entry for transient/default changes;
- automatic `popstate` handling for browser back/forward;
- preservation of the current pathname and hash while query parameters are rewritten through the existing `AnalysisScopeUrl` contract.

The following Explore views now use the shared controller:

- Technologies;
- Project types;
- Contributions;
- Projects.

This removes four separate copies of URL parsing/history synchronization while keeping current user-visible behaviour unchanged.

## State ownership

The URL remains the source of truth for analysis selection. The hook keeps a React snapshot so rendering updates immediately after a write, but browser navigation always re-parses `window.location.search`.

View-only state remains local. Examples include:

- Projects pagination and `Activity` list option;
- activity metric/colour presentation options;
- open/closed detail/disclosure state.

## Verification

`AnalysisScopeControllerFeature.test.tsx` covers:

1. initial parsing from the URL;
2. `pushScope` serialization while preserving pathname/hash;
3. `replaceScope` without adding a history entry;
4. browser `popstate` re-parsing.

Existing Explore regression tests continue to exercise deep links and back/forward behaviour through the migrated views.

## Next step

Step 10.2 should make Explore navigation preserve the active AnalysisScope when switching between views such as Technologies → Activity → Projects.
