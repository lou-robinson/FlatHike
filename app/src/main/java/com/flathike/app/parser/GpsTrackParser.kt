package com.flathike.app.parser

import com.flathike.app.model.GpsTrack
import java.io.InputStream

/**
 * Interface for parsing GPS track file formats.
 */
interface GpsTrackParser {
    /**
     * Parses an input stream into a [GpsTrack].
     *
     * @param inputStream Stream containing track data
     * @param defaultName Default name if track has no title in metadata
     * @return Parsed track with points
     */
    fun parse(inputStream: InputStream, defaultName: String = "GPS Трек"): GpsTrack
}
