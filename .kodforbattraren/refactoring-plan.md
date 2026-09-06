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
10. **R-009 – Använd gemensam `getJson` i `useOverviewDashboard`** *(implementerad, CI-verifiering väntar)*
11. **R-010 – Migrera request-delen i `useActivityView` till gemensam `getJson`** *(planerad)*
12. **R-011 – Migrera request-delen i `useContributions` till gemensam `getJson`** *(planerad)*

## Rebaseline 2026-09-06

Efter merge av R-008 gjordes en ny riskbaserad bedömning av aktuell `main` (`980a2e50cb776df88769e9dfa455d5e2aace8430`). Ingen ny hög-risk backendfinding identifierades i den fokuserade omanalysen.

F-003 kvarstår delvis: frontendens gemensamma GET-requestmekanik är etablerad i `frontend/src/api/request.ts`, men används ännu inte konsekvent av konkreta callers. Planen fortsätter därför med små, separata caller-migreringar. Ingen massmigrering, generell API-klient eller beteendeförändring ingår.

## R-009 – implementerat

**Finding:** F-003 *(partially resolved)*  
**Klassificering:** beteendebevarande refaktorering  
**Risk:** low  
**Förväntad nytta:** medium  
**Effort:** small

### Genomfört

- Den lokala `getJson`-implementationen i `useOverviewDashboard.ts` är borttagen.
- Hooken använder nu befintlig `frontend/src/api/request.ts`.
- Samma fem endpoints används.
- Samma `AbortController`/`AbortSignal` används.
- Credentials, Accept-header, HTTP-statuskontroll och JSON-deserialisering delegeras till den gemensamma helpern.
- Caller-specifik feltext bevaras genom att URL:en används som `errorMessage`.
- Dashboard-aggregation och state-semantik är oförändrade.

### Out of scope

- Inga andra hooks i samma steg.
- Ingen förändring av API-kontrakt eller dashboard-aggregation.
- Ingen ny generell API-client-abstraktion.

### Verifiering

R-009 markeras inte klar förrän relevanta frontend unit tests, lint, typecheck och build passerar i GitHub Actions på den uppdaterade PR-headen.

## Efterföljande steg

R-010 och R-011 följer samma princip men hålls separata eftersom respektive hook har egen response-normalisering/state-semantik. R-010 startas först efter grön verifiering av R-009. Efter R-011 görs en ny bedömning innan fler `fetch`-callers övervägs.
