CREATE TABLE repository_user_activity_week (
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    repository_id UUID NOT NULL REFERENCES source_repository(id) ON DELETE CASCADE,
    week_start DATE NOT NULL,
    commits INTEGER NOT NULL DEFAULT 0,
    additions BIGINT NOT NULL DEFAULT 0,
    deletions BIGINT NOT NULL DEFAULT 0,
    observed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, repository_id, week_start)
);

CREATE INDEX idx_repository_user_activity_week_user_week
    ON repository_user_activity_week(user_id, week_start);

CREATE INDEX idx_repository_user_activity_week_repository_week
    ON repository_user_activity_week(repository_id, week_start);
