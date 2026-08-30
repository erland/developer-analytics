# Version 1 Large-Account Acceptance Test

Step 87 defines a mandatory v1 acceptance scenario for accounts with hundreds
of repositories.

The repeatable acceptance fixture uses **240 repositories** by default. It is
designed to exercise the same system boundaries as a realistic 200+ repository
GitHub account without depending on a particular person's live GitHub account,
network conditions or current rate-limit quota.

Run:

```bash
export DB_PASSWORD='large-account-test-password'
export CREDENTIAL_ENCRYPTION_KEY='AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8='
export WEB_PORT=18087
bash ./scripts/test-large-account-acceptance.sh
```

## Acceptance coverage

| v1 requirement | Verification |
| --- | --- |
| Repository discovery remains usable | Seeds 240 discovered repositories and verifies the user-scoped inventory API returns the expected analysed set |
| Initial analysis progresses incrementally | Mixes `SYNCED`, `SYNCING` and `NOT_SYNCED` repositories instead of pretending enrichment is complete |
| API remains responsive | Repeated inventory/activity/technology calls must complete within a bounded local CI response time |
| Dashboard works before enrichment completes | Built frontend must load while most repositories are only discovered or syncing; activity/technology APIs must still return valid responses |
| UI remains usable with hundreds of repositories | Inventory results stay page-sized rather than returning all repositories |
| Pagination/filtering works | Checks multiple pages plus search, ownership and visibility filters |
| Background jobs recover after restart | Seeds a stale `RUNNING` job, restarts the worker and requires it to leave the stale running state |
| Rate limiting is handled cleanly | Seeds a realistic `RATE_LIMITED` repository sync run and verifies status, remaining quota, reset timestamp and explanatory error are exposed through the API |

## Fixture profile

Default repository distribution:

- 240 total discovered repositories,
- every 10th repository is private and intentionally excluded from analysis,
- every 5th repository represents an external contribution,
- 40 repositories are already `SYNCED`,
- 80 are `SYNCING`,
- the remainder are `NOT_SYNCED`,
- activity dates span active and inactive projects,
- periodic names are deterministic search targets.

This deliberately represents a dashboard that becomes useful before background
enrichment is complete.

## Why this is deterministic

The acceptance test does not call live GitHub. Live-provider behavior is already
covered by the GitHub adapter boundary and provider-specific tests.

For a version-1 release gate, deterministic large-account behavior is more useful
than a test whose success changes with:

- external network health,
- a personal account's repository count,
- repository permission changes,
- GitHub rate-limit state,
- GitHub API availability.

A separate manual real-account validation can still be performed before a major
release when appropriate, but it does not replace this CI acceptance gate.

## Pass criteria

The test passes only when all eight checks complete:

1. large repository inventory is queryable,
2. partial enrichment remains usable,
3. representative APIs remain responsive,
4. page size bounds the UI dataset,
5. pagination and filtering work,
6. stale worker jobs recover after restart,
7. rate-limit state is represented cleanly,
8. the account remains queryable after worker restart.

The GitHub Actions job is named **Large-account acceptance test**.

## Local execution note

The acceptance test requires Docker. In environments without a Docker daemon,
the shell/YAML structure can be validated but the full acceptance run must occur
in GitHub Actions or another Docker-capable host.
