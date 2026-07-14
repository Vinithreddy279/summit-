package com.example

import com.example.data.Activity
import com.example.data.ProgressionStepEntity
import com.example.data.ProgressionStepMatchResult
import com.example.data.ProgressionStepMatcher
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionStepMatcherTest {

    private fun createStep(
        targetDistanceMeters: Double? = null,
        targetElevationGainMeters: Double? = null,
        targetDurationMinutes: Int? = null
    ) = ProgressionStepEntity(
        id = 1L,
        planId = 1L,
        stepNumber = 1,
        type = "hike",
        title = "Test Step",
        targetDistanceMeters = targetDistanceMeters,
        targetElevationGainMeters = targetElevationGainMeters,
        targetDurationMinutes = targetDurationMinutes,
        focusDimension = null,
        isTargetHike = false,
        status = "PENDING"
    )

    private fun createActivity(
        distanceKm: Double = 0.0,
        elevationGainM: Double = 0.0,
        durationSeconds: Long = 0L,
        hasElevationData: Boolean = false
    ) = Activity(
        id = 1L,
        title = "Test Activity",
        sportType = "hike",
        durationSeconds = durationSeconds,
        distanceKm = distanceKm,
        avgSpeedKmh = 0.0,
        maxSpeedKmh = 0.0,
        elevationGainM = elevationGainM,
        gearId = null,
        routePointsJson = "",
        notes = "",
        privacy = "Public",
        hasElevationData = hasElevationData,
        timestamp = System.currentTimeMillis()
    )

    @Test
    fun test_1_no_required_metrics_returns_matched() {
        val step = createStep()
        val activity = createActivity()
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.MATCHED, result)
    }

    @Test
    fun test_2_exact_distance_match() {
        val step = createStep(targetDistanceMeters = 1000.0)
        val activity = createActivity(distanceKm = 1.0)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.MATCHED, result)
    }

    @Test
    fun test_3_distance_ratio_at_80_percent_is_matched() {
        val step = createStep(targetDistanceMeters = 1000.0)
        val activity = createActivity(distanceKm = 0.8)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.MATCHED, result)
    }

    @Test
    fun test_4_distance_ratio_below_80_percent_is_not_matched() {
        val step = createStep(targetDistanceMeters = 1000.0)
        val activity = createActivity(distanceKm = 0.79)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.NOT_MATCHED, result)
    }

    @Test
    fun test_5_exact_duration_match() {
        val step = createStep(targetDurationMinutes = 60)
        val activity = createActivity(durationSeconds = 3600)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.MATCHED, result)
    }

    @Test
    fun test_6_duration_ratio_at_80_percent_is_matched() {
        val step = createStep(targetDurationMinutes = 60)
        val activity = createActivity(durationSeconds = 2880) // 48 mins (80%)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.MATCHED, result)
    }

    @Test
    fun test_7_duration_ratio_below_80_percent_is_not_matched() {
        val step = createStep(targetDurationMinutes = 60)
        val activity = createActivity(durationSeconds = 2800)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.NOT_MATCHED, result)
    }

    @Test
    fun test_8_exact_elevation_match_with_elevation_data() {
        val step = createStep(targetElevationGainMeters = 100.0)
        val activity = createActivity(elevationGainM = 100.0, hasElevationData = true)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.MATCHED, result)
    }

    @Test
    fun test_9_elevation_ratio_at_80_percent_is_matched() {
        val step = createStep(targetElevationGainMeters = 100.0)
        val activity = createActivity(elevationGainM = 80.0, hasElevationData = true)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.MATCHED, result)
    }

    @Test
    fun test_10_elevation_ratio_below_80_percent_is_not_matched() {
        val step = createStep(targetElevationGainMeters = 100.0)
        val activity = createActivity(elevationGainM = 79.0, hasElevationData = true)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.NOT_MATCHED, result)
    }

    @Test
    fun test_11_elevation_required_but_missing_data_returns_not_matched() {
        val step = createStep(targetElevationGainMeters = 100.0)
        val activity = createActivity(elevationGainM = 100.0, hasElevationData = false)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.NOT_MATCHED, result)
    }

    @Test
    fun test_12_all_three_metrics_pass_is_matched() {
        val step = createStep(targetDistanceMeters = 1000.0, targetElevationGainMeters = 100.0, targetDurationMinutes = 60)
        val activity = createActivity(distanceKm = 0.9, elevationGainM = 90.0, durationSeconds = 3000, hasElevationData = true)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.MATCHED, result)
    }

    @Test
    fun test_13_all_three_required_but_one_fails_is_partially_matched() {
        val step = createStep(targetDistanceMeters = 1000.0, targetElevationGainMeters = 100.0, targetDurationMinutes = 60)
        val activity = createActivity(distanceKm = 0.9, elevationGainM = 10.0, durationSeconds = 3000, hasElevationData = true)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.PARTIALLY_MATCHED, result)
    }

    @Test
    fun test_14_all_three_required_but_all_fail_is_not_matched() {
        val step = createStep(targetDistanceMeters = 1000.0, targetElevationGainMeters = 100.0, targetDurationMinutes = 60)
        val activity = createActivity(distanceKm = 0.1, elevationGainM = 10.0, durationSeconds = 300, hasElevationData = true)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.NOT_MATCHED, result)
    }

    @Test
    fun test_15_elevation_unverifiable_but_distance_passed_is_partially_matched() {
        val step = createStep(targetDistanceMeters = 1000.0, targetElevationGainMeters = 100.0)
        val activity = createActivity(distanceKm = 1.0, elevationGainM = 0.0, hasElevationData = false)
        val result = ProgressionStepMatcher.match(step, activity)
        assertEquals(ProgressionStepMatchResult.PARTIALLY_MATCHED, result)
    }
}
