# UX filtering step 15.4 – Browser history through project detail

## Goal

Lock the user flow where an analysis selection survives opening a project and using the browser Back action.

The key contract is:

```text
Technology = Java + Year = 2025
        ↓
Matching projects
        ↓
Open project
        ↓
Browser Back
        ↓
Technology = Java + Year = 2025
```

## Coverage added

`MatchingProjectsFeature.test.tsx` now exercises the complete component flow:

1. Start at `?technology=java&year=2025`.
2. Render `MatchingProjects` with the corresponding `AnalysisScope`.
3. Open a project from the matching result.
4. Verify that only `project=repo-1` is added and the analysis parameters remain intact.
5. Trigger browser Back.
6. Verify that `project` is removed while `technology=java&year=2025` remains.
7. Verify that the matching-project list is rendered again with the same scope and page.

This complements the lower-level `ProjectDetailScopeNavigationFeature` hook tests from step 10.3 by testing the user-visible component path.

## Runtime impact

None. This step only strengthens regression coverage.
