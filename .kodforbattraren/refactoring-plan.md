# Refaktoreringsplan – Developer Analytics

R-001–R-011 är klara och verifierade.

## Rebaseline efter R-011

Rebaselinen är gjord mot `main@645ca57218fcdeb163857830160df25b1674eaae` efter merge av PR #63.

Ingen ny hög-riskfinding identifierades i backend. De tidigare arkitektur- och privacyfynden förblir lösta.

Ett nytt fokuserat maintainability-fynd, **F-006**, identifierades i frontend: flera projektorienterade hooks använder fortfarande lokal `fetch` + credentials + Accept-header + statuskontroll + JSON-deserialisering trots att samma mekanik redan finns i `frontend/src/api/request.ts`.

Detta gäller tydligt:

- `useProjectInventory`
- `useMatchingProjects`
- request-delen i `useProjectDetail`

`useDataFreshness` och andra callers med mer specialiserad fel-/state-semantik lämnas uttryckligen utanför. Ingen massmigrering rekommenderas.

## Nya steg

13. **R-012 – Migrera `useProjectInventory` till gemensam `getJson`** *(planerad)*
14. **R-013 – Migrera `useMatchingProjects` till gemensam `getJson`** *(planerad efter R-012)*
15. **R-014 – Migrera request-delen i `useProjectDetail` till gemensam `getJson`** *(planerad efter R-013)*

R-012 prioriteras först eftersom den är en ren, liten och beteendebevarande migration utan response-normalisering. R-014 hålls separat eftersom `useProjectDetail` har egen timeline-normalisering och contributors-defaulting som inte ska flyttas eller ändras.

## Nästa steg

**R-012 – migrera endast GET-requestmekaniken i `useProjectInventory` till den befintliga helpern.**
