CREATE TABLE user_events (
                             id BIGSERIAL PRIMARY KEY,
                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                             registration_date DATE NOT NULL DEFAULT CURRENT_DATE,
                             CONSTRAINT uq_user_event UNIQUE (user_id, event_id)
);

CREATE INDEX idx_user_events_user ON user_events(user_id);
CREATE INDEX idx_user_events_event ON user_events(event_id);
