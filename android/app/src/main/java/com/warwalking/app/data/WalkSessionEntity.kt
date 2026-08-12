package com.warwalking.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A completed walk, stored entirely on-device. This is the whole "database"
 * now that there's no backend - steps/APs/points/streaks all live here
 * instead of a hosted Postgres table.
 */
@Entity(tableName = "walk_sessions")
data class WalkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long, // epoch millis
    val endTime: Long,
    val stepsCounted: Int,
    val apDiscovered: Int,
    val title: String? = null,
    val wigleTransId: String? = null, // null until (if ever) the WiGLE upload succeeds
    val createdAt: Long = System.currentTimeMillis()
)

val WalkSessionEntity.pointsEarned: Long
    get() = stepsCounted.toLong() * apDiscovered.toLong()
