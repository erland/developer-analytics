CREATE TABLE technology_catalogue (
    id UUID PRIMARY KEY,
    technology_key VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    homepage_url TEXT,
    aliases JSONB NOT NULL DEFAULT '[]'::jsonb,
    language_evidence JSONB NOT NULL DEFAULT '[]'::jsonb,
    file_evidence JSONB NOT NULL DEFAULT '[]'::jsonb,
    manifest_evidence JSONB NOT NULL DEFAULT '[]'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_technology_catalogue_category
    ON technology_catalogue(category);

CREATE INDEX idx_technology_catalogue_active
    ON technology_catalogue(active);
