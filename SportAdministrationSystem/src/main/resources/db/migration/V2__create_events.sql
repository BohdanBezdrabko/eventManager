CREATE TABLE events (
                        id BIGSERIAL PRIMARY KEY,
                        name TEXT NOT NULL,
                        location TEXT NOT NULL,
    -- додайте ваші поля дат/лімітів
                        created_at TIMESTAMP DEFAULT NOW() NOT NULL,
                        registered_count INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_events_name_ci ON events ((lower(name)));
CREATE INDEX idx_events_location_ci ON events ((lower(location)));
