# Kodförbättraren – riskbaserad rebaseline 2026-09-06

## Baseline

Analysen utgår från aktuell `main` efter merge av PR #60 (`980a2e50cb776df88769e9dfa455d5e2aace8430`). R-001–R-008 betraktas som genomförda och verifierade. Ingen produktionskod ändras i detta rebaseline-steg.

Analysen är riskbaserad och selektiv. Den återbesöker tidigare findings och centrala hotspots som påverkats av refaktoreringsserien; den är inte en full rad-för-rad-granskning av hela kodbasen.

## Bekräftat nuläge

- F-001, F-002, F-004 och F-005 är lösta genom R-001–R-008.
- External analysis-ytan ligger bakom application-service-gränsen och R-008 har korrigerat profile `contributionCount` så att aggregate privacy-scope följs.
- Ingen ny källdrift finns efter merge av R-008; aktuell `main` är merge-committen från PR #60.
- Den gemensamma frontend-helpern `frontend/src/api/request.ts` finns kvar med ett medvetet litet `getJson<T>`-API.

## Kvarvarande finding

### F-003 – Frontend request-mekanik är fortfarande delvis utspridd

**Kategori:** maintainability / developer experience  
**Severity:** medium  
**Change risk:** low–medium  
**Expected benefit:** medium  
**Effort:** incremental  
**Leverage:** medium  
**Confidence:** high  
**Klassificering:** beteendebevarande refaktorering

R-006 etablerade den gemensamma request-helpern och migrerade endast två callers för att undvika massmigrering. Kvarvarande duplicering är nu konkret:

- `useOverviewDashboard.ts` definierar en egen lokal `getJson` som duplicerar credentials, JSON Accept-header, AbortSignal, statuskontroll och JSON-deserialisering.
- `useActivityView.ts` bygger samma request-mekanik inline innan sin domänspecifika normalisering.
- `useContributions.ts` duplicerar samma credentials/header/status/JSON-mekanik i promise-form.

Konsekvensen är inte ett aktuellt funktionsfel, men gemensamma request-regler behöver fortfarande ändras på flera ställen och felhanteringen riskerar att drifta mellan callers.

### Rekommendation

Fortsätt inkrementellt. Ingen bred sök-och-ersätt eller generisk API-klient rekommenderas.

1. R-009: ersätt den lokala `getJson`-kopian i `useOverviewDashboard` med den befintliga gemensamma helpern.
2. R-010: migrera endast request-delen i `useActivityView`; behåll all activity-specifik normalisering lokalt.
3. R-011: migrera endast request-delen i `useContributions`; behåll response-defaulting och state-semantik lokalt.

Varje steg ska bevara caller-specifik feltext och befintligt observerbart beteende.

## Medvetet inte prioritera nu

- Ingen ytterligare uppdelning av `ExternalAnalysisApplicationService` enbart på grund av storlek.
- Ingen generell repository-/JPQL-abstraktion utan konkret förändringsnytta.
- Ingen massmigrering av alla `fetch(...)`-anrop; specialfall för mutationer, sync, auth och andra avvikande kontrakt lämnas orörda tills konkret behov finns.
- Ingen CSS-omstrukturering, ramverksmigration eller mikroserviceuppdelning.

## Diagnostisk baseline efter R-008

| Dimension | Nivå | Motivering |
|---|---|---|
| Maintainability | adequate | Centrala backend-gränser är förbättrade; kvarvarande tydlig skuld är lokal frontend request-duplicering. |
| Architecture | strong | De tidigare högst prioriterade lageröverträdelserna i activity/external analysis är åtgärdade. |
| Testability | strong | Characterization- och privacy-testskydd finns kring de tidigare riskområdena och CI har verifierat R-008. |
| Developer experience | adequate | Lockfil/npm ci är etablerat; request-mekanik är ännu inte konsekvent använd över relevanta GET-callers. |
| Usability | unknown | UX är inte scope för denna rebaseline. |
| Accessibility | unknown | Tillgänglighet har inte analyserats i detta steg. |

## Nästa steg

**R-009 – Använd gemensam `getJson` i `useOverviewDashboard`.**

Steget är avsiktligt litet: ta bort den lokala duplicerade helpern, importera den befintliga gemensamma helpern och bevara endpoint- och felbeteende. Verifiera med relevanta frontendtester samt lint/typecheck/build i CI.
