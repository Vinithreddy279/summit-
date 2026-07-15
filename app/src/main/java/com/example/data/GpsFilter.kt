package com.example.data

import android.location.Location

enum class GpsValidationResult {
    ACCEPTED,
    INVALID_COORDINATES,
    POOR_ACCURACY,
    INVALID_TIMESTAMP,
    IMPOSSIBLE_SPEED,
    STATIONARY_JITTER
}

object GpsFilter {
    // Constants for GPS filtering
    const val MAX_ACCURACY_METERS = 25.0 // Reject points with worse accuracy than this
    const val MIN_ACCURACY_METERS = 2.0  // Floor accuracy for threshold calculations
    const val ACCURACY_MULTIPLIER = 1.15 // Factor to multiply accuracy to get movement threshold
    const val MAX_SPEED_MPS = 30.0       // Suspicious jump threshold (approx 108 km/h)

    /**
     * Determines the impossible speed threshold based on the activity.
     * Generous enough to avoid rejecting downhill movement or elite athletes.
     */
    fun getImpossibleSpeedThreshold(sportType: String): Double {
        return when (sportType.lowercase()) {
            "walk" -> 6.0        // ~21.6 km/h
            "hike" -> 8.0        // ~28.8 km/h
            "trek" -> 8.0        // ~28.8 km/h
            "run" -> 12.0        // ~43.2 km/h
            "ride", "cycle" -> 40.0 // ~144.0 km/h
            else -> MAX_SPEED_MPS
        }
    }

    /**
     * Detailed validation returning specific enum reasons for diagnostics tracking.
     */
    fun validateLocation(
        newLocation: Location,
        lastAccepted: Location?,
        sportType: String = "run"
    ): GpsValidationResult {
        // 1. Evaluate Lat/Lng range
        if (newLocation.latitude < -90.0 || newLocation.latitude > 90.0 ||
            newLocation.longitude < -180.0 || newLocation.longitude > 180.0) {
            return GpsValidationResult.INVALID_COORDINATES
        }

        // 2. Evaluate Horizontal Accuracy existence and range
        if (!newLocation.hasAccuracy() || newLocation.accuracy <= 0f) {
            return GpsValidationResult.POOR_ACCURACY
        }
        if (newLocation.accuracy > MAX_ACCURACY_METERS) {
            return GpsValidationResult.POOR_ACCURACY
        }

        if (lastAccepted == null) {
            // First point is always accepted if it meets basic accuracy requirements
            return GpsValidationResult.ACCEPTED
        }

        // 3. Evaluate Displacement and Elapsed Time
        val displacement = newLocation.distanceTo(lastAccepted)
        val elapsedTimeSec = (newLocation.time - lastAccepted.time) / 1000.0

        if (elapsedTimeSec <= 0) {
            // Duplicate time stamps or reverse order points are rejected
            return GpsValidationResult.INVALID_TIMESTAMP
        }

        // 4. Evaluate Implied Speed & Suspicious Jumps
        val impliedSpeed = displacement / elapsedTimeSec
        val speedLimit = getImpossibleSpeedThreshold(sportType)
        if (impliedSpeed > speedLimit) {
            // Suspicious jump!
            return GpsValidationResult.IMPOSSIBLE_SPEED
        }

        // 5. Accuracy-Aware Movement Threshold
        // Floor the accuracy to MIN_ACCURACY_METERS to avoid sub-meter noise
        val effectiveAccuracy = maxOf(MIN_ACCURACY_METERS, newLocation.accuracy.toDouble())
        val movementThreshold = effectiveAccuracy * ACCURACY_MULTIPLIER

        // If displacement is less than the accuracy-aware threshold, it is treated as stationary drift
        if (displacement < movementThreshold) {
            return GpsValidationResult.STATIONARY_JITTER
        }

        return GpsValidationResult.ACCEPTED
    }

    /**
     * Backward-compatible simple boolean check with optional sportType.
     */
    fun shouldAcceptLocation(
        newLocation: Location,
        lastAccepted: Location?,
        sportType: String = "run"
    ): Boolean {
        return validateLocation(newLocation, lastAccepted, sportType) == GpsValidationResult.ACCEPTED
    }
}

