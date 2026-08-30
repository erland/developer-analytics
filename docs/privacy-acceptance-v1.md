# Version 1 Privacy Acceptance Test

Step 88 is the v1 end-to-end privacy gate.

The scenario deliberately combines all privacy-sensitive concepts in one
running application:

- public repository,
- explicitly included private repository,
- excluded private repository,
- private-provenance AI analysis,
- second application user,
- public-only external GPT token,
- private-aggregate GPT token,
- fully authorised GPT token,
- public report,
- full private report.

Run:

```bash
export DB_PASSWORD='privacy-acceptance-password'
export CREDENTIAL_ENCRYPTION_KEY='AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8='
export WEB_PORT=18088
bash ./scripts/test-privacy-acceptance.sh
```

## Privacy matrix

| Boundary | Expected result |
| --- | --- |
| Other logged-in user -> private repository ID | `404`; no target-user private metadata |
| Public OSS report -> included private repository | Excluded even if caller requests full-private mode |
| Public OSS report -> private-provenance AI insight | Excluded |
| Full private report -> explicitly included private repository | Included |
| Full private report -> excluded private repository | Excluded |
| Full private report -> another user's repository | Excluded |
| Unauthenticated external analysis endpoint | `401`; no user data |
| `PUBLIC_ONLY` GPT token -> projects | Public projects only |
| `PUBLIC_PLUS_PRIVATE_AGGREGATES` GPT token -> projects | Public project detail only |
| Aggregate GPT token -> profile totals | May contain private aggregate count, never private repository names |
| `FULL_AUTHORISED_ANALYSIS` GPT token -> projects | Explicitly included private project detail allowed |
| Any GPT scope -> excluded private repository | Excluded |

## AI privacy check

The fixture stores a distinctive private-only AI marker with
`PRIVATE_AGGREGATE` provenance.

The acceptance test requires:

- public report: marker absent,
- full private report: marker present.

This proves the report path is filtering the AI inference itself, not only
repository names.

## Why two users are included

Privacy cannot be accepted from one-user fixtures alone. The test creates a
second authenticated account with its own private repository and then verifies
that:

- user B cannot fetch user A's private repository by UUID,
- user A's private report never contains user B's repository,
- user A's external tokens remain scoped to user A.

## External GPT scopes

The acceptance scenario creates tokens through the normal browser-authenticated
token API rather than writing raw token rows manually.

That exercises both:

- token creation/scoping,
- bearer-token enforcement.

The aggregate-only token is especially important: it may receive private
aggregate counts, but it must not receive private project names/detail.

## CI

The GitHub Actions job is named **Privacy acceptance test** and uses a temporary
Compose/PostgreSQL environment.

No live GitHub or Gemini call is required. This keeps the privacy gate
deterministic while exercising the real application/API/export boundaries.
