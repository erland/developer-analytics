# R-006 – Extrahera minimal gemensam frontend request-mekanik

## Status

**Implementerad; CI-verifiering väntar.**

## Genomfört

- Ny `frontend/src/api/request.ts` med ett medvetet litet `getJson<T>`-API.
- Helpern centraliserar endast verkligt gemensam mekanik: `credentials: include`, JSON `Accept`, optional `AbortSignal`, HTTP-statuskontroll och JSON-deserialisering.
- Caller-specifik feltext skickas in så befintligt användarbeteende behålls.
- Endast två nästan identiska hooks migreras i detta steg:
  - `useProjectTypes`
  - `useTechnologyViews`
- Activity, corrections, sync och övriga specialfall lämnas utanför för att undvika en massmigrering.
- Nytt `frontend/src/api/request.test.ts` verifierar request-kontraktet och HTTP-fel.

## Verifiering

Lokalt:
- `python3 scripts/check-frontend-test-layers.py` passerar.
- `frontend/package.json` och `frontend/package-lock.json` är giltig JSON.
- Full `npm ci` kunde inte slutföras på grund av registryåtkomst i körmiljön, så `lint`, `typecheck`, `test` och `build` kan inte markeras gröna lokalt.

R-006 markeras därför inte klar förrän GitHub Actions har verifierat ändringen.
