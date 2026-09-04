# Kodförbättraren – status

- **R-001–R-006 är klara och verifierade.**
- R-006 verifierades av användaren efter implementationen av den minimala gemensamma frontend-requestmekaniken.
- Aktuellt steg är **R-007A – Säkra external classification API och ta bort obsolete classification-correction filtering** och är **implementerat men väntar på CI-verifiering**.
- `ExternalAnalysisResource` använder inte längre `PROJECT_CATEGORY_REJECTED` eller `TECHNOLOGY_INFERENCE_SUPPRESSED` för technologies, project types eller evidence.
- `UserAnalysisCorrection.Type` innehåller nu endast `PROJECT_EXCLUDED_FROM_AI_PROFILE`, i linje med V35 som redan raderar de två borttagna correction-typerna ur databasen.
- Ett regressionsskydd verifierar att endast AI-profile-exkludering återstår som manuell analysis correction.
- **Nästa rekommenderade åtgärd:** verifiera R-007A i GitHub Actions. Om den är grön kan nästa del av R-007 flytta kvarvarande query-/aggregationslogik från `ExternalAnalysisResource` till application-service/repository-lagret.
