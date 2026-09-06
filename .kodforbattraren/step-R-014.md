# R-014 – Migrera request-delen i useProjectDetail till gemensam getJson

## Status

**Implementerad; CI-verifiering väntar.**

## Genomfört

- Ersatte direkt `fetch` i `frontend/src/hooks/useProjectDetail.ts` med befintlig `getJson` från `frontend/src/api/request.ts`.
- Samma `/api/me/projects/${repositoryId}` URL används.
- Samma `AbortController`/`AbortSignal` används.
- Caller-specifik feltext bevaras med `errorMessage: 'Project detail request failed'`.
- Timeline-normalisering är oförändrad.
- Contributors-defaulting är oförändrad.
- Idle/loading/ready/error-state och abort-hantering är oförändrade.

## Out of scope

- Timeline-normalisering.
- Contributors-defaulting.
- State-semantik.
- `useDataFreshness` och andra specialiserade callers.
- Generell API-client-abstraktion eller bred fetch-migrering.

## Verifiering

GitHub Actions ska verifiera relevanta frontend unit tests, lint, typecheck och build innan R-014 markeras klar.

Efter grön verifiering görs en ny riskbaserad bedömning innan ytterligare refaktorering väljs.
