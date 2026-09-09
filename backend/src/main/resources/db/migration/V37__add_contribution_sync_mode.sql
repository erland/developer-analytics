ALTER TABLE contribution_sync_run
    ADD COLUMN sync_mode VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN';

ALTER TABLE contribution_sync_run
    ALTER COLUMN sync_mode DROP DEFAULT;
