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
        WeatherCache::class
    ],
    version = 7,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "summit_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
