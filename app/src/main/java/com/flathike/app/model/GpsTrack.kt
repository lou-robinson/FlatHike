package com.flathike.app.model

/**
 * A parsed GPS track consisting of points and metadata.
 */
data class GpsTrack(
    val name: String,
    val points: List<GpsPoint>,
    val description: String? = null,
    val waypoints: List<TrackWaypoint> = emptyList()
)
