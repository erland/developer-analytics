# Kodförbättraren – status

- Fas: **R-002 implementation klar, verifiering blockerad**.
- Senast avslutade steg: **R-001 – Skydda activity-use-caset med characterization tests**.
- Pågående steg: **R-002 – Extrahera activity application service**.
- Förändring: `MeActivityResource` är nu en tunn HTTP/auth/input-output-adapter. Query-, project-scoping-, aggregation- och label-logik ligger i nya `ActivityApplicationService`. API-recordsen är kvar i resursen och mappas från service-resultatet för att bevara kontraktet.
- Testanpassning: det befintliga implementationstäta `MeActivityResourceQueryTest` pekar tillfälligt på både resurs och service så att dess gamla skydd inte tappas före R-003.
- Verifiering: `python3 scripts/check-backend-test-layers.py` passerar (10 persistence, 67 unit). Statisk kontroll visar att resursen inte längre importerar `EntityManager`, `ProjectInventoryRepository`, `RepositoryUserActivityWeekRepository` eller `@Transactional`.
- Blockerare: **B-002 – full `mvn verify` kan inte köras i denna miljö eftersom Maven och Docker saknas**.
- Nästa rekommenderade steg: **R-002 kvarstår tills full backend-CI är grön**. Därefter blir R-003 körbart.
