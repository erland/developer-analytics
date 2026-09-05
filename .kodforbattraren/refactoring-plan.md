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
9. **R-008 – Säkra och korrigera profile contribution-count privacy semantics** *(klar och verifierad via PR #60)*

## R-008 – resultat

**Finding:** F-005 *(löst)*  
**Klassificering:** defect/privacy-correctness fix  
**Risk:** high  
**Förväntad nytta:** high  
**Effort:** small–medium

`ExternalAnalysisApplicationService.profile(...)` använder nu samma aggregate privacy-scope för `contributionCount` som för repository-räkningarna och profilens privacy provenance. `PUBLIC_ONLY` räknar endast contributions från publika repositories, medan scopes som tillåter privata aggregat fortsatt inkluderar både publika och privata contributions.

`ExternalAnalysisPrivacyCharacterizationTest` verifierar profile-beteendet med både ett publikt och ett privat repository/contribution. CI run #244 och Dependency Review #164 passerade på PR #60.

### Out of scope som bevarades

- Ingen förändring av external API-kontraktet.
- Ingen generell omskrivning av `ExternalAnalysisApplicationService`.
- Ingen ändring av projekt-detail-semantik eller AI-profile-exkludering.

Planen är fortsatt riskstyrd. Efter merge av PR #60 görs en ny bedömning av aktuell `main` innan nästa förbättring väljs.
