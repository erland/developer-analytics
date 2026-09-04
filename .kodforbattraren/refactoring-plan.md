# Refaktoreringsplan – Developer Analytics

1. **R-001 – Skydda activity-use-caset med characterization tests** *(klar)*
2. **R-002 – Extrahera activity application service** *(klar)*
3. **R-003 – Ersätt implementationstätt activity-querytest** *(klar)*
4. **R-004 – Etablera use-case-gräns för externa analys-API:t** *(klar och CI-verifierad)*
5. **R-005A – Generera frontend-lockfil i nätverksansluten CI** *(implementerad, CI-verifiering väntar)*
6. **R-005 – Lås frontendens dependency-resolution** *(väntar på lockartefakten från R-005A)*
7. **R-006 – Extrahera minimal gemensam frontend request-mekanik**

Planen är riskstyrd. R-005 delades när den lokala körmiljön visade sig sakna npm-registryåtkomst. I stället för att fabricera en lockfil låter R-005A projektets egen GitHub Actions-miljö och exakt `npm@11.6.0` generera `package-lock.json` och publicera den som en kortlivad artefakt. R-005 kan därefter lägga in just den filen och byta CI/Docker till `npm ci`.
