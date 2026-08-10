-- 1. Competitive event windows (e.g. "Austin Turf War August 2026")
CREATE TABLE event_windows (
    event_id SERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CHECK (end_time > start_time)
);

-- 2. Per-user streak state
CREATE TABLE user_streaks (
    user_id INT PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_walk_date DATE
);

-- 3. On every completed walk session: advance the streak and roll up daily_summary.
-- Runs once per INSERT, so two sessions logged on the same calendar day only
-- count once toward the streak but still both accumulate into daily_summary.
CREATE OR REPLACE FUNCTION update_user_streak()
RETURNS TRIGGER AS $$
DECLARE
    today DATE := (NEW.end_time AT TIME ZONE 'UTC')::DATE;
    yesterday DATE := today - INTERVAL '1 day';
    streak_row RECORD;
BEGIN
    INSERT INTO user_streaks (user_id, current_streak, longest_streak, last_walk_date)
    VALUES (NEW.user_id, 0, 0, NULL)
    ON CONFLICT (user_id) DO NOTHING;

    SELECT * INTO streak_row FROM user_streaks WHERE user_id = NEW.user_id;

    IF streak_row.last_walk_date IS DISTINCT FROM today THEN
        IF streak_row.last_walk_date = yesterday THEN
            UPDATE user_streaks
            SET current_streak = current_streak + 1,
                longest_streak = GREATEST(longest_streak, current_streak + 1),
                last_walk_date = today
            WHERE user_id = NEW.user_id;
        ELSE
            -- Either the streak was broken, or this is the user's first ever walk.
            UPDATE user_streaks
            SET current_streak = 1,
                longest_streak = GREATEST(longest_streak, 1),
                last_walk_date = today
            WHERE user_id = NEW.user_id;
        END IF;
    END IF;

    INSERT INTO daily_summary (user_id, summary_date, total_steps, total_aps, total_points)
    VALUES (NEW.user_id, today, NEW.steps_counted, NEW.ap_discovered, NEW.points_earned)
    ON CONFLICT (user_id, summary_date) DO UPDATE
    SET total_steps = daily_summary.total_steps + EXCLUDED.total_steps,
        total_aps = daily_summary.total_aps + EXCLUDED.total_aps,
        total_points = daily_summary.total_points + EXCLUDED.total_points;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_streak
AFTER INSERT ON walk_sessions
FOR EACH ROW
EXECUTE FUNCTION update_user_streak();

-- 4. Leaderboard scoped to a single event window.
-- Usage from the API layer: SELECT * FROM get_event_leaderboard($1);
CREATE OR REPLACE FUNCTION get_event_leaderboard(target_event_id INT)
RETURNS TABLE(username VARCHAR, steps BIGINT, aps BIGINT, score BIGINT) AS $$
BEGIN
    RETURN QUERY
    SELECT
        u.username,
        COALESCE(SUM(s.steps_counted), 0)::BIGINT AS steps,
        COALESCE(SUM(s.ap_discovered), 0)::BIGINT AS aps,
        COALESCE(SUM(s.points_earned), 0)::BIGINT AS score
    FROM event_windows e
    JOIN walk_sessions s ON s.start_time >= e.start_time AND s.end_time <= e.end_time
    JOIN users u ON u.user_id = s.user_id
    WHERE e.event_id = target_event_id
    GROUP BY u.username
    ORDER BY score DESC;
END;
$$ LANGUAGE plpgsql;
