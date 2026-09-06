# Refaktoreringsplan – Developer Analytics

1. **R-001 – Skydda activity-use-caset med characterization tests** *(klar)*
2. **R-002 – Extrahera activity application service** *(klar)*
3. **R-003 – Ersätt implementationstätt activity-querytest** *(klar)*
4. **R-004 – Etablera use-case-gräns för externa analys-API:t** *(klar)*
5. **R-005A – Generera frontend-lockfil i nätverksansluten CI** *(klar)*
6. **R-005 – Lås frontendens dependency-resolution** *(klar)*
7. **R-006 – Extrahera minimal gemensam frontend request-mekanik** *(klar och verifierad)*
8. **R-007 – Slutför external analysis application-service-gränsen** *(klar och verifierad via PR #58)*
9. **R-008 – Säkra och korrigera profile contribution-count privacy semantics** *(klar och verifierad via PR #60)*
10. **R-009 – Använd gemensam `getJson` i `useOverviewDashboard`** *(klar och verifierad via PR #61)*
11. **R-010 – Migrera request-delen i `useActivityView` till gemensam `getJson`** *(klar och verifierad via PR #62)*
12. **R-011 – Migrera request-delen i `useContributions` till gemensam `getJson`** *(klar och verifierad via PR #63)*

## R-011 – resultat

R-011 verifierades grönt i GitHub Actions CI #257 och Dependency Review #174 på PR #63.

### Genomfört

- `useContributions.ts` använder befintlig `frontend/src/api/request.ts` för contributions-GET-anropet.
- Direkt `fetch`, lokal HTTP-statuskontroll och lokal JSON-deserialisering är borttagna från hooken.
- Samma URL/query och samma `AbortSignal` används.
- Feltexten `Contributions request failed with HTTP <status>` är bevarad.
- Response-defaulting (`?? 0`, `?? []`) och state-semantik är oförändrade och ligger kvar lokalt.

### Verifiering

- GitHub Actions CI #257: success.
- GitHub Actions Dependency Review #174: success.

## Nästa steg

Efter merge av PR #63 görs en ny riskbaserad rebaseline mot aktuell `main` innan ytterligare refaktorering väljs. Ingen automatisk massmigrering av fler `fetch`-callers ska följa.
