-- W8: replace password-based auth with OAuth/SSO.
-- Email is no longer a property of the user record; it lives on the identity row
-- so the same user can have multiple provider identities in future (post-v1).

ALTER TABLE users
    DROP COLUMN password_hash,
    DROP COLUMN email;

DROP INDEX IF EXISTS idx_users_email;

CREATE TABLE user_identities (
    provider         VARCHAR(20)   NOT NULL
                                   CHECK (provider IN ('GOOGLE','FACEBOOK')),
    email            VARCHAR(320)  NOT NULL,
    user_id          UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_subject VARCHAR(255),                    -- OAuth sub / user-id for audit
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    PRIMARY KEY (provider, email)
);

-- Fast lookup of all identities belonging to a user (e.g. GET /me profile join)
CREATE INDEX idx_user_identities_user_id ON user_identities (user_id);
