# GitHub Disconnect Workflow

Step 69 makes GitHub disconnect a complete lifecycle operation rather than only
a connection-status change.

## Immediate effects

On disconnect, Developer Analytics:

1. cancels queued/waiting GitHub background jobs,
2. clears the encrypted GitHub provider credential,
3. removes the optional private-repository authorisation flag,
4. marks the provider connection `DISCONNECTED`.

`ProviderCredentialService` also refuses to return a credential for a
disconnected provider, preventing new synchronisation work from starting even
if stale application code attempts to access the connection.

A job already executing concurrently may finish its current provider request,
but it cannot obtain a new credential after disconnect. Queued and waiting jobs
are marked `CANCELLED`.

## Explicit data disposition

The request must contain exactly one choice:

- `PRESERVE_ANALYSED_DATA`
- `REMOVE_ANALYSED_DATA`

There is no hidden default.

### Preserve analysed data

Existing repository history and derived analysis remain available in the
dashboard. No further GitHub synchronisation is possible until the user
reconnects.

### Remove analysed data

Developer Analytics deletes the account's GitHub repository inventory.
Repository-owned data is removed through database cascades, including:

- contributions,
- repository activity,
- contribution sync runs,
- technology evidence,
- project-category assignments,
- project significance assessments,
- project AI classifications,
- repository-scoped corrections.

It also removes user-level analytical derivatives that can contain GitHub data:

- user activity aggregates,
- technology activity aggregates,
- user technology assessments,
- user corrections,
- user AI insights,
- returned AI assessments,
- GitHub repository-sync history.

External GPT/API credentials are not GitHub provider credentials and therefore
are not revoked by this workflow. They remain user-controlled under Account.

## Provider revocation boundary

The service forgets its locally stored GitHub credential on disconnect. A
provider-side OAuth/application grant may still be visible in GitHub account
settings; remote provider revocation is a separate provider capability and is
not implied unless the provider API supports and performs it.
