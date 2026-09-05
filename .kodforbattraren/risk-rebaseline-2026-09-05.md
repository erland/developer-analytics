# Kodförbättraren – riskbaserad rebaseline 2026-09-05

## Baseline

Analysen utgår från aktuell `main` efter merge av PR #58 (`1f0b2d365994662ed0e302893a987ec34596f0e8`). R-001–R-007 betraktas som genomförda. Fokus är kvarvarande risk och förändringsnytta, inte filstorlek eller mekanisk uppdelning.

## Bekräftat resultat från R-007

- `ExternalAnalysisResource` har endast auth- och application-service-beroenden och mappar application-resultat till API-DTO:er.
- Profile, projects, activity, technologies, project-types, contributions och evidence ligger bakom `ExternalAnalysisApplicationService`.
- `PROJECT_EXCLUDED_FROM_AI_PROFILE` är den enda kvarvarande manuella `UserAnalysisCorrection.Type`.
- Obsolete category-rejection och technology-suppression är borttagna även från AI-insight- och canonical-report-flöden.

## Prioriterade kvarvarande findings

### F-005 – Profile contributionCount kan bryta privacy-scope

**Kategori:** privacy correctness / defect risk  
**Severity:** high  
**Change risk:** medium  
**Expected benefit:** high  
**Effort:** small–medium  
**Leverage:** high  
**Confidence:** high  
**Klassificering:** functional defect fix

`ExternalAnalysisApplicationService.profile(...)` bygger först en repository-lista som filtreras enligt `PrivacyScope`. Repository count och privacy provenance beräknas från detta filtrerade urval. `contributionCount` hämtas däremot i en separat query som endast filtrerar på user och `includedInAnalysis=true`, utan visibility/privacy-scope.

Konsekvensen är att ett `PUBLIC_ONLY`-token kan få ett `contributionCount` som påverkas av privata repositories samtidigt som svaret anger `privacyProvenance=PUBLIC_ONLY`. Det skapar en semantisk privacy-inkonsistens och kan läcka information genom ett aggregat.

**Rekommendation:** R-008. Lägg först characterization tests för profile med ett publikt och ett privat repository, båda med contributions. Korrigera därefter contribution-queryn minimalt så den följer samma aggregate privacy-scope som resten av profilen.

### F-003 – Frontend request-mekanik är fortfarande delvis utspridd

**Kategori:** maintainability / developer experience  
**Severity:** medium  
**Change risk:** medium  
**Expected benefit:** medium  
**Effort:** incremental  
**Leverage:** medium  
**Confidence:** medium

R-006 etablerade den avsedda lilla `getJson`-gränsen och migrerade ett begränsat urval hooks. Findingen är därför reducerad men inte nödvändigtvis helt eliminerad. Ingen massmigrering rekommenderas utan ny evidens från konkreta callers.

**Rekommendation:** avvakta tills en faktisk frontendändring berör duplicerad request-mekanik och migrera då lokalt. Prioritera inte före F-005.

## Medvetet inte prioritera nu

- Dela inte `ExternalAnalysisApplicationService` enbart för att den blivit större; R-007 löste transport/persistence-sammanblandningen som var den faktiska risken.
- Ingen generell repository-abstraktion över all JPQL utan konkret test- eller förändringsnytta.
- Ingen bred frontend-API-migrering bara för enhetlighet.
- Ingen ramverksmigration, mikroserviceuppdelning eller generell rewrite.

## Nästa steg

**R-008 – Säkra och korrigera profile contribution-count privacy semantics.**

Done-kriterier:

1. Profile privacy characterization tests täcker `PUBLIC_ONLY`, `PUBLIC_PLUS_PRIVATE_AGGREGATES` och `FULL_AUTHORISED_ANALYSIS`.
2. `PUBLIC_ONLY` contribution count påverkas inte av privata repositories.
3. Aggregate/full scopes behåller tillåten privat aggregation.
4. API-kontraktet ändras inte.
5. Backend verify och privacy/authorization-testlager passerar.
