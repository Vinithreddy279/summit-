package com.example.data

import kotlin.math.min

object ProgressionEngine {

    // V1 Cold-start safeguard constants (Step 4I)
    private const val ZERO_HISTORY_MAX_DISTANCE_METERS = 8000.0 // 8 km
    private const val ZERO_HISTORY_MAX_ELEVATION_METERS = 400.0 // 400 m
    private const val ZERO_HISTORY_MAX_DURATION_MINUTES = 180.0 // 180 min

    fun calculate(target: TargetHike, readinessResult: ReadinessResult): ProgressionPlan {
        val overallScore = readinessResult.overallScore
        val evidence = readinessResult.evidence
        val historyActivityCount = evidence.historyActivityCount

        // Step 4B - Plan Eligibility and Limited History
        val isLimitedHistory = when (historyActivityCount) {
            0 -> true
            1, 2 -> true
            else -> false
        }

        if (overallScore >= 90) {
            return ProgressionPlan(
                state = ProgressionPlanState.TARGET_READY,
                targetHikeId = target.id,
                targetName = target.name,
                startingReadinessScore = overallScore,
                mainLimiter = readinessResult.mainLimiter,
                isLimitedHistory = isLimitedHistory,
                buildWeekCount = 0,
                steps = emptyList()
            )
        }

        // Step 4D - Plan Length selection (BUILD week count)
        val buildWeekCount = when (overallScore) {
            in 0..39 -> 4
            in 40..59 -> 3
            in 60..74 -> 2
            in 75..89 -> 1
            else -> 1 // Default fallback, although >=90 is handled above
        }

        // Step 4E - Baseline Capacity extraction and sanitization
        val distanceBaseline = sanitizeBaseline(evidence.recentDistanceCapacityMeters)
        val durationBaseline = sanitizeBaseline(evidence.recentEnduranceCapacityMinutes)

        val targetElevationAvailable = target.hasElevationData
        val elevationBaseline = if (targetElevationAvailable) {
            if (evidence.validRecentElevationActivityCount > 0) {
                sanitizeBaseline(evidence.recentElevationCapacityMeters)
            } else {
                0.0
            }
        } else {
            0.0
        }

        // Step 4F - Overshoot Rule: effectiveBaseline = min(baseline, target)
        val effectiveDistanceBaseline = minOf(distanceBaseline, target.distanceMeters)
        val effectiveDurationBaseline = minOf(durationBaseline, target.estimatedDurationMinutes.toDouble())
        val effectiveElevationBaseline = if (targetElevationAvailable) {
            minOf(elevationBaseline, target.elevationGainMeters)
        } else {
            0.0
        }

        // Generate BUILD steps
        val buildSteps = mutableListOf<ProgressionStep>()
        val mainLimiter = readinessResult.mainLimiter

        for (i in 1..buildWeekCount) {
            val normalProgressFraction = i.toDouble() / (buildWeekCount + 1)

            // Step 4G - Main Limiter Emphasis (+0.10 heuristic)
            var distanceFraction = normalProgressFraction
            var durationFraction = normalProgressFraction
            var elevationFraction = normalProgressFraction

            var stepTitle = "BUILD CONSISTENCY"
            var focusDimension: ReadinessDimension? = null

            when (mainLimiter) {
                ReadinessDimension.DISTANCE -> {
                    distanceFraction = minOf(1.0, normalProgressFraction + 0.10)
                    stepTitle = "BUILD DISTANCE"
                    focusDimension = ReadinessDimension.DISTANCE
                }
                ReadinessDimension.ELEVATION -> {
                    elevationFraction = minOf(1.0, normalProgressFraction + 0.10)
                    stepTitle = "BUILD CLIMBING"
                    focusDimension = ReadinessDimension.ELEVATION
                }
                ReadinessDimension.ENDURANCE -> {
                    durationFraction = minOf(1.0, normalProgressFraction + 0.10)
                    stepTitle = "BUILD ENDURANCE"
                    focusDimension = ReadinessDimension.ENDURANCE
                }
                ReadinessDimension.RECENT_LOAD -> {
                    stepTitle = "BUILD CONSISTENCY"
                    focusDimension = ReadinessDimension.RECENT_LOAD
                }
            }

            // Interpolation
            var rawDistance = effectiveDistanceBaseline + ((target.distanceMeters - effectiveDistanceBaseline) * distanceFraction)
            var rawDuration = effectiveDurationBaseline + ((target.estimatedDurationMinutes - effectiveDurationBaseline) * durationFraction)
            var rawElevation = if (targetElevationAvailable) {
                effectiveElevationBaseline + ((target.elevationGainMeters - effectiveElevationBaseline) * elevationFraction)
            } else {
                null
            }

            // Step 4I - Zero-history conservative start
            if (i == 1 && historyActivityCount == 0) {
                rawDistance = minOf(rawDistance, ZERO_HISTORY_MAX_DISTANCE_METERS)
                rawDuration = minOf(rawDuration, ZERO_HISTORY_MAX_DURATION_MINUTES)
                if (rawElevation != null) {
                    rawElevation = minOf(rawElevation, ZERO_HISTORY_MAX_ELEVATION_METERS)
                }
            }

            // Step 4H - Rounding
            val roundedDistance = roundDistanceToMeters(rawDistance)
            val roundedDuration = roundDurationToMinutes(rawDuration)
            val roundedElevation = if (rawElevation != null) {
                roundElevationToMeters(rawElevation)
            } else {
                null
            }

            // Clamping: 0 <= value <= target demand
            val finalDistance = minOf(roundedDistance, target.distanceMeters)
            val finalDuration = minOf(roundedDuration, target.estimatedDurationMinutes)
            val finalElevation = if (targetElevationAvailable && roundedElevation != null) {
                minOf(roundedElevation, target.elevationGainMeters)
            } else {
                null
            }

            buildSteps.add(
                ProgressionStep(
                    stepNumber = i,
                    type = ProgressionStepType.BUILD,
                    title = stepTitle,
                    targetDistanceMeters = finalDistance,
                    targetElevationGainMeters = finalElevation,
                    targetDurationMinutes = finalDuration,
                    focusDimension = focusDimension,
                    isTargetHike = false
                )
            )
        }

        // Step 4L - Monotonic Build Steps (ensure targets do not decrease across BUILD steps)
        for (i in 1 until buildSteps.size) {
            val prev = buildSteps[i - 1]
            val curr = buildSteps[i]

            val monoDistance = maxOf(curr.targetDistanceMeters ?: 0.0, prev.targetDistanceMeters ?: 0.0)
            val monoDuration = maxOf(curr.targetDurationMinutes ?: 15, prev.targetDurationMinutes ?: 15)
            val monoElevation = if (targetElevationAvailable) {
                maxOf(curr.targetElevationGainMeters ?: 0.0, prev.targetElevationGainMeters ?: 0.0)
            } else {
                null
            }

            // Re-clamp to target demand just in case
            val finalDistance = minOf(monoDistance, target.distanceMeters)
            val finalDuration = minOf(monoDuration, target.estimatedDurationMinutes)
            val finalElevation = if (targetElevationAvailable && monoElevation != null) {
                minOf(monoElevation, target.elevationGainMeters)
            } else {
                null
            }

            buildSteps[i] = curr.copy(
                targetDistanceMeters = finalDistance,
                targetDurationMinutes = finalDuration,
                targetElevationGainMeters = finalElevation
            )
        }

        // Step 4J - Recovery Step
        val stepsList = mutableListOf<ProgressionStep>()
        stepsList.addAll(buildSteps)

        if (buildSteps.isNotEmpty()) {
            val finalBuild = buildSteps.last()
            val recoveryDistanceRaw = (finalBuild.targetDistanceMeters ?: 0.0) * 0.5
            val recoveryDurationRaw = (finalBuild.targetDurationMinutes ?: 0).toDouble() * 0.5
            val recoveryElevationRaw = if (targetElevationAvailable) {
                (finalBuild.targetElevationGainMeters ?: 0.0) * 0.5
            } else {
                null
            }

            val roundedRecDistance = roundDistanceToMeters(recoveryDistanceRaw)
            val roundedRecDuration = roundDurationToMinutes(recoveryDurationRaw)
            val roundedRecElevation = if (recoveryElevationRaw != null) {
                roundElevationToMeters(recoveryElevationRaw)
            } else {
                null
            }

            // Ensure recovery values do not exceed final build values
            val clampedRecDistance = minOf(roundedRecDistance, finalBuild.targetDistanceMeters ?: 0.0)
            val clampedRecDuration = minOf(roundedRecDuration, finalBuild.targetDurationMinutes ?: 15)
            val clampedRecElevation = if (targetElevationAvailable && roundedRecElevation != null) {
                minOf(roundedRecElevation, finalBuild.targetElevationGainMeters ?: 0.0)
            } else {
                null
            }

            stepsList.add(
                ProgressionStep(
                    stepNumber = buildWeekCount + 1,
                    type = ProgressionStepType.RECOVERY,
                    title = "RECOVERY WEEK",
                    targetDistanceMeters = clampedRecDistance,
                    targetElevationGainMeters = clampedRecElevation,
                    targetDurationMinutes = clampedRecDuration,
                    focusDimension = null,
                    isTargetHike = false
                )
            )
        }

        // Step 4K - Target Step
        stepsList.add(
            ProgressionStep(
                stepNumber = buildWeekCount + 2,
                type = ProgressionStepType.TARGET,
                title = target.name,
                targetDistanceMeters = target.distanceMeters,
                targetElevationGainMeters = if (target.hasElevationData) target.elevationGainMeters else null,
                targetDurationMinutes = target.estimatedDurationMinutes,
                focusDimension = null,
                isTargetHike = true
            )
        )

        return ProgressionPlan(
            state = ProgressionPlanState.ACTIVE,
            targetHikeId = target.id,
            targetName = target.name,
            startingReadinessScore = overallScore,
            mainLimiter = mainLimiter,
            isLimitedHistory = isLimitedHistory,
            buildWeekCount = buildWeekCount,
            steps = stepsList
        )
    }

    private fun sanitizeBaseline(value: Double?): Double {
        if (value == null || value.isNaN() || value.isInfinite() || value < 0.0) {
            return 0.0
        }
        return value
    }

    private fun roundDistanceToMeters(meters: Double): Double {
        val km = meters / 1000.0
        val roundedKm = Math.round(km * 2.0) / 2.0
        return maxOf(0.0, roundedKm * 1000.0)
    }

    private fun roundElevationToMeters(meters: Double): Double {
        val rounded = Math.round(meters / 50.0) * 50.0
        return maxOf(0.0, rounded.toDouble())
    }

    private fun roundDurationToMinutes(minutes: Double): Int {
        val rounded = Math.round(minutes / 15.0) * 15
        return maxOf(15, rounded.toInt())
    }
}
