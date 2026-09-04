# Refaktoreringsplan – Developer Analytics

1. **R-001 – Skydda activity-use-caset med characterization tests** *(klar)*
2. **R-002 – Extrahera activity application service** *(implementation klar, verifiering blockerad)*
3. **R-003 – Ersätt implementationstätt activity-querytest**
4. **R-004 – Etablera use-case-gräns för externa analys-API:t**
5. **R-005 – Lås frontendens dependency-resolution**
6. **R-006 – Extrahera minimal gemensam frontend request-mekanik**

Planen är riskstyrd. Activity-spåret färdigställs och verifieras före den privacy-känsligare externa analysrefaktoreringen. Tooling/frontendarbete hålls separat från backendrefaktoreringen för små verifierbara diffar.
