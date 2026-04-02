-- V33__ensure_clean_schema.sql
-- Убедиться что все таблицы имеют правильную структуру
-- и что конфликтующие колны удалены

-- 1. Убедиться что event_subscriptions имеет правильный unique constraint
DO $$
BEGIN
  -- Удалить старый constraint если существует
  ALTER TABLE event_subscriptions
    DROP CONSTRAINT IF EXISTS uq_event_userTelegram_messenger;

  -- Добавить правильный constraint если не существует
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_name = 'event_subscriptions' AND constraint_name = 'uq_event_user_telegram'
  ) THEN
    ALTER TABLE event_subscriptions
      ADD CONSTRAINT uq_event_user_telegram UNIQUE (event_id, user_telegram_id);
  END IF;
END $$;

-- 2. Убедиться что posts не имеет deprecated колон
ALTER TABLE posts DROP COLUMN IF EXISTS telegram_chat_id;
ALTER TABLE posts DROP COLUMN IF EXISTS whatsapp_personal;
ALTER TABLE posts DROP COLUMN IF EXISTS whatsapp_group_id;

-- 3. Убедиться что event_subscriptions_whatsapp имеет правильные колны
ALTER TABLE event_subscriptions_whatsapp
  ADD COLUMN IF NOT EXISTS reminder_72h_sent BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS reminder_24h_sent BOOLEAN NOT NULL DEFAULT FALSE;

-- 4. Создать индекс для напоминаний если не существует
CREATE INDEX IF NOT EXISTS ix_esw_reminder_72h
  ON event_subscriptions_whatsapp (event_id, active, reminder_72h_sent)
  WHERE active = TRUE AND reminder_72h_sent = FALSE;

CREATE INDEX IF NOT EXISTS ix_esw_reminder_24h
  ON event_subscriptions_whatsapp (event_id, active, reminder_24h_sent)
  WHERE active = TRUE AND reminder_24h_sent = FALSE;

-- Логирование
-- Migration V33: Ensured clean schema - removed deprecated fields, verified structure
