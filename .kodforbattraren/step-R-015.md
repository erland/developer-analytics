# R-015 – Återställ useOverviewDashboard felmeddelandekontrakt

## Status

**Implementerad; CI-verifiering väntar.**

## Klassificering

Defect-fix. R-009 var avsett som beteendebevarande refaktorering men ändrade oavsiktligt HTTP-feltexten.

## Genomfört

- Ändrade endast caller-specifik `errorMessage` i `frontend/src/hooks/useOverviewDashboard.ts` från `<url>` till `<url> failed`.
- Befintlig gemensam `getJson` lämnas oförändrad.
- Tidigare kontrakt `<url> failed with HTTP <status>` återställs.
- Lade till `frontend/src/test-layers/api-error/OverviewDashboardApiError.test.tsx` som verifierar exakt feltext för `/api/me/activity` vid HTTP 503.

## Out of scope

- Global ändring av `getJson`.
- Overview-successflöde.
- Abort- eller state-semantik.
- Andra hooks eller ytterligare fetch-migrering.

## Verifiering

GitHub Actions ska verifiera regressionstest, frontend unit tests, lint, typecheck och build innan R-015 markeras klar.

Efter grön verifiering bör refaktoreringsomgången avslutas om ingen ny konkret risk framkommer.
