# Kodförbättraren – status

- **R-001–R-007 är klara och verifierade.**
- R-007A–R-007D mergades kumulativt via PR #58 till `main` i merge-commit `1f0b2d365994662ed0e302893a987ec34596f0e8`.
- External analysis-ytan följer nu mönstret **auth → application service → API DTO**. `ExternalAnalysisResource` har inga direkta persistence-beroenden (`EntityManager` eller repositories).
- AI-profile-exkludering (`PROJECT_EXCLUDED_FROM_AI_PROFILE`) är den enda kvarvarande manuella analysis-correction-typen. Obsolete category-rejection och technology-suppression har tagits bort även från AI-insight- och rapportflöden.
- En ny riskbaserad rebaseline av aktuell `main` har genomförts efter R-007.
- Högst prioriterad kvarvarande risk är att `/api/me/profile` beräknar `repositoryCount` och `privacyProvenance` enligt tokenens privacy-scope, men `contributionCount` fortfarande räknar contributions från alla repositories som är inkluderade i analysen. För `PUBLIC_ONLY` kan detta göra att privata contributions påverkar profilen trots `privacyProvenance=PUBLIC_ONLY`.
- Nästa rekommenderade steg är **R-008 – Säkra och korrigera profile contribution-count privacy semantics**.
- R-008 ska först lägga characterization tests för `PROFILE_READ` med public/private repositories och därefter göra minsta möjliga korrigering så contribution count använder samma aggregate-scope som resten av profilen.
- Ingen bredare omstrukturering av `ExternalAnalysisApplicationService` rekommenderas i R-008.
