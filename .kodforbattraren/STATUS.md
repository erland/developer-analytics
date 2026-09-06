# Kodförbättraren – status

- **R-001–R-013 är klara och verifierade.**
- PR #65 är mergad till `main` i `a513dfac815331bf002e605a8e8c4d8faeaff022`.
- R-013 verifierades grönt före merge i GitHub Actions CI #272 och Dependency Review #187.
- F-001–F-005 är lösta. **F-006 är fortsatt öppen tills R-014 verifierats.**
- **R-014 är implementerad på separat branch och väntar på CI-verifiering.**
- `useProjectDetail.ts` använder nu gemensam `frontend/src/api/request.ts` för GET-anropet.
- Repository-URL, AbortSignal och feltexten `Project detail request failed with HTTP <status>` är bevarade.
- Timeline-normalisering, contributors-defaulting samt idle/loading/ready/error-state är fortsatt lokala och oförändrade.
- `useDataFreshness` och övriga specialiserade callers är fortsatt utanför scope.
- **Nästa steg är att verifiera R-014 grönt. Därefter görs en ny riskbedömning innan ytterligare arbete väljs.**
