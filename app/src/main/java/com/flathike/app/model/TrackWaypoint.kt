package com.flathike.app.model

/**
 * Represents a marked intermediate waypoint or point of interest along a track.
 *
 * @param name Display name or label (e.g. "Campsite", "Spring", "Summit")
 * @param latitude Latitude in degrees [-90.0, 90.0]
 * @param longitude Longitude in degrees [-180.0, 180.0]
 * @param elevation Altitude above sea level in meters
 * @param description Optional description or notes
 * @param distanceKm Distance along the track from the start in kilometers
 */
data class TrackWaypoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double = 0.0,
    val description: String? = null,
    val distanceKm: Double = 0.0
)
