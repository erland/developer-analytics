# User Data Deletion

Step 70 implements full Developer Analytics account-data deletion.

## User action

The signed-in user deletes all Developer Analytics data through:

```text
DELETE /api/me/data
```

with the explicit body:

```json
{
  "confirmation": "DELETE_MY_DATA"
}
```

The Account UI requires the same literal confirmation before enabling the
destructive action.

## Deletion boundary

The database `app_user` row is the authoritative deletion root. All
user-owned tables use foreign keys with `ON DELETE CASCADE`, while repository
analysis also cascades through `source_repository`.

Deleting the user therefore removes, in one transaction:

- provider identities and provider connections,
- browser sessions,
- source repositories,
- contributions,
- repository and user activity aggregates,
- technology evidence and user technology assessments,
- project-category classifications,
- project significance assessments,
- AI project classifications,
- user AI insights,
- returned external AI assessments,
- user corrections,
- repository/contribution synchronisation history,
- background jobs,
- external GPT/API client tokens.

This approach avoids a fragile application-maintained delete order.

## Reports

Markdown and PDF exports are generated as response files and are not persisted
server-side, so there are no stored report records to delete. The deletion
response makes this explicit and reports `persistedReportsDeleted = 0`.

Files that a user has already downloaded are outside the service's storage
boundary and cannot be deleted by the server.

## Session termination

The current browser session is deleted by the cascade and the deletion response
also expires the session cookie. External client tokens are deleted by the same
user cascade.

## Verification

`UserDataDeletionIntegrationTest` creates a realistic relational graph containing:

- account,
- provider identity/connection,
- browser session,
- repository,
- contribution,
- background job,
- sync history,
- user/repository/technology aggregates.

It then deletes the user through `UserDataDeletionService` and verifies that
every related row is gone. This validates the actual PostgreSQL/Flyway cascade
model rather than a mocked deletion sequence.
