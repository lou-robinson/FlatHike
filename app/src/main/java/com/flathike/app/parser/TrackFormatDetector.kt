package com.flathike.app.parser

import com.flathike.app.model.GpsTrack
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Automatically detects the format of a GPS track file (GPX, KML, GeoJSON, TXT/CSV)
 * and invokes the appropriate parser.
 */
object TrackFormatDetector {

    private val gpxParser = GpxParser()
    private val kmlParser = KmlParser()
    private val geoJsonParser = GeoJsonParser()
    private val simpleTextParser = SimpleTextParser()

    /**
     * Parses an input stream by sniffing its initial bytes or filename extension.
     */
    fun parseStream(
        inputStream: InputStream,
        filename: String = "track.gpx",
        defaultName: String = "GPS Трек"
    ): GpsTrack {
        var bytes = inputStream.readBytes()
        if (bytes.isEmpty()) {
            return GpsTrack(name = defaultName, points = emptyList())
        }

        // Strip UTF-8 BOM (0xEF, 0xBB, 0xBF) if present
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            bytes = bytes.copyOfRange(3, bytes.size)
        }

        val snippet = String(bytes.take(8192).toByteArray(), StandardCharsets.UTF_8).trim().lowercase()
        val ext = filename.substringAfterLast('.', "").lowercase()

        val parser: GpsTrackParser = when {
            snippet.contains("<gpx") || ext == "gpx" -> gpxParser
            snippet.contains("<kml") || snippet.contains("<gx:") || ext == "kml" -> kmlParser
            snippet.startsWith("{") || snippet.startsWith("[") || snippet.contains("\"coordinates\"") || ext == "geojson" || ext == "json" -> geoJsonParser
            else -> simpleTextParser
        }

        return try {
            parser.parse(ByteArrayInputStream(bytes), defaultName)
        } catch (e: Exception) {
            // Fallback to simple text parser if XML/JSON fails
            try {
                simpleTextParser.parse(ByteArrayInputStream(bytes), defaultName)
            } catch (_: Exception) {
                throw e
            }
        }
    }

    /**
     * Parses a raw coordinate or XML/JSON string directly (e.g. from copy-paste).
     */
    fun parseText(text: String, trackName: String = "Вставленный трек"): GpsTrack {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        return parseStream(ByteArrayInputStream(bytes), "manual.txt", trackName)
    }
}
