CREATE TABLE IF NOT EXISTS user_whatsapp (
                                             id BIGSERIAL PRIMARY KEY,
                                             wa_id VARCHAR(32) NOT NULL UNIQUE,
    profile_name TEXT,
    linked_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS event_subscriptions_whatsapp (
                                                            id BIGSERIAL PRIMARY KEY,
                                                            event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_whatsapp_id BIGINT NOT NULL REFERENCES user_whatsapp(id) ON DELETE CASCADE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_event_user_whatsapp UNIQUE (event_id, user_whatsapp_id)
    );

CREATE INDEX IF NOT EXISTS ix_esw_event_active
    ON event_subscriptions_whatsapp (event_id, active);

CREATE INDEX IF NOT EXISTS ix_esw_user
    ON event_subscriptions_whatsapp (user_whatsapp_id);
