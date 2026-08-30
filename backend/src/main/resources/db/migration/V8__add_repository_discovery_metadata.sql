ALTER TABLE source_repository
    ADD COLUMN discovered_at TIMESTAMPTZ,
    ADD COLUMN last_seen_at TIMESTAMPTZ;

CREATE INDEX idx_source_repository_user_last_seen
    ON source_repository (user_id, last_seen_at DESC);
