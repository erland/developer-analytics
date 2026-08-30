CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE provider_identity (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    external_user_id VARCHAR(255) NOT NULL,
    login VARCHAR(255),
    display_name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_provider_identity_external UNIQUE (provider, external_user_id),
    CONSTRAINT uq_provider_identity_user_provider UNIQUE (user_id, provider)
);

CREATE TABLE provider_connection (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider_identity_id UUID REFERENCES provider_identity(id) ON DELETE SET NULL,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_validated_at TIMESTAMPTZ,
    disconnected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_provider_connection_user_provider UNIQUE (user_id, provider)
);

CREATE TABLE source_repository (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    external_repository_id VARCHAR(255) NOT NULL,
    owner_external_id VARCHAR(255),
    owner_login VARCHAR(255),
    owner_type VARCHAR(32) NOT NULL DEFAULT 'USER',
    ownership_relation VARCHAR(32) NOT NULL DEFAULT 'OWNED_BY_USER',
    name VARCHAR(255) NOT NULL,
    full_name VARCHAR(512),
    html_url TEXT,
    visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    is_fork BOOLEAN NOT NULL DEFAULT FALSE,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    first_activity_at TIMESTAMPTZ,
    last_activity_at TIMESTAMPTZ,
    sync_status VARCHAR(32) NOT NULL DEFAULT 'NOT_SYNCED',
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_source_repository_user_provider_external
      UNIQUE (user_id, provider, external_repository_id)
);

CREATE INDEX idx_provider_identity_user_id ON provider_identity(user_id);
CREATE INDEX idx_provider_connection_user_id ON provider_connection(user_id);
CREATE INDEX idx_source_repository_user_id ON source_repository(user_id);
CREATE INDEX idx_source_repository_provider_external
    ON source_repository(provider, external_repository_id);
CREATE INDEX idx_source_repository_user_ownership
    ON source_repository(user_id, ownership_relation);
CREATE INDEX idx_source_repository_user_visibility
    ON source_repository(user_id, visibility);
CREATE INDEX idx_source_repository_user_sync_status
    ON source_repository(user_id, sync_status);
CREATE INDEX idx_source_repository_last_activity
    ON source_repository(user_id, last_activity_at DESC);
