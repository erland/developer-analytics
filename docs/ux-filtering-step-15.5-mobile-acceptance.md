# UX filtering step 15.5 – mobile acceptance coverage

## Goal

Lock the redesigned Explore experience at phone width using the Playwright suite that already runs in the GitHub Actions **Mobile acceptance test** job.

The acceptance criteria are deliberately user-facing:

1. Active analysis filters remain visible as compact chips.
2. Filter controls are collapsed behind **Edit filters** on phone width and can be opened/closed without losing the current scope.
3. In Technologies, **Over time** appears before **Projects matching this selection**, and matching projects appear before secondary evidence details.
4. In Activity, the timeline appears before the secondary statistics summary.
5. Activity drill-down works year → month → week and exposes the matching project for the selected week.
6. Back-navigation within the timeline works month → year and year → all years.
7. The tested Explore screens do not introduce horizontal page overflow.

## Changes

`frontend/e2e/mobile/primary-flows.spec.ts` has been updated to the current Explore UI and API contracts.

The fixtures now include:

- project inventory facets,
- complete activity metric fields,
- project lifecycle/monthly/weekly activity,
- multi-technology/project-type project metadata,
- current Technology timeline metric fields.

The existing primary mobile flow was updated to stop asserting removed UI patterns such as the old Activity `.bar-chart` and Technology list button.

A dedicated Explore mobile acceptance scenario now verifies the compact filter panel, content priority, matching-project visibility and time drill-down/back flow.

## CI verification

The repository already runs:

```bash
npm run test:mobile
```

through `.github/workflows/ci.yml` using the iPhone 13 Playwright device profile and Chromium.

The local work environment did not complete `npm install` within its execution window, so the full Playwright run could not be completed locally. No partial `node_modules` or generated lock file is included in the delivered project.
