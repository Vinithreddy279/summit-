package com.example.data

import java.io.Serializable

enum class ProgressionStepMatchResult {
    MATCHED,
    PARTIALLY_MATCHED,
    NOT_MATCHED
}

object ProgressionStepMatcher {

    fun match(step: ProgressionStepEntity, activity: Activity): ProgressionStepMatchResult {
        // Required metrics flags
        val isDistanceRequired = step.targetDistanceMeters != null && step.targetDistanceMeters > 0.0
        val isDurationRequired = step.targetDurationMinutes != null && step.targetDurationMinutes > 0
        val isElevationRequired = step.targetElevationGainMeters != null && step.targetElevationGainMeters > 0.0

        // Ratio calculations with safety clamps and zero/null guards
        val distanceRatio = if (isDistanceRequired) {
            val actDistM = activity.distanceKm * 1000.0
            val targetM = step.targetDistanceMeters!!
            if (actDistM.isNaN() || actDistM.isInfinite() || actDistM < 0.0) 0.0 else actDistM / targetM
        } else {
            null
        }

        val durationRatio = if (isDurationRequired) {
            val actDurMin = activity.durationSeconds / 60.0
            val targetMin = step.targetDurationMinutes!!.toDouble()
            if (actDurMin.isNaN() || actDurMin.isInfinite() || actDurMin < 0.0) 0.0 else actDurMin / targetMin
        } else {
            null
        }

        val elevationRatio = if (isElevationRequired) {
            if (activity.hasElevationData) {
                val actEle = activity.elevationGainM
                val targetEle = step.targetElevationGainMeters!!
                if (actEle.isNaN() || actEle.isInfinite() || actEle < 0.0) 0.0 else actEle / targetEle
            } else {
                null // Required but unavailable from activity
            }
        } else {
            null
        }

        // Gather all required metrics
        val requiredCount = (if (isDistanceRequired) 1 else 0) +
                (if (isDurationRequired) 1 else 0) +
                (if (isElevationRequired) 1 else 0)

        if (requiredCount == 0) {
            // No required metrics to check
            return ProgressionStepMatchResult.MATCHED
        }

        // Check verification states
        val distancePassed = if (isDistanceRequired) (distanceRatio ?: 0.0) >= 0.80 else true
        val durationPassed = if (isDurationRequired) (durationRatio ?: 0.0) >= 0.80 else true
        val elevationPassed = if (isElevationRequired) {
            if (activity.hasElevationData) {
                (elevationRatio ?: 0.0) >= 0.80
            } else {
                false // Elevation is required but cannot be verified
            }
        } else {
            true
        }

        val isElevationUnverifiable = isElevationRequired && !activity.hasElevationData

        // Check if all required metrics passed
        val allPassed = distancePassed && durationPassed && elevationPassed && !isElevationUnverifiable

        if (allPassed) {
            return ProgressionStepMatchResult.MATCHED
        }

        // Check if at least one required metric passed
        val distanceReached = isDistanceRequired && (distanceRatio ?: 0.0) >= 0.80
        val durationReached = isDurationRequired && (durationRatio ?: 0.0) >= 0.80
        val elevationReached = isElevationRequired && activity.hasElevationData && (elevationRatio ?: 0.0) >= 0.80

        val anyPassed = distanceReached || durationReached || elevationReached

        return if (anyPassed || (isElevationUnverifiable && (distanceReached || durationReached))) {
            ProgressionStepMatchResult.PARTIALLY_MATCHED
        } else {
            ProgressionStepMatchResult.NOT_MATCHED
        }
    }
}
