# R-012 – Migrera useProjectInventory till gemensam getJson

## Status

**Implementerad; CI-verifiering väntar.**

## Genomfört

- Ersatte direkt `fetch` i `frontend/src/hooks/useProjectInventory.ts` med befintlig `getJson` från `frontend/src/api/request.ts`.
- Samma `/api/me/project-inventory?...` URL/query används.
- Samma `AbortController`/`AbortSignal` används.
- Caller-specifik feltext bevaras med `errorMessage: 'Project inventory request failed'`.
- Paging, `InventoryResponse` och loading/ready/error-state är oförändrade.

## Out of scope

- `useMatchingProjects` och `useProjectDetail`.
- `useDataFreshness` och andra specialiserade callers.
- Querybyggande/paging, response-kontrakt och generell API-client-abstraktion.

## Verifiering

GitHub Actions ska verifiera relevanta frontend unit tests, lint, typecheck och build innan R-012 markeras klar.

Efter grön R-012 är nästa planerade steg R-013.
