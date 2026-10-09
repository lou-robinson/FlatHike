package com.flathike.app

import com.flathike.app.calculator.GeodesyCalculator
import com.flathike.app.model.GpsPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GeodesyCalculatorTest {

    @Test
    fun testSamePointDistanceIsZero() {
        val p = GpsPoint(55.7558, 37.6173, 150.0)
        val distMeters = GeodesyCalculator.haversineDistanceMeters(p.latitude, p.longitude, p.latitude, p.longitude)
        assertEquals(0.0, distMeters, 0.0001)

        val straight3d = GeodesyCalculator.straightLine3dMeters(p, p)
        assertEquals(0.0, straight3d, 0.0001)

        val seaChord = GeodesyCalculator.seaLevelChordMeters(p, p)
        assertEquals(0.0, seaChord, 0.0001)
    }

    @Test
    fun testEquatorOneDegreeDistance() {
        // 1 degree at equator ≈ 2 * PI * R / 360 ≈ 111,195 meters
        val p1 = GpsPoint(0.0, 0.0, 0.0)
        val p2 = GpsPoint(0.0, 1.0, 0.0)
        val distKm = GeodesyCalculator.seaLevelStraightLineKm(p1, p2)
        assertEquals(111.195, distKm, 0.5)
    }

    @Test
    fun testQuarterEarthCircumference() {
        // From Equator (0,0) to North Pole (90,0) is 1/4 of Earth perimeter ≈ 10,007 km
        val p1 = GpsPoint(0.0, 0.0, 0.0)
        val p2 = GpsPoint(90.0, 0.0, 0.0)
        val distKm = GeodesyCalculator.seaLevelStraightLineKm(p1, p2)
        assertEquals(10007.5, distKm, 10.0)
    }

    @Test
    fun testChordShorterThanArc() {
        // Chord through Earth must always be strictly less than arc along surface for non-zero distances
        val p1 = GpsPoint(45.0, 30.0, 0.0)
        val p2 = GpsPoint(46.0, 31.0, 0.0)
        val arcMeters = GeodesyCalculator.haversineDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        val chordMeters = GeodesyCalculator.seaLevelChordMeters(p1, p2)

        assertTrue(chordMeters < arcMeters)
        assertTrue(chordMeters > 0.0)
        // For short distances (~130 km), chord difference is small but measurable: ~d^3 / (24 R^2) ≈ a few meters
        assertTrue(arcMeters - chordMeters > 0.1)
    }

    @Test
    fun test3dSegmentHypotenuse() {
        // Segment with 100m horizontal and 100m vertical
        // Latitude 0.0009 deg ≈ ~100m
        val p1 = GpsPoint(0.0, 0.0, 0.0)
        val lat2 = (100.0 / GeodesyCalculator.EARTH_RADIUS_SEA_LEVEL_METERS) * (180.0 / Math.PI)
        val p2 = GpsPoint(lat2, 0.0, 100.0)

        val dHoriz = GeodesyCalculator.haversineDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        val d3d = GeodesyCalculator.segment3dMeters(p1, p2)

        assertEquals(100.0, dHoriz, 0.5)
        // sqrt(100^2 + 100^2) ≈ 141.42m
        assertEquals(141.42, d3d, 1.0)
    }

    @Test
    fun testGeodeticReductionAtAltitude() {
        // At 5,000m altitude (Elbrus/Himalayas), 10 km on ground has reduction:
        // delta = 10,000 * (5000 / (6,371,000 + 5000)) ≈ 7.84 meters
        val reduction = GeodesyCalculator.geodeticReductionCorrectionMeters(10000.0, 5000.0)
        assertEquals(7.84, reduction, 0.2)

        // At sea level (0m) reduction is 0
        assertEquals(0.0, GeodesyCalculator.geodeticReductionCorrectionMeters(10000.0, 0.0), 0.001)
    }

    @Test
    fun testVincentyEquatorEllipsoidalDistance() {
        // 1 degree of longitude at equator on WGS-84 reference ellipsoid:
        // equatorial circumference = 2 * PI * 6378137.0 ≈ 40075016.68m
        // 1 degree = 40075016.68 / 360 ≈ 111319.49 meters
        val p1 = GpsPoint(0.0, 0.0, 0.0)
        val p2 = GpsPoint(0.0, 1.0, 0.0)
        val distMeters = GeodesyCalculator.vincentyDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        assertEquals(111319.49, distMeters, 1.0)
    }

    @Test
    fun testVincentyMatchesHaversineClosePoints() {
        // For short hiking distance (~1 km), Vincenty and Haversine should be extremely close (< 0.5% diff)
        val p1 = GpsPoint(55.7500, 37.6100, 0.0)
        val p2 = GpsPoint(55.7590, 37.6100, 0.0)
        val dV = GeodesyCalculator.vincentyDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        val dH = GeodesyCalculator.haversineDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        assertEquals(dV, dH, dV * 0.005)
    }
}
