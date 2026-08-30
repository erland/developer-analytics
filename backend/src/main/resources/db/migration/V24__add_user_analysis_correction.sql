CREATE TABLE user_analysis_correction (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    repository_id UUID REFERENCES source_repository(id) ON DELETE CASCADE,
    correction_type VARCHAR(50) NOT NULL,
    correction_key VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_analysis_correction
        UNIQUE(user_id, repository_id, correction_type, correction_key)
);

CREATE INDEX idx_user_analysis_correction_user_type
    ON user_analysis_correction(user_id, correction_type);
