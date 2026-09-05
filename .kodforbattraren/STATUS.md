# Kodförbättraren – status

- **R-001–R-007 är klara och verifierade.**
- R-007A–R-007D mergades kumulativt via PR #58 till `main` i merge-commit `1f0b2d365994662ed0e302893a987ec34596f0e8`.
- External analysis-ytan följer nu mönstret **auth → application service → API DTO**. `ExternalAnalysisResource` har inga direkta persistence-beroenden (`EntityManager` eller repositories).
- AI-profile-exkludering (`PROJECT_EXCLUDED_FROM_AI_PROFILE`) är den enda kvarvarande manuella analysis-correction-typen. Obsolete category-rejection och technology-suppression har tagits bort även från AI-insight- och rapportflöden.
- En ny riskbaserad rebaseline av aktuell `main` har genomförts efter R-007.
- Högst prioriterad kvarvarande risk var att `/api/me/profile` beräknade `repositoryCount` och `privacyProvenance` enligt tokenens privacy-scope, medan `contributionCount` räknade contributions från alla repositories som var inkluderade i analysen.
- **R-008 – Säkra och korrigera profile contribution-count privacy semantics** är implementerat och väntar på CI-verifiering.
- `ExternalAnalysisPrivacyCharacterizationTest` täcker nu `/api/me/profile` för både `PUBLIC_ONLY` och `PUBLIC_PLUS_PRIVATE_AGGREGATES` med en publik och en privat repository/contribution.
- `ExternalAnalysisApplicationService.profile` använder nu samma aggregate privacy-scope för `contributionCount` som resten av profilen: `PUBLIC_ONLY` räknar endast publika repositories, medan scopes som tillåter privata aggregat fortsatt räknar både publika och privata contributions.
- Ingen bredare omstrukturering av `ExternalAnalysisApplicationService` gjordes i R-008.
- Nästa beslut tas efter grön CI: markera F-005/R-008 som verifierade och gör därefter nästa riskbaserade prioritering.
