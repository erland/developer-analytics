CREATE TABLE returned_ai_assessment (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    external_client_token_id UUID REFERENCES external_client_token(id) ON DELETE SET NULL,
    analysis_type VARCHAR(120) NOT NULL,
    source_client VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    data_scope VARCHAR(50) NOT NULL,
    content JSONB NOT NULL,
    contains_private_data BOOLEAN NOT NULL
);

CREATE INDEX idx_returned_ai_assessment_user_created
    ON returned_ai_assessment(user_id, created_at DESC);
