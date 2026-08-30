ALTER TABLE source_repository
    ADD COLUMN included_in_analysis BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE source_repository
SET included_in_analysis = FALSE
WHERE visibility = 'PRIVATE';

CREATE INDEX idx_source_repository_user_analysis
    ON source_repository(user_id, included_in_analysis);
