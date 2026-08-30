CREATE TABLE repository_technology_evidence (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    repository_id UUID NOT NULL REFERENCES source_repository(id) ON DELETE CASCADE,
    technology_id UUID NOT NULL REFERENCES technology_catalogue(id) ON DELETE CASCADE,
    evidence_type VARCHAR(32) NOT NULL,
    strength VARCHAR(32) NOT NULL,
    source_value VARCHAR(255),
    measured_value BIGINT,
    observed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_repository_technology_evidence
        UNIQUE(repository_id, technology_id, evidence_type, source_value)
);

CREATE INDEX idx_repository_technology_evidence_user
    ON repository_technology_evidence(user_id);

CREATE INDEX idx_repository_technology_evidence_repository
    ON repository_technology_evidence(repository_id);

CREATE INDEX idx_repository_technology_evidence_technology
    ON repository_technology_evidence(technology_id);
