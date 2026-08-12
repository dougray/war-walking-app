package com.warwalking.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Recomputes the streak from full session history each call rather than
 * maintaining incremental state - meaning it correctly shows 0 once more
 * than a day has passed since the last walk, instead of staying frozen at
 * whatever it was when a session was last inserted (a real bug in the
 * original Postgres trigger this replaces: without a new INSERT, that
 * streak value never decayed on its own).
 */
object StreakCalculator {
    fun currentStreak(sessions: List<WalkSessionEntity>, zone: ZoneId = ZoneId.systemDefault()): Int {
        val days = sessions.map { Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate() }.toSet()
        if (days.isEmpty()) return 0

        val today = LocalDate.now(zone)
        val mostRecent = days.max()
        if (mostRecent.isBefore(today.minusDays(1))) return 0

        var streak = 0
        var cursor = mostRecent
        while (days.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
