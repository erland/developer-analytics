# GPT/API Integration Guide

This guide is the practical companion to
[`openapi/external-analysis-v1.yaml`](openapi/external-analysis-v1.yaml).

The integration is designed so an external GPT can **read a compact developer
profile** and, when explicitly allowed, **write an AI assessment back** to the
user's Developer Analytics account.

## 1. Create an external client credential

Sign in to Developer Analytics in the browser and open **Account → GPT/API access
tokens**.

Choose:

1. a descriptive client name, for example `ChatGPT – developer profile`,
2. the endpoint scopes that client needs,
3. one privacy scope.

The raw `da_ext_...` token is displayed once. Store it in the external client's
secret/authentication configuration. Do not paste it into prompts or commit it to
source control.

The external token is:

- user-specific,
- revocable,
- independent of the browser session,
- independent of the GitHub OAuth credential,
- persisted by Developer Analytics only as a SHA-256 hash.

## 2. Endpoint scopes

| Scope | Allows |
| --- | --- |
| `PROFILE_READ` | `GET /api/me/profile` |
| `PROJECTS_READ` | `GET /api/me/projects` |
| `ACTIVITY_READ` | `GET /api/me/activity` |
| `TECHNOLOGIES_READ` | `GET /api/me/technologies` |
| `PROJECT_TYPES_READ` | `GET /api/me/project-types` |
| `CONTRIBUTIONS_READ` | `GET /api/me/contributions` |
| `EVIDENCE_READ` | `GET /api/me/evidence` |
| `AI_ASSESSMENTS_WRITE` | `POST /api/me/ai-assessments` |

Grant only the scopes the client actually needs.

## 3. Privacy scopes

Endpoint scopes say **what operation** a token may perform. Privacy scope says
**which data class** may be returned.

| Privacy scope | Public | Private aggregates | Private project detail |
| --- | ---: | ---: | ---: |
| `PUBLIC_ONLY` | Yes | No | No |
| `PUBLIC_PLUS_PRIVATE_AGGREGATES` | Yes | Yes | No |
| `FULL_AUTHORISED_ANALYSIS` | Yes | Yes | Yes |

`PUBLIC_ONLY` is the safe default.

`FULL_AUTHORISED_ANALYSIS` does **not** itself authorize private GitHub
repositories. A private repository must already have been explicitly authorized
and included in Developer Analytics.

For returned assessments, a `PUBLIC_ONLY` token cannot submit
`containsPrivateData=true`.

## 4. Required headers

Read operations use both bearer authentication and the versioned media type:

```http
Authorization: Bearer da_ext_<secret>
Accept: application/vnd.developer-analytics.analysis.v1+json
```

Write-back of AI assessments uses:

```http
Authorization: Bearer da_ext_<secret>
Content-Type: application/json
Accept: application/json
```

## 5. Example calls

### Profile

```bash
curl \
  -H "Authorization: Bearer ${DEVELOPER_ANALYTICS_TOKEN}" \
  -H "Accept: application/vnd.developer-analytics.analysis.v1+json" \
  https://developer-analytics.example.com/api/me/profile
```

### Projects

```bash
curl \
  -H "Authorization: Bearer ${DEVELOPER_ANALYTICS_TOKEN}" \
  -H "Accept: application/vnd.developer-analytics.analysis.v1+json" \
  "https://developer-analytics.example.com/api/me/projects?limit=25"
```

### Activity

```bash
curl \
  -H "Authorization: Bearer ${DEVELOPER_ANALYTICS_TOKEN}" \
  -H "Accept: application/vnd.developer-analytics.analysis.v1+json" \
  "https://developer-analytics.example.com/api/me/activity?months=24"
```

### Technologies and project types

```bash
curl \
  -H "Authorization: Bearer ${DEVELOPER_ANALYTICS_TOKEN}" \
  -H "Accept: application/vnd.developer-analytics.analysis.v1+json" \
  "https://developer-analytics.example.com/api/me/technologies?limit=30"

curl \
  -H "Authorization: Bearer ${DEVELOPER_ANALYTICS_TOKEN}" \
  -H "Accept: application/vnd.developer-analytics.analysis.v1+json" \
  "https://developer-analytics.example.com/api/me/project-types?limit=30"
```

### Contributions and evidence

```bash
curl \
  -H "Authorization: Bearer ${DEVELOPER_ANALYTICS_TOKEN}" \
  -H "Accept: application/vnd.developer-analytics.analysis.v1+json" \
  https://developer-analytics.example.com/api/me/contributions

curl \
  -H "Authorization: Bearer ${DEVELOPER_ANALYTICS_TOKEN}" \
  -H "Accept: application/vnd.developer-analytics.analysis.v1+json" \
  "https://developer-analytics.example.com/api/me/evidence?limit=50"
```

### Return an AI assessment

```bash
curl -X POST \
  -H "Authorization: Bearer ${DEVELOPER_ANALYTICS_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "analysisType": "developer-profile",
    "content": {
      "summary": "Predominantly backend and platform-oriented activity.",
      "likelyRoles": [
        {
          "role": "Backend developer",
          "confidence": 0.84
        }
      ],
      "observations": [
        "Strong Java and Quarkus evidence",
        "Increasing infrastructure and automation activity"
      ]
    },
    "containsPrivateData": false
  }' \
  https://developer-analytics.example.com/api/me/ai-assessments
```

## 6. Recommended GPT usage pattern

For a general developer-profile analysis, a GPT should normally:

1. call `/api/me/profile`,
2. use `/api/me/technologies` and `/api/me/project-types` for the main evidence,
3. use `/api/me/activity` and `/api/me/contributions` for evolution and engagement,
4. query `/api/me/projects` only when project-level context is needed,
5. query `/api/me/evidence` when it needs to explain why an inference is supported,
6. clearly separate measured API facts from its own interpretations,
7. optionally return a structured assessment through `/api/me/ai-assessments`.

A GPT should not infer that private data is available merely because a token
works. The server's privacy scope is authoritative.

## 7. Error semantics

- `401 Unauthorized`: bearer token is missing, invalid or revoked.
- `403 Forbidden`: token is valid but lacks the endpoint scope, privacy scope,
  or write-back privacy permission.
- `400 Bad Request`: malformed or incomplete write-back payload.

## 8. Custom GPT Action

A sample Action schema is available at:

[`openapi/custom-gpt-action-example.yaml`](openapi/custom-gpt-action-example.yaml)

Before importing it into a Custom GPT:

1. replace the example server URL with the deployed Developer Analytics URL,
2. configure bearer authentication with the generated `da_ext_...` token,
3. create a token with only the endpoint scopes/privacy scope that GPT needs.

The Action schema deliberately excludes browser-only credential-management and
assessment-management endpoints. The GPT can write an assessment, but only the
signed-in user can list/delete stored assessments through the normal application.
