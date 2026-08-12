-- Social feed layer. Deliberately keeps scanned network data (SSIDs, MACs,
-- per-AP coordinates) out of anything public - that data already goes to
-- WiGLE; the feed only ever surfaces aggregate stats (steps/points/AP count)
-- plus whatever caption the walker chose to write.

ALTER TABLE walk_sessions
    ADD COLUMN title VARCHAR(140),
    ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT true;

CREATE TABLE kudos (
    kudos_id SERIAL PRIMARY KEY,
    session_id INT NOT NULL REFERENCES walk_sessions(session_id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (session_id, user_id)
);
CREATE INDEX idx_kudos_session_id ON kudos(session_id);

CREATE TABLE session_comments (
    comment_id SERIAL PRIMARY KEY,
    session_id INT NOT NULL REFERENCES walk_sessions(session_id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    body VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_session_comments_session_id ON session_comments(session_id);
