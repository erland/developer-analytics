CREATE TABLE contribution_file_change (
    id UUID PRIMARY KEY,
    contribution_id UUID NOT NULL REFERENCES contribution(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    source_repository_id UUID NOT NULL REFERENCES source_repository(id) ON DELETE CASCADE,
    path VARCHAR(4096) NOT NULL,
    additions INTEGER NOT NULL,
    deletions INTEGER NOT NULL,
    change_kind VARCHAR(32) NOT NULL,
    classifier_confidence DOUBLE PRECISION NOT NULL,
    classifier_rule_key VARCHAR(128) NOT NULL,
    classifier_version VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    privacy_provenance VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_contribution_file_change_path UNIQUE (contribution_id, path),
    CONSTRAINT ck_contribution_file_change_additions CHECK (additions >= 0),
    CONSTRAINT ck_contribution_file_change_deletions CHECK (deletions >= 0),
    CONSTRAINT ck_contribution_file_change_confidence CHECK (classifier_confidence >= 0.0 AND classifier_confidence <= 1.0)
);

CREATE INDEX idx_contribution_file_change_contribution
    ON contribution_file_change(contribution_id);

CREATE INDEX idx_contribution_file_change_user_time_kind
    ON contribution_file_change(user_id, occurred_at DESC, change_kind);

CREATE INDEX idx_contribution_file_change_repository_time_kind
    ON contribution_file_change(source_repository_id, occurred_at DESC, change_kind);

CREATE INDEX idx_contribution_file_change_user_kind_time
    ON contribution_file_change(user_id, change_kind, occurred_at DESC);
