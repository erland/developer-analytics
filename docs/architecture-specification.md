# Architecture Specification – Developer Activity & Experience Analytics

**Version:** 1.0  
**Status:** Draft architecture specification  
**Scope:** Version 1 architecture with selected future extension points

---

# 1. Purpose

This document describes the proposed technical architecture for the Developer Activity & Experience Analytics service.

The architecture is intended to support the functional specification for version 1 while remaining:

- straightforward to develop,
- simple to run locally,
- easy to self-host,
- suitable for accounts with hundreds of repositories,
- secure for both public and private repository data,
- extensible to additional Git providers and AI services later,
- usable on desktop and mobile,
- operable as a small single-host Docker Compose deployment without preventing future scaling.

The architecture deliberately favours a small number of well-understood components over a distributed microservice architecture.

---

# 2. Architecture Summary

The recommended version 1 stack is:

| Area | Recommended technology |
|---|---|
| Frontend | React + TypeScript |
| Frontend build | Vite |
| Backend/API | Java + Quarkus |
| REST API | Quarkus REST |
| Persistence | Jakarta Persistence / Hibernate ORM |
| Persistence style | Repository pattern; Panache may be used where useful |
| Database migrations | Flyway |
| Database | PostgreSQL |
| Authentication | GitHub-based authentication/authorisation |
| GitHub access | GitHub App / GitHub APIs |
| AI processing | Provider abstraction; Gemini API as initial optional provider |
| External GPT integration | Authenticated REST API |
| Background work | Quarkus-based worker using persistent database jobs |
| Reverse proxy / web entry point | Nginx |
| Packaging | Docker images |
| Local/self-hosted orchestration | Docker Compose |
| Primary API format | JSON over HTTPS |
| Export generation | Backend-generated Markdown; PDF generation as a separate rendering step |

No additional database, message broker, search engine or cache is required for version 1.

---

# 3. Architectural Principles

## 3.1 Modular monolith before microservices

Version 1 shall use a modular monolithic backend rather than multiple independently developed microservices.

Logical boundaries shall nevertheless be explicit so that selected parts can later be separated if needed.

Suggested backend modules include:

- identity and account,
- Git provider integration,
- repository inventory,
- contribution collection,
- statistics,
- technology detection,
- project classification,
- AI analysis,
- report generation,
- external AI/GPT API,
- background jobs,
- privacy and data lifecycle.

The objective is to keep deployment simple without creating a tightly coupled codebase.

## 3.2 PostgreSQL as the single system of record

PostgreSQL shall be the authoritative store for:

- users,
- provider identities,
- repository metadata,
- contribution metadata,
- commit statistics,
- time-series aggregates,
- detected technologies,
- project classifications,
- AI assessments,
- background jobs,
- report metadata,
- privacy settings,
- synchronisation state.

The architecture shall avoid introducing another persistence technology until there is demonstrated need.

## 3.3 Asynchronous ingestion

Large GitHub accounts shall be treated as normal.

Repository discovery, historical statistics collection, project analysis and AI enrichment shall therefore be executed asynchronously.

Interactive API requests shall not perform a complete GitHub profile scan before responding.

## 3.4 Privacy by architecture

Private repository information shall be isolated by user at the persistence and API layers.

Public GitHub source data does not imply that the service's aggregated user profile is public.

The default architecture exposes no public user profile endpoint.

## 3.5 Evidence and interpretation are separate

Measured facts, derived values and AI interpretations shall be represented separately.

AI output shall never overwrite factual source data.

## 3.6 Provider abstractions

GitHub is the only source-code provider required by version 1, but the domain model shall not assume that every repository or identity originates from GitHub.

Similarly, AI functionality shall be accessed through an internal AI-provider abstraction rather than being embedded directly into domain logic.

---

# 4. High-Level Architecture

```text
                         Internet
                            |
                            v
                   +----------------+
                   |     Nginx      |
                   | Web / Gateway  |
                   +-------+--------+
                           |
             +-------------+-------------+
             |                           |
             v                           v
     React static application        /api/*
                                      |
                                      v
                              +---------------+
                              | Quarkus API   |
                              | Application   |
                              +-------+-------+
                                      |
                  +-------------------+------------------+
                  |                   |                  |
                  v                   v                  v
           PostgreSQL          GitHub APIs        AI Provider API
                  ^                                  (optional)
                  |
                  |
            +-----+--------+
            | Quarkus      |
            | Worker       |
            +--------------+
```

External GPT integration:

```text
External GPT / AI client
          |
          | authenticated API
          v
     Nginx / Gateway
          |
          v
      Quarkus API
          |
          v
     User-scoped data
       in PostgreSQL
```

The external client may optionally return a complementary analysis:

```text
External GPT
    |
    | POST analysis
    v
Quarkus API
    |
    v
Stored AI assessment
```

---

# 5. Deployment Topology

The standard version 1 Docker Compose deployment should contain four runtime services:

```text
+------------------------------------------------------+
| Docker Compose                                       |
|                                                      |
|  +-------------+                                     |
|  | web/nginx   |  <-- only normal public entrypoint |
|  +------+------+                                     |
|         |                                            |
|         +-----------------+                          |
|                           |                          |
|                    +------v------+                   |
|                    | backend/API |                   |
|                    +------+------+\                  |
|                           |        \                 |
|                           |         \ external APIs  |
|                           |          -> GitHub       |
|                           |          -> AI provider  |
|                           |                          |
|                    +------v------+                   |
|                    | PostgreSQL  |                   |
|                    +------^------+                   |
|                           |                          |
|                    +------+------+\                  |
|                    | worker      | \ external APIs   |
|                    +-------------+  -> GitHub        |
|                                      -> AI provider  |
+------------------------------------------------------+
```

The recommended services are:

1. **web**
   - builds/contains the React frontend,
   - serves static frontend assets through Nginx,
   - proxies `/api` requests to the backend,
   - provides the single ordinary HTTP/HTTPS entry point.

2. **backend**
   - exposes REST endpoints,
   - handles login callback/session handling,
   - performs interactive application logic,
   - schedules background work,
   - reads and writes PostgreSQL,
   - provides the external GPT/API interface.

3. **worker**
   - runs background collection and analysis jobs,
   - uses the same domain/application codebase as the backend where practical,
   - does not need to be exposed publicly,
   - reads work from persistent job records in PostgreSQL.

4. **database**
   - PostgreSQL,
   - persistent Docker volume,
   - accessible only from backend/worker networks.

---

# 6. Why a Separate Background Worker

GitHub analysis is inherently uneven.

A normal dashboard request may require milliseconds of database access, while an initial account import may involve:

- discovering hundreds of repositories,
- paging through API results,
- retrieving historical contribution information,
- calculating aggregates,
- identifying technology evidence,
- classifying projects,
- calling an AI provider,
- retrying API requests after temporary limits.

These activities shall not occupy request-handling threads or make the web API appear unavailable.

The architecture therefore separates:

```text
Interactive request path
User -> API -> PostgreSQL -> response
```

from:

```text
Background analysis path
Job -> worker -> GitHub/AI -> PostgreSQL
```

The API and worker can be separate processes built from the same backend repository and Docker build.

This provides process isolation without introducing a microservice architecture.

---

# 7. Job Queue Strategy

Version 1 does not require Redis, Kafka, RabbitMQ or another message broker.

A PostgreSQL-backed job queue is sufficient.

A background job record should conceptually contain:

- job identifier,
- user identifier,
- job type,
- status,
- priority,
- creation time,
- next execution time,
- attempt count,
- progress,
- error information,
- optional structured parameters.

Example job types:

- discover repositories,
- synchronise repository,
- collect commit statistics,
- rebuild monthly aggregates,
- detect technologies,
- classify project,
- perform AI enrichment,
- regenerate user summary,
- generate report,
- remove user data.

Workers shall claim jobs atomically so multiple worker containers could be used later without processing the same job simultaneously.

This approach keeps version 1 operationally simple while allowing additional worker instances if needed.

A dedicated message broker should only be introduced after demonstrated workload requires it.

---

# 8. Frontend Architecture

## 8.1 Technology

The frontend should use:

- React,
- TypeScript,
- Vite.

This stack is suitable for:

- interactive dashboards,
- responsive views,
- filtering,
- charts,
- long-lived single-page navigation,
- progressive loading of analysis results,
- typed API clients.

## 8.2 Application structure

The frontend should be organised by functional areas rather than by generic technical folders only.

Suggested feature areas:

```text
frontend/
  app/
  auth/
  overview/
  activity/
  projects/
  technologies/
  project-types/
  contributions/
  ai-insights/
  reports/
  privacy/
  account/
  shared/
```

## 8.3 Server state

Data obtained from the backend should be treated as server state.

The frontend should:

- retrieve paginated data,
- cache currently viewed results,
- refetch when synchronisation completes,
- avoid duplicating server-side business logic,
- represent loading/partial/failed analysis states explicitly.

## 8.4 Responsive design

The application shall be responsive from the beginning.

Desktop may use:

- multi-column dashboards,
- side navigation,
- larger tables,
- simultaneous filters and charts.

Mobile shall use:

- vertically stacked dashboard cards,
- compact navigation,
- project cards where tables would become too wide,
- collapsible filter panels,
- touch-friendly controls,
- charts adapted to narrow widths.

Responsive behaviour is a frontend concern; separate desktop and mobile applications are not required.

## 8.5 Charting

A React-compatible chart library may be selected during implementation.

The architecture does not mandate a specific library.

Charts shall support:

- responsive sizing,
- accessible labels/tooltips,
- time-series data,
- category breakdowns,
- technology evolution,
- commit distributions.

Chart data shall be calculated primarily by the backend rather than by downloading raw commit history to the browser.

---

# 9. Web Entry Point and Nginx

Nginx is recommended as the public web/gateway component for version 1.

Its responsibilities shall be intentionally small:

- serve the compiled React application,
- provide SPA fallback routing,
- proxy `/api/*` to the Quarkus backend,
- add standard HTTP security headers,
- support response compression,
- enforce reasonable request-size limits,
- optionally terminate TLS in self-hosted deployments.

Conceptually:

```text
/                 -> React application
/assets/*         -> static frontend assets
/api/*            -> Quarkus backend
```

The browser therefore communicates with one origin.

Benefits include:

- simple deployment,
- no frontend CORS requirement for normal application traffic,
- simple container topology,
- frontend and API accessible through a single hostname.

For environments with an existing ingress or reverse proxy, TLS may instead terminate outside the Compose stack.

---

# 10. Backend Architecture

## 10.1 Technology

The backend should use:

- Java,
- Quarkus,
- Quarkus REST,
- Jakarta Persistence / Hibernate ORM,
- PostgreSQL JDBC,
- Flyway.

Hibernate ORM with Quarkus supports PostgreSQL directly and is suitable for the relational domain model required by the service.

## 10.2 Internal layering

The backend should maintain clear logical layers:

```text
REST/API
   |
   v
Application services
   |
   v
Domain logic
   |
   v
Repositories / provider adapters
   |
   +------> PostgreSQL
   +------> GitHub
   +------> AI providers
```

External provider representations shall not be used directly as core domain entities.

## 10.3 Suggested backend modules

### Identity

Responsibilities:

- user account,
- authenticated identity,
- connected provider identities,
- session/access context.

### Provider integration

Responsibilities:

- GitHub API client,
- GitHub App installation information,
- repository discovery,
- API pagination,
- rate-limit awareness,
- provider-specific data mapping.

### Repository domain

Responsibilities:

- repositories,
- ownership,
- visibility,
- forks,
- activity periods,
- project metadata.

### Contributions

Responsibilities:

- commits,
- pull requests,
- reviews,
- issues,
- release/maintenance activity where supported.

### Statistics

Responsibilities:

- monthly/yearly aggregates,
- commit-size statistics,
- active projects,
- contribution trends,
- public/private breakdowns.

### Technology analysis

Responsibilities:

- technology evidence,
- evidence strength,
- normalised technologies,
- technology timelines.

### Project classification

Responsibilities:

- project categories,
- deterministic classification evidence,
- AI classifications,
- confidence.

### AI analysis

Responsibilities:

- AI provider abstraction,
- AI prompts/requests,
- structured AI responses,
- AI usage policies,
- storage of assessments.

### Reports

Responsibilities:

- report configuration,
- Markdown generation,
- PDF rendering orchestration,
- privacy-aware export.

### External analysis API

Responsibilities:

- expose user-scoped analysis,
- scopes/authorisation,
- receive complementary AI assessments,
- audit external analysis submissions.

### Jobs

Responsibilities:

- scheduling,
- job state,
- retry handling,
- progress.

---

# 11. Persistence Architecture

## 11.1 PostgreSQL suitability

A relational database is appropriate for version 1.

The dominant data model consists of strongly related entities:

```text
User
  |
  +-- Provider Identity
  |
  +-- Repository
         |
         +-- Contributions
         +-- Commit statistics
         +-- Technology evidence
         +-- Project classifications
         +-- Time aggregates
```

Typical queries are also relational:

- repositories for user,
- commits for repository and time period,
- technologies across user's projects,
- activity grouped by month,
- external projects ranked by involvement,
- classifications by project,
- public/private breakdown.

PostgreSQL handles these naturally.

## 11.2 Time-series data

A specialised time-series database is not required.

Time-series information should be stored as:

1. source-level facts where useful,
2. precomputed daily/monthly/yearly aggregates for dashboard queries.

Example aggregate grain:

```text
user_activity_month
  user_id
  year_month
  commit_count
  additions
  deletions
  changed_lines
  active_repository_count
  pull_request_count
  review_count
  issue_count
```

Indexes on user and time period are sufficient for version 1 scale.

## 11.3 Flexible metadata

Provider-specific or analysis-specific metadata that does not justify dedicated columns may be stored in PostgreSQL JSONB.

Examples:

- raw provider metadata retained temporarily,
- AI evidence payloads,
- classification explanation,
- provider-specific repository attributes.

JSONB shall supplement rather than replace the relational domain model.

## 11.4 No graph database in version 1

Although relationships exist between users, repositories, technologies and categories, version 1 queries do not require graph traversal sophisticated enough to justify a graph database.

Graph-style views can be produced from relational queries.

A graph database should only be reconsidered if future functionality introduces complex cross-user ecosystem relationship analysis.

## 11.5 No document database in version 1

Repository metadata and AI output do not require a dedicated document database.

PostgreSQL relational tables plus JSONB provide sufficient flexibility.

## 11.6 No search engine in version 1

Project search can initially use PostgreSQL filtering and text-search capabilities.

A dedicated search platform should only be introduced if later functionality requires:

- cross-user public search,
- very large full-text indexes,
- semantic search across large repository documents.

None are version 1 requirements.

---

# 12. Conceptual Data Model

The exact physical schema will be defined during implementation, but the main concepts should include:

```text
User
 |
 +-- ProviderIdentity
 |
 +-- ProviderConnection
 |
 +-- Repository
 |    |
 |    +-- RepositorySnapshot
 |    +-- Contribution
 |    +-- CommitStatistic
 |    +-- TechnologyEvidence
 |    +-- ProjectClassification
 |
 +-- UserActivityAggregate
 |
 +-- TechnologyAggregate
 |
 +-- AiAssessment
 |
 +-- Report
 |
 +-- BackgroundJob
```

Important identity keys shall include both:

- internal immutable IDs,
- provider + provider-specific external ID.

Repository URLs or names shall not be treated as stable primary identifiers.

---

# 13. Data Volume Strategy

A user with more than 200 repositories shall be treated as a normal case.

The architecture shall avoid blindly storing every possible GitHub object forever.

Data shall be separated into three levels.

## 13.1 Core inventory

Retain:

- repository identity,
- ownership,
- visibility,
- important metadata,
- synchronisation state.

## 13.2 Analysis facts

Retain data required for calculations and evidence, such as:

- contribution identifiers,
- timestamps,
- commit size statistics,
- contribution type,
- relevant project metadata.

## 13.3 Aggregates

Retain precomputed:

- monthly activity,
- yearly activity,
- repository activity summaries,
- technology timelines,
- category summaries.

Dashboard queries should usually read aggregates rather than repeatedly aggregate raw history.

---

# 14. GitHub Integration Architecture

## 14.1 GitHub roles

GitHub serves two different purposes:

1. user authentication,
2. repository/contribution data source.

These roles shall remain logically separated.

## 14.2 Public analysis

A user shall be able to establish an account and analyse authorised public GitHub information.

## 14.3 Private repository access

Private repository access shall use explicit GitHub authorisation.

The user shall control which private repositories are accessible where GitHub permissions allow such selection.

The service shall request only permissions required for analysis.

## 14.4 Provider adapter

The backend shall access GitHub through an internal provider interface.

Conceptually:

```text
SourceControlProvider

  getCurrentIdentity()
  listRepositories()
  getRepository()
  getRepositoryLanguages()
  getContributions()
  getCommitStatistics()
  getPullRequests()
  getReviews()
  getIssues()
```

GitHub-specific request/response models shall remain inside the GitHub adapter.

This allows a future GitLab adapter to implement the same application-facing concepts.

## 14.5 Rate-limit handling

GitHub API limits shall be considered normal operational constraints.

The integration shall support:

- pagination,
- request throttling,
- retry after temporary limit,
- persisted synchronisation cursors/state,
- prioritisation of recent/important repositories,
- incremental refresh.

A rate-limit condition shall delay background work rather than fail the user's entire account.

---

# 15. Synchronisation Architecture

## 15.1 Initial import

Suggested stages:

```text
1. Establish identity
2. Discover repositories
3. Store repository inventory
4. Determine public/private/own/external
5. Queue repository synchronisation
6. Collect contribution/statistical data
7. Calculate aggregates
8. Detect technologies
9. Classify projects
10. Run optional AI enrichment
11. Build user summary
```

The user may begin browsing partial results after stages 2–4.

## 15.2 Incremental refresh

Each repository shall retain synchronisation metadata such as:

- last successful sync,
- provider update timestamp,
- last processed activity marker,
- analysis version.

Unchanged repositories shall not undergo full re-analysis.

## 15.3 Analysis versioning

Technology detection and AI classification logic will evolve.

Derived records should include an analysis/version identifier so the service can determine which repositories need reprocessing after analysis rules change.

---

# 16. Technology Detection Architecture

Technology detection should use multiple evidence sources.

## 16.1 Deterministic detection

Examples:

- repository language statistics,
- dependency manifests,
- build files,
- workflow files,
- container files,
- infrastructure manifests,
- repository topics.

Detectors should produce structured evidence:

```text
TechnologyEvidence
  technology
  evidence_type
  source
  confidence
  first_seen
  last_seen
```

## 16.2 AI enrichment

AI may:

- normalise ambiguous technologies,
- classify project purpose,
- summarise evidence,
- identify likely higher-level technical areas.

AI output shall reference evidence rather than replace it.

---

# 17. AI Provider Architecture

## 17.1 Provider abstraction

AI functionality shall be accessed through an internal abstraction.

Conceptually:

```text
AiProvider
  classifyProject(...)
  summariseProject(...)
  inferRoles(...)
  analyseTechnologyHistory(...)
```

Initial implementations may use Gemini.

The rest of the application shall not depend on Gemini-specific request formats.

## 17.2 Structured responses

AI requests should prefer structured responses matching explicit schemas.

For example:

```json
{
  "projectTypes": [],
  "technologies": [],
  "likelyRoles": [],
  "confidence": 0.0,
  "explanation": ""
}
```

This reduces ambiguity and makes results storable and reviewable.

## 17.3 AI availability

AI processing is optional enrichment.

If an AI quota is exhausted:

- GitHub synchronisation continues,
- deterministic analysis continues,
- dashboards remain available,
- AI jobs remain pending or are deferred.

## 17.4 Private repository policy

The architecture shall distinguish between:

- factual local processing of private repository metadata,
- information transmitted to an external AI provider.

Private repository information shall not be sent to an AI provider unless the configured privacy policy and user consent permit it.

The service should support different AI policies for:

- public repositories,
- private repositories.

---

# 18. External GPT Integration

The service shall expose an authenticated user-scoped API suitable for a Custom GPT or another authorised AI client.

The external API shall not expose arbitrary users.

Conceptual endpoints:

```text
GET  /api/me/profile
GET  /api/me/projects
GET  /api/me/projects/{id}
GET  /api/me/activity
GET  /api/me/technologies
GET  /api/me/project-types
GET  /api/me/contributions
GET  /api/me/evidence

POST /api/me/ai-assessments
GET  /api/me/ai-assessments
DELETE /api/me/ai-assessments/{id}
```

The API should support scopes that restrict access to:

- public data only,
- public plus private aggregates,
- explicitly authorised detailed private analysis.

The authentication mechanism used for external AI clients shall be separate from a browser session and revocable by the user.

---

# 19. Authentication and Session Architecture

## 19.1 Browser login

The browser shall authenticate through a GitHub-based flow.

After successful authentication, the backend shall establish the service's own authenticated session/security context.

The frontend shall not need to persist long-lived GitHub access credentials.

## 19.2 Provider credentials

Provider access credentials shall be stored only when required for synchronisation.

They shall:

- never be returned to the frontend,
- never be included in reports,
- never be written to normal application logs,
- be handled as secrets.

## 19.3 External API credentials

Credentials/tokens granted to a GPT or other client shall be:

- user-specific,
- scoped,
- revocable,
- separate from GitHub provider credentials.

---

# 20. Authorisation Architecture

Every user-owned domain object shall be associated directly or indirectly with an internal user ID.

API access checks shall be based on authenticated user identity, not on identifiers supplied by the client.

Incorrect:

```text
GET /users/123/private-projects
```

with trust in `123`.

Preferred:

```text
GET /me/projects
```

where the backend resolves the user from the authenticated security context.

Administrative functionality, if introduced, shall not automatically permit viewing private repository analysis.

---

# 21. Private Data Isolation

Version 1 uses one PostgreSQL database for all users, with logical row-level ownership enforced by the application.

Every private user-derived entity shall be linked to its owner.

The architecture should make accidental cross-user queries difficult through:

- repository access patterns that require user context,
- API endpoints based on `/me`,
- integration tests for tenant isolation,
- no unauthenticated analysis endpoints.

For a version 1 service this is preferable to creating a separate physical database per user.

A future high-assurance deployment could add database-level row security if desired.

---

# 22. Sensitive Data Handling

The service shall avoid storing:

- raw GitHub passwords,
- unnecessary raw private source code,
- unnecessary commit diffs,
- secrets found inside repositories.

Standard analysis should rely primarily on:

- metadata,
- statistics,
- language information,
- selected project descriptors,
- dependency/configuration evidence.

Any future deep source-code analysis shall be a separately controlled capability.

---

# 23. Database Migrations

Flyway shall own database schema migrations.

Requirements:

- schema changes are version controlled,
- migrations run in deterministic order,
- production data is not recreated on application startup,
- container upgrades preserve the existing database,
- rollback strategy is handled through backup/restore and forward fixes rather than automatic destructive downgrade scripts.

The application shall not rely on automatic ORM schema generation in production.

---

# 24. API Design

The frontend and external AI clients shall communicate with the backend through REST/JSON.

## 24.1 General design

APIs should be:

- resource oriented,
- versionable,
- paginated for large collections,
- explicit about partial results,
- explicit about public/private scope.

## 24.2 Long-running operations

A long-running action shall return a job or analysis identifier rather than keep an HTTP request open.

Example:

```text
POST /api/me/synchronisation

202 Accepted
{
  "jobId": "...",
  "status": "QUEUED"
}
```

The frontend can retrieve job state separately.

---

# 25. Reporting Architecture

## 25.1 Markdown

Markdown is the canonical version 1 report representation.

The backend shall assemble report content from:

- persisted statistics,
- persisted evidence,
- selected AI assessments,
- user report/privacy settings.

## 25.2 PDF

PDF should be generated from the same logical report representation rather than by independently rebuilding report content.

Conceptually:

```text
Analysis data
     |
     v
Report model
     |
     +----> Markdown
     |
     +----> PDF renderer
```

This keeps Markdown and PDF content consistent.

PDF rendering may occur as a background job.

## 25.3 Report privacy

The report builder shall receive an explicit privacy scope:

```text
PUBLIC_ONLY
PRIVATE_AGGREGATED
PRIVATE_FULL
```

Report templates shall not decide privacy based merely on repository visibility after the fact.

---

# 26. Docker Packaging

Every runtime component shall be distributed as a Docker image or standard database image.

Recommended source repository structure:

```text
/
  frontend/
  backend/
  deploy/
    compose.yaml
    nginx/
  docs/
```

Possible build outputs:

```text
project-web
project-backend
postgres
```

The same backend image may be started in API or worker mode if practical.

Example conceptual Compose services:

```yaml
services:
  web:
    # React static assets + Nginx
  backend:
    # Quarkus API
  worker:
    # Quarkus background worker
  db:
    # PostgreSQL
```

---

# 27. Docker Networking

The standard Compose deployment should define at least:

```text
public/web network
  web <-> backend

internal/data network
  backend <-> database
  worker  <-> database
```

The database shall not expose a host port by default.

The worker shall not expose a public port.

The backend need not expose a host port when all browser/API traffic is routed through Nginx.

Only the web/gateway service normally needs to publish an external port.

---

# 28. Persistent Storage

PostgreSQL data shall be stored in a persistent Docker volume or host-mounted persistent location.

Generated reports may use:

- a persistent application volume, or
- database metadata plus regenerated export.

Version 1 should avoid requiring an external object-storage service.

If report/file volume later becomes significant, S3-compatible storage can be introduced behind a storage abstraction.

---

# 29. Configuration

Runtime configuration shall come from environment variables and/or mounted secret/config files.

Configuration areas include:

- public application URL,
- database connection,
- GitHub application configuration,
- AI provider configuration,
- session/security configuration,
- report/export settings,
- logging level.

Secrets shall not be compiled into frontend assets or Docker images.

---

# 30. Development Environment

The standard development setup should be runnable with Docker Compose for supporting infrastructure.

Developers may run:

- frontend locally with hot reload,
- backend locally in Quarkus development mode,
- PostgreSQL through Docker,

or run the complete stack in Docker.

Production and development shall use the same database technology to avoid behavioural differences.

---

# 31. Observability

Version 1 shall provide structured operational visibility without requiring a large monitoring stack.

At minimum:

- structured application logs,
- request correlation identifier,
- background job identifiers,
- synchronisation status,
- health endpoints,
- readiness checks,
- database health,
- GitHub API error/rate-limit logging,
- AI provider error/quota logging.

Sensitive data and credentials shall not be written to logs.

A full metrics/observability platform may be added later.

---

# 32. Health and Container Lifecycle

Containers shall expose meaningful health/readiness information.

Docker Compose startup order alone shall not be treated as proof that PostgreSQL is ready.

The backend/worker shall tolerate temporary unavailability of:

- database during startup,
- GitHub,
- AI provider.

External API failure shall not cause a permanent container crash loop when graceful retry is possible.

---

# 33. Backup and Restore

For a self-hosted deployment, PostgreSQL is the primary backup target.

A backup shall be sufficient to restore:

- accounts,
- repository inventory,
- calculated statistics,
- AI assessments,
- configuration stored in database,
- job history/state where retained.

Secrets supplied through deployment configuration shall be backed up separately according to deployment practice.

Generated reports that can be regenerated need not necessarily be part of the primary backup strategy.

---

# 34. Performance Strategy

Version 1 performance should rely on:

1. incremental synchronisation,
2. background processing,
3. database indexes,
4. precomputed aggregates,
5. pagination,
6. cached/persisted AI classifications.

The architecture should not introduce a distributed cache pre-emptively.

Redis or another cache may be reconsidered if measurement shows a database or coordination bottleneck.

---

# 35. Scaling Path

Version 1 shall optimise for a single-host deployment but not prevent horizontal scaling.

A future deployment could evolve from:

```text
1 web
1 backend
1 worker
1 PostgreSQL
```

to:

```text
N web/backend instances
N workers
managed/high-availability PostgreSQL
external object storage
optional dedicated job broker/cache
```

The most important prerequisite is that:

- application processes remain stateless apart from PostgreSQL,
- jobs are persistently coordinated,
- user files do not depend on ephemeral container storage.

---

# 36. Why PostgreSQL Is Enough

The service has several data shapes:

| Data type | PostgreSQL approach |
|---|---|
| Users/accounts | relational tables |
| Repositories | relational tables |
| Contributions | relational tables |
| Monthly/yearly activity | aggregate tables |
| Commit size | numeric/statistical columns |
| Technologies | relational many-to-many |
| Project categories | relational many-to-many |
| Provider-specific metadata | JSONB where useful |
| AI output | relational metadata + JSONB payload |
| Job queue | relational table |
| Reports | relational metadata/text |
| Search | PostgreSQL initially |

None of these create a compelling requirement for another database in version 1.

Adding MongoDB, Elasticsearch, Neo4j, InfluxDB/Timescale-specific architecture or a separate event store would increase:

- operational complexity,
- backup complexity,
- Docker Compose complexity,
- development effort,
- data consistency concerns.

without clear version 1 benefit.

Standard PostgreSQL should therefore be the only database.

A PostgreSQL extension or specialised secondary store can be evaluated later if actual usage demonstrates a need.

---

# 37. Technology Decisions

## ADR-01 – React + TypeScript frontend

**Decision:** Use React with TypeScript.

**Reasoning:**

- well suited to interactive dashboards,
- strong component ecosystem,
- good responsive UI support,
- TypeScript improves API/domain contract safety,
- no need for server-side rendered public profile pages in version 1.

## ADR-02 – Vite frontend build

**Decision:** Use Vite for frontend development/build.

**Reasoning:**

- simple SPA build model,
- fast development workflow,
- produces static assets suitable for Nginx.

## ADR-03 – Quarkus + Java backend

**Decision:** Use Quarkus and Java.

**Reasoning:**

- suitable for REST APIs,
- good container operation,
- strong PostgreSQL/JPA integration,
- supports background and scheduled processing,
- appropriate for a modular backend.

## ADR-04 – Hibernate ORM / JPA persistence

**Decision:** Use Jakarta Persistence with Hibernate ORM.

**Reasoning:**

- service domain is predominantly relational,
- supports clear transactional boundaries,
- established PostgreSQL support.

## ADR-05 – Flyway migrations

**Decision:** Use Flyway as schema migration authority.

**Reasoning:**

- repeatable version-controlled database evolution,
- avoids relying on production ORM auto-schema changes.

## ADR-06 – PostgreSQL only in version 1

**Decision:** Use PostgreSQL as the only database.

**Reasoning:**

- relational core model,
- adequate time-series aggregation,
- JSONB handles limited flexible data,
- reduces operational complexity.

## ADR-07 – PostgreSQL-backed background jobs

**Decision:** Do not introduce a separate message broker in version 1.

**Reasoning:**

- expected workload is moderate,
- jobs require durable state,
- PostgreSQL is already required,
- worker scaling remains possible.

## ADR-08 – Nginx as web entry point

**Decision:** Use Nginx to serve frontend and proxy API.

**Reasoning:**

- single external origin,
- simple SPA hosting,
- straightforward Docker deployment,
- keeps backend and database unexposed.

## ADR-09 – Docker Compose reference deployment

**Decision:** Provide a complete Docker Compose deployment.

**Reasoning:**

- simple self-hosting,
- reproducible local deployment,
- contains all required runtime services.

## ADR-10 – Separate API and worker processes

**Decision:** Run interactive API and asynchronous analysis as separate processes/containers.

**Reasoning:**

- hundreds of repositories may require long processing,
- prevents ingestion from impacting interactive responsiveness,
- same codebase can still be used.

## ADR-11 – AI provider abstraction

**Decision:** AI integration shall not depend directly on one vendor in core domain code.

**Reasoning:**

- quota/cost/privacy can change,
- different providers may be appropriate for different deployments,
- service shall remain usable without AI.

## ADR-12 – GitHub provider abstraction

**Decision:** GitHub-specific implementation shall sit behind source-control provider interfaces.

**Reasoning:**

- GitLab or other providers can be added later,
- core analysis should operate on normalised domain data.

---

# 38. Explicitly Deferred Technologies

The following are not recommended for the initial architecture unless later evidence requires them:

- Kubernetes,
- Redis,
- Kafka,
- RabbitMQ,
- Elasticsearch/OpenSearch,
- MongoDB,
- Neo4j,
- dedicated time-series database,
- separate API gateway product,
- service mesh,
- multiple independently deployed domain microservices.

Docker Compose, PostgreSQL, Nginx and two Quarkus process roles are sufficient for the version 1 problem.

---

# 39. Future Architecture Extensions

## 39.1 GitLab and other providers

Add provider adapters without altering the core repository/contribution model.

## 39.2 Multiple identity providers

Google/Apple authentication can be added by separating service identity from source-control connections.

## 39.3 Public snapshots

If introduced, public snapshot data should be an explicit immutable or versioned publication projection rather than exposing the user's live private profile.

## 39.4 Object storage

Introduce S3-compatible storage if generated report/file volume warrants it.

## 39.5 Dedicated job infrastructure

Introduce a broker only if PostgreSQL job coordination becomes a measured limitation.

## 39.6 Search platform

Introduce dedicated search if public cross-user or large semantic/full-text search becomes a real requirement.

## 39.7 Advanced analytics store

Consider specialised analytical/time-series storage only if dataset scale or query latency demonstrates PostgreSQL is insufficient.

---

# 40. Recommended Version 1 Repository Structure

A pragmatic monorepo is recommended:

```text
developer-activity-analytics/
|
+-- frontend/
|   +-- src/
|   +-- package.json
|   +-- vite.config.*
|
+-- backend/
|   +-- src/main/
|   +-- src/test/
|   +-- pom.xml
|
+-- deploy/
|   +-- compose.yaml
|   +-- nginx/
|   +-- env.example
|
+-- docs/
|   +-- functional-specification.md
|   +-- architecture-specification.md
|
+-- README.md
```

A monorepo keeps version 1:

- easy to clone,
- easy to build,
- easy to version,
- easy to package,
- easy to run with one Compose file.

There is no version 1 architectural benefit in splitting frontend, backend and deployment files into separate repositories.

---

# 41. Recommended Runtime Flow

## Login and initial import

```text
Browser
   |
   v
Nginx
   |
   v
Quarkus API ----> GitHub authentication
   |
   v
PostgreSQL
   |
   +---- create import jobs
            |
            v
          Worker
            |
            +----> GitHub APIs
            |
            +----> AI provider (when allowed)
            |
            v
         PostgreSQL
```

## Dashboard

```text
Browser
   |
   v
Nginx
   |
   v
Quarkus API
   |
   v
Precomputed PostgreSQL aggregates
   |
   v
JSON response
```

## External GPT

```text
GPT
 |
 | scoped credential
 v
Nginx
 |
 v
Quarkus API
 |
 +----> user profile/aggregates/evidence
 |
 <---- structured response

GPT analysis
 |
 v
POST /api/me/ai-assessments
 |
 v
PostgreSQL
```

---

# 42. Recommended Initial Docker Compose Experience

The reference installation should aim for a workflow conceptually similar to:

```text
1. Copy environment configuration template
2. Configure GitHub application credentials
3. Optionally configure AI provider credentials
4. Start Docker Compose
5. Open the service URL
6. Sign in with GitHub
```

The user running the service should not need to install:

- Java,
- Node.js,
- PostgreSQL,
- Nginx,

on the host system.

Docker and Docker Compose should be sufficient runtime prerequisites.

---

# 43. Final Architectural Recommendation

The proposed React/TypeScript + Quarkus/Java/JPA/Flyway/PostgreSQL stack is a good fit for this service and does not need to be replaced by a more specialised technology architecture.

The recommended version 1 architecture is deliberately compact:

```text
React/TypeScript
       |
     Nginx
       |
   Quarkus API
       |
   PostgreSQL
       ^
       |
 Quarkus Worker

External integrations:
- GitHub
- optional AI provider
- optional external GPT
```

The most important addition beyond the initially proposed frontend/backend/database structure is the dedicated background worker process.

That worker is justified because historical GitHub ingestion and analysis is long-running and rate-limit sensitive, while the interactive application should remain responsive.

PostgreSQL should remain the sole database in version 1. It is suitable for the relational domain model, time-based aggregates, job state and flexible JSON metadata required by the service.

Nginx should be the standard external entry point, serving the React application and proxying API requests. Backend, worker and database should remain internal to the Docker Compose network.

This architecture supports a small self-hosted installation while leaving clear extension points for additional Git providers, additional authentication methods, other AI providers and larger deployments.

---

# 44. Reference Documentation

The following official documentation informed the architecture choices:

- Quarkus – Hibernate ORM with Panache:  
  https://quarkus.io/guides/hibernate-orm-panache

- Quarkus – Hibernate ORM and Jakarta Persistence:  
  https://quarkus.io/guides/hibernate-orm

- Docker – Compose networking:  
  https://docs.docker.com/compose/how-tos/networking/

- Docker – Compose services:  
  https://docs.docker.com/reference/compose-file/services/

- PostgreSQL documentation:  
  https://www.postgresql.org/docs/
