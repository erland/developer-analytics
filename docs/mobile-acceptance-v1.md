# Version 1 Mobile Acceptance Test

Step 89 adds a real-browser phone-width acceptance gate for Developer Analytics
v1.

The test uses Playwright with the iPhone 13 device profile and exercises the
built React application with deterministic API fixtures.

Run locally:

```bash
cd frontend
npm install
npx playwright install chromium
npm run test:mobile
```

## Covered primary flows

The browser acceptance test verifies:

1. **Login** — the GitHub sign-in action is visible and reachable at phone width.
2. **Overview** — the authenticated dashboard renders with mobile navigation.
3. **Activity** — chart content remains visible without horizontal overflow.
4. **Projects** — inventory cards, pagination-oriented UI and filters remain
   usable; the flow does not depend on a wide table.
5. **Technologies** — technology list/detail content collapses to a phone-friendly
   layout and does not require a table.
6. **Filters** — project search/filter controls remain visible and usable.
7. **AI insights** — generated insight content and AI privacy controls remain
   available.
8. **Reports** — report type, privacy settings and privacy preview can be
   completed on the phone viewport.
9. **Privacy/data sources** — private repository selection and synchronisation
   recovery controls remain reachable.
10. **Account** — external GPT/API token controls and account-data controls remain
    usable.

For each major authenticated view the test also checks that the document does
not overflow horizontally beyond the phone viewport.

## Why a browser test

The responsive Vitest layer from Step 76 verifies component behavior in jsdom.
That is useful but cannot prove viewport layout.

Step 89 therefore uses Chromium through Playwright so CSS media queries,
scroll-width and the actual mobile navigation are evaluated by a browser engine.

## Deterministic data

API requests are intercepted inside Playwright and supplied with representative
fixtures. This keeps mobile acceptance focused on layout and user flows instead
of depending on:

- a live GitHub account,
- GitHub rate limits,
- Gemini availability,
- a running backend/database.

Backend/data privacy and large-account behaviors have separate acceptance gates.

## CI

GitHub Actions now contains **Mobile acceptance test**.

It installs Chromium and runs:

```bash
npm run test:mobile
```

A v1 change fails this gate if a primary mobile flow becomes unavailable,
depends on a wide table, or introduces page-level horizontal overflow.
