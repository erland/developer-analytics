ALTER TABLE source_repository
    ADD COLUMN description TEXT,
    ADD COLUMN topics JSONB NOT NULL DEFAULT '[]'::jsonb;

CREATE INDEX idx_source_repository_topics
    ON source_repository USING GIN(topics);
