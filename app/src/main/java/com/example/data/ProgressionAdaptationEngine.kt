package com.example.data

import java.io.Serializable
import kotlin.math.abs

enum class ProgressionAdaptationState {
    NO_UPDATE,
    UPDATE_AVAILABLE
}

data class ProgressionStepChange(
    val stepId: Long,
    val stepNumber: Int,
    val oldTitle: String,
    val newTitle: String,
    val oldDistanceMeters: Double?,
    val newDistanceMeters: Double?,
    val oldElevationGainMeters: Double?,
    val newElevationGainMeters: Double?,
    val oldDurationMinutes: Int?,
    val newDurationMinutes: Int?,
    val oldFocusDimension: ReadinessDimension?,
    val newFocusDimension: ReadinessDimension?
) : Serializable

data class ProgressionAdaptationProposal(
    val state: ProgressionAdaptationState,
    val planId: Long,
    val readinessAtPlanStart: Int,
    val currentReadiness: Int,
    val readinessDelta: Int,
    val changes: List<ProgressionStepChange>
) : Serializable

object ProgressionAdaptationEngine {

    fun evaluate(
        target: TargetHike,
        currentReadiness: ReadinessResult,
        activePlan: ProgressionPlanEntity,
        persistedSteps: List<ProgressionStepEntity>
    ): ProgressionAdaptationProposal {
        val startingReadinessScore = activePlan.startingReadinessScore
        val currentScore = currentReadiness.overallScore
        val readinessDelta = currentScore - startingReadinessScore
        val absoluteReadinessDelta = abs(readinessDelta)

        val noUpdateProposal = ProgressionAdaptationProposal(
            state = ProgressionAdaptationState.NO_UPDATE,
            planId = activePlan.id,
            readinessAtPlanStart = startingReadinessScore,
            currentReadiness = currentScore,
            readinessDelta = readinessDelta,
            changes = emptyList()
        )

        // 1. Eligibility Checks (PART J)
        if (activePlan.status != "ACTIVE") {
            return noUpdateProposal
        }

        val hasCurrentStep = persistedSteps.any { it.status == "CURRENT" }
        if (!hasCurrentStep) {
            return noUpdateProposal
        }

        val hasPendingBuildStep = persistedSteps.any { it.status == "PENDING" && it.type == "BUILD" }
        if (!hasPendingBuildStep) {
            return noUpdateProposal
        }

        if (currentScore >= 90) {
            return noUpdateProposal
        }

        if (absoluteReadinessDelta < 10) {
            return noUpdateProposal
        }

        // 2. Generate Candidate Plan (PART K)
        val candidatePlan = ProgressionEngine.calculate(target, currentReadiness)
        if (candidatePlan.state != ProgressionPlanState.ACTIVE) {
            return noUpdateProposal
        }

        val candidateBuildSteps = candidatePlan.steps.filter { it.type == ProgressionStepType.BUILD }.sortedBy { it.stepNumber }
        if (candidateBuildSteps.isEmpty()) {
            return noUpdateProposal
        }

        val existingPendingBuildSteps = persistedSteps.filter { it.status == "PENDING" && it.type == "BUILD" }.sortedBy { it.stepNumber }
        if (existingPendingBuildSteps.isEmpty()) {
            return noUpdateProposal
        }

        // 3. Map Candidate Build Steps (PART L)
        val proposedChanges = mutableListOf<ProgressionStepChange>()
        val mappedProposedBuildSteps = mutableListOf<ProgressionStepChange>()

        for (j in existingPendingBuildSteps.indices) {
            val oldStep = existingPendingBuildSteps[j]
            val candidateIndex = when {
                existingPendingBuildSteps.size == 1 -> candidateBuildSteps.lastIndex
                candidateBuildSteps.size == 1 -> 0
                else -> {
                    val raw = j * (candidateBuildSteps.lastIndex.toDouble() / existingPendingBuildSteps.lastIndex)
                    Math.round(raw).toInt().coerceIn(0, candidateBuildSteps.lastIndex)
                }
            }
            val candidateStep = candidateBuildSteps[candidateIndex]

            val newTitle = candidateStep.title.replace(Regex("WEEK \\d+"), "WEEK ${oldStep.stepNumber}")
            val oldFocus = oldStep.focusDimension?.let {
                try { ReadinessDimension.valueOf(it) } catch (e: Exception) { null }
            }

            val proposedBuildChange = ProgressionStepChange(
                stepId = oldStep.id,
                stepNumber = oldStep.stepNumber,
                oldTitle = oldStep.title,
                newTitle = newTitle,
                oldDistanceMeters = oldStep.targetDistanceMeters,
                newDistanceMeters = candidateStep.targetDistanceMeters,
                oldElevationGainMeters = oldStep.targetElevationGainMeters,
                newElevationGainMeters = candidateStep.targetElevationGainMeters,
                oldDurationMinutes = oldStep.targetDurationMinutes,
                newDurationMinutes = candidateStep.targetDurationMinutes,
                oldFocusDimension = oldFocus,
                newFocusDimension = candidateStep.focusDimension
            )
            proposedChanges.add(proposedBuildChange)
            mappedProposedBuildSteps.add(proposedBuildChange)
        }

        // 4. Proposed Recovery Update (PART M)
        val pendingRecoverySteps = persistedSteps.filter { it.status == "PENDING" && it.type == "RECOVERY" }.sortedBy { it.stepNumber }
        if (pendingRecoverySteps.isNotEmpty() && mappedProposedBuildSteps.isNotEmpty()) {
            val finalProposedBuild = mappedProposedBuildSteps.last()

            val recoveryDistanceRaw = (finalProposedBuild.newDistanceMeters ?: 0.0) * 0.5
            val recoveryDurationRaw = (finalProposedBuild.newDurationMinutes ?: 0).toDouble() * 0.5
            val recoveryElevationRaw = if (target.hasElevationData) {
                (finalProposedBuild.newElevationGainMeters ?: 0.0) * 0.5
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

            val clampedRecDistance = minOf(roundedRecDistance, finalProposedBuild.newDistanceMeters ?: 0.0)
            val clampedRecDuration = minOf(roundedRecDuration, finalProposedBuild.newDurationMinutes ?: 15)
            val clampedRecElevation = if (target.hasElevationData && roundedRecElevation != null && finalProposedBuild.newElevationGainMeters != null) {
                minOf(roundedRecElevation, finalProposedBuild.newElevationGainMeters)
            } else {
                null
            }

            for (oldRecStep in pendingRecoverySteps) {
                val oldFocus = oldRecStep.focusDimension?.let {
                    try { ReadinessDimension.valueOf(it) } catch (e: Exception) { null }
                }

                proposedChanges.add(
                    ProgressionStepChange(
                        stepId = oldRecStep.id,
                        stepNumber = oldRecStep.stepNumber,
                        oldTitle = oldRecStep.title,
                        newTitle = "RECOVERY WEEK",
                        oldDistanceMeters = oldRecStep.targetDistanceMeters,
                        newDistanceMeters = clampedRecDistance,
                        oldElevationGainMeters = oldRecStep.targetElevationGainMeters,
                        newElevationGainMeters = clampedRecElevation,
                        oldDurationMinutes = oldRecStep.targetDurationMinutes,
                        newDurationMinutes = clampedRecDuration,
                        oldFocusDimension = oldFocus,
                        newFocusDimension = null
                    )
                )
            }
        }

        // 5. Material Difference Check (PART N)
        var hasMaterialDifference = false
        val changesToReturn = mutableListOf<ProgressionStepChange>()

        for (change in proposedChanges) {
            val oldStep = persistedSteps.find { it.id == change.stepId } ?: continue
            val isDiff = isMaterialDifference(oldStep, change)
            if (isDiff) {
                hasMaterialDifference = true
            }
            changesToReturn.add(change)
        }

        return if (hasMaterialDifference) {
            ProgressionAdaptationProposal(
                state = ProgressionAdaptationState.UPDATE_AVAILABLE,
                planId = activePlan.id,
                readinessAtPlanStart = startingReadinessScore,
                currentReadiness = currentScore,
                readinessDelta = readinessDelta,
                changes = changesToReturn
            )
        } else {
            noUpdateProposal
        }
    }

    private fun isMaterialDifference(old: ProgressionStepEntity, proposed: ProgressionStepChange): Boolean {
        val distDiff = when {
            old.targetDistanceMeters == null && proposed.newDistanceMeters == null -> false
            old.targetDistanceMeters == null || proposed.newDistanceMeters == null -> true
            else -> abs(old.targetDistanceMeters - proposed.newDistanceMeters) >= 1000.0
        }
        if (distDiff) return true

        val eleDiff = when {
            old.targetElevationGainMeters == null && proposed.newElevationGainMeters == null -> false
            old.targetElevationGainMeters == null || proposed.newElevationGainMeters == null -> true
            else -> abs(old.targetElevationGainMeters - proposed.newElevationGainMeters) >= 100.0
        }
        if (eleDiff) return true

        val durDiff = when {
            old.targetDurationMinutes == null && proposed.newDurationMinutes == null -> false
            old.targetDurationMinutes == null || proposed.newDurationMinutes == null -> true
            else -> abs(old.targetDurationMinutes - proposed.newDurationMinutes) >= 30
        }
        if (durDiff) return true

        val oldFocusStr = old.focusDimension
        val newFocusStr = proposed.newFocusDimension?.name
        if (oldFocusStr != newFocusStr) return true

        return false
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
