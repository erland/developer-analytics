CREATE TABLE contribution_sync_run (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    repository_id UUID NOT NULL REFERENCES source_repository(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    contributions_seen INTEGER NOT NULL DEFAULT 0,
    contributions_created INTEGER NOT NULL DEFAULT 0,
    contributions_updated INTEGER NOT NULL DEFAULT 0,
    pages_processed INTEGER NOT NULL DEFAULT 0,
    rate_limit_remaining INTEGER,
    rate_limit_reset_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_contribution_sync_run_user_created
    ON contribution_sync_run(user_id, created_at DESC);

CREATE INDEX idx_contribution_sync_run_repository_created
    ON contribution_sync_run(repository_id, created_at DESC);
