ALTER TABLE provider_connection
    ADD COLUMN private_repository_access VARCHAR(30) NOT NULL DEFAULT 'NOT_AUTHORISED',
    ADD COLUMN private_repository_authorised_at TIMESTAMPTZ;

ALTER TABLE auth_login_attempt
    ADD COLUMN purpose VARCHAR(40) NOT NULL DEFAULT 'LOGIN',
    ADD COLUMN user_id UUID REFERENCES app_user(id) ON DELETE CASCADE;

CREATE INDEX idx_auth_login_attempt_user
    ON auth_login_attempt(user_id);
