# Kodförbättraren – status

- **R-001–R-014 är klara och verifierade.**
- PR #66 är mergad till `main` i `61c927951f8ec04542f2c06fb09c08cea1d3dff7`.
- **F-001–F-006 är lösta.**
- Post-R-014 rebaseline identifierade ingen ny bred refaktorering som motiveras.
- **F-007 är öppen:** R-009 tappade ordet `failed` i `useOverviewDashboard` HTTP-feltext.
- **R-015 är implementerad på PR #67 och väntar på CI-verifiering.**
- Caller-specifika `errorMessage` är ändrade från `<url>` till `<url> failed`, vilket åter ger `<url> failed with HTTP <status>` via gemensam `getJson`.
- Ett fokuserat API-error regressionstest har lagts till för `/api/me/activity` med HTTP 503.
- Ingen global ändring av `getJson`, overview-successflödet eller andra hooks ingår.
- **Nästa steg är att verifiera R-015 grönt.** Därefter bör denna refaktoreringsomgång avslutas om ingen ny konkret risk framkommer.
