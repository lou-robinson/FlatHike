package com.flathike.app

import com.flathike.app.calculator.TrackAnalyzer
import com.flathike.app.model.GpsPoint
import com.flathike.app.model.HikingPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackAnalyzerTest {

    @Test
    fun testEmptyPointsList() {
        val result = TrackAnalyzer.analyze(emptyList(), "Empty")
        assertEquals(0, result.pointCount)
        assertEquals(0.0, result.straightLineSeaLevelKm, 0.001)
        assertEquals(0.0, result.trackLengthSeaLevelKm, 0.001)
        assertEquals(0.0, result.totalAscentMeters, 0.001)
        assertEquals(0.0, result.totalDescentMeters, 0.001)
        assertTrue(result.elevationProfile.isEmpty())
    }

    @Test
    fun testSinglePointList() {
        val pt = GpsPoint(45.0, 35.0, 500.0)
        val result = TrackAnalyzer.analyze(listOf(pt), "Single")
        assertEquals(1, result.pointCount)
        assertEquals(0.0, result.straightLineSeaLevelKm, 0.001)
        assertEquals(0.0, result.trackLengthSeaLevelKm, 0.001)
        assertEquals(500.0, result.minElevationMeters, 0.001)
        assertEquals(500.0, result.maxElevationMeters, 0.001)
        assertEquals(1, result.elevationProfile.size)
    }

    @Test
    fun testStraightAscentTrack() {
        // Track climbing 1000m over ~10 km
        val points = mutableListOf<GpsPoint>()
        val startLat = 50.0
        val endLat = 50.09 // ~10 km
        for (i in 0..10) {
            val f = i / 10.0
            val lat = startLat + (endLat - startLat) * f
            val ele = 100.0 + 1000.0 * f
            points.add(GpsPoint(lat, 30.0, ele))
        }

        val result = TrackAnalyzer.analyze(points, "Ascent")
        assertEquals(11, result.pointCount)
        assertEquals(10.0, result.straightLineSeaLevelKm, 0.2)
        assertEquals(10.0, result.trackLengthSeaLevelKm, 0.2)
        assertEquals(1000.0, result.totalAscentMeters, 5.0)
        assertEquals(0.0, result.totalDescentMeters, 0.1)
        assertEquals(100.0, result.minElevationMeters, 0.1)
        assertEquals(1100.0, result.maxElevationMeters, 0.1)

        // Effective distance calculation:
        // SAC k = 10: 10 km base + (1000m / 1000m) * 10 = 20 km
        val effSac = result.calculateEffectiveDistance(HikingPreset.SWISS_SAC.ascentCoefficient)
        assertEquals(10.0, effSac.ascentBonusKm, 0.2)
        assertEquals(20.0, effSac.effectiveDistanceKm, 0.3)

        // Flat k = 0: exactly track length
        val effFlat = result.calculateEffectiveDistance(HikingPreset.FLAT_ONLY.ascentCoefficient)
        assertEquals(0.0, effFlat.ascentBonusKm, 0.001)
        assertEquals(result.trackLengthSeaLevelKm, effFlat.effectiveDistanceKm, 0.001)

        // NOLS k = 8: +8 km
        val effNols = result.calculateEffectiveDistance(HikingPreset.PETZOLDT_NOLS.ascentCoefficient)
        assertEquals(8.0, effNols.ascentBonusKm, 0.2)

        // FSTR k = 6: +6 km
        val effFstr = result.calculateEffectiveDistance(HikingPreset.RUSSIAN_FSTR.ascentCoefficient)
        assertEquals(6.0, effFstr.ascentBonusKm, 0.2)

        // Trail running k = 4: +4 km
        val effTrail = result.calculateEffectiveDistance(HikingPreset.TRAIL_RUNNING.ascentCoefficient)
        assertEquals(4.0, effTrail.ascentBonusKm, 0.2)
    }

    @Test
    fun testElevationNoiseJitterFiltering() {
        // Flat track with 0.5m GPS noise jitter (below 2m threshold)
        val points = listOf(
            GpsPoint(50.00, 30.0, 100.0),
            GpsPoint(50.01, 30.0, 100.8), // +0.8m (< 2.0m, should not trigger ascent)
            GpsPoint(50.02, 30.0, 99.7),  // -1.1m from anchor
            GpsPoint(50.03, 30.0, 100.2)
        )
        val result = TrackAnalyzer.analyze(points, "Noisy Flat")
        // Total ascent should remain 0 because jitter never crossed 2.0m threshold
        assertEquals(0.0, result.totalAscentMeters, 0.01)
        assertEquals(0.0, result.totalDescentMeters, 0.01)
    }

    @Test
    fun testCircularLoopTrackDetection() {
        val loopTrack = com.flathike.app.data.DemoTracks.allTracks.first { it.name.contains("Мыс Айя") }
        val result = TrackAnalyzer.analyze(loopTrack.points, loopTrack.name)

        // Loop track detection
        assertTrue("Track must be recognized as closed loop", result.isClosedLoop)
        // Straight line start to end is close to 0
        assertEquals(0.0, result.straightLineSeaLevelKm, 0.05)
        // Apex radial distance must be positive (~4.9 km)
        assertTrue("Max distance from start must be > 3 km", result.maxDistanceFromStartKm > 3.0)
        // Highest point is detected
        assertEquals(680.0, result.highestPoint.elevation, 5.0)

        // For loop tracks, straight-line baseline must use max radial distance, not 0.0km start-to-end
        val effLoopStraight = result.calculateEffectiveDistance(10.0, useTrackLength = false)
        assertEquals(result.maxDistanceFromStartKm, effLoopStraight.baseDistanceKm, 0.01)
        assertTrue(effLoopStraight.baseDistanceKm > 3.0)
    }

    @Test
    fun testEffectiveDistanceOnStraightLineBaseline() {
        val pt1 = GpsPoint(55.0, 37.0, 100.0)
        val pt2 = GpsPoint(55.09, 37.0, 1100.0) // ~10 km straight line, 1000m climb
        val result = TrackAnalyzer.analyze(listOf(pt1, pt2), "Straight")

        // Track baseline: ~10 km + 10 km = ~20 km
        val effTrack = result.calculateEffectiveDistance(10.0, useTrackLength = true)
        assertEquals(10.0, effTrack.ascentBonusKm, 0.2)
        assertEquals(20.0, effTrack.effectiveDistanceKm, 0.3)

        // Straight baseline: should also be ~20 km
        val effStraight = result.calculateEffectiveDistance(10.0, useTrackLength = false)
        assertEquals(effTrack.baseDistanceKm, effStraight.baseDistanceKm, 0.1)
        assertEquals(effTrack.effectiveDistanceKm, effStraight.effectiveDistanceKm, 0.1)
    }

    @Test
    fun testMissingElevationDetectionOn2dTrack() {
        // Track with 0.0m elevation
        val points2d = listOf(
            GpsPoint(43.985, 39.870, 0.0),
            GpsPoint(43.995, 39.885, 0.0),
            GpsPoint(44.010, 39.910, 0.0),
            GpsPoint(44.025, 39.935, 0.0)
        )
        val result2d = TrackAnalyzer.analyze(points2d, "Fisht 2D")
        assertTrue("Track with 0.0m elevations must be detected as missing elevation", result2d.isElevationMissing)
        assertEquals(0.0, result2d.totalAscentMeters, 0.001)

        // Track with real mountain elevations
        val points3d = listOf(
            GpsPoint(43.985, 39.870, 1850.0),
            GpsPoint(43.995, 39.885, 2100.0),
            GpsPoint(44.010, 39.910, 2450.0),
            GpsPoint(44.025, 39.935, 2867.0)
        )
        val result3d = TrackAnalyzer.analyze(points3d, "Fisht 3D")
        org.junit.Assert.assertFalse("Track with real elevations must not be marked as missing elevation", result3d.isElevationMissing)
        assertTrue("Ascent must be calculated", result3d.totalAscentMeters > 900.0)
    }

    @Test
    fun testRecordedTimeAndTemperatureMetrics() {
        val t0 = 1723450000000L
        val points = listOf(
            GpsPoint(55.0, 37.0, 100.0, time = t0, temperatureCelsius = 18.0),
            GpsPoint(55.01, 37.0, 120.0, time = t0 + 1800000L, temperatureCelsius = 20.0),
            GpsPoint(55.02, 37.0, 140.0, time = t0 + 3600000L, temperatureCelsius = 22.0)
        )
        val result = TrackAnalyzer.analyze(points, "Timed Track")
        assertTrue("Track must have recorded time metrics", result.hasRecordedTime)
        assertTrue("Track must have recorded temperature metrics", result.hasRecordedTemperature)

        val timeMetrics = result.recordedTimeMetrics!!
        assertEquals(3600L, timeMetrics.totalDurationSeconds)
        assertTrue("Moving duration must be > 0", timeMetrics.movingDurationSeconds > 0)
        assertTrue("Average moving speed must be reasonable", timeMetrics.avgMovingSpeedKmH > 1.0)

        val tempMetrics = result.recordedTemperatureMetrics!!
        assertEquals(20.0, tempMetrics.avgCelsius, 0.1)
        assertEquals(18.0, tempMetrics.minCelsius, 0.1)
        assertEquals(22.0, tempMetrics.maxCelsius, 0.1)
        assertEquals(18.0, tempMetrics.startCelsius, 0.1)
        assertEquals(22.0, tempMetrics.endCelsius, 0.1)
    }
}
