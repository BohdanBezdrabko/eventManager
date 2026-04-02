-- V21: Додаємо підтримку bind-кодів для Telegram груп

CREATE TABLE IF NOT EXISTS telegram_bind_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    tg_group_chat_id BIGINT NOT NULL,
    group_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL DEFAULT NOW() + INTERVAL '24 hours',
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_by_user_id BIGINT,
    used_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bind_codes_code ON telegram_bind_codes(code);
CREATE INDEX IF NOT EXISTS idx_bind_codes_expires_at ON telegram_bind_codes(expires_at);
CREATE INDEX IF NOT EXISTS idx_bind_codes_tg_group_chat_id ON telegram_bind_codes(tg_group_chat_id);
