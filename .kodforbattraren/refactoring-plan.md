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
11. **R-010 – Migrera request-delen i `useActivityView` till gemensam `getJson`** *(implementerad, CI-verifiering väntar)*
12. **R-011 – Migrera request-delen i `useContributions` till gemensam `getJson`** *(planerad)*

## R-009 – resultat

R-009 verifierades grönt i GitHub Actions CI #253 och Dependency Review #172 innan PR #61 mergades till `main` i `c68c13d8d62768fda967e56d0360a49337ffef64`.

## R-010 – implementerat

**Finding:** F-003 *(partially resolved)*  
**Klassificering:** beteendebevarande refaktorering  
**Risk:** low–medium  
**Förväntad nytta:** medium  
**Effort:** small

### Genomfört

- `useActivityView.ts` använder nu befintlig `frontend/src/api/request.ts` för activity-GET-anropet.
- Direkt `fetch`, lokal statuskontroll och lokal JSON-deserialisering är borttagna från hooken.
- Samma URL/querybyggande och `AbortSignal` används.
- Feltexten `Activity request failed with HTTP <status>` är bevarad.
- Activity-specifik normalisering, period-/scope-logik och state-semantik är oförändrade.

### Out of scope

- `useContributions` och övriga hooks.
- Ingen ändring av activity-normalisering eller period-/scope-logik.
- Ingen ny generell API-client-abstraktion.

### Verifiering

R-010 markeras inte klar förrän relevanta frontend unit tests, lint, typecheck och build passerar i GitHub Actions på PR-headen.

## Efterföljande steg

R-011 migrerar endast request-delen i `useContributions` efter att R-010 verifierats grönt. Response-defaulting och state-semantik ska fortsätta vara lokala. Därefter görs en ny riskbedömning innan fler callers övervägs.
