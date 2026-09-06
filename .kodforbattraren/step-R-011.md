# R-011 – Migrera request-delen i useContributions till gemensam getJson

## Status

**Klar och verifierad.**

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

- GitHub Actions CI #257: success.
- GitHub Actions Dependency Review #174: success.

## Nästa steg

Efter merge av PR #63 görs en ny riskbaserad rebaseline mot aktuell `main` innan ytterligare refaktorering väljs.
