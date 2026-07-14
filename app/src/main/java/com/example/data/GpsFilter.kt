package com.example.data

import android.location.Location

object GpsFilter {
    // Constants for GPS filtering
    const val MAX_ACCURACY_METERS = 25.0 // Reject points with worse accuracy than this
    const val MIN_ACCURACY_METERS = 2.0  // Floor accuracy for threshold calculations
    const val ACCURACY_MULTIPLIER = 1.15 // Factor to multiply accuracy to get movement threshold
    const val MAX_SPEED_MPS = 30.0       // Suspicious jump threshold (approx 108 km/h)

    /**
     * Determines whether a new location update should be accepted.
     * Uses the last accepted location to evaluate horizontal accuracy, displacement,
     * elapsed time, implied speed, and suspicious jumps.
     */
    fun shouldAcceptLocation(
        newLocation: Location,
        lastAccepted: Location?
    ): Boolean {
        // 1. Evaluate Horizontal Accuracy
        if (newLocation.accuracy > MAX_ACCURACY_METERS) {
            return false
        }

        if (lastAccepted == null) {
            // First point is always accepted if it meets basic accuracy requirements
            return true
        }

        // 2. Evaluate Displacement and Elapsed Time
        val displacement = newLocation.distanceTo(lastAccepted)
        val elapsedTimeSec = (newLocation.time - lastAccepted.time) / 1000.0

        if (elapsedTimeSec <= 0) {
            // Duplicate time stamps or reverse order points are rejected
            return false
        }

        // 3. Evaluate Implied Speed & Suspicious Jumps
        val impliedSpeed = displacement / elapsedTimeSec
        if (impliedSpeed > MAX_SPEED_MPS) {
            // Suspicious jump!
            return false
        }

        // 4. Accuracy-Aware Movement Threshold
        // Floor the accuracy to MIN_ACCURACY_METERS to avoid sub-meter noise
        val effectiveAccuracy = maxOf(MIN_ACCURACY_METERS, newLocation.accuracy.toDouble())
        val movementThreshold = effectiveAccuracy * ACCURACY_MULTIPLIER

        // If displacement is less than the accuracy-aware threshold, it is treated as stationary drift
        if (displacement < movementThreshold) {
            return false
        }

        return true
    }
}
