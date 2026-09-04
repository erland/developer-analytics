# Kodförbättraren – status

- **R-001–R-006 är klara och verifierade.**
- R-006 verifierades av användaren efter implementationen av den minimala gemensamma frontend-requestmekaniken.
- **R-007A – Säkra external classification API och ta bort obsolete classification-correction filtering** är implementerat på PR #55.
- R-007A:s första CI-körning stoppades av att det nya regressionsskyddet saknade backend-testtagg; testet är nu märkt `@Tag("unit")`.
- Vid CI-felsökningen hittades även kvarvarande referenser till de borttagna correction-typerna i `ExternalAnalysisApplicationService`; de är borttagna så classification-modellen nu är konsekvent genom hela external-flödet.
- Aktuellt steg är **R-007B – Flytta external technology/project-type summaries till application-service-gränsen** och är **implementerat men väntar på CI-verifiering**.
- `ExternalAnalysisResource` äger inte längre `UserTechnologyAssessmentRepository` eller `ProjectTypeAnalyticsRepository` och innehåller inte längre den public-only JPQL-frågan för project-type summaries.
- `/api/me/technologies`, `/api/me/project-types` och profilens motsvarande topplistor går nu genom `ExternalAnalysisApplicationService`, medan REST-resursen behåller auth och API-DTO-mappning.
- Evidence-queries och profilens övriga repository/contribution-aggregation ligger medvetet kvar i REST-resursen till senare steg.
- **Nästa rekommenderade åtgärd efter grön CI:** flytta external evidence-aggregation ur REST-resursen som nästa avgränsade R-007-del.
