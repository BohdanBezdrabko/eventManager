-- V26__whatsapp_templates.sql
-- Таблиця для збереження Meta-approved WhatsApp шаблонів

CREATE TABLE IF NOT EXISTS whatsapp_templates (
    id BIGSERIAL PRIMARY KEY,
    notification_type VARCHAR(50) NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    language_code VARCHAR(10) NOT NULL DEFAULT 'uk',
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_notification_type UNIQUE (notification_type)
);

-- Індекс для пошуку по типу сповіщення
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_notification_type ON whatsapp_templates(notification_type);

-- Індекс для пошуку по імʼю шаблону
CREATE INDEX IF NOT EXISTS idx_whatsapp_templates_name ON whatsapp_templates(template_name);

-- Початкові значення шаблонів (Meta-approved)
INSERT INTO whatsapp_templates (notification_type, template_name, language_code, description, enabled)
VALUES
    ('SUBSCRIPTION_CONFIRM', 'event_subscription_confirm', 'uk', 'Підтвердження підписки на івент', TRUE),
    ('REGISTRATION_CONFIRM', 'event_registration_confirm', 'uk', 'Підтвердження реєстрації на івент', TRUE),
    ('REMINDER_72H', 'event_reminder_72h', 'uk', 'Нагадування за 72 години до подій', TRUE),
    ('REMINDER_24H', 'event_reminder_24h', 'uk', 'Нагадування за 24 години до подій', TRUE),
    ('EVENT_UPDATED', 'event_updated', 'uk', 'Сповіщення про оновлення івенту', TRUE),
    ('EVENT_CANCELLED', 'event_cancelled', 'uk', 'Сповіщення про скасування івенту', TRUE)
ON CONFLICT (notification_type) DO NOTHING;
