# Kodförbättraren – status

- **R-001–R-006 är klara och verifierade.**
- **R-007A – Säkra external classification API och ta bort obsolete classification-correction filtering** är implementerat på PR #55.
- Under CI-verifieringen av R-007A hittades kvarvarande beroenden till de borttagna correction-typerna i testkod, `ExternalAnalysisApplicationService`, `UserCorrectionService` och `UserAiInsightService`. Samtliga är nu rättade; AI-profile-exkludering är den enda kvarvarande manuella correction-typen.
- **R-007B – Flytta external technology/project-type summaries till application-service-gränsen** är implementerat på PR #56.
- **R-007C – Flytta external evidence-aggregation till application-service-gränsen** är implementerat på PR #57.
- Vid CI-verifieringen av R-007C hittades ytterligare ett äldre regressionstest som fortfarande refererade till de två borttagna correction-typerna. Testet är nu korrigerat i den stackade kedjan.
- Aktuellt steg är **R-007D – Flytta external profile-aggregation till application-service-gränsen** och är implementerat men väntar på CI-verifiering.
- `/api/me/profile` hämtar nu hela profilsammanställningen via `ExternalAnalysisApplicationService`; repository-urval, repository-räkning, contribution count, privacy provenance samt technology/project-type-topplistor ligger bakom application-service-gränsen.
- `ExternalAnalysisResource` har nu inga direkta persistence-beroenden (`EntityManager` eller repositories) och dess externa analysendpoints följer i huvudsak mönstret auth → application service → API DTO.
- Nästa rekommenderade åtgärd efter grön CI är att göra en ny riskbaserad analys av kvarvarande hotspots i stället för att fortsätta dela `ExternalAnalysisResource` av strukturella skäl.
