# R-005 – Lås frontendens dependency-resolution

## Status

**Blockerad efter säker förberedelse.**

## Genomfört

- `frontend/package.json` pinnar nu package manager till `npm@11.6.0`, samma npm-version som CI använder.
- Befintlig `npm install` i CI och Dockerfile har avsiktligt **inte** bytts till `npm ci` ännu, eftersom `npm ci` kräver en giltig `package-lock.json`.

## Blockerare

Den aktuella körmiljön kan inte DNS-resolvera/nå `registry.npmjs.org`. `npm install --package-lock-only` kan därför inte skapa en korrekt lockfil. Att fabricera eller kombinera en lockfil utan npm:s resolver skulle kunna ge en inkonsistent eller trasig dependency-graf och görs inte.

## För att slutföra R-005

1. Kör med npm 11.6.0 i en miljö med npm-registryåtkomst:
   `cd frontend && npm install --package-lock-only --ignore-scripts --no-audit --no-fund`
2. Verifiera:
   `npm ci`
3. Byt `.github/workflows/ci.yml` och `frontend/Dockerfile` från `npm install --no-audit --no-fund` till `npm ci --no-audit --no-fund`.
4. Kör `npm run lint && npm run typecheck && npm test && npm run build`.

R-006 ska inte påbörjas innan detta är grönt.
