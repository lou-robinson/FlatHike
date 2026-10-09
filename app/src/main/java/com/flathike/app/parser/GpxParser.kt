package com.flathike.app.parser

import com.flathike.app.model.GpsPoint
import com.flathike.app.model.GpsTrack
import com.flathike.app.model.TrackWaypoint
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

/**
 * High-performance streaming SAX parser for GPX (GPS Exchange Format) files.
 * Supports <trkpt>, <rtept>, and <wpt> tags, along with elevation, intermediate waypoints, and track names.
 */
class GpxParser : GpsTrackParser {

    override fun parse(inputStream: InputStream, defaultName: String): GpsTrack {
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            try {
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            } catch (_: Exception) {}
        }
        val saxParser = factory.newSAXParser()
        val handler = GpxSaxHandler(defaultName)

        val inputSource = InputSource(inputStream).apply {
            encoding = "UTF-8"
        }
        saxParser.parse(inputSource, handler)

        val resolvedPoints = when {
            handler.trkPoints.isNotEmpty() -> handler.trkPoints
            handler.rtePoints.isNotEmpty() -> handler.rtePoints
            else -> handler.wptPoints
        }

        return GpsTrack(
            name = handler.trackName.ifBlank { defaultName },
            points = resolvedPoints,
            description = handler.description.ifBlank { null },
            waypoints = handler.parsedWaypoints
        )
    }

    private class GpxSaxHandler(val fallbackName: String) : DefaultHandler() {
        val trkPoints = mutableListOf<GpsPoint>()
        val rtePoints = mutableListOf<GpsPoint>()
        val wptPoints = mutableListOf<GpsPoint>()
        val parsedWaypoints = mutableListOf<TrackWaypoint>()

        var trackName: String = fallbackName
        var description: String = ""

        private var currentPointType: String? = null // "trkpt", "rtept", "wpt"
        private var currentLat: Double? = null
        private var currentLon: Double? = null
        private var currentEle: Double = 0.0
        private var currentTime: Long? = null
        private var currentTemp: Double? = null
        private var currentPointName: String? = null
        private var currentPointDesc: String? = null
        private var hasPointEle = false
        private var lastKnownEle: Double? = null

        private var inName = false
        private var inDesc = false
        private var inPointName = false
        private var inPointDesc = false
        private var inEle = false
        private var inTime = false
        private var inTemp = false
        private val textBuffer = StringBuilder()

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes?
        ) {
            val tag = (localName?.ifEmpty { null } ?: qName ?: "").lowercase()
            textBuffer.setLength(0)

            when (tag) {
                "trkpt", "rtept", "wpt" -> {
                    currentPointType = tag
                    currentEle = lastKnownEle ?: 0.0
                    currentTime = null
                    currentTemp = null
                    currentPointName = null
                    currentPointDesc = null
                    hasPointEle = false

                    var latStr = attributes?.getValue("", "lat") ?: attributes?.getValue("lat")
                    var lonStr = attributes?.getValue("", "lon") ?: attributes?.getValue("lon")

                    // Fallback iteration across attributes in case of namespace prefixes
                    if ((latStr == null || lonStr == null) && attributes != null) {
                        for (i in 0 until attributes.length) {
                            val attrName = (attributes.getLocalName(i).ifEmpty { attributes.getQName(i) }).lowercase()
                            if (attrName == "lat" && latStr == null) latStr = attributes.getValue(i)
                            if (attrName == "lon" && lonStr == null) lonStr = attributes.getValue(i)
                        }
                    }

                    currentLat = latStr?.trim()?.replace(',', '.')?.toDoubleOrNull()
                    currentLon = lonStr?.trim()?.replace(',', '.')?.toDoubleOrNull()
                }
                "ele" -> {
                    if (currentPointType != null) inEle = true
                }
                "time" -> {
                    if (currentPointType != null) inTime = true
                }
                "atemp", "temp", "temperature", "gpxtpx:atemp" -> {
                    if (currentPointType != null) inTemp = true
                }
                "name" -> {
                    if (currentPointType != null) inPointName = true
                    else if (trackName == fallbackName) inName = true
                }
                "desc" -> {
                    if (currentPointType != null) inPointDesc = true
                    else inDesc = true
                }
            }
        }

        override fun characters(ch: CharArray?, start: Int, length: Int) {
            if (ch != null) {
                textBuffer.append(ch, start, length)
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            val tag = (localName?.ifEmpty { null } ?: qName ?: "").lowercase()

            when (tag) {
                "ele" -> {
                    if (currentPointType != null && inEle) {
                        val parsed = textBuffer.toString().trim().replace(',', '.').toDoubleOrNull()
                        if (parsed != null) {
                            currentEle = parsed
                            lastKnownEle = parsed
                            hasPointEle = true
                        }
                        inEle = false
                    }
                }
                "time" -> {
                    if (currentPointType != null && inTime) {
                        currentTime = parseIso8601ToEpoch(textBuffer.toString())
                        inTime = false
                    }
                }
                "atemp", "temp", "temperature", "gpxtpx:atemp" -> {
                    if (currentPointType != null && inTemp) {
                        val parsed = textBuffer.toString().trim().replace(',', '.').toDoubleOrNull()
                        if (parsed != null) {
                            currentTemp = parsed
                        }
                        inTemp = false
                    }
                }
                "name" -> {
                    if (inPointName) {
                        currentPointName = textBuffer.toString().trim()
                        inPointName = false
                    } else if (inName) {
                        trackName = textBuffer.toString().trim()
                        inName = false
                    }
                }
                "desc" -> {
                    if (inPointDesc) {
                        currentPointDesc = textBuffer.toString().trim()
                        inPointDesc = false
                    } else if (inDesc) {
                        description = textBuffer.toString().trim()
                        inDesc = false
                    }
                }
                "trkpt", "rtept", "wpt" -> {
                    if (currentPointType != null) {
                        val lat = currentLat
                        val lon = currentLon
                        if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                            val resolvedEle = if (hasPointEle) currentEle else (lastKnownEle ?: 0.0)
                            val pt = GpsPoint(
                                latitude = lat,
                                longitude = lon,
                                elevation = resolvedEle,
                                time = currentTime,
                                temperatureCelsius = currentTemp
                            )
                            when (currentPointType) {
                                "trkpt" -> {
                                    trkPoints.add(pt)
                                    if (!currentPointName.isNullOrBlank()) {
                                        parsedWaypoints.add(
                                            TrackWaypoint(
                                                name = currentPointName!!,
                                                latitude = lat,
                                                longitude = lon,
                                                elevation = resolvedEle,
                                                description = currentPointDesc
                                            )
                                        )
                                    }
                                }
                                "rtept" -> rtePoints.add(pt)
                                "wpt" -> {
                                    wptPoints.add(pt)
                                    val wName = currentPointName?.ifBlank { null }
                                        ?: "Точка ${parsedWaypoints.size + 1}"
                                    parsedWaypoints.add(
                                        TrackWaypoint(
                                            name = wName,
                                            latitude = lat,
                                            longitude = lon,
                                            elevation = resolvedEle,
                                            description = currentPointDesc
                                        )
                                    )
                                }
                            }
                        }
                        currentPointType = null
                        currentLat = null
                        currentLon = null
                        currentTime = null
                        currentTemp = null
                        currentPointName = null
                        currentPointDesc = null
                        hasPointEle = false
                    }
                }
            }
        }
    }

    companion object {
        fun parseIso8601ToEpoch(timeStr: String): Long? {
            val clean = timeStr.trim()
            if (clean.isEmpty()) return null
            try {
                return java.time.Instant.parse(clean).toEpochMilli()
            } catch (_: Throwable) {}
            try {
                return java.time.OffsetDateTime.parse(clean).toInstant().toEpochMilli()
            } catch (_: Throwable) {}

            val patterns = arrayOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
            )
            for (pattern in patterns) {
                try {
                    val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val date = sdf.parse(clean)
                    if (date != null) return date.time
                } catch (_: Throwable) {}
            }
            return null
        }
    }
}
