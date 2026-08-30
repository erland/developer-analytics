CREATE TABLE user_technology_assessment (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    technology_id UUID NOT NULL REFERENCES technology_catalogue(id) ON DELETE CASCADE,
    strength VARCHAR(32) NOT NULL,
    repository_count INTEGER NOT NULL DEFAULT 0,
    evidence_count INTEGER NOT NULL DEFAULT 0,
    independent_evidence_types INTEGER NOT NULL DEFAULT 0,
    first_observed_at TIMESTAMPTZ,
    last_observed_at TIMESTAMPTZ,
    recent_repository_count INTEGER NOT NULL DEFAULT 0,
    score INTEGER NOT NULL DEFAULT 0,
    rationale JSONB NOT NULL DEFAULT '{}'::jsonb,
    calculated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_technology_assessment UNIQUE(user_id, technology_id)
);

CREATE INDEX idx_user_technology_assessment_user_strength
    ON user_technology_assessment(user_id, strength);

CREATE INDEX idx_user_technology_assessment_technology
    ON user_technology_assessment(technology_id);
