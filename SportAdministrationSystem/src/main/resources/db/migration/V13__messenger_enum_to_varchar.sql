-- Якщо є дефолт на колонці цього enum — спершу приберіть
ALTER TABLE event_subscriptions
    ALTER COLUMN messenger DROP DEFAULT;

-- Переведення enum->varchar(16)
ALTER TABLE event_subscriptions
ALTER COLUMN messenger TYPE varchar(16) USING messenger::text;

-- (Необов'язково) захисний чек, щоб не зіпсувати дані в майбутньому
ALTER TABLE event_subscriptions
    ADD CONSTRAINT chk_event_subscriptions_messenger
        CHECK (messenger IN ('TELEGRAM'));

-- Після того як жодна колонка не використовує тип — можна прибрати тип
DROP TYPE IF EXISTS messenger;
