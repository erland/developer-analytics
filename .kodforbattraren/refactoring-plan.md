# Refaktoreringsplan – Developer Analytics

R-001–R-011 är klara och verifierade.

## Rebaseline efter R-011

Rebaselinen är gjord mot `main@645ca57218fcdeb163857830160df25b1674eaae` efter merge av PR #63. Ingen ny hög-riskfinding identifierades i backend. Ett nytt fokuserat maintainability-fynd, **F-006**, identifierades i frontend: flera projektorienterade hooks använder fortfarande lokal `fetch` + credentials + Accept-header + statuskontroll + JSON-deserialisering trots att samma mekanik redan finns i `frontend/src/api/request.ts`.

Detta gäller tydligt `useProjectInventory`, `useMatchingProjects` och request-delen i `useProjectDetail`. `useDataFreshness` och andra callers med mer specialiserad fel-/state-semantik lämnas uttryckligen utanför.

## R-012 – implementerat

**Finding:** F-006  
**Klassificering:** beteendebevarande refaktorering  
**Risk:** low

- `useProjectInventory.ts` använder nu befintlig `getJson` från `frontend/src/api/request.ts`.
- Direkt `fetch`, lokal HTTP-statuskontroll och lokal JSON-deserialisering är borttagna från hooken.
- URL/querybyggande, paging och samma `AbortSignal` är oförändrade.
- Feltexten `Project inventory request failed with HTTP <status>` bevaras via caller-specifik `errorMessage`.
- State-semantik och `InventoryResponse`-kontrakt är oförändrade.
- Övriga hooks är utanför scopet.

### Verifiering

R-012 markeras inte klar förrän relevanta frontend unit tests, lint, typecheck och build passerar i GitHub Actions på den uppdaterade PR-headen.

## Efterföljande steg

14. **R-013 – Migrera `useMatchingProjects` till gemensam `getJson`** *(planerad efter grön R-012)*
15. **R-014 – Migrera request-delen i `useProjectDetail` till gemensam `getJson`** *(planerad efter R-013)*

Ingen massmigrering eller generell API-client-abstraktion ska följa automatiskt.
