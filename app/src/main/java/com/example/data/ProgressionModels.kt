package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

enum class ProgressionPlanState {
    TARGET_READY,
    ACTIVE
}

enum class ProgressionStepType {
    BUILD,
    RECOVERY,
    TARGET
}

data class ProgressionStep(
    val stepNumber: Int,
    val type: ProgressionStepType,
    val title: String,
    val targetDistanceMeters: Double?,
    val targetElevationGainMeters: Double?,
    val targetDurationMinutes: Int?,
    val focusDimension: ReadinessDimension?,
    val isTargetHike: Boolean
) : Serializable

data class ProgressionPlan(
    val state: ProgressionPlanState,
    val targetHikeId: Long,
    val targetName: String,
    val startingReadinessScore: Int,
    val mainLimiter: ReadinessDimension,
    val isLimitedHistory: Boolean,
    val buildWeekCount: Int,
    val steps: List<ProgressionStep>
) : Serializable

@Entity(tableName = "progression_plans")
data class ProgressionPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetHikeId: Long,
    val startingReadinessScore: Int,
    val mainLimiter: String, // String representation of ReadinessDimension
    val isLimitedHistory: Boolean,
    val state: String, // ProgressionPlanState representation
    val currentStepIndex: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String // "ACTIVE", "COMPLETED", "ARCHIVED"
) : Serializable

@Entity(
    tableName = "progression_steps",
    foreignKeys = [
        ForeignKey(
            entity = ProgressionPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class ProgressionStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val stepNumber: Int,
    val type: String, // "BUILD", "RECOVERY", "TARGET"
    val title: String,
    val targetDistanceMeters: Double?,
    val targetElevationGainMeters: Double?,
    val targetDurationMinutes: Int?,
    val focusDimension: String?, // String representation of ReadinessDimension or null
    val isTargetHike: Boolean,
    val status: String, // "PENDING", "CURRENT", "COMPLETED", "SKIPPED"
    val completedActivityId: Long? = null,
    val completedAt: Long? = null
) : Serializable

