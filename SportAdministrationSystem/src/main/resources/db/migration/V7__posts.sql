CREATE TABLE IF NOT EXISTS posts (
                                     id           BIGSERIAL PRIMARY KEY,
                                     event_id     BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    title        VARCHAR(256) NOT NULL,
    body         TEXT NOT NULL,
    publish_at   TIMESTAMP NOT NULL,
    status       VARCHAR(16) NOT NULL,      -- DRAFT,SCHEDULED,PUBLISHED,FAILED,CANCELED
    audience     VARCHAR(16) NOT NULL,      -- PUBLIC,SUBSCRIBERS
    channel      VARCHAR(16) NOT NULL,      -- TELEGRAM,INTERNAL
    external_id  VARCHAR(128),
    error        TEXT,
    generated    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_posts_event     ON posts(event_id);
CREATE INDEX IF NOT EXISTS idx_posts_publish   ON posts(publish_at);
CREATE INDEX IF NOT EXISTS idx_posts_status    ON posts(status);

CREATE OR REPLACE FUNCTION trg_posts_touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_posts_touch ON posts;
CREATE TRIGGER trg_posts_touch BEFORE UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION trg_posts_touch_updated_at();
