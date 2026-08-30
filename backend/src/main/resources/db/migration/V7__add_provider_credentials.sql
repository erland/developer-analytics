ALTER TABLE provider_connection
    ADD COLUMN credential_ciphertext TEXT,
    ADD COLUMN credential_key_version VARCHAR(32),
    ADD COLUMN credential_updated_at TIMESTAMPTZ;

CREATE INDEX idx_provider_connection_credential_version
    ON provider_connection (credential_key_version);
