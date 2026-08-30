CREATE TABLE worker_heartbeat (
    worker_id VARCHAR(128) PRIMARY KEY,
    runtime_role VARCHAR(32) NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_worker_heartbeat_last_seen
    ON worker_heartbeat(last_seen_at DESC);
