ALTER TABLE source_repository
    ADD COLUMN sync_error TEXT;

CREATE TABLE repository_sync_run (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    repositories_seen INTEGER NOT NULL DEFAULT 0,
    repositories_created INTEGER NOT NULL DEFAULT 0,
    repositories_updated INTEGER NOT NULL DEFAULT 0,
    pages_processed INTEGER NOT NULL DEFAULT 0,
    rate_limit_remaining INTEGER,
    rate_limit_reset_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_repository_sync_run_user_created
    ON repository_sync_run(user_id, created_at DESC);
