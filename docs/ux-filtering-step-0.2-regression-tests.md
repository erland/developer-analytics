# UX filtering redesign – step 0.2 regression-test baseline

This step adds regression coverage for the existing Explore behaviour before the shared analysis-scope refactor begins.

## Added coverage

### Technologies

- the first technology is selected by default
- selecting another technology replaces the detail content
- opening a matching project replaces the technology view with project detail
- returning from project detail restores the locally selected technology

### Project types

- the first project type is selected by default
- selecting another project type replaces the detail content
- opening and returning from a project restores the locally selected project type

### Timeline

- the current year → month → week drill-down is covered
- the selected period exposes its project list
- a project can be opened through the supplied callback
- backing out from month/week returns to the previous time level

### Projects

- filters are passed to the inventory query state
- changing a filter resets pagination to page zero
- category and technology options are currently derived from the returned page of projects
- opening and returning from project detail preserves the local project-filter state

## Test files

- `frontend/src/test-layers/feature/ExploreCurrentBehaviourFeature.test.tsx`
- `frontend/src/test-layers/feature/ProjectInventoryFilteringFeature.test.tsx`

## Why some tests deliberately protect behaviour that will later change

The purpose of step 0.2 is to capture the current interaction boundaries before refactoring. For example, the Projects test explicitly records that category/technology filter options are derived from the currently returned inventory items. A later facet-API step is expected to replace that behaviour; at that point this regression test should be intentionally rewritten rather than silently broken.

Likewise, Technologies and Project types currently keep selection in local component state. Later steps will move that selection into `AnalysisScope` and URL state. The tests provide a visible checkpoint for that migration.

## Local verification note

The repository snapshot does not contain installed frontend dependencies or a package-manager lock file. The execution environment used for this step could therefore not run Vitest without fetching the complete npm dependency tree. The test source has been added against the project's existing Vitest/Testing Library conventions; the normal repository CI/npm install should execute it together with the existing feature suite.
