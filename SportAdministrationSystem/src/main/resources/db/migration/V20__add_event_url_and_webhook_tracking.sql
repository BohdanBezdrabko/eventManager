-- V20: Додаємо поле URL до івентів і таблицю для tracking webhook-eventi (ідемпотентність)

-- 1) Додаємо url до событий
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS url VARCHAR(1024);

-- 2) Таблиця для tracking вхідних webhook-подій (ідемпотентність)
CREATE TABLE IF NOT EXISTS webhook_events (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(128) NOT NULL UNIQUE,
    messenger VARCHAR(16) NOT NULL,
    payload TEXT NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processing BOOLEAN NOT NULL DEFAULT FALSE
);

-- 3) Індекси для швидких вибірок
CREATE INDEX IF NOT EXISTS idx_webhook_events_external_id ON webhook_events(external_id);
CREATE INDEX IF NOT EXISTS idx_webhook_events_messenger_received ON webhook_events(messenger, received_at);
CREATE INDEX IF NOT EXISTS idx_webhook_events_processed ON webhook_events(processed, processing);

-- 4) Таблиця для tracking доставки повідомлень (retry policy)
CREATE TABLE IF NOT EXISTS message_delivery (
    id BIGSERIAL PRIMARY KEY,
    external_message_id VARCHAR(128) UNIQUE,
    messenger VARCHAR(16) NOT NULL,
    recipient_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    delivery_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    last_attempt_at TIMESTAMP,
    next_attempt_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 5) Індекси для message_delivery
CREATE INDEX IF NOT EXISTS idx_msg_delivery_status ON message_delivery(delivery_status);
CREATE INDEX IF NOT EXISTS idx_msg_delivery_next_attempt ON message_delivery(next_attempt_at) WHERE delivery_status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_msg_delivery_recipient ON message_delivery(messenger, recipient_id);
