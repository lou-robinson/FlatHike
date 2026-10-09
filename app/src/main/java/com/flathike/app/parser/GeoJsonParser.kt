package com.flathike.app.parser

import com.flathike.app.model.GpsPoint
import com.flathike.app.model.GpsTrack
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Parser for GeoJSON tracks (FeatureCollection, Feature, or direct LineString/MultiLineString).
 */
class GeoJsonParser : GpsTrackParser {

    override fun parse(inputStream: InputStream, defaultName: String): GpsTrack {
        var jsonText = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        // Strip BOM if present
        if (jsonText.startsWith("\uFEFF")) {
            jsonText = jsonText.substring(1)
        }
        jsonText = jsonText.trim()

        val points = mutableListOf<GpsPoint>()
        val waypoints = mutableListOf<com.flathike.app.model.TrackWaypoint>()
        var trackName = defaultName

        if (jsonText.startsWith("[")) {
            try {
                val array = JSONArray(jsonText)
                parseCoordinateArray(array, points)
            } catch (_: Exception) {}
            return GpsTrack(name = defaultName, points = points)
        }

        val root = JSONObject(jsonText)
        val type = root.optString("type", "")

        when (type) {
            "FeatureCollection" -> {
                val features = root.optJSONArray("features") ?: JSONArray()
                for (i in 0 until features.length()) {
                    val feature = features.optJSONObject(i) ?: continue
                    val props = feature.optJSONObject("properties")
                    var featureName = ""
                    if (props != null) {
                        val nameCandidate = props.optString("name", props.optString("title", ""))
                        if (nameCandidate.isNotBlank()) {
                            featureName = nameCandidate
                            if (trackName == defaultName) {
                                trackName = nameCandidate
                            }
                        }
                    }
                    val geometry = feature.optJSONObject("geometry") ?: continue
                    if (geometry.optString("type", "") == "Point") {
                        val coords = geometry.optJSONArray("coordinates")
                        if (coords != null && coords.length() >= 2) {
                            val lon = coords.optDouble(0)
                            val lat = coords.optDouble(1)
                            val ele = if (coords.length() >= 3) coords.optDouble(2, 0.0) else 0.0
                            if (!lon.isNaN() && !lat.isNaN()) {
                                waypoints.add(
                                    com.flathike.app.model.TrackWaypoint(
                                        name = featureName.ifBlank { "Точка ${waypoints.size + 1}" },
                                        latitude = lat,
                                        longitude = lon,
                                        elevation = if (ele.isNaN()) 0.0 else ele
                                    )
                                )
                            }
                        }
                    } else {
                        extractPointsFromGeometry(geometry, points)
                    }
                }
            }
            "Feature" -> {
                val props = root.optJSONObject("properties")
                if (props != null) {
                    val nameCandidate = props.optString("name", props.optString("title", ""))
                    if (nameCandidate.isNotBlank()) trackName = nameCandidate
                }
                val geometry = root.optJSONObject("geometry")
                if (geometry != null) {
                    extractPointsFromGeometry(geometry, points)
                }
            }
            "LineString", "MultiLineString", "GeometryCollection" -> {
                extractPointsFromGeometry(root, points)
            }
        }

        return GpsTrack(
            name = trackName,
            points = points,
            waypoints = waypoints
        )
    }

    private fun extractPointsFromGeometry(geometry: JSONObject, target: MutableList<GpsPoint>) {
        val geomType = geometry.optString("type", "")
        if (geomType == "GeometryCollection") {
            val geometries = geometry.optJSONArray("geometries") ?: return
            for (i in 0 until geometries.length()) {
                val subGeom = geometries.optJSONObject(i) ?: continue
                extractPointsFromGeometry(subGeom, target)
            }
            return
        }

        val coords = geometry.optJSONArray("coordinates") ?: return

        when (geomType) {
            "LineString" -> {
                parseCoordinateArray(coords, target)
            }
            "MultiLineString" -> {
                for (i in 0 until coords.length()) {
                    val line = coords.optJSONArray(i) ?: continue
                    parseCoordinateArray(line, target)
                }
            }
        }
    }

    private fun parseCoordinateArray(coords: JSONArray, target: MutableList<GpsPoint>) {
        for (i in 0 until coords.length()) {
            val pointArr = coords.optJSONArray(i) ?: continue
            if (pointArr.length() >= 2) {
                val lon = pointArr.optDouble(0)
                val lat = pointArr.optDouble(1)
                val ele = if (pointArr.length() >= 3) pointArr.optDouble(2, 0.0) else 0.0

                if (!lon.isNaN() && !lat.isNaN() && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    target.add(GpsPoint(latitude = lat, longitude = lon, elevation = if (ele.isNaN()) 0.0 else ele))
                }
            }
        }
    }
}
