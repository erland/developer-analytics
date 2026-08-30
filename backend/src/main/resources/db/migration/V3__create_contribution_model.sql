CREATE TABLE contribution (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    source_repository_id UUID NOT NULL REFERENCES source_repository(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_contribution_id VARCHAR(255) NOT NULL,
    contribution_type VARCHAR(32) NOT NULL,
    title VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    state VARCHAR(32),
    additions INTEGER,
    deletions INTEGER,
    changed_files INTEGER,
    merged BOOLEAN,
    provider_metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_contribution_provider
      UNIQUE (provider, provider_contribution_id, contribution_type)
);

CREATE INDEX idx_contribution_user_occurred_at
    ON contribution(user_id, occurred_at DESC);
CREATE INDEX idx_contribution_repository_occurred_at
    ON contribution(source_repository_id, occurred_at DESC);
CREATE INDEX idx_contribution_user_type_time
    ON contribution(user_id, contribution_type, occurred_at DESC);
