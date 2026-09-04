# UX filtering step 9.2 – separate project scope filters from list options

## Goal

Make the Projects view explicit about the difference between filters that define the current analysis selection and options that only refine the project inventory presentation.

## Changes

The Projects view now has three visually distinct control areas:

1. **Analysis filters** – shared Explore dimensions handled by `AnalysisFilters`, currently Technology, Project type and Ownership.
2. **Project filters** – project-specific dimensions that are still part of `AnalysisScope`, currently Search and Visibility. These are URL-backed and can therefore survive navigation/deep links.
3. **Project list options** – local inventory presentation controls. Activity (`All / Active / Inactive`) remains here because it is a recency/list concept rather than the same thing as the shared analysis period.

This resolves the ambiguity introduced when Search, Visibility and Activity were rendered inside the same panel even though they had different state semantics.

## State rules

- Search → `AnalysisScope.search` and URL.
- Visibility → `AnalysisScope.visibility` and URL.
- Activity → local Projects state only; it is not serialized into `AnalysisScope`.
- Pagination remains local Projects state.

The API query continues to combine both scope and the local Activity option, but the UI now communicates their different meaning.

## Tests

`ProjectInventoryFilteringFeature.test.tsx` now verifies that:

- both **Project filters** and **Project list options** headings are rendered;
- Search and Visibility update `AnalysisScope` and the URL;
- Activity is passed separately to the inventory hook;
- Activity is not added to `AnalysisScope` or the URL.

## Runtime impact

No backend/API behavior changes in this step. The change is a UI/state-semantics clarification on top of step 9.1.
