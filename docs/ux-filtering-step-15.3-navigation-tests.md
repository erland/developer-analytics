# UX filtering step 15.3 – Explore navigation tests

## Goal

Lock the cross-view navigation contract introduced in steps 10.1–10.2:

> A selection made in one Explore view remains the active AnalysisScope when the user changes perspective.

The primary scenario is:

```text
Technologies
  → select Java + 2026
  → Activity
  → Projects
```

Java and 2026 must remain active throughout the flow.

## Added regression coverage

`frontend/src/test-layers/feature/ExploreScopeNavigationFeature.test.tsx` verifies that:

1. a Technology selection can update the URL-backed AnalysisScope;
2. the Explore filter badge updates from the shared scope event;
3. moving to Activity preserves the exact query string;
4. moving to Projects preserves the exact query string;
5. revisiting Technologies does not discard inherited Technology, Project type, or time filters.

The test intentionally exercises the authenticated application shell rather than only testing the URL helper in isolation. Component-level selection behaviour remains covered by the step 15.2 tests.

## Behavioural contract

Explore navigation must not silently clear AnalysisScope. Navigation changes the **perspective**, not the **selection**.

View-specific presentation state may remain local, but the following analysis dimensions continue to travel through the URL:

- technologies;
- project types;
- ownership;
- visibility;
- search;
- time selection.

## Runtime impact

No production behaviour is changed in this step. It adds regression coverage for behaviour already implemented in steps 10.1–10.2 and helps protect the final refactoring/cleanup phase.
