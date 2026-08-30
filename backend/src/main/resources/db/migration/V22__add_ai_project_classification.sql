CREATE TABLE ai_project_classification (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL REFERENCES source_repository(id) ON DELETE CASCADE,
    input_fingerprint VARCHAR(64) NOT NULL,
    classifications JSONB NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    explanation TEXT NOT NULL,
    analysis_version VARCHAR(40) NOT NULL,
    provider_id VARCHAR(80) NOT NULL,
    model_id VARCHAR(160) NOT NULL,
    privacy_provenance VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ai_project_classification_fingerprint
        UNIQUE(repository_id, input_fingerprint, analysis_version, provider_id, model_id)
);

CREATE INDEX idx_ai_project_classification_repository_created
    ON ai_project_classification(repository_id, created_at DESC);
