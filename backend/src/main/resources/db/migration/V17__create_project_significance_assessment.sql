CREATE TABLE project_significance_assessment (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    repository_id UUID NOT NULL REFERENCES source_repository(id) ON DELETE CASCADE,

    significance_level VARCHAR(32) NOT NULL,
    significance_score INTEGER NOT NULL,
    popularity_score INTEGER NOT NULL,
    contributor_score INTEGER NOT NULL,
    longevity_score INTEGER NOT NULL,
    ecosystem_score INTEGER NOT NULL,
    activity_score INTEGER NOT NULL,
    significance_rationale JSONB NOT NULL DEFAULT '{}'::jsonb,

    involvement_level VARCHAR(32) NOT NULL,
    involvement_score INTEGER NOT NULL,
    contribution_score INTEGER NOT NULL,
    involvement_duration_score INTEGER NOT NULL,
    involvement_recency_score INTEGER NOT NULL,
    relative_contribution_score INTEGER NOT NULL,
    involvement_rationale JSONB NOT NULL DEFAULT '{}'::jsonb,

    calculated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_project_significance_assessment
        UNIQUE(user_id, repository_id)
);

CREATE INDEX idx_project_significance_user
    ON project_significance_assessment(user_id);

CREATE INDEX idx_project_significance_rank
    ON project_significance_assessment(
        user_id,
        significance_score DESC,
        involvement_score DESC
    );
