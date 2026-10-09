package com.flathike.app.model

/**
 * Standard categories of terrain slope / incline gradient.
 */
enum class SlopeCategory(
    val minSlopePct: Double,
    val maxSlopePct: Double
) {
    STEEP_ASCENT(15.0, Double.POSITIVE_INFINITY),
    MODERATE_ASCENT(5.0, 15.0),
    FLAT(-5.0, 5.0),
    MODERATE_DESCENT(-15.0, -5.0),
    STEEP_DESCENT(Double.NEGATIVE_INFINITY, -15.0);

    companion object {
        fun fromSlopePercent(slopePct: Double): SlopeCategory = when {
            slopePct > 15.0 -> STEEP_ASCENT
            slopePct in 5.0..15.0 -> MODERATE_ASCENT
            slopePct in -5.0..5.0 -> FLAT
            slopePct in -15.0..-5.0 -> MODERATE_DESCENT
            else -> STEEP_DESCENT
        }
    }
}

/**
 * Aggregated metrics for all track sections within a specific slope category.
 */
data class SlopeSpeedSummary(
    val category: SlopeCategory,
    val totalDistanceKm: Double,
    val percentageOfTrack: Double,
    val totalAscentMeters: Double,
    val totalDescentMeters: Double,
    val durationSeconds: Long,
    val avgSpeedKmH: Double,
    val isRealGpsSpeed: Boolean
)

/**
 * A discrete logical segment of the track (e.g. split by slope, by distance, or by waypoints).
 */
data class TrackSegment(
    val index: Int,
    val name: String,
    val startDistanceKm: Double,
    val endDistanceKm: Double,
    val distanceKm: Double,
    val startElevationM: Double,
    val endElevationM: Double,
    val elevationChangeM: Double,
    val avgSlopePercent: Double,
    val slopeCategory: SlopeCategory,
    val durationSeconds: Long,
    val avgSpeedKmH: Double,
    val isRealGpsSpeed: Boolean,
    val fromPointName: String? = null,
    val toPointName: String? = null
) {
    /**
     * Returns a localized display name for the segment according to the current UI language.
     */
    fun getDisplayName(appStrings: com.flathike.app.ui.AppStrings): String {
        return when {
            fromPointName != null && toPointName != null -> {
                val from = if (fromPointName == "Старт" || fromPointName.equals("Start", ignoreCase = true)) {
                    appStrings.start
                } else fromPointName
                val to = if (toPointName == "Финиш" || toPointName.equals("Finish", ignoreCase = true)) {
                    appStrings.finish
                } else toPointName
                "$from → $to"
            }
            name.startsWith("Участок") || name.startsWith("Segment") -> {
                "${appStrings.segmentPrefix} $index"
            }
            name.contains("–") || name.contains("-") -> {
                "${String.format(java.util.Locale.US, "%.1f", startDistanceKm)}–${String.format(java.util.Locale.US, "%.1f", endDistanceKm)} ${appStrings.km}"
            }
            else -> name
        }
    }
}

/**
 * Strategy for partitioning the track into logical parts.
 */
enum class TrackSplitMode {
    BY_SLOPE,
    BY_DISTANCE_1KM,
    BY_DISTANCE_5KM,
    BY_WAYPOINTS
}
