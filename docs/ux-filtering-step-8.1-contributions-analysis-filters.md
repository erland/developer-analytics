# UX filtering step 8.1 – Contributions uses AnalysisScope

## Goal

Bring Contributions into the shared Explore filtering model before adding the common MatchingProjects result list in step 8.2.

## Changes

- Contributions now reads its selection from `AnalysisScope` in the URL.
- Browser back/forward restores the contribution filters.
- The shared `AnalysisFilters` component exposes:
  - Technology
  - Project type
  - Ownership
  - rolling Period presets
- Technology and project-type choices reuse the existing evidence/classification inventories.
- `useContributions(scope)` serialises the same AnalysisScope contract used by Projects.
- `/api/me/contributions` now accepts the project-scope parameters already supported by Project Inventory:
  - `technology` (multi-value)
  - `projectType` / legacy `category` (multi-value)
  - `ownership`
  - `visibility`
  - `search`
  - `from` / `to` / `year` / `month` / `week`

## Contribution-period semantics

A period selection constrains both sides of the result:

1. the repositories must match the AnalysisScope and have activity in the selected period; and
2. contribution counts/recent-project contribution totals only include contribution events whose `occurredAt` falls inside that period.

This avoids the misleading case where selecting 2025 would select projects active in 2025 but still show their all-time contribution totals.

## Deliberately deferred

Step 8.1 does **not** yet add the common `MatchingProjects` component to Contributions. The existing "Recently active projects" result remains as the contribution-specific project summary for one more step.

Step 8.2 will add the canonical matching-project list so the same project set is visible below the contribution analysis just like Technologies and Project types.
