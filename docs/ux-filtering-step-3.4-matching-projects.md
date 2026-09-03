# UX filtering step 3.4 – shared MatchingProjects component

## Purpose

Introduce the reusable project-result component that will become the common endpoint of Explore filtering:

> Filters and time selections above the component determine exactly which projects are listed below it.

This step adds the reusable building block without migrating Technologies, Project types, Activity or Contributions yet.

## Added

### `useMatchingProjects`

A scope-driven hook that translates `AnalysisScope` to the existing `/api/me/project-inventory` query contract and adds pagination.

It therefore supports the backend capabilities introduced in step 3.2, including:

- multiple technologies
- multiple project types
- ownership / visibility
- search
- `from` / `to`
- year / month / week

### `MatchingProjects`

The component provides:

- a consistent `Projects matching this selection` heading
- matching project count
- loading state
- API error state
- explicit empty state
- paginated project list
- project-detail navigation
- restoration of the same result when returning from project detail
- page reset when the supplied analysis scope changes

## Scope ownership

`MatchingProjects` deliberately does not own or modify analysis filters. Its input is an `AnalysisScope` supplied by the parent Explore view.

It owns only result presentation state:

- project-list page
- currently opened project detail

This preserves the separation introduced in steps 1.1 and 1.2.

## Runtime impact

No existing Explore view uses `MatchingProjects` in this step, so current user-visible behaviour is unchanged. Technologies will be the first migration in phase 4.
