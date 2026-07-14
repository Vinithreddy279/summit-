package com.example

import com.example.data.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityImpactCalculatorTest {

    private fun createDummyTarget() = TargetHike(
        id = 1L,
        name = "Half Dome",
        distanceMeters = 22000.0,
        elevationGainMeters = 1500.0,
        estimatedDurationMinutes = 480,
        maxElevationMeters = 2694.0,
        minElevationMeters = 1219.0,
        gpxPath = null,
        createdAt = System.currentTimeMillis(),
        status = "ACTIVE"
    )

    private fun createDummyReadiness(score: Int, level: ReadinessLevel, limiter: ReadinessDimension) = ReadinessResult(
        overallScore = score,
        distanceScore = score + 5,
        elevationScore = score - 5,
        enduranceScore = score + 10,
        recentLoadScore = score,
        mainLimiter = limiter,
        readinessLevel = level,
        evidence = ReadinessEvidence(0.0, 0.0, 0.0, null, 0.0, 0.0, 0, 0.0, 0.0, 0, 0, 0, emptyList(), 0)
    )

    @Test
    fun test_1_impact_maps_activity_id_and_progression_step_id_properly() {
        val target = createDummyTarget()
        val before = createDummyReadiness(50, ReadinessLevel.BUILDING, ReadinessDimension.DISTANCE)
        val after = createDummyReadiness(65, ReadinessLevel.MODERATE, ReadinessDimension.DISTANCE)

        val impact = ActivityImpactCalculator.calculateImpact(
            activityId = 42L,
            progressionStepId = 101L,
            target = target,
            beforeReadiness = before,
            afterReadiness = after
        )

        assertEquals(42L, impact.activityId)
        assertEquals(101L, impact.progressionStepId)
    }

    @Test
    fun test_2_impact_maps_overall_score_transition() {
        val target = createDummyTarget()
        val before = createDummyReadiness(40, ReadinessLevel.BUILDING, ReadinessDimension.DISTANCE)
        val after = createDummyReadiness(75, ReadinessLevel.READY, ReadinessDimension.ENDURANCE)

        val impact = ActivityImpactCalculator.calculateImpact(
            activityId = 42L,
            progressionStepId = null,
            target = target,
            beforeReadiness = before,
            afterReadiness = after
        )

        assertEquals(40, impact.overallBefore)
        assertEquals(75, impact.overallAfter)
    }

    @Test
    fun test_3_impact_preserves_readiness_level_before_as_single_source_of_truth() {
        val target = createDummyTarget()
        val before = createDummyReadiness(30, ReadinessLevel.NOT_READY, ReadinessDimension.DISTANCE)
        val after = createDummyReadiness(80, ReadinessLevel.READY, ReadinessDimension.ELEVATION)

        val impact = ActivityImpactCalculator.calculateImpact(
            activityId = 1L,
            progressionStepId = null,
            target = target,
            beforeReadiness = before,
            afterReadiness = after
        )

        assertEquals(ReadinessLevel.NOT_READY, impact.readinessLevelBefore)
    }

    @Test
    fun test_4_impact_preserves_readiness_level_after_as_single_source_of_truth() {
        val target = createDummyTarget()
        val before = createDummyReadiness(70, ReadinessLevel.MODERATE, ReadinessDimension.DISTANCE)
        val after = createDummyReadiness(95, ReadinessLevel.HIGHLY_READY, ReadinessDimension.DISTANCE)

        val impact = ActivityImpactCalculator.calculateImpact(
            activityId = 1L,
            progressionStepId = null,
            target = target,
            beforeReadiness = before,
            afterReadiness = after
        )

        assertEquals(ReadinessLevel.HIGHLY_READY, impact.readinessLevelAfter)
    }

    @Test
    fun test_5_impact_handles_null_progression_step_id_for_normal_hikes() {
        val target = createDummyTarget()
        val before = createDummyReadiness(55, ReadinessLevel.BUILDING, ReadinessDimension.DISTANCE)
        val after = createDummyReadiness(62, ReadinessLevel.MODERATE, ReadinessDimension.DISTANCE)

        val impact = ActivityImpactCalculator.calculateImpact(
            activityId = 99L,
            progressionStepId = null,
            target = target,
            beforeReadiness = before,
            afterReadiness = after
        )

        assertEquals(null, impact.progressionStepId)
    }

    @Test
    fun test_6_impact_correctly_maps_all_individual_score_dimensions() {
        val target = createDummyTarget()
        val before = createDummyReadiness(60, ReadinessLevel.MODERATE, ReadinessDimension.ELEVATION)
        val after = createDummyReadiness(80, ReadinessLevel.READY, ReadinessDimension.RECENT_LOAD)

        val impact = ActivityImpactCalculator.calculateImpact(
            activityId = 7L,
            progressionStepId = null,
            target = target,
            beforeReadiness = before,
            afterReadiness = after
        )

        assertEquals(65, impact.distanceBefore)
        assertEquals(85, impact.distanceAfter)
        assertEquals(55, impact.elevationBefore)
        assertEquals(75, impact.elevationAfter)
        assertEquals(70, impact.enduranceBefore)
        assertEquals(90, impact.enduranceAfter)
        assertEquals(60, impact.recentLoadBefore)
        assertEquals(80, impact.recentLoadAfter)
    }
}
