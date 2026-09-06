# Kodförbättraren – status

- **R-001–R-009 är klara och verifierade.**
- PR #61 är mergad till `main` i `c68c13d8d62768fda967e56d0360a49337ffef64`.
- R-009 verifierades grönt före merge i GitHub Actions CI #253 och Dependency Review #172.
- F-001, F-002, F-004 och F-005 är lösta. **F-003 kvarstår delvis löst.**
- **R-010 är implementerad på en separat branch och väntar på CI-verifiering.**
- `useActivityView.ts` använder nu gemensam `frontend/src/api/request.ts` för själva GET-anropet.
- Period-/scope-logik, querybyggande, activity-normalisering och state-semantik är oförändrade.
- Feltexten `Activity request failed with HTTP <status>` bevaras genom caller-specifik `errorMessage`.
- Ingen annan hook eller generell API-client-abstraktion ingår i R-010.
- **Nästa implementationssteg är R-011 först efter att R-010 verifierats grönt.**
