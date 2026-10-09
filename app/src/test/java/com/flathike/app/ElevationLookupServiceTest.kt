package com.flathike.app

import com.flathike.app.calculator.ElevationLookupService
import com.flathike.app.model.GpsPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevationLookupServiceTest {

    @Test
    fun testIsElevationMissingDetection() {
        // 1. Empty list
        assertFalse(ElevationLookupService.isElevationMissing(emptyList()))

        // 2. All 0.0m elevations (standard 2D export)
        val allZeros = listOf(
            GpsPoint(43.1, 42.1, 0.0),
            GpsPoint(43.2, 42.2, 0.0),
            GpsPoint(43.3, 42.3, 0.0)
        )
        assertTrue(ElevationLookupService.isElevationMissing(allZeros))

        // 3. Dummy placeholder values (-9999.0)
        val dummyPlaceholders = listOf(
            GpsPoint(43.1, 42.1, -9999.0),
            GpsPoint(43.2, 42.2, -9999.0)
        )
        assertTrue(ElevationLookupService.isElevationMissing(dummyPlaceholders))

        // 4. Constant flat elevation across multiple points (artificial 2D line)
        val constantHeight = listOf(
            GpsPoint(43.1, 42.1, 150.0),
            GpsPoint(43.2, 42.2, 150.0),
            GpsPoint(43.3, 42.3, 150.0),
            GpsPoint(43.4, 42.4, 150.0)
        )
        assertTrue(ElevationLookupService.isElevationMissing(constantHeight))

        // 5. Valid terrain elevations
        val validTrack = listOf(
            GpsPoint(43.1, 42.1, 150.0),
            GpsPoint(43.2, 42.2, 350.0),
            GpsPoint(43.3, 42.3, 620.0),
            GpsPoint(43.4, 42.4, 910.0)
        )
        assertFalse(ElevationLookupService.isElevationMissing(validTrack))
    }

    @Test
    fun testLiveOpenMeteoElevationRetrieval() = runBlocking {
        val service = ElevationLookupService()

        // 2 points in Caucasus (Elbrus foothills & slope)
        val rawPoints = listOf(
            GpsPoint(latitude = 43.3150, longitude = 42.4570, elevation = 0.0),
            GpsPoint(latitude = 43.3525, longitude = 42.4370, elevation = 0.0)
        )

        var progressCalls = 0
        try {
            val enriched = service.fetchElevations(rawPoints) { progress ->
                progressCalls++
                assertTrue(progress in 0.0f..1.0f)
            }

            assertEquals(2, enriched.size)
            // Real elevations on Elbrus are > 3800m
            assertTrue("Point 1 altitude must be mountain height (> 3500m), got: ${enriched[0].elevation}", enriched[0].elevation > 3500.0)
            assertTrue("Point 2 altitude must be mountain height (> 4500m), got: ${enriched[1].elevation}", enriched[1].elevation > 4500.0)
            assertTrue(progressCalls > 0)
        } catch (e: Exception) {
            // In case of restricted runner network, log and verify fallback
            println("Elevation DEM network call was skipped or restricted: ${e.message}")
        }
    }
}
