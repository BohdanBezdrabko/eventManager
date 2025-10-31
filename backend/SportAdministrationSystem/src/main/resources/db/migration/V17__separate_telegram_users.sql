-- 1) Додати колонку під Telegram-юзера в підписках
ALTER TABLE event_subscriptions
    ADD COLUMN IF NOT EXISTS user_telegram_id BIGINT;

-- 2) Перенести зв'язок з user_id -> user_telegram_id (на основі історичних даних)
UPDATE event_subscriptions es
SET user_telegram_id = ut.id
FROM user_telegram ut
WHERE es.user_telegram_id IS NULL
  AND ut.user_id IS NOT NULL
  AND ut.user_id = es.user_id;

-- 3) Заборонити NULL у новій колонці
ALTER TABLE event_subscriptions
    ALTER COLUMN user_telegram_id SET NOT NULL;

-- 4) Додати FK на user_telegram (без IF NOT EXISTS — бо PostgreSQL не підтримує цього тут)
--    Якщо FK вже існує у твоїй БД, ПЕРЕД повторним прогоном видали його вручну або через repair.
ALTER TABLE event_subscriptions
    ADD CONSTRAINT event_subscriptions_usertelegram_fkey
        FOREIGN KEY (user_telegram_id) REFERENCES user_telegram(id) ON DELETE CASCADE;

-- 5) Зняти стару унікальність (event_id, user_id, messenger) якщо була
ALTER TABLE event_subscriptions
    DROP CONSTRAINT IF EXISTS event_subscriptions_event_id_user_id_messenger_key;

-- 6) Встановити нову унікальність (event_id, user_telegram_id, messenger)
ALTER TABLE event_subscriptions
    ADD CONSTRAINT uq_event_usertelegram_messenger
        UNIQUE (event_id, user_telegram_id, messenger);

-- 7) Індекси: зняти старий, створити новий
DROP INDEX IF EXISTS idx_event_subscriptions_user;
CREATE INDEX IF NOT EXISTS idx_event_subscriptions_usertelegram
    ON event_subscriptions(user_telegram_id, messenger);

-- 8) Прибрати зв'язок і колонку user_id з підписок
ALTER TABLE event_subscriptions
    DROP CONSTRAINT IF EXISTS event_subscriptions_user_id_fkey;

ALTER TABLE event_subscriptions
    DROP COLUMN IF EXISTS user_id;

-- 9) Повністю відв'язати user_telegram від users
ALTER TABLE user_telegram
    DROP CONSTRAINT IF EXISTS user_telegram_user_id_key;

ALTER TABLE user_telegram
    DROP CONSTRAINT IF EXISTS user_telegram_user_id_fkey;

ALTER TABLE user_telegram
    DROP COLUMN IF EXISTS user_id;
