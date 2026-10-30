CREATE TABLE IF NOT EXISTS user_interactions (
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    max_weight DOUBLE PRECISION NOT NULL,
    "timestamp" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, event_id)
);

CREATE INDEX IF NOT EXISTS idx_user_interactions_user_id ON user_interactions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_interactions_timestamp ON user_interactions("timestamp" DESC);

CREATE TABLE IF NOT EXISTS event_similarities (
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    "timestamp" TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (event_a, event_b)
);

CREATE INDEX IF NOT EXISTS idx_event_similarities_event_a ON event_similarities(event_a);
CREATE INDEX IF NOT EXISTS idx_event_similarities_event_b ON event_similarities(event_b);