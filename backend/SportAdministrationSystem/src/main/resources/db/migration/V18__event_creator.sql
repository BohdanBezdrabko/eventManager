-- Додаємо поле автора події
ALTER TABLE events ADD COLUMN IF NOT EXISTS created_by BIGINT;

-- Індекс + FK
CREATE INDEX IF NOT EXISTS idx_events_created_by ON events(created_by);
ALTER TABLE events
    ADD CONSTRAINT fk_events_created_by
        FOREIGN KEY (created_by) REFERENCES users(id)
            ON UPDATE CASCADE ON DELETE SET NULL;
