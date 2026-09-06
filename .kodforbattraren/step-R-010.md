# R-010 – Migrera request-delen i useActivityView till gemensam getJson

## Status

**Implementerad; CI-verifiering väntar.**

## Genomfört

- `frontend/src/hooks/useActivityView.ts` importerar nu `getJson` från `frontend/src/api/request.ts`.
- Den lokala direkta `fetch`-hanteringen, HTTP-statuskontrollen och JSON-deserialiseringen är borttagna.
- Samma activity-URL och befintlig querybyggnad används fortsatt.
- Samma `AbortController`/`AbortSignal` används.
- Feltexten bevaras genom `errorMessage: 'Activity request failed'`.
- Period-/scope-logik, normalisering av response-data och state-semantik är oförändrade.

## Out of scope

- `useContributions` och övriga hooks.
- Activity-normalisering, period-/scope-logik och state-semantik.
- Ny generell API-client-abstraktion.

## Verifiering

GitHub Actions ska verifiera relevanta frontend unit tests, lint, typecheck och build innan R-010 markeras klar.

Nästa implementationssteg är R-011 först efter grön verifiering av denna PR-head.
