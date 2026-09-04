# Kodförbättraren – status

- **R-001–R-004 är klara.** R-004 betraktas som CI-verifierat efter användarens bekräftelse att senaste routing-/acceptansfixen är grön.
- Aktuellt steg är **R-005A – Generera frontend-lockfil i nätverksansluten CI**.
- Den lokala körmiljön kan inte nå `registry.npmjs.org`; därför skapas ingen fabricerad eller lånad lockfil.
- Frontendjobbet använder redan `npm@11.6.0`. Efter den befintliga installationen laddar det nu upp npm:s egen genererade `frontend/package-lock.json` som artefakten `developer-analytics-frontend-package-lock` med en dags retention.
- R-005A ändrar ingen produktionskod och byter ännu inte till `npm ci`.
- **Nästa rekommenderade åtgärd:** kör CI för denna version. När lockartefakten finns kan R-005 slutföras genom att lägga in den exakta filen och byta frontend-CI samt Dockerfile till `npm ci`.
- R-006 får inte påbörjas innan R-005 är grönt.
