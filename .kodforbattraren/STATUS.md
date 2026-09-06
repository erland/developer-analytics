# Kodförbättraren – status

- **R-001–R-011 är klara och verifierade.**
- PR #63 är mergad till `main` i `645ca57218fcdeb163857830160df25b1674eaae`.
- F-001–F-005 är lösta.
- Post-R-011 rebaseline är genomförd mot aktuell `main` och identifierade inget nytt hög-riskproblem i backend.
- **F-006 är öppen:** projektorienterade frontend-hooks duplicerar fortfarande gemensam autentiserad GET-mekanik.
- **R-012 är implementerad på PR #64 och väntar på CI-verifiering.**
- `useProjectInventory.ts` använder nu gemensam `frontend/src/api/request.ts` för GET-anropet.
- URL/query, paging, AbortSignal, feltext och state-semantik är avsedda att vara beteendemässigt oförändrade.
- `useMatchingProjects`, `useProjectDetail`, `useDataFreshness` och andra hooks är orörda i R-012.
- **Nästa implementationssteg är R-013 först efter att R-012 verifierats grönt.**
