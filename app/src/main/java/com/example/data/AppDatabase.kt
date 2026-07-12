package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GearDao {
    @Query("SELECT * FROM gears ORDER BY isRetired ASC, name ASC")
    fun getAllGears(): Flow<List<Gear>>

    @Query("SELECT * FROM gears WHERE id = :id")
    suspend fun getGearById(id: Int): Gear?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGear(gear: Gear): Long

    @Update
    suspend fun updateGear(gear: Gear)

    @Delete
    suspend fun deleteGear(gear: Gear)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY timestamp DESC")
    fun getAllActivities(): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: Long): Activity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: Activity): Long

    @Update
    suspend fun updateActivity(activity: Activity)

    @Delete
    suspend fun deleteActivity(activity: Activity)
}

@Dao
interface SegmentDao {
    @Query("SELECT * FROM segments")
    fun getAllSegments(): Flow<List<Segment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<Segment>)

    @Query("SELECT * FROM segments WHERE id = :id")
    suspend fun getSegmentById(id: String): Segment?
}

@Dao
interface SegmentEffortDao {
    @Query("SELECT * FROM segment_efforts WHERE activityId = :activityId")
    fun getEffortsForActivity(activityId: Long): Flow<List<SegmentEffort>>

    @Query("SELECT * FROM segment_efforts WHERE segmentId = :segmentId ORDER BY elapsedTimeSeconds ASC")
    fun getEffortsForSegment(segmentId: String): Flow<List<SegmentEffort>>

    @Query("SELECT * FROM segment_efforts WHERE segmentId = :segmentId ORDER BY elapsedTimeSeconds ASC LIMIT 1")
    suspend fun getBestEffortForSegment(segmentId: String): SegmentEffort?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEffort(effort: SegmentEffort): Long
}

@Dao
interface FeedPostDao {
    @Query("SELECT * FROM feed_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<FeedPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: FeedPost): Long

    @Update
    suspend fun updatePost(post: FeedPost)

    @Delete
    suspend fun deletePost(post: FeedPost)
}

@Dao
interface FeedCommentDao {
    @Query("SELECT * FROM feed_comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: Long): Flow<List<FeedComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: FeedComment): Long

    @Delete
    suspend fun deleteComment(comment: FeedComment)
}

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY dateCreated DESC")
    fun getAllRoutes(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getRouteById(id: Long): Route?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: Route): Long

    @Update
    suspend fun updateRoute(route: Route)

    @Delete
    suspend fun deleteRoute(route: Route)
}

@Dao
interface TargetHikeDao {
    @Query("SELECT * FROM target_hikes WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActiveTarget(): Flow<TargetHike?>

    @Query("SELECT * FROM target_hikes WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveTarget(): TargetHike?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargetHike(targetHike: TargetHike): Long

    @Update
    suspend fun updateTargetHike(targetHike: TargetHike)

    @Query("UPDATE target_hikes SET status = :status WHERE id = :id")
    suspend fun updateTargetStatus(id: Long, status: String)

    @Query("SELECT * FROM target_hikes ORDER BY createdAt DESC")
    fun observeTargetHistory(): Flow<List<TargetHike>>

    @Query("SELECT * FROM target_hikes ORDER BY createdAt DESC")
    suspend fun getTargetHistory(): List<TargetHike>

    @Query("UPDATE target_hikes SET status = 'ARCHIVED' WHERE status = 'ACTIVE'")
    suspend fun archiveActiveTargets()

    @Query("UPDATE progression_plans SET status = 'ARCHIVED' WHERE status = 'ACTIVE'")
    suspend fun archiveAllActivePlans()

    @Transaction
    suspend fun setActiveTargetTransaction(target: TargetHike): Long {
        archiveActiveTargets()
        archiveAllActivePlans()
        val targetToInsert = target.copy(id = 0, status = "ACTIVE")
        return insertTargetHike(targetToInsert)
    }
}

@Dao
interface ProgressionDao {
    @Query("SELECT * FROM progression_plans WHERE targetHikeId = :targetHikeId AND status = 'ACTIVE' LIMIT 1")
    fun observeActivePlanForTarget(targetHikeId: Long): Flow<ProgressionPlanEntity?>

    @Query("SELECT * FROM progression_plans WHERE targetHikeId = :targetHikeId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActivePlanForTarget(targetHikeId: Long): ProgressionPlanEntity?

    @Query("SELECT * FROM progression_steps WHERE planId = :planId ORDER BY stepNumber ASC")
    fun observeStepsForPlan(planId: Long): Flow<List<ProgressionStepEntity>>

    @Query("SELECT * FROM progression_steps WHERE planId = :planId ORDER BY stepNumber ASC")
    suspend fun getStepsForPlan(planId: Long): List<ProgressionStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: ProgressionPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<ProgressionStepEntity>)

    @Update
    suspend fun updatePlan(plan: ProgressionPlanEntity)

    @Update
    suspend fun updateStep(step: ProgressionStepEntity)

    @Query("SELECT * FROM progression_steps WHERE planId = :planId AND status = 'CURRENT' LIMIT 1")
    suspend fun getCurrentStep(planId: Long): ProgressionStepEntity?

    @Query("UPDATE progression_plans SET status = 'ARCHIVED' WHERE targetHikeId = :targetHikeId AND status = 'ACTIVE'")
    suspend fun archiveActivePlanForTarget(targetHikeId: Long)

    @Query("SELECT * FROM progression_plans WHERE id = :id")
    suspend fun getPlanById(id: Long): ProgressionPlanEntity?

    @Query("SELECT * FROM progression_steps WHERE id = :id")
    suspend fun getStepById(id: Long): ProgressionStepEntity?

    @Query("UPDATE target_hikes SET status = 'COMPLETED' WHERE id = :targetHikeId")
    suspend fun completeTargetHike(targetHikeId: Long)

    @Transaction
    suspend fun createActivePlanWithSteps(
        plan: ProgressionPlanEntity,
        steps: List<ProgressionStepEntity>
    ) {
        archiveActivePlanForTarget(plan.targetHikeId)
        val planId = insertPlan(plan.copy(status = "ACTIVE"))
        val stepsWithPlanId = steps.mapIndexed { index, step ->
            step.copy(
                planId = planId,
                status = if (index == 0) "CURRENT" else "PENDING"
            )
        }
        insertSteps(stepsWithPlanId)
    }

    @Transaction
    suspend fun completeStepTransaction(
        planId: Long,
        stepId: Long,
        activityId: Long,
        completedAt: Long = System.currentTimeMillis()
    ) {
        val step = getStepById(stepId) ?: return
        if (step.planId != planId || step.status != "CURRENT") return

        val completedStep = step.copy(
            status = "COMPLETED",
            completedActivityId = activityId,
            completedAt = completedAt
        )
        updateStep(completedStep)

        val allSteps = getStepsForPlan(planId).sortedBy { it.stepNumber }
        val nextStep = allSteps.firstOrNull { it.stepNumber > step.stepNumber && it.status == "PENDING" }

        if (nextStep != null) {
            updateStep(nextStep.copy(status = "CURRENT"))
            val plan = getPlanById(planId)
            if (plan != null) {
                updatePlan(plan.copy(currentStepIndex = nextStep.stepNumber - 1))
            }
        } else {
            val plan = getPlanById(planId)
            if (plan != null) {
                updatePlan(plan.copy(
                    status = "COMPLETED",
                    currentStepIndex = allSteps.size
                ))
                if (step.isTargetHike) {
                    completeTargetHike(plan.targetHikeId)
                }
            }
        }
    }

    @Transaction
    suspend fun applyAdaptationTransaction(planId: Long, changes: List<ProgressionStepChange>) {
        val plan = getPlanById(planId) ?: return
        if (plan.status != "ACTIVE") return

        val persistedSteps = getStepsForPlan(planId)
        
        for (change in changes) {
            val step = persistedSteps.find { it.id == change.stepId } ?: continue
            if (step.planId != planId || step.status != "PENDING") continue
            if (step.type != "BUILD" && step.type != "RECOVERY") continue
            if (step.isTargetHike) continue

            val updatedStep = step.copy(
                title = change.newTitle,
                targetDistanceMeters = change.newDistanceMeters,
                targetElevationGainMeters = change.newElevationGainMeters,
                targetDurationMinutes = change.newDurationMinutes,
                focusDimension = change.newFocusDimension?.name
            )
            updateStep(updatedStep)
        }
    }
}

@Dao
interface ReadinessHistoryDao {
    @Query("SELECT * FROM readiness_history WHERE targetHikeId = :targetHikeId ORDER BY recordedAt ASC")
    fun observeReadinessHistoryForTarget(targetHikeId: Long): Flow<List<ReadinessHistoryEntity>>

    @Query("SELECT * FROM readiness_history WHERE targetHikeId = :targetHikeId ORDER BY recordedAt ASC")
    suspend fun getReadinessHistoryForTarget(targetHikeId: Long): List<ReadinessHistoryEntity>

    @Query("SELECT * FROM readiness_history WHERE targetHikeId = :targetHikeId ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestReadinessSnapshotForTarget(targetHikeId: Long): ReadinessHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadinessSnapshot(snapshot: ReadinessHistoryEntity): Long

    @Query("SELECT * FROM readiness_history WHERE targetHikeId = :targetHikeId AND activityId = :activityId LIMIT 1")
    suspend fun findSnapshotForActivity(targetHikeId: Long, activityId: Long): ReadinessHistoryEntity?
}

@Database(
    entities = [
        Gear::class,
        Activity::class,
        Segment::class,
        SegmentEffort::class,
        FeedPost::class,
        FeedComment::class,
        User::class,
        Route::class,
        WeatherCache::class,
        TargetHike::class,
        ProgressionPlanEntity::class,
        ProgressionStepEntity::class,
        ReadinessHistoryEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gearDao(): GearDao
    abstract fun activityDao(): ActivityDao
    abstract fun segmentDao(): SegmentDao
    abstract fun segmentEffortDao(): SegmentEffortDao
    abstract fun feedPostDao(): FeedPostDao
    abstract fun feedCommentDao(): FeedCommentDao
    abstract fun userDao(): UserDao
    abstract fun routeDao(): RouteDao
    abstract fun weatherDao(): WeatherDao
    abstract fun targetHikeDao(): TargetHikeDao
    abstract fun progressionDao(): ProgressionDao
    abstract fun readinessHistoryDao(): ReadinessHistoryDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `target_hikes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `distanceMeters` REAL NOT NULL, 
                        `elevationGainMeters` REAL NOT NULL, 
                        `estimatedDurationMinutes` INTEGER NOT NULL, 
                        `maxElevationMeters` REAL NOT NULL, 
                        `minElevationMeters` REAL NOT NULL, 
                        `gpxPath` TEXT, 
                        `createdAt` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `target_hikes_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `distanceMeters` REAL NOT NULL, 
                        `elevationGainMeters` REAL NOT NULL, 
                        `estimatedDurationMinutes` INTEGER NOT NULL, 
                        `maxElevationMeters` REAL, 
                        `minElevationMeters` REAL, 
                        `gpxPath` TEXT, 
                        `createdAt` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL,
                        `hasElevationData` INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `target_hikes_new` (
                        `id`, `name`, `distanceMeters`, `elevationGainMeters`, `estimatedDurationMinutes`, 
                        `maxElevationMeters`, `minElevationMeters`, `gpxPath`, `createdAt`, `status`, `hasElevationData`
                    )
                    SELECT 
                        `id`, `name`, `distanceMeters`, `elevationGainMeters`, `estimatedDurationMinutes`,
                        CASE WHEN `maxElevationMeters` = -9999.0 THEN NULL ELSE `maxElevationMeters` END,
                        CASE WHEN `minElevationMeters` = -9999.0 THEN NULL ELSE `minElevationMeters` END,
                        `gpxPath`, `createdAt`, `status`,
                        CASE WHEN `maxElevationMeters` = -9999.0 OR `minElevationMeters` = -9999.0 THEN 0 ELSE 1 END
                    FROM `target_hikes`
                """.trimIndent())

                db.execSQL("DROP TABLE `target_hikes`")
                db.execSQL("ALTER TABLE `target_hikes_new` RENAME TO `target_hikes`")
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `activities` ADD COLUMN `hasElevationData` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `progression_plans` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `targetHikeId` INTEGER NOT NULL, 
                        `startingReadinessScore` INTEGER NOT NULL, 
                        `mainLimiter` TEXT NOT NULL, 
                        `isLimitedHistory` INTEGER NOT NULL, 
                        `state` TEXT NOT NULL, 
                        `currentStepIndex` INTEGER NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `progression_steps` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `planId` INTEGER NOT NULL, 
                        `stepNumber` INTEGER NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `targetDistanceMeters` REAL, 
                        `targetElevationGainMeters` REAL, 
                        `targetDurationMinutes` INTEGER, 
                        `focusDimension` TEXT, 
                        `isTargetHike` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `completedActivityId` INTEGER, 
                        `completedAt` INTEGER, 
                        FOREIGN KEY(`planId`) REFERENCES `progression_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_progression_steps_planId` ON `progression_steps` (`planId`)
                """.trimIndent())
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `readiness_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `targetHikeId` INTEGER NOT NULL, 
                        `activityId` INTEGER, 
                        `progressionPlanId` INTEGER, 
                        `progressionStepId` INTEGER, 
                        `overallScore` INTEGER NOT NULL, 
                        `distanceScore` INTEGER NOT NULL, 
                        `elevationScore` INTEGER, 
                        `enduranceScore` INTEGER NOT NULL, 
                        `recentLoadScore` INTEGER NOT NULL, 
                        `mainLimiter` TEXT NOT NULL, 
                        `readinessLevel` TEXT NOT NULL, 
                        `recordedAt` INTEGER NOT NULL, 
                        `reason` TEXT NOT NULL, 
                        FOREIGN KEY(`targetHikeId`) REFERENCES `target_hikes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_readiness_history_targetHikeId` ON `readiness_history` (`targetHikeId`)
                """.trimIndent())

                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_readiness_history_targetHikeId_activityId` ON `readiness_history` (`targetHikeId`, `activityId`)
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "summit_database"
                )
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
