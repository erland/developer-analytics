# Kodförbättraren – status

- **R-001–R-005 är klara.**
- R-005 verifierades grönt på PR #50 head `88e5237378944dc72b0c90f46e5f7d3807d69e96`: CI run `33883581268` och Dependency Review run `33883581278` passerade.
- Aktuellt steg är **R-006 – Extrahera minimal gemensam frontend request-mekanik** och är **implementerat men väntar på CI-verifiering**.
- Ny `frontend/src/api/request.ts` innehåller en liten `getJson<T>`-helper för det gemensamma dashboard-kontraktet: `credentials: include`, `Accept: application/json`, optional `AbortSignal`, HTTP-statuskontroll och JSON-deserialisering.
- `useProjectTypes` och `useTechnologyViews` använder nu helpern; inga andra hooks har massmigrerats.
- `frontend/src/api/request.test.ts` verifierar request-kontraktet och caller-specifika HTTP-fel.
- Frontendens testlagerkontroll passerar oförändrat.
- Full lokal `npm ci` kunde inte slutföras eftersom körmiljön inte får färdig registryåtkomst; därför är lint/typecheck/test/build ännu inte grönt lokalt.
- **Nästa rekommenderade åtgärd:** kör GitHub Actions på denna version. Om den är grön kan R-006 markeras klar.
