ALTER TABLE background_job
    ADD COLUMN deduplication_key VARCHAR(255);

CREATE INDEX idx_background_job_user_deduplication
    ON background_job(user_id, deduplication_key, status);

CREATE UNIQUE INDEX uq_background_job_active_deduplication
    ON background_job(user_id, deduplication_key)
    WHERE deduplication_key IS NOT NULL
      AND status IN ('QUEUED', 'WAITING', 'RUNNING');
