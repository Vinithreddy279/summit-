package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

class ProgressionEngineTest {

    private fun createMockTarget(
        id: Long = 1,
        name: String = "Test Trail",
        distanceMeters: Double = 20000.0,
        elevationGainMeters: Double = 1000.0,
        estimatedDurationMinutes: Int = 300,
        hasElevationData: Boolean = true
    ) = TargetHike(
        id = id,
        name = name,
        distanceMeters = distanceMeters,
        elevationGainMeters = elevationGainMeters,
        estimatedDurationMinutes = estimatedDurationMinutes,
        maxElevationMeters = null,
        minElevationMeters = null,
        gpxPath = null,
        status = "ACTIVE",
        hasElevationData = hasElevationData
    )

    private fun createMockReadinessResult(
        overallScore: Int,
        mainLimiter: ReadinessDimension = ReadinessDimension.RECENT_LOAD,
        historyActivityCount: Int = 3,
        recentDistanceCapacityMeters: Double = 5000.0,
        recentEnduranceCapacityMinutes: Double = 90.0,
        recentElevationCapacityMeters: Double = 200.0,
        validRecentElevationActivityCount: Int = 1,
        targetDistanceMeters: Double = 20000.0,
        targetElevationGainMeters: Double? = 1000.0,
        targetDurationMinutes: Int = 300
    ) = ReadinessResult(
        overallScore = overallScore,
        distanceScore = overallScore,
        elevationScore = overallScore,
        enduranceScore = overallScore,
        recentLoadScore = overallScore,
        mainLimiter = mainLimiter,
        readinessLevel = ReadinessLevel.BUILDING,
        evidence = ReadinessEvidence(
            targetDistanceMeters = targetDistanceMeters,
            recentDistanceCapacityMeters = recentDistanceCapacityMeters,
            maxRecentDistanceMeters = recentDistanceCapacityMeters,
            targetElevationGainMeters = targetElevationGainMeters,
            recentElevationCapacityMeters = recentElevationCapacityMeters,
            maxRecentElevationGainMeters = recentElevationCapacityMeters,
            targetDurationMinutes = targetDurationMinutes,
            recentEnduranceCapacityMinutes = recentEnduranceCapacityMinutes,
            maxRecentDurationMinutes = recentEnduranceCapacityMinutes,
            recentActivityCount = historyActivityCount,
            recentActiveWeeks = 2,
            historyActivityCount = historyActivityCount,
            activeWeeks = listOf(true, true),
            validRecentElevationActivityCount = validRecentElevationActivityCount
        )
    )

    @Test
    fun testScore35Generates4Build1Recovery1Target() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(35)
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(ProgressionPlanState.ACTIVE, plan.state)
        assertEquals(4, plan.buildWeekCount)
        assertEquals(6, plan.steps.size) // 4 build + 1 recovery + 1 target
        assertEquals(ProgressionStepType.BUILD, plan.steps[0].type)
        assertEquals(ProgressionStepType.BUILD, plan.steps[1].type)
        assertEquals(ProgressionStepType.BUILD, plan.steps[2].type)
        assertEquals(ProgressionStepType.BUILD, plan.steps[3].type)
        assertEquals(ProgressionStepType.RECOVERY, plan.steps[4].type)
        assertEquals(ProgressionStepType.TARGET, plan.steps[5].type)
    }

    @Test
    fun testScore55Generates3Build1Recovery1Target() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(55)
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(3, plan.buildWeekCount)
        assertEquals(5, plan.steps.size)
        assertEquals(ProgressionStepType.BUILD, plan.steps[0].type)
        assertEquals(ProgressionStepType.BUILD, plan.steps[1].type)
        assertEquals(ProgressionStepType.BUILD, plan.steps[2].type)
        assertEquals(ProgressionStepType.RECOVERY, plan.steps[3].type)
        assertEquals(ProgressionStepType.TARGET, plan.steps[4].type)
    }

    @Test
    fun testScore68Generates2Build1Recovery1Target() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(68)
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(2, plan.buildWeekCount)
        assertEquals(4, plan.steps.size)
        assertEquals(ProgressionStepType.BUILD, plan.steps[0].type)
        assertEquals(ProgressionStepType.BUILD, plan.steps[1].type)
        assertEquals(ProgressionStepType.RECOVERY, plan.steps[2].type)
        assertEquals(ProgressionStepType.TARGET, plan.steps[3].type)
    }

    @Test
    fun testScore82Generates1Build1Recovery1Target() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(82)
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(1, plan.buildWeekCount)
        assertEquals(3, plan.steps.size)
        assertEquals(ProgressionStepType.BUILD, plan.steps[0].type)
        assertEquals(ProgressionStepType.RECOVERY, plan.steps[1].type)
        assertEquals(ProgressionStepType.TARGET, plan.steps[2].type)
    }

    @Test
    fun testScore90ReturnsTargetReady() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(90)
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(ProgressionPlanState.TARGET_READY, plan.state)
        assertEquals(0, plan.buildWeekCount)
        assertTrue(plan.steps.isEmpty())
    }

    @Test
    fun testScore100ReturnsTargetReady() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(100)
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(ProgressionPlanState.TARGET_READY, plan.state)
        assertEquals(0, plan.buildWeekCount)
        assertTrue(plan.steps.isEmpty())
    }

    @Test
    fun testDistanceInterpolationBasicCase() {
        val target = createMockTarget(distanceMeters = 20000.0) // 20 km
        val rr = createMockReadinessResult(
            overallScore = 55, // 3 build weeks
            mainLimiter = ReadinessDimension.RECENT_LOAD, // use recent_load so no distance emphasis
            recentDistanceCapacityMeters = 10000.0 // 10 km baseline
        )
        // buildWeekCount = 3. i.e., i is 1..3. N = 3
        // Week 1 fraction = 1 / 4 = 0.25
        // expected pre-emphasis distance = 10000 + (20000 - 10000) * 0.25 = 12500 m (12.5 km)
        // Week 2 fraction = 2 / 4 = 0.50
        // expected distance = 10000 + 10000 * 0.50 = 15000 m (15 km)
        // Week 3 fraction = 3 / 4 = 0.75
        // expected distance = 10000 + 10000 * 0.75 = 17500 m (17.5 km)
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(5, plan.steps.size)
        assertEquals(12500.0, plan.steps[0].targetDistanceMeters ?: 0.0, 0.01)
        assertEquals(15000.0, plan.steps[1].targetDistanceMeters ?: 0.0, 0.01)
        assertEquals(17500.0, plan.steps[2].targetDistanceMeters ?: 0.0, 0.01)
    }

    @Test
    fun testElevationLimiterEmphasis() {
        // Baseline 400 m, target 1200 m, 3 build weeks.
        // N = 3. Week 1 fraction = 0.25.
        // Emphasized (limiters: +0.10) -> 0.25 + 0.10 = 0.35
        // Raw: 400 + (1200 - 400) * 0.35 = 400 + 800 * 0.35 = 680 m
        // Rounded to nearest 50 m: 680 / 50 = 13.6 -> 14 * 50 = 700 m
        val target = createMockTarget(elevationGainMeters = 1200.0)
        val rr = createMockReadinessResult(
            overallScore = 55, // 3 build weeks
            mainLimiter = ReadinessDimension.ELEVATION,
            recentElevationCapacityMeters = 400.0,
            validRecentElevationActivityCount = 1
        )
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(700.0, plan.steps[0].targetElevationGainMeters ?: 0.0, 0.01)
    }

    @Test
    fun testDistanceLimiterEmphasis() {
        // Baseline 10 km (10000 m), target 20 km (20000 m), 3 build weeks.
        // Week 1 fraction = 0.25.
        // Emphasized fraction = 0.25 + 0.10 = 0.35.
        // Raw: 10000 + (20000 - 10000) * 0.35 = 13500 m (13.5 km)
        // Rounded nearest 0.5 km: 13.5 km is exactly on the half-km mark, rounds to 13.5 km (13500 m)
        val target = createMockTarget(distanceMeters = 20000.0)
        val rr = createMockReadinessResult(
            overallScore = 55,
            mainLimiter = ReadinessDimension.DISTANCE,
            recentDistanceCapacityMeters = 10000.0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(13500.0, plan.steps[0].targetDistanceMeters ?: 0.0, 0.01)
    }

    @Test
    fun testEnduranceLimiterEmphasis() {
        // Baseline 100 min, target 200 min, 3 build weeks.
        // Week 1 fraction = 0.25.
        // Emphasized fraction = 0.25 + 0.10 = 0.35.
        // Raw: 100 + (200 - 100) * 0.35 = 135 min.
        // Rounded to nearest 15 mins: 135 is exactly a multiple of 15 (15 * 9 = 135) -> 135 mins.
        val target = createMockTarget(estimatedDurationMinutes = 200)
        val rr = createMockReadinessResult(
            overallScore = 55,
            mainLimiter = ReadinessDimension.ENDURANCE,
            recentEnduranceCapacityMinutes = 100.0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(135, plan.steps[0].targetDurationMinutes ?: 0)
    }

    @Test
    fun testRecentLoadLimiterDoesNotModifyMetricFractions() {
        val target = createMockTarget(distanceMeters = 20000.0, elevationGainMeters = 1000.0, estimatedDurationMinutes = 300)
        val rr = createMockReadinessResult(
            overallScore = 55,
            mainLimiter = ReadinessDimension.RECENT_LOAD,
            recentDistanceCapacityMeters = 10000.0,
            recentElevationCapacityMeters = 500.0,
            recentEnduranceCapacityMinutes = 150.0,
            validRecentElevationActivityCount = 1
        )
        val plan = ProgressionEngine.calculate(target, rr)
        // Fractions should remain normal: 0.25, 0.50, 0.75
        // Distance: 10000 + 10000 * 0.25 = 12500 m
        assertEquals(12500.0, plan.steps[0].targetDistanceMeters ?: 0.0, 0.01)
    }

    @Test
    fun testBaselineGreaterAndOvershootRule() {
        // Capacity = 25 km, target = 20 km. Effective baseline = 20 km.
        val target = createMockTarget(distanceMeters = 20000.0)
        val rr = createMockReadinessResult(
            overallScore = 55,
            recentDistanceCapacityMeters = 25000.0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        // All build steps should stay at target distance (20 km) rather than descending.
        assertEquals(20000.0, plan.steps[0].targetDistanceMeters ?: 0.0, 0.01)
        assertEquals(20000.0, plan.steps[1].targetDistanceMeters ?: 0.0, 0.01)
        assertEquals(20000.0, plan.steps[2].targetDistanceMeters ?: 0.0, 0.01)
    }

    @Test
    fun testDistanceRounding() {
        val target = createMockTarget(distanceMeters = 20000.0)
        val rr = createMockReadinessResult(
            overallScore = 55,
            recentDistanceCapacityMeters = 8240.0 // 8.24 km
        )
        // Raw at W1 (0.25 fraction) = 8240 + (20000 - 8240) * 0.25 = 11180 m (11.18 km). Nearest 0.5 km is 11.0 km (11000 m)
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(11000.0, plan.steps[0].targetDistanceMeters ?: 0.0, 0.01)
    }

    @Test
    fun testElevationRounding() {
        val target = createMockTarget(elevationGainMeters = 1000.0)
        val rr = createMockReadinessResult(
            overallScore = 82, // 1 build week, i.e., fraction 0.50
            recentElevationCapacityMeters = 424.0,
            validRecentElevationActivityCount = 1,
            targetElevationGainMeters = 424.0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        // Raw = 424 + (1000 - 424) * 0.5 = 712 m. Nearest 50 m is 700 m.
        assertEquals(700.0, plan.steps[0].targetElevationGainMeters ?: 0.0, 0.01)
    }

    @Test
    fun testDurationRounding() {
        val target = createMockTarget(estimatedDurationMinutes = 300)
        val rr = createMockReadinessResult(
            overallScore = 82, // 1 build week
            recentEnduranceCapacityMinutes = 188.0
        )
        // Raw = 188 + (300 - 188) * 0.5 = 244 mins. Nearest 15 mins is 240 mins.
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(240, plan.steps[0].targetDurationMinutes ?: 0)
    }

    @Test
    fun testZeroHistoryFirstBuildDistanceCap() {
        val target = createMockTarget(distanceMeters = 40000.0)
        val rr = createMockReadinessResult(
            overallScore = 35,
            historyActivityCount = 0,
            recentDistanceCapacityMeters = 0.0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        assertTrue((plan.steps[0].targetDistanceMeters ?: 0.0) <= 8000.0)
    }

    @Test
    fun testZeroHistoryFirstBuildElevationCap() {
        val target = createMockTarget(elevationGainMeters = 1200.0)
        val rr = createMockReadinessResult(
            overallScore = 35,
            historyActivityCount = 0,
            recentElevationCapacityMeters = 0.0,
            validRecentElevationActivityCount = 0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        assertTrue((plan.steps[0].targetElevationGainMeters ?: 0.0) <= 400.0)
    }

    @Test
    fun testZeroHistoryFirstBuildDurationCap() {
        val target = createMockTarget(estimatedDurationMinutes = 600)
        val rr = createMockReadinessResult(
            overallScore = 35,
            historyActivityCount = 0,
            recentEnduranceCapacityMinutes = 0.0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        assertTrue((plan.steps[0].targetDurationMinutes ?: 0) <= 180)
    }

    @Test
    fun testZeroHistoryCapsApplyOnlyToFirstBuildStep() {
        val target = createMockTarget(distanceMeters = 100000.0, estimatedDurationMinutes = 1000)
        val rr = createMockReadinessResult(
            overallScore = 35,
            historyActivityCount = 0,
            recentDistanceCapacityMeters = 0.0,
            recentEnduranceCapacityMinutes = 0.0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(8000.0, plan.steps[0].targetDistanceMeters ?: 0.0, 0.01)
        assertTrue((plan.steps[1].targetDistanceMeters ?: 0.0) > 8000.0)
    }

    @Test
    fun testRecoveryFormulaApprox50Percent() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(82)
        val plan = ProgressionEngine.calculate(target, rr)
        val buildStep = plan.steps[0]
        val recoveryStep = plan.steps[1]

        val buildDist = buildStep.targetDistanceMeters ?: 0.0
        val recDist = recoveryStep.targetDistanceMeters ?: 0.0
        assertTrue(recDist <= buildDist)
    }

    @Test
    fun testTargetStepExactDistance() {
        val target = createMockTarget(distanceMeters = 12345.6)
        val rr = createMockReadinessResult(82)
        val plan = ProgressionEngine.calculate(target, rr)
        val targetStep = plan.steps.last()
        assertEquals(12345.6, targetStep.targetDistanceMeters ?: 0.0, 0.001)
    }

    @Test
    fun testTargetStepExactElevationWhenAvailable() {
        val target = createMockTarget(elevationGainMeters = 789.0, hasElevationData = true)
        val rr = createMockReadinessResult(82)
        val plan = ProgressionEngine.calculate(target, rr)
        val targetStep = plan.steps.last()
        assertEquals(789.0, targetStep.targetElevationGainMeters ?: 0.0, 0.001)
    }

    @Test
    fun testTargetStepElevationNullWhenUnavailable() {
        val target = createMockTarget(elevationGainMeters = 789.0, hasElevationData = false)
        val rr = createMockReadinessResult(82)
        val plan = ProgressionEngine.calculate(target, rr)
        val targetStep = plan.steps.last()
        assertNull(targetStep.targetElevationGainMeters)
    }

    @Test
    fun testTargetStepExactDuration() {
        val target = createMockTarget(estimatedDurationMinutes = 234)
        val rr = createMockReadinessResult(82)
        val plan = ProgressionEngine.calculate(target, rr)
        val targetStep = plan.steps.last()
        assertEquals(234, targetStep.targetDurationMinutes ?: 0)
    }

    @Test
    fun testTargetStepIsAlwaysFinal() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(55)
        val plan = ProgressionEngine.calculate(target, rr)
        val lastStep = plan.steps.last()
        assertEquals(ProgressionStepType.TARGET, lastStep.type)
        assertTrue(lastStep.isTargetHike)
    }

    @Test
    fun testBuildStepsMonotonicDistance() {
        val target = createMockTarget(distanceMeters = 30000.0)
        val rr = createMockReadinessResult(
            overallScore = 35,
            recentDistanceCapacityMeters = 5000.0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        for (i in 1..3) {
            val prev = plan.steps[i - 1].targetDistanceMeters ?: 0.0
            val curr = plan.steps[i].targetDistanceMeters ?: 0.0
            assertTrue(curr >= prev)
        }
    }

    @Test
    fun testBuildStepsMonotonicElevation() {
        val target = createMockTarget(elevationGainMeters = 1500.0)
        val rr = createMockReadinessResult(
            overallScore = 35,
            recentElevationCapacityMeters = 200.0,
            validRecentElevationActivityCount = 1
        )
        val plan = ProgressionEngine.calculate(target, rr)
        for (i in 1..3) {
            val prev = plan.steps[i - 1].targetElevationGainMeters ?: 0.0
            val curr = plan.steps[i].targetElevationGainMeters ?: 0.0
            assertTrue(curr >= prev)
        }
    }

    @Test
    fun testBuildStepsMonotonicDuration() {
        val target = createMockTarget(estimatedDurationMinutes = 400)
        val rr = createMockReadinessResult(
            overallScore = 35,
            recentEnduranceCapacityMinutes = 60.0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        for (i in 1..3) {
            val prev = plan.steps[i - 1].targetDurationMinutes ?: 0
            val curr = plan.steps[i].targetDurationMinutes ?: 0
            assertTrue(curr >= prev)
        }
    }

    @Test
    fun testLimitedHistoryTrueFor0Activities() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(55, historyActivityCount = 0)
        val plan = ProgressionEngine.calculate(target, rr)
        assertTrue(plan.isLimitedHistory)
    }

    @Test
    fun testLimitedHistoryTrueFor1Activity() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(55, historyActivityCount = 1)
        val plan = ProgressionEngine.calculate(target, rr)
        assertTrue(plan.isLimitedHistory)
    }

    @Test
    fun testLimitedHistoryTrueFor2Activities() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(55, historyActivityCount = 2)
        val plan = ProgressionEngine.calculate(target, rr)
        assertTrue(plan.isLimitedHistory)
    }

    @Test
    fun testLimitedHistoryFalseFor3PlusActivities() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(55, historyActivityCount = 3)
        val plan = ProgressionEngine.calculate(target, rr)
        assertFalse(plan.isLimitedHistory)
    }

    @Test
    fun testValidFlatTargetElevationProduces0mBuildElevation() {
        val target = createMockTarget(elevationGainMeters = 0.0, hasElevationData = true)
        val rr = createMockReadinessResult(
            overallScore = 55,
            recentElevationCapacityMeters = 0.0,
            validRecentElevationActivityCount = 0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(0.0, plan.steps[0].targetElevationGainMeters ?: -1.0, 0.01)
    }

    @Test
    fun testUnavailableTargetElevationProducesNullElevation() {
        val target = createMockTarget(elevationGainMeters = 1000.0, hasElevationData = false)
        val rr = createMockReadinessResult(55)
        val plan = ProgressionEngine.calculate(target, rr)
        plan.steps.forEach { step ->
            assertNull(step.targetElevationGainMeters)
        }
    }

    @Test
    fun testSameInputsGenerateIdenticalProgressionPlanResults() {
        val target = createMockTarget()
        val rr = createMockReadinessResult(55)
        val plan1 = ProgressionEngine.calculate(target, rr)
        val plan2 = ProgressionEngine.calculate(target, rr)
        assertEquals(plan1, plan2)
    }

    @Test
    fun testInvalidNegativeBaselineCapacitySafelyClampedTo0() {
        val target = createMockTarget(distanceMeters = 20000.0)
        val rr = createMockReadinessResult(
            overallScore = 55,
            recentDistanceCapacityMeters = -500.0
        )
        val plan = ProgressionEngine.calculate(target, rr)
        assertEquals(5000.0, plan.steps[0].targetDistanceMeters ?: 0.0, 0.01)
    }

    @Test
    fun testNaNOrInfiniteBaselineCapacityHandledSafely() {
        val target = createMockTarget(distanceMeters = 20000.0)
        val rr = createMockReadinessResult(
            overallScore = 55,
            recentDistanceCapacityMeters = Double.NaN
        )
        val plan1 = ProgressionEngine.calculate(target, rr)
        assertEquals(5000.0, plan1.steps[0].targetDistanceMeters ?: 0.0, 0.01)

        val rr2 = createMockReadinessResult(
            overallScore = 55,
            recentDistanceCapacityMeters = Double.POSITIVE_INFINITY
        )
        val plan2 = ProgressionEngine.calculate(target, rr2)
        assertEquals(5000.0, plan2.steps[0].targetDistanceMeters ?: 0.0, 0.01)
    }
}
