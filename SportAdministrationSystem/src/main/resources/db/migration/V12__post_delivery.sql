-- Лог доставки кожної спроби відправки поста конкретному таргету (чат/канал)
CREATE TABLE IF NOT EXISTS post_delivery (
                                             id           BIGSERIAL PRIMARY KEY,
                                             post_id      BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    target       VARCHAR(255) NOT NULL,            -- chat_id / channel_id
    attempt_no   INT          NOT NULL,            -- 1..N
    status       VARCHAR(16)  NOT NULL,            -- SENT | FAILED
    error        TEXT,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
    );

-- Щоб уникати дублювань записів спроб
CREATE UNIQUE INDEX IF NOT EXISTS ux_post_delivery_unique_attempt
    ON post_delivery (post_id, target, attempt_no);

-- Швидкі вибірки для ретраїв
CREATE INDEX IF NOT EXISTS idx_post_delivery_failed
    ON post_delivery (status, created_at);

-- Аналітика/агрегації
CREATE INDEX IF NOT EXISTS idx_post_delivery_post
    ON post_delivery (post_id);

-- Опційно: матв'юшка під адвайзорі-лок (жодних DDL не треба)
-- SELECT pg_try_advisory_lock(bigint '824221123'); SELECT pg_advisory_unlock(bigint '824221123');
