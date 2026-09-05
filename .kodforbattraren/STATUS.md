# Kodförbättraren – status

- **R-001–R-008 är klara och verifierade.**
- R-007A–R-007D mergades kumulativt via PR #58 till `main` i merge-commit `1f0b2d365994662ed0e302893a987ec34596f0e8`.
- External analysis-ytan följer nu mönstret **auth → application service → API DTO**. `ExternalAnalysisResource` har inga direkta persistence-beroenden (`EntityManager` eller repositories).
- AI-profile-exkludering (`PROJECT_EXCLUDED_FROM_AI_PROFILE`) är den enda kvarvarande manuella analysis-correction-typen. Obsolete category-rejection och technology-suppression har tagits bort även från AI-insight- och rapportflöden.
- **F-005 är löst genom R-008.** `/api/me/profile` använder nu samma aggregate privacy-scope för `contributionCount` som för repository-räkning och privacy provenance.
- `ExternalAnalysisPrivacyCharacterizationTest` täcker `/api/me/profile` för både `PUBLIC_ONLY` och `PUBLIC_PLUS_PRIVATE_AGGREGATES` med en publik och en privat repository/contribution.
- För `PUBLIC_ONLY` räknas endast publika contributions. Scopes som tillåter privata aggregat räknar fortsatt både publika och privata contributions.
- R-008 verifierades grönt i GitHub Actions: CI run #244 och Dependency Review #164 passerade på PR #60.
- Ingen bredare omstrukturering av `ExternalAnalysisApplicationService` gjordes i R-008.
- Nästa steg är en ny riskbaserad prioritering av kvarvarande hotspots efter att PR #60 mergats till `main`.
