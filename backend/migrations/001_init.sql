-- 1. Users
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    wigle_api_name VARCHAR(100) NOT NULL,
    wigle_api_token_encrypted TEXT NOT NULL, -- Fernet ciphertext, never plaintext. See app/crypto.py
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Individual walking & scanning sessions
CREATE TABLE walk_sessions (
    session_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    steps_counted INT NOT NULL DEFAULT 0 CHECK (steps_counted >= 0),
    ap_discovered INT NOT NULL DEFAULT 0 CHECK (ap_discovered >= 0), -- total Wi-Fi/BT networks seen
    wigle_file_id VARCHAR(50), -- transid returned by WiGLE after upload
    points_earned INT GENERATED ALWAYS AS (steps_counted * ap_discovered) STORED,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CHECK (end_time > start_time)
);

CREATE INDEX idx_walk_sessions_user_id ON walk_sessions(user_id);
CREATE INDEX idx_walk_sessions_start_time ON walk_sessions(start_time);

-- 3. Daily aggregates (fitness rings / streak history)
CREATE TABLE daily_summary (
    summary_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    summary_date DATE NOT NULL,
    total_steps INT NOT NULL DEFAULT 0,
    total_aps INT NOT NULL DEFAULT 0,
    total_points INT NOT NULL DEFAULT 0,
    UNIQUE(user_id, summary_date)
);

-- 4. All-time leaderboard, calculated live from session history
CREATE VIEW leaderboard AS
SELECT
    u.user_id,
    u.username,
    COALESCE(SUM(s.steps_counted), 0) AS total_steps,
    COALESCE(SUM(s.ap_discovered), 0) AS total_aps_mapped,
    COALESCE(SUM(s.points_earned), 0) AS total_score,
    COUNT(s.session_id) AS total_walks
FROM users u
LEFT JOIN walk_sessions s ON u.user_id = s.user_id
GROUP BY u.user_id, u.username
ORDER BY total_score DESC;
