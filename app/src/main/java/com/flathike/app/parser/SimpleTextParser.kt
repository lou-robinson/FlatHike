package com.flathike.app.parser

import com.flathike.app.model.GpsPoint
import com.flathike.app.model.GpsTrack
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Parser for plain text or CSV coordinate tracks.
 * Format per line:
 *   latitude, longitude [, elevation]
 * or tab/space separated.
 */
class SimpleTextParser : GpsTrackParser {

    override fun parse(inputStream: InputStream, defaultName: String): GpsTrack {
        val points = mutableListOf<GpsPoint>()
        var latCol = -1
        var lonCol = -1
        var eleCol = -1
        var timeCol = -1
        var tempCol = -1
        var headerDetected = false

        inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            for (rawLine in lines) {
                var line = rawLine.trim()
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1).trim()
                }
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue

                val tokens = tokenizeLine(line)
                if (tokens.size < 2) continue

                // Check for header row
                if (!headerDetected && isHeaderRow(tokens)) {
                    for (i in tokens.indices) {
                        val lower = tokens[i].lowercase()
                        if (lower.contains("lat") || lower.contains("широт")) latCol = i
                        else if (lower.contains("lon") || lower.contains("lng") || lower.contains("долгот")) lonCol = i
                        else if (lower.contains("ele") || lower.contains("alt") || lower.contains("высот")) eleCol = i
                        else if (lower.contains("time") || lower.contains("время") || lower.contains("timestamp") || lower.contains("date")) timeCol = i
                        else if (lower.contains("temp") || lower.contains("темп") || lower.contains("atemp")) tempCol = i
                    }
                    headerDetected = true
                    continue
                }

                val latVal: Double?
                val lonVal: Double?
                val eleVal: Double
                val timeVal: Long?
                val tempVal: Double?

                if (latCol != -1 && lonCol != -1) {
                    latVal = tokens.getOrNull(latCol)?.replace(',', '.')?.toDoubleOrNull()
                    lonVal = tokens.getOrNull(lonCol)?.replace(',', '.')?.toDoubleOrNull()
                    eleVal = if (eleCol != -1) tokens.getOrNull(eleCol)?.replace(',', '.')?.toDoubleOrNull() ?: 0.0 else 0.0
                    timeVal = if (timeCol != -1) {
                        val rawTime = tokens.getOrNull(timeCol) ?: ""
                        rawTime.toLongOrNull() ?: GpxParser.parseIso8601ToEpoch(rawTime)
                    } else null
                    tempVal = if (tempCol != -1) tokens.getOrNull(tempCol)?.replace(',', '.')?.toDoubleOrNull() else null
                } else {
                    // Default positional: 0: lat, 1: lon, 2: ele, 3: time, 4: temp
                    latVal = tokens[0].replace(',', '.').toDoubleOrNull()
                    lonVal = tokens[1].replace(',', '.').toDoubleOrNull()
                    eleVal = if (tokens.size >= 3) tokens[2].replace(',', '.').toDoubleOrNull() ?: 0.0 else 0.0
                    timeVal = if (tokens.size >= 4) {
                        tokens[3].toLongOrNull() ?: GpxParser.parseIso8601ToEpoch(tokens[3])
                    } else null
                    tempVal = if (tokens.size >= 5) tokens[4].replace(',', '.').toDoubleOrNull() else null
                }

                if (latVal != null && lonVal != null && latVal in -90.0..90.0 && lonVal in -180.0..180.0) {
                    points.add(
                        GpsPoint(
                            latitude = latVal,
                            longitude = lonVal,
                            elevation = eleVal,
                            time = timeVal,
                            temperatureCelsius = tempVal
                        )
                    )
                }
            }
        }

        return GpsTrack(
            name = defaultName,
            points = points
        )
    }

    private fun isHeaderRow(tokens: List<String>): Boolean {
        val line = tokens.joinToString(" ").lowercase()
        return line.contains("lat") || line.contains("lon") || line.contains("lng") ||
                line.contains("широт") || line.contains("долгот")
    }

    private fun tokenizeLine(line: String): List<String> {
        // 1. Semicolon delimiter (European CSV)
        if (line.contains(';')) {
            return line.split(';').map { it.trim() }.filter { it.isNotEmpty() }
        }
        // 2. Tab delimiter
        if (line.contains('\t')) {
            return line.split('\t').map { it.trim() }.filter { it.isNotEmpty() }
        }
        // 3. Whitespace-separated numbers (e.g. "55,7558 37,6176 150,0" or "55.7558 37.6176")
        val spaceTokens = line.split("\\s+".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
        if (spaceTokens.size >= 2 && spaceTokens.take(2).all { token ->
            token.replace(',', '.').toDoubleOrNull() != null
        }) {
            return spaceTokens
        }
        // 4. Comma delimiter (e.g. "55.7558, 37.6176, 150.0")
        if (line.contains(',')) {
            val commaParts = line.split(',')
            if (commaParts.size >= 2) {
                return commaParts.map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        // 5. Fallback whitespace
        return spaceTokens
    }
}
