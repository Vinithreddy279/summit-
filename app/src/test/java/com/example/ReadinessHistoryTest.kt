package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReadinessHistoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SummitRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SummitRepository(db)
    }

    @After
    fun closeDb() {
        db.close()
    }

    private suspend fun insertTarget(id: Long, name: String): Long {
        val target = TargetHike(
            id = id,
            name = name,
            distanceMeters = 10000.0,
            elevationGainMeters = 500.0,
            estimatedDurationMinutes = 180,
            maxElevationMeters = 1000.0,
            minElevationMeters = 500.0,
            gpxPath = null,
            createdAt = System.currentTimeMillis(),
            status = "ACTIVE"
        )
        return db.targetHikeDao().insertTargetHike(target)
    }

    @Test
    fun test_normal_completed_hike_creates_activity_impact_event() = runBlocking {
        val targetId = insertTarget(1L, "Yosemite Falls")

        val entity = ReadinessHistoryEntity(
            targetHikeId = targetId,
            activityId = 100L,
            progressionPlanId = null,
            progressionStepId = null,
            overallScore = 75,
            distanceScore = 80,
            elevationScore = 70,
            enduranceScore = 75,
            recentLoadScore = 70,
            mainLimiter = ReadinessDimension.ELEVATION.name,
            readinessLevel = ReadinessLevel.READY.name,
            recordedAt = System.currentTimeMillis(),
            reason = "ACTIVITY_IMPACT"
        )

        repository.insertReadinessSnapshot(entity)

        val history = repository.getReadinessHistoryForTarget(targetId)
        assertEquals(1, history.size)
        val snapshot = history[0]
        assertEquals("ACTIVITY_IMPACT", snapshot.reason)
        assertNull(snapshot.progressionPlanId)
        assertNull(snapshot.progressionStepId)
        assertEquals(100L, snapshot.activityId)
    }

    @Test
    fun test_progression_linked_hike_creates_activity_impact_event_with_plan_and_step_ids() = runBlocking {
        val targetId = insertTarget(1L, "Yosemite Falls")

        val entity = ReadinessHistoryEntity(
            targetHikeId = targetId,
            activityId = 100L,
            progressionPlanId = 5L,
            progressionStepId = 20L,
            overallScore = 75,
            distanceScore = 80,
            elevationScore = 70,
            enduranceScore = 75,
            recentLoadScore = 70,
            mainLimiter = ReadinessDimension.ELEVATION.name,
            readinessLevel = ReadinessLevel.READY.name,
            recordedAt = System.currentTimeMillis(),
            reason = "ACTIVITY_IMPACT"
        )

        repository.insertReadinessSnapshot(entity)

        val history = repository.getReadinessHistoryForTarget(targetId)
        assertEquals(1, history.size)
        val snapshot = history[0]
        assertEquals("ACTIVITY_IMPACT", snapshot.reason)
        assertEquals(5L, snapshot.progressionPlanId)
        assertEquals(20L, snapshot.progressionStepId)
        assertEquals(100L, snapshot.activityId)
    }

    @Test
    fun test_same_target_plus_same_activity_cannot_duplicate_activity_impact() = runBlocking {
        val targetId = insertTarget(1L, "Yosemite Falls")

        val entity1 = ReadinessHistoryEntity(
            targetHikeId = targetId,
            activityId = 100L,
            overallScore = 75,
            distanceScore = 80,
            elevationScore = 70,
            enduranceScore = 75,
            recentLoadScore = 70,
            mainLimiter = ReadinessDimension.ELEVATION.name,
            readinessLevel = ReadinessLevel.READY.name,
            reason = "ACTIVITY_IMPACT"
        )

        repository.insertReadinessSnapshot(entity1)

        val check = repository.findSnapshotForActivity(targetId, 100L)
        assertNotNull(check)

        val entity2 = entity1.copy(id = 0, overallScore = 80)
        repository.insertReadinessSnapshot(entity2)

        val history = repository.getReadinessHistoryForTarget(targetId)
        assertEquals(1, history.size)
    }

    @Test
    fun test_target_completed_cannot_duplicate_when_activity_id_is_null() = runBlocking {
        val targetId = insertTarget(1L, "Yosemite Falls")

        val completed1 = ReadinessHistoryEntity(
            targetHikeId = targetId,
            activityId = null,
            progressionPlanId = 1L,
            overallScore = 95,
            distanceScore = 95,
            elevationScore = 95,
            enduranceScore = 95,
            recentLoadScore = 95,
            mainLimiter = ReadinessDimension.DISTANCE.name,
            readinessLevel = ReadinessLevel.HIGHLY_READY.name,
            reason = "TARGET_COMPLETED"
        )

        val existing = repository.findTargetCompletedSnapshot(targetId)
        assertNull(existing)

        repository.insertReadinessSnapshot(completed1)

        val existingAfter = repository.findTargetCompletedSnapshot(targetId)
        assertNotNull(existingAfter)

        if (repository.findTargetCompletedSnapshot(targetId) == null) {
            repository.insertReadinessSnapshot(completed1.copy(id = 0))
        }

        val history = repository.getReadinessHistoryForTarget(targetId)
        assertEquals(1, history.size)
    }

    @Test
    fun test_deterministic_id_ordering_for_identical_recorded_at() = runBlocking {
        val targetId = insertTarget(1L, "Yosemite Falls")
        val now = 10000000L

        val row1 = ReadinessHistoryEntity(
            id = 10L,
            targetHikeId = targetId,
            overallScore = 50,
            distanceScore = 50,
            elevationScore = 50,
            enduranceScore = 50,
            recentLoadScore = 50,
            mainLimiter = "DISTANCE",
            readinessLevel = "BUILDING",
            recordedAt = now,
            reason = "ACTIVITY_IMPACT"
        )

        val row2 = ReadinessHistoryEntity(
            id = 5L,
            targetHikeId = targetId,
            overallScore = 60,
            distanceScore = 60,
            elevationScore = 60,
            enduranceScore = 60,
            recentLoadScore = 60,
            mainLimiter = "DISTANCE",
            readinessLevel = "BUILDING",
            recordedAt = now,
            reason = "ACTIVITY_IMPACT"
        )

        repository.insertReadinessSnapshot(row1)
        repository.insertReadinessSnapshot(row2)

        val historyAsc = repository.getReadinessHistoryForTarget(targetId)
        assertEquals(2, historyAsc.size)
        assertEquals(5L, historyAsc[0].id)
        assertEquals(10L, historyAsc[1].id)

        val latest = repository.getLatestReadinessSnapshotForTarget(targetId)
        assertNotNull(latest)
        assertEquals(10L, latest!!.id)
    }

    @Test
    fun test_readiness_level_snapshot_uses_actual_resolved_level() = runBlocking {
        val targetId = insertTarget(1L, "Yosemite Falls")

        val result = ReadinessResult(
            overallScore = 95,
            distanceScore = 95,
            elevationScore = 95,
            enduranceScore = 95,
            recentLoadScore = 95,
            mainLimiter = ReadinessDimension.DISTANCE,
            readinessLevel = ReadinessLevel.HIGHLY_READY,
            evidence = ReadinessEvidence(0.0, 0.0, 0.0, null, 0.0, 0.0, 0, 0.0, 0.0, 0, 0, 0, emptyList(), 0)
        )

        val snapshot = ReadinessHistoryEntity(
            targetHikeId = targetId,
            overallScore = result.overallScore,
            distanceScore = result.distanceScore,
            elevationScore = result.elevationScore,
            enduranceScore = result.enduranceScore,
            recentLoadScore = result.recentLoadScore,
            mainLimiter = result.mainLimiter.name,
            readinessLevel = result.readinessLevel.name,
            reason = "ACTIVITY_IMPACT"
        )

        repository.insertReadinessSnapshot(snapshot)

        val history = repository.getReadinessHistoryForTarget(targetId)
        assertEquals(ReadinessLevel.HIGHLY_READY.name, history[0].readinessLevel)
    }

    @Test
    fun test_baseline_remains_target_specific() = runBlocking {
        val targetA = insertTarget(1L, "Target A")
        val targetB = insertTarget(2L, "Target B")

        val readiness = ReadinessResult(
            overallScore = 60,
            distanceScore = 60,
            elevationScore = 60,
            enduranceScore = 60,
            recentLoadScore = 60,
            mainLimiter = ReadinessDimension.DISTANCE,
            readinessLevel = ReadinessLevel.MODERATE,
            evidence = ReadinessEvidence(0.0, 0.0, 0.0, null, 0.0, 0.0, 0, 0.0, 0.0, 0, 0, 0, emptyList(), 0)
        )

        repository.ensureBaselineSnapshot(targetA, readiness)
        repository.ensureBaselineSnapshot(targetB, readiness)

        val historyA = repository.getReadinessHistoryForTarget(targetA)
        val historyB = repository.getReadinessHistoryForTarget(targetB)

        assertEquals(1, historyA.size)
        assertEquals(1, historyB.size)
        assertEquals(targetA, historyA[0].targetHikeId)
        assertEquals(targetB, historyB[0].targetHikeId)
    }

    @Test
    fun test_history_for_target_a_does_not_appear_for_target_b() = runBlocking {
        val targetA = insertTarget(1L, "Target A")
        val targetB = insertTarget(2L, "Target B")

        val snapshot = ReadinessHistoryEntity(
            targetHikeId = targetA,
            overallScore = 50,
            distanceScore = 50,
            elevationScore = 50,
            enduranceScore = 50,
            recentLoadScore = 50,
            mainLimiter = "DISTANCE",
            readinessLevel = "BUILDING",
            reason = "ACTIVITY_IMPACT"
        )

        repository.insertReadinessSnapshot(snapshot)

        val historyA = repository.getReadinessHistoryForTarget(targetA)
        val historyB = repository.getReadinessHistoryForTarget(targetB)

        assertEquals(1, historyA.size)
        assertEquals(0, historyB.size)
    }
}
