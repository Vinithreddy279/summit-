package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SummitRepository(private val db: AppDatabase) {
    private val gearDao = db.gearDao()
    private val activityDao = db.activityDao()
    private val segmentDao = db.segmentDao()
    private val segmentEffortDao = db.segmentEffortDao()
    private val feedPostDao = db.feedPostDao()
    private val feedCommentDao = db.feedCommentDao()
    private val userDao = db.userDao()
    private val routeDao = db.routeDao()
    private val weatherDao = db.weatherDao()
    private val targetHikeDao = db.targetHikeDao()

    val gears: Flow<List<Gear>> = gearDao.getAllGears()
    val activities: Flow<List<Activity>> = activityDao.getAllActivities()
    val segments: Flow<List<Segment>> = segmentDao.getAllSegments()
    val feedPosts: Flow<List<FeedPost>> = feedPostDao.getAllPosts()
    val routes: Flow<List<Route>> = routeDao.getAllRoutes()

    fun getWeatherCacheFlow(): Flow<WeatherCache?> = weatherDao.getWeatherFlow()
    suspend fun getWeatherCacheDirect(): WeatherCache? = weatherDao.getWeatherDirect()
    suspend fun insertWeatherCache(weather: WeatherCache) = weatherDao.insertWeather(weather)
    suspend fun clearWeatherCache() = weatherDao.clearWeather()

    fun getComments(postId: Long): Flow<List<FeedComment>> = feedCommentDao.getCommentsForPost(postId)
    fun getEffortsForActivity(activityId: Long): Flow<List<SegmentEffort>> = segmentEffortDao.getEffortsForActivity(activityId)
    fun getEffortsForSegment(segmentId: String): Flow<List<SegmentEffort>> = segmentEffortDao.getEffortsForSegment(segmentId)

    // Route Actions
    suspend fun getRouteById(id: Long): Route? = routeDao.getRouteById(id)
    suspend fun insertRoute(route: Route): Long = routeDao.insertRoute(route)
    suspend fun updateRoute(route: Route) = routeDao.updateRoute(route)
    suspend fun deleteRoute(route: Route) = routeDao.deleteRoute(route)

    // Gear Actions
    suspend fun insertGear(gear: Gear): Long = gearDao.insertGear(gear)
    suspend fun updateGear(gear: Gear) = gearDao.updateGear(gear)
    suspend fun deleteGear(gear: Gear) = gearDao.deleteGear(gear)

    // Segment Actions
    suspend fun insertSegments(list: List<Segment>) = segmentDao.insertSegments(list)

    // Social Actions
    suspend fun insertFeedPost(post: FeedPost): Long = feedPostDao.insertPost(post)
    suspend fun updateFeedPost(post: FeedPost) = feedPostDao.updatePost(post)
    suspend fun deleteFeedPost(post: FeedPost) = feedPostDao.deletePost(post)

    suspend fun insertComment(comment: FeedComment): Long = feedCommentDao.insertComment(comment)
    suspend fun deleteComment(comment: FeedComment) = feedCommentDao.deleteComment(comment)

    // Big transactional activity saver: Match segments, update gear, create feed post!
    suspend fun saveRecordedActivity(activity: Activity, authorName: String = "You", authorAvatar: String = "avatar_you"): Long {
        // 1. Insert the activity
        val activityId = activityDao.insertActivity(activity)
        val savedActivity = activity.copy(id = activityId)

        // 2. Update gear mileage dynamically (Calculated on-the-fly via Flow combine in ViewModel)
        // activity.gearId?.let { gid ->
        //     val gear = gearDao.getGearById(gid)
        //     if (gear != null) {
        //         val updatedGear = gear.copy(
        //             currentMileageKm = gear.currentMileageKm + activity.distanceKm
        //         )
        //         gearDao.updateGear(updatedGear)
        //     }
        // }

        // 3. Match Segments
        val gpsPoints = JsonHelper.jsonToPoints(activity.routePointsJson)
        val allSegmentsList = segmentDao.getAllSegments().first()
        val matchedEfforts = SegmentMatcher.matchActivity(gpsPoints, allSegmentsList)

        for (effort in matchedEfforts) {
            // Find current personal best
            val bestEffort = segmentEffortDao.getBestEffortForSegment(effort.segmentId)
            val isPr = if (bestEffort == null) {
                true // first effort is always a PR
            } else {
                effort.elapsedTimeSeconds < bestEffort.elapsedTimeSeconds
            }

            // Save the effort with link to this activity
            segmentEffortDao.insertEffort(effort.copy(
                activityId = activityId,
                isPr = isPr
            ))
        }

        // 4. Create and insert a FeedPost for the activity
        val sportEmoji = when (activity.sportType.lowercase()) {
            "ride" -> "🚴"
            "run" -> "🏃"
            "hike" -> "🥾"
            "walk" -> "🚶"
            "swim" -> "🏊"
            else -> "💪"
        }

        val hasPrText = if (matchedEfforts.any { it.isPr }) " ✨ Personal Record!" else ""
        val postTitle = "$sportEmoji $authorName finished a ${activity.sportType.lowercase()}"
        val postContent = buildString {
            if (activity.notes.isNotEmpty()) {
                append(activity.notes)
                append("\n\n")
            }
            if (matchedEfforts.isNotEmpty()) {
                append("Matched ${matchedEfforts.size} segments:")
                matchedEfforts.forEach { eff ->
                    val mins = (eff.elapsedTimeSeconds / 60).toInt()
                    val secs = (eff.elapsedTimeSeconds % 60).toInt()
                    val prStar = if (eff.isPr) " 🥇 PR!" else ""
                    append("\n• ${eff.segmentName} in ${mins}m ${secs}s$prStar")
                }
            } else {
                append("Explored new paths and pushed limits!")
            }
        }

        val feedPost = FeedPost(
            userName = authorName,
            userAvatar = authorAvatar,
            activityId = activityId,
            title = postTitle,
            content = postContent,
            sportType = activity.sportType,
            distanceKm = activity.distanceKm,
            durationSeconds = activity.durationSeconds,
            elevationGainM = activity.elevationGainM,
            kudosCount = 0,
            commentsCount = 0,
            isKudosedByMe = false,
            timestamp = activity.timestamp,
            privacy = activity.privacy
        )
        feedPostDao.insertPost(feedPost)

        return activityId
    }

    // Toggle Kudos for a feed post
    suspend fun toggleKudos(postId: Long) {
        val posts = feedPostDao.getAllPosts().first()
        val post = posts.find { it.id == postId } ?: return
        val updated = post.copy(
            isKudosedByMe = !post.isKudosedByMe,
            kudosCount = if (post.isKudosedByMe) post.kudosCount - 1 else post.kudosCount + 1
        )
        feedPostDao.insertPost(updated)

        // Also check if it's linked to an activity to sync status
        post.activityId?.let { aid ->
            val act = activityDao.getActivityById(aid)
            if (act != null) {
                activityDao.insertActivity(act.copy(
                    isKudosedByMe = updated.isKudosedByMe,
                    kudosCount = updated.kudosCount
                ))
            }
        }
    }

    // User Operations
    suspend fun updateActivity(activity: Activity) = activityDao.updateActivity(activity)
    
    suspend fun deleteActivity(activity: Activity) {
        activityDao.deleteActivity(activity)
        try {
            val posts = feedPostDao.getAllPosts().first()
            val linkedPost = posts.find { it.activityId == activity.id }
            if (linkedPost != null) {
                feedPostDao.deletePost(linkedPost)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    fun observeUserByEmail(email: String): Flow<User?> = userDao.observeUserByEmail(email)
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    // Target Hike Actions
    fun observeActiveTarget(): Flow<TargetHike?> = targetHikeDao.observeActiveTarget()
    suspend fun getActiveTarget(): TargetHike? = targetHikeDao.getActiveTarget()
    suspend fun setActiveTarget(target: TargetHike): Long = targetHikeDao.setActiveTargetTransaction(target)
    suspend fun updateTarget(target: TargetHike) = targetHikeDao.updateTargetHike(target)
    suspend fun completeTarget(targetId: Long) = targetHikeDao.updateTargetStatus(targetId, "COMPLETED")
    suspend fun archiveTarget(targetId: Long) = targetHikeDao.updateTargetStatus(targetId, "ARCHIVED")
    fun observeTargetHistory(): Flow<List<TargetHike>> = targetHikeDao.observeTargetHistory()

    // Progression Actions
    private val progressionDao = db.progressionDao()

    fun observeActivePlanForTarget(targetHikeId: Long): Flow<ProgressionPlanEntity?> =
        progressionDao.observeActivePlanForTarget(targetHikeId)

    suspend fun getActivePlanForTarget(targetHikeId: Long): ProgressionPlanEntity? =
        progressionDao.getActivePlanForTarget(targetHikeId)

    fun observeStepsForPlan(planId: Long): Flow<List<ProgressionStepEntity>> =
        progressionDao.observeStepsForPlan(planId)

    suspend fun getStepsForPlan(planId: Long): List<ProgressionStepEntity> =
        progressionDao.getStepsForPlan(planId)

    suspend fun insertPlan(plan: ProgressionPlanEntity): Long =
        progressionDao.insertPlan(plan)

    suspend fun insertSteps(steps: List<ProgressionStepEntity>) =
        progressionDao.insertSteps(steps)

    suspend fun updatePlan(plan: ProgressionPlanEntity) =
        progressionDao.updatePlan(plan)

    suspend fun updateStep(step: ProgressionStepEntity) =
        progressionDao.updateStep(step)

    suspend fun getCurrentStep(planId: Long): ProgressionStepEntity? =
        progressionDao.getCurrentStep(planId)

    suspend fun createActivePlanWithSteps(plan: ProgressionPlanEntity, steps: List<ProgressionStepEntity>) =
        progressionDao.createActivePlanWithSteps(plan, steps)

    suspend fun completeStepTransaction(planId: Long, stepId: Long, activityId: Long, completedAt: Long = System.currentTimeMillis()) =
        progressionDao.completeStepTransaction(planId, stepId, activityId, completedAt)

    suspend fun archiveActivePlanForTarget(targetHikeId: Long) =
        progressionDao.archiveActivePlanForTarget(targetHikeId)

    // Readiness History Actions
    private val readinessHistoryDao = db.readinessHistoryDao()

    fun observeReadinessHistoryForTarget(targetHikeId: Long): Flow<List<ReadinessHistoryEntity>> =
        readinessHistoryDao.observeReadinessHistoryForTarget(targetHikeId)

    suspend fun getReadinessHistoryForTarget(targetHikeId: Long): List<ReadinessHistoryEntity> =
        readinessHistoryDao.getReadinessHistoryForTarget(targetHikeId)

    suspend fun getLatestReadinessSnapshotForTarget(targetHikeId: Long): ReadinessHistoryEntity? =
        readinessHistoryDao.getLatestReadinessSnapshotForTarget(targetHikeId)

    suspend fun insertReadinessSnapshot(snapshot: ReadinessHistoryEntity): Long =
        readinessHistoryDao.insertReadinessSnapshot(snapshot)

    suspend fun findSnapshotForActivity(targetHikeId: Long, activityId: Long): ReadinessHistoryEntity? =
        readinessHistoryDao.findSnapshotForActivity(targetHikeId, activityId)

    suspend fun findTargetCompletedSnapshot(targetHikeId: Long): ReadinessHistoryEntity? =
        readinessHistoryDao.findTargetCompletedSnapshot(targetHikeId)

    suspend fun ensureBaselineSnapshot(targetHikeId: Long, readiness: ReadinessResult, recordedAt: Long = System.currentTimeMillis()) {
        val existing = getReadinessHistoryForTarget(targetHikeId)
        if (existing.isEmpty()) {
            val baseline = ReadinessHistoryEntity(
                targetHikeId = targetHikeId,
                activityId = null,
                progressionPlanId = null,
                progressionStepId = null,
                overallScore = readiness.overallScore,
                distanceScore = readiness.distanceScore,
                elevationScore = readiness.elevationScore,
                enduranceScore = readiness.enduranceScore,
                recentLoadScore = readiness.recentLoadScore,
                mainLimiter = readiness.mainLimiter.name,
                readinessLevel = readiness.readinessLevel.name,
                recordedAt = recordedAt,
                reason = "BASELINE"
            )
            insertReadinessSnapshot(baseline)
        }
    }

    suspend fun applyAdaptationTransaction(planId: Long, changes: List<ProgressionStepChange>) =
        progressionDao.applyAdaptationTransaction(planId, changes)
}

