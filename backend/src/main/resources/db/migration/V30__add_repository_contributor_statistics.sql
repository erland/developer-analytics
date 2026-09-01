ALTER TABLE source_repository
    ADD COLUMN contributor_count INTEGER,
    ADD COLUMN human_contributor_count INTEGER,
    ADD COLUMN bot_contributor_count INTEGER,
    ADD COLUMN user_commit_count INTEGER,
    ADD COLUMN repository_commit_count INTEGER,
    ADD COLUMN user_additions BIGINT,
    ADD COLUMN user_deletions BIGINT,
    ADD COLUMN contributor_stats_at TIMESTAMPTZ;
