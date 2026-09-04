# UX filtering step 10.3 – project detail scope navigation

Project detail navigation now preserves the current Explore analysis selection in browser history.

## Behaviour

Opening a project adds a separate `project` query parameter while leaving all `AnalysisScope` parameters intact, for example:

```text
?technology=java&projectType=backend&year=2025
```

becomes:

```text
?technology=java&projectType=backend&year=2025&project=repo-123
```

Browser Back removes the project detail entry and restores the exact scoped analysis URL. The visible Back action uses the same history entry when the detail was opened from the application. Direct project deep links can be closed by removing only `project`, leaving the analysis selection untouched.

## Shared implementation

`useProjectDetailNavigation` is used by:

- Activity through `AuthenticatedShell`
- Projects
- `MatchingProjects` (Technologies, Project types and Contributions)

This removes separate local project-detail navigation semantics while keeping project detail separate from `AnalysisScope` itself.
