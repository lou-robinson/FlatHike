package com.flathike.app.calculator

import com.flathike.app.model.GpsPoint
import kotlin.math.*

/**
 * High-precision geodesy and geometric calculations for GPS tracks.
 *
 * Implements:
 * 1) Great-Circle distance at sea level (orthodrome on WGS-84 reference sphere)
 * 2) 3D Euclidean distance in Earth-Centered, Earth-Fixed (ECEF) coordinates
 * 3) 3D straight-line chord at sea level (tunnel distance through the curvature)
 * 4) Geodesic sea-level reduction (reduction of distance at altitude H to sea level H=0)
 * 5) Segment-level 3D hypotenuse (accounting for slope)
 */
object GeodesyCalculator {

    /**
     * IUGG standard mean Earth radius at sea level (WGS-84 mean radius) in meters.
     * R_1 = (2*a + b) / 3 ≈ 6,371,008.77 m
     */
    const val EARTH_RADIUS_SEA_LEVEL_METERS = 6371008.7714

    /**
     * WGS-84 semi-major axis (equatorial radius) in meters.
     */
    const val WGS84_A = 6378137.0

    /**
     * WGS-84 flattening reciprocal: 1 / 298.257223563
     */
    const val WGS84_F = 1.0 / 298.257223563

    /**
     * WGS-84 semi-minor axis in meters: b = a * (1 - f)
     */
    val WGS84_B = WGS84_A * (1.0 - WGS84_F)

    /**
     * WGS-84 first eccentricity squared: e^2 = 2f - f^2
     */
    val WGS84_E_SQ = 2.0 * WGS84_F - WGS84_F * WGS84_F

    /**
     * Computes the Great-Circle distance at sea level (h = 0) between two coordinates
     * using the numerically stable Haversine formula.
     *
     * @return Distance in meters along the sphere surface at sea level.
     */
    fun haversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
        radiusMeters: Double = EARTH_RADIUS_SEA_LEVEL_METERS
    ): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val sinHalfPhi = sin(deltaPhi / 2.0)
        val sinHalfLambda = sin(deltaLambda / 2.0)

        val a = sinHalfPhi * sinHalfPhi + cos(phi1) * cos(phi2) * sinHalfLambda * sinHalfLambda
        val c = 2.0 * atan2(sqrt(a), sqrt(max(0.0, 1.0 - a)))

        return radiusMeters * c
    }

    /**
     * Computes the high-precision geodesic distance at sea level on the WGS-84 reference ellipsoid
     * using Vincenty's inverse formula. Accurate to within 0.5 mm on Earth's ellipsoid.
     * Falls back to Haversine if points are antipodal and iterations do not converge.
     */
    fun vincentyDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        if (lat1 == lat2 && lon1 == lon2) return 0.0

        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = WGS84_A
        val b = WGS84_B
        val f = WGS84_F

        val u1 = atan((1.0 - f) * tan(phi1))
        val u2 = atan((1.0 - f) * tan(phi2))

        val sinU1 = sin(u1)
        val cosU1 = cos(u1)
        val sinU2 = sin(u2)
        val cosU2 = cos(u2)

        var lambda = deltaLambda
        var lambdaP: Double
        var iterLimit = 100
        var cosSqAlpha: Double
        var sinSigma: Double
        var cos2SigmaM: Double
        var cosSigma: Double
        var sigma: Double

        do {
            val sinLambda = sin(lambda)
            val cosLambda = cos(lambda)
            sinSigma = sqrt(
                (cosU2 * sinLambda) * (cosU2 * sinLambda) +
                        (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
            )
            if (sinSigma == 0.0) return 0.0 // Co-incident points

            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
            sigma = atan2(sinSigma, cosSigma)

            val sinAlpha = (cosU1 * cosU2 * sinLambda) / sinSigma
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha
            cos2SigmaM = if (cosSqAlpha != 0.0) cosSigma - (2.0 * sinU1 * sinU2 / cosSqAlpha) else 0.0

            val c = (f / 16.0) * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha))
            lambdaP = lambda
            lambda = deltaLambda + (1.0 - c) * f * sinAlpha *
                    (sigma + c * sinSigma * (cos2SigmaM + c * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM)))
        } while (abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0)

        if (iterLimit == 0) {
            // Did not converge (nearly antipodal) -> fallback to Haversine
            return haversineDistanceMeters(lat1, lon1, lat2, lon2)
        }

        val uSq = cosSqAlpha * (a * a - b * b) / (b * b)
        val capA = 1.0 + (uSq / 16384.0) * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)))
        val capB = (uSq / 1024.0) * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)))
        val deltaSigma = capB * sinSigma * (cos2SigmaM + (capB / 4.0) * (cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) -
                (capB / 6.0) * cos2SigmaM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)))

        return b * capA * (sigma - deltaSigma)
    }

    /**
     * Ellipsoidal geodesic distance at sea level in kilometers.
     */
    fun seaLevelStraightLineKm(p1: GpsPoint, p2: GpsPoint): Double {
        return vincentyDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude) / 1000.0
    }

    /**
     * Converts geodetic coordinates (lat, lon, elevation) to Earth-Centered, Earth-Fixed (ECEF)
     * Cartesian coordinates (X, Y, Z) in meters using the WGS-84 reference ellipsoid.
     */
    fun toEcef(point: GpsPoint): DoubleArray {
        val phi = Math.toRadians(point.latitude)
        val lambda = Math.toRadians(point.longitude)
        val h = point.elevation

        val sinPhi = sin(phi)
        val cosPhi = cos(phi)
        val sinLambda = sin(lambda)
        val cosLambda = cos(lambda)

        // Prime vertical radius of curvature N(phi)
        val n = WGS84_A / sqrt(1.0 - WGS84_E_SQ * sinPhi * sinPhi)

        val x = (n + h) * cosPhi * cosLambda
        val y = (n + h) * cosPhi * sinLambda
        val z = (n * (1.0 - WGS84_E_SQ) + h) * sinPhi

        return doubleArrayOf(x, y, z)
    }

    /**
     * Computes the true 3D Euclidean straight-line distance (through space / chord)
     * between two points in meters, accounting for their 3D ellipsoidal positions and altitudes.
     */
    fun straightLine3dMeters(p1: GpsPoint, p2: GpsPoint): Double {
        val ecef1 = toEcef(p1)
        val ecef2 = toEcef(p2)

        val dx = ecef2[0] - ecef1[0]
        val dy = ecef2[1] - ecef1[1]
        val dz = ecef2[2] - ecef1[2]

        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Computes the straight-line 3D chord distance through the Earth at sea level (h = 0).
     * For central angle c, chord = 2 * R * sin(c / 2).
     */
    fun seaLevelChordMeters(p1: GpsPoint, p2: GpsPoint): Double {
        val phi1 = Math.toRadians(p1.latitude)
        val phi2 = Math.toRadians(p2.latitude)
        val deltaPhi = Math.toRadians(p2.latitude - p1.latitude)
        val deltaLambda = Math.toRadians(p2.longitude - p1.longitude)

        val sinHalfPhi = sin(deltaPhi / 2.0)
        val sinHalfLambda = sin(deltaLambda / 2.0)

        val a = sinHalfPhi * sinHalfPhi + cos(phi1) * cos(phi2) * sinHalfLambda * sinHalfLambda
        val c = 2.0 * atan2(sqrt(a), sqrt(max(0.0, 1.0 - a)))

        return 2.0 * EARTH_RADIUS_SEA_LEVEL_METERS * sin(c / 2.0)
    }

    /**
     * Computes the 3D distance of a track segment in meters:
     * hypotenuse = sqrt(horizontal_distance^2 + delta_elevation^2).
     */
    fun segment3dMeters(p1: GpsPoint, p2: GpsPoint): Double {
        val dHoriz = haversineDistanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        val dEle = p2.elevation - p1.elevation
        return sqrt(dHoriz * dHoriz + dEle * dEle)
    }

    /**
     * In classic geodesy and cartography, a distance measured on the physical Earth's surface
     * at average elevation H is slightly longer than the projected distance on the reference ellipsoid / sea level:
     *   D_sea = D_ground * (R / (R + H)) ≈ D_ground * (1 - H / R)
     *
     * This function calculates the sea-level reduction correction:
     *   ΔD = D_ground - D_sea = D_ground * (H / (R + H)) in meters.
     */
    fun geodeticReductionCorrectionMeters(groundDistanceMeters: Double, avgElevationMeters: Double): Double {
        if (avgElevationMeters <= 0.0) return 0.0
        return groundDistanceMeters * (avgElevationMeters / (EARTH_RADIUS_SEA_LEVEL_METERS + avgElevationMeters))
    }
}
