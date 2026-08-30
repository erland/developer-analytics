# Development Plan – Developer Analytics

**Version:** 1.0  
**Status:** Proposed implementation plan  
**Target repository:** `developer-analytics`  
**Primary implementation approach:** React/TypeScript frontend, Quarkus/Java backend, PostgreSQL, Docker Compose, Nginx, GitHub Actions, GHCR

---

# 1. Purpose of This Plan

This document describes, step by step, how an LLM should build the Developer Analytics service defined by the functional and architecture specifications.

The plan is intentionally ordered so that:

- the repository becomes buildable very early,
- `.gitignore` and repository hygiene are established before generated files accumulate,
- automated tests and GitHub Actions are introduced before substantial functionality,
- Docker packaging is continuously verified,
- GHCR publication works before the application grows large,
- each later feature is added on top of a functioning end-to-end baseline,
- large GitHub accounts with hundreds of repositories are treated as a normal use case,
- privacy and private-repository handling are built into the design rather than added afterward.

The implementation should proceed one numbered step at a time. After each step, the repository should remain in a coherent, buildable state.

---

# 2. Working Principles for the LLM

For every implementation step, the LLM should:

1. inspect the current repository before changing it,
2. preserve working functionality,
3. make the smallest coherent change that completes the step,
4. add or update tests together with functionality,
5. run relevant tests locally when possible,
6. run build/lint/static validation relevant to changed components,
7. update documentation when behaviour or setup changes,
8. avoid introducing dependencies not justified by the specifications,
9. avoid leaving generated, temporary or local-development files in the repository,
10. ensure Docker and CI continue to work,
11. clearly document any deviation from this plan.

The LLM should not skip ahead simply because later features are more interesting.

---

# 3. Target Repository Structure

The plan should converge toward a monorepo similar to:

```text
developer-analytics/
|
+-- .github/
|   +-- workflows/
|
+-- frontend/
|   +-- src/
|   +-- public/
|   +-- package.json
|   +-- tsconfig.json
|   +-- vite.config.*
|   +-- Dockerfile
|
+-- backend/
|   +-- src/
|   |   +-- main/
|   |   +-- test/
|   +-- pom.xml
|   +-- Dockerfile
|
+-- deploy/
|   +-- compose.yaml
|   +-- nginx/
|   |   +-- nginx.conf
|   +-- env.example
|
+-- docs/
|   +-- functional-specification.md
|   +-- architecture-specification.md
|   +-- development-plan.md
|
+-- scripts/
|
+-- .dockerignore
+-- .editorconfig
+-- .gitignore
+-- LICENSE
+-- README.md
```

The exact layout may evolve when implementation reveals a better local structure, but major changes should be justified.

---

# 4. GitHub Actions Strategy

The repository shall eventually contain at least two logical CI/CD workflows.

## 4.1 Validation workflow

Triggered for:

- pull requests,
- pushes to `main`,
- optionally manual execution.

It shall verify:

- frontend dependency installation,
- frontend lint/type check,
- frontend tests,
- frontend production build,
- backend compilation,
- backend tests,
- database/migration-related integration tests,
- Dockerfile validity/build,
- Docker Compose configuration validity,
- selected end-to-end smoke tests.

Pull-request validation shall build Docker images but shall not publish production image tags.

## 4.2 Container publication workflow

Triggered primarily by:

- Git tags/releases matching the project's release convention,
- optionally manual workflow dispatch.

It shall:

- authenticate to GitHub Container Registry,
- build relevant application images,
- assign deterministic image tags,
- publish to `ghcr.io`,
- add OCI metadata,
- produce provenance/SBOM where practical,
- verify that all expected images were published.

Expected version 1 application images:

```text
ghcr.io/<owner>/developer-analytics-web
ghcr.io/<owner>/developer-analytics-backend
```

The worker should normally use the same backend image with a different runtime command/mode rather than requiring a third independently built image.

PostgreSQL shall use the standard upstream image and shall not be republished.

---

# 5. Image Tagging Strategy

The implementation should establish a tag strategy early.

Recommended release examples:

```text
ghcr.io/<owner>/developer-analytics-web:1.0.0
ghcr.io/<owner>/developer-analytics-web:1.0
ghcr.io/<owner>/developer-analytics-web:latest

ghcr.io/<owner>/developer-analytics-backend:1.0.0
ghcr.io/<owner>/developer-analytics-backend:1.0
ghcr.io/<owner>/developer-analytics-backend:latest
```

Development builds may additionally use:

```text
sha-<git-sha>
```

The exact release tag convention should be documented before the first published release.

---

# 6. Implementation Phases

The implementation is divided into the following phases:

1. Repository foundation
2. Minimal buildable applications
3. CI/CD and Docker foundation
4. Core backend domain and persistence
5. Authentication and user isolation
6. GitHub connection and repository inventory
7. Background processing and synchronisation
8. Activity and commit analytics
9. Technology detection
10. Project classification and significance
11. Dashboard and responsive user experience
12. Private repository support
13. AI-assisted analysis
14. External GPT API
15. Reports and export
16. Data lifecycle, privacy and operational hardening
17. Full-system verification and version 1 release

---

# Phase 1 – Repository Foundation

# Step 1 – Create the Initial Repository Skeleton

Create the basic monorepo layout:

```text
frontend/
backend/
deploy/
docs/
scripts/
.github/workflows/
```

Add:

- `README.md`,
- `.editorconfig`,
- placeholder documentation locations,
- license file appropriate for the project.

The README should initially explain:

- project purpose,
- current development status,
- planned architecture at a high level,
- repository structure.

Do not scaffold full applications yet.

### Acceptance criteria

- repository structure is clear,
- no generated files are committed,
- README describes the project sufficiently for a new contributor.

---

# Step 2 – Create `.gitignore` Immediately

Create a comprehensive root `.gitignore` before installing/building frontend or backend dependencies.

It shall cover at minimum:

## Operating systems/editors

```text
.DS_Store
Thumbs.db
.idea/
.vscode/
*.swp
```

Repository-specific shared IDE configuration may later be explicitly re-included if desired.

## Java/Maven

```text
target/
*.class
*.log
```

## Node/Vite

```text
node_modules/
dist/
.vite/
coverage/
```

## Local environment/secrets

```text
.env
.env.*
!.env.example
*.local
```

Ensure example environment files remain trackable.

## Test/runtime artefacts

```text
test-results/
playwright-report/
```

## Temporary/generated files

```text
tmp/
temp/
*.tmp
```

## Local database/volumes if stored in project tree

Ignore any local volume/data directories that the Compose configuration might optionally use.

### Acceptance criteria

- frontend dependency installation does not dirty Git with generated content,
- Maven builds do not dirty Git,
- local environment files containing secrets are ignored,
- example configuration files can still be committed.

---

# Step 3 – Add `.dockerignore`

Create appropriate Docker ignore rules.

Avoid sending:

- `.git`,
- local IDE files,
- `node_modules`,
- frontend build output when rebuilt inside Docker,
- Maven target output when rebuilt inside Docker,
- local environment secrets,
- test output,

into unnecessary Docker build contexts.

Prefer component-specific `.dockerignore` files if this substantially reduces build context.

---

# Step 4 – Store the Specifications in the Repository

Add to `docs/`:

- functional specification,
- architecture specification,
- this development plan.

Add a short documentation index to README.

The specifications should become the baseline against which future implementation decisions are checked.

---

# Phase 2 – Minimal Buildable Applications

# Step 5 – Scaffold the React/TypeScript Frontend

Create a React + TypeScript + Vite frontend.

Initial application should contain:

- application shell,
- placeholder home page,
- basic error boundary,
- minimal responsive layout,
- test setup.

Do not implement real dashboard functionality yet.

Add scripts for at least:

```text
dev
build
test
lint
typecheck
```

Use a predictable dependency lock file and commit it.

### Acceptance criteria

- frontend starts locally,
- production build succeeds,
- tests run,
- lint/type check succeeds.

---

# Step 6 – Scaffold the Quarkus Backend

Create the backend using Java + Quarkus.

Include only the extensions needed for the early baseline, such as:

- Quarkus REST,
- JSON serialization,
- Hibernate ORM,
- PostgreSQL driver,
- Flyway,
- health endpoints,
- testing support.

Create an initial API:

```text
GET /api/health/application
```

or equivalent application-level smoke endpoint.

Do not create GitHub or AI integration yet.

### Acceptance criteria

- Maven build succeeds,
- Quarkus tests run,
- smoke endpoint works,
- application can be configured through environment variables.

---

# Step 7 – Add PostgreSQL and the First Flyway Migration

Create the first schema migration.

Initially define only the smallest required schema, for example:

```text
application_metadata
```

or the initial user/account table if identity modelling is already clear.

Do not build the complete final schema at once.

Configure:

- PostgreSQL connection,
- Flyway startup migration,
- test database strategy.

### Acceptance criteria

- fresh database starts successfully,
- migrations execute automatically in controlled application startup/test flow,
- ORM schema auto-creation is not relied upon as the production schema authority.

---

# Step 8 – Create the Initial Nginx Configuration

Configure Nginx to:

- serve frontend static files,
- use SPA route fallback,
- proxy `/api/` to backend,
- expose one external HTTP endpoint.

Add basic appropriate headers and compression.

The frontend shall call the API using same-origin `/api/...` URLs rather than hard-coded backend hosts.

---

# Step 9 – Create the First Dockerfiles

Create production-oriented Dockerfiles for:

## Frontend/web image

Multi-stage build:

1. Node build stage,
2. compile React application,
3. copy static assets into Nginx runtime image.

## Backend image

Multi-stage or otherwise optimised build:

1. Maven/Java build,
2. minimal Quarkus runtime image.

The backend image must support both API and worker runtime roles later.

### Acceptance criteria

- both images build independently,
- no source-development dependencies are unnecessarily present in final images,
- images run without host-installed Java or Node.

---

# Step 10 – Create the Initial Docker Compose Stack

Create `deploy/compose.yaml` containing:

```text
web
backend
db
```

The worker will be added shortly afterward.

Requirements:

- only web/Nginx exposed to the host by default,
- PostgreSQL uses persistent storage,
- backend connects using internal Docker DNS,
- web proxies API internally,
- health checks are defined where useful,
- configuration comes from environment variables.

Add `deploy/env.example`.

### Acceptance criteria

A new developer can conceptually:

```text
copy env.example
docker compose up
```

and reach the React page plus backend smoke API.

---

# Phase 3 – CI/CD and Docker Foundation

# Step 11 – Add the First GitHub Actions Validation Workflow

Create `.github/workflows/ci.yml`.

Trigger on:

- pull request,
- push to `main`,
- workflow dispatch.

Initial jobs:

## Frontend

- checkout,
- install pinned Node version,
- install dependencies from lockfile,
- lint,
- typecheck,
- test,
- production build.

## Backend

- checkout,
- install required Java version,
- Maven verification,
- unit/integration tests.

Prefer dependency caching supported by standard setup actions.

### Acceptance criteria

- a deliberately broken frontend test fails CI,
- a deliberately broken backend test fails CI,
- successful repository state gives green CI.

---

# Step 12 – Add Docker Build Validation to CI

Extend CI so pull requests validate:

- frontend/web Docker image build,
- backend Docker image build,
- Compose syntax/configuration.

Do not push images on pull requests.

Use Docker Buildx-based builds.

Where supported, enable build configuration validation/checks.

### Acceptance criteria

- broken Dockerfile fails CI,
- invalid Compose configuration fails CI,
- images are buildable before application features proceed.

---

# Step 13 – Add GHCR Publication Workflow

Create `.github/workflows/publish-images.yml`.

The workflow shall:

- run on release tag/release event selected by the project,
- obtain package write permission through GitHub Actions,
- authenticate to `ghcr.io`,
- use Docker metadata generation,
- build frontend and backend images,
- publish semantically useful tags,
- attach OCI labels,
- publish SHA traceability.

Use GitHub's workflow token where sufficient rather than storing unnecessary long-lived registry passwords.

### Acceptance criteria

A test/pre-release can publish:

```text
ghcr.io/<owner>/developer-analytics-web:<version>
ghcr.io/<owner>/developer-analytics-backend:<version>
```

---

# Step 14 – Add SBOM and Provenance to Published Images

Enable available build provenance and SBOM generation for release images.

Document:

- what metadata is produced,
- where it is visible,
- any permissions required by the workflow.

This should be introduced early so later releases automatically inherit the supply-chain metadata.

---

# Step 15 – Add CI Status Documentation

Update README with:

- CI workflow purpose,
- image publication behaviour,
- GHCR package names,
- release tagging convention,
- how PR builds differ from release builds.

At this point the repository foundation should be considered complete.

---

# Phase 4 – Core Backend Domain and Persistence

# Step 16 – Define Core Domain Identifiers

Introduce internal immutable identifiers and provider-neutral concepts for:

- User,
- ProviderIdentity,
- ProviderConnection,
- Repository.

Do not use GitHub usernames, repository names or URLs as primary database identities.

Add database migrations and persistence tests.

---

# Step 17 – Add Repository Ownership and Visibility Model

Represent at minimum:

- provider,
- provider repository ID,
- owner,
- repository name,
- public/private visibility,
- fork status,
- archived status,
- user-owned,
- organisation-owned,
- externally owned,
- first/last known activity,
- synchronisation state.

Add repository query services.

---

# Step 18 – Add Contribution Domain Model

Introduce normalised concepts for available contribution activity:

- commit,
- pull request,
- review,
- issue,
- release/maintenance activity where appropriate.

Avoid storing complete remote payloads as the primary domain model.

Provider-specific metadata may be retained selectively in JSONB where useful.

---

# Step 19 – Add Time-Based Aggregate Model

Create aggregate tables for dashboard use, initially monthly.

Example concepts:

```text
user_activity_month
repository_activity_month
technology_activity_month
```

Include fields needed for:

- commits,
- additions,
- deletions,
- changed lines,
- active repositories,
- PRs,
- reviews,
- issues.

Add yearly aggregation as query/materialisation logic rather than necessarily duplicating every metric immediately.

---

# Step 20 – Add Background Job Model

Create persistent PostgreSQL-backed jobs.

Support:

- type,
- owner/user,
- state,
- payload,
- priority,
- attempts,
- next execution time,
- progress,
- error,
- timestamps.

States should include:

```text
QUEUED
RUNNING
COMPLETED
FAILED
WAITING
CANCELLED
```

Implement safe atomic worker job claiming.

---

# Step 21 – Add Worker Runtime Mode

Run a second container/process from the backend codebase in worker mode.

Update Compose:

```text
web
backend
worker
db
```

The worker:

- must not expose a public port,
- reads jobs from PostgreSQL,
- handles retries,
- reports progress.

Add tests proving API and worker do not process the same job concurrently.

---

# Phase 5 – Authentication and User Isolation

# Step 22 – Implement GitHub Sign-In

Implement GitHub-based user authentication.

Separate:

- service user identity,
- GitHub provider identity,
- provider permissions.

The browser must never need to store long-lived GitHub credentials.

Add logout and session expiry handling.

---

# Step 23 – Enforce `/me`-Style User Scoping

Build user-facing endpoints around authenticated context.

Examples:

```text
GET /api/me
GET /api/me/repositories
GET /api/me/activity
```

Avoid APIs where the client supplies another user's internal identifier for normal access.

Add security tests proving one account cannot access another account's data.

---

# Step 24 – Add Account and Connection Management

Implement:

- connected GitHub identity display,
- connection status,
- last synchronisation,
- disconnect action,
- data deletion action or initial deletion workflow.

The UI shall clearly distinguish disconnecting GitHub from deleting analysed data.

---

# Phase 6 – GitHub Repository Inventory

# Step 25 – Implement GitHub Provider Adapter

Introduce a provider-neutral interface and a GitHub implementation.

Initial GitHub operations:

- current identity,
- repository list,
- repository metadata,
- language information,
- relevant contribution discovery,
- rate-limit state.

Keep GitHub DTOs inside the integration layer.

---

# Step 26 – Implement Initial Repository Discovery

After authentication/connection, enqueue repository discovery.

Store repository inventory incrementally.

The user should be able to see:

```text
repositories discovered
repositories processed
repositories pending
repositories failed
```

Support accounts with 200+ repositories without loading everything in one request.

---

# Step 27 – Implement Incremental Repository Synchronisation

Persist enough provider state to avoid complete rescans.

Use:

- provider update timestamps,
- last successful sync,
- cursors/markers where appropriate,
- analysis version.

Unchanged repositories should skip unnecessary downstream analysis.

---

# Step 28 – Add API Rate-Limit Handling

Treat GitHub rate limits as expected operating conditions.

Implement:

- response interpretation,
- retry scheduling,
- WAITING state,
- next permitted execution,
- visible synchronisation status.

Do not spin or repeatedly retry immediately.

---

# Phase 7 – Activity and Commit Analytics

# Step 29 – Collect Commit Activity

Collect the factual data required for:

- commits per month,
- commits per year,
- repository activity periods,
- total observed commits.

Validate author/user matching carefully to reduce incorrect attribution.

---

# Step 30 – Collect Commit-Size Statistics

Where available, collect:

- additions,
- deletions,
- changed lines.

Calculate:

- average commit size,
- median commit size,
- distribution/outlier indicators.

Do not send raw diffs to the frontend merely to calculate charts.

---

# Step 31 – Implement Activity Aggregation

Build reproducible aggregation jobs.

The same input shall produce consistent monthly aggregates.

Add tests for:

- month boundaries,
- year boundaries,
- empty months,
- outlier commits,
- public/private separation.

---

# Step 32 – Add Pull Request, Review and Issue Statistics

Where GitHub data supports reliable collection, add:

- PR count,
- merged PR count,
- review activity,
- issue activity.

Keep unavailable/incomplete data visibly distinct from zero.

---

# Phase 8 – Technology Detection

# Step 33 – Define the Technology Catalogue

Create normalised technology concepts.

Examples:

```text
Java
TypeScript
React
Quarkus
PostgreSQL
Docker
GitHub Actions
Kubernetes
Swift
```

Support:

- aliases,
- technology type/category,
- canonical display name.

Do not create a fixed enum that requires code deployment for every new technology.

---

# Step 34 – Implement Language-Based Evidence

Use GitHub repository language information as one evidence source.

Store evidence rather than directly declaring expertise.

Example:

```text
repository X
technology Java
evidence type: LANGUAGE
strength: observed
```

---

# Step 35 – Implement File/Manifest-Based Detection

Detect technologies from relevant project files and metadata.

Examples:

- `pom.xml`,
- `package.json`,
- `Dockerfile`,
- Compose files,
- GitHub workflow files,
- Kubernetes manifests,
- Helm charts,
- Terraform,
- Swift package files,
- Flyway structures.

Use deterministic parsers/rules where practical.

---

# Step 36 – Calculate Technology Evidence Strength

Create a transparent evidence model based on signals such as:

- number of projects,
- contribution depth,
- duration,
- recency,
- multiple independent evidence types.

Use labels such as:

```text
STRONG
MODERATE
LIMITED
EXPOSURE
```

Avoid claiming formal skill proficiency.

---

# Step 37 – Build Technology Timeline

Aggregate:

- first observed use,
- latest observed use,
- projects per year,
- activity per technology,
- public/private evidence.

Expose this through API endpoints ready for charts.

---

# Phase 9 – Project Classification and Significance

# Step 38 – Define Project Category Taxonomy

Create a data-driven taxonomy including categories such as:

- web application,
- mobile application,
- game,
- backend service,
- API,
- library,
- framework,
- developer tooling,
- automation,
- infrastructure/platform,
- DevOps/CI/CD,
- security,
- observability,
- data/database,
- integration,
- AI/ML,
- architecture/modelling,
- documentation/education,
- experiment/prototype.

Allow multiple categories per project.

---

# Step 39 – Implement Deterministic Classification Signals

Use:

- repository topics,
- detected technologies,
- known files,
- project metadata,
- repository structure.

Generate classification evidence even before AI is introduced.

---

# Step 40 – Implement Project Significance Model

Keep two concepts separate:

## Project significance

Examples:

- popularity,
- contributors,
- longevity,
- organisation/ecosystem,
- activity.

## User involvement

Examples:

- contributions,
- duration,
- recency,
- relative contribution.

Create an explainable ranking rather than one opaque global score.

---

# Step 41 – Identify Significant External Projects

Create queries/views returning external projects where either:

- project significance is high,
- user involvement is high,
- both are high.

Expose explanation/evidence in API responses.

---

# Phase 10 – Dashboard and Responsive UX

# Step 42 – Implement the Authenticated Application Shell

Add:

- responsive navigation,
- authenticated user state,
- loading/error states,
- data freshness indicator,
- mobile navigation.

Primary sections:

```text
Overview
Activity
Projects
Technologies
Project types
Contributions
AI insights
Reports
Privacy/data sources
Account
```

---

# Step 43 – Implement Overview Dashboard

Display:

- repositories analysed,
- own/external counts,
- public/private counts,
- commits,
- activity period,
- active projects,
- key technologies,
- project categories,
- significant projects.

Ensure all cards work on phone-sized screens.

---

# Step 44 – Implement Activity Views

Add charts/views for:

- commits per year,
- commits per month,
- active projects,
- average commit size,
- median commit size,
- additions/deletions.

Provide period filters.

---

# Step 45 – Implement Project Inventory

Support:

- pagination,
- search,
- filters,
- own/external,
- public/private,
- active/inactive,
- category,
- technology.

Use responsive cards or adaptive tables on mobile.

---

# Step 46 – Implement Project Detail

Show:

- metadata,
- activity timeline,
- technologies/evidence,
- categories,
- user involvement,
- significance,
- synchronisation state.

---

# Step 47 – Implement Technology Views

Add:

- technology list,
- evidence level,
- number of projects,
- first/latest use,
- timeline,
- representative projects.

---

# Step 48 – Implement Project-Type Views

Show:

- project counts per category,
- activity per category,
- category evolution over time,
- representative projects.

---

# Phase 11 – Private Repository Support

# Step 49 – Add Explicit Private Repository Authorisation

Extend GitHub connection to support authorised private repositories.

User must consciously choose/authorise access.

Do not silently widen permissions.

---

# Step 50 – Add Private Data Marking Throughout the Model

Every derived statistic/assessment that can contain private evidence shall retain enough provenance to determine:

- public only,
- includes private,
- private aggregate.

Add tests around privacy propagation.

---

# Step 51 – Add Private Repository Selection UI

Allow:

- include/exclude private repositories,
- inspect which repositories are authorised,
- refresh permissions,
- remove private repositories from analysis.

---

# Step 52 – Add Private Export Controls

Implement options to:

- exclude private data,
- include aggregated private data,
- include full private project detail,
- hide private repository names.

No export shall infer these settings implicitly.

---

# Phase 12 – AI-Assisted Analysis

# Step 53 – Introduce AI Provider Abstraction

Create an internal interface independent of vendor.

Initial operations may include:

```text
classifyProject
summariseProject
normaliseTechnologies
inferRoles
summariseTechnologyHistory
```

Core application functionality must continue without a configured AI provider.

---

# Step 54 – Add Gemini as the Initial Optional Provider

Implement Gemini behind the abstraction.

Configuration shall come from secret environment configuration.

Use structured outputs.

Log:

- request type,
- success/failure,
- usage metadata where appropriate,

but not sensitive prompt data unnecessarily.

---

# Step 55 – Add AI Privacy Policy Enforcement

Before any AI request, evaluate:

- source visibility,
- user consent,
- configured provider policy.

Private repository content shall not automatically be sent to an external AI service.

Support at least:

```text
PUBLIC_ONLY
PRIVATE_METADATA_ALLOWED
PRIVATE_AI_DISABLED
```

or equivalent policy semantics.

---

# Step 56 – Add Project AI Classification

Use AI only where it improves deterministic classification.

Store:

- classification,
- confidence,
- explanation,
- analysis version,
- creation time,
- provider/model metadata as appropriate.

Persist and reuse results for unchanged repositories.

---

# Step 57 – Add User-Level AI Insights

Generate optional analyses such as:

- likely roles,
- technical focus,
- breadth/depth observations,
- technology evolution summary,
- open-source engagement summary.

Clearly label them as AI-generated.

---

# Step 58 – Add User Correction Feedback

Allow the user to:

- reject an incorrect project category,
- suppress an incorrect technology inference,
- exclude a project from AI profile conclusions.

Keep source facts intact.

---

# Phase 13 – External GPT Integration

# Step 59 – Define External Analysis API Contract

Define and document endpoints such as:

```text
GET /api/me/profile
GET /api/me/projects
GET /api/me/activity
GET /api/me/technologies
GET /api/me/project-types
GET /api/me/contributions
GET /api/me/evidence
```

Return compact structured data suitable for LLM consumption rather than frontend-specific payloads.

---

# Step 60 – Add External Client Authentication

Create user-controlled credentials/tokens for external GPT clients.

Requirements:

- scoped,
- revocable,
- user-specific,
- separate from browser session,
- separate from GitHub provider credential.

---

# Step 61 – Add Privacy Scopes for External AI

Support scopes such as:

```text
PUBLIC_ONLY
PUBLIC_PLUS_PRIVATE_AGGREGATES
FULL_AUTHORISED_ANALYSIS
```

The API must enforce scope server-side.

---

# Step 62 – Add Returned AI Assessment API

Implement:

```text
POST /api/me/ai-assessments
GET /api/me/ai-assessments
DELETE /api/me/ai-assessments/{id}
```

Store:

- analysis type,
- source/client,
- timestamp,
- data scope,
- content,
- private-data indication.

---

# Step 63 – Publish GPT/API Documentation

Create:

- OpenAPI documentation,
- example calls,
- authentication instructions,
- privacy scope explanation,
- sample Custom GPT Action schema if appropriate.

---

# Phase 14 – Reports and Export

# Step 64 – Create a Canonical Report Model

Separate report content from output format.

Build a report model containing:

- summary,
- period,
- data coverage,
- project categories,
- technology analysis,
- activity,
- significant projects,
- role/AI assessments,
- methodology,
- privacy scope.

---

# Step 65 – Implement Markdown Export

Markdown is the first canonical export.

Support at least:

- public OSS report,
- full developer report,
- technology profile,
- activity report.

Include methodology and data-coverage sections.

---

# Step 66 – Implement Report Privacy Preview

Before generating a report, show:

- private repositories included?,
- private names included?,
- AI assessments included?,
- public-only or private scope?,
- analysed time range.

Require explicit generation action.

---

# Step 67 – Implement PDF Export

Render PDF from the same report model/content used for Markdown.

Do not maintain an independently authored PDF report path.

Ensure:

- charts fit,
- wide tables wrap/transform appropriately,
- mobile dashboard layout does not dictate PDF layout,
- private-data markings remain visible.

---

# Step 68 – Add Export Tests

Test:

- public-only report cannot contain private repository names,
- aggregated-private report masks names,
- full-private report contains only authorised private data,
- Markdown/PDF have matching core content.

---

# Phase 15 – Data Lifecycle and Operational Hardening

# Step 69 – Implement Disconnect Workflow Fully

On GitHub disconnect:

- stop future synchronisation,
- revoke/forget provider access where appropriate,
- preserve or remove analysed data according to explicit user choice.

---

# Step 70 – Implement User Data Deletion

Implement a reliable deletion workflow for:

- provider connections,
- repositories,
- contributions,
- aggregates,
- technology evidence,
- classifications,
- AI assessments,
- reports,
- background jobs.

Test deletion using realistic relational data.

---

# Step 71 – Add Synchronisation Recovery

Handle:

- worker restart,
- backend restart,
- temporary GitHub errors,
- AI outage,
- interrupted jobs,
- lost repository permission.

Jobs must be safely retriable.

---

# Step 72 – Add Structured Logging and Correlation IDs

Provide:

- request correlation IDs,
- background job IDs,
- synchronisation IDs,
- structured error logs.

Ensure:

- tokens,
- credentials,
- private source content,

are not logged.

---

# Step 73 – Add Health and Readiness Checks

Expose health/readiness for:

- backend process,
- database connectivity,
- migration readiness,
- worker availability/status where meaningful.

Do not mark the entire service unhealthy solely because optional Gemini is temporarily unavailable.

---

# Step 74 – Add Database Backup/Restore Documentation

Document self-hosted backup and restore for PostgreSQL.

Test that a backup can restore:

- account,
- repository inventory,
- aggregates,
- AI analysis metadata.

---

# Phase 16 – Testing and Quality Expansion

# Step 75 – Establish Backend Test Layers

Maintain:

- unit tests,
- persistence tests,
- GitHub adapter tests with mocked/recorded responses as appropriate,
- authorisation tests,
- worker/job tests,
- privacy tests.

---

# Step 76 – Establish Frontend Test Layers

Maintain:

- component tests,
- page/feature tests,
- responsive behaviour tests where practical,
- API error-state tests,
- privacy indicator tests.

---

# Step 77 – Add End-to-End Smoke Tests

Create a test environment in Compose or CI that verifies:

```text
web -> nginx -> backend -> PostgreSQL
```

At minimum:

- frontend loads,
- API responds through Nginx,
- database migration succeeds,
- representative authenticated flow can be tested with a suitable test strategy.

Do not require live GitHub calls for every CI run.

---

# Step 78 – Add Migration Verification

CI should detect:

- broken Flyway migrations,
- migration order problems,
- fresh-database startup failures.

Optionally also test upgrading from the most recent released schema once releases exist.

---

# Step 79 – Add Container Runtime Smoke Tests

After building images in CI:

- start the Compose stack using built images,
- wait for health,
- call application endpoint,
- verify frontend/static response,
- shut down cleanly.

This ensures Docker images are more than syntactically buildable.

---

# Step 80 – Add Basic Security Checks

Introduce pragmatic automated checks such as:

- dependency vulnerability scanning where available,
- container/image scanning where appropriate,
- secret scanning through GitHub capabilities,
- frontend/backend dependency update automation.

Avoid introducing a large security toolchain that makes basic development unmanageable.

---

# Phase 17 – Release and Distribution

# Step 81 – Finalise Production Compose File

Ensure reference Compose deployment can use GHCR images rather than requiring source builds.

A release deployment should resemble:

```text
web image from GHCR
backend image from GHCR
worker using backend GHCR image
PostgreSQL upstream image
```

Provide version selection through environment/configuration.

---

# Step 82 – Add Release Compose Example

Provide a Compose file or documented mode appropriate for end users.

The user should only need:

- Docker,
- Docker Compose,
- GitHub app configuration,
- required environment values,
- optional Gemini configuration.

Host installation of Java/Node/PostgreSQL/Nginx must not be required.

---

# Step 83 – Verify GHCR Package Permissions

Test installation from a clean environment.

Confirm:

- images can be pulled as intended,
- visibility is correct,
- version tags are correct,
- worker can use backend image,
- no repository secrets are embedded in image layers.

---

# Step 84 – Add Automated Release Verification

After image publication:

- pull published image tags,
- start a minimal Compose deployment using published images,
- perform smoke checks.

Fail the release workflow if images cannot actually be started.

---

# Step 85 – Produce Version 1 Installation Documentation

README/docs shall describe:

1. prerequisites,
2. GitHub application setup,
3. environment configuration,
4. optional AI configuration,
5. Compose startup,
6. first login,
7. private repository authorisation,
8. backup,
9. upgrade,
10. troubleshooting.

---

# Step 86 – Produce Version 1 Operator Documentation

Document:

- containers,
- volumes,
- ports,
- health checks,
- logs,
- jobs,
- synchronisation recovery,
- database backup,
- database migration behaviour,
- upgrading images.

---

# Step 87 – Perform Large-Account Acceptance Test

Use a realistic account with 200+ repositories.

Verify:

- repository discovery remains usable,
- initial analysis can progress incrementally,
- API remains responsive,
- dashboard works before all enrichment is complete,
- UI remains usable with hundreds of repositories,
- pagination/filtering works,
- background jobs recover after restart,
- rate limiting is handled cleanly.

This is a key version 1 acceptance test, not an optional performance exercise.

---

# Step 88 – Perform Privacy Acceptance Test

Create test scenarios containing:

- public repositories,
- private repositories,
- excluded private repositories,
- AI analysis,
- external GPT token,
- public report,
- private report.

Verify there is no path that accidentally exposes private information to:

- another user,
- public export,
- public API,
- insufficiently scoped GPT client.

---

# Step 89 – Perform Mobile Acceptance Test

Verify all primary flows on phone-sized viewport:

- login,
- overview,
- activity charts,
- projects,
- technologies,
- filters,
- AI insights,
- report configuration,
- account/privacy.

No essential feature may depend exclusively on a wide table.

---

# Step 90 – Version 1 Release Candidate

Before tagging the release candidate, require:

- all CI green,
- Docker runtime smoke test green,
- Flyway migration test green,
- privacy tests green,
- responsive/mobile checks complete,
- GHCR publication tested,
- clean installation tested,
- documentation current.

---

# Step 91 – Version 1 Release

Create the version 1 release/tag.

The release workflow shall:

- compile/test as required,
- build web image,
- build backend image,
- publish GHCR images,
- publish version tags,
- publish SBOM/provenance if configured,
- verify published containers.

Record release notes summarising:

- features,
- known limitations,
- required configuration,
- upgrade notes.

---

# 7. Recommended Early Milestones

The LLM should use these milestones as checkpoints.

## Milestone A – Repository is clean and buildable

Completed after approximately Steps 1–10.

Result:

- `.gitignore`,
- frontend,
- backend,
- PostgreSQL,
- Nginx,
- Docker Compose.

## Milestone B – CI/CD foundation works

Completed after approximately Steps 11–15.

Result:

- frontend/backend tests,
- Docker builds in CI,
- GHCR release publishing,
- SBOM/provenance.

This milestone should be reached before significant feature development.

## Milestone C – Core application architecture exists

Completed after approximately Steps 16–24.

Result:

- relational domain,
- background jobs,
- worker,
- authentication,
- user isolation.

## Milestone D – GitHub analytics baseline works

Completed after approximately Steps 25–32.

Result:

- repositories,
- sync,
- commits,
- commit sizes,
- contribution statistics.

At this point the product already has useful non-AI value.

## Milestone E – Experience analytics works

Completed after approximately Steps 33–48.

Result:

- technologies,
- project categories,
- significance,
- dashboards,
- mobile experience.

## Milestone F – Private + AI capability works

Completed after approximately Steps 49–63.

Result:

- private repositories,
- AI enrichment,
- external GPT API,
- returned AI assessments.

## Milestone G – Shareable product works

Completed after approximately Steps 64–74.

Result:

- Markdown/PDF reports,
- privacy controls,
- data deletion,
- operational resilience.

## Milestone H – Release-ready service

Completed after approximately Steps 75–91.

Result:

- comprehensive CI,
- runtime smoke tests,
- GHCR distribution,
- self-hosted installation,
- version 1 release.

---

# 8. CI Workflow Target State

At version 1, a pull request should conceptually execute:

```text
PR
 |
 +--> frontend
 |      + lint
 |      + typecheck
 |      + tests
 |      + production build
 |
 +--> backend
 |      + compile
 |      + unit tests
 |      + persistence/integration tests
 |      + Flyway validation
 |
 +--> containers
 |      + web Docker build
 |      + backend Docker build
 |      + Compose validation
 |
 +--> smoke
        + start stack
        + health checks
        + web/API smoke
```

The workflow should fail quickly where possible but retain enough independent jobs that a contributor can see whether the failure is frontend, backend or packaging-related.

---

# 9. Release Workflow Target State

A release should conceptually execute:

```text
Release/tag
    |
    +--> validate source
    |
    +--> build web image
    |       |
    |       +--> GHCR
    |
    +--> build backend image
    |       |
    |       +--> GHCR
    |
    +--> SBOM/provenance
    |
    +--> pull published images
    |
    +--> release Compose smoke test
    |
    +--> release complete
```

Published images shall be traceable back to:

- Git commit,
- repository,
- release version.

---

# 10. GitHub Actions Implementation Guidance

Use maintained official/established actions where practical.

At the time this plan was created, Docker's official GitHub Actions support:

- Buildx setup,
- metadata generation,
- registry login,
- image build/push,
- build validation,
- GitHub Actions build cache,
- SBOM/provenance generation.

Action versions should be checked against current official documentation when the workflow is implemented rather than copied blindly from this document.

For security-sensitive workflows, consider pinning third-party actions to immutable commit SHAs once the workflow stabilises.

Permissions shall follow least privilege.

Typical publication permissions may include only what is required, such as:

```text
contents: read
packages: write
id-token: write   # only where required for provenance/attestation
```

Do not grant broad repository write permissions to build jobs without need.

---

# 11. Testing Philosophy

The service shall not measure progress merely by the amount of implemented code.

Every major behaviour should have a corresponding verification layer.

Examples:

| Behaviour | Primary verification |
|---|---|
| Technology calculation | unit/domain tests |
| PostgreSQL persistence | integration tests |
| Flyway migration | fresh DB CI test |
| GitHub mapping | adapter tests |
| User isolation | security integration tests |
| Worker claiming | concurrency/integration tests |
| Dashboard component | frontend tests |
| Report privacy | backend/report tests |
| Docker packaging | CI Docker build |
| Full stack routing | Compose smoke test |
| 200+ repositories | large-account acceptance test |

---

# 12. Database Development Strategy

Do not design the complete final database schema before implementing the domain.

Instead:

1. add the minimum tables required for each step,
2. create a new Flyway migration,
3. preserve previous migrations,
4. add indexes based on concrete query paths,
5. measure before introducing specialised storage.

PostgreSQL remains the sole version 1 database unless implementation provides concrete evidence otherwise.

---

# 13. Dependency Discipline

Before adding a dependency, the LLM should ask:

1. Is this already solved adequately by the selected framework?
2. Is the dependency maintained?
3. Does it introduce a runtime service?
4. Does it complicate self-hosted Docker Compose deployment?
5. Is it needed for version 1?

Do not add:

- Redis,
- Kafka,
- Elasticsearch,
- MongoDB,
- graph database,
- dedicated time-series database,

without a demonstrated requirement that PostgreSQL and the existing architecture cannot reasonably satisfy.

---

# 14. Privacy Development Checklist

Whenever a new feature uses repository information, verify:

- Is the repository public or private?
- Is provenance retained?
- Can it affect an aggregate?
- Is the aggregate marked as containing private data?
- Can it be sent to AI?
- Can it be sent to an external GPT?
- Can it be exported?
- Can its repository name be revealed?
- Is deletion handled?

Privacy propagation should be part of domain logic and tests, not a frontend-only concern.

---

# 15. AI Development Checklist

For each AI-assisted feature:

1. define deterministic input evidence,
2. define expected structured output,
3. define privacy policy,
4. define caching/reuse,
5. define analysis version,
6. define unavailable/quota-exhausted behaviour,
7. store AI output separately from facts,
8. allow correction/suppression where relevant,
9. ensure the service remains useful without the AI call.

---

# 16. Definition of Done for Each Step

A step is complete only when applicable items are satisfied:

- functionality implemented,
- relevant tests added,
- tests pass,
- frontend type checks pass,
- backend builds,
- migrations work,
- Docker builds remain valid,
- no secrets committed,
- `.gitignore` remains adequate,
- docs updated,
- CI remains green.

A feature that only works in a developer's local non-container setup is not considered complete once Docker packaging has been introduced.

---

# 17. Definition of Done for Version 1

Version 1 is complete when a clean installation can:

1. start using Docker Compose,
2. serve the application through one web endpoint,
3. authenticate a user with GitHub,
4. import a GitHub account containing hundreds of repositories,
5. show progress during asynchronous analysis,
6. display repository inventory,
7. display commit/activity timelines,
8. display average and median commit-size statistics,
9. distinguish own and external projects,
10. identify technologies and their evolution,
11. classify project types,
12. identify significant external projects,
13. optionally include authorised private repositories,
14. preserve private-data isolation,
15. provide optional AI analysis,
16. expose user-scoped data to an authorised external GPT,
17. accept complementary GPT analysis,
18. export Markdown reports,
19. export PDF reports if included in the final V1 scope,
20. remove/disconnect user data safely,
21. work on desktop and mobile,
22. pass automated CI,
23. build reproducible Docker images,
24. publish release images to GHCR,
25. start successfully from those published images.

---

# 18. Recommended First Execution Order

When using this plan interactively with an LLM, the preferred workflow is:

```text
Ask the LLM to perform Step 1
        |
        v
Receive updated repository/archive
        |
        v
Review/test
        |
        v
Ask for next step
```

The LLM should always inspect the repository's actual current state before performing the next numbered step.

If a step has already been completed manually, verify it and move to the next incomplete step rather than recreating it.

---

# 19. Primary Risk Areas to Revisit During Development

The following areas deserve explicit review as implementation progresses:

## GitHub historical data completeness

Not all historical contribution information is exposed in identical ways. The service must be transparent about coverage.

## Commit attribution

Multiple emails/accounts and authored versus committed-by identities can affect statistics.

## GitHub API rate limits

Large histories require incremental/background collection.

## Private repository data

Privacy provenance must survive aggregation, AI enrichment and export.

## AI inference

The service must not turn weak evidence into unjustified expertise claims.

## Commit-size interpretation

Generated files, imports and formatting changes can create extreme outliers.

## Large histories

200+ repositories must remain a routine supported case.

## External GPT access

The `/me` and scope model must prevent cross-user data access.

---

# 20. Final Recommendation

The LLM should resist the temptation to begin with GitHub analytics immediately.

The preferred order is deliberately:

```text
repository hygiene
    ->
buildable skeleton
    ->
Docker
    ->
CI/GHCR
    ->
database/domain
    ->
authentication
    ->
background jobs
    ->
GitHub data
    ->
analytics
    ->
AI
    ->
reports
    ->
hardening/release
```

This ensures that when complex GitHub and AI functionality arrives, the project already has:

- repository hygiene,
- automated testing,
- reproducible builds,
- container packaging,
- a release mechanism,
- an upgradeable database,
- an asynchronous processing model.

That foundation is particularly important for an LLM-driven implementation, because every later incremental change can be automatically validated against the same build and packaging pipeline.

---

# 21. Reference Sources for CI/CD Implementation

When implementing the workflows, verify exact current action versions against official documentation.

- GitHub documentation – Publishing Docker images:  
  https://docs.github.com/en/actions/use-cases-and-examples/publishing-packages/publishing-docker-images

- Docker documentation – GitHub Actions:  
  https://docs.docker.com/build/ci/github-actions/

- Docker documentation – Introduction to GitHub Actions with Docker:  
  https://docs.docker.com/guides/gha/

- GitHub Container Registry / packages documentation:  
  https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry
