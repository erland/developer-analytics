# R-011 – Migrera request-delen i useContributions till gemensam getJson

## Status

**Implementerad; CI-verifiering väntar.**

## Genomfört

- Ersatte direkt `fetch` i `frontend/src/hooks/useContributions.ts` med befintlig `getJson` från `frontend/src/api/request.ts`.
- Samma `/api/me/contributions?...` URL används.
- Samma `AbortController`/`AbortSignal` används.
- Caller-specifik feltext bevaras med `errorMessage: 'Contributions request failed'`.
- Response-defaulting för total/commits/pullRequests/reviews/issues/recentProjects är oförändrad.
- Loading/ready/error-state och abort-hantering är oförändrade.

## Out of scope

- Övriga hooks.
- Response-defaulting och state-semantik.
- Generell API-client-abstraktion eller bred fetch-migrering.

## Verifiering

GitHub Actions ska verifiera relevanta frontend unit tests, lint, typecheck och build innan R-011 markeras klar.

Efter grön verifiering och merge görs en ny riskbaserad bedömning innan ytterligare refaktorering väljs.
