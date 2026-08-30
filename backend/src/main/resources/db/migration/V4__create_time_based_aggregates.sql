CREATE TABLE user_activity_month (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    year_month DATE NOT NULL,
    commit_count INTEGER NOT NULL DEFAULT 0,
    additions BIGINT NOT NULL DEFAULT 0,
    deletions BIGINT NOT NULL DEFAULT 0,
    changed_lines BIGINT NOT NULL DEFAULT 0,
    active_repository_count INTEGER NOT NULL DEFAULT 0,
    pull_request_count INTEGER NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    issue_count INTEGER NOT NULL DEFAULT 0,
    release_count INTEGER NOT NULL DEFAULT 0,
    maintenance_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_activity_month UNIQUE (user_id, year_month)
);

CREATE TABLE repository_activity_month (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    source_repository_id UUID NOT NULL REFERENCES source_repository(id) ON DELETE CASCADE,
    year_month DATE NOT NULL,
    commit_count INTEGER NOT NULL DEFAULT 0,
    additions BIGINT NOT NULL DEFAULT 0,
    deletions BIGINT NOT NULL DEFAULT 0,
    changed_lines BIGINT NOT NULL DEFAULT 0,
    pull_request_count INTEGER NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    issue_count INTEGER NOT NULL DEFAULT 0,
    release_count INTEGER NOT NULL DEFAULT 0,
    maintenance_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_repository_activity_month UNIQUE (source_repository_id, year_month)
);

CREATE TABLE technology_activity_month (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    technology_key VARCHAR(128) NOT NULL,
    year_month DATE NOT NULL,
    repository_count INTEGER NOT NULL DEFAULT 0,
    activity_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_technology_activity_month UNIQUE (user_id, technology_key, year_month)
);

CREATE INDEX idx_user_activity_month_user_period
    ON user_activity_month(user_id, year_month DESC);
CREATE INDEX idx_repository_activity_month_repo_period
    ON repository_activity_month(source_repository_id, year_month DESC);
CREATE INDEX idx_technology_activity_month_technology
    ON technology_activity_month(user_id, technology_key, year_month DESC);
