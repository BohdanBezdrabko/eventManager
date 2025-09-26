-- лічильник уже ініціалізовано колонкою за замовчуванням; якщо треба перерахувати:
UPDATE events e
SET registered_count = sub.cnt
    FROM (
  SELECT event_id, COUNT(*)::int AS cnt
  FROM user_events
  GROUP BY event_id
) sub
WHERE e.id = sub.event_id;

CREATE OR REPLACE FUNCTION trg_user_events_change() RETURNS trigger AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
UPDATE events SET registered_count = registered_count + 1 WHERE id = NEW.event_id;
RETURN NEW;
ELSIF TG_OP = 'DELETE' THEN
UPDATE events SET registered_count = registered_count - 1 WHERE id = OLD.event_id;
RETURN OLD;
ELSIF TG_OP = 'UPDATE' THEN
    IF NEW.event_id <> OLD.event_id THEN
UPDATE events SET registered_count = registered_count - 1 WHERE id = OLD.event_id;
UPDATE events SET registered_count = registered_count + 1 WHERE id = NEW.event_id;
END IF;
RETURN NEW;
END IF;
RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS user_events_ai ON user_events;
DROP TRIGGER IF EXISTS user_events_ad ON user_events;
DROP TRIGGER IF EXISTS user_events_au ON user_events;

CREATE TRIGGER user_events_ai AFTER INSERT ON user_events
    FOR EACH ROW EXECUTE FUNCTION trg_user_events_change();

CREATE TRIGGER user_events_ad AFTER DELETE ON user_events
    FOR EACH ROW EXECUTE FUNCTION trg_user_events_change();

CREATE TRIGGER user_events_au AFTER UPDATE ON user_events
    FOR EACH ROW EXECUTE FUNCTION trg_user_events_change();
