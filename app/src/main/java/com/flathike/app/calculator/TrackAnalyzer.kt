package com.flathike.app.calculator

import com.flathike.app.model.ElevationProfilePoint
import com.flathike.app.model.GpsPoint
import com.flathike.app.model.RecordedTemperatureMetrics
import com.flathike.app.model.RecordedTimeMetrics
import com.flathike.app.model.SlopeSpeedSummary
import com.flathike.app.model.TrackCalculationResult
import com.flathike.app.model.TrackSegment
import com.flathike.app.model.TrackWaypoint
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Analyzes GPS track coordinates, computing straight lines at sea level,
 * true 3D distances, elevation dynamics, slopes, and hiking physics.
 */
object TrackAnalyzer {

    /**
     * Threshold in meters to filter out consumer GPS altitude noise (jitter).
     */
    const val DEFAULT_ELEVATION_NOISE_THRESHOLD_METERS = 2.0

    /**
     * Maximum number of points sampled for rendering the elevation profile chart.
     */
    const val MAX_PROFILE_SAMPLE_POINTS = 120

    /**
     * Analyzes a list of GPS points.
     *
     * @param points Ordered list of GPS track points
     * @param trackName Display name of the track
     * @param noiseThresholdMeters Minimum elevation change required to count towards ascent/descent
     * @param rawWaypoints Waypoints / intermediate points extracted from track or marked by user
     */
    fun analyze(
        points: List<GpsPoint>,
        trackName: String = "GPS Трек",
        noiseThresholdMeters: Double = DEFAULT_ELEVATION_NOISE_THRESHOLD_METERS,
        rawWaypoints: List<TrackWaypoint> = emptyList(),
        walkingSpeedDefaultKmH: Double = 4.0
    ): TrackCalculationResult {
        if (points.isEmpty()) {
            val dummy = GpsPoint(0.0, 0.0, 0.0)
            return TrackCalculationResult(
                trackName = trackName,
                pointCount = 0,
                startPoint = dummy,
                endPoint = dummy,
                straightLineSeaLevelKm = 0.0,
                straightLine3dKm = 0.0,
                straightLineSeaLevelChordKm = 0.0,
                isClosedLoop = false,
                maxDistanceFromStartKm = 0.0,
                furthestPoint = dummy,
                highestPoint = dummy,
                lowestPoint = dummy,
                trackLengthSeaLevelKm = 0.0,
                trackLength3dKm = 0.0,
                terrainExtensionDifferenceMeters = 0.0,
                geodeticReductionMeters = 0.0,
                totalAscentMeters = 0.0,
                totalDescentMeters = 0.0,
                minElevationMeters = 0.0,
                maxElevationMeters = 0.0,
                avgElevationMeters = 0.0,
                elevationDifferenceMeters = 0.0,
                maxSlopePercent = 0.0,
                avgSlopePercent = 0.0,
                isElevationMissing = false,
                elevationProfile = emptyList(),
                toblerHikingTimeMinutes = 0.0,
                naismithHikingTimeMinutes = 0.0
            )
        }

        val start = points.first()
        val end = points.last()

        if (points.size == 1) {
            return TrackCalculationResult(
                trackName = trackName,
                pointCount = 1,
                startPoint = start,
                endPoint = end,
                straightLineSeaLevelKm = 0.0,
                straightLine3dKm = 0.0,
                straightLineSeaLevelChordKm = 0.0,
                isClosedLoop = false,
                maxDistanceFromStartKm = 0.0,
                furthestPoint = start,
                highestPoint = start,
                lowestPoint = start,
                trackLengthSeaLevelKm = 0.0,
                trackLength3dKm = 0.0,
                terrainExtensionDifferenceMeters = 0.0,
                geodeticReductionMeters = 0.0,
                totalAscentMeters = 0.0,
                totalDescentMeters = 0.0,
                minElevationMeters = start.elevation,
                maxElevationMeters = start.elevation,
                avgElevationMeters = start.elevation,
                elevationDifferenceMeters = 0.0,
                maxSlopePercent = 0.0,
                avgSlopePercent = 0.0,
                isElevationMissing = false,
                elevationProfile = listOf(ElevationProfilePoint(0.0, start.elevation)),
                toblerHikingTimeMinutes = 0.0,
                naismithHikingTimeMinutes = 0.0
            )
        }

        // 1. Straight line metrics from Start to End
        val straightLineSeaLevelKm = GeodesyCalculator.seaLevelStraightLineKm(start, end)
        val straightLine3dKm = GeodesyCalculator.straightLine3dMeters(start, end) / 1000.0
        val straightLineSeaLevelChordKm = GeodesyCalculator.seaLevelChordMeters(start, end) / 1000.0

        // 2. Accumulators along the track
        var totalSeaLevelMeters = 0.0
        var total3dMeters = 0.0
        var totalAscent = 0.0
        var totalDescent = 0.0

        var minEle = start.elevation
        var maxEle = start.elevation
        var weightedEleSum = 0.0
        var highestPt = start
        var lowestPt = start

        var maxDistFromStartM = 0.0
        var furthestPt = start

        var maxSlopePct = 0.0
        var weightedSlopeSum = 0.0
        var toblerHoursTotal = 0.0

        // Track cumulative distances for profile
        val cumulativeDistances = ArrayList<Double>(points.size)
        cumulativeDistances.add(0.0)

        // Hysteresis noise filter for elevation
        var lastAnchorElevation = start.elevation

        // Window accumulator for robust slope detection (avoiding single-meter GPS jitter spikes)
        var slopeWindowDistM = 0.0
        var slopeWindowEleStart = start.elevation

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            if (p2.elevation < minEle) {
                minEle = p2.elevation
                lowestPt = p2
            }
            if (p2.elevation > maxEle) {
                maxEle = p2.elevation
                highestPt = p2
            }


            val dHorizMeters = GeodesyCalculator.haversineDistanceMeters(
                p1.latitude, p1.longitude,
                p2.latitude, p2.longitude
            )
            val dVertMeters = p2.elevation - p1.elevation
            val d3dMeters = GeodesyCalculator.segment3dMeters(p1, p2)

            // Distance-weighted elevation (midpoint of segment × distance)
            val segMidEle = (p1.elevation + p2.elevation) / 2.0
            weightedEleSum += segMidEle * dHorizMeters

            totalSeaLevelMeters += dHorizMeters
            total3dMeters += d3dMeters
            cumulativeDistances.add(totalSeaLevelMeters / 1000.0)

            // Track maximum radial distance from start point
            val distFromStartM = GeodesyCalculator.haversineDistanceMeters(
                start.latitude, start.longitude,
                p2.latitude, p2.longitude
            )
            if (distFromStartM > maxDistFromStartM) {
                maxDistFromStartM = distFromStartM
                furthestPt = p2
            }

            // Elevation hysteresis filtering
            val eleDiffFromAnchor = p2.elevation - lastAnchorElevation
            if (abs(eleDiffFromAnchor) >= noiseThresholdMeters) {
                if (eleDiffFromAnchor > 0) {
                    totalAscent += eleDiffFromAnchor
                } else {
                    totalDescent += abs(eleDiffFromAnchor)
                }
                lastAnchorElevation = p2.elevation
            }

            // Windowed slope calculation: accumulate over at least 10 meters for realistic slope values
            slopeWindowDistM += dHorizMeters
            if (slopeWindowDistM >= 10.0 || i == points.size - 2) {
                if (slopeWindowDistM > 1.0) {
                    val windowVert = p2.elevation - slopeWindowEleStart
                    val windowSlopePct = (abs(windowVert) / slopeWindowDistM) * 100.0
                    if (windowSlopePct > maxSlopePct) {
                        maxSlopePct = min(150.0, windowSlopePct) // Cap reasonable terrestrial hiking max
                    }
                }
                slopeWindowDistM = 0.0
                slopeWindowEleStart = p2.elevation
            }

            // Segment-level slope & Tobler Hiking Function calculation
            if (dHorizMeters > 0.5) {
                val segSlope = dVertMeters / dHorizMeters
                val segSlopePct = abs(segSlope) * 100.0
                weightedSlopeSum += segSlopePct * dHorizMeters

                // Tobler velocity: V = 6 * exp(-3.5 * |slope + 0.05|) in km/h
                val velocityKmH = 6.0 * exp(-3.5 * abs(segSlope + 0.05))
                val segmentHours = (dHorizMeters / 1000.0) / max(0.5, velocityKmH)
                toblerHoursTotal += segmentHours
            }
        }

        val pointCount = points.size
        val avgEle = if (totalSeaLevelMeters > 0) weightedEleSum / totalSeaLevelMeters else (start.elevation + end.elevation) / 2.0
        val totalTrackKm = totalSeaLevelMeters / 1000.0
        val total3dKm = total3dMeters / 1000.0
        val terrainDiffMeters = max(0.0, total3dMeters - totalSeaLevelMeters)
        val geodeticReduction = GeodesyCalculator.geodeticReductionCorrectionMeters(totalSeaLevelMeters, avgEle)

        val avgSlopePct = if (totalSeaLevelMeters > 0) weightedSlopeSum / totalSeaLevelMeters else 0.0

        // Determine if track is a closed loop route (start ≈ finish)
        val startEndDistanceMeters = straightLineSeaLevelKm * 1000.0
        val isClosedLoop = totalTrackKm >= 0.1 && (
                startEndDistanceMeters < 80.0 ||
                        (totalTrackKm > 1.0 && (startEndDistanceMeters / totalSeaLevelMeters) < 0.04)
                )

        // Naismith walking time: 5 km/h flat + 600 m ascent per hour
        val naismithHours = (totalTrackKm / 5.0) + (totalAscent / 600.0)

        // Resample elevation profile for chart
        val profile = buildProfilePoints(points, cumulativeDistances, MAX_PROFILE_SAMPLE_POINTS)
        val isMissingElevation = ElevationLookupService.isElevationMissing(points)

        // Recorded GPS time & speed metrics (if track points contain timestamps)
        val pointsWithTime = points.filter { it.time != null }
        val recordedTimeMetrics: RecordedTimeMetrics? = if (pointsWithTime.size >= 2) {
            val startTime = pointsWithTime.first().time!!
            val endTime = pointsWithTime.last().time!!
            val durationSeconds = (endTime - startTime) / 1000L

            if (durationSeconds > 0) {
                var movingSeconds = 0.0
                var movingDistMeters = 0.0
                val instantaneousSpeeds = ArrayList<Double>()

                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val t1 = p1.time
                    val t2 = p2.time
                    if (t1 != null && t2 != null && t2 > t1) {
                        val dtSec = (t2 - t1) / 1000.0
                        if (dtSec in 0.2..1800.0) {
                            val dsMeters = GeodesyCalculator.segment3dMeters(p1, p2)
                            val speedKmH = (dsMeters / dtSec) * 3.6
                            if (speedKmH > 0.5 && speedKmH < 120.0) {
                                movingSeconds += dtSec
                                movingDistMeters += dsMeters
                                instantaneousSpeeds.add(speedKmH)
                            }
                        }
                    }
                }

                val movingDurationSec = movingSeconds.toLong().coerceAtMost(durationSeconds)
                val stoppedDurationSec = (durationSeconds - movingDurationSec).coerceAtLeast(0L)

                val avgSpeed = if (durationSeconds > 0) {
                    totalTrackKm / (durationSeconds / 3600.0)
                } else 0.0

                val avgMovingSpeed = if (movingDurationSec > 0 && movingDistMeters > 0) {
                    (movingDistMeters / 1000.0) / (movingDurationSec / 3600.0)
                } else avgSpeed

                // Robust max speed (98th percentile to filter outlier GPS spikes)
                val maxSpeed = if (instantaneousSpeeds.isNotEmpty()) {
                    instantaneousSpeeds.sort()
                    val p98Index = (instantaneousSpeeds.size * 0.98).toInt().coerceIn(0, instantaneousSpeeds.size - 1)
                    instantaneousSpeeds[p98Index]
                } else 0.0

                val paceMinPerKm = if (avgMovingSpeed > 0.1) {
                    60.0 / avgMovingSpeed
                } else 0.0

                RecordedTimeMetrics(
                    startTimeMillis = startTime,
                    endTimeMillis = endTime,
                    totalDurationSeconds = durationSeconds,
                    movingDurationSeconds = movingDurationSec,
                    stoppedDurationSeconds = stoppedDurationSec,
                    avgSpeedKmH = avgSpeed,
                    avgMovingSpeedKmH = avgMovingSpeed,
                    maxSpeedKmH = maxSpeed,
                    paceMinutesPerKm = paceMinPerKm
                )
            } else null
        } else null

        // Recorded temperature metrics (if track points contain temperature data)
        val pointsWithTemp = points.mapNotNull { it.temperatureCelsius }
        val recordedTempMetrics: RecordedTemperatureMetrics? = if (pointsWithTemp.isNotEmpty()) {
            val minTemp = pointsWithTemp.minOrNull() ?: 0.0
            val maxTemp = pointsWithTemp.maxOrNull() ?: 0.0
            val avgTemp = pointsWithTemp.average()
            val startTemp = pointsWithTemp.first()
            val endTemp = pointsWithTemp.last()
            RecordedTemperatureMetrics(
                avgCelsius = avgTemp,
                minCelsius = minTemp,
                maxCelsius = maxTemp,
                startCelsius = startTemp,
                endCelsius = endTemp
            )
        } else null

        // Waypoints projected onto track distance
        val resolvedWaypoints = rawWaypoints.map { wpt ->
            var bestIdx = 0
            var bestDistM = Double.MAX_VALUE
            for (idx in points.indices) {
                val d = GeodesyCalculator.haversineDistanceMeters(
                    wpt.latitude, wpt.longitude,
                    points[idx].latitude, points[idx].longitude
                )
                if (d < bestDistM) {
                    bestDistM = d
                    bestIdx = idx
                }
            }
            val trackDistKm = cumulativeDistances[bestIdx]
            val resolvedEle = if (wpt.elevation != 0.0) wpt.elevation else points[bestIdx].elevation
            wpt.copy(
                distanceKm = trackDistKm,
                elevation = resolvedEle
            )
        }.sortedBy { it.distanceKm }

        val slopeSpeedAnalysis = TrackSplitter.analyzeSlopeSpeed(points, cumulativeDistances, walkingSpeedDefaultKmH)
        val slopeSegments = TrackSplitter.splitBySlope(points, cumulativeDistances, walkingSpeedDefaultKmH)
        val distance1KmSegments = TrackSplitter.splitByDistance(points, cumulativeDistances, 1.0, walkingSpeedDefaultKmH)
        val distance5KmSegments = TrackSplitter.splitByDistance(points, cumulativeDistances, 5.0, walkingSpeedDefaultKmH)
        val waypointSegments = TrackSplitter.splitByWaypoints(points, cumulativeDistances, resolvedWaypoints, walkingSpeedDefaultKmH)

        return TrackCalculationResult(
            trackName = trackName,
            pointCount = pointCount,
            startPoint = start,
            endPoint = end,
            straightLineSeaLevelKm = straightLineSeaLevelKm,
            straightLine3dKm = straightLine3dKm,
            straightLineSeaLevelChordKm = straightLineSeaLevelChordKm,
            isClosedLoop = isClosedLoop,
            maxDistanceFromStartKm = maxDistFromStartM / 1000.0,
            furthestPoint = furthestPt,
            highestPoint = highestPt,
            lowestPoint = lowestPt,
            trackLengthSeaLevelKm = totalTrackKm,
            trackLength3dKm = total3dKm,
            terrainExtensionDifferenceMeters = terrainDiffMeters,
            geodeticReductionMeters = geodeticReduction,
            totalAscentMeters = totalAscent,
            totalDescentMeters = totalDescent,
            minElevationMeters = minEle,
            maxElevationMeters = maxEle,
            avgElevationMeters = avgEle,
            elevationDifferenceMeters = maxEle - minEle,
            maxSlopePercent = maxSlopePct,
            avgSlopePercent = avgSlopePct,
            isElevationMissing = isMissingElevation,
            elevationProfile = profile,
            toblerHikingTimeMinutes = toblerHoursTotal * 60.0,
            naismithHikingTimeMinutes = naismithHours * 60.0,
            recordedTimeMetrics = recordedTimeMetrics,
            recordedTemperatureMetrics = recordedTempMetrics,
            waypoints = resolvedWaypoints,
            slopeSpeedAnalysis = slopeSpeedAnalysis,
            slopeSegments = slopeSegments,
            distance1KmSegments = distance1KmSegments,
            distance5KmSegments = distance5KmSegments,
            waypointSegments = waypointSegments
        )
    }

    /**
     * Resamples the elevation profile down to [maxSamples] evenly distributed points
     * along the cumulative distance for high-performance chart rendering.
     */
    private fun buildProfilePoints(
        points: List<GpsPoint>,
        cumulativeDistancesKm: List<Double>,
        maxSamples: Int
    ): List<ElevationProfilePoint> {
        if (points.size <= maxSamples) {
            return points.mapIndexed { i, pt ->
                ElevationProfilePoint(cumulativeDistancesKm[i], pt.elevation)
            }
        }

        val step = (points.size - 1).toDouble() / (maxSamples - 1).toDouble()
        val result = ArrayList<ElevationProfilePoint>(maxSamples)
        for (i in 0 until maxSamples) {
            val index = min(points.size - 1, (i * step).toInt())
            result.add(ElevationProfilePoint(cumulativeDistancesKm[index], points[index].elevation))
        }
        return result
    }
}
