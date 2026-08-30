CREATE TABLE project_category (
    id UUID PRIMARY KEY,
    category_key VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    description TEXT,
    aliases JSONB NOT NULL DEFAULT '[]'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE repository_project_category (
    repository_id UUID NOT NULL REFERENCES source_repository(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES project_category(id) ON DELETE CASCADE,
    source VARCHAR(32) NOT NULL,
    confidence VARCHAR(32) NOT NULL,
    rationale JSONB NOT NULL DEFAULT '{}'::jsonb,
    observed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(repository_id, category_id, source)
);

CREATE INDEX idx_project_category_active
    ON project_category(active, sort_order, display_name);

CREATE INDEX idx_repository_project_category_repository
    ON repository_project_category(repository_id);

CREATE INDEX idx_repository_project_category_category
    ON repository_project_category(category_id);
