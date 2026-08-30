# External Analysis API Contract


> Machine-readable OpenAPI: [`openapi/external-analysis-v1.yaml`](openapi/external-analysis-v1.yaml)  
> Practical GPT/API guide: [`gpt-api-integration.md`](gpt-api-integration.md)  
> Sample Custom GPT Action: [`openapi/custom-gpt-action-example.yaml`](openapi/custom-gpt-action-example.yaml)


Status: **v1 contract, Step 59**

The External Analysis API exposes compact user-scoped analytics suitable for an LLM/GPT client. It deliberately avoids frontend presentation objects, chart configuration, HTML, source-code content, verbose rationales and provider-specific UI state.

## Media type and versioning

Clients MUST request:

```http
Accept: application/vnd.developer-analytics.analysis.v1+json
```

The URL remains `/api/me/...`, while the media type versions the compact external contract independently from the dashboard's ordinary `application/json` responses.

All endpoints are user-scoped. A client can only receive the authenticated user's data.

## Authentication

External clients authenticate with a dedicated bearer token:

```http
Authorization: Bearer da_ext_<secret>
Accept: application/vnd.developer-analytics.analysis.v1+json
```

External tokens are:

- created by the signed-in user under Account,
- separate from browser-session cookies,
- separate from the GitHub provider credential,
- stored server-side only as SHA-256 hashes,
- returned in raw form once at creation time,
- revocable,
- restricted to explicit read scopes.

Scopes map directly to the compact analysis endpoints:

| Scope | Endpoint |
| --- | --- |
| `PROFILE_READ` | `/api/me/profile` |
| `PROJECTS_READ` | `/api/me/projects` |
| `ACTIVITY_READ` | `/api/me/activity` |
| `TECHNOLOGIES_READ` | `/api/me/technologies` |
| `PROJECT_TYPES_READ` | `/api/me/project-types` |
| `CONTRIBUTIONS_READ` | `/api/me/contributions` |
| `EVIDENCE_READ` | `/api/me/evidence` |
| `AI_ASSESSMENTS_WRITE` | `POST /api/me/ai-assessments` |

A missing/invalid/revoked bearer token is rejected. A valid token without the endpoint's scope is forbidden.


## Privacy scopes

Each external token also has exactly one server-enforced privacy scope:

| Privacy scope | Public data | Private aggregates | Private project detail |
| --- | --- | --- | --- |
| `PUBLIC_ONLY` | Yes | No | No |
| `PUBLIC_PLUS_PRIVATE_AGGREGATES` | Yes | Yes | No |
| `FULL_AUTHORISED_ANALYSIS` | Yes | Yes | Yes, for repositories already authorised and included in analysis |

`PUBLIC_ONLY` is the default.

Privacy scopes are independent from endpoint read scopes. A token therefore needs both the endpoint scope (for example `PROJECTS_READ`) and a sufficiently permissive privacy scope.

Server enforcement rules include:

- `/projects` never returns private repository records unless the token has `FULL_AUTHORISED_ANALYSIS`.
- profile/activity/contribution aggregates include private data only with `PUBLIC_PLUS_PRIVATE_AGGREGATES` or `FULL_AUTHORISED_ANALYSIS`.
- technology/project-type/evidence aggregates are restricted to public-only provenance for `PUBLIC_ONLY`.
- a privacy scope never grants access to a private repository that the user has not already authorised and included in Developer Analytics.

## Privacy and corrections

The contract:

- includes only repositories currently `included_in_analysis`,
- preserves `privacyProvenance` where an aggregate may contain private evidence,
- respects rejected project categories,
- respects suppressed technology inferences,
- exposes whether a project is excluded from user-level AI-profile conclusions,
- never returns source file contents, commit diffs, prompt text or provider credentials.

Private-data authorization and export policy remain separate concepts. This API does not silently widen either.

## Endpoints

### `GET /api/me/profile`

Compact whole-profile summary.

```json
{
  "contractVersion": "v1",
  "repositoryCount": 42,
  "publicRepositoryCount": 38,
  "privateRepositoryCount": 4,
  "ownedRepositoryCount": 31,
  "externalRepositoryCount": 11,
  "contributionCount": 5820,
  "privacyProvenance": "INCLUDES_PRIVATE",
  "topTechnologies": [],
  "topProjectTypes": []
}
```

### `GET /api/me/projects?limit=50`

Returns compact project records. `limit` is capped at 200.

Fields:

- `id`
- `name`
- `visibility`
- `ownership`
- `lastActivityAt`
- `projectTypes`
- `technologies`
- `excludedFromAiProfile`

Descriptions, repository URLs, source files and detailed evidence are intentionally omitted.

### `GET /api/me/activity?months=24`

Returns compact activity totals and a monthly time series. `months` is capped at 120.

Fields:

- `contributionCount`
- `activeProjectCount`
- `contributionTypes`
- `monthly`
- `privacyProvenance`

### `GET /api/me/technologies?limit=30`

Returns evidence-based technology summaries after user corrections.

Fields:

- `key`
- `name`
- `evidenceLevel`
- `evidenceScore`
- `projectCount`
- `firstObservedAt`
- `lastObservedAt`
- `privacyProvenance`

### `GET /api/me/project-types?limit=30`

Returns compact project-type counts.

Fields:

- `key`
- `name`
- `projectCount`

### `GET /api/me/contributions`

Returns contribution totals grouped by type rather than individual commit/PR/issue titles.

```json
{
  "total": 5820,
  "byType": {
    "commit": 5100,
    "pull_request": 340,
    "review": 210,
    "issue": 170,
    "release": 0,
    "maintenance": 0
  },
  "privacyProvenance": "INCLUDES_PRIVATE"
}
```

### `GET /api/me/evidence?limit=50`

Returns compact aggregated evidence, not raw repository content.

Technology evidence fields:

- `technologyKey`
- `evidenceType`
- `strength`
- `observations`
- `privacyProvenance`

Project-type evidence fields:

- `projectTypeKey`
- `source`
- `confidence`
- `observations`
- `privacyProvenance`

## Contract principles

1. **Compact before exhaustive.** The API is intended to provide enough structured context for reasoning, not mirror every database field.
2. **Measured data remains distinguishable from inference.** Evidence strength, confidence and source fields are retained.
3. **No frontend coupling.** No cards, labels, chart widths or presentation state appear in the contract.
4. **No raw private source content.** Repository source content is outside this contract.
5. **Corrections are honored.** User corrections affect the analytical view without deleting source facts.
6. **Stable version boundary.** Breaking field changes require a new analysis media type version.


## Returned AI assessments

An authorised external GPT/client can return an assessment to Developer Analytics:

```http
POST /api/me/ai-assessments
Authorization: Bearer da_ext_<secret>
Content-Type: application/json

{
  "analysisType": "developer-profile",
  "content": {
    "summary": "Predominantly backend and platform-oriented work.",
    "observations": [
      "Strong Java/Quarkus evidence",
      "Increasing infrastructure activity"
    ]
  },
  "containsPrivateData": false
}
```

The token must include `AI_ASSESSMENTS_WRITE`.

The server derives and stores `sourceClient`, `timestamp` and `dataScope` from the authenticated external token. The client cannot override those fields.

If `containsPrivateData=true`, the token privacy scope must allow at least private aggregates. `PUBLIC_ONLY` tokens cannot submit an assessment marked as containing private data.

The signed-in user can manage returned assessments with the normal browser session:

```text
GET    /api/me/ai-assessments
DELETE /api/me/ai-assessments/{id}
```

Returned assessment records contain:

- `analysisType`
- `sourceClient`
- `timestamp`
- `dataScope`
- structured JSON `content`
- `containsPrivateData`

Deleting a returned assessment does not delete measured Developer Analytics source facts.
