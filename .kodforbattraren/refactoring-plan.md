# Refaktoreringsplan – Developer Analytics

1. **R-001 – Skydda activity-use-caset med characterization tests** *(klar)*
2. **R-002 – Extrahera activity application service** *(klar)*
3. **R-003 – Ersätt implementationstätt activity-querytest** *(klar)*
4. **R-004 – Etablera use-case-gräns för externa analys-API:t** *(klar och CI-verifierad)*
5. **R-005A – Generera frontend-lockfil i nätverksansluten CI** *(klar och CI-verifierad)*
6. **R-005 – Lås frontendens dependency-resolution** *(implementerad, CI-verifiering väntar)*
7. **R-006 – Extrahera minimal gemensam frontend request-mekanik**

Planen är riskstyrd. R-005 delades när den lokala körmiljön saknade npm-registryåtkomst. R-005A lät projektets egen GitHub Actions-miljö och exakt `npm@11.6.0` generera `package-lock.json`. Artefakten från den gröna CI-körningen matchar projektets `package.json` och har nu lagts in i R-005. CI:s båda frontendinstallationer och frontendens Dockerbygge använder `npm ci`. R-006 får inte påbörjas förrän denna nya `npm ci`-version har verifierats grön i CI.
