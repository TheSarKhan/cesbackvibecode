-- İnvestor portal: push token-ları + bildiriş tarixçəsi (M5)

CREATE TABLE investor_push_tokens (
    id          BIGSERIAL PRIMARY KEY,
    investor_id BIGINT NOT NULL REFERENCES investors(id),
    token       VARCHAR(512) NOT NULL UNIQUE,
    platform    VARCHAR(20),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_push_token_investor ON investor_push_tokens(investor_id);

CREATE TABLE investor_notifications (
    id          BIGSERIAL PRIMARY KEY,
    investor_id BIGINT NOT NULL REFERENCES investors(id),
    title       VARCHAR(255) NOT NULL,
    body        VARCHAR(1000),
    type        VARCHAR(40),
    related_id  BIGINT,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notif_investor_created ON investor_notifications(investor_id, created_at DESC);
