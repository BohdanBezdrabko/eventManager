-- V23__add_whatsapp_fields_to_posts.sql
-- Додаємо WhatsApp-специфічні поля до таблиці posts для підтримки:
-- 1. whatsapp_group_id - ID групи для відправки у групу (формат: 120363xxxxx@g.us)
-- 2. whatsapp_personal - флаг, чи відправляти у особисті чати підписників

ALTER TABLE posts
    ADD COLUMN whatsapp_group_id VARCHAR(255),
    ADD COLUMN whatsapp_personal BOOLEAN NOT NULL DEFAULT TRUE;

-- Індекс для швидкого пошуку постів за Group ID
CREATE INDEX IF NOT EXISTS idx_posts_whatsapp_group_id ON posts(whatsapp_group_id);
