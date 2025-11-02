-- Відновлення лічильника events.registered_count через зміни у user_events.

-- 1) Разовий перерахунок на старті міграції
UPDATE events e
SET registered_count = COALESCE(sub.cnt, 0)
FROM (
         SELECT event_id, COUNT(*)::int AS cnt
         FROM user_events
         GROUP BY event_id
     ) AS sub
WHERE e.id = sub.event_id;

UPDATE events e
SET registered_count = 0
WHERE NOT EXISTS (SELECT 1 FROM user_events ue WHERE ue.event_id = e.id);

-- 2) Функція тригера
CREATE OR REPLACE FUNCTION trg_user_events_change() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE events SET registered_count = registered_count + 1
        WHERE id = NEW.event_id;
        RETURN NEW;

    ELSIF TG_OP = 'DELETE' THEN
        UPDATE events SET registered_count = GREATEST(registered_count - 1, 0)
        WHERE id = OLD.event_id;
        RETURN OLD;

    ELSIF TG_OP = 'UPDATE' THEN
        IF NEW.event_id <> OLD.event_id THEN
            UPDATE events SET registered_count = GREATEST(registered_count - 1, 0)
            WHERE id = OLD.event_id;
            UPDATE events SET registered_count = registered_count + 1
            WHERE id = NEW.event_id;
        END IF;
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 3) (Пере)створення тригерів
DROP TRIGGER IF EXISTS user_events_ai ON user_events;
DROP TRIGGER IF EXISTS user_events_ad ON user_events;
DROP TRIGGER IF EXISTS user_events_au ON user_events;

CREATE TRIGGER user_events_ai AFTER INSERT ON user_events
    FOR EACH ROW EXECUTE FUNCTION trg_user_events_change();

CREATE TRIGGER user_events_ad AFTER DELETE ON user_events
    FOR EACH ROW EXECUTE FUNCTION trg_user_events_change();

CREATE TRIGGER user_events_au AFTER UPDATE ON user_events
    FOR EACH ROW EXECUTE FUNCTION trg_user_events_change();
