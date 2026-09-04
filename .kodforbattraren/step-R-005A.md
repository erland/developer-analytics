# R-005A – Generera frontend-lockfil i nätverksansluten CI

## Status

**Implementerad; CI-verifiering väntar.**

## Bakgrund

R-005 kräver en verklig `frontend/package-lock.json`, men den lokala körmiljön kan inte DNS-resolvera `registry.npmjs.org`. En lockfil ska därför inte fabriceras eller byggas från ett annat projekts dependency-graf.

## Genomfört

- R-004 markeras som färdig efter användarens bekräftelse att senaste CI-körningen är grön.
- R-005 har delats explicit och R-005A har lagts in som förberedande verifierbart steg.
- Frontendjobbet använder fortsatt projektets befintliga `npm@11.6.0` och `npm install --no-audit --no-fund`.
- Direkt efter installation laddar jobbet upp den av npm genererade `frontend/package-lock.json` som artefakten `developer-analytics-frontend-package-lock`.
- Artefakten har `retention-days: 1` och `if-no-files-found: error`, så avsaknad av lockfil blir ett tydligt CI-fel.
- Ingen produktionskod, dependency-version eller runtime-installation har ändrats.

## Verifiering

Lokalt:
- workflow-YAML kan parsas,
- frontendens testlagerkontroll kan köras,
- diffen är begränsad till CI + Kodförbättrarens status/plan.

CI:
1. Kör frontend validation.
2. Bekräfta att jobbet fortfarande är grönt.
3. Bekräfta att artefakten `developer-analytics-frontend-package-lock` finns.
4. Nästa steg hämtar artefakten, placerar `package-lock.json` i `frontend/`, byter CI/Docker till `npm ci` och kör full frontendvalidering.
