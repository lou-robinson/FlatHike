package com.flathike.app

import com.flathike.app.calculator.TrackAnalyzer
import com.flathike.app.calculator.TrackSplitter
import com.flathike.app.model.AppLanguage
import com.flathike.app.model.GpsPoint
import com.flathike.app.model.SlopeCategory
import com.flathike.app.model.ThemeMode
import com.flathike.app.model.TrackSegment
import com.flathike.app.model.TrackSplitMode
import com.flathike.app.model.TrackWaypoint
import com.flathike.app.ui.AppStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackWaypointsAndSegmentationTest {

    @Test
    fun testWaypointsProjectedOntoTrack() {
        val points = listOf(
            GpsPoint(50.00, 30.00, 100.0),
            GpsPoint(50.01, 30.00, 200.0), // ~1.11 km
            GpsPoint(50.02, 30.00, 400.0), // ~2.22 km
            GpsPoint(50.03, 30.00, 600.0), // ~3.33 km
            GpsPoint(50.04, 30.00, 800.0)  // ~4.44 km
        )

        val rawWaypoints = listOf(
            TrackWaypoint(name = "Spring", latitude = 50.01, longitude = 30.00, elevation = 0.0),
            TrackWaypoint(name = "Summit", latitude = 50.04, longitude = 30.00, elevation = 800.0)
        )

        val result = TrackAnalyzer.analyze(
            points = points,
            trackName = "Test Trail",
            rawWaypoints = rawWaypoints
        )

        assertTrue(result.hasWaypoints)
        assertEquals(2, result.waypoints.size)

        val spring = result.waypoints[0]
        assertEquals("Spring", spring.name)
        assertTrue("Distance should be around 1.1 km", spring.distanceKm in 1.0..1.3)
        assertEquals("Should inherit elevation from track point", 200.0, spring.elevation, 1.0)

        val summit = result.waypoints[1]
        assertEquals("Summit", summit.name)
        assertTrue("Summit should be at track end ~4.4 km", summit.distanceKm in 4.0..4.6)
        assertEquals(800.0, summit.elevation, 1.0)
    }

    @Test
    fun testSlopeSpeedAnalysisWithRecordedGpsTime() {
        // Track with timestamps and clear ascent
        val baseTime = 1700000000000L
        val points = mutableListOf<GpsPoint>()
        for (i in 0..10) {
            val lat = 50.0 + (i * 0.005) // ~555m per point
            val ele = 100.0 + (i * 100.0) // 100m climb per 555m => ~18% steep ascent
            val time = baseTime + (i * 600_000L) // 10 min per point => ~3.3 km/h
            points.add(GpsPoint(lat, 30.0, ele, time = time))
        }

        val result = TrackAnalyzer.analyze(points, "Steep Timed Climb")

        assertNotNull(result.recordedTimeMetrics)
        assertTrue(result.hasRecordedTime)
        assertEquals(5, result.slopeSpeedAnalysis.size)

        val steepAscent = result.slopeSpeedAnalysis.first { it.category == SlopeCategory.STEEP_ASCENT }
        assertTrue("Most distance should be steep ascent", steepAscent.totalDistanceKm > 4.0)
        assertTrue("Steep ascent should have recorded GPS speed", steepAscent.isRealGpsSpeed)
        assertTrue("Speed should be around ~3.3 km/h", steepAscent.avgSpeedKmH in 2.5..4.5)
        assertTrue("Total climb should be around 1000m", steepAscent.totalAscentMeters in 950.0..1050.0)
    }

    @Test
    fun testSlopeSpeedAnalysisWithoutTimestampsUsesTobler() {
        val points = mutableListOf<GpsPoint>()
        for (i in 0..10) {
            val lat = 50.0 + (i * 0.005)
            val ele = 100.0 + (i * 5.0) // 5m per 555m => ~0.9% flat
            points.add(GpsPoint(lat, 30.0, ele))
        }

        val result = TrackAnalyzer.analyze(points, "Flat Untimed")
        assertFalse(result.hasRecordedTime)

        val flat = result.slopeSpeedAnalysis.first { it.category == SlopeCategory.FLAT }
        assertTrue("Most distance should be flat", flat.totalDistanceKm > 4.0)
        assertFalse("Should be model/Tobler estimated", flat.isRealGpsSpeed)
        assertTrue("Tobler flat speed should be around 4-5 km/h", flat.avgSpeedKmH in 3.5..5.5)
    }

    @Test
    fun testTrackPartitionModes() {
        val baseTime = 1700000000000L
        val points = mutableListOf<GpsPoint>()
        for (i in 0..20) {
            val lat = 50.0 + (i * 0.005) // total ~11 km
            val ele = 200.0 + (if (i < 10) i * 20.0 else (20 - i) * 20.0) // climb then descent
            val time = baseTime + (i * 300_000L) // 5 min per step
            points.add(GpsPoint(lat, 30.0, ele, time = time))
        }

        val waypoints = listOf(
            TrackWaypoint("Pass", 50.05, 30.0, 400.0)
        )

        val result = TrackAnalyzer.analyze(points, "Loop Ridge", rawWaypoints = waypoints)

        // Test Split by Slope
        val slopeSegs = result.getSegmentsForMode(TrackSplitMode.BY_SLOPE)
        assertTrue("Should produce multiple slope sections", slopeSegs.isNotEmpty())
        for (seg in slopeSegs) {
            assertTrue(seg.distanceKm > 0)
            assertTrue(seg.avgSpeedKmH > 0)
        }

        // Test Split by 1 km
        val km1Segs = result.getSegmentsForMode(TrackSplitMode.BY_DISTANCE_1KM)
        assertTrue("Should have roughly 10-12 1km segments", km1Segs.size >= 8)

        // Test Split by 5 km
        val km5Segs = result.getSegmentsForMode(TrackSplitMode.BY_DISTANCE_5KM)
        assertTrue("Should have 2-3 5km segments", km5Segs.size in 2..3)

        // Test Split by Waypoints
        val wptSegs = result.getSegmentsForMode(TrackSplitMode.BY_WAYPOINTS)
        assertEquals("Start->Pass and Pass->Finish = 2 segments", 2, wptSegs.size)
        assertTrue(wptSegs[0].name.contains("Старт"))
        assertTrue(wptSegs[0].name.contains("Pass"))
        assertTrue(wptSegs[1].name.contains("Pass"))
        assertTrue(wptSegs[1].name.contains("Финиш"))
    }

    @Test
    fun testAppStringsLocalization() {
        val ru = AppStrings(AppLanguage.RU)
        val en = AppStrings(AppLanguage.EN)

        assertTrue(ru.isRu)
        assertFalse(en.isRu)

        assertEquals("Настройки", ru.settings)
        assertEquals("Settings", en.settings)

        assertEquals("Высотный профиль", ru.elevationProfile)
        assertEquals("Elevation Profile", en.elevationProfile)

        assertEquals("Промежуточные точки", ru.waypointsTitle)
        assertEquals("Intermediate Waypoints", en.waypointsTitle)

        assertEquals("Анализ скорости и разбивка трека", ru.speedAnalysisTitle)
        assertEquals("Speed Analysis & Track Splits", en.speedAnalysisTitle)

        assertEquals("Светлая тема", ru.themeLight)
        assertEquals("Light Theme", en.themeLight)
    }

    @Test
    fun testThemeModeAndLanguageEnums() {
        assertEquals("Системная тема", ThemeMode.SYSTEM.titleRu)
        assertEquals("Light Theme", ThemeMode.LIGHT.titleEn)
        assertEquals("Dark Theme", ThemeMode.DARK.titleEn)

        assertEquals("ru", AppLanguage.RU.code)
        assertEquals("en", AppLanguage.EN.code)
    }

    @Test
    fun testFindPointAtDistanceNonUniformDistribution() {
        // Track with non-uniform point density:
        // 100 points huddled in first 1 km (camp site / rest pause)
        // 2 points covering next 9 km (fast descent)
        val points = mutableListOf<GpsPoint>()
        for (i in 0..99) {
            // All clustered around lat 50.0000 -> 50.0090 (~1.0 km)
            points.add(GpsPoint(50.0 + (i * 0.00009), 30.0, 500.0))
        }
        // Then 1 long segment to ~10 km
        points.add(GpsPoint(50.09, 30.0, 100.0))

        // Find point at distance 5.5 km (which is halfway along the long segment, NOT at index 50!)
        val ptAt5Km = TrackAnalyzer.findPointAtDistance(points, 5.5)
        assertNotNull(ptAt5Km)
        // Latitude should be between 50.009 and 50.09, closer to ~50.05
        assertTrue("Point at 5.5 km should be interpolated halfway along trail", ptAt5Km!!.latitude in 50.04..50.06)
        assertTrue("Elevation should be interpolated", ptAt5Km.elevation in 250.0..350.0)
    }

    @Test
    fun testExplicitWaypointDistancePreservation() {
        val points = listOf(
            GpsPoint(50.00, 30.00, 100.0),
            GpsPoint(50.01, 30.00, 200.0),
            GpsPoint(50.02, 30.00, 300.0),
            GpsPoint(50.03, 30.00, 400.0)
        )

        // Waypoint explicitly placed by user at km 2.5
        val manualWpt = TrackWaypoint(
            name = "Picnic Table",
            latitude = 50.022,
            longitude = 30.00,
            elevation = 320.0,
            distanceKm = 2.5
        )

        val result = TrackAnalyzer.analyze(points, "Preserve Test", rawWaypoints = listOf(manualWpt))
        assertEquals(1, result.waypoints.size)
        assertEquals("Explicit distanceKm must be preserved exactly", 2.5, result.waypoints[0].distanceKm, 0.001)
    }

    @Test
    fun testSegmentLocalizedDisplayNames() {
        val ru = AppStrings(AppLanguage.RU)
        val en = AppStrings(AppLanguage.EN)

        val slopeSeg = TrackSegment(
            index = 3,
            name = "Участок 3",
            startDistanceKm = 2.0,
            endDistanceKm = 3.5,
            distanceKm = 1.5,
            startElevationM = 100.0,
            endElevationM = 150.0,
            elevationChangeM = 50.0,
            avgSlopePercent = 3.3,
            slopeCategory = SlopeCategory.FLAT,
            durationSeconds = 1200,
            avgSpeedKmH = 4.5,
            isRealGpsSpeed = false
        )

        assertEquals("Участок 3", slopeSeg.getDisplayName(ru))
        assertEquals("Segment 3", slopeSeg.getDisplayName(en))

        val wptSeg = TrackSegment(
            index = 1,
            name = "Старт → Перевал",
            startDistanceKm = 0.0,
            endDistanceKm = 4.0,
            distanceKm = 4.0,
            startElevationM = 100.0,
            endElevationM = 600.0,
            elevationChangeM = 500.0,
            avgSlopePercent = 12.5,
            slopeCategory = SlopeCategory.MODERATE_ASCENT,
            durationSeconds = 3600,
            avgSpeedKmH = 4.0,
            isRealGpsSpeed = false,
            fromPointName = "Старт",
            toPointName = "Перевал"
        )

        assertEquals("Старт → Перевал", wptSeg.getDisplayName(ru))
        assertEquals("Start → Перевал", wptSeg.getDisplayName(en))
    }

    @Test
    fun testDeadFlatTrackNoDivisionByZeroOrNaN() {
        // Completely flat track (sea kayaking / salt flats)
        val points = listOf(
            GpsPoint(60.00, 25.00, 0.0),
            GpsPoint(60.01, 25.00, 0.0),
            GpsPoint(60.02, 25.00, 0.0),
            GpsPoint(60.03, 25.00, 0.0)
        )

        val result = TrackAnalyzer.analyze(points, "Dead Flat Sea")
        assertEquals(0.0, result.totalAscentMeters, 0.001)
        assertEquals(0.0, result.totalDescentMeters, 0.001)
        assertEquals(0.0, result.avgSlopePercent, 0.001)
        assertEquals(0.0, result.maxSlopePercent, 0.001)
        assertFalse(result.avgSlopePercent.isNaN())
        assertFalse(result.maxSlopePercent.isNaN())

        // Slope breakdown should classify entire track as FLAT
        val flatBreakdown = result.slopeSpeedAnalysis.first { it.category == SlopeCategory.FLAT }
        assertTrue(flatBreakdown.totalDistanceKm > 3.0)
        assertTrue(flatBreakdown.avgSpeedKmH > 0.0)
        assertFalse(flatBreakdown.avgSpeedKmH.isNaN())
    }

    @Test
    fun testFarAwayWaypointDoesNotCrashOrCorruptTrack() {
        val points = listOf(
            GpsPoint(50.00, 30.00, 100.0),
            GpsPoint(50.01, 30.00, 150.0),
            GpsPoint(50.02, 30.00, 200.0)
        )

        // Waypoint in Paris (thousands of km away from Kyiv/lat 50, lon 30)
        val farWaypoint = TrackWaypoint(
            name = "Eiffel Tower",
            latitude = 48.8584,
            longitude = 2.2945,
            elevation = 300.0,
            distanceKm = 0.0
        )

        val result = TrackAnalyzer.analyze(points, "Kyiv Trail", rawWaypoints = listOf(farWaypoint))
        assertEquals(1, result.waypoints.size)
        val projected = result.waypoints[0]
        assertEquals("Eiffel Tower", projected.name)
        // Should safely project to one of the track endpoints without throwing or producing NaN
        assertFalse(projected.distanceKm.isNaN())
        assertTrue(projected.distanceKm >= 0.0)
    }
}

