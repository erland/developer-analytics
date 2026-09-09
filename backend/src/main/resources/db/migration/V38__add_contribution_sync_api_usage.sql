ALTER TABLE contribution_sync_run
    ADD COLUMN api_request_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN api_requests_by_endpoint JSONB NOT NULL DEFAULT '{}'::jsonb;
