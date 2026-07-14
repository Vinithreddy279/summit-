package com.example

import android.location.Location
import com.example.data.GpsFilter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GpsFilterTest {

    private fun createLocation(
        lat: Double,
        lng: Double,
        accuracy: Float,
        speedMps: Float,
        timeMs: Long
    ): Location {
        return Location("gps").apply {
            latitude = lat
            longitude = lng
            this.accuracy = accuracy
            speed = speedMps
            time = timeMs
        }
    }

    @Test
    fun `test stationary GPS drift between 3-10 m is rejected`() {
        // Last accepted point: SF center
        val lastAccepted = createLocation(37.7749, -122.4194, 3.0f, 0.0f, 1000L)

        // A tiny drift of ~4.5 meters
        // 0.00004 degrees of latitude is about 4.4 meters
        val driftPoint = createLocation(37.77494, -122.4194, 5.0f, 0.0f, 2000L)

        val accepted = GpsFilter.shouldAcceptLocation(driftPoint, lastAccepted)
        assertFalse("Stationary GPS drift should be rejected", accepted)
    }

    @Test
    fun `test stationary phone for one minute maintains 0m and rejects updates`() {
        var lastAccepted: Location? = createLocation(37.7749, -122.4194, 4.0f, 0.0f, 1000L)
        var totalDistanceMeters = 0.0

        // Simulating stationary updates for 60 seconds (1 update/sec)
        // Fluctuating slightly within 2-3 meters around SF center
        for (sec in 1..60) {
            val offsetLat = if (sec % 2 == 0) 0.00001 else -0.00001
            val offsetLng = if (sec % 3 == 0) 0.00001 else -0.00001
            val rawUpdate = createLocation(
                37.7749 + offsetLat,
                -122.4194 + offsetLng,
                4.5f,
                0.0f,
                1000L + (sec * 1000L)
            )

            val accepted = GpsFilter.shouldAcceptLocation(rawUpdate, lastAccepted)
            if (accepted) {
                totalDistanceMeters += rawUpdate.distanceTo(lastAccepted!!)
                lastAccepted = rawUpdate
            }
        }

        assertEquals("Stationary phone for one minute must result in 0.0 distance", 0.0, totalDistanceMeters, 0.001)
    }

    @Test
    fun `test slow genuine walking is eventually accepted`() {
        // Walking at 0.8 m/s: moves ~8 meters in 10 seconds
        val lastAccepted = createLocation(37.7749, -122.4194, 3.0f, 0.8f, 1000L)

        // After 10 seconds, user has walked ~8.8 meters (approx 0.00008 lat change)
        val walkPoint = createLocation(37.77498, -122.4194, 3.0f, 0.8f, 11000L)

        val accepted = GpsFilter.shouldAcceptLocation(walkPoint, lastAccepted)
        assertTrue("Slow genuine walking after 10s should be accepted", accepted)
    }

    @Test
    fun `test normal walking and hiking is accepted`() {
        // Walking at 1.4 m/s: moves ~14 meters in 10 seconds
        val lastAccepted = createLocation(37.7749, -122.4194, 4.0f, 1.4f, 1000L)

        val hikePoint = createLocation(37.77505, -122.4194, 4.0f, 1.4f, 11000L)

        val accepted = GpsFilter.shouldAcceptLocation(hikePoint, lastAccepted)
        assertTrue("Normal walking/hiking should be accepted", accepted)
    }

    @Test
    fun `test cycling movement is accepted`() {
        // Cycling at 6.0 m/s (approx 21.6 km/h)
        val lastAccepted = createLocation(37.7749, -122.4194, 3.5f, 6.0f, 1000L)

        // Moves ~30 meters in 5 seconds (approx 0.00027 lat change)
        val cyclePoint = createLocation(37.77517, -122.4194, 3.5f, 6.0f, 6000L)

        val accepted = GpsFilter.shouldAcceptLocation(cyclePoint, lastAccepted)
        assertTrue("Cycling movement should be accepted", accepted)
    }

    @Test
    fun `test poor accuracy fix is rejected`() {
        val lastAccepted = createLocation(37.7749, -122.4194, 3.0f, 1.0f, 1000L)

        // Accuracy is 30m, which exceeds MAX_ACCURACY_METERS (25.0m)
        val poorAccuracyPoint = createLocation(37.7752, -122.4194, 30.0f, 1.0f, 5000L)

        val accepted = GpsFilter.shouldAcceptLocation(poorAccuracyPoint, lastAccepted)
        assertFalse("Poor accuracy fixes must be rejected", accepted)
    }

    @Test
    fun `test sudden GPS jump of 50 to 100 m is rejected`() {
        val lastAccepted = createLocation(37.7749, -122.4194, 3.0f, 1.5f, 1000L)

        // A sudden 80-meter jump in just 1 second (implies speed of 80 m/s, which is impossible)
        val jumpPoint = createLocation(37.77562, -122.4194, 5.0f, 80.0f, 2000L)

        val accepted = GpsFilter.shouldAcceptLocation(jumpPoint, lastAccepted)
        assertFalse("Sudden GPS jumps must be rejected as suspicious", accepted)
    }
}
