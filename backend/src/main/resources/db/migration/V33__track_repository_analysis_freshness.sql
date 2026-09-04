ALTER TABLE source_repository
    ADD COLUMN analysis_completed_at TIMESTAMPTZ,
    ADD COLUMN analyzed_activity_at TIMESTAMPTZ,
    ADD COLUMN analysis_version INTEGER NOT NULL DEFAULT 0;
