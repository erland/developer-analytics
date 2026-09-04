# R-002 – Extrahera activity application service

## Klassificering
Beteendebevarande refaktorering.

## Genomfört
- Skapat `io.github.developeranalytics.service.activity.ActivityApplicationService`.
- Flyttat project inventory-scoping, JPQL-frågor, veckostatistik, aggregation, label-ranking och lifecycle-sammanställning från `MeActivityResource` till application service.
- Låtit `MeActivityResource` behålla autentisering, `AnalysisPeriod`-normalisering och transport-DTO-mappning.
- Behållit befintliga API-records i `MeActivityResource` för att minimera kontrakts- och testpåverkan.
- Anpassat det tillfälliga källkodstestet så att det följer den flyttade implementationen; det ersätts beteendemässigt i R-003.

## Verifiering
- `python3 scripts/check-backend-test-layers.py`: PASS.
- Persistence layer: 10 tester. Unit layer: 67 tester.
- Statisk lagerkontroll: `MeActivityResource` har inga direkta persistence-importer eller `@Transactional`.
- Full `mvn --batch-mode --no-transfer-progress verify`: EJ KÖRD, eftersom denna miljö saknar Maven och Docker.

## Status
Implementation klar men steget är **blocked** tills full backendverifiering passerar.
