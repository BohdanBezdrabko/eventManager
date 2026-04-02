-- V25__remove_whatsapp_groups_and_cleanup.sql
-- Видалити групо-орієнтовану логіку для WhatsApp (не підтримується Cloud API)

-- 1. Видалити поле whatsapp_group_id з таблиці posts
ALTER TABLE posts DROP COLUMN IF EXISTS whatsapp_group_id;

-- 2. Видалити таблиці для групо-орієнтованої логіки (якщо вони існують)
DROP TABLE IF EXISTS whatsapp_group_bind_codes;
DROP TABLE IF EXISTS whatsapp_event_groups;
