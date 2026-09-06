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
12. **R-011 – Migrera request-delen i `useContributions` till gemensam `getJson`** *(implementerad, CI-verifiering väntar)*

## R-010 – resultat

R-010 verifierades grönt i GitHub Actions CI #255 och Dependency Review #173 innan PR #62 mergades till `main` i `f996c5533d5bf7a603ae72c80dffd3caf7e8c302`.

## R-011 – implementerat

**Finding:** F-003 *(partially resolved)*  
**Klassificering:** beteendebevarande refaktorering  
**Risk:** low–medium  
**Förväntad nytta:** medium  
**Effort:** small

### Genomfört

- `useContributions.ts` använder nu befintlig `frontend/src/api/request.ts` för contributions-GET-anropet.
- Direkt `fetch`, lokal HTTP-statuskontroll och lokal JSON-deserialisering är borttagna från hooken.
- Samma URL/query och samma `AbortSignal` används.
- Feltexten `Contributions request failed with HTTP <status>` bevaras genom caller-specifik `errorMessage`.
- Response-defaulting (`?? 0`, `?? []`) och state-semantik är oförändrade och ligger kvar lokalt.

### Out of scope

- Övriga hooks.
- Ingen ändring av response-defaulting eller state-semantik.
- Ingen ny generell API-client-abstraktion.

### Verifiering

R-011 markeras inte klar förrän relevanta frontend unit tests, lint, typecheck och build passerar i GitHub Actions på PR-headen.

## Efterföljande steg

Efter grön verifiering och merge av R-011 görs en ny riskbaserad bedömning innan fler `fetch`-callers övervägs. Ingen automatisk massmigrering ska följa.
