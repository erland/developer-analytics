# Refaktoreringsplan – Developer Analytics

R-001–R-014 är klara och verifierade.

## Rebaseline efter R-014

Rebaselinen mot aktuell `main` efter PR #66 hittade ingen ny bred arkitektur- eller maintainability-refaktorering som motiveras. Däremot bekräftades **F-007**, en beteenderegression från R-009 i `useOverviewDashboard`.

Före R-009 gav HTTP-fel `<url> failed with HTTP <status>`. Efter R-009 gav samma fel `<url> with HTTP <status>` eftersom caller-specifik `errorMessage` endast bestod av URL:en.

## R-015 – implementerat

**Finding:** F-007  
**Klassificering:** defect-fix  
**Risk:** low

- `useOverviewDashboard` skickar nu `<url> failed` som caller-specifik `errorMessage` till befintlig `getJson`.
- Därmed återställs tidigare kontrakt `<url> failed with HTTP <status>` utan att ändra den gemensamma helpern.
- `frontend/src/test-layers/api-error/OverviewDashboardApiError.test.tsx` verifierar att ett 503-fel från `/api/me/activity` ger exakt `/api/me/activity failed with HTTP 503`.
- Success-flöde, abort-hantering, state-semantik och övriga hooks är utanför scope och oförändrade.

### Verifiering

R-015 markeras klar först när regressionstest, frontend unit tests, lint, typecheck och build passerar på PR #67:s uppdaterade head.

## Nästa steg

**Verifiera R-015.** Om verifieringen är grön och ingen ny konkret risk framkommer bör refaktoreringsomgången avslutas i stället för att fortsätta med generell städning.
