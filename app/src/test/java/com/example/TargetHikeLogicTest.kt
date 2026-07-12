package com.example

import com.example.data.GPSPoint
import com.example.data.RouteDemandLevel
import com.example.data.TargetHikeLogic
import org.junit.Assert.*
import org.junit.Test

class TargetHikeLogicTest {

    @Test
    fun testNormalRouteContinuousElevation() {
        val points = listOf(
            GPSPoint(10.0, 20.0, elevation = 100.0, hasElevation = true),
            GPSPoint(10.1, 20.1, elevation = 150.0, hasElevation = true),
            GPSPoint(10.2, 20.2, elevation = 120.0, hasElevation = true),
            GPSPoint(10.3, 20.3, elevation = 180.0, hasElevation = true)
        )

        val gain = TargetHikeLogic.calculateElevationGain(points)
        val max = TargetHikeLogic.calculateMaxElevation(points)
        val min = TargetHikeLogic.calculateMinElevation(points)
        
        // Expected gain: (150 - 100) + (180 - 120) = 110.0
        assertEquals(110.0, gain, 0.001)
        assertEquals(180.0, max ?: -1.0, 0.001)
        assertEquals(100.0, min ?: -1.0, 0.001)

        val demand = TargetHikeLogic.classifyDemand(5000.0, gain, 95, hasElevationData = true)
        assertEquals(RouteDemandLevel.LOW, demand.climbingDemand)
    }

    @Test
    fun testMissingElevationSegments() {
        val points = listOf(
            GPSPoint(10.0, 20.0, elevation = 520.0, hasElevation = true),
            GPSPoint(10.1, 20.1, elevation = 0.0, hasElevation = false), // missing segment
            GPSPoint(10.2, 20.2, elevation = 530.0, hasElevation = true)
        )

        val gain = TargetHikeLogic.calculateElevationGain(points)
        val max = TargetHikeLogic.calculateMaxElevation(points)
        val min = TargetHikeLogic.calculateMinElevation(points)

        // Since either adjacent point of segment 1->2 or 2->3 lacks elevation, gain must be 0
        assertEquals(0.0, gain, 0.001)
        assertEquals(530.0, max ?: -1.0, 0.001)
        assertEquals(520.0, min ?: -1.0, 0.001)
    }

    @Test
    fun testRealFlatRouteWithValidZeroElevation() {
        val points = listOf(
            GPSPoint(10.0, 20.0, elevation = 0.0, hasElevation = true),
            GPSPoint(10.1, 20.1, elevation = 0.0, hasElevation = true)
        )

        val gain = TargetHikeLogic.calculateElevationGain(points)
        val max = TargetHikeLogic.calculateMaxElevation(points)
        val min = TargetHikeLogic.calculateMinElevation(points)

        assertEquals(0.0, gain, 0.001)
        assertEquals(0.0, max ?: -1.0, 0.001)
        assertEquals(0.0, min ?: -1.0, 0.001)
    }

    @Test
    fun testNoElevationTagRoutes() {
        val points = listOf(
            GPSPoint(10.0, 20.0, elevation = 0.0, hasElevation = false),
            GPSPoint(10.1, 20.1, elevation = 0.0, hasElevation = false)
        )

        val gain = TargetHikeLogic.calculateElevationGain(points)
        val max = TargetHikeLogic.calculateMaxElevation(points)
        val min = TargetHikeLogic.calculateMinElevation(points)

        assertEquals(0.0, gain, 0.001)
        assertNull(max)
        assertNull(min)

        val demand = TargetHikeLogic.classifyDemand(5000.0, gain, 95, hasElevationData = false)
        assertEquals(RouteDemandLevel.UNKNOWN, demand.climbingDemand)
    }

    @Test
    fun testManualTargetMetricInputs() {
        // Distance 5.0 km, elevation gain 200m
        val distance = 5000.0
        val elevationGain = 200.0
        val estimatedMins = TargetHikeLogic.estimateDurationMinutes(distance, elevationGain)

        // Naismith rule: (5.0 / 4) * 60 = 75 mins base + (200 / 100) * 10 = 20 mins elevation = 95 mins total
        assertEquals(95, estimatedMins)
    }
}
