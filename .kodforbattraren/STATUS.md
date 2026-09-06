# Kodförbättraren – status

- **R-001–R-011 är klara och verifierade.**
- PR #63 är mergad till `main` i `645ca57218fcdeb163857830160df25b1674eaae`.
- F-001–F-005 är lösta.
- Post-R-011 rebaseline är genomförd mot aktuell `main`.
- Ingen ny hög-riskfinding identifierades i backend.
- **F-006 är öppnad:** projektorienterade frontend-hooks duplicerar fortfarande gemensam autentiserad GET-mekanik.
- F-006 avgränsas till `useProjectInventory`, `useMatchingProjects` och request-delen i `useProjectDetail`.
- `useDataFreshness` och andra specialiserade callers lämnas utanför tills vidare.
- **Nästa rekommenderade steg är R-012: migrera `useProjectInventory` till befintlig `getJson`.**
- Därefter följer R-013 (`useMatchingProjects`) och R-014 (`useProjectDetail`) som separata steg.
