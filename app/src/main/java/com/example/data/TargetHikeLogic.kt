package com.example.data

import kotlin.math.roundToInt

enum class RouteDemandLevel {
    LOW, MODERATE, HIGH, VERY_HIGH, UNKNOWN
}

data class RouteDemand(
    val distanceDemand: RouteDemandLevel,
    val climbingDemand: RouteDemandLevel,
    val enduranceDemand: RouteDemandLevel
)

object TargetHikeLogic {
    fun classifyDemand(
        distanceMeters: Double,
        elevationGainMeters: Double,
        durationMinutes: Int,
        hasElevationData: Boolean
    ): RouteDemand {
        val distKm = distanceMeters / 1000.0
        
        val distLevel = when {
            distKm <= 5.0 -> RouteDemandLevel.LOW
            distKm <= 12.0 -> RouteDemandLevel.MODERATE
            distKm <= 20.0 -> RouteDemandLevel.HIGH
            else -> RouteDemandLevel.VERY_HIGH
        }
        
        val climbLevel = if (!hasElevationData) {
            RouteDemandLevel.UNKNOWN
        } else {
            when {
                elevationGainMeters <= 150.0 -> RouteDemandLevel.LOW
                elevationGainMeters <= 500.0 -> RouteDemandLevel.MODERATE
                elevationGainMeters <= 1000.0 -> RouteDemandLevel.HIGH
                else -> RouteDemandLevel.VERY_HIGH
            }
        }
        
        val enduranceLevel = when {
            durationMinutes <= 120 -> RouteDemandLevel.LOW
            durationMinutes <= 300 -> RouteDemandLevel.MODERATE
            durationMinutes <= 480 -> RouteDemandLevel.HIGH
            else -> RouteDemandLevel.VERY_HIGH
        }
        
        return RouteDemand(distLevel, climbLevel, enduranceLevel)
    }

    // A V1 estimated duration in minutes heuristic
    // Uses standard Naismith's rule: 4 km/h base speed for hiking (15 mins/km)
    // plus 10 mins per 100m elevation gain.
    fun estimateDurationMinutes(distanceMeters: Double, elevationGainMeters: Double): Int {
        val distKm = distanceMeters / 1000.0
        val baseTimeMins = (distKm / 4.0) * 60.0 // 4 km/h base speed for hiking
        val elevationMins = (elevationGainMeters / 100.0) * 10.0 // 10 mins per 100m elevation gain
        return (baseTimeMins + elevationMins).roundToInt().coerceAtLeast(1)
    }

    fun calculateElevationGain(points: List<GPSPoint>): Double {
        if (points.size < 2) return 0.0
        var gain = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (p1.hasElevation && p2.hasElevation) {
                val diff = p2.elevation - p1.elevation
                if (diff > 0.0) {
                    gain += diff
                }
            }
        }
        return gain
    }

    fun calculateMaxElevation(points: List<GPSPoint>): Double? {
        val validPoints = points.filter { it.hasElevation }
        if (validPoints.isEmpty()) return null
        return validPoints.maxOf { it.elevation }
    }

    fun calculateMinElevation(points: List<GPSPoint>): Double? {
        val validPoints = points.filter { it.hasElevation }
        if (validPoints.isEmpty()) return null
        return validPoints.minOf { it.elevation }
    }
}
