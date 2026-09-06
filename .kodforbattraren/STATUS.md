# Kodförbättraren – status

- **R-001–R-012 är klara och verifierade.**
- PR #64 är mergad till `main` i `7bc779f51811aa0a9fabfe586a82f8b32337ffe7`.
- R-012 verifierades grönt före merge i GitHub Actions CI #270 och Dependency Review #186.
- F-001–F-005 är lösta. **F-006 är fortsatt öppen tills R-013 och R-014 är klara.**
- **R-013 är implementerad på separat branch och väntar på CI-verifiering.**
- `useMatchingProjects.ts` använder nu gemensam `frontend/src/api/request.ts` för GET-anropet.
- Scope/paging, URL/query, AbortSignal, feltext, `InventoryResponse` och state-semantik är oförändrade.
- `useProjectDetail`, `useDataFreshness` och andra hooks är orörda i R-013.
- **Nästa implementationssteg är R-014 först efter att R-013 verifierats grönt.**
