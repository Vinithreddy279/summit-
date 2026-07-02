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
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
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
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START -> {
                    val sport = it.getStringExtra("SPORT_TYPE") ?: "run"
                    val simulation = it.getStringExtra("SIMULATION_ROUTE") ?: "None"
                    val autoPause = it.getBooleanExtra("AUTO_PAUSE", false)
                    
                    currentSportType.value = sport
                    selectedSimulationRoute.value = simulation
                    autoPauseSetting.value = autoPause
                    
                    startTracking()
                }
                ACTION_PAUSE -> {
                    isPaused.value = true
                }
                ACTION_RESUME -> {
                    isPaused.value = false
                    isAutoPaused.value = false
                }
                ACTION_STOP -> {
                    stopTracking()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTracking() {
        resetStates()
        isRecording.value = true
        simulationIndex = 0
        lastLocation = null

        // Start Foreground Service with notification
        startForeground(NOTIFICATION_ID, buildNotification("Starting Summit Tracker..."))

        // Register Real GPS Location Client if simulation is None
        if (selectedSimulationRoute.value == "None") {
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
                        // Real location mode: If we haven't got location update, we don't update coordinates.
                        // However, we can update elapsed time and other stats.
                    }
                    updateStats()
                    updateNotification()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).apply {
                setMinUpdateIntervalMillis(1000L)
                setMinUpdateDistanceMeters(0.5f)
            }.build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    if (isPaused.value || !isRecording.value) return
                    
                    val location = result.lastLocation ?: return
                    gpsAccuracyMeters.value = location.accuracy.toDouble()
                    
                    // Auto pause check
                    val speedMps = location.speed
                    val speedKmhValue = speedMps * 3.6
                    
                    if (autoPauseSetting.value && speedKmhValue < 1.5) {
                        isAutoPaused.value = true
                        currentSpeedKmh.value = 0.0
                        return
                    } else if (isAutoPaused.value && speedKmhValue >= 2.0) {
                        isAutoPaused.value = false
                    }

                    if (isAutoPaused.value) return

                    // Interpolate/smooth jumping GPS coordinates
                    val filteredLocation = if (lastLocation != null) {
                        val distance = location.distanceTo(lastLocation!!)
                        if (distance > 40.0 && location.accuracy > 20f) {
                            // High probability of GPS jump, smooth/interpolate it
                            val alpha = 0.3f
                            Location(location).apply {
                                latitude = alpha * location.latitude + (1 - alpha) * lastLocation!!.latitude
                                longitude = alpha * location.longitude + (1 - alpha) * lastLocation!!.longitude
                            }
                        } else {
                            location
                        }
                    } else {
                        location
                    }

                    // Calculate distance
                    if (lastLocation != null) {
                        val distanceDeltaM = filteredLocation.distanceTo(lastLocation!!)
                        distanceKm.value += (distanceDeltaM / 1000.0)
                    }

                    lastLocation = filteredLocation
                    currentSpeedKmh.value = filteredLocation.speed * 3.6

                    // Append point
                    val pts = trackpoints.value.toMutableList()
                    val point = GPSPoint(
                        lat = filteredLocation.latitude,
                        lng = filteredLocation.longitude,
                        elevation = filteredLocation.altitude,
                        timeMs = System.currentTimeMillis(),
                        speedMps = filteredLocation.speed.toDouble()
                    )
                    pts.add(point)
                    trackpoints.value = pts
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

    private fun stopTracking() {
        isRecording.value = false
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        serviceJob.cancel()
        stopForeground(true)
        stopSelf()
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

            val point = GPSPoint(
                lat = finalCoord.first,
                lng = finalCoord.second,
                elevation = 15.0 + (simulationIndex * 5.0),
                timeMs = System.currentTimeMillis(),
                speedMps = paceMps + random.nextDouble(-0.5, 0.5)
            )

            if (pts.isNotEmpty()) {
                val prev = pts.last()
                val stepDist = SegmentMatcher.haversineM(prev.latlng, point.latlng)
                distanceKm.value += (stepDist / 1000.0)
            }

            pts.add(point)
            trackpoints.value = pts
            currentSpeedKmh.value = point.speedMps * 3.6
            gpsAccuracyMeters.value = random.nextDouble(1.5, 3.2)
            simulationIndex++
        } else {
            // Stand-by or slow motion at end point
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
            gpsAccuracyMeters.value = random.nextDouble(1.1, 1.8)
        }
    }

    private fun updateStats() {
        val dist = distanceKm.value
        val sec = durationSeconds.value

        // Average Speed
        avgSpeedKmh.value = if (sec > 0) (dist / (sec / 3600.0)) else 0.0

        // Current Pace
        val currentSpeedMps = currentSpeedKmh.value / 3.6
        currentPaceString.value = formatPace(currentSpeedMps)

        // Average Pace
        val avgSpeedMps = (dist * 1000.0) / sec
        avgPaceString.value = formatPace(avgSpeedMps)

        // Calories
        val burnRate = when (currentSportType.value) {
            "run" -> 68.0
            "ride" -> 32.0
            "walk" -> 44.0
            "hike" -> 58.0
            else -> 50.0
        }
        caloriesBurned.value = dist * burnRate
    }

    private fun formatPace(speedMps: Double): String {
        if (speedMps <= 0.1) return "00:00"
        val secondsPerKm = (1000.0 / speedMps).roundToInt()
        val m = secondsPerKm / 60
        val s = secondsPerKm % 60
        if (m > 99) return "99:59"
        return String.format("%02d:%02d", m, s)
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
}
