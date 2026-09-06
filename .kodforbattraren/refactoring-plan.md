# Refaktoreringsplan – Developer Analytics

R-001–R-012 är klara och verifierade.

## Rebaseline efter R-011

Rebaselinen gjordes mot `main@645ca57218fcdeb163857830160df25b1674eaae`. Ingen ny hög-riskfinding identifierades i backend. F-006 avgränsar kvarvarande duplicerad autentiserad GET-mekanik till `useProjectInventory`, `useMatchingProjects` och request-delen i `useProjectDetail`. Specialiserade callers som `useDataFreshness` är fortsatt utanför scope.

## R-012 – resultat

R-012 verifierades grönt i GitHub Actions CI #270 och Dependency Review #186 innan PR #64 mergades till `main` i `7bc779f51811aa0a9fabfe586a82f8b32337ffe7`.

## R-013 – implementerat

**Finding:** F-006  
**Klassificering:** beteendebevarande refaktorering  
**Risk:** low

- `useMatchingProjects.ts` använder nu befintlig `getJson` från `frontend/src/api/request.ts`.
- Direkt `fetch`, lokal HTTP-statuskontroll och lokal JSON-deserialisering är borttagna.
- Scope/paging och URL/querybyggande är oförändrade.
- Samma `AbortSignal` används.
- Feltexten `Matching projects request failed with HTTP <status>` bevaras via caller-specifik `errorMessage`.
- `InventoryResponse` och loading/ready/error-state är oförändrade.

### Verifiering

R-013 markeras inte klar förrän relevanta frontend unit tests, lint, typecheck och build passerar i GitHub Actions på den nya PR-headen.

## Nästa steg

**R-014 – migrera endast request-delen i `useProjectDetail` till gemensam `getJson` efter grön R-013.** Timeline-normalisering, contributors-defaulting och state-semantik ska lämnas oförändrade.
