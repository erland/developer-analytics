# Kodförbättraren – initial analys

## Scope och begränsningar

Analysen är ett statiskt, riskbaserat första pass av hela ZIP-projektet. Projektkod eller projektets egna script har inte körts i analyssteget. Fokus har varit projektstruktur, arkitekturdokumentation, centrala backend-gränser, frontendens datahämtning, teststruktur och CI. Detta är inte en rad-för-rad-granskning av samtliga 375 käll-/frontendfiler.

## Projekt- och stackinventering

- Fullstack modulär monolit.
- Backend: Java 21, Quarkus 3.39.1, Jakarta Persistence/Hibernate ORM, PostgreSQL, Flyway.
- Frontend: React 19, TypeScript 6, Vite 8, Vitest och Playwright.
- CI: lint, typecheck, frontendtest, Maven verify, containerbyggen, Trivy, runtime-smoke, backup/restore och E2E-flöden.
- Testmängd i källträdet: cirka 81 Java-testfiler och 23 frontendtestfiler.
- Databasmigrationer: 32 Flyway-filer.

## Diagnostisk baseline

| Dimension | Nivå | Evidens | Confidence |
|---|---|---|---|
| Maintainability | adequate | Tydlig paketstruktur och många fokuserade services/repositories, men några REST-resurser samlar persistence, regler, aggregation och DTO-mappning. | high |
| Architecture | adequate | Arkitekturspecifikationen kräver REST/API → application services → domain → repositories/adapters. Flera resurser använder ändå EntityManager/repositories direkt. | high |
| Testability | adequate | Omfattande testlager och CI finns, men `MeActivityResource` skyddas delvis med ett test som läser Java-källtext och letar efter query-strängar i stället för beteende. | high |
| Developer experience | adequate | CI är omfattande, men frontend saknar lockfil och CI använder `npm install`, vilket gör transitiva dependency-resolutioner mindre reproducerbara. | high |
| Usability | unknown | UX är inte primärt scope i denna första kodkvalitetsanalys. | high |
| Accessibility | unknown | Tillgänglighetsarbete har inte granskats tillräckligt djupt i detta pass. | high |

## Prioriterade findings

### F-001 – Aktivitets-API:t korsar flera lager
**Kategori:** architecture / testability  
**Severity:** high  
**Change risk:** medium  
**Expected benefit:** high  
**Effort:** medium  
**Leverage:** high  
**Confidence:** high  
**Klassificering:** beteendebevarande refaktorering

`backend/src/main/java/io/github/developeranalytics/api/MeActivityResource.java` innehåller autentiserings-/HTTP-hantering, analysperiod, urval av matchande projekt, direkt JPQL via `EntityManager`, repository-anrop, tidsaggregation, label-rankning och responsmodellering i samma resurs. Detta avviker från projektets egen lagerprincip och gör den centrala aktivitetsberäkningen svår att testa isolerat.

Testskyddet visar samma friktion: `MeActivityResourceQueryTest` läser produktionsfilens källtext och verifierar att vissa query-fragment finns. Det skyddar implementationens form snarare än observerbart beteende och blir lätt ett hinder vid säker refaktorering.

**Rekommendation:** etablera först characterization tests för de viktigaste aktivitetsfallen och extrahera därefter beräknings-/queryansvaret till ett application-service-lager. API-kontrakt och observerbart beteende ska behållas.

### F-002 – Externa analys-API:t blandar privacy-beslut och persistence i REST-lagret
**Kategori:** architecture / privacy-sensitive maintainability  
**Severity:** high  
**Change risk:** high  
**Expected benefit:** high  
**Effort:** large  
**Leverage:** high  
**Confidence:** high  
**Klassificering:** beteendebevarande refaktorering

`backend/src/main/java/io/github/developeranalytics/api/external/ExternalAnalysisResource.java` har flera endpoints och kombinerar scope-auktorisation, privacy-filtrering, repository-anrop, direkta JPQL-frågor, aggregation och API-DTO:er. Filen är inte problematisk för att den är stor, utan för att privacy-regler och dataåtkomst är sammanflätade med transportlagret.

**Rekommendation:** gör detta efter F-001 så att samma application-service-mönster kan återanvändas. Dela upp per use case och lägg till fokuserade privacy characterization tests innan flytt.

### F-003 – Frontendens API-anrop och felhantering är utspridda
**Kategori:** maintainability / developer experience  
**Severity:** medium  
**Change risk:** medium  
**Expected benefit:** medium  
**Effort:** medium  
**Leverage:** medium  
**Confidence:** medium  
**Klassificering:** beteendebevarande refaktorering

`fetch(...)` förekommer i minst 23 frontendfiler, både hooks och komponenter. Credentials, Accept-headers, statuskontroll, abort-hantering och felmeddelanden byggs lokalt i många use cases. Detta ökar risken för inkonsekvent felhantering och gör kontraktsförändringar dyrare.

**Rekommendation:** inför inte ett stort generiskt API-ramverk. Börja senare med en liten gemensam request-helper för verkligt duplicerade mekanismer och migrera ett fåtal hooks åt gången.

### F-004 – Frontendbygget saknar dependency-lockfil
**Kategori:** reproducibility / developer experience  
**Severity:** medium  
**Change risk:** low  
**Expected benefit:** medium  
**Effort:** small  
**Leverage:** medium  
**Confidence:** high  
**Klassificering:** build/tooling-förändring

`frontend/package.json` finns men ingen `package-lock.json`, och CI kör `npm install`. Exakta direkta versionsnummer minskar risken men transitiva beroenden kan ändå ändras mellan körningar.

**Rekommendation:** skapa och committa lockfil samt byt CI till `npm ci`, med oförändrad dependency-uppsättning. Detta görs separat från kodrefaktorering.

## Beroenden

- F-001 behöver testskydd före extraction.
- F-002 bör följa efter F-001 så att ett beprövat service-/boundary-mönster används i den privacy-känsliga ytan.
- F-003 är oberoende och bör göras inkrementellt efter de högre riskerna.
- F-004 är oberoende och kan genomföras som ett litet tooling-steg.

## Medvetet inte åtgärda nu

- `GitHubProviderAdapter.java` är stor men ansvaren är i huvudsak sammanhållna kring GitHub-providergränsen. Ingen split rekommenderas enbart på grund av storlek.
- `frontend/src/styles.css` är stor, men denna analys har inte visat en tillräckligt konkret underhålls- eller regressionskostnad för att motivera en bred CSS-omstrukturering.
- Ingen ramverksmigration, mikroserviceuppdelning eller generell rewrite rekommenderas. Den dokumenterade modulära monoliten passar projektets nuvarande form.

## Rekommenderad riktning

Börja med att säkra aktivitetsberäkningen med beteendetester, extrahera sedan activity-use-caset ur REST-resursen. Använd erfarenheten därifrån för den mer riskfyllda externa analys-/privacy-gränsen. Håll frontend- och dependency-arbetet separerat så att varje diff kan verifieras oberoende.
