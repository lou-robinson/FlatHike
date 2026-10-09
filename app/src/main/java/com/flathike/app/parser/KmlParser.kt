package com.flathike.app.parser

import com.flathike.app.model.GpsPoint
import com.flathike.app.model.GpsTrack
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

/**
 * SAX parser for Google Earth KML files.
 * Extracts points from <coordinates> blocks in format:
 *   longitude,latitude[,altitude]
 */
class KmlParser : GpsTrackParser {

    override fun parse(inputStream: InputStream, defaultName: String): GpsTrack {
        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            try {
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            } catch (_: Exception) {}
        }
        val saxParser = factory.newSAXParser()
        val handler = KmlSaxHandler(defaultName)

        val inputSource = InputSource(inputStream).apply {
            encoding = "UTF-8"
        }
        saxParser.parse(inputSource, handler)

        val resolvedPoints = if (handler.trackPoints.isNotEmpty()) {
            handler.trackPoints
        } else {
            handler.isolatedPoints
        }

        val waypoints = if (handler.trackPoints.isNotEmpty()) {
            handler.isolatedPoints.mapIndexed { idx, pt ->
                com.flathike.app.model.TrackWaypoint(
                    name = "Точка ${idx + 1}",
                    latitude = pt.latitude,
                    longitude = pt.longitude,
                    elevation = pt.elevation
                )
            }
        } else emptyList()

        return GpsTrack(
            name = handler.trackName.ifBlank { defaultName },
            points = resolvedPoints,
            waypoints = waypoints
        )
    }

    private class KmlSaxHandler(val fallbackName: String) : DefaultHandler() {
        val trackPoints = mutableListOf<GpsPoint>()
        val isolatedPoints = mutableListOf<GpsPoint>()
        var trackName: String = fallbackName

        private var inCoordinates = false
        private var inGxCoord = false
        private var inName = false
        private var inLineOrTrack = false
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
                "linestring", "track", "multitrack" -> inLineOrTrack = true
                "coordinates" -> inCoordinates = true
                "coord", "gx:coord" -> inGxCoord = true
                "name" -> if (trackName == fallbackName) inName = true
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
                "linestring", "track", "multitrack" -> inLineOrTrack = false
                "name" -> {
                    if (inName) {
                        trackName = textBuffer.toString().trim()
                        inName = false
                    }
                }
                "coordinates" -> {
                    if (inCoordinates) {
                        val target = if (inLineOrTrack) trackPoints else isolatedPoints
                        parseCoordinateTuples(textBuffer.toString(), target)
                        inCoordinates = false
                    }
                }
                "coord", "gx:coord" -> {
                    if (inGxCoord) {
                        parseGxCoord(textBuffer.toString(), trackPoints)
                        inGxCoord = false
                    }
                }
            }
        }

        private fun parseCoordinateTuples(raw: String, target: MutableList<GpsPoint>) {
            // Normalize spaces around commas so "lon, lat, alt" becomes "lon,lat,alt"
            val normalized = raw.replace(Regex("\\s*,\\s*"), ",")
            val tokens = normalized.trim().split("\\s+".toRegex())
            for (token in tokens) {
                if (token.isBlank()) continue
                val parts = token.split(",")
                if (parts.size >= 2) {
                    val lon = parts[0].trim().replace(',', '.').toDoubleOrNull()
                    val lat = parts[1].trim().replace(',', '.').toDoubleOrNull()
                    val alt = if (parts.size >= 3) {
                        parts[2].trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                    } else 0.0

                    if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                        target.add(GpsPoint(latitude = lat, longitude = lon, elevation = alt))
                    }
                }
            }
        }

        private fun parseGxCoord(raw: String, target: MutableList<GpsPoint>) {
            // Google Earth gx:coord format: "lon lat alt" separated by spaces
            val parts = raw.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                val lon = parts[0].trim().replace(',', '.').toDoubleOrNull()
                val lat = parts[1].trim().replace(',', '.').toDoubleOrNull()
                val alt = if (parts.size >= 3) {
                    parts[2].trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                } else 0.0

                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    target.add(GpsPoint(latitude = lat, longitude = lon, elevation = alt))
                }
            }
        }
    }
}
