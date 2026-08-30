CREATE TABLE user_ai_insight (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    input_fingerprint VARCHAR(64) NOT NULL,
    likely_roles JSONB NOT NULL,
    technical_focus TEXT NOT NULL,
    breadth_depth_observation TEXT NOT NULL,
    technology_evolution_summary TEXT NOT NULL,
    open_source_engagement_summary TEXT NOT NULL,
    analysis_version VARCHAR(40) NOT NULL,
    provider_id VARCHAR(80) NOT NULL,
    model_id VARCHAR(160) NOT NULL,
    privacy_provenance VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_ai_insight_fingerprint
        UNIQUE(user_id, input_fingerprint, analysis_version, provider_id, model_id)
);

CREATE INDEX idx_user_ai_insight_user_created
    ON user_ai_insight(user_id, created_at DESC);
