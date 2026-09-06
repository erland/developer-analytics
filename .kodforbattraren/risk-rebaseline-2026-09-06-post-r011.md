# Risk-rebaseline efter R-011

Bas: `main@645ca57218fcdeb163857830160df25b1674eaae`

## Slutsats

R-001–R-011 har reducerat de tidigare viktigaste arkitektur-, privacy-, reproducerbarhets- och frontend-requestproblemen. Den nya bedömningen hittar inget nytt hög-riskproblem som motiverar större backendrefaktorering.

## Nytt konkret fynd: F-006

**Kategori:** maintainability / frontend request mechanics  
**Prioritet:** medium  
**Risk att åtgärda:** low–medium

Tre projektorienterade hooks duplicerar fortfarande samma autentiserade GET-mekanik som redan finns i `frontend/src/api/request.ts`:

- `frontend/src/hooks/useProjectInventory.ts`
- `frontend/src/hooks/useMatchingProjects.ts`
- request-delen i `frontend/src/hooks/useProjectDetail.ts`

Dupliceringen omfattar `fetch`, `credentials: 'include'`, `Accept: application/json`, AbortSignal, HTTP-statuskontroll och JSON-deserialisering. Caller-specifik querybyggnad, feltext, response-normalisering och state-semantik ska fortsatt ligga kvar hos respektive hook.

## Rekommenderad ordning

1. **R-012 useProjectInventory** – renast migration och lägst risk.
2. **R-013 useMatchingProjects** – samma inventory-kontrakt men egen scope/paging-logik.
3. **R-014 useProjectDetail** – requestmekaniken kan delas, men timeline-normalisering och contributors-defaulting ska lämnas orörda.

## Medvetet ej prioriterat

`useDataFreshness` har specialiserad state- och felhantering där själva HTTP-felet medvetet inte exponeras. Den ska därför inte mekaniskt migreras bara för konsekvensens skull.

Ingen generell API-klient, massmigrering, backendomstrukturering eller ramverksförändring rekommenderas i denna rebaseline.
