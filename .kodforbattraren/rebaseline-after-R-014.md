# Risk-rebaseline efter R-014

## Bas

Aktuell `main` efter merge av PR #66. R-001–R-014 är slutförda och verifierade.

## Resultat

Ingen ny bred arkitektur-, testbarhets- eller maintainability-finding motiverar ytterligare generell refaktorering.

Ett konkret beteendefel identifierades däremot i `frontend/src/hooks/useOverviewDashboard.ts`.

### F-007 – felmeddelandekontrakt ändrades av R-009

Före R-009 använde hooken en lokal `getJson` som vid HTTP-fel kastade:

`<url> failed with HTTP <status>`

Nuvarande implementation använder den gemensamma `frontend/src/api/request.ts` och skickar URL:en som `errorMessage`. Den gemensamma helpern lägger till `with HTTP <status>`, vilket ger:

`<url> with HTTP <status>`

Ordet `failed` försvann alltså i R-009 trots att steget var klassificerat som beteendebevarande refaktorering.

## Bedömning

- Typ: behavior regression / defect.
- Risk: låg.
- Effekt: användar-/diagnostiktext skiljer sig från tidigare kontrakt och från vad R-009 avsåg att bevara.
- Rotorsak: caller-specifik `errorMessage` valdes som enbart URL i stället för `<url> failed`.
- Rekommenderad åtgärd: korrigera endast `useOverviewDashboard` och lägg till ett fokuserat regressionstest.
- Ska inte göras: global ändring av `getJson`, generell API-client eller ytterligare massmigrering.

## Rekommenderat nästa steg

R-015 – återställ `useOverviewDashboard` felmeddelandekontrakt och skydda det med ett regressionstest.
