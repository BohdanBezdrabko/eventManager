-- Category як ENUM-рядок у таблиці events
ALTER TABLE events ADD COLUMN IF NOT EXISTS category VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_events_category ON events(category);

-- Tags + зв’язка
CREATE TABLE IF NOT EXISTS tags (
                                    id   BIGSERIAL PRIMARY KEY,
                                    name VARCHAR(128) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS event_tags (
                                          event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    tag_id   BIGINT NOT NULL REFERENCES tags(id)   ON DELETE CASCADE,
    CONSTRAINT pk_event_tags PRIMARY KEY (event_id, tag_id)
    );

-- Допоміжні індекси
CREATE INDEX IF NOT EXISTS idx_event_tags_event ON event_tags(event_id);
CREATE INDEX IF NOT EXISTS idx_event_tags_tag   ON event_tags(tag_id);
