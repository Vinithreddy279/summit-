package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "gears")
data class Gear(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val brand: String,
    val type: String, // "shoes" or "bike"
    val maxMileageKm: Double,
    val currentMileageKm: Double = 0.0,
    val isRetired: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val alertThresholdPercent: Int = 85,
    val notes: String = ""
) : Serializable

@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sportType: String, // "ride", "run", "hike", "walk", "swim"
    val durationSeconds: Long,
    val distanceKm: Double,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val elevationGainM: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val gearId: Int? = null,
    val routePointsJson: String, // List of GPS points: [{"lat":..., "lng":..., "ts":...}]
    val kudosCount: Int = 0,
    val isKudosedByMe: Boolean = false,
    val notes: String = "",
    val privacy: String = "Public", // "Public", "Friends Only", "Private"
    val isFavorite: Boolean = false
) : Serializable

@Entity(tableName = "segments")
data class Segment(
    @PrimaryKey val id: String,
    val name: String,
    val sportType: String,
    val polylinePointsJson: String, // List of [lat, lng]
    val lengthM: Double,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double
) : Serializable

@Entity(tableName = "segment_efforts")
data class SegmentEffort(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityId: Long,
    val segmentId: String,
    val segmentName: String,
    val startSeq: Int,
    val endSeq: Int,
    val startTime: Long,
    val elapsedTimeSeconds: Double,
    val avgSpeedMps: Double,
    val matchConfidence: Double,
    val isPr: Boolean = false,
    val flaggedAnomalous: Boolean = false,
    val flagReason: String? = null
) : Serializable

@Entity(tableName = "feed_posts")
data class FeedPost(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userName: String,
    val userAvatar: String, // e.g. "avatar_1", "avatar_2"
    val activityId: Long? = null, // linked activity
    val title: String,
    val content: String,
    val sportType: String? = null,
    val distanceKm: Double? = null,
    val durationSeconds: Long? = null,
    val elevationGainM: Double? = null,
    val kudosCount: Int = 0,
    val commentsCount: Int = 0,
    val isKudosedByMe: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null, // local drawable resource name or generic illustration
    val privacy: String = "Public" // "Public", "Friends Only", "Private"
) : Serializable

@Entity(tableName = "feed_comments")
data class FeedComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Long,
    val userName: String,
    val userAvatar: String,
    val commentText: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "routes")
data class Route(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val activityType: String, // "run", "ride", "hike", "walk"
    val routePointsJson: String, // List of GPS points: [{"lat":..., "lng":..., "ts":...}]
    val distanceKm: Double,
    val durationSeconds: Long,
    val elevationGainM: Double = 0.0,
    val difficulty: String = "Easy", // "Easy", "Moderate", "Hard", "Expert"
    val notes: String = "",
    val isFavorite: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis()
) : Serializable

