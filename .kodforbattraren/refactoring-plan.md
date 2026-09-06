# Refaktoreringsplan – Developer Analytics

R-001–R-014 är klara och verifierade.

## Rebaseline efter R-011

Rebaselinen gjordes mot `main@645ca57218fcdeb163857830160df25b1674eaae`. Ingen ny hög-riskfinding identifierades i backend. F-006 avgränsade kvarvarande duplicerad autentiserad GET-mekanik till `useProjectInventory`, `useMatchingProjects` och request-delen i `useProjectDetail`. Specialiserade callers som `useDataFreshness` är fortsatt utanför scope.

## R-012 – resultat

R-012 verifierades grönt i GitHub Actions CI #270 och Dependency Review #186 innan PR #64 mergades.

## R-013 – resultat

R-013 verifierades grönt i GitHub Actions CI #272 och Dependency Review #187 innan PR #65 mergades till `main` i `a513dfac815331bf002e605a8e8c4d8faeaff022`.

## R-014 – resultat

R-014 verifierades grönt i GitHub Actions CI #274 och Dependency Review #188 på PR #66.

- `useProjectDetail.ts` använder befintlig `getJson` från `frontend/src/api/request.ts` endast för GET-requestmekaniken.
- Samma `/api/me/projects/${repositoryId}` URL och samma `AbortSignal` används.
- Feltexten `Project detail request failed with HTTP <status>` är bevarad.
- Timeline-normalisering är oförändrad och ligger kvar lokalt.
- Contributors-defaulting är oförändrad och ligger kvar lokalt.
- Idle/loading/ready/error-state och abort-hantering är oförändrade.

F-006 är därmed löst.

## Nästa steg

Efter merge av PR #66 görs en ny riskbaserad rebaseline mot aktuell `main` innan ytterligare refaktorering väljs. `useDataFreshness` och andra specialiserade callers migreras inte automatiskt.
