package com.example.data

import kotlin.math.roundToInt

object ReadinessEngine {
    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
    private const val NINETY_DAYS_MS = 90 * ONE_DAY_MS

    fun calculate(
        target: TargetHike,
        activities: List<Activity>,
        nowMillis: Long
    ): ReadinessResult {
        // Step 1: Filter and identify completed, valid hiking activities that are not in the future
        val allCompletedHikes = activities.filter {
            it.sportType == "hike" &&
                    it.timestamp <= nowMillis &&
                    it.distanceKm.isFinite() && it.distanceKm > 0.0 &&
                    it.durationSeconds > 0 &&
                    it.elevationGainM.isFinite() && it.elevationGainM >= 0.0
        }

        val historyActivityCount = allCompletedHikes.size

        // Step 2: Extract recent activities in the last 90 days for capacity calculations
        val recentHikes90Days = allCompletedHikes.filter {
            val age = nowMillis - it.timestamp
            age in 0 until NINETY_DAYS_MS
        }

        val recentActivityCount = recentHikes90Days.size

        // Step 3: Compute Capacities using TOP 3 valid recent activities (mean / descending)
        // Distance Capacity (convert km to meters)
        val sortedDistances = recentHikes90Days.map { it.distanceKm * 1000.0 }.sortedDescending()
        val top3Distances = sortedDistances.take(3)
        val recentDistanceCapacityMeters = if (top3Distances.isNotEmpty()) top3Distances.average() else 0.0
        val maxRecentDistanceMeters = if (sortedDistances.isNotEmpty()) sortedDistances.first() else 0.0

        // Elevation Capacity
        val recentElevationHikes = recentHikes90Days.filter {
            it.hasElevationData && it.elevationGainM.isFinite() && it.elevationGainM >= 0.0
        }
        val sortedElevations = recentElevationHikes.map { it.elevationGainM }.sortedDescending()
        val top3Elevations = sortedElevations.take(3)
        val recentElevationCapacityMeters = if (top3Elevations.isNotEmpty()) top3Elevations.average() else 0.0
        val maxRecentElevationGainMeters = if (sortedElevations.isNotEmpty()) sortedElevations.first() else 0.0
        val validRecentElevationActivityCount = recentElevationHikes.size

        // Endurance Capacity (duration seconds to minutes)
        val sortedDurations = recentHikes90Days.map { it.durationSeconds / 60.0 }.sortedDescending()
        val top3Durations = sortedDurations.take(3)
        val recentEnduranceCapacityMinutes = if (top3Durations.isNotEmpty()) top3Durations.average() else 0.0
        val maxRecentDurationMinutes = if (sortedDurations.isNotEmpty()) sortedDurations.first() else 0.0

        // Step 4: Compute Recent Load Score (Active weeks in the last 28 days)
        val activeWeeksList = MutableList(4) { false }
        for (activity in allCompletedHikes) {
            val ageMillis = nowMillis - activity.timestamp
            val ageDays = ageMillis.toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)
            if (ageDays >= 0.0 && ageDays < 28.0) {
                val bucket = (ageDays / 7.0).toInt()
                if (bucket in 0..3) {
                    activeWeeksList[bucket] = true
                }
            }
        }
        val recentActiveWeeks = activeWeeksList.count { it }
        val recentLoadScore = recentActiveWeeks * 25

        // Step 5: Calculate Individual Scores (Clamped 0 to 100)
        val distanceRatio = if (target.distanceMeters > 0.0) recentDistanceCapacityMeters / target.distanceMeters else 0.0
        val distanceScore = (distanceRatio * 100.0).roundToInt().coerceIn(0, 100)

        val elevationScore = if (!target.hasElevationData) {
            null
        } else if (target.elevationGainMeters == 0.0) {
            100
        } else {
            val elevationRatio = recentElevationCapacityMeters / target.elevationGainMeters
            (elevationRatio * 100.0).roundToInt().coerceIn(0, 100)
        }

        val enduranceRatio = if (target.estimatedDurationMinutes > 0) {
            recentEnduranceCapacityMinutes / target.estimatedDurationMinutes.toDouble()
        } else {
            0.0
        }
        val enduranceScore = (enduranceRatio * 100.0).roundToInt().coerceIn(0, 100)

        // Step 6: Calculate Overall Readiness Score (Weighted or Proportional Normalized)
        val overallScore = if (elevationScore != null) {
            val raw = distanceScore * 0.30 + elevationScore * 0.30 + enduranceScore * 0.25 + recentLoadScore * 0.15
            raw.roundToInt().coerceIn(0, 100)
        } else {
            val raw = (distanceScore * 0.30 + enduranceScore * 0.25 + recentLoadScore * 0.15) / 0.70
            raw.roundToInt().coerceIn(0, 100)
        }

        // Step 7: Map to Readiness Level
        val readinessLevel = getLevelForScore(overallScore)

        // Step 8: Main Limiter Selection with Tie-Break Priority
        val priorityList = listOfNotNull(
            if (elevationScore != null) ReadinessDimension.ELEVATION else null,
            ReadinessDimension.ENDURANCE,
            ReadinessDimension.DISTANCE,
            ReadinessDimension.RECENT_LOAD
        )
        val mainLimiter = priorityList.minByOrNull { dimension ->
            when (dimension) {
                ReadinessDimension.ELEVATION -> elevationScore!!
                ReadinessDimension.ENDURANCE -> enduranceScore
                ReadinessDimension.DISTANCE -> distanceScore
                ReadinessDimension.RECENT_LOAD -> recentLoadScore
            }
        } ?: ReadinessDimension.DISTANCE

        // Step 9: Assemble Evidence Data
        val evidence = ReadinessEvidence(
            targetDistanceMeters = target.distanceMeters,
            recentDistanceCapacityMeters = recentDistanceCapacityMeters,
            maxRecentDistanceMeters = maxRecentDistanceMeters,
            targetElevationGainMeters = if (target.hasElevationData) target.elevationGainMeters else null,
            recentElevationCapacityMeters = if (target.hasElevationData) recentElevationCapacityMeters else null,
            maxRecentElevationGainMeters = if (target.hasElevationData) maxRecentElevationGainMeters else null,
            targetDurationMinutes = target.estimatedDurationMinutes,
            recentEnduranceCapacityMinutes = recentEnduranceCapacityMinutes,
            maxRecentDurationMinutes = maxRecentDurationMinutes,
            recentActivityCount = recentActivityCount,
            recentActiveWeeks = recentActiveWeeks,
            historyActivityCount = historyActivityCount,
            activeWeeks = activeWeeksList,
            validRecentElevationActivityCount = validRecentElevationActivityCount
        )

        return ReadinessResult(
            overallScore = overallScore,
            distanceScore = distanceScore,
            elevationScore = elevationScore,
            enduranceScore = enduranceScore,
            recentLoadScore = recentLoadScore,
            mainLimiter = mainLimiter,
            readinessLevel = readinessLevel,
            evidence = evidence
        )
    }

    fun getLevelForScore(score: Int): ReadinessLevel {
        return when (score) {
            in 0..39 -> ReadinessLevel.NOT_READY
            in 40..59 -> ReadinessLevel.BUILDING
            in 60..74 -> ReadinessLevel.MODERATE
            in 75..89 -> ReadinessLevel.READY
            in 90..100 -> ReadinessLevel.HIGHLY_READY
            else -> ReadinessLevel.NOT_READY
        }
    }
}
