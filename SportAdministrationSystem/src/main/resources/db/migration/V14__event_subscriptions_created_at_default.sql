-- Проставимо дефолт і виправимо існуючі null
ALTER TABLE event_subscriptions
    ALTER COLUMN created_at SET DEFAULT NOW();

UPDATE event_subscriptions
SET created_at = NOW()
WHERE created_at IS NULL;
