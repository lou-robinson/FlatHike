package com.flathike.app

import com.flathike.app.data.DemoTracks
import com.flathike.app.parser.GeoJsonParser
import com.flathike.app.parser.GpxParser
import com.flathike.app.parser.KmlParser
import com.flathike.app.parser.SimpleTextParser
import com.flathike.app.parser.TrackFormatDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class ParsersAndDetectorTest {

    @Test
    fun testGpxParser() {
        val gpxData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="FlatHike">
              <metadata><name>Тестовый маршрут GPX</name></metadata>
              <trk>
                <name>Трек на перевал</name>
                <trkseg>
                  <trkpt lat="43.5000" lon="40.2000"><ele>1200.5</ele></trkpt>
                  <trkpt lat="43.5100" lon="40.2100"><ele>1350.0</ele></trkpt>
                  <trkpt lat="43.5200" lon="40.2200"><ele>1500.2</ele></trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val parser = GpxParser()
        val track = parser.parse(ByteArrayInputStream(gpxData.toByteArray(StandardCharsets.UTF_8)))

        assertEquals(3, track.points.size)
        assertEquals(43.5000, track.points[0].latitude, 0.0001)
        assertEquals(40.2000, track.points[0].longitude, 0.0001)
        assertEquals(1200.5, track.points[0].elevation, 0.0001)
        assertEquals(1500.2, track.points[2].elevation, 0.0001)
    }

    @Test
    fun testKmlParser() {
        val kmlData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document>
                <name>KML Тропа</name>
                <Placemark>
                  <LineString>
                    <coordinates>
                      40.1000,43.2000,500.0
                      40.1100,43.2100,600.0
                    </coordinates>
                  </LineString>
                </Placemark>
              </Document>
            </kml>
        """.trimIndent()

        val parser = KmlParser()
        val track = parser.parse(ByteArrayInputStream(kmlData.toByteArray(StandardCharsets.UTF_8)))

        assertEquals(2, track.points.size)
        assertEquals(43.2000, track.points[0].latitude, 0.0001)
        assertEquals(40.1000, track.points[0].longitude, 0.0001)
        assertEquals(500.0, track.points[0].elevation, 0.0001)
    }

    @Test
    fun testGeoJsonParser() {
        val geoJsonData = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": { "name": "GeoJSON Трек" },
                  "geometry": {
                    "type": "LineString",
                    "coordinates": [
                      [37.6173, 55.7558, 150.0],
                      [37.6200, 55.7600, 160.0]
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parser = GeoJsonParser()
        val track = parser.parse(ByteArrayInputStream(geoJsonData.toByteArray(StandardCharsets.UTF_8)))

        assertEquals("GeoJSON Трек", track.name)
        assertEquals(2, track.points.size)
        assertEquals(55.7558, track.points[0].latitude, 0.0001)
        assertEquals(37.6173, track.points[0].longitude, 0.0001)
        assertEquals(150.0, track.points[0].elevation, 0.0001)
    }

    @Test
    fun testSimpleTextParser() {
        val textData = """
            # Тестовый CSV
            55.7558, 37.6173, 150.0
            55.7600, 37.6200, 165.5
        """.trimIndent()

        val parser = SimpleTextParser()
        val track = parser.parse(ByteArrayInputStream(textData.toByteArray(StandardCharsets.UTF_8)))

        assertEquals(2, track.points.size)
        assertEquals(55.7558, track.points[0].latitude, 0.0001)
        assertEquals(37.6173, track.points[0].longitude, 0.0001)
        assertEquals(150.0, track.points[0].elevation, 0.0001)
    }

    @Test
    fun testTrackFormatDetector() {
        val gpxSample = "<gpx version=\"1.1\"><trk><trkseg><trkpt lat=\"50\" lon=\"30\"><ele>100</ele></trkpt></trkseg></trk></gpx>"
        val trackGpx = TrackFormatDetector.parseText(gpxSample)
        assertEquals(1, trackGpx.points.size)

        val csvSample = "50.1, 30.1, 120.0\n50.2, 30.2, 130.0"
        val trackCsv = TrackFormatDetector.parseText(csvSample)
        assertEquals(2, trackCsv.points.size)
    }

    @Test
    fun testGpxWithWaypointsDoesNotCorruptTrack() {
        val gpxData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="FlatHike">
              <wpt lat="45.0000" lon="35.0000"><name>Spring Waypoint</name></wpt>
              <wpt lat="45.1000" lon="35.1000"><name>Camp Waypoint</name></wpt>
              <trk>
                <name>Real Track</name>
                <trkseg>
                  <trkpt lat="43.5000" lon="40.2000"><ele>1200.0</ele></trkpt>
                  <trkpt lat="43.5100" lon="40.2100"><ele>1350.0</ele></trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val parser = GpxParser()
        val track = parser.parse(ByteArrayInputStream(gpxData.toByteArray(StandardCharsets.UTF_8)))

        // Must ONLY contain the 2 trkpt points, not the 2 wpt waypoints!
        assertEquals(2, track.points.size)
        assertEquals(43.5000, track.points[0].latitude, 0.0001)
        assertEquals(40.2000, track.points[0].longitude, 0.0001)
        assertEquals(43.5100, track.points[1].latitude, 0.0001)
    }

    @Test
    fun testKmlWithGxTrackAndGxCoord() {
        val kmlData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2">
              <Document>
                <name>Google Earth Track</name>
                <Placemark>
                  <gx:Track>
                    <when>2023-01-01T10:00:00Z</when>
                    <gx:coord>40.2000 43.5000 1200.0</gx:coord>
                    <when>2023-01-01T10:05:00Z</when>
                    <gx:coord>40.2100 43.5100 1350.0</gx:coord>
                  </gx:Track>
                </Placemark>
              </Document>
            </kml>
        """.trimIndent()

        val parser = KmlParser()
        val track = parser.parse(ByteArrayInputStream(kmlData.toByteArray(StandardCharsets.UTF_8)))

        assertEquals(2, track.points.size)
        assertEquals(43.5000, track.points[0].latitude, 0.0001)
        assertEquals(40.2000, track.points[0].longitude, 0.0001)
        assertEquals(1200.0, track.points[0].elevation, 0.0001)
    }

    @Test
    fun testEuropeanSemicolonCsvParser() {
        // European formatting: semicolon separator and comma decimal points
        val textData = """
            55,755814; 37,617635; 150,5
            55,760012; 37,620045; 165,0
        """.trimIndent()

        val parser = SimpleTextParser()
        val track = parser.parse(ByteArrayInputStream(textData.toByteArray(StandardCharsets.UTF_8)))

        assertEquals(2, track.points.size)
        assertEquals(55.755814, track.points[0].latitude, 0.0001)
        assertEquals(37.617635, track.points[0].longitude, 0.0001)
        assertEquals(150.5, track.points[0].elevation, 0.0001)
    }

    @Test
    fun testCsvWithHeaderColumns() {
        val textData = """
            latitude,longitude,elevation
            55.7558,37.6173,150.0
            55.7600,37.6200,165.5
        """.trimIndent()

        val parser = SimpleTextParser()
        val track = parser.parse(ByteArrayInputStream(textData.toByteArray(StandardCharsets.UTF_8)))

        assertEquals(2, track.points.size)
        assertEquals(55.7558, track.points[0].latitude, 0.0001)
        assertEquals(37.6173, track.points[0].longitude, 0.0001)
    }

    @Test
    fun testGeoJsonWithBom() {
        val geoJsonData = "\uFEFF{\"type\":\"LineString\",\"coordinates\":[[37.6173,55.7558,150.0],[37.6200,55.7600,160.0]]}"
        val parser = GeoJsonParser()
        val track = parser.parse(ByteArrayInputStream(geoJsonData.toByteArray(StandardCharsets.UTF_8)))

        assertEquals(2, track.points.size)
        assertEquals(55.7558, track.points[0].latitude, 0.0001)
    }

    @Test
    fun testKmlCoordinatesWithSpacesAroundCommas() {
        val kmlData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document>
                <name>KML With Spaces</name>
                <Placemark>
                  <LineString>
                    <coordinates>
                      40.1000, 43.2000, 500.0
                      40.1100, 43.2100, 600.0
                    </coordinates>
                  </LineString>
                </Placemark>
              </Document>
            </kml>
        """.trimIndent()

        val parser = KmlParser()
        val track = parser.parse(ByteArrayInputStream(kmlData.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(2, track.points.size)
        assertEquals(43.2000, track.points[0].latitude, 0.0001)
        assertEquals(40.1000, track.points[0].longitude, 0.0001)
        assertEquals(500.0, track.points[0].elevation, 0.0001)
    }

    @Test
    fun testSpaceSeparatedCoordinatesWithCommaDecimals() {
        val textData = """
            55,7558 37,6173 150,5
            55,7600 37,6200 165,0
        """.trimIndent()

        val parser = SimpleTextParser()
        val track = parser.parse(ByteArrayInputStream(textData.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(2, track.points.size)
        assertEquals(55.7558, track.points[0].latitude, 0.0001)
        assertEquals(37.6173, track.points[0].longitude, 0.0001)
        assertEquals(150.5, track.points[0].elevation, 0.0001)
    }

    @Test
    fun testGeoJsonGeometryCollection() {
        val geoJsonData = """
            {
              "type": "GeometryCollection",
              "geometries": [
                {
                  "type": "LineString",
                  "coordinates": [
                    [37.6173, 55.7558, 150.0],
                    [37.6200, 55.7600, 160.0]
                  ]
                }
              ]
            }
        """.trimIndent()

        val parser = GeoJsonParser()
        val track = parser.parse(ByteArrayInputStream(geoJsonData.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(2, track.points.size)
        assertEquals(55.7558, track.points[0].latitude, 0.0001)
        assertEquals(37.6173, track.points[0].longitude, 0.0001)
    }

    @Test
    fun testGpxMissingElevationForwardFills() {
        val gpxData = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="FlatHike">
              <trk>
                <name>Track With Missing Ele</name>
                <trkseg>
                  <trkpt lat="43.5000" lon="40.2000"><ele>1200.0</ele></trkpt>
                  <trkpt lat="43.5100" lon="40.2100"></trkpt>
                  <trkpt lat="43.5200" lon="40.2200"><ele>1250.0</ele></trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val parser = GpxParser()
        val track = parser.parse(ByteArrayInputStream(gpxData.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(3, track.points.size)
        assertEquals(1200.0, track.points[0].elevation, 0.0001)
        assertEquals(1200.0, track.points[1].elevation, 0.0001)
        assertEquals(1250.0, track.points[2].elevation, 0.0001)
    }

    @Test
    fun testAllDemoTracksAreValid() {
        val demos = DemoTracks.allTracks
        assertEquals(6, demos.size)

        for (demo in demos) {
            assertTrue("Demo ${demo.name} has points", demo.points.isNotEmpty())
            assertTrue("Demo ${demo.name} has at least 30 points", demo.points.size >= 30)
            assertNotNull(demo.name)
            for (p in demo.points) {
                assertTrue(p.latitude in -90.0..90.0)
                assertTrue(p.longitude in -180.0..180.0)
            }
        }
    }
}
