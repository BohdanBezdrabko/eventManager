-- V30__cleanup_post_fields.sql
-- Видалити непотрібні поля з таблиці posts

-- 1. Видалити дані де channel=WHATSAPP і є telegram_chat_id
UPDATE posts SET telegram_chat_id = NULL WHERE channel = 'WHATSAPP' AND telegram_chat_id IS NOT NULL;

-- 2. Видалити колону telegram_chat_id
ALTER TABLE posts DROP COLUMN IF EXISTS telegram_chat_id;

-- 3. Видалити колону whatsapp_personal (все дорівнює true)
ALTER TABLE posts DROP COLUMN IF EXISTS whatsapp_personal;

-- Логування
-- Migration V30: Cleaned up unnecessary Post fields (telegram_chat_id, whatsapp_personal)
