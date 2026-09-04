# Kodförbättraren – status

- Fas: **R-004 routing + dashboard media-type testkorrigering implementerad, full CI-verifiering väntar**.
- Senast korrigerade steg: **R-004 – Etablera use-case-gräns för externa analys-API:t**.
- GitHub Actions-run `33847694102` visade att externa `/api/me/activity` gav 406 på grund av JAX-RS path-precedens, inte privacy-logik.
- UI-resurserna för `activity`, `contributions`, `technologies` och `project-types` har nu gemensam klassväg `/api/me` och metodspecifika delvägar.
- Alla befintliga URL:er är oförändrade; skillnaden mellan dashboard-JSON och External Analysis-medietyp kan nu göras via content negotiation.
- Ny `ExternalAnalysisApplicationService` äger fortsatt persistence/aggregation för `projects`, `activity` och `contributions`.
- Privacy-scope förs explicit över use-case-gränsen; policysemantiken är oförändrad.
- GitHub Actions-run `33848355442` bekräftade att External Analysis-testerna nu passerar. De tre dashboard-activity-testerna föll däremot med 401 eftersom RestAssured skickade `Accept: */*` och routevalet då kunde välja External Analysis-varianten.
- `MeActivityResourceCharacterizationTest` skickar nu explicit `Accept: application/json`, samma kontrakt som frontendens faktiska fetch-anrop använder.
- Lokal testlagerkontroll passerar: authorization 7, privacy 19, persistence 10, unit 66.
- Full Maven/Quarkus-svit behöver bekräftas av GitHub Actions.
- **R-005 är fortfarande blockerad** på att skapa en verklig `package-lock.json` i en npm-registryansluten miljö.
- Nästa rekommenderade steg efter grön backend-CI är fortfarande att slutföra **R-005 – Lås frontendens dependency-resolution**.
