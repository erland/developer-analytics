# Refaktoreringsplan – Developer Analytics

1. **R-001 – Skydda activity-use-caset med characterization tests** *(klar)*
2. **R-002 – Extrahera activity application service** *(klar)*
3. **R-003 – Ersätt implementationstätt activity-querytest** *(klar)*
4. **R-004 – Etablera use-case-gräns för externa analys-API:t** *(klar)*
5. **R-005A – Generera frontend-lockfil i nätverksansluten CI** *(klar)*
6. **R-005 – Lås frontendens dependency-resolution** *(klar)*
7. **R-006 – Extrahera minimal gemensam frontend request-mekanik** *(klar och verifierad)*
8. **R-007 – Slutför external analysis application-service-gränsen** *(klar och verifierad via PR #58)*
   - R-007A: ta bort obsolete classification-correction filtering.
   - R-007B: flytta technology/project-type summaries bakom application service.
   - R-007C: flytta evidence-aggregation bakom application service.
   - R-007D: flytta profile-aggregation bakom application service.
9. **R-008 – Säkra och korrigera profile contribution-count privacy semantics** *(nästa)*

## R-008

**Finding:** F-005  
**Klassificering:** defect/privacy-correctness fix  
**Risk:** high  
**Förväntad nytta:** high  
**Effort:** small–medium

`ExternalAnalysisApplicationService.profile(...)` filtrerar repository-räkningarna efter tokenens `PrivacyScope`, men den separata `contributionCount`-queryn räknar contributions från alla repositories med `includedInAnalysis=true`. Det innebär att `PUBLIC_ONLY` kan exponera ett aggregerat värde påverkat av privata repositories samtidigt som profilens privacy provenance är `PUBLIC_ONLY`.

### Genomförandeordning

1. Lägg characterization tests för `/api/me/profile` med ett publikt och ett privat repository, båda med contributions.
2. Verifiera förväntat beteende för `PUBLIC_ONLY`, `PUBLIC_PLUS_PRIVATE_AGGREGATES` och `FULL_AUTHORISED_ANALYSIS`.
3. Justera endast contribution-count-urvalet så det följer samma aggregate privacy-scope som profilens repository-urval.
4. Kör backend verify och privacy/authorization-testlagren.

### Out of scope

- Ingen förändring av external API-kontraktet.
- Ingen generell omskrivning av `ExternalAnalysisApplicationService`.
- Ingen ändring av projekt-detail-semantik eller AI-profile-exkludering.

Planen är fortsatt riskstyrd: efter R-008 görs en ny bedömning innan ytterligare strukturell refaktorering väljs.
