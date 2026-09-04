# R-005 – Lås frontendens dependency-resolution

## Status

**Klar och CI-verifierad.**

## Genomfört

- `frontend/package.json` pinnar package manager till `npm@11.6.0`.
- Developer Analytics egen CI-genererade npm v3-lockfil är incheckad som `frontend/package-lock.json`.
- Frontend validation och mobile acceptance använder `npm ci --no-audit --no-fund`.
- `frontend/Dockerfile` kopierar `package.json` och `package-lock.json` innan `npm ci`.
- Det tillfälliga artefaktsteget från R-005A har tagits bort.

## Verifiering

PR #50 head `88e5237378944dc72b0c90f46e5f7d3807d69e96` verifierades grönt:
- CI run `33883581268`: success.
- Dependency Review run `33883581278`: success.

R-005 är därmed avslutat och F-004 är löst.
