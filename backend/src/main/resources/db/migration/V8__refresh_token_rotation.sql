-- =============================================================================
-- V8 — Refresh-token rotation with reuse detection (Phase 4 Resilience)
--
-- Each refresh token belongs to a family (one per user/device login session).
-- On refresh, the old token is marked as "used" and a new one is issued. If a
-- token that has ALREADY been used is presented again, the entire family is
-- revoked — this signals a possible token theft (an attacker replayed a
-- previously-used refresh token).
-- =============================================================================

CREATE TABLE refresh_token_families (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users(id),
    family_hash   VARCHAR(128) NOT NULL,
    revoked       BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at    TIMESTAMP
);

CREATE TABLE refresh_tokens (
    id            UUID PRIMARY KEY,
    family_id     UUID NOT NULL REFERENCES refresh_token_families(id),
    token_hash    VARCHAR(255) NOT NULL UNIQUE,
    expires_at    TIMESTAMP NOT NULL,
    used          BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at       TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_families_user ON refresh_token_families(user_id);