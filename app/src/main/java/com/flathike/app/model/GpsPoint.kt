package com.flathike.app.model

/**
 * Represents a single GPS point with coordinates and altitude above sea level.
 *
 * @param latitude Latitude in degrees [-90.0, 90.0]
 * @param longitude Longitude in degrees [-180.0, 180.0]
 * @param elevation Altitude above sea level in meters (WGS-84 geoid/ellipsoid)
 * @param time Epoch timestamp in milliseconds (optional, null if absent)
 * @param temperatureCelsius Ambient air temperature in degrees Celsius (optional, null if absent)
 */
data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double = 0.0,
    val time: Long? = null,
    val temperatureCelsius: Double? = null
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90, got: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180, got: $longitude" }
    }
}
