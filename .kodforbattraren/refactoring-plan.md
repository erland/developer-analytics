# Refaktoreringsplan – Developer Analytics

R-001–R-014 är klara och verifierade.

## Rebaseline efter R-014

Rebaselinen är gjord mot aktuell `main` efter merge av PR #66.

Ingen ny bred arkitektur- eller maintainability-refaktorering rekommenderas. Däremot identifierades ett konkret beteendefel i `useOverviewDashboard` som introducerades av R-009.

Före R-009 hade hookens lokala helper följande felkontrakt:

`<url> failed with HTTP <status>`

Efter migrationen till gemensam `getJson` skickas bara URL:en som `errorMessage`, vilket ger:

`<url> with HTTP <status>`

Detta är en beteenderegression och klassificeras som defect-fix, inte refaktorering.

## R-015 – Återställ useOverviewDashboard felmeddelandekontrakt

**Finding:** F-007  
**Klassificering:** defect-fix  
**Risk:** low

### Scope

- Justera endast caller-specifik `errorMessage` i `useOverviewDashboard` så att tidigare feltext återställs.
- Lägg till ett fokuserat regressionstest som fångar HTTP-feltexten.

### Out of scope

- Ingen global ändring av `getJson`.
- Ingen ändring av overview-successflödet.
- Ingen ytterligare fetch-migrering.
- Inga andra hooks.

### Done when

- HTTP-fel åter ger `<url> failed with HTTP <status>`.
- Regressionstestet passerar.
- Success-, abort- och state-semantik är oförändrade.

## Nästa steg

**R-015 – återställ `useOverviewDashboard` felmeddelandekontrakt och skydda det med ett regressionstest.**
