-- Створюємо enum для месенджера (можна розширювати у майбутньому)
DO $$ BEGIN
CREATE TYPE messenger AS ENUM ('TELEGRAM');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- Зв'язок користувача з Telegram
CREATE TABLE IF NOT EXISTS user_telegram (
                                             id         BIGSERIAL PRIMARY KEY,
                                             user_id    BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    tg_user_id BIGINT NOT NULL UNIQUE,
    tg_chat_id BIGINT NOT NULL UNIQUE,
    linked_at  TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_user_telegram_user ON user_telegram(user_id);

-- Підписки на події
CREATE TABLE IF NOT EXISTS event_subscriptions (
                                                   id         BIGSERIAL PRIMARY KEY,
                                                   event_id   BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    messenger  messenger NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, user_id, messenger)
    );

CREATE INDEX IF NOT EXISTS idx_event_subscriptions_event_active
    ON event_subscriptions(event_id, messenger, active);
CREATE INDEX IF NOT EXISTS idx_event_subscriptions_user
    ON event_subscriptions(user_id, messenger);
