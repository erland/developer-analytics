# Kodförbättraren – status

- **R-001–R-008 är klara och verifierade.**
- PR #60 är mergad till `main` i `980a2e50cb776df88769e9dfa455d5e2aace8430`.
- Risk-rebaseline 2026-09-06 är genomförd mot denna baseline utan produktionskodändringar.
- Ingen ny hög-risk backendfinding identifierades i den fokuserade omanalysen.
- F-001, F-002, F-004 och F-005 är lösta. **F-003 kvarstår delvis löst.**
- Gemensam frontend-helper `frontend/src/api/request.ts` finns, men konkret duplicerad GET-requestmekanik finns fortfarande i några callers.
- `useOverviewDashboard.ts` har en lokal duplicerad `getJson`; `useActivityView.ts` och `useContributions.ts` har motsvarande inline-mekanik.
- Ingen massmigrering rekommenderas. Planen fortsätter med små, separata caller-steg.
- **Nästa steg: R-009 – använd gemensam `getJson` i `useOverviewDashboard`.**
- Därefter är R-010 (`useActivityView`) och R-011 (`useContributions`) planerade, följt av ny bedömning innan fler callers berörs.
