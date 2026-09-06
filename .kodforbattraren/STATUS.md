# Kodförbättraren – status

- **R-001–R-010 är klara och verifierade.**
- PR #62 är mergad till `main` i `f996c5533d5bf7a603ae72c80dffd3caf7e8c302`.
- R-010 verifierades grönt före merge i GitHub Actions CI #255 och Dependency Review #173.
- F-001, F-002, F-004 och F-005 är lösta. **F-003 kvarstår delvis löst tills R-011 verifierats.**
- **R-011 är implementerad på separat branch och väntar på CI-verifiering.**
- `useContributions.ts` använder nu gemensam `frontend/src/api/request.ts` för GET-anropet.
- URL/query, AbortSignal och feltexten `Contributions request failed with HTTP <status>` är bevarade.
- Response-defaulting och state-semantik är fortsatt lokala och oförändrade.
- Ingen annan hook eller generell API-client-abstraktion ingår i R-011.
- **Nästa steg efter grön R-011 är en ny riskbaserad bedömning innan fler callers berörs.**
