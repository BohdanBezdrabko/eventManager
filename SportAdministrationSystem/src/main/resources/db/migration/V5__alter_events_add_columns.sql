-- V5__alter_events_add_columns.sql
ALTER TABLE events ADD COLUMN IF NOT EXISTS start_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE events ADD COLUMN IF NOT EXISTS capacity INTEGER;
ALTER TABLE events ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS cover_url TEXT;

CREATE INDEX IF NOT EXISTS idx_events_start_at ON events(start_at);
