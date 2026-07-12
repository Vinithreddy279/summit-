package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

class ReadinessEngineTest {

    private val now = 1700000000000L // Sunday, Nov 12, 2023, 22:13:20 UTC
    private val oneDayMs = 24 * 60 * 60 * 1000L

    private val standardTarget = TargetHike(
        id = 1,
        name = "Standard Trek",
        distanceMeters = 20000.0, // 20 km
        elevationGainMeters = 1000.0, // 1000m
        estimatedDurationMinutes = 400, // 400 mins
        maxElevationMeters = 3000.0,
        minElevationMeters = 2000.0,
        gpxPath = null,
        status = "ACTIVE",
        hasElevationData = true
    )

    @Test
    fun testNoActivityHistory() {
        val result = ReadinessEngine.calculate(standardTarget, emptyList(), now)
        assertEquals(0, result.distanceScore)
        assertEquals(0, result.elevationScore ?: -1)
        assertEquals(0, result.enduranceScore)
        assertEquals(0, result.recentLoadScore)
        assertEquals(0, result.overallScore)
        assertEquals(ReadinessLevel.NOT_READY, result.readinessLevel)
        assertEquals(ReadinessDimension.ELEVATION, result.mainLimiter) // tie-break select elevation since all are 0
        assertEquals(0, result.evidence.historyActivityCount)
    }

    @Test
    fun testDistanceScoringAndTop3() {
        // Target 20 km (20000.0 m)
        // Top recent distances: 18 km, 15 km, 12 km, 8 km, 5 km
        // Capacity: (18 + 15 + 12) / 3 = 15 km (15000.0 m)
        // Score: (15 / 20) * 100 = 75
        val activities = listOf(
            createHike(distanceKm = 18.0, ageDays = 5),
            createHike(distanceKm = 15.0, ageDays = 10),
            createHike(distanceKm = 12.0, ageDays = 15),
            createHike(distanceKm = 8.0, ageDays = 20),
            createHike(distanceKm = 5.0, ageDays = 25)
        )

        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(15000.0, result.evidence.recentDistanceCapacityMeters, 0.001)
        assertEquals(18000.0, result.evidence.maxRecentDistanceMeters, 0.001)
        assertEquals(75, result.distanceScore)
    }

    @Test
    fun testTop3WithFourActivities() {
        // Distances 30, 20, 10, 5 km
        // Capacity = (30 + 20 + 10) / 3 = 20 km (20000.0 m)
        val activities = listOf(
            createHike(distanceKm = 30.0, ageDays = 2),
            createHike(distanceKm = 20.0, ageDays = 4),
            createHike(distanceKm = 10.0, ageDays = 6),
            createHike(distanceKm = 5.0, ageDays = 8)
        )

        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(20000.0, result.evidence.recentDistanceCapacityMeters, 0.001)
        assertEquals(30000.0, result.evidence.maxRecentDistanceMeters, 0.001)
        assertEquals(100, result.distanceScore)
    }

    @Test
    fun testActivitiesOlderThan90DaysExcluded() {
        val activities = listOf(
            createHike(distanceKm = 15.0, ageDays = 5),  // included
            createHike(distanceKm = 30.0, ageDays = 91)   // excluded
        )

        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(15000.0, result.evidence.recentDistanceCapacityMeters, 0.001)
        assertEquals(15000.0, result.evidence.maxRecentDistanceMeters, 0.001)
        assertEquals(75, result.distanceScore)
        assertEquals(2, result.evidence.historyActivityCount) // history count includes older
        assertEquals(1, result.evidence.recentActivityCount)  // recent only includes < 90 days
    }

    @Test
    fun testFutureDatedActivitiesExcluded() {
        val activities = listOf(
            createHike(distanceKm = 15.0, ageDays = -5), // future dated, age in days is negative
            createHike(distanceKm = 10.0, ageDays = 2)   // valid
        )

        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(10000.0, result.evidence.recentDistanceCapacityMeters, 0.001)
        assertEquals(1, result.evidence.historyActivityCount) // future are ignored completely
        assertEquals(1, result.evidence.recentActivityCount)
    }

    @Test
    fun testElevationScoringValid() {
        // Target elevation gain = 1000m
        // User capacity = 500m
        // Score = 50
        val activities = listOf(
            createHike(elevationM = 600.0, ageDays = 5),
            createHike(elevationM = 500.0, ageDays = 12),
            createHike(elevationM = 400.0, ageDays = 18)
        )

        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(500.0, result.evidence.recentElevationCapacityMeters ?: 0.0, 0.001)
        assertEquals(50, result.elevationScore ?: -1)
    }

    @Test
    fun testTargetElevationUnavailable() {
        val targetNoElev = standardTarget.copy(hasElevationData = false)
        val activities = listOf(
            createHike(distanceKm = 10.0, elevationM = 500.0, durationSeconds = 12000, ageDays = 5)
        )

        val result = ReadinessEngine.calculate(targetNoElev, activities, now)
        assertNull(result.elevationScore)
        assertNull(result.evidence.targetElevationGainMeters)
        assertNull(result.evidence.recentElevationCapacityMeters)

        // Limiter cannot be Elevation
        assertNotEquals(ReadinessDimension.ELEVATION, result.mainLimiter)
    }

    @Test
    fun testValidFlatTarget() {
        val flatTarget = standardTarget.copy(elevationGainMeters = 0.0, hasElevationData = true)
        val result = ReadinessEngine.calculate(flatTarget, emptyList(), now)
        assertEquals(100, result.elevationScore ?: -1)
    }

    @Test
    fun testEnduranceScoring() {
        // Target duration = 400 mins
        // User top durations: 300 mins (18000s), 200 mins (12000s), 100 mins (6000s)
        // Average: 200 mins
        // Score = 200 / 400 * 100 = 50
        val activities = listOf(
            createHike(durationSeconds = 18000, ageDays = 2),
            createHike(durationSeconds = 12000, ageDays = 5),
            createHike(durationSeconds = 6000, ageDays = 10)
        )

        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(200.0, result.evidence.recentEnduranceCapacityMinutes, 0.001)
        assertEquals(50, result.enduranceScore)
    }

    @Test
    fun testRecentLoadAllFourWeeks() {
        // One hike per week
        val activities = listOf(
            createHike(ageDays = 2),
            createHike(ageDays = 9),
            createHike(ageDays = 16),
            createHike(ageDays = 23)
        )

        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(100, result.recentLoadScore)
        assertEquals(4, result.evidence.recentActiveWeeks)
    }

    @Test
    fun testRecentLoadMultipleActivitiesInOneWeek() {
        // Four hikes in week 0, none in others
        val activities = listOf(
            createHike(ageDays = 1),
            createHike(ageDays = 2),
            createHike(ageDays = 3),
            createHike(ageDays = 4)
        )

        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(25, result.recentLoadScore)
        assertEquals(1, result.evidence.recentActiveWeeks)
    }

    @Test
    fun testActivityExactly28DaysOldExcludedFromLoad() {
        val activities = listOf(
            createHike(ageDays = 28) // age exactly 28 days -> excluded
        )

        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(0, result.recentLoadScore)
        assertEquals(0, result.evidence.recentActiveWeeks)
    }

    @Test
    fun testMainLimiterTieBreak() {
        // All scores are 50
        val activities = listOf(
            createHike(distanceKm = 10.0, elevationM = 500.0, durationSeconds = 12000, ageDays = 5), // 10km (50%), 500m (50%), 200 mins (50%)
            createHike(distanceKm = 10.0, elevationM = 500.0, durationSeconds = 12000, ageDays = 12) // activates week 1, recent active weeks = 2 (50%)
        )

        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        // Scores are indeed:
        // Distance: 10km cap / 20km tar = 50%
        // Elevation: 500m cap / 1000m tar = 50%
        // Endurance: 200m cap / 400m tar = 50%
        // Load: 2 active weeks = 50%
        assertEquals(50, result.distanceScore)
        assertEquals(50, result.elevationScore ?: -1)
        assertEquals(50, result.enduranceScore)
        assertEquals(50, result.recentLoadScore)

        // Tie-break: ELEVATION first!
        assertEquals(ReadinessDimension.ELEVATION, result.mainLimiter)

        // If elevation is unavailable, ENDURANCE is next
        val targetNoElev = standardTarget.copy(hasElevationData = false)
        val resultNoElev = ReadinessEngine.calculate(targetNoElev, activities, now)
        assertEquals(ReadinessDimension.ENDURANCE, resultNoElev.mainLimiter)

        // If endurance score is higher (e.g. 80), but Distance and Recent Load are tie at 50, DISTANCE is next
        val targetHighDur = standardTarget.copy(estimatedDurationMinutes = 100, hasElevationData = false) // endurance score = 200% -> 100
        val resultHighDur = ReadinessEngine.calculate(targetHighDur, activities, now)
        assertEquals(ReadinessDimension.DISTANCE, resultHighDur.mainLimiter)
    }

    @Test
    fun testOverallScoreWithElevation() {
        // Distance Score = 100, Elevation = 50, Endurance = 80, Recent Load = 100
        // Weights: Distance=30%, Elevation=30%, Endurance=25%, Recent Load=15%
        // Expected overall: 100 * 0.30 + 50 * 0.30 + 80 * 0.25 + 100 * 0.15 = 30 + 15 + 20 + 15 = 80
        val target = standardTarget.copy(
            distanceMeters = 10000.0,      // capacity 10km -> score 100
            elevationGainMeters = 1000.0,   // capacity 500m -> score 50
            estimatedDurationMinutes = 250 // capacity 200 mins -> score 80
        )
        val activities = listOf(
            createHike(distanceKm = 10.0, elevationM = 500.0, durationSeconds = 12000, ageDays = 2),
            createHike(distanceKm = 10.0, elevationM = 500.0, durationSeconds = 12000, ageDays = 9),
            createHike(distanceKm = 10.0, elevationM = 500.0, durationSeconds = 12000, ageDays = 16),
            createHike(distanceKm = 10.0, elevationM = 500.0, durationSeconds = 12000, ageDays = 23)
        )

        val result = ReadinessEngine.calculate(target, activities, now)
        assertEquals(80, result.overallScore)
        assertEquals(ReadinessLevel.READY, result.readinessLevel)
    }

    @Test
    fun testOverallScoreWithoutElevation() {
        // Distance Score = 100, Endurance = 80, Recent Load = 100
        // Normalized Weights: Distance=30/70, Endurance=25/70, Recent Load=15/70
        // Expected: (100 * 0.30 + 80 * 0.25 + 100 * 0.15) / 0.70 = (30 + 20 + 15) / 0.7 = 65 / 0.7 = 92.857 -> 93
        val targetNoElev = standardTarget.copy(
            distanceMeters = 10000.0,
            estimatedDurationMinutes = 250,
            hasElevationData = false
        )
        val activities = listOf(
            createHike(distanceKm = 10.0, elevationM = 0.0, durationSeconds = 12000, ageDays = 2),
            createHike(distanceKm = 10.0, elevationM = 0.0, durationSeconds = 12000, ageDays = 9),
            createHike(distanceKm = 10.0, elevationM = 0.0, durationSeconds = 12000, ageDays = 16),
            createHike(distanceKm = 10.0, elevationM = 0.0, durationSeconds = 12000, ageDays = 23)
        )

        val result = ReadinessEngine.calculate(targetNoElev, activities, now)
        assertEquals(93, result.overallScore)
        assertEquals(ReadinessLevel.HIGHLY_READY, result.readinessLevel)
    }

    @Test
    fun testInvalidActivityMetricsIgnored() {
        val activities = listOf(
            createHike(distanceKm = Double.NaN, elevationM = -50.0, durationSeconds = -10, ageDays = 5)
        )
        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(0, result.evidence.historyActivityCount) // completely filtered out
    }

    private fun createHike(
        distanceKm: Double = 10.0,
        elevationM: Double = 300.0,
        durationSeconds: Long = 7200, // 2 hours
        ageDays: Int = 1,
        hasElevationData: Boolean = true
    ): Activity {
        return Activity(
            title = "Test Hike",
            sportType = "hike",
            distanceKm = distanceKm,
            elevationGainM = elevationM,
            durationSeconds = durationSeconds,
            avgSpeedKmh = 5.0,
            maxSpeedKmh = 10.0,
            timestamp = now - ageDays * oneDayMs,
            routePointsJson = "",
            hasElevationData = hasElevationData
        )
    }

    // --- FOCUSED TESTS FOR ELEVATION VALIDITY AND SCENARIOS A-H ---

    @Test
    fun testScenarioA_UnknownElevationActivityExcluded() {
        val activities = listOf(
            createHike(elevationM = 500.0, hasElevationData = false, ageDays = 5)
        )
        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(0.0, result.evidence.recentElevationCapacityMeters ?: 0.0, 0.001)
        assertEquals(0, result.evidence.validRecentElevationActivityCount)
    }

    @Test
    fun testScenarioB_ValidFlatHikeIncluded() {
        val activities = listOf(
            createHike(elevationM = 0.0, hasElevationData = true, ageDays = 5)
        )
        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(0.0, result.evidence.recentElevationCapacityMeters ?: -1.0, 0.001)
        assertEquals(1, result.evidence.validRecentElevationActivityCount)
    }

    @Test
    fun testScenarioC_UnknownAndUnknownAndValidClimb() {
        val activities = listOf(
            createHike(elevationM = 500.0, hasElevationData = false, ageDays = 5),
            createHike(elevationM = 300.0, hasElevationData = false, ageDays = 10),
            createHike(elevationM = 600.0, hasElevationData = true, ageDays = 15)
        )
        val result = ReadinessEngine.calculate(standardTarget, activities, now)
        assertEquals(600.0, result.evidence.recentElevationCapacityMeters ?: 0.0, 0.001)
        assertEquals(1, result.evidence.validRecentElevationActivityCount)
    }

    @Test
    fun testScenarioD_NoValidRecentElevationActivities() {
        val target = standardTarget.copy(elevationGainMeters = 500.0, hasElevationData = true)
        val activities = listOf(
            createHike(elevationM = 500.0, hasElevationData = false, ageDays = 5)
        )
        val result = ReadinessEngine.calculate(target, activities, now)
        assertEquals(0, result.elevationScore ?: -1)
        assertEquals(0, result.evidence.validRecentElevationActivityCount)
    }

    @Test
    fun testScenarioE_ValidFlatRecentElevationHistory() {
        val target = standardTarget.copy(elevationGainMeters = 500.0, hasElevationData = true)
        val activities = listOf(
            createHike(elevationM = 0.0, hasElevationData = true, ageDays = 5)
        )
        val result = ReadinessEngine.calculate(target, activities, now)
        assertEquals(0, result.elevationScore ?: -1)
        assertEquals(1, result.evidence.validRecentElevationActivityCount)
    }

    // Helper function reproducing the segment elevation gain and presence check in save flow
    private fun calculateTrackpointsElevation(points: List<GPSPoint>): Pair<Boolean, Double> {
        var hasElevationData = false
        var eleGain = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (p1.hasElevation && p2.hasElevation) {
                hasElevationData = true
                val diff = p2.elevation - p1.elevation
                if (diff > 0.0) {
                    eleGain += diff
                }
            }
        }
        return Pair(hasElevationData, eleGain)
    }

    @Test
    fun testScenarioF_AdjacentValidRecordedAltitudeSamples() {
        val points = listOf(
            GPSPoint(37.0, -122.0, 100.0, 1L, 1.0, true),
            GPSPoint(37.1, -122.1, 120.0, 2L, 1.0, true),
            GPSPoint(37.2, -122.2, 110.0, 3L, 1.0, true),
            GPSPoint(37.3, -122.3, 150.0, 4L, 1.0, true)
        )
        val (hasEle, gain) = calculateTrackpointsElevation(points)
        assertTrue(hasEle)
        assertEquals(60.0, gain, 0.001) // (120-100) + (150-110) = 20 + 40 = 60
    }

    @Test
    fun testScenarioG_MissingAltitudeBreaksSegment() {
        val points = listOf(
            GPSPoint(37.0, -122.0, 520.0, 1L, 1.0, true),
            GPSPoint(37.1, -122.1, 0.0, 2L, 1.0, false), // missing altitude
            GPSPoint(37.2, -122.2, 530.0, 3L, 1.0, true)
        )
        val (hasEle, gain) = calculateTrackpointsElevation(points)
        assertFalse(hasEle)
        assertEquals(0.0, gain, 0.001)
    }

    @Test
    fun testScenarioH_RealZeroAltitudeSamples() {
        val points = listOf(
            GPSPoint(37.0, -122.0, 0.0, 1L, 1.0, true),
            GPSPoint(37.1, -122.1, 5.0, 2L, 1.0, true),
            GPSPoint(37.2, -122.2, 0.0, 3L, 1.0, true),
            GPSPoint(37.3, -122.3, 10.0, 4L, 1.0, true)
        )
        val (hasEle, gain) = calculateTrackpointsElevation(points)
        assertTrue(hasEle)
        assertEquals(15.0, gain, 0.001) // (5-0) + (10-0) = 15
    }
}
