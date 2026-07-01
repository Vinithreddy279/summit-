package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class SummitViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = SummitRepository(db)

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

    private val _recordingSportType = MutableStateFlow("run") // "run", "ride", "hike", "walk", "swim"
    val recordingSportType: StateFlow<String> = _recordingSportType.asStateFlow()

    private val _recordingGearId = MutableStateFlow<Int?>(null)
    val recordingGearId: StateFlow<Int?> = _recordingGearId.asStateFlow()

    private val _recordingTrackpoints = MutableStateFlow<List<GPSPoint>>(emptyList())
    val recordingTrackpoints: StateFlow<List<GPSPoint>> = _recordingTrackpoints.asStateFlow()

    private val _selectedSimulationRoute = MutableStateFlow<String?>("None")
    val selectedSimulationRoute: StateFlow<String?> = _selectedSimulationRoute.asStateFlow()

    // Temporary variables for recording simulation
    private var recordingJob: Job? = null
    private var simulationIndex = 0
    private var simulatedCoordinates = emptyList<Pair<Double, Double>>()

    init {
        // Seed initial data if segments are empty
        viewModelScope.launch {
            val existingSegments = repository.segments.first()
            if (existingSegments.isEmpty()) {
                seedDatabase()
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
            }

            _newCommentText.value = ""
        }
    }

    fun insertCustomPost(title: String, content: String) {
        viewModelScope.launch {
            repository.insertFeedPost(
                FeedPost(
                    userName = "You",
                    userAvatar = "avatar_you",
                    title = "📣 $title",
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
            )
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
        _isRecording.value = true
        _recordingDurationSeconds.value = 0L
        _recordingDistanceKm.value = 0.0
        _recordingTrackpoints.value = emptyList()
        simulationIndex = 0

        // Prepare simulation points if a route is selected
        val routeName = _selectedSimulationRoute.value
        if (routeName != null && routeName != "None") {
            simulatedCoordinates = when (routeName) {
                "Twin Peaks Hill Climb" -> listOf(
                    Pair(37.7510, -122.4430),
                    Pair(37.7514, -122.4434),
                    Pair(37.7518, -122.4438),
                    Pair(37.7522, -122.4442),
                    Pair(37.7525, -122.4445),
                    Pair(37.7529, -122.4449),
                    Pair(37.7533, -122.4453),
                    Pair(37.7538, -122.4458),
                    Pair(37.7541, -122.4461),
                    Pair(37.7545, -122.4465)
                )
                "Golden Gate Bridge Crossing" -> listOf(
                    Pair(37.8110, -122.4770),
                    Pair(37.8122, -122.4771),
                    Pair(37.8134, -122.4774),
                    Pair(37.8146, -122.4776),
                    Pair(37.8158, -122.4778),
                    Pair(37.8170, -122.4780),
                    Pair(37.8182, -122.4781),
                    Pair(37.8194, -122.4783),
                    Pair(37.8206, -122.4784),
                    Pair(37.8220, -122.4786)
                )
                "Presidio Loop Cycle" -> listOf(
                    Pair(37.7980, -122.4660),
                    Pair(37.7992, -122.4666),
                    Pair(37.8005, -122.4672),
                    Pair(37.8018, -122.4678),
                    Pair(37.8030, -122.4685),
                    Pair(37.8042, -122.4690),
                    Pair(37.8055, -122.4695)
                )
                "Hawk Hill Peak Climb" -> listOf(
                    Pair(37.8280, -122.4820),
                    Pair(37.8288, -122.4835),
                    Pair(37.8300, -122.4860),
                    Pair(37.8310, -122.4890),
                    Pair(37.8320, -122.4920),
                    Pair(37.8330, -122.4960),
                    Pair(37.8335, -122.4990)
                )
                else -> emptyList()
            }
        } else {
            simulatedCoordinates = emptyList()
        }

        recordingJob = viewModelScope.launch {
            val rand = Random(42)
            while (_isRecording.value) {
                delay(1000)
                _recordingDurationSeconds.value += 1

                val currentPoints = _recordingTrackpoints.value.toMutableList()
                val paceMps = if (_recordingSportType.value == "ride") 6.5 else 3.5

                if (simulatedCoordinates.isNotEmpty()) {
                    // Simulating step-by-step
                    if (simulationIndex < simulatedCoordinates.size) {
                        val baseCoord = simulatedCoordinates[simulationIndex]
                        // Add tiny GPS drift/noise
                        val noiseLat = rand.nextDouble(-0.00002, 0.00002)
                        val noiseLng = rand.nextDouble(-0.00002, 0.00002)
                        val finalCoord = Pair(baseCoord.first + noiseLat, baseCoord.second + noiseLng)

                        val currentPoint = GPSPoint(
                            lat = finalCoord.first,
                            lng = finalCoord.second,
                            elevation = 12.0 + (simulationIndex * 4.5), // simulated climb
                            timeMs = System.currentTimeMillis(),
                            speedMps = paceMps + rand.nextDouble(-0.5, 0.5)
                        )

                        if (currentPoints.isNotEmpty()) {
                            val prev = currentPoints.last()
                            val stepDist = SegmentMatcher.haversineM(prev.latlng, currentPoint.latlng)
                            _recordingDistanceKm.value += (stepDist / 1000.0)
                        }

                        currentPoints.add(currentPoint)
                        _recordingTrackpoints.value = currentPoints
                        simulationIndex++
                    } else {
                        // Loop back or hold at end of simulation route
                        val baseCoord = simulatedCoordinates.last()
                        val noiseLat = rand.nextDouble(-0.00002, 0.00002)
                        val noiseLng = rand.nextDouble(-0.00002, 0.00002)
                        val finalCoord = Pair(baseCoord.first + noiseLat, baseCoord.second + noiseLng)

                        val currentPoint = GPSPoint(
                            lat = finalCoord.first,
                            lng = finalCoord.second,
                            elevation = 12.0 + (simulatedCoordinates.size * 4.5),
                            timeMs = System.currentTimeMillis(),
                            speedMps = 0.0
                        )
                        currentPoints.add(currentPoint)
                        _recordingTrackpoints.value = currentPoints
                    }
                } else {
                    // Manual dynamic tracker (for live demo walking) - we generate some artificial walk step in SF
                    val baseLat = 37.7749
                    val baseLng = -122.4194
                    val size = currentPoints.size
                    val movement = size * 0.0001
                    val finalCoord = Pair(baseLat + movement, baseLng + movement)

                    val currentPoint = GPSPoint(
                        lat = finalCoord.first,
                        lng = finalCoord.second,
                        elevation = 5.0,
                        timeMs = System.currentTimeMillis(),
                        speedMps = paceMps
                    )

                    if (currentPoints.isNotEmpty()) {
                        val prev = currentPoints.last()
                        val stepDist = SegmentMatcher.haversineM(prev.latlng, currentPoint.latlng)
                        _recordingDistanceKm.value += (stepDist / 1000.0)
                    }

                    currentPoints.add(currentPoint)
                    _recordingTrackpoints.value = currentPoints
                }
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        _isRecording.value = false
    }

    fun finishRecording(notes: String = "") {
        stopRecording()
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
            notes = notes
        )

        viewModelScope.launch {
            repository.saveRecordedActivity(activity)
            _recordingDurationSeconds.value = 0L
            _recordingDistanceKm.value = 0.0
            _recordingTrackpoints.value = emptyList()
            _currentTab.value = Tab.SOCIAL_FEED // jump to the feed to see your workout card!
        }
    }

    fun discardRecording() {
        stopRecording()
        _recordingDurationSeconds.value = 0L
        _recordingDistanceKm.value = 0.0
        _recordingTrackpoints.value = emptyList()
    }
}
