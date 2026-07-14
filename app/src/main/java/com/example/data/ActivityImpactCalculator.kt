package com.example.data

object ActivityImpactCalculator {
    fun calculateImpact(
        activityId: Long,
        progressionStepId: Long?,
        target: TargetHike,
        beforeReadiness: ReadinessResult,
        afterReadiness: ReadinessResult
    ): ActivityReadinessImpact {
        return ActivityReadinessImpact(
            activityId = activityId,
            progressionStepId = progressionStepId,
            overallBefore = beforeReadiness.overallScore,
            overallAfter = afterReadiness.overallScore,
            distanceBefore = beforeReadiness.distanceScore,
            distanceAfter = afterReadiness.distanceScore,
            elevationBefore = beforeReadiness.elevationScore,
            elevationAfter = afterReadiness.elevationScore,
            enduranceBefore = beforeReadiness.enduranceScore,
            enduranceAfter = afterReadiness.enduranceScore,
            recentLoadBefore = beforeReadiness.recentLoadScore,
            recentLoadAfter = afterReadiness.recentLoadScore,
            mainLimiterBefore = beforeReadiness.mainLimiter,
            mainLimiterAfter = afterReadiness.mainLimiter,
            readinessLevelBefore = beforeReadiness.readinessLevel,
            readinessLevelAfter = afterReadiness.readinessLevel
        )
    }
}
