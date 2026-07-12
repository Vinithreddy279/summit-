package com.example.ui

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.GPSPoint
import com.example.data.SegmentMatcher
import com.example.data.AppDatabase
import com.example.data.SessionManager
import com.example.data.JsonHelper
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.roundToInt
import kotlin.random.Random

class TrackingService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var lastLocation: Location? = null
    private var simulationIndex = 0
    private val random = Random(42)

    // Upgraded recording engine states
    private var userWeightKg = 70.0
    private var lastMovementTimeMs = System.currentTimeMillis()
    private val recentSpeeds = mutableListOf<Double>()
    private var smoothedSpeedKmh = 0.0
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    companion object {
        const val NOTIFICATION_ID = 888
        const val CHANNEL_ID = "summit_tracking_service"

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"

        // Tracking States exposed to UI
        val isRecording = MutableStateFlow(false)
        val isPaused = MutableStateFlow(false)
        val isAutoPaused = MutableStateFlow(false)
        val durationSeconds = MutableStateFlow(0L)
        val distanceKm = MutableStateFlow(0.0)
        val trackpoints = MutableStateFlow<List<GPSPoint>>(emptyList())
        val currentSportType = MutableStateFlow("run")
        val selectedSimulationRoute = MutableStateFlow<String?>("None")
        
        // Detailed stats
        val currentSpeedKmh = MutableStateFlow(0.0)
        val avgSpeedKmh = MutableStateFlow(0.0)
        val currentPaceString = MutableStateFlow("00:00")
        val avgPaceString = MutableStateFlow("00:00")
        val gpsAccuracyMeters = MutableStateFlow(0.0)
        val caloriesBurned = MutableStateFlow(0.0)
        
        // Settings
        val autoPauseSetting = MutableStateFlow(false)

        fun startService(context: Context, sportType: String, simulationRoute: String?, autoPause: Boolean) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_START
                putExtra("SPORT_TYPE", sportType)
                putExtra("SIMULATION_ROUTE", simulationRoute)
                putExtra("AUTO_PAUSE", autoPause)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseTracking(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeTracking(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun resetStates() {
            isRecording.value = false
            isPaused.value = false
            isAutoPaused.value = false
            durationSeconds.value = 0L
            distanceKm.value = 0.0
            trackpoints.value = emptyList()
            currentSpeedKmh.value = 0.0
            avgSpeedKmh.value = 0.0
            currentPaceString.value = "00:00"
            avgPaceString.value = "00:00"
            gpsAccuracyMeters.value = 0.0
            caloriesBurned.value = 0.0
        }

        fun restoreActiveStateFromPrefs(context: Context): Boolean {
            val prefs = context.getSharedPreferences("summit_active_workout", Context.MODE_PRIVATE)
            val recording = prefs.getBoolean("is_recording", false)
            if (recording) {
                isRecording.value = true
                isPaused.value = prefs.getBoolean("is_paused", false)
                isAutoPaused.value = prefs.getBoolean("is_auto_paused", false)
                currentSportType.value = prefs.getString("sport_type", "run") ?: "run"
                selectedSimulationRoute.value = prefs.getString("simulation_route", "None") ?: "None"
                durationSeconds.value = prefs.getLong("duration_seconds", 0L)
                distanceKm.value = prefs.getFloat("distance_km", 0f).toDouble()
                caloriesBurned.value = prefs.getFloat("calories", 0f).toDouble()
                val pointsJson = prefs.getString("points_json", "") ?: ""
                if (pointsJson.isNotEmpty()) {
                    try {
                        trackpoints.value = JsonHelper.jsonToPoints(pointsJson)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return true
            }
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        // Acquire WakeLock to maintain tracking when screen is off
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WAKE_LOCK) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Summit::TrackingWakeLock").apply {
                    acquire(10 * 60 * 1000L) // 10 minutes timeout
                }
            } else {
                android.util.Log.w("TrackingService", "WAKE_LOCK permission not granted; skipping acquire")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Load user's weight from Room dynamically
        serviceScope.launch(Dispatchers.IO) {
            try {
                val sessionManager = SessionManager(applicationContext)
                val email = sessionManager.userEmailFlow.firstOrNull()
                if (!email.isNullOrEmpty()) {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val user = db.userDao().getUserByEmail(email)
                    if (user != null) {
                        userWeightKg = user.weightKg
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Dynamically observe simulation route changes to support immediate mode switching
        serviceScope.launch {
            selectedSimulationRoute.collect { route ->
                if (isRecording.value) {
                    val isDemo = route != null && route != "None"
                    if (isDemo) {
                        // Disable real GPS completely and reset simulator index
                        removeLocationUpdates()
                        lastLocation = null
                        simulationIndex = 0
                    } else {
                        // Disable simulator completely and use only real GPS
                        removeLocationUpdates()
                        if (!isPaused.value && !isAutoPaused.value) {
                            startLocationUpdates()
                        }
                    }
                    saveActiveStateToPrefs()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // Service killed and restarted by system: restore active state
            val restored = restoreActiveStateFromPrefs(this)
            if (restored && isRecording.value) {
                startTracking(isRestore = true)
            } else {
                stopSelf()
            }
        } else {
            intent.let {
                when (it.action) {
                    ACTION_START -> {
                        val isRestore = it.getBooleanExtra("IS_RESTORE", false)
                        if (isRestore) {
                            startTracking(isRestore = true)
                        } else {
                            val sport = it.getStringExtra("SPORT_TYPE") ?: "run"
                            val simulation = it.getStringExtra("SIMULATION_ROUTE") ?: "None"
                            val autoPause = it.getBooleanExtra("AUTO_PAUSE", false)
                            
                            currentSportType.value = sport
                            selectedSimulationRoute.value = simulation
                            autoPauseSetting.value = autoPause
                            
                            startTracking(isRestore = false)
                        }
                    }
                    ACTION_PAUSE -> {
                        isPaused.value = true
                        removeLocationUpdates() // stop GPS to save battery when manually paused
                        saveActiveStateToPrefs()
                        vibrate(applicationContext, 200L) // Double pulse pattern for pause
                    }
                    ACTION_RESUME -> {
                        isPaused.value = false
                        isAutoPaused.value = false
                        lastMovementTimeMs = System.currentTimeMillis()
                        if (selectedSimulationRoute.value == "None") {
                            startLocationUpdates() // resume GPS
                        }
                        saveActiveStateToPrefs()
                        vibrate(applicationContext, 400L) // Long vibration for resume
                    }
                    ACTION_STOP -> {
                        stopTracking()
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTracking(isRestore: Boolean = false) {
        if (isRestore) {
            restoreActiveStateFromPrefs(this)
        } else {
            resetStates()
            simulationIndex = 0
            lastLocation = null
        }
        isRecording.value = true
        lastMovementTimeMs = System.currentTimeMillis()
        recentSpeeds.clear()
        smoothedSpeedKmh = if (trackpoints.value.isNotEmpty()) currentSpeedKmh.value else 0.0

        // Start Foreground Service with notification
        startForeground(NOTIFICATION_ID, buildNotification("Starting Summit Tracker..."))

        // Register Real GPS Location Client if simulation is None
        if (selectedSimulationRoute.value == "None" && !isPaused.value && !isAutoPaused.value) {
            startLocationUpdates()
        }

        // Start the 1-second interval coroutine for time counting and simulation
        serviceScope.launch {
            while (isRecording.value) {
                delay(1000L)
                if (!isPaused.value && !isAutoPaused.value) {
                    durationSeconds.value += 1
                    
                    val simRoute = selectedSimulationRoute.value
                    if (simRoute != null && simRoute != "None") {
                        simulateStep()
                    } else {
                        // Real location mode: Check stationary condition for auto-pause (10 seconds)
                        val elapsedStationary = System.currentTimeMillis() - lastMovementTimeMs
                        if (autoPauseSetting.value && elapsedStationary > 10000L) {
                            isAutoPaused.value = true
                            currentSpeedKmh.value = 0.0
                            smoothedSpeedKmh = 0.0
                            triggerDoubleVibration() // Auto pause vibration feedback
                            saveActiveStateToPrefs()
                        }
                    }
                    
                    // High-accuracy calorie tracking per second based on MET, weight, speed
                    val met = getMetValue(currentSportType.value, currentSpeedKmh.value)
                    val calPerSec = (met * 3.5 * userWeightKg) / (200.0 * 60.0)
                    caloriesBurned.value += calPerSec

                    updateStats()
                    updateNotification()

                    // Periodically auto-save state to prefs to handle unexpected closes safely
                    if (durationSeconds.value % 5 == 0L) {
                        saveActiveStateToPrefs()
                    }
                }
            }
        }

        // Trigger Start vibration
        vibrate(applicationContext, 300L)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("TrackingService", "Location permissions not granted; cannot request location updates")
            return
        }
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).apply {
                setMinUpdateIntervalMillis(1000L)
                setMinUpdateDistanceMeters(0.5f)
            }.build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    if (isPaused.value || !isRecording.value) return
                    
                    val location = result.lastLocation ?: return
                    val accuracy = location.accuracy
                    gpsAccuracyMeters.value = accuracy.toDouble()
                    
                    // 1. High-accuracy GPS filtering: ignore points with poor accuracy (> 20 meters)
                    if (accuracy > 20f) return
                    
                    val speedMps = location.speed
                    val speedKmhValue = speedMps * 3.6
                    
                    // Check if movement resumed to auto-resume
                    if (isAutoPaused.value) {
                        if (speedMps >= 0.6) { // > 2.16 km/h
                            isAutoPaused.value = false
                            lastMovementTimeMs = System.currentTimeMillis()
                            vibrate(applicationContext, 400L) // Auto resume vibration
                        } else {
                            // Still stationary
                            currentSpeedKmh.value = 0.0
                            return
                        }
                    }
                    
                    // 2. Ignore duplicate points and GPS jumps
                    val lastLoc = lastLocation
                    if (lastLoc != null) {
                        val distanceDeltaM = location.distanceTo(lastLoc)
                        val timeDeltaSec = (location.time - lastLoc.time) / 1000.0
                        
                        // Ignore exact duplicates or tiny GPS drift while standing still (< 1.2m)
                        if (distanceDeltaM < 1.2) {
                            return
                        }
                        
                        // Ignore massive GPS jumps (speed of jump > 35 m/s = 126 km/h) with moderate/high accuracy
                        if (timeDeltaSec > 0.0) {
                            val calculatedSpeed = distanceDeltaM / timeDeltaSec
                            if (calculatedSpeed > 35.0 && accuracy > 12f) {
                                // Clear jump, ignore this location
                                return
                            }
                        }
                        
                        // 3. Increment distance
                        distanceKm.value += (distanceDeltaM / 1000.0)
                    }
                    
                    lastLocation = location
                    lastMovementTimeMs = System.currentTimeMillis()
                    
                    // Low-pass EMA filter for smoothing current speed updates
                    val instantSpeed = location.speed * 3.6
                    smoothedSpeedKmh = if (smoothedSpeedKmh == 0.0) instantSpeed else (0.7 * smoothedSpeedKmh + 0.3 * instantSpeed)
                    currentSpeedKmh.value = smoothedSpeedKmh

                    // Add current speed to sliding window for smooth pace calculation
                    recentSpeeds.add(location.speed.toDouble())
                    if (recentSpeeds.size > 5) {
                        recentSpeeds.removeAt(0)
                    }

                    // Append point
                    val pts = trackpoints.value.toMutableList()
                    val point = GPSPoint(
                        lat = location.latitude,
                        lng = location.longitude,
                        elevation = location.altitude,
                        timeMs = System.currentTimeMillis(),
                        speedMps = location.speed.toDouble(),
                        hasElevation = location.hasAltitude()
                    )
                    pts.add(point)
                    trackpoints.value = pts
                    
                    saveActiveStateToPrefs()
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun removeLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    private fun stopTracking() {
        isRecording.value = false
        clearActiveStatePrefs()
        removeLocationUpdates()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        serviceJob.cancel()
        stopForeground(true)
        stopSelf()

        // Trigger Finish vibration
        vibrate(applicationContext, 600L)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    private fun simulateStep() {
        val simRoute = selectedSimulationRoute.value ?: return
        val coordinates = getSimulationRouteCoordinates(simRoute)
        if (coordinates.isEmpty()) return

        val paceMps = if (currentSportType.value == "ride") 8.5 else 3.5
        val pts = trackpoints.value.toMutableList()

        if (simulationIndex < coordinates.size) {
            val baseCoord = coordinates[simulationIndex]
            
            // Add tiny GPS drift/noise for high-fidelity maps updates
            val noiseLat = random.nextDouble(-0.00001, 0.00001)
            val noiseLng = random.nextDouble(-0.00001, 0.00001)
            val finalCoord = Pair(baseCoord.first + noiseLat, baseCoord.second + noiseLng)

            val speedMpsValue = paceMps + random.nextDouble(-0.5, 0.5)
            val point = GPSPoint(
                lat = finalCoord.first,
                lng = finalCoord.second,
                elevation = 15.0 + (simulationIndex * 5.0),
                timeMs = System.currentTimeMillis(),
                speedMps = speedMpsValue
            )

            if (pts.isNotEmpty()) {
                val prev = pts.last()
                val stepDist = SegmentMatcher.haversineM(prev.latlng, point.latlng)
                distanceKm.value += (stepDist / 1000.0)
            }

            pts.add(point)
            trackpoints.value = pts
            
            // Speed smoothing
            val instantSpeed = speedMpsValue * 3.6
            smoothedSpeedKmh = if (smoothedSpeedKmh == 0.0) instantSpeed else (0.7 * smoothedSpeedKmh + 0.3 * instantSpeed)
            currentSpeedKmh.value = smoothedSpeedKmh

            // Sliding window pace speed
            recentSpeeds.add(speedMpsValue)
            if (recentSpeeds.size > 5) {
                recentSpeeds.removeAt(0)
            }

            gpsAccuracyMeters.value = random.nextDouble(1.5, 3.2)
            simulationIndex++
            lastMovementTimeMs = System.currentTimeMillis()
        } else {
            // Stand-by or slow motion at end point (stationary)
            val baseCoord = coordinates.last()
            val noiseLat = random.nextDouble(-0.000005, 0.000005)
            val noiseLng = random.nextDouble(-0.000005, 0.000005)
            val finalCoord = Pair(baseCoord.first + noiseLat, baseCoord.second + noiseLng)

            val point = GPSPoint(
                lat = finalCoord.first,
                lng = finalCoord.second,
                elevation = 15.0 + (coordinates.size * 5.0),
                timeMs = System.currentTimeMillis(),
                speedMps = 0.0
            )
            pts.add(point)
            trackpoints.value = pts
            
            currentSpeedKmh.value = 0.0
            smoothedSpeedKmh = 0.0
            recentSpeeds.clear()
            gpsAccuracyMeters.value = random.nextDouble(1.1, 1.8)
        }
    }

    private fun updateStats() {
        val dist = distanceKm.value
        val sec = durationSeconds.value

        // Average Speed
        avgSpeedKmh.value = if (sec > 0) (dist / (sec / 3600.0)) else 0.0

        // Current Pace: Smoothed with sliding window
        val avgCurrentSpeedMps = if (recentSpeeds.isNotEmpty()) recentSpeeds.average() else (currentSpeedKmh.value / 3.6)
        currentPaceString.value = formatPace(avgCurrentSpeedMps)

        // Average Pace
        val avgSpeedMps = if (dist > 0) (dist * 1000.0) / sec else 0.0
        avgPaceString.value = formatPace(avgSpeedMps)
    }

    private fun formatPace(speedMps: Double): String {
        if (speedMps <= 0.1) return "00:00"
        val secondsPerKm = (1000.0 / speedMps).roundToInt()
        val m = secondsPerKm / 60
        val s = secondsPerKm % 60
        if (m > 99) return "99:59"
        return String.format("%02d:%02d", m, s)
    }

    private fun getMetValue(sportType: String, speedKmh: Double): Double {
        if (speedKmh < 0.5) return 1.0 // Stationary (Resting BMR)
        return when (sportType.lowercase()) {
            "run" -> {
                when {
                    speedKmh < 6.4 -> 6.0
                    speedKmh < 8.0 -> 8.3
                    speedKmh < 9.6 -> 9.8
                    speedKmh < 11.2 -> 11.0
                    speedKmh < 12.8 -> 11.8
                    else -> 12.8
                }
            }
            "ride" -> {
                when {
                    speedKmh < 15.0 -> 4.0
                    speedKmh < 20.0 -> 6.0
                    speedKmh < 25.0 -> 8.0
                    speedKmh < 30.0 -> 10.0
                    else -> 12.0
                }
            }
            "walk" -> {
                when {
                    speedKmh < 3.2 -> 2.0
                    speedKmh < 4.8 -> 3.3
                    speedKmh < 6.4 -> 4.3
                    else -> 5.0
                }
            }
            "hike" -> {
                if (speedKmh < 4.0) 6.0 else 7.5
            }
            else -> 6.0
        }
    }

    private fun saveActiveStateToPrefs() {
        val prefs = getSharedPreferences("summit_active_workout", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_recording", isRecording.value)
            putBoolean("is_paused", isPaused.value)
            putBoolean("is_auto_paused", isAutoPaused.value)
            putString("sport_type", currentSportType.value)
            putString("simulation_route", selectedSimulationRoute.value ?: "None")
            putLong("duration_seconds", durationSeconds.value)
            putFloat("distance_km", distanceKm.value.toFloat())
            putFloat("calories", caloriesBurned.value.toFloat())
            putString("points_json", JsonHelper.pointsToJson(trackpoints.value))
            apply()
        }
    }

    private fun clearActiveStatePrefs() {
        val prefs = getSharedPreferences("summit_active_workout", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun getSimulationRouteCoordinates(route: String): List<Pair<Double, Double>> {
        return when (route) {
            "Twin Peaks Hill Climb" -> listOf(
                Pair(37.7510, -122.4430),
                Pair(37.7512, -122.4432),
                Pair(37.7514, -122.4434),
                Pair(37.7516, -122.4436),
                Pair(37.7518, -122.4438),
                Pair(37.7520, -122.4440),
                Pair(37.7522, -122.4442),
                Pair(37.7524, -122.4444),
                Pair(37.7525, -122.4445),
                Pair(37.7527, -122.4447),
                Pair(37.7529, -122.4449),
                Pair(37.7531, -122.4451),
                Pair(37.7533, -122.4453),
                Pair(37.7535, -122.4455),
                Pair(37.7538, -122.4458),
                Pair(37.7540, -122.4460),
                Pair(37.7541, -122.4461),
                Pair(37.7543, -122.4463),
                Pair(37.7545, -122.4465)
            )
            "Golden Gate Bridge Crossing" -> listOf(
                Pair(37.8110, -122.4770),
                Pair(37.8115, -122.4770),
                Pair(37.8122, -122.4771),
                Pair(37.8128, -122.4772),
                Pair(37.8134, -122.4774),
                Pair(37.8140, -122.4775),
                Pair(37.8146, -122.4776),
                Pair(37.8152, -122.4777),
                Pair(37.8158, -122.4778),
                Pair(37.8164, -122.4779),
                Pair(37.8170, -122.4780),
                Pair(37.8176, -122.4781),
                Pair(37.8182, -122.4781),
                Pair(37.8188, -122.4782),
                Pair(37.8194, -122.4783),
                Pair(37.8200, -122.4783),
                Pair(37.8206, -122.4784),
                Pair(37.8212, -122.4785),
                Pair(37.8220, -122.4786)
            )
            "Presidio Loop Cycle" -> listOf(
                Pair(37.7980, -122.4660),
                Pair(37.7985, -122.4663),
                Pair(37.7992, -122.4666),
                Pair(37.7998, -122.4669),
                Pair(37.8005, -122.4672),
                Pair(37.8012, -122.4675),
                Pair(37.8018, -122.4678),
                Pair(37.8024, -122.4682),
                Pair(37.8030, -122.4685),
                Pair(37.8036, -122.4687),
                Pair(37.8042, -122.4690),
                Pair(37.8048, -122.4693),
                Pair(37.8055, -122.4695)
            )
            "Hawk Hill Peak Climb" -> listOf(
                Pair(37.8280, -122.4820),
                Pair(37.8284, -122.4827),
                Pair(37.8288, -122.4835),
                Pair(37.8294, -122.4847),
                Pair(37.8300, -122.4860),
                Pair(37.8305, -122.4875),
                Pair(37.8310, -122.4890),
                Pair(37.8315, -122.4905),
                Pair(37.8320, -122.4920),
                Pair(37.8325, -122.4940),
                Pair(37.8330, -122.4960),
                Pair(37.8333, -122.4975),
                Pair(37.8335, -122.4990)
            )
            else -> emptyList()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Summit Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Summit Live Recording")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val distText = String.format("%.2f km", distanceKm.value)
        val durationText = formatDuration(durationSeconds.value)
        val text = "Distance: $distText • Time: $durationText"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(context: Context, durationMs: Long) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerDoubleVibration() {
        serviceScope.launch {
            vibrate(applicationContext, 150L)
            delay(250L)
            vibrate(applicationContext, 150L)
        }
    }
}
