CREATE TABLE provider_sync_run (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    sync_mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    repositories_planned INTEGER NOT NULL DEFAULT 0,
    api_request_count INTEGER NOT NULL DEFAULT 0,
    api_requests_by_endpoint JSONB NOT NULL DEFAULT '{}'::jsonb,
    rate_limit_pause_count INTEGER NOT NULL DEFAULT 0,
    paused_duration_seconds BIGINT NOT NULL DEFAULT 0,
    pause_started_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_provider_sync_run_user_started
    ON provider_sync_run(user_id, started_at DESC);

ALTER TABLE contribution_sync_run
    ADD COLUMN provider_sync_run_id UUID REFERENCES provider_sync_run(id) ON DELETE SET NULL;

CREATE INDEX idx_contribution_sync_run_provider_sync
    ON contribution_sync_run(provider_sync_run_id);
