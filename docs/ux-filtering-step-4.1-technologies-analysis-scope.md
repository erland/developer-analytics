# UX filtering step 4.1 – Technologies selection in AnalysisScope

## Goal

Move the selected technology out of `TechnologyViews` local `selectedKey` state and into the shared `AnalysisScope` URL contract introduced in steps 1.1–1.3.

This is intentionally a state migration only. The larger Technologies layout redesign happens in later 4.x steps.

## Behaviour

- The selected technology is read from the repeated `technology` query parameter.
- Technologies remains a single-selection view for now; selecting a technology replaces the technology dimension with one key.
- If no technology is present, the first available technology becomes the default and is written with `history.replaceState`.
- If the URL contains a technology that is no longer available, the first available technology replaces it.
- Explicit user selection uses `history.pushState`, making the selection part of browser navigation history.
- `popstate` reparses `AnalysisScope`, so browser back/forward restores the technology selection.
- Opening and closing project detail leaves the current technology query parameter unchanged.
- Non-technology query parameters are preserved when technology selection changes. This is important for upcoming Project type, time and ownership filters.

## Not changed yet

- Technology detail layout and metric cards.
- Timeline filtering semantics.
- Matching projects still comes from the existing Technology view payload.
- Shared cross-view Explore scope.
- `AnalysisFilters` is not yet rendered in Technologies.

## Regression coverage

`ExploreCurrentBehaviourFeature.test.tsx` now verifies:

1. The default Java selection is reflected in URL state.
2. Selecting Swift updates the URL.
3. Project detail/back preserves Swift.
4. Initial URL selection is respected.
5. A simulated browser navigation (`popstate`) updates the selected technology.
6. Other AnalysisScope parameters survive technology navigation.
