package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class SummitViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = SummitRepository(db)
    val sessionManager = SessionManager(application)

    // Offline Authentication & Settings flows
    val isLoggedIn: StateFlow<Boolean> = sessionManager.isLoggedInFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val loggedInUserEmail: StateFlow<String?> = sessionManager.userEmailFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val loggedInUser: StateFlow<User?> = loggedInUserEmail
        .flatMapLatest { email ->
            if (email != null) repository.observeUserByEmail(email) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val useImperial: StateFlow<Boolean> = sessionManager.useImperialFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoPauseSetting: StateFlow<Boolean> = sessionManager.autoPauseFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val gpsAccuracyMeters: StateFlow<Int> = sessionManager.gpsAccuracyThresholdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val appDarkMode: StateFlow<Boolean?> = sessionManager.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI States
    private val _currentTab = MutableStateFlow(Tab.DASHBOARD)
    val currentTab: StateFlow<Tab> = _currentTab.asStateFlow()

    enum class Tab {
        DASHBOARD, SOCIAL_FEED, RECORD, GEAR, SEGMENTS
    }

    enum class AppFlow {
        SPLASH, ONBOARDING, LOGIN, MAIN
    }

    private val _appFlow = MutableStateFlow(AppFlow.SPLASH)
    val appFlow: StateFlow<AppFlow> = _appFlow.asStateFlow()

    private val _selectedActivity = MutableStateFlow<Activity?>(null)
    val selectedActivity: StateFlow<Activity?> = _selectedActivity.asStateFlow()

    fun selectActivity(activity: Activity?) {
        _selectedActivity.value = activity
    }

    fun setAppFlow(flow: AppFlow) {
        _appFlow.value = flow
    }

    // Repository Flows
    val activities: StateFlow<List<Activity>> = repository.activities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gears: StateFlow<List<Gear>> = repository.gears
        .combine(repository.activities) { gearList, activityList ->
            gearList.map { gear ->
                val aggregatedDistance = activityList
                    .filter { it.gearId == gear.id }
                    .sumOf { it.distanceKm }
                gear.copy(currentMileageKm = gear.currentMileageKm + aggregatedDistance)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val segments: StateFlow<List<Segment>> = repository.segments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedPosts: StateFlow<List<FeedPost>> = repository.feedPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customizable Feed States
    private val _feedSportFilter = MutableStateFlow("all")
    val feedSportFilter: StateFlow<String> = _feedSportFilter.asStateFlow()

    private val _feedAuthorFilter = MutableStateFlow("all") // "all", "me", "friends"
    val feedAuthorFilter: StateFlow<String> = _feedAuthorFilter.asStateFlow()

    private val _feedCompactMode = MutableStateFlow(false)
    val feedCompactMode: StateFlow<Boolean> = _feedCompactMode.asStateFlow()

    private val _feedShowStats = MutableStateFlow(true)
    val feedShowStats: StateFlow<Boolean> = _feedShowStats.asStateFlow()

    // Theme Mode Selection: "system", "dark", "light"
    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Comments dialog state
    private val _activeCommentsPostId = MutableStateFlow<Long?>(null)
    val activeCommentsPostId: StateFlow<Long?> = _activeCommentsPostId.asStateFlow()

    val activeComments: StateFlow<List<FeedComment>> = _activeCommentsPostId
        .flatMapLatest { pid ->
            if (pid != null) repository.getComments(pid) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _newCommentText = MutableStateFlow("")
    val newCommentText: StateFlow<String> = _newCommentText.asStateFlow()

    // Active Recording States
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0L)
    val recordingDurationSeconds: StateFlow<Long> = _recordingDurationSeconds.asStateFlow()

    private val _recordingDistanceKm = MutableStateFlow(0.0)
    val recordingDistanceKm: StateFlow<Double> = _recordingDistanceKm.asStateFlow()

    private val _recordingSportType = MutableStateFlow("") // "run", "ride", "hike", "walk", "swim"
    val recordingSportType: StateFlow<String> = _recordingSportType.asStateFlow()

    private val _recordingGearId = MutableStateFlow<Int?>(null)
    val recordingGearId: StateFlow<Int?> = _recordingGearId.asStateFlow()

    private val _recordingTrackpoints = MutableStateFlow<List<GPSPoint>>(emptyList())
    val recordingTrackpoints: StateFlow<List<GPSPoint>> = _recordingTrackpoints.asStateFlow()

    private val _selectedSimulationRoute = MutableStateFlow<String?>("None")
    val selectedSimulationRoute: StateFlow<String?> = _selectedSimulationRoute.asStateFlow()

    // In-App Toast Alert state
    data class InAppAlert(val title: String, val message: String, val icon: String = "🔔")
    private val _inAppAlert = MutableStateFlow<InAppAlert?>(null)
    val inAppAlert: StateFlow<InAppAlert?> = _inAppAlert.asStateFlow()

    fun triggerNotification(title: String, message: String, icon: String = "🔔") {
        try {
            NotificationHelper.showNotification(getApplication(), title, message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        viewModelScope.launch {
            _inAppAlert.value = InAppAlert(title, message, icon)
            delay(3500)
            if (_inAppAlert.value?.title == title && _inAppAlert.value?.message == message) {
                _inAppAlert.value = null
            }
        }
    }

    fun dismissInAppAlert() {
        _inAppAlert.value = null
    }

    // Temporary variables for recording simulation
    private var recordingJob: Job? = null
    private var simulationIndex = 0
    private var simulatedCoordinates = emptyList<Pair<Double, Double>>()

    init {
        // Restore session status & settings
        viewModelScope.launch {
            sessionManager.isLoggedInFlow.collect { loggedIn ->
                if (loggedIn) {
                    _appFlow.value = AppFlow.MAIN
                }
            }
        }
        viewModelScope.launch {
            sessionManager.darkModeFlow.collect { dark ->
                if (dark != null) {
                    _themeMode.value = if (dark) "dark" else "light"
                }
            }
        }

        // Seed initial data if segments are empty
        viewModelScope.launch {
            val existingSegments = repository.segments.first()
            if (existingSegments.isEmpty()) {
                seedDatabase()
            }
        }

        // Attempt to restore active workout state from SharedPreferences on VM startup
        viewModelScope.launch(Dispatchers.Main) {
            val restored = TrackingService.restoreActiveStateFromPrefs(getApplication())
            if (restored) {
                if (TrackingService.isRecording.value && !TrackingService.isPaused.value) {
                    val intent = Intent(getApplication(), TrackingService::class.java).apply {
                        action = TrackingService.ACTION_START
                        putExtra("IS_RESTORE", true)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        getApplication<Application>().startForegroundService(intent)
                    } else {
                        getApplication<Application>().startService(intent)
                    }
                }
            }
        }

        // Sync with TrackingService companion flows
        viewModelScope.launch {
            TrackingService.isRecording.collect {
                _isRecording.value = it
            }
        }
        viewModelScope.launch {
            TrackingService.durationSeconds.collect {
                _recordingDurationSeconds.value = it
            }
        }
        viewModelScope.launch {
            TrackingService.distanceKm.collect {
                _recordingDistanceKm.value = it
            }
        }
        viewModelScope.launch {
            TrackingService.trackpoints.collect {
                _recordingTrackpoints.value = it
            }
        }
        viewModelScope.launch {
            TrackingService.selectedSimulationRoute.collect {
                _selectedSimulationRoute.value = it
            }
        }
    }

    private suspend fun seedDatabase() {
        // 1. Seed Predefined Segments
        val seedSegs = listOf(
            Segment(
                id = "seg-gg-bridge",
                name = "Golden Gate Bridge Crossing",
                sportType = "run",
                polylinePointsJson = JsonHelper.polylineToJson(listOf(
                    Pair(37.8110, -122.4770),
                    Pair(37.8130, -122.4773),
                    Pair(37.8160, -122.4778),
                    Pair(37.8190, -122.4782),
                    Pair(37.8220, -122.4786)
                )),
                lengthM = 1250.0,
                startLat = 37.8110,
                startLng = -122.4770,
                endLat = 37.8220,
                endLng = -122.4786
            ),
            Segment(
                id = "seg-twin-peaks",
                name = "Twin Peaks Hill Climb",
                sportType = "run",
                polylinePointsJson = JsonHelper.polylineToJson(listOf(
                    Pair(37.7510, -122.4430),
                    Pair(37.7525, -122.4445),
                    Pair(37.7538, -122.4458),
                    Pair(37.7545, -122.4465)
                )),
                lengthM = 620.0,
                startLat = 37.7510,
                startLng = -122.4430,
                endLat = 37.7545,
                endLng = -122.4465
            ),
            Segment(
                id = "seg-presidio-sprint",
                name = "Presidio Loop Cycle",
                sportType = "ride",
                polylinePointsJson = JsonHelper.polylineToJson(listOf(
                    Pair(37.7980, -122.4660),
                    Pair(37.8005, -122.4672),
                    Pair(37.8030, -122.4685),
                    Pair(37.8055, -122.4695)
                )),
                lengthM = 1000.0,
                startLat = 37.7980,
                startLng = -122.4660,
                endLat = 37.8055,
                endLng = -122.4695
            ),
            Segment(
                id = "seg-hawk-hill",
                name = "Hawk Hill Peak Climb",
                sportType = "ride",
                polylinePointsJson = JsonHelper.polylineToJson(listOf(
                    Pair(37.8280, -122.4820),
                    Pair(37.8300, -122.4860),
                    Pair(37.8320, -122.4920),
                    Pair(37.8335, -122.4990)
                )),
                lengthM = 2100.0,
                startLat = 37.8280,
                startLng = -122.4820,
                endLat = 37.8335,
                endLng = -122.4990
            )
        )
        repository.insertSegments(seedSegs)

        // 2. Seed Predefined Gears
        val gearShoesId = repository.insertGear(
            Gear(
                name = "Pegasus 40",
                brand = "Nike",
                type = "shoes",
                maxMileageKm = 800.0,
                currentMileageKm = 411.6
            )
        ).toInt()

        val gearBikeId = repository.insertGear(
            Gear(
                name = "Tarmac SL7",
                brand = "Specialized",
                type = "bike",
                maxMileageKm = 5000.0,
                currentMileageKm = 1227.5
            )
        ).toInt()

        // Insert a past running activity for Pegasus 40
        val runPoints = listOf(
            GPSPoint(37.7510, -122.4430, 100.0, System.currentTimeMillis() - 86400000, 3.5),
            GPSPoint(37.7525, -122.4445, 150.0, System.currentTimeMillis() - 86350000, 3.5),
            GPSPoint(37.7545, -122.4465, 200.0, System.currentTimeMillis() - 86300000, 3.5)
        )
        repository.saveRecordedActivity(
            Activity(
                title = "Twin Peaks Morning Run",
                sportType = "run",
                durationSeconds = 1200,
                distanceKm = 8.4,
                avgSpeedKmh = 25.2,
                maxSpeedKmh = 30.0,
                elevationGainM = 100.0,
                timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
                gearId = gearShoesId,
                routePointsJson = JsonHelper.pointsToJson(runPoints),
                notes = "Felt great in the Pegasus 40s! Beautiful sunrise over SF."
            )
        )

        // Insert a past ride activity for Tarmac SL7
        val ridePoints = listOf(
            GPSPoint(37.8280, -122.4820, 20.0, System.currentTimeMillis() - 172800000, 8.5),
            GPSPoint(37.8300, -122.4860, 80.0, System.currentTimeMillis() - 172700000, 8.5),
            GPSPoint(37.8335, -122.4990, 180.0, System.currentTimeMillis() - 172600000, 8.5)
        )
        repository.saveRecordedActivity(
            Activity(
                title = "Hawk Hill Power Hour",
                sportType = "ride",
                durationSeconds = 2400,
                distanceKm = 22.5,
                avgSpeedKmh = 33.75,
                maxSpeedKmh = 45.0,
                elevationGainM = 160.0,
                timestamp = System.currentTimeMillis() - 172800000, // 2 days ago
                gearId = gearBikeId,
                routePointsJson = JsonHelper.pointsToJson(ridePoints),
                notes = "Windy at the peak, specialized tarmac rolled super smoothly."
            )
        )

        // 3. Seed Initial Social Feed Posts
        val post1Id = repository.insertFeedPost(
            FeedPost(
                userName = "Sarah Chen",
                userAvatar = "avatar_1",
                title = "🏃 Sarah Chen finished a run",
                content = "Sunny afternoon jog on the Golden Gate Bridge! Wind was quite strong, but managed a decent pace. Loving the weather! ☀️",
                sportType = "run",
                distanceKm = 6.4,
                durationSeconds = 1820,
                elevationGainM = 45.0,
                kudosCount = 12,
                commentsCount = 2,
                isKudosedByMe = false,
                timestamp = System.currentTimeMillis() - 3600000 // 1 hour ago
            )
        )

        repository.insertComment(
            FeedComment(
                postId = post1Id,
                userName = "You",
                userAvatar = "avatar_you",
                commentText = "Awesome run Sarah! Pace looks super steady.",
                timestamp = System.currentTimeMillis() - 1800000
            )
        )
        repository.insertComment(
            FeedComment(
                postId = post1Id,
                userName = "Alex Mercer",
                userAvatar = "avatar_2",
                commentText = "Great effort! Bridge runs are always windy but worth it.",
                timestamp = System.currentTimeMillis() - 1200000
            )
        )

        repository.insertFeedPost(
            FeedPost(
                userName = "Alex Mercer",
                userAvatar = "avatar_2",
                title = "🚴 Alex Mercer finished a ride",
                content = "Hawk hill repeats before breakfast. Foggy at the base, but clear blue skies at the summit! 🌫️➡️☀️ #climbing",
                sportType = "ride",
                distanceKm = 34.2,
                durationSeconds = 4850,
                elevationGainM = 620.0,
                kudosCount = 24,
                commentsCount = 0,
                isKudosedByMe = true,
                timestamp = System.currentTimeMillis() - 14400000 // 4 hours ago
            )
        )
    }

    // Tab Navigation
    fun setTab(tab: Tab) {
        _currentTab.value = tab
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    // Feed customizers
    fun setFeedSportFilter(filter: String) {
        _feedSportFilter.value = filter
    }

    fun setFeedAuthorFilter(filter: String) {
        _feedAuthorFilter.value = filter
    }

    fun toggleFeedCompactMode() {
        _feedCompactMode.value = !_feedCompactMode.value
    }

    fun toggleFeedShowStats() {
        _feedShowStats.value = !_feedShowStats.value
    }

    // Toggle post kudos
    fun toggleKudos(postId: Long) {
        viewModelScope.launch {
            repository.toggleKudos(postId)
            val posts = feedPosts.value
            val post = posts.find { it.id == postId }
            if (post != null) {
                if (post.userName == "You") {
                    triggerNotification("Kudos Received! 👍", "An athlete gave kudos to your post: '${post.title}'")
                } else {
                    triggerNotification("Kudos Given! 👍", "You gave kudos on ${post.userName}'s post.")
                }
            }
        }
    }

    // Feed Comments management
    fun openComments(postId: Long) {
        _activeCommentsPostId.value = postId
        _newCommentText.value = ""
    }

    fun closeComments() {
        _activeCommentsPostId.value = null
    }

    fun setNewCommentText(text: String) {
        _newCommentText.value = text
    }

    fun postComment() {
        val postId = _activeCommentsPostId.value ?: return
        val text = _newCommentText.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            repository.insertComment(
                FeedComment(
                    postId = postId,
                    userName = "You",
                    userAvatar = "avatar_you",
                    commentText = text
                )
            )

            // Update comments count on feed post
            val posts = feedPosts.value
            val post = posts.find { it.id == postId }
            if (post != null) {
                repository.insertFeedPost(
                    post.copy(commentsCount = post.commentsCount + 1)
                )
                if (post.userName == "You") {
                    triggerNotification("Comment Posted 💬", "You commented on your own post: '$text'")
                } else {
                    triggerNotification("Comment Sent 💬", "You commented on ${post.userName}'s post.")
                }
            }

            _newCommentText.value = ""
        }
    }

    fun insertCustomPost(title: String, content: String) {
        viewModelScope.launch {
            val postId = repository.insertFeedPost(
                FeedPost(
                    userName = "You",
                    userAvatar = "avatar_you",
                    title = "📣 $title",
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
            )
            triggerNotification("Post Published 📣", "Your post '$title' is now live on the feed!")
            simulateSocialInteractionsForPost(postId, title)
        }
    }

    private fun simulateSocialInteractionsForPost(postId: Long, postTitle: String) {
        viewModelScope.launch {
            // After 5s, Sarah Chen gives a Kudos
            delay(5000)
            val posts = feedPosts.value
            val userPost = posts.find { it.id == postId && it.userName == "You" }
            if (userPost != null) {
                repository.insertFeedPost(
                    userPost.copy(kudosCount = userPost.kudosCount + 1)
                )
                triggerNotification(
                    "Kudos Received! 👍",
                    "Sarah Chen gave you kudos on your post: '$postTitle'"
                )
            }

            // After another 5s, Alex Mercer leaves a comment
            delay(5000)
            val posts2 = feedPosts.value
            val userPost2 = posts2.find { it.id == postId && it.userName == "You" }
            if (userPost2 != null) {
                repository.insertComment(
                    FeedComment(
                        postId = postId,
                        userName = "Alex Mercer",
                        userAvatar = "avatar_2",
                        commentText = "Outstanding! Keep up the brilliant updates! 🚀"
                    )
                )
                repository.insertFeedPost(
                    userPost2.copy(commentsCount = userPost2.commentsCount + 1)
                )
                triggerNotification(
                    "New Comment! 💬",
                    "Alex Mercer commented on your post: 'Outstanding! Keep up...'"
                )
            }
        }
    }

    fun simulateSocialInteractionsForActivity(activityId: Long, activityTitle: String) {
        viewModelScope.launch {
            // After 5s, Sarah Chen gives a Kudos
            delay(5000)
            val posts = feedPosts.value
            val userPost = posts.find { it.activityId == activityId && it.userName == "You" }
            if (userPost != null) {
                repository.insertFeedPost(
                    userPost.copy(kudosCount = userPost.kudosCount + 1)
                )
                triggerNotification(
                    "Kudos Received! 👍",
                    "Sarah Chen gave you kudos on your activity: '$activityTitle'"
                )
            }

            // After another 5s, Markus Vance leaves a comment
            delay(5000)
            val posts2 = feedPosts.value
            val userPost2 = posts2.find { it.activityId == activityId && it.userName == "You" }
            if (userPost2 != null) {
                repository.insertComment(
                    FeedComment(
                        postId = userPost2.id,
                        userName = "Markus Vance",
                        userAvatar = "avatar_3",
                        commentText = "Sensational pace on this run! Absolute inspiration. 🔥"
                    )
                )
                repository.insertFeedPost(
                    userPost2.copy(commentsCount = userPost2.commentsCount + 1)
                )
                triggerNotification(
                    "New Comment! 💬",
                    "Markus Vance commented on your activity: 'Sensational pace on this...'"
                )
            }
        }
    }

    // Gear Actions
    fun addGear(name: String, brand: String, type: String, limitKm: Double, alertThresholdPercent: Int = 85, notes: String = "") {
        if (name.trim().isEmpty() || brand.trim().isEmpty() || limitKm <= 0) return
        viewModelScope.launch {
            repository.insertGear(
                Gear(
                    name = name.trim(),
                    brand = brand.trim(),
                    type = type,
                    maxMileageKm = limitKm,
                    alertThresholdPercent = alertThresholdPercent,
                    notes = notes.trim()
                )
            )
        }
    }

    fun deleteGear(gear: Gear) {
        viewModelScope.launch {
            repository.deleteGear(gear)
        }
    }

    fun retireGear(gear: Gear) {
        viewModelScope.launch {
            repository.updateGear(gear.copy(isRetired = !gear.isRetired))
        }
    }

    // Recording Actions
    fun setRecordingSportType(sport: String) {
        _recordingSportType.value = sport
    }

    fun setRecordingGear(gearId: Int?) {
        _recordingGearId.value = gearId
    }

    fun setSimulationRoute(route: String?) {
        _selectedSimulationRoute.value = route
    }

    fun startRecording() {
        TrackingService.startService(
            getApplication(),
            _recordingSportType.value,
            _selectedSimulationRoute.value,
            TrackingService.autoPauseSetting.value
        )
    }

    fun stopRecording() {
        TrackingService.pauseTracking(getApplication())
    }

    fun resumeRecording() {
        TrackingService.resumeTracking(getApplication())
    }

    fun finishRecording(notes: String = "", privacy: String = "Public") {
        TrackingService.stopService(getApplication())
        val points = _recordingTrackpoints.value
        if (points.size < 2) return

        val distance = _recordingDistanceKm.value
        val duration = _recordingDurationSeconds.value
        if (duration <= 0) return

        val avgSpeed = (distance / (duration / 3600.0))
        val maxSpeed = avgSpeed * 1.3 // estimated max speed

        // Calculate elevation gain
        var eleGain = 0.0
        for (i in 1 until points.size) {
            val diff = points[i].elevation - points[i - 1].elevation
            if (diff > 0) eleGain += diff
        }

        val activity = Activity(
            title = when (_selectedSimulationRoute.value) {
                "Twin Peaks Hill Climb" -> "Twin Peaks Climb " + _recordingSportType.value.replaceFirstChar { it.uppercase() }
                "Golden Gate Bridge Crossing" -> "Golden Gate Bridge Bridge crossing"
                "Presidio Loop Cycle" -> "Presidio Loop Cycle Ride"
                "Hawk Hill Peak Climb" -> "Hawk Hill Climb Ride"
                else -> "Workout " + _recordingSportType.value.replaceFirstChar { it.uppercase() }
            },
            sportType = _recordingSportType.value,
            durationSeconds = duration,
            distanceKm = distance,
            avgSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeed,
            elevationGainM = eleGain,
            gearId = _recordingGearId.value,
            routePointsJson = JsonHelper.pointsToJson(points),
            notes = notes,
            privacy = privacy
        )

        viewModelScope.launch {
            val activityId = repository.saveRecordedActivity(activity)
            triggerNotification("Workout Saved 🎉", "Successfully saved '${activity.title}'! It's posted to the community.")
            _recordingDurationSeconds.value = 0L
            _recordingDistanceKm.value = 0.0
            _recordingTrackpoints.value = emptyList()
            _currentTab.value = Tab.SOCIAL_FEED // jump to the feed to see your workout card!
            simulateSocialInteractionsForActivity(activityId, activity.title)
        }
    }

    fun discardRecording() {
        TrackingService.stopService(getApplication())
        _recordingDurationSeconds.value = 0L
        _recordingDistanceKm.value = 0.0
        _recordingTrackpoints.value = emptyList()
    }

    // Settings modifiers
    fun setUseImperial(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setUseImperial(enabled)
        }
    }

    fun setAutoPause(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setAutoPause(enabled)
        }
    }

    fun setGpsAccuracyThreshold(meters: Int) {
        viewModelScope.launch {
            sessionManager.setGpsAccuracyThreshold(meters)
        }
    }

    fun setDarkModeSetting(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setDarkMode(enabled)
            _themeMode.value = if (enabled) "dark" else "light"
        }
    }

    // Authentication Functions
    fun signUp(
        email: String,
        name: String,
        password: String,
        rememberMe: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val normalizedEmail = email.trim().lowercase()
            if (normalizedEmail.isEmpty() || name.trim().isEmpty() || password.isEmpty()) {
                onError("Please fill in all fields.")
                return@launch
            }
            val existingUser = repository.getUserByEmail(normalizedEmail)
            if (existingUser != null) {
                onError("Email already registered.")
                return@launch
            }
            val hash = HashUtils.hashPassword(password)
            val newUser = User(
                email = normalizedEmail,
                name = name.trim(),
                passwordHash = hash
            )
            repository.insertUser(newUser)
            sessionManager.saveSession(normalizedEmail, rememberMe)
            triggerNotification("Welcome, ${newUser.name}! 🎉", "Your account has been created successfully.")
            _appFlow.value = AppFlow.MAIN
            onSuccess()
        }
    }

    fun login(
        email: String,
        password: String,
        rememberMe: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val normalizedEmail = email.trim().lowercase()
            if (normalizedEmail.isEmpty() || password.isEmpty()) {
                onError("Please enter your email and password.")
                return@launch
            }
            val user = repository.getUserByEmail(normalizedEmail)
            if (user == null) {
                onError("User not found.")
                return@launch
            }
            val valid = HashUtils.checkPassword(password, user.passwordHash)
            if (!valid) {
                onError("Incorrect password.")
                return@launch
            }
            sessionManager.saveSession(normalizedEmail, rememberMe)
            triggerNotification("Welcome back! 👋", "Logged in as ${user.name}.")
            _appFlow.value = AppFlow.MAIN
            onSuccess()
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _appFlow.value = AppFlow.LOGIN
            triggerNotification("Logged Out 🔑", "You have been securely logged out.")
        }
    }

    fun changePassword(
        oldPass: String,
        newPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val email = loggedInUserEmail.value
            if (email == null) {
                onError("No active session.")
                return@launch
            }
            val user = repository.getUserByEmail(email)
            if (user == null) {
                onError("User not found.")
                return@launch
            }
            if (!HashUtils.checkPassword(oldPass, user.passwordHash)) {
                onError("Current password is incorrect.")
                return@launch
            }
            if (newPass.length < 4) {
                onError("New password must be at least 4 characters.")
                return@launch
            }
            val hashed = HashUtils.hashPassword(newPass)
            val updated = user.copy(passwordHash = hashed)
            repository.updateUser(updated)
            triggerNotification("Password Updated 🔒", "Your password was changed successfully.")
            onSuccess()
        }
    }

    fun editProfile(
        name: String,
        avatar: String,
        heightCm: Double,
        weightKg: Double,
        birthday: String,
        gender: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val email = loggedInUserEmail.value
            if (email == null) {
                onError("No active session.")
                return@launch
            }
            val user = repository.getUserByEmail(email)
            if (user == null) {
                onError("User not found.")
                return@launch
            }
            val updated = user.copy(
                name = name.trim(),
                avatar = avatar,
                heightCm = heightCm,
                weightKg = weightKg,
                birthday = birthday,
                gender = gender
            )
            repository.updateUser(updated)
            triggerNotification("Profile Updated 👤", "Your changes have been saved.")
            onSuccess()
        }
    }

    fun deleteAccount(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val email = loggedInUserEmail.value
            if (email == null) {
                onError("No active session.")
                return@launch
            }
            val user = repository.getUserByEmail(email)
            if (user == null) {
                onError("User not found.")
                return@launch
            }
            repository.deleteUser(user)
            sessionManager.clearSession()
            _appFlow.value = AppFlow.LOGIN
            triggerNotification("Account Deleted ⚠️", "Your account has been deleted.")
            onSuccess()
        }
    }

    fun updateActivity(activity: Activity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateActivity(activity)
            if (_selectedActivity.value?.id == activity.id) {
                _selectedActivity.value = activity
            }
        }
    }

    fun deleteActivity(activity: Activity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteActivity(activity)
            if (_selectedActivity.value?.id == activity.id) {
                _selectedActivity.value = null
            }
        }
    }
}
