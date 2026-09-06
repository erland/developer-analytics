# R-015 – Återställ useOverviewDashboard felmeddelandekontrakt

## Status

**Klar och verifierad.**

## Klassificering

Defect-fix. R-009 var avsett som beteendebevarande refaktorering men ändrade oavsiktligt HTTP-feltexten.

## Genomfört

- Ändrade endast caller-specifik `errorMessage` i `frontend/src/hooks/useOverviewDashboard.ts` från `<url>` till `<url> failed`.
- Befintlig gemensam `getJson` lämnades oförändrad.
- Tidigare kontrakt `<url> failed with HTTP <status>` är återställt.
- Lade till `frontend/src/test-layers/api-error/OverviewDashboardApiError.test.tsx` som verifierar exakt feltext för `/api/me/activity` vid HTTP 503.

## Out of scope

- Global ändring av `getJson`.
- Overview-successflöde.
- Abort- eller state-semantik.
- Andra hooks eller ytterligare fetch-migrering.

## Verifiering

- GitHub Actions CI #290: success.
- GitHub Actions Dependency Review #201: success.

F-007 är löst. Post-R-014-rebaselinen hittade inget ytterligare konkret arbete som motiverar ännu ett steg, så refaktoreringsomgången avslutas efter merge av PR #67.
