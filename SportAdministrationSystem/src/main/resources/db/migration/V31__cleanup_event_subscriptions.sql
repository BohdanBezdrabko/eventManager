-- V32__cleanup_event_subscriptions.sql
-- Видалити messenger поле з event_subscriptions та очистити логіку

-- 1. Видалити messenger колону якщо існує
ALTER TABLE event_subscriptions DROP COLUMN IF EXISTS messenger;

-- 2. Видалити unique constraint який залежав від messenger (якщо існує)
ALTER TABLE event_subscriptions
  DROP CONSTRAINT IF EXISTS uq_event_userTelegram_messenger;

-- 3. Добавити новий unique constraint без messenger (якщо його немає)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_name = 'event_subscriptions' AND constraint_name = 'uq_event_user_telegram'
  ) THEN
    ALTER TABLE event_subscriptions
      ADD CONSTRAINT uq_event_user_telegram UNIQUE (event_id, user_telegram_id);
  END IF;
END $$;

-- Логування
-- Migration V32: Cleaned up EventSubscription (removed messenger field)
-- Now using separate tables: EventSubscription (Telegram) and EventSubscriptionWhatsapp (WhatsApp)
