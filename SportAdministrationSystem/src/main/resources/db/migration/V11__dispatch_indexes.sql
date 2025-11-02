-- Індекс для швидкого відбору SCHEDULED постів з дедлайном
CREATE INDEX IF NOT EXISTS idx_posts_sched_publish
    ON posts (publish_at)
    WHERE status = 'SCHEDULED';

-- Композитний індекс під перевірку існування згенерованих постів
-- (event_id, channel, audience, publish_at) часто використовується
CREATE INDEX IF NOT EXISTS idx_posts_exist_check
    ON posts (event_id, channel, audience, publish_at);
