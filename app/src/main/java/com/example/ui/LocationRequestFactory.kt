package com.example.ui

import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority

object LocationRequestFactory {
    fun create(sport: String): LocationRequest {
        return when (sport.lowercase()) {
            "walk" -> LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L).apply {
                setMinUpdateIntervalMillis(2000L)
                setMinUpdateDistanceMeters(3.0f)
            }.build()

            "hike" -> LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).apply {
                setMinUpdateIntervalMillis(3000L)
                setMinUpdateDistanceMeters(3.0f)
            }.build()

            "trek" -> LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).apply {
                setMinUpdateIntervalMillis(3000L)
                setMinUpdateDistanceMeters(3.0f)
            }.build()

            "run" -> LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).apply {
                setMinUpdateIntervalMillis(2000L)
                setMinUpdateDistanceMeters(3.0f)
            }.build()

            "ride", "cycle" -> LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).apply {
                setMinUpdateIntervalMillis(2000L)
                setMinUpdateDistanceMeters(5.0f)
            }.build()

            else -> LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).apply {
                setMinUpdateIntervalMillis(2000L)
                setMinUpdateDistanceMeters(3.0f)
            }.build()
        }
    }
}
