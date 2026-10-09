package com.flathike.app.calculator

import com.flathike.app.model.GpsPoint
import com.flathike.app.model.SlopeCategory
import com.flathike.app.model.SlopeSpeedSummary
import com.flathike.app.model.TrackSegment
import com.flathike.app.model.TrackWaypoint
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * Splits tracks into logical parts (by slope, distance, or waypoints)
 * and computes average speeds per segment and slope category.
 */
object TrackSplitter {

    /**
     * Tobler's Hiking Function speed in km/h for a given slope gradient.
     * W = 6 * exp(-3.5 * |S + 0.05|)
     * @param slopeGradient dh / ds (e.g. 0.10 for 10% grade)
     */
    fun toblerSpeedKmH(slopeGradient: Double, baseSpeedKmH: Double = 5.0): Double {
        val standardTobler = 6.0 * exp(-3.5 * abs(slopeGradient + 0.05))
        // Scale to user's flat base speed (standard Tobler flat speed is ~5 km/h)
        val factor = (baseSpeedKmH / 5.0).coerceIn(0.5, 2.5)
        return (standardTobler * factor).coerceIn(0.5, 12.0)
    }

    /**
     * Computes slope breakdown and speed per slope category across the entire track.
     */
    fun analyzeSlopeSpeed(
        points: List<GpsPoint>,
        cumulativeDistancesKm: List<Double>,
        baseSpeedKmH: Double = 4.0
    ): List<SlopeSpeedSummary> {
        if (points.size < 2) return emptyList()

        val categoryDistances = mutableMapOf<SlopeCategory, Double>()
        val categoryAscent = mutableMapOf<SlopeCategory, Double>()
        val categoryDescent = mutableMapOf<SlopeCategory, Double>()
        val categoryDurationSec = mutableMapOf<SlopeCategory, Double>()
        val categoryGpsDurSec = mutableMapOf<SlopeCategory, Double>()
        val categoryGpsDistM = mutableMapOf<SlopeCategory, Double>()

        SlopeCategory.entries.forEach { cat ->
            categoryDistances[cat] = 0.0
            categoryAscent[cat] = 0.0
            categoryDescent[cat] = 0.0
            categoryDurationSec[cat] = 0.0
            categoryGpsDurSec[cat] = 0.0
            categoryGpsDistM[cat] = 0.0
        }

        var totalGpsPoints = 0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val dsM = GeodesyCalculator.segment3dMeters(p1, p2)
            if (dsM <= 0.001) continue

            val dEle = p2.elevation - p1.elevation
            val slopeGrad = dEle / dsM
            val slopePct = slopeGrad * 100.0
            val cat = SlopeCategory.fromSlopePercent(slopePct)

            val distKm = dsM / 1000.0
            categoryDistances[cat] = (categoryDistances[cat] ?: 0.0) + distKm
            if (dEle > 0) {
                categoryAscent[cat] = (categoryAscent[cat] ?: 0.0) + dEle
            } else {
                categoryDescent[cat] = (categoryDescent[cat] ?: 0.0) + abs(dEle)
            }

            // Time and speed calculation
            val t1 = p1.time
            val t2 = p2.time
            if (t1 != null && t2 != null && t2 > t1) {
                val dtSec = (t2 - t1) / 1000.0
                if (dtSec in 0.2..1800.0) {
                    val instSpeedKmH = (dsM / dtSec) * 3.6
                    if (instSpeedKmH in 0.3..100.0) {
                        categoryGpsDurSec[cat] = (categoryGpsDurSec[cat] ?: 0.0) + dtSec
                        categoryGpsDistM[cat] = (categoryGpsDistM[cat] ?: 0.0) + dsM
                        totalGpsPoints++
                    }
                }
            }

            // Model-based duration fallback using Tobler formula
            val estSpeedKmH = toblerSpeedKmH(slopeGrad, baseSpeedKmH)
            val estDtSec = (distKm / estSpeedKmH) * 3600.0
            categoryDurationSec[cat] = (categoryDurationSec[cat] ?: 0.0) + estDtSec
        }

        val totalTrackKm = categoryDistances.values.sum()
        val hasSignificantGpsTime = totalGpsPoints > (points.size / 4)

        return SlopeCategory.entries.map { cat ->
            val distKm = categoryDistances[cat] ?: 0.0
            val pctOfTrack = if (totalTrackKm > 0) (distKm / totalTrackKm) * 100.0 else 0.0
            val ascentM = categoryAscent[cat] ?: 0.0
            val descentM = categoryDescent[cat] ?: 0.0

            val gpsDistKm = (categoryGpsDistM[cat] ?: 0.0) / 1000.0
            val gpsDurSec = categoryGpsDurSec[cat] ?: 0.0

            val (avgSpeedKmH, durationSec, isRealGps) = if (hasSignificantGpsTime && gpsDistKm > 0.05 && gpsDurSec > 10.0) {
                val spd = gpsDistKm / (gpsDurSec / 3600.0)
                Triple(spd, gpsDurSec.toLong(), true)
            } else {
                val durSec = categoryDurationSec[cat] ?: 0.0
                val spd = if (durSec > 0) distKm / (durSec / 3600.0) else baseSpeedKmH
                Triple(spd, durSec.toLong(), false)
            }

            SlopeSpeedSummary(
                category = cat,
                totalDistanceKm = distKm,
                percentageOfTrack = pctOfTrack,
                totalAscentMeters = ascentM,
                totalDescentMeters = descentM,
                durationSeconds = durationSec,
                avgSpeedKmH = avgSpeedKmH,
                isRealGpsSpeed = isRealGps
            )
        }
    }

    /**
     * Splits the track into continuous sections where the slope category is consistent.
     * Short jittery micro-segments (< 100m) are merged to produce clean logical segments.
     */
    fun splitBySlope(
        points: List<GpsPoint>,
        cumulativeDistancesKm: List<Double>,
        baseSpeedKmH: Double = 4.0
    ): List<TrackSegment> {
        if (points.size < 2) return emptyList()

        // 1. Compute slope category for each point interval
        data class RawInterval(
            val startIndex: Int,
            val endIndex: Int,
            val category: SlopeCategory,
            val distKm: Double
        )

        val rawIntervals = mutableListOf<RawInterval>()
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val dsM = GeodesyCalculator.segment3dMeters(p1, p2)
            val dEle = p2.elevation - p1.elevation
            val slopeGrad = if (dsM > 0.001) dEle / dsM else 0.0
            val cat = SlopeCategory.fromSlopePercent(slopeGrad * 100.0)
            val distKm = dsM / 1000.0
            rawIntervals.add(RawInterval(i, i + 1, cat, distKm))
        }

        if (rawIntervals.isEmpty()) return emptyList()

        // 2. Group adjacent intervals with the same category
        data class SegmentDraft(
            var startIndex: Int,
            var endIndex: Int,
            var category: SlopeCategory,
            var distKm: Double
        )

        val drafts = mutableListOf<SegmentDraft>()
        var currentDraft = SegmentDraft(
            rawIntervals[0].startIndex,
            rawIntervals[0].endIndex,
            rawIntervals[0].category,
            rawIntervals[0].distKm
        )

        for (i in 1 until rawIntervals.size) {
            val item = rawIntervals[i]
            if (item.category == currentDraft.category) {
                currentDraft.endIndex = item.endIndex
                currentDraft.distKm += item.distKm
            } else {
                drafts.add(currentDraft)
                currentDraft = SegmentDraft(item.startIndex, item.endIndex, item.category, item.distKm)
            }
        }
        drafts.add(currentDraft)

        // 3. Merge short micro-segments (< 0.12 km) into adjacent segments to reduce noise
        val mergedDrafts = mutableListOf<SegmentDraft>()
        for (draft in drafts) {
            if (mergedDrafts.isNotEmpty() && draft.distKm < 0.12) {
                val last = mergedDrafts.last()
                last.endIndex = draft.endIndex
                last.distKm += draft.distKm
            } else {
                mergedDrafts.add(draft)
            }
        }

        val finalDrafts = if (mergedDrafts.isEmpty()) drafts else mergedDrafts

        // 4. Build TrackSegment objects
        return finalDrafts.mapIndexed { idx, draft ->
            buildSegment(
                index = idx + 1,
                namePrefix = "Участок",
                startIndex = draft.startIndex,
                endIndex = draft.endIndex,
                points = points,
                cumulativeDistancesKm = cumulativeDistancesKm,
                baseSpeedKmH = baseSpeedKmH,
                forcedCategory = draft.category
            )
        }
    }

    /**
     * Splits track into equal distance slices (e.g. 1.0 km or 5.0 km splits).
     */
    fun splitByDistance(
        points: List<GpsPoint>,
        cumulativeDistancesKm: List<Double>,
        intervalKm: Double = 1.0,
        baseSpeedKmH: Double = 4.0
    ): List<TrackSegment> {
        if (points.size < 2 || cumulativeDistancesKm.isEmpty()) return emptyList()

        val totalDistKm = cumulativeDistancesKm.last()
        if (totalDistKm <= 0.0) return emptyList()

        val segments = mutableListOf<TrackSegment>()
        var targetKm = intervalKm
        var startIdx = 0
        var segIndex = 1

        for (i in points.indices) {
            val currentDist = cumulativeDistancesKm[i]
            if (currentDist >= targetKm || i == points.size - 1) {
                if (i > startIdx) {
                    val seg = buildSegment(
                        index = segIndex++,
                        namePrefix = "${String.format(java.util.Locale.US, "%.1f", cumulativeDistancesKm[startIdx])}–${String.format(java.util.Locale.US, "%.1f", currentDist)} км",
                        startIndex = startIdx,
                        endIndex = i,
                        points = points,
                        cumulativeDistancesKm = cumulativeDistancesKm,
                        baseSpeedKmH = baseSpeedKmH
                    )
                    segments.add(seg)
                    startIdx = i
                    targetKm += intervalKm
                }
            }
        }

        return segments
    }

    /**
     * Splits track into segments between sequential waypoints.
     */
    fun splitByWaypoints(
        points: List<GpsPoint>,
        cumulativeDistancesKm: List<Double>,
        waypoints: List<TrackWaypoint>,
        baseSpeedKmH: Double = 4.0
    ): List<TrackSegment> {
        if (points.size < 2 || waypoints.isEmpty()) return emptyList()

        // Sort waypoints along the track distance
        val sortedWaypoints = waypoints.sortedBy { it.distanceKm }

        val splitPoints = mutableListOf<Pair<String, Int>>()
        splitPoints.add("Старт" to 0)

        for (wpt in sortedWaypoints) {
            // Find closest track point index
            val closestIdx = points.indices.minByOrNull { idx ->
                abs(cumulativeDistancesKm[idx] - wpt.distanceKm)
            } ?: 0
            if (closestIdx > splitPoints.last().second) {
                splitPoints.add(wpt.name to closestIdx)
            }
        }

        if (splitPoints.last().second < points.size - 1) {
            splitPoints.add("Финиш" to points.size - 1)
        }

        val segments = mutableListOf<TrackSegment>()
        for (i in 0 until splitPoints.size - 1) {
            val from = splitPoints[i]
            val to = splitPoints[i + 1]
            if (to.second > from.second) {
                val seg = buildSegment(
                    index = i + 1,
                    namePrefix = "${from.first} → ${to.first}",
                    startIndex = from.second,
                    endIndex = to.second,
                    points = points,
                    cumulativeDistancesKm = cumulativeDistancesKm,
                    baseSpeedKmH = baseSpeedKmH,
                    fromPointName = from.first,
                    toPointName = to.first
                )
                segments.add(seg)
            }
        }

        return segments
    }

    private fun buildSegment(
        index: Int,
        namePrefix: String,
        startIndex: Int,
        endIndex: Int,
        points: List<GpsPoint>,
        cumulativeDistancesKm: List<Double>,
        baseSpeedKmH: Double,
        forcedCategory: SlopeCategory? = null,
        fromPointName: String? = null,
        toPointName: String? = null
    ): TrackSegment {
        val startPt = points[startIndex]
        val endPt = points[endIndex]
        val startDist = cumulativeDistancesKm[startIndex]
        val endDist = cumulativeDistancesKm[endIndex]
        val distKm = max(0.001, endDist - startDist)

        val startEle = startPt.elevation
        val endEle = endPt.elevation
        val elevDiff = endEle - startEle
        val avgSlopePct = (elevDiff / (distKm * 1000.0)) * 100.0
        val category = forcedCategory ?: SlopeCategory.fromSlopePercent(avgSlopePct)

        // Time and speed calculation
        val tStart = startPt.time
        val tEnd = endPt.time
        val hasGpsTime = tStart != null && tEnd != null && tEnd > tStart
        val durationSec: Long
        val avgSpeedKmH: Double
        val isRealGps: Boolean

        if (hasGpsTime) {
            val totalSec = (tEnd!! - tStart!!) / 1000L
            if (totalSec in 1..86400) {
                durationSec = totalSec
                avgSpeedKmH = distKm / (durationSec / 3600.0)
                isRealGps = true
            } else {
                val estSpeed = toblerSpeedKmH(avgSlopePct / 100.0, baseSpeedKmH)
                durationSec = ((distKm / estSpeed) * 3600.0).toLong()
                avgSpeedKmH = estSpeed
                isRealGps = false
            }
        } else {
            val estSpeed = toblerSpeedKmH(avgSlopePct / 100.0, baseSpeedKmH)
            durationSec = ((distKm / estSpeed) * 3600.0).toLong()
            avgSpeedKmH = estSpeed
            isRealGps = false
        }

        return TrackSegment(
            index = index,
            name = namePrefix,
            startDistanceKm = startDist,
            endDistanceKm = endDist,
            distanceKm = distKm,
            startElevationM = startEle,
            endElevationM = endEle,
            elevationChangeM = elevDiff,
            avgSlopePercent = avgSlopePct,
            slopeCategory = category,
            durationSeconds = durationSec,
            avgSpeedKmH = avgSpeedKmH,
            isRealGpsSpeed = isRealGps,
            fromPointName = fromPointName,
            toPointName = toPointName
        )
    }
}
