# UX filtering step 6.1 – Project types use AnalysisScope

## Goal

Move the selected project type from component-local state into the shared `AnalysisScope` URL contract, matching the behaviour introduced for Technologies in step 4.1.

## Changes

- `ProjectTypeViews` no longer owns a local `selectedKey`.
- The selected project type is read from repeated `projectType` query parameters.
- If no valid project type is present, the first available project type becomes the default and is written with `history.replaceState`.
- User selection uses `history.pushState`, so browser history can restore earlier project-type selections.
- `popstate` reparses `AnalysisScope` from the URL.
- Existing scope dimensions such as `technology`, `year`, ownership, visibility and search are preserved when project type changes.
- Project detail remains local to the view, but returning from detail keeps the same URL-backed project-type selection.

## Deliberately unchanged

This step does not yet redesign the Project types layout. It still uses the existing project-type list, metrics, timeline and representative-project list. Those changes belong to the following 6.x steps.

## Regression coverage

`ExploreCurrentBehaviourFeature.test.tsx` now verifies:

1. the default project type is reflected in the URL;
2. selecting another project type updates `projectType` in the URL;
3. project detail/back preserves the selection;
4. deep-linking to a project type restores it;
5. simulated browser navigation restores a previous project type without dropping other AnalysisScope parameters.
