-- V27__whatsapp_delivery_tracking.sql
-- Таблиці для трекінгу доставки та ідемпотентності webhook

-- 1. Таблиця для обробних повідомлень (ідемпотентність)
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

-- Індекс для швидкого пошуку
CREATE INDEX IF NOT EXISTS idx_processed_messages_wa_id ON processed_whatsapp_messages(wa_id);
CREATE INDEX IF NOT EXISTS idx_processed_messages_timestamp ON processed_whatsapp_messages(webhook_timestamp);

-- 2. Таблиця для трекінгу доставки вихідних повідомлень
CREATE TABLE IF NOT EXISTS whatsapp_delivery_tracking (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(255),
    recipient VARCHAR(50) NOT NULL,
    template_name VARCHAR(255),
    notification_type VARCHAR(50),
    event_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_description TEXT
);

-- Індекси для запитів
CREATE INDEX IF NOT EXISTS idx_delivery_tracking_recipient ON whatsapp_delivery_tracking(recipient);
CREATE INDEX IF NOT EXISTS idx_delivery_tracking_status ON whatsapp_delivery_tracking(status);
CREATE INDEX IF NOT EXISTS idx_delivery_tracking_event_id ON whatsapp_delivery_tracking(event_id);
CREATE INDEX IF NOT EXISTS idx_delivery_tracking_notification_type ON whatsapp_delivery_tracking(notification_type);
CREATE INDEX IF NOT EXISTS idx_delivery_tracking_sent_at ON whatsapp_delivery_tracking(sent_at);
