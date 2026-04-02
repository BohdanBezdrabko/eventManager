-- V31__whatsapp_message_idempotency.sql
-- Переконатися що таблиці ідемпотентності існують (це дублювання V27, але необхідно для безпеки)
-- Якщо таблиці вже існують, цей скрипт нічого не зробить

-- Таблиця для обробних повідомлень (ідемпотентність)
CREATE TABLE IF NOT EXISTS processed_whatsapp_messages (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    wa_id VARCHAR(50) NOT NULL,
    webhook_timestamp TIMESTAMP,
    message_type VARCHAR(50),
    message_summary TEXT,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_message_id UNIQUE (message_id)
);

-- Створити індекси якщо їх немає
CREATE INDEX IF NOT EXISTS idx_processed_messages_wa_id ON processed_whatsapp_messages(wa_id);
CREATE INDEX IF NOT EXISTS idx_processed_messages_timestamp ON processed_whatsapp_messages(webhook_timestamp);
