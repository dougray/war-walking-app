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
    val createdAt: Long = System.currentTimeMillis(),
    // "lat,lon;lat,lon;..." - a location breadcrumb sampled during the walk,
    // not the per-AP scan coordinates (those only ever live in the transient
    // WigleWifi CSV). Plain string, not a child table: this is a cheap route
    // sketch, not a real map, so there's nothing here worth a join over.
    val routePoints: String? = null
)

val WalkSessionEntity.pointsEarned: Long
    get() = stepsCounted.toLong() * apDiscovered.toLong()

fun encodeRoutePoints(points: List<Pair<Double, Double>>): String? =
    points.takeIf { it.isNotEmpty() }?.joinToString(";") { "${it.first},${it.second}" }

fun WalkSessionEntity.decodeRoutePoints(): List<Pair<Double, Double>> =
    routePoints?.split(";")?.mapNotNull { pair ->
        val parts = pair.split(",")
        val lat = parts.getOrNull(0)?.toDoubleOrNull()
        val lon = parts.getOrNull(1)?.toDoubleOrNull()
        if (lat != null && lon != null) lat to lon else null
    } ?: emptyList()
