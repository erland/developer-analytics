# Basic Security Checks

Step 80 adds a deliberately small automated security baseline. The goal is to
catch common dependency, image and secret risks without turning normal
development into a separate security-tooling project.

## Automated checks

### Pull-request dependency review

`.github/workflows/dependency-review.yml` uses GitHub's dependency review action
on pull requests and rejects newly introduced dependencies with known
vulnerabilities rated `high` or `critical`.

This relies on the repository's GitHub dependency graph.

### Frontend runtime dependency audit

The normal frontend CI job runs:

```bash
npm audit --omit=dev --audit-level=high
```

Only production dependencies are blocking here. Development-tool dependency
risk is still visible through Dependabot/dependency review but does not make
ordinary frontend CI unnecessarily fragile.

### Container image scanning

The existing container CI job scans the exact images it just built:

- `developer-analytics-web:ci`
- `developer-analytics-backend:ci`

Trivy is configured to fail for **fixable CRITICAL** findings. Unfixed findings
remain visible in scan output but do not block development. This keeps the
baseline actionable rather than making CI permanently red for issues that
cannot yet be remediated.

The images are subsequently exercised by the Step 79 runtime smoke test.

## Dependency update automation

`.github/dependabot.yml` requests weekly updates for:

- npm dependencies in `/frontend`,
- Maven dependencies in `/backend`,
- frontend Docker base images,
- backend Docker base images,
- GitHub Actions.

Dependabot PRs go through the same validation and security checks as other pull
requests.

## GitHub secret scanning

Secret scanning and push protection are repository-level GitHub security
capabilities rather than source files. They should be enabled in the GitHub
repository under the code-security settings when the repository/plan supports
them.

Recommended repository settings:

- Dependency graph: enabled
- Dependabot alerts: enabled
- Dependabot security updates: enabled
- Secret scanning: enabled
- Push protection for secrets: enabled

No second secret-scanning engine is added in Step 80. This avoids duplicating
GitHub's native detection and keeps the local/CI toolchain small.

## Scope

This baseline intentionally does **not** introduce:

- a large SAST platform,
- DAST against every pull request,
- mandatory live external security services,
- broad vulnerability gates on unfixed findings,
- private-source upload to third-party scanners.

Those can be added later if the threat model or deployment environment warrants
them.
