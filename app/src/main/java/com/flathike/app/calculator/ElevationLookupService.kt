package com.flathike.app.calculator

import com.flathike.app.model.GpsPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Service to detect missing elevation profiles and fetch real ground altitudes
 * using global Digital Elevation Models (DEM).
 *
 * Resilience features:
 * 1. Smart anchor downsampling: for long tracks (>100 points), queries ~150 anchor points
 *    and linearly interpolates intermediate coordinates. This cuts HTTP calls from 20+ down to 1-2,
 *    eliminating HTTP 429 rate limit triggers while preserving the 30m/90m DEM resolution.
 * 2. Multi-tier provider fallback:
 *    - Primary: Open-Meteo Elevation API (Copernicus 90m + SRTM 30m, high speed)
 *    - Fallback 1: OpenTopoData Mapzen (Global 30m DEM)
 *    - Fallback 2: Open-Elevation (SRTM)
 * 3. Rate-limit backoff: automatically waits and retries on HTTP 429.
 */
class ElevationLookupService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val BATCH_SIZE = 100
        private const val MAX_ANCHOR_POINTS = 150
        private const val OPEN_METEO_URL = "https://api.open-meteo.com/v1/elevation"
        private const val OPENTOPO_URL = "https://api.opentopodata.org/v1/mapzen"
        private const val OPEN_ELEVATION_URL = "https://api.open-elevation.com/api/v1/lookup"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /**
         * Checks whether a list of GPS points lacks a real elevation profile (2D track).
         */
        fun isElevationMissing(points: List<GpsPoint>): Boolean {
            if (points.isEmpty()) return false

            // Scenario 1: All points are zero, dummy placeholder, or NaN
            val allZeroOrDummy = points.all { pt ->
                pt.elevation == 0.0 || pt.elevation == -9999.0 || pt.elevation.isNaN()
            }
            if (allZeroOrDummy) return true

            // Scenario 2: All points have identical elevation (artificial 2D line with fixed dummy z)
            if (points.size > 3) {
                val firstEle = points.first().elevation
                val allIdentical = points.all { abs(it.elevation - firstEle) < 0.001 }
                if (allIdentical) return true
            }

            // Scenario 3: Negligible elevation change (< 0.1m) on non-trivial point list starting at 0m
            if (points.size > 5 && points.first().elevation == 0.0) {
                val minEle = points.minOf { it.elevation }
                val maxEle = points.maxOf { it.elevation }
                if ((maxEle - minEle) < 0.1) return true
            }

            return false
        }
    }

    /**
     * Enriches the provided GPS points with real ground elevations from DEM servers.
     * Uses anchor-point sampling and linear interpolation to avoid HTTP 429 rate limits.
     */
    suspend fun fetchElevations(
        points: List<GpsPoint>,
        onProgress: ((Float) -> Unit)? = null
    ): List<GpsPoint> = withContext(Dispatchers.IO) {
        if (points.isEmpty()) return@withContext emptyList()

        if (points.size <= BATCH_SIZE) {
            // Small track: fetch all points directly in a single request
            onProgress?.invoke(0.2f)
            val elevations = fetchBatchWithFallbackAndRetry(points)
            onProgress?.invoke(1.0f)
            return@withContext points.mapIndexed { idx, pt ->
                pt.copy(elevation = elevations.getOrElse(idx) { pt.elevation })
            }
        }

        // Long track (>100 points): Select anchor points to avoid triggering rate limits
        val anchorIndices = selectAnchorIndices(points, MAX_ANCHOR_POINTS)
        val anchorPoints = anchorIndices.map { points[it] }
        val anchorChunks = anchorPoints.chunked(BATCH_SIZE)
        val anchorElevations = ArrayList<Double>(anchorPoints.size)

        var processed = 0
        for ((chunkIndex, chunk) in anchorChunks.withIndex()) {
            if (chunkIndex > 0) {
                // Rate-limit throttle between chunks
                delay(350)
            }
            val chunkElevations = fetchBatchWithFallbackAndRetry(chunk)
            anchorElevations.addAll(chunkElevations)
            processed += chunk.size
            onProgress?.invoke((processed.toFloat() / anchorPoints.size.toFloat()) * 0.85f)
        }

        // Interpolate elevation for all original points
        onProgress?.invoke(0.95f)
        val enriched = interpolateElevations(points, anchorIndices, anchorElevations)
        onProgress?.invoke(1.0f)
        enriched
    }

    /**
     * Selects uniformly distributed anchor indices along cumulative distance.
     * Always includes start (0) and end (N - 1).
     */
    private fun selectAnchorIndices(points: List<GpsPoint>, maxAnchors: Int): List<Int> {
        val n = points.size
        if (n <= maxAnchors) return (0 until n).toList()

        // Calculate cumulative distances
        val cumulative = DoubleArray(n)
        cumulative[0] = 0.0
        for (i in 0 until n - 1) {
            val d = GeodesyCalculator.haversineDistanceMeters(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude
            )
            cumulative[i + 1] = cumulative[i] + d
        }

        val totalDist = cumulative[n - 1]
        val indicesSet = sortedSetOf<Int>()
        indicesSet.add(0)
        indicesSet.add(n - 1)

        val targetCount = min(maxAnchors, n)
        for (step in 1 until targetCount - 1) {
            val targetDist = totalDist * (step.toDouble() / (targetCount - 1).toDouble())
            // Binary search or linear scan for closest point
            var bestIdx = 0
            var bestDiff = Double.MAX_VALUE
            for (j in 0 until n) {
                val diff = abs(cumulative[j] - targetDist)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestIdx = j
                }
            }
            indicesSet.add(bestIdx)
        }

        return indicesSet.toList()
    }

    /**
     * Linearly interpolates elevations for non-anchor points.
     */
    private fun interpolateElevations(
        points: List<GpsPoint>,
        anchorIndices: List<Int>,
        anchorElevations: List<Double>
    ): List<GpsPoint> {
        val n = points.size
        val elevations = DoubleArray(n)

        // Map anchor indices to elevations
        val anchorMap = mutableMapOf<Int, Double>()
        for (i in anchorIndices.indices) {
            val ptIdx = anchorIndices[i]
            val ele = anchorElevations.getOrElse(i) { 0.0 }
            anchorMap[ptIdx] = ele
            elevations[ptIdx] = ele
        }

        // Interpolate between successive anchors
        for (a in 0 until anchorIndices.size - 1) {
            val startIdx = anchorIndices[a]
            val endIdx = anchorIndices[a + 1]
            val startEle = anchorElevations[a]
            val endEle = anchorElevations[a + 1]
            val span = endIdx - startIdx

            if (span > 1) {
                for (k in 1 until span) {
                    val currIdx = startIdx + k
                    val ratio = k.toDouble() / span.toDouble()
                    elevations[currIdx] = startEle + ratio * (endEle - startEle)
                }
            }
        }

        return points.mapIndexed { idx, pt ->
            pt.copy(elevation = elevations[idx])
        }
    }

    /**
     * Tries primary Open-Meteo, then OpenTopoData Mapzen, then Open-Elevation with retries.
     */
    private suspend fun fetchBatchWithFallbackAndRetry(chunk: List<GpsPoint>): List<Double> {
        var lastError: Exception? = null

        // 1. Primary: Open-Meteo (with 429 retry)
        try {
            return executeWithRetry { fetchBatchFromOpenMeteo(chunk) }
        } catch (e: Exception) {
            lastError = e
        }

        // 2. Fallback 1: OpenTopoData Mapzen
        try {
            delay(300)
            return executeWithRetry { fetchBatchFromOpenTopoData(chunk) }
        } catch (e: Exception) {
            lastError = e
        }

        // 3. Fallback 2: Open-Elevation
        try {
            delay(300)
            return executeWithRetry { fetchBatchFromOpenElevation(chunk) }
        } catch (e: Exception) {
            lastError = e
        }

        val msg = lastError?.message ?: "неизвестная сетевая ошибка"
        if (msg.contains("429") || msg.contains("Too Many Requests")) {
            throw IOException(
                "Серверы рельефа временно перегружены (лимит запросов 429). Пожалуйста, подождите 30 секунд и нажмите «Повторить»."
            )
        }
        throw IOException("Не удалось получить высоты DEM: $msg")
    }

    /**
     * Executes block with backoff retry on HTTP 429 rate limit.
     */
    private suspend fun <T> executeWithRetry(block: () -> T): T {
        var delayMs = 1200L
        for (attempt in 1..2) {
            try {
                return block()
            } catch (e: Exception) {
                val isRateLimit = e.message?.contains("429") == true || e.message?.contains("Too Many Requests") == true
                if (isRateLimit && attempt < 2) {
                    delay(delayMs)
                    delayMs *= 2
                } else {
                    throw e
                }
            }
        }
        return block()
    }

    /**
     * Fetches elevations via Open-Meteo Elevation API (Copernicus 90m + SRTM).
     */
    private fun fetchBatchFromOpenMeteo(chunk: List<GpsPoint>): List<Double> {
        val lats = chunk.joinToString(",") { String.format(Locale.US, "%.6f", it.latitude) }
        val lons = chunk.joinToString(",") { String.format(Locale.US, "%.6f", it.longitude) }

        val url = "$OPEN_METEO_URL?latitude=$lats&longitude=$lons"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "FlatHike-Android/1.4 (Elevation-DEM-Enricher)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Open-Meteo вернул HTTP ${response.code}: ${response.message}")
            }
            val bodyStr = response.body?.string() ?: throw IOException("Пустой ответ от Open-Meteo")
            val root = JSONObject(bodyStr)
            val eleArray = root.optJSONArray("elevation")
                ?: throw IOException("Поле 'elevation' отсутствует в ответе Open-Meteo")

            val result = ArrayList<Double>(eleArray.length())
            for (i in 0 until eleArray.length()) {
                val ele = eleArray.optDouble(i, 0.0)
                result.add(if (ele.isNaN()) 0.0 else ele)
            }
            return result
        }
    }

    /**
     * Fetches elevations via OpenTopoData Mapzen API.
     */
    private fun fetchBatchFromOpenTopoData(chunk: List<GpsPoint>): List<Double> {
        val locs = chunk.joinToString("|") {
            "${String.format(Locale.US, "%.6f", it.latitude)},${String.format(Locale.US, "%.6f", it.longitude)}"
        }

        val url = "$OPENTOPO_URL?locations=$locs"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "FlatHike-Android/1.4 (Elevation-DEM-Enricher)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("OpenTopoData вернул HTTP ${response.code}: ${response.message}")
            }
            val bodyStr = response.body?.string() ?: throw IOException("Пустой ответ от OpenTopoData")
            val root = JSONObject(bodyStr)
            val resultsArray = root.optJSONArray("results")
                ?: throw IOException("Поле 'results' отсутствует в ответе OpenTopoData")

            val result = ArrayList<Double>(resultsArray.length())
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i)
                val ele = item?.optDouble("elevation", 0.0) ?: 0.0
                result.add(if (ele.isNaN()) 0.0 else ele)
            }
            return result
        }
    }

    /**
     * Fallback lookup via Open-Elevation API (POST JSON).
     */
    private fun fetchBatchFromOpenElevation(chunk: List<GpsPoint>): List<Double> {
        val locationsArray = JSONArray()
        for (pt in chunk) {
            val loc = JSONObject().apply {
                put("latitude", pt.latitude)
                put("longitude", pt.longitude)
            }
            locationsArray.put(loc)
        }

        val requestJson = JSONObject().apply {
            put("locations", locationsArray)
        }

        val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(OPEN_ELEVATION_URL)
            .post(requestBody)
            .header("User-Agent", "FlatHike-Android/1.4 (Elevation-DEM-Enricher)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Open-Elevation вернул HTTP ${response.code}: ${response.message}")
            }
            val bodyStr = response.body?.string() ?: throw IOException("Пустой ответ от Open-Elevation")
            val root = JSONObject(bodyStr)
            val resultsArray = root.optJSONArray("results")
                ?: throw IOException("Поле 'results' отсутствует в ответе Open-Elevation")

            val result = ArrayList<Double>(resultsArray.length())
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i)
                val ele = item?.optDouble("elevation", 0.0) ?: 0.0
                result.add(if (ele.isNaN()) 0.0 else ele)
            }
            return result
        }
    }
}
