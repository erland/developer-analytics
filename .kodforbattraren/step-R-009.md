# R-009 – Använd gemensam getJson i useOverviewDashboard

## Status

**Implementerad; CI-verifiering väntar.**

## Genomfört

- Tog bort den lokala `getJson`-implementationen i `frontend/src/hooks/useOverviewDashboard.ts`.
- Importerar nu befintliga `getJson` från `frontend/src/api/request.ts`.
- Samma fem GET-endpoints används fortfarande.
- `AbortSignal` skickas fortsatt från samma `AbortController`.
- Credentials, JSON `Accept`, HTTP-statuskontroll och JSON-deserialisering hanteras nu av den redan testade gemensamma helpern.
- Befintlig feltext bevaras genom att respektive URL skickas som `errorMessage`, vilket ger samma `<url> failed with HTTP <status>` som tidigare.
- Dashboard-aggregation, response-typer och state-semantik är oförändrade.

## Out of scope

- `useActivityView`, `useContributions` och övriga hooks.
- Mutationer, auth/sync-specialfall och generell API-client-abstraktion.
- API-kontrakts- eller dashboard-beteendeförändringar.

## Verifiering

GitHub Actions ska verifiera frontend unit tests, lint, typecheck och build innan R-009 markeras klar.

Nästa implementationssteg är R-010 först efter grön verifiering av denna PR-head.
