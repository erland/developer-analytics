ALTER TABLE external_client_token
    ADD COLUMN privacy_scope VARCHAR(50)
    NOT NULL DEFAULT 'PUBLIC_ONLY';
