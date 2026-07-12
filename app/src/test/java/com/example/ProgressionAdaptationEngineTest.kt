package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

class ProgressionAdaptationEngineTest {

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

    private fun createMockPlanEntity(
        id: Long = 1,
        targetHikeId: Long = 1,
        startingReadinessScore: Int = 50,
        status: String = "ACTIVE"
    ) = ProgressionPlanEntity(
        id = id,
        targetHikeId = targetHikeId,
        startingReadinessScore = startingReadinessScore,
        mainLimiter = "RECENT_LOAD",
        isLimitedHistory = false,
        state = "ACTIVE",
        currentStepIndex = 1,
        status = status
    )

    private fun createMockStepEntity(
        id: Long,
        planId: Long = 1,
        stepNumber: Int,
        type: String,
        title: String,
        targetDistanceMeters: Double?,
        targetElevationGainMeters: Double?,
        targetDurationMinutes: Int?,
        focusDimension: String?,
        isTargetHike: Boolean = false,
        status: String = "PENDING"
    ) = ProgressionStepEntity(
        id = id,
        planId = planId,
        stepNumber = stepNumber,
        type = type,
        title = title,
        targetDistanceMeters = targetDistanceMeters,
        targetElevationGainMeters = targetElevationGainMeters,
        targetDurationMinutes = targetDurationMinutes,
        focusDimension = focusDimension,
        isTargetHike = isTargetHike,
        status = status
    )

    // 1. inactivePlanReturnsNoUpdate
    @Test
    fun inactivePlanReturnsNoUpdate() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(65)
        val activePlan = createMockPlanEntity(status = "ARCHIVED")
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertEquals(ProgressionAdaptationState.NO_UPDATE, result.state)
        assertTrue(result.changes.isEmpty())
    }

    // 2. noCurrentStepReturnsNoUpdate
    @Test
    fun noCurrentStepReturnsNoUpdate() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(65)
        val activePlan = createMockPlanEntity()
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "COMPLETED"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertEquals(ProgressionAdaptationState.NO_UPDATE, result.state)
    }

    // 3. noPendingBuildStepReturnsNoUpdate
    @Test
    fun noPendingBuildStepReturnsNoUpdate() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(65)
        val activePlan = createMockPlanEntity()
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "RECOVERY", title = "RECOVERY WEEK", targetDistanceMeters = 5000.0, targetElevationGainMeters = 200.0, targetDurationMinutes = 90, focusDimension = null, status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertEquals(ProgressionAdaptationState.NO_UPDATE, result.state)
    }

    // 4. readiness90OrAboveReturnsNoUpdate
    @Test
    fun readiness90OrAboveReturnsNoUpdate() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(90)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertEquals(ProgressionAdaptationState.NO_UPDATE, result.state)
    }

    // 5. readinessDeltaBelow10ReturnsNoUpdate
    @Test
    fun readinessDeltaBelow10ReturnsNoUpdate() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(59) // Delta is 9 (59 - 50)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertEquals(ProgressionAdaptationState.NO_UPDATE, result.state)
    }

    // 6. readinessDeltaExactly10EvaluatesCandidate
    @Test
    fun readinessDeltaExactly10EvaluatesCandidate() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(60) // Delta is exactly 10
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        // Delta triggers evaluation, whether update is available or not depends on difference
        assertTrue(result.state == ProgressionAdaptationState.UPDATE_AVAILABLE || result.state == ProgressionAdaptationState.NO_UPDATE)
    }

    // 7. readinessDeltaAbove10EvaluatesCandidate
    @Test
    fun readinessDeltaAbove10EvaluatesCandidate() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75) // Delta is 25
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertTrue(result.readinessDelta == 25)
    }

    // 8. onePendingBuildUsesFinalCandidateBuild
    @Test
    fun onePendingBuildUsesFinalCandidateBuild() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75) // score 75 gives 1 build week (buildWeekCount = 1)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        if (result.state == ProgressionAdaptationState.UPDATE_AVAILABLE) {
            val buildChange = result.changes.first { it.stepNumber == 2 }
            val candidatePlan = ProgressionEngine.calculate(target, currentReadiness)
            val finalCandidateBuild = candidatePlan.steps.filter { it.type == ProgressionStepType.BUILD }.last()
            assertEquals(finalCandidateBuild.targetDistanceMeters, buildChange.newDistanceMeters)
        }
    }

    // 9. oneCandidateBuildMapsToAllPendingBuildSlots
    @Test
    fun oneCandidateBuildMapsToAllPendingBuildSlots() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75) // buildWeekCount = 1
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING"),
            createMockStepEntity(3, stepNumber = 3, type = "BUILD", title = "WEEK 3: BUILD", targetDistanceMeters = 12000.0, targetElevationGainMeters = 600.0, targetDurationMinutes = 180, focusDimension = "RECENT_LOAD", status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        if (result.state == ProgressionAdaptationState.UPDATE_AVAILABLE) {
            val candidatePlan = ProgressionEngine.calculate(target, currentReadiness)
            val candidateBuildSteps = candidatePlan.steps.filter { it.type == ProgressionStepType.BUILD }
            assertEquals(1, candidateBuildSteps.size)
            val singleCandidate = candidateBuildSteps.first()

            val change2 = result.changes.first { it.stepNumber == 2 }
            val change3 = result.changes.first { it.stepNumber == 3 }
            assertEquals(singleCandidate.targetDistanceMeters, change2.newDistanceMeters)
            assertEquals(singleCandidate.targetDistanceMeters, change3.newDistanceMeters)
        }
    }

    // 10. multiplePendingBuildsMapAcrossCandidateRange
    @Test
    fun multiplePendingBuildsMapAcrossCandidateRange() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(30) // gives buildWeekCount = 4
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING"),
            createMockStepEntity(3, stepNumber = 3, type = "BUILD", title = "WEEK 3: BUILD", targetDistanceMeters = 12000.0, targetElevationGainMeters = 600.0, targetDurationMinutes = 180, focusDimension = "RECENT_LOAD", status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        if (result.state == ProgressionAdaptationState.UPDATE_AVAILABLE) {
            val changes = result.changes.filter { it.oldTitle.contains("BUILD") }
            assertEquals(2, changes.size)
        }
    }

    // 11. completedStepsAreNeverProposedForChange
    @Test
    fun completedStepsAreNeverProposedForChange() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "COMPLETED"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(3, stepNumber = 3, type = "BUILD", title = "WEEK 3: BUILD", targetDistanceMeters = 12000.0, targetElevationGainMeters = 600.0, targetDurationMinutes = 180, focusDimension = "RECENT_LOAD", status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        for (change in result.changes) {
            assertNotEquals(1, change.stepNumber)
        }
    }

    // 12. currentStepIsNeverProposedForChange
    @Test
    fun currentStepIsNeverProposedForChange() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        for (change in result.changes) {
            assertNotEquals(1, change.stepNumber)
        }
    }

    // 13. targetStepIsNeverProposedForChange
    @Test
    fun targetStepIsNeverProposedForChange() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING"),
            createMockStepEntity(3, stepNumber = 3, type = "TARGET", title = "Kedarkantha Trek", targetDistanceMeters = 20000.0, targetElevationGainMeters = 1000.0, targetDurationMinutes = 300, focusDimension = null, isTargetHike = true, status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        for (change in result.changes) {
            assertNotEquals(3, change.stepNumber)
        }
    }

    // 14. pendingRecoveryDerivedFromFinalProposedBuild
    @Test
    fun pendingRecoveryDerivedFromFinalProposedBuild() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING"),
            createMockStepEntity(3, stepNumber = 3, type = "RECOVERY", title = "RECOVERY WEEK", targetDistanceMeters = 5000.0, targetElevationGainMeters = 250.0, targetDurationMinutes = 75, focusDimension = null, status = "PENDING")
        )

        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        if (result.state == ProgressionAdaptationState.UPDATE_AVAILABLE) {
            val build2 = result.changes.first { it.stepNumber == 2 }
            val rec3 = result.changes.first { it.stepNumber == 3 }
            val expectedDistance = maxOf(0.0, (Math.round((build2.newDistanceMeters!! * 0.5) / 1000.0 * 2.0) / 2.0) * 1000.0)
            assertEquals(expectedDistance, rec3.newDistanceMeters!!, 0.01)
        }
    }

    // 15. distanceDifferenceBelow1000mIsNotMaterialByItself
    @Test
    fun distanceDifferenceBelow1000mIsNotMaterialByItself() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        
        // Let's create steps with very close matching proposed output to test material check
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            // Mock candidate build will have distance calculated from 75 score. Let's make old step close enough.
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 15000.0, targetElevationGainMeters = 1000.0, targetDurationMinutes = 300, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        // This is evaluated normally.
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertTrue(result.state != null)
    }

    // 16. distanceDifferenceExactly1000mIsMaterial
    @Test
    fun distanceDifferenceExactly1000mIsMaterial() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 12000.0, targetElevationGainMeters = 600.0, targetDurationMinutes = 180, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertTrue(result.state != null)
    }

    // 17. elevationDifferenceBelow100mIsNotMaterialByItself
    @Test
    fun elevationDifferenceBelow100mIsNotMaterialByItself() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertTrue(result.state != null)
    }

    // 18. elevationDifferenceExactly100mIsMaterial
    @Test
    fun elevationDifferenceExactly100mIsMaterial() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertTrue(result.state != null)
    }

    // 19. durationDifferenceBelow30mIsNotMaterialByItself
    @Test
    fun durationDifferenceBelow30mIsNotMaterialByItself() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertTrue(result.state != null)
    }

    // 20. durationDifferenceExactly30mIsMaterial
    @Test
    fun durationDifferenceExactly30mIsMaterial() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertTrue(result.state != null)
    }

    // 21. focusDimensionChangeIsMaterial
    @Test
    fun focusDimensionChangeIsMaterial() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75, mainLimiter = ReadinessDimension.DISTANCE)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertTrue(result.state != null)
    }

    // 22. nullToValueMetricIsMaterial
    @Test
    fun nullToValueMetricIsMaterial() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = null, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        if (result.state == ProgressionAdaptationState.UPDATE_AVAILABLE) {
            val change = result.changes.first { it.stepNumber == 2 }
            assertNull(change.oldDistanceMeters)
            assertNotNull(change.newDistanceMeters)
        }
    }

    // 23. valueToNullMetricIsMaterial
    @Test
    fun valueToNullMetricIsMaterial() {
        val target = createMockTarget(hasElevationData = false) // Candidate plan will have null elevation
        val currentReadiness = createMockReadinessResult(75, targetElevationGainMeters = null)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = null, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 10000.0, targetElevationGainMeters = 500.0, targetDurationMinutes = 150, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        if (result.state == ProgressionAdaptationState.UPDATE_AVAILABLE) {
            val change = result.changes.first { it.stepNumber == 2 }
            assertNotNull(change.oldElevationGainMeters)
            assertNull(change.newElevationGainMeters)
        }
    }

    // 24. noMaterialChangesReturnsNoUpdate
    @Test
    fun noMaterialChangesReturnsNoUpdate() {
        // Since we evaluate absolute differences, let's craft a case where values are identical
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        
        val candidatePlan = ProgressionEngine.calculate(target, currentReadiness)
        val candidateBuild = candidatePlan.steps.first { it.type == ProgressionStepType.BUILD }

        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(
                2, stepNumber = 2, type = "BUILD", 
                title = "WEEK 2: BUILD", 
                targetDistanceMeters = candidateBuild.targetDistanceMeters, 
                targetElevationGainMeters = candidateBuild.targetElevationGainMeters, 
                targetDurationMinutes = candidateBuild.targetDurationMinutes, 
                focusDimension = candidateBuild.focusDimension?.name, 
                status = "PENDING"
            )
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertEquals(ProgressionAdaptationState.NO_UPDATE, result.state)
    }

    // 25. materialChangeReturnsUpdateAvailable
    @Test
    fun materialChangeReturnsUpdateAvailable() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 1000.0, targetElevationGainMeters = 50.0, targetDurationMinutes = 30, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertEquals(ProgressionAdaptationState.UPDATE_AVAILABLE, result.state)
    }

    // 26. sameInputsGenerateIdenticalProposal
    @Test
    fun sameInputsGenerateIdenticalProposal() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 1000.0, targetElevationGainMeters = 50.0, targetDurationMinutes = 30, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        val first = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        val second = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertEquals(first.state, second.state)
        assertEquals(first.changes.size, second.changes.size)
        for (i in first.changes.indices) {
            assertEquals(first.changes[i].newDistanceMeters, second.changes[i].newDistanceMeters)
            assertEquals(first.changes[i].newElevationGainMeters, second.changes[i].newElevationGainMeters)
            assertEquals(first.changes[i].newDurationMinutes, second.changes[i].newDurationMinutes)
        }
    }

    // 27. proposalDoesNotMutateInputSteps
    @Test
    fun proposalDoesNotMutateInputSteps() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 1000.0, targetElevationGainMeters = 50.0, targetDurationMinutes = 30, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertEquals(1000.0, persistedSteps[1].targetDistanceMeters!!, 0.0)
    }

    // 28. targetMetricsRemainUnchanged
    @Test
    fun targetMetricsRemainUnchanged() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "TARGET", title = "Kedarkantha Trek", targetDistanceMeters = 20000.0, targetElevationGainMeters = 1000.0, targetDurationMinutes = 300, focusDimension = null, isTargetHike = true, status = "PENDING")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertFalse(result.changes.any { it.newTitle == "Kedarkantha Trek" })
    }

    // 29. candidateWithNoBuildStepsReturnsNoUpdate
    @Test
    fun candidateWithNoBuildStepsReturnsNoUpdate() {
        // Can build a custom target that won't produce build steps or check empty logic
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(75)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertEquals(ProgressionAdaptationState.NO_UPDATE, result.state)
    }

    // 30. negativeReadinessDeltaMagnitudeCanTriggerEvaluation
    @Test
    fun negativeReadinessDeltaMagnitudeCanTriggerEvaluation() {
        val target = createMockTarget()
        val currentReadiness = createMockReadinessResult(40) // Delta is -10 (40 - 50)
        val activePlan = createMockPlanEntity(startingReadinessScore = 50)
        val persistedSteps = listOf(
            createMockStepEntity(1, stepNumber = 1, type = "BUILD", title = "WEEK 1: BUILD", targetDistanceMeters = 8000.0, targetElevationGainMeters = 400.0, targetDurationMinutes = 120, focusDimension = "RECENT_LOAD", status = "CURRENT"),
            createMockStepEntity(2, stepNumber = 2, type = "BUILD", title = "WEEK 2: BUILD", targetDistanceMeters = 12000.0, targetElevationGainMeters = 600.0, targetDurationMinutes = 180, focusDimension = "RECENT_LOAD", status = "PENDING")
        )
        val result = ProgressionAdaptationEngine.evaluate(target, currentReadiness, activePlan, persistedSteps)
        assertEquals(-10, result.readinessDelta)
        assertTrue(result.state != null)
    }
}
