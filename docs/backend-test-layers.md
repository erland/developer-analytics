# Backend Test Layers

Step 75 makes backend test responsibilities explicit while keeping
`mvn verify` as the authoritative full-suite CI gate.

Every backend `*Test.java` class carries one or more JUnit 5 `@Tag` values.
The tags are intentionally capability-oriented rather than mirroring production
package names.

| Layer | Tag | Purpose |
| --- | --- | --- |
| Unit | `unit` | Pure domain/service/model/rendering logic with fast feedback |
| Persistence | `persistence` | PostgreSQL/Flyway/JPA behavior and realistic relational integration |
| GitHub adapter | `github-adapter` | Provider mapping and mocked/recorded GitHub response contracts |
| Authorisation | `authorization` | `/me` isolation, sessions, credentials and external-client access |
| Worker/job | `worker-job` | Persistent jobs, retries, recovery and sync orchestration |
| Privacy | `privacy` | Private repository boundaries, AI consent, export filtering, deletion and redaction |

Tests can deliberately have multiple tags. For example a database-backed `/me`
test can be both `authorization` and `persistence`.

## Run the full backend suite

```bash
cd backend
mvn verify
```

or:

```bash
bash ./scripts/run-backend-test-layer.sh all
```

The default Maven lifecycle has no tag filter, so this remains equivalent to the
pre-Step-75 behavior.

## Run one layer

Examples:

```bash
bash ./scripts/run-backend-test-layer.sh unit
bash ./scripts/run-backend-test-layer.sh persistence
bash ./scripts/run-backend-test-layer.sh github-adapter
bash ./scripts/run-backend-test-layer.sh authorization
bash ./scripts/run-backend-test-layer.sh worker-job
bash ./scripts/run-backend-test-layer.sh privacy
```

These map to Maven profiles named `test-<layer>`, for example:

```bash
cd backend
mvn -Ptest-privacy test
```

## Maintenance rule

Every new backend `*Test.java` must have at least one supported layer tag.
CI runs:

```bash
python3 scripts/check-backend-test-layers.py
```

before the complete backend verification. The check fails if:

- any required layer has no tests, or
- any backend test class has no layer tag.

This prevents the layer model from silently decaying as the test suite grows.

## GitHub adapter test data

GitHub adapter tests should prefer deterministic mocked/recorded provider
responses. They must not require a live GitHub account, personal token or
network access in normal CI. Recorded fixtures must contain no credentials,
private source code or personally sensitive repository content.

## Layer expectations

### Unit

Keep most deterministic classification, aggregation, report rendering, privacy
policy and value-object behavior here. Unit tests should remain fast and should
not start infrastructure unless the behavior genuinely depends on it.

### Persistence

Use real PostgreSQL through Quarkus Dev Services/Testcontainers for schema,
constraint, JSONB, Flyway and transactional behavior where H2/in-memory
substitution would hide production differences.

### GitHub adapter

Verify mapping of GitHub payloads into provider-neutral domain types, pagination
and error/rate-limit semantics. Provider HTTP behavior should be mocked or
recorded deterministically.

### Authorisation

Verify current-user scoping, session/token boundaries, external-client scopes
and mutation isolation. A user must never be able to read or mutate another
user's data.

### Worker/job

Verify persistent job state transitions, retries, interruption recovery,
deduplication and orchestration idempotency.

### Privacy

Verify public/private provenance, explicit private repository authorization,
AI privacy rules, report/export privacy, deletion, token/credential handling and
structured-log redaction.
