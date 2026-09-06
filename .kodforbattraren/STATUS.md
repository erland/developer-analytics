# Kodförbättraren – status

- **R-001–R-008 är klara och verifierade.**
- PR #60 är mergad till `main` i `980a2e50cb776df88769e9dfa455d5e2aace8430`.
- Risk-rebaseline 2026-09-06 identifierade ingen ny hög-risk backendfinding.
- F-001, F-002, F-004 och F-005 är lösta. **F-003 kvarstår delvis löst.**
- **R-009 är implementerad på PR #61 men väntar på CI-verifiering.**
- `useOverviewDashboard.ts` använder nu gemensam `frontend/src/api/request.ts` i stället för en lokal duplicerad `getJson`.
- Endpoints, abort-semantik, credentials, Accept-header, HTTP-statuskontroll, JSON-deserialisering och feltext är avsedda att vara beteendemässigt oförändrade.
- Ingen annan hook eller generell API-client-abstraktion ingår i R-009.
- **Nästa implementationssteg är R-010 först efter att R-009 verifierats grönt.**
- R-010 gäller endast request-delen i `useActivityView`; R-011 gäller därefter `useContributions`.
