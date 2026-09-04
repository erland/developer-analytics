ALTER TABLE source_repository
    ADD COLUMN repository_size_bytes BIGINT,
    ADD COLUMN code_size_bytes BIGINT;
