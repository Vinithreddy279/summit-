package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

enum class ReadinessDimension {
    DISTANCE,
    ELEVATION,
    ENDURANCE,
    RECENT_LOAD
}

enum class ReadinessLevel {
    NOT_READY,
    BUILDING,
    MODERATE,
    READY,
    HIGHLY_READY
}

data class ReadinessResult(
    val overallScore: Int,
    val distanceScore: Int,
    val elevationScore: Int?,
    val enduranceScore: Int,
    val recentLoadScore: Int,
    val mainLimiter: ReadinessDimension,
    val readinessLevel: ReadinessLevel,
    val evidence: ReadinessEvidence
) : Serializable

data class ReadinessEvidence(
    val targetDistanceMeters: Double,
    val recentDistanceCapacityMeters: Double,
    val maxRecentDistanceMeters: Double,
    val targetElevationGainMeters: Double?,
    val recentElevationCapacityMeters: Double?,
    val maxRecentElevationGainMeters: Double?,
    val targetDurationMinutes: Int,
    val recentEnduranceCapacityMinutes: Double,
    val maxRecentDurationMinutes: Double,
    val recentActivityCount: Int,
    val recentActiveWeeks: Int,
    val historyActivityCount: Int,
    val activeWeeks: List<Boolean> = emptyList(),
    val validRecentElevationActivityCount: Int = 0
) : Serializable

data class ActivityReadinessImpact(
    val activityId: Long,
    val progressionStepId: Long,
    val overallBefore: Int,
    val overallAfter: Int,
    val distanceBefore: Int,
    val distanceAfter: Int,
    val elevationBefore: Int?,
    val elevationAfter: Int?,
    val enduranceBefore: Int,
    val enduranceAfter: Int,
    val recentLoadBefore: Int,
    val recentLoadAfter: Int,
    val mainLimiterBefore: ReadinessDimension,
    val mainLimiterAfter: ReadinessDimension
) : Serializable

@Entity(
    tableName = "readiness_history",
    foreignKeys = [
        ForeignKey(
            entity = TargetHike::class,
            parentColumns = ["id"],
            childColumns = ["targetHikeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("targetHikeId"),
        Index(value = ["targetHikeId", "activityId"], unique = true)
    ]
)
data class ReadinessHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetHikeId: Long,
    val activityId: Long? = null,
    val progressionPlanId: Long? = null,
    val progressionStepId: Long? = null,
    val overallScore: Int,
    val distanceScore: Int,
    val elevationScore: Int?,
    val enduranceScore: Int,
    val recentLoadScore: Int,
    val mainLimiter: String, // String representation of ReadinessDimension
    val readinessLevel: String, // String representation of ReadinessLevel
    val recordedAt: Long = System.currentTimeMillis(),
    val reason: String // BASELINE, ACTIVITY_IMPACT, PLAN_STARTED, TARGET_COMPLETED
) : Serializable

