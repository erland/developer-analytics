# Kodförbättraren – status

- **R-001–R-006 är klara och verifierade.**
- **R-007A – Säkra external classification API och ta bort obsolete classification-correction filtering** är implementerat på PR #55.
- Under CI-verifieringen av R-007A hittades tre kvarvarande beroenden till de borttagna correction-typerna: testlager-taggen, `UserCorrectionService` och `UserAiInsightService`. Samtliga är nu rättade; AI-profile-exkludering är den enda kvarvarande manuella correction-typen.
- **R-007B – Flytta external technology/project-type summaries till application-service-gränsen** är implementerat på PR #56.
- `/api/me/technologies`, `/api/me/project-types` och profilens motsvarande topplistor går nu genom `ExternalAnalysisApplicationService`.
- Aktuellt steg är **R-007C – Flytta external evidence-aggregation till application-service-gränsen** och är **implementerat men väntar på CI-verifiering**.
- `/api/me/evidence` innehåller inte längre JPQL eller privacy-filtrering i REST-resursen. Technology- och project-type-evidence hämtas och filtreras i `ExternalAnalysisApplicationService`, medan `ExternalAnalysisResource` endast autentiserar och mappar service-resultat till API-kontraktet.
- Profilens repository- och contribution-aggregation ligger fortfarande kvar i REST-resursen och är nästa tydliga kandidat inom R-007 efter grön CI.
