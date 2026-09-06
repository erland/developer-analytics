# R-013 – Migrera useMatchingProjects till gemensam getJson

## Status

**Implementerad; CI-verifiering väntar.**

## Genomfört

- Ersatte direkt `fetch` i `frontend/src/hooks/useMatchingProjects.ts` med befintlig `getJson` från `frontend/src/api/request.ts`.
- Samma `/api/me/project-inventory?...` URL används.
- Scope- och paging-logik är oförändrade.
- Samma `AbortController`/`AbortSignal` används.
- Caller-specifik feltext bevaras med `errorMessage: 'Matching projects request failed'`.
- `InventoryResponse` och loading/ready/error-state är oförändrade.

## Out of scope

- `useProjectDetail`, `useDataFreshness` och andra hooks.
- Scope/paging-logik.
- Generell API-client-abstraktion.

## Verifiering

GitHub Actions ska verifiera relevanta frontend unit tests, lint, typecheck och build innan R-013 markeras klar.

Efter grön verifiering är nästa steg R-014, begränsat till request-delen i `useProjectDetail`.
