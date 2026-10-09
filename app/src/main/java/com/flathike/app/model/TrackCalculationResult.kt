package com.flathike.app.model

/**
 * Complete analysis result of a GPS track, including:
 * 1) Straight-line distance at sea level (start -> finish).
 * 2) 3D straight-line chord distance.
 * 3) Cumulative track distance at sea level.
 * 4) Cumulative 3D terrain distance.
 * 5) Elevation gains/losses, min/max altitude, slopes.
 * 6) Elevation profile points for chart plotting.
 * 7) Hiking time estimates (Tobler function and Naismith rule).
 */
data class TrackCalculationResult(
    val trackName: String,
    val pointCount: Int,
    val startPoint: GpsPoint,
    val endPoint: GpsPoint,

    // Straight-line metrics (Start to Finish)
    val straightLineSeaLevelKm: Double,
    val straightLine3dKm: Double,
    val straightLineSeaLevelChordKm: Double,

    // Route shape & extent metrics
    val isClosedLoop: Boolean = false,
    val maxDistanceFromStartKm: Double = 0.0,
    val furthestPoint: GpsPoint = endPoint,
    val highestPoint: GpsPoint = endPoint,
    val lowestPoint: GpsPoint = startPoint,

    // Track path metrics
    val trackLengthSeaLevelKm: Double,
    val trackLength3dKm: Double,
    val terrainExtensionDifferenceMeters: Double,
    val geodeticReductionMeters: Double,

    // Elevation & Slope
    val totalAscentMeters: Double,
    val totalDescentMeters: Double,
    val minElevationMeters: Double,
    val maxElevationMeters: Double,
    val avgElevationMeters: Double,
    val elevationDifferenceMeters: Double,
    val maxSlopePercent: Double,
    val avgSlopePercent: Double,

    // Elevation profile & DEM detection
    val isElevationMissing: Boolean = false,
    val elevationProfile: List<ElevationProfilePoint>,

    // Estimated walking times in minutes
    val toblerHikingTimeMinutes: Double,
    val naismithHikingTimeMinutes: Double,

    // Recorded sensor & time metrics (optional, if present in track file)
    val recordedTimeMetrics: RecordedTimeMetrics? = null,
    val recordedTemperatureMetrics: RecordedTemperatureMetrics? = null,

    // Intermediate points / waypoints along track
    val waypoints: List<TrackWaypoint> = emptyList(),

    // Slope analysis and segmentation
    val slopeSpeedAnalysis: List<SlopeSpeedSummary> = emptyList(),
    val slopeSegments: List<TrackSegment> = emptyList(),
    val distance1KmSegments: List<TrackSegment> = emptyList(),
    val distance5KmSegments: List<TrackSegment> = emptyList(),
    val waypointSegments: List<TrackSegment> = emptyList()
) {
    val hasRecordedTime: Boolean get() = recordedTimeMetrics != null
    val hasRecordedTemperature: Boolean get() = recordedTemperatureMetrics != null
    val hasWaypoints: Boolean get() = waypoints.isNotEmpty()

    fun getSegmentsForMode(mode: TrackSplitMode): List<TrackSegment> = when (mode) {
        TrackSplitMode.BY_SLOPE -> slopeSegments
        TrackSplitMode.BY_DISTANCE_1KM -> distance1KmSegments
        TrackSplitMode.BY_DISTANCE_5KM -> distance5KmSegments
        TrackSplitMode.BY_WAYPOINTS -> waypointSegments
    }
    /**
     * Calculates the effort-equivalent flat distance for a given climb coefficient.
     *
     * @param ascentCoefficient km of flat equivalent added per 1000m of ascent (e.g. 10.0 for SAC)
     * @param useTrackLength if true, adds to track length; if false, adds to straight-line distance
     */
    fun calculateEffectiveDistance(
        ascentCoefficient: Double,
        useTrackLength: Boolean = true
    ): EffectiveDistanceResult {
        val baseDistanceKm = if (useTrackLength) {
            trackLengthSeaLevelKm
        } else {
            if (isClosedLoop) maxDistanceFromStartKm else straightLineSeaLevelKm
        }
        val ascentBonusKm = (totalAscentMeters / 1000.0) * ascentCoefficient
        val effectiveTotalKm = baseDistanceKm + ascentBonusKm
        return EffectiveDistanceResult(
            baseDistanceKm = baseDistanceKm,
            ascentBonusKm = ascentBonusKm,
            effectiveDistanceKm = effectiveTotalKm,
            ascentCoefficient = ascentCoefficient
        )
    }
}

/**
 * Result of applying an elevation coefficient to a distance.
 */
data class EffectiveDistanceResult(
    val baseDistanceKm: Double,
    val ascentBonusKm: Double,
    val effectiveDistanceKm: Double,
    val ascentCoefficient: Double
)

/**
 * Metrics derived from actual GPS timestamps in the recorded track.
 */
data class RecordedTimeMetrics(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val totalDurationSeconds: Long,
    val movingDurationSeconds: Long,
    val stoppedDurationSeconds: Long,
    val avgSpeedKmH: Double,
    val avgMovingSpeedKmH: Double,
    val maxSpeedKmH: Double,
    val paceMinutesPerKm: Double
)

/**
 * Metrics derived from temperature sensors recorded in the track.
 */
data class RecordedTemperatureMetrics(
    val avgCelsius: Double,
    val minCelsius: Double,
    val maxCelsius: Double,
    val startCelsius: Double,
    val endCelsius: Double
)
