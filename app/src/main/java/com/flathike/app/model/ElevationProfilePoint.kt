package com.flathike.app.model

/**
 * A point in the elevation profile chart along the track.
 *
 * @param distanceKm Cumulative distance from track start in kilometers
 * @param elevationMeters Altitude at this point in meters
 */
data class ElevationProfilePoint(
    val distanceKm: Double,
    val elevationMeters: Double
)
