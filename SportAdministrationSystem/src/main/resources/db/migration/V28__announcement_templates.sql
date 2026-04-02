-- V28__announcement_templates.sql
-- Таблиця для збереження CMS шаблонів оголошень для ручної публікації у групи

CREATE TABLE IF NOT EXISTS announcement_templates (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    template_title VARCHAR(255) NOT NULL,
    template_body TEXT NOT NULL,
    channel VARCHAR(50) NOT NULL DEFAULT 'WHATSAPP',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_announcement_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

-- Індекси
CREATE INDEX IF NOT EXISTS idx_announcement_templates_event_id ON announcement_templates(event_id);
CREATE INDEX IF NOT EXISTS idx_announcement_templates_channel ON announcement_templates(channel);
CREATE INDEX IF NOT EXISTS idx_announcement_templates_enabled ON announcement_templates(enabled);
