# Kodförbättraren – status

- Fas: **R-004 implementerad, full CI-verifiering väntar**.
- Senast implementerade steg: **R-004 – Etablera use-case-gräns för externa analys-API:t**.
- Ny `ExternalAnalysisApplicationService` äger persistence/aggregation för `projects`, `activity` och `contributions`.
- `ExternalAnalysisResource` behåller autentisering/scope-kontroll och transportmappning för de migrerade use casen.
- Privacy-scope förs explicit över use-case-gränsen; policysemantiken är oförändrad.
- Nytt end-to-end characterization-skydd täcker PUBLIC_ONLY, PUBLIC_PLUS_PRIVATE_AGGREGATES och FULL_AUTHORISED_ANALYSIS.
- Lokal testlagerkontroll passerar: authorization 7, privacy 19, persistence 10, unit 66.
- Full Maven/Quarkus-svit kräver GitHub Actions eftersom lokal miljö saknar Maven/Docker.
- Nästa rekommenderade steg efter grön CI: **R-005 – Lås frontendens dependency-resolution**.
