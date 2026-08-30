CREATE TABLE auth_login_attempt (
    id UUID PRIMARY KEY,
    state_hash VARCHAR(128) NOT NULL UNIQUE,
    pkce_verifier VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_session_user ON user_session(user_id);
CREATE INDEX idx_user_session_expiry ON user_session(expires_at);
CREATE INDEX idx_auth_login_attempt_expiry ON auth_login_attempt(expires_at);
