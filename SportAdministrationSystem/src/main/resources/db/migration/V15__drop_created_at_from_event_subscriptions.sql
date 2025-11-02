-- Видаляємо колонку created_at з таблиці підписок
ALTER TABLE event_subscriptions
DROP COLUMN IF EXISTS created_at;
