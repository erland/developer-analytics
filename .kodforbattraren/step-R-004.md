# R-004 – Etablera use-case-gräns för externa analys-API:t

## Klassificering
Beteendebevarande backendrefaktorering med privacy-känsligt regressionsskydd.

## Genomfört
- Infört `ExternalAnalysisApplicationService` som explicit use-case-gräns för tre centrala externa analysflöden:
  - `projects`,
  - `activity`,
  - `contributions`.
- Flyttat repository-scoping, persistence-frågor, aggregationslogik och relevanta korrigeringskontroller för dessa use cases ur `ExternalAnalysisResource`.
- REST-resursen ansvarar efter migreringen för autentisering/scope-kontroll, transportparametrar och mappning till det befintliga externa API-kontraktet.
- Privacy-scope skickas explicit till application-servicen. Ingen privacy-policy eller media-type har ändrats.
- Lagt till `ExternalAnalysisPrivacyCharacterizationTest` som låser följande beteenden end-to-end:
  - `PUBLIC_ONLY` döljer privata projektdetaljer och privata aggregat,
  - `PUBLIC_PLUS_PRIVATE_AGGREGATES` inkluderar privata aggregat men döljer privata projektdetaljer,
  - `FULL_AUTHORISED_ANALYSIS` tillåter privata projektdetaljer.
- Utökat `TestFixtureService` med fokuserade fixtures för externa klienttoken och repository-visibility.

## Medveten avgränsning
`profile`, `technologies`, `project-types` och `evidence` ligger kvar i resursen i detta steg. R-004 etablerar gränsen och migrerar de use cases som bäst karakteriserar de två privacy-nivåerna utan att göra en stor totalomskrivning.

## Verifiering
- `python3 scripts/check-backend-test-layers.py`: passerar.
  - authorization: 7
  - privacy: 19
  - persistence: 10
  - unit: 66
- Strukturell kontroll: de migrerade endpointsen använder `ExternalAnalysisApplicationService` och har inte längre egna persistence-frågor/aggregationer.
- Java-källfilerna har balanserade parenteser/klamrar i leveranskontrollen.
- Full `mvn verify` kan inte köras i aktuell lokalmiljö eftersom Maven/Docker saknas och behöver därför bekräftas av GitHub Actions.

## Status
R-004 är **implementerad men väntar på full CI-verifiering**. Nästa planerade steg efter grön CI är R-005.
