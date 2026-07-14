package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Terrain
import com.example.ui.theme.SlateCardSurface
import com.example.ui.theme.SlateCardSurfaceVariant
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.OrangePrimary
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.data.GPSPoint

// Mapbox SDK (MapLibre Native Android) imports
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.annotations.Polyline
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.*

enum class OutdoorMapMode {
    HIKE,
    TREK,
    RUN,
    WALK,
    CYCLE
}

enum class MapViewMode {
    OUTDOOR,
    SATELLITE,
    TERRAIN_3D
}

enum class MapFollowMode {
    FOLLOWING,
    FREE_EXPLORE
}

enum class MapLoadState {
    LOADING,
    READY,
    MISSING_CONFIGURATION,
    STYLE_LOAD_FAILED,
    NETWORK_UNAVAILABLE
}

private const val MAP_DIAGNOSTICS_ENABLED = false

class OutdoorMapStateManager(
    initialMode: OutdoorMapMode = OutdoorMapMode.HIKE,
    initialFollowMode: MapFollowMode = MapFollowMode.FOLLOWING
) {
    var mode by mutableStateOf(initialMode)
        private set

    var followMode by mutableStateOf(initialFollowMode)
        private set

    fun setMapMode(newMode: OutdoorMapMode) {
        mode = newMode
    }

    fun handleUserInteraction() {
        followMode = MapFollowMode.FREE_EXPLORE
    }

    fun handleRecenter() {
        followMode = MapFollowMode.FOLLOWING
    }
}

fun mapSportToOutdoorMapMode(sport: String): OutdoorMapMode {
    return when (sport.lowercase()) {
        "hike" -> OutdoorMapMode.HIKE
        "run" -> OutdoorMapMode.RUN
        "walk" -> OutdoorMapMode.WALK
        "ride" -> OutdoorMapMode.CYCLE
        else -> OutdoorMapMode.TREK
    }
}

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}

@Composable
fun MapUnavailableView(
    message: String,
    icon: String = "🗺️",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFF0F172A))
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .border(1.dp, androidx.compose.ui.graphics.Color(0xFF334155), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = androidx.compose.ui.graphics.Color(0xFF334155).copy(alpha = 0.3f),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = icon,
                            fontSize = 28.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    color = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

fun applyStyleForMode(style: Style, mode: OutdoorMapMode) {
    try {
        android.util.Log.d("SummitOutdoorMap", "Applying map style for mode: $mode")
        
        // Task 1: Concise layer inventory for debugging in debug builds
        if (BuildConfig.DEBUG) {
            android.util.Log.v("SummitOutdoorMapStyle", "Layer inventory start (total: ${style.layers.size})")
            style.layers.forEach { layer ->
                android.util.Log.v("SummitOutdoorMapStyle", "Layer ID: ${layer.id}, Type: ${layer::class.java.simpleName}")
            }
            android.util.Log.v("SummitOutdoorMapStyle", "Layer inventory end")
        }

        // Task 5: Style reload safety. Remove previously added custom Summit-owned emphasis layers.
        val summitLayers = listOf(
            "summit-hike-emphasis",
            "summit-trek-emphasis",
            "summit-cycle-emphasis",
            "summit-run-emphasis",
            "summit-walk-emphasis"
        )
        summitLayers.forEach { layerId ->
            style.getLayer(layerId)?.let { layer ->
                style.removeLayer(layer)
            }
        }

        // Revert any "Outdoor" layer modifications back to default (Task 3/4/5)
        style.getLayer("Outdoor")?.let { outdoorLayer ->
            outdoorLayer.setProperties(
                PropertyFactory.iconSize(
                    interpolate(
                        linear(),
                        zoom(),
                        stop(14f, 0.8f),
                        stop(22f, 1.0f)
                    )
                )
            )
        }

        // Task 4: Add verified dynamic overlays with proper filter expressions
        when (mode) {
            OutdoorMapMode.HIKE -> {
                // Focus: hiking/footpaths
                val filter = all(
                    eq(literal("\$type"), literal("LineString")),
                    any(
                        eq(get("class"), "hiking"),
                        eq(get("class"), "foot"),
                        has("color")
                    )
                )
                val hikeLayer = LineLayer("summit-hike-emphasis", "outdoor")
                    .withSourceLayer("trail")
                    .withFilter(filter)
                hikeLayer.setProperties(
                    PropertyFactory.lineColor("#E65100"), // Vibrant Orange
                    PropertyFactory.lineWidth(4f),
                    PropertyFactory.lineOpacity(0.9f)
                )
                if (style.getLayer("Road labels") != null) {
                    style.addLayerBelow(hikeLayer, "Road labels")
                } else {
                    style.addLayer(hikeLayer)
                }
                android.util.Log.d("SummitOutdoorMap", "HIKE: Applied summit-hike-emphasis overlay on trail with source-layer trail.")
            }
            OutdoorMapMode.TREK -> {
                // Focus: wilderness trails and via_ferrata, or iwn/nwn routes
                val filter = all(
                    eq(literal("\$type"), literal("LineString")),
                    any(
                        eq(get("class"), "hiking"),
                        eq(get("class"), "via_ferrata"),
                        eq(get("network"), "iwn"),
                        eq(get("network"), "nwn")
                    )
                )
                val trekLayer = LineLayer("summit-trek-emphasis", "outdoor")
                    .withSourceLayer("trail")
                    .withFilter(filter)
                trekLayer.setProperties(
                    PropertyFactory.lineColor("#D84315"), // Deep Red-Orange
                    PropertyFactory.lineWidth(4.5f),
                    PropertyFactory.lineOpacity(0.95f)
                )
                if (style.getLayer("Road labels") != null) {
                    style.addLayerBelow(trekLayer, "Road labels")
                } else {
                    style.addLayer(trekLayer)
                }

                // Focus: shelter/hut POI scaling via expression on the verified "Outdoor" layer (Task 3)
                style.getLayer("Outdoor")?.let { outdoorLayer ->
                    outdoorLayer.setProperties(
                        PropertyFactory.iconSize(
                            switchCase(
                                any(eq(get("class"), "shelter"), eq(get("class"), "hut")),
                                literal(1.5f),
                                interpolate(
                                    linear(),
                                    zoom(),
                                    stop(14f, 0.8f),
                                    stop(22f, 1.0f)
                                )
                            )
                        )
                    )
                }
                android.util.Log.d("SummitOutdoorMap", "TREK: Applied summit-trek-emphasis and enlarged shelter POIs.")
            }
            OutdoorMapMode.CYCLE -> {
                // Focus: bicycle paths and cycle routes
                val filter = all(
                    eq(literal("\$type"), literal("LineString")),
                    any(
                        eq(get("class"), "bicycle"),
                        eq(get("network"), "icn"),
                        eq(get("network"), "ncn")
                    )
                )
                val cycleLayer = LineLayer("summit-cycle-emphasis", "outdoor")
                    .withSourceLayer("trail")
                    .withFilter(filter)
                cycleLayer.setProperties(
                    PropertyFactory.lineColor("#00E676"), // Bright Green
                    PropertyFactory.lineWidth(4f),
                    PropertyFactory.lineOpacity(0.9f)
                )
                if (style.getLayer("Road labels") != null) {
                    style.addLayerBelow(cycleLayer, "Road labels")
                } else {
                    style.addLayer(cycleLayer)
                }
                android.util.Log.d("SummitOutdoorMap", "CYCLE: Applied summit-cycle-emphasis overlay.")
            }
            OutdoorMapMode.RUN -> {
                // Focus: path and pedestrian roads in maptiler_planet
                val filter = all(
                    eq(literal("\$type"), literal("LineString")),
                    any(
                        eq(get("class"), "path"),
                        eq(get("class"), "pedestrian")
                    )
                )
                val runLayer = LineLayer("summit-run-emphasis", "maptiler_planet")
                    .withSourceLayer("transportation")
                    .withFilter(filter)
                runLayer.setProperties(
                    PropertyFactory.lineColor("#00B0FF"), // Electric Blue
                    PropertyFactory.lineWidth(4f),
                    PropertyFactory.lineOpacity(0.9f)
                )
                if (style.getLayer("Road labels") != null) {
                    style.addLayerBelow(runLayer, "Road labels")
                } else {
                    style.addLayer(runLayer)
                }
                android.util.Log.d("SummitOutdoorMap", "RUN: Applied summit-run-emphasis overlay.")
            }
            OutdoorMapMode.WALK -> {
                // Focus: paths, pedestrian, and steps
                val filter = all(
                    eq(literal("\$type"), literal("LineString")),
                    any(
                        eq(get("class"), "path"),
                        eq(get("class"), "pedestrian"),
                        eq(get("subclass"), "steps")
                    )
                )
                val walkLayer = LineLayer("summit-walk-emphasis", "maptiler_planet")
                    .withSourceLayer("transportation")
                    .withFilter(filter)
                walkLayer.setProperties(
                    PropertyFactory.lineColor("#8E24AA"), // Vibrant Purple
                    PropertyFactory.lineWidth(3.5f),
                    PropertyFactory.lineOpacity(0.85f)
                )
                if (style.getLayer("Road labels") != null) {
                    style.addLayerBelow(walkLayer, "Road labels")
                } else {
                    style.addLayer(walkLayer)
                }
                android.util.Log.d("SummitOutdoorMap", "WALK: Applied summit-walk-emphasis overlay.")
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("SummitOutdoorMap", "Error applying style for mode $mode: ${e.message}", e)
    }
}

@Composable
fun SummitOutdoorMapView(
    points: List<GPSPoint>,
    isLiveTracking: Boolean,
    sportType: String,
    modifier: Modifier = Modifier,
    viewMode: MapViewMode = MapViewMode.OUTDOOR,
    onViewModeChange: ((MapViewMode) -> Unit)? = null,
    stateManager: OutdoorMapStateManager = remember { OutdoorMapStateManager() }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Maintain local state for the map view mode if none is provided (backwards compatibility)
    var localViewMode by remember { mutableStateOf(MapViewMode.OUTDOOR) }
    val activeViewMode = if (onViewModeChange != null) viewMode else localViewMode
    val activeOnViewModeChange: (MapViewMode) -> Unit = { mode ->
        if (onViewModeChange != null) {
            onViewModeChange(mode)
        } else {
            localViewMode = mode
        }
    }

    LaunchedEffect(sportType) {
        stateManager.setMapMode(mapSportToOutdoorMapMode(sportType))
    }

    val maptilerKey = BuildConfig.MAPTILER_API_KEY
    val isPlaceholderKey = maptilerKey.isNullOrEmpty() || maptilerKey == "placeholder_maptiler_key"
    
    val styleUrl = remember(activeViewMode, maptilerKey) {
        if (isPlaceholderKey) {
            "https://demotiles.maplibre.org/style.json"
        } else {
            when (activeViewMode) {
                MapViewMode.OUTDOOR -> "https://api.maptiler.com/maps/outdoor-v2/style.json?key=$maptilerKey"
                MapViewMode.SATELLITE -> "https://api.maptiler.com/maps/hybrid/style.json?key=$maptilerKey"
                MapViewMode.TERRAIN_3D -> "https://api.maptiler.com/maps/outdoor-v2/style.json?key=$maptilerKey"
            }
        }
    }

    // Diagnostic logging with redacted key
    LaunchedEffect(maptilerKey) {
        val configuredStatus = if (isPlaceholderKey) "false (using placeholder or null)" else "true"
        android.util.Log.d("SummitOutdoorMap", "MAPTILER_API_KEY configured: $configuredStatus")
    }

    remember {
        Mapbox.getInstance(context)
        android.util.Log.d("SummitOutdoorMap", "Mapbox SDK initialized. Global User-Agent override is not supported in MapLibre 10.0.2.")
        true
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    var maplibreMapState by remember { mutableStateOf<MapboxMap?>(null) }

    LaunchedEffect(stateManager.mode, maplibreMapState) {
        val map = maplibreMapState ?: return@LaunchedEffect
        map.getStyle { style ->
            applyStyleForMode(style, stateManager.mode)
        }
    }
    
    var startMarker by remember { mutableStateOf<Marker?>(null) }
    var locationMarker by remember { mutableStateOf<Marker?>(null) }

    var mapLoadState by remember { mutableStateOf(if (isPlaceholderKey) MapLoadState.MISSING_CONFIGURATION else MapLoadState.LOADING) }

    val startIcon = remember(context) {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xFF4CAF50.toInt()
        canvas.drawCircle(size / 2f, size / 2f, 8f, paint)
        paint.color = 0x404CAF50.toInt()
        canvas.drawCircle(size / 2f, size / 2f, 16f, paint)
        IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    val locationIcon = remember(context) {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xFF007AFF.toInt()
        canvas.drawCircle(size / 2f, size / 2f, 10f, paint)
        paint.color = 0x40007AFF.toInt()
        canvas.drawCircle(size / 2f, size / 2f, 20f, paint)
        IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    val finishIcon = remember(context) {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xFFE91E63.toInt()
        canvas.drawCircle(size / 2f, size / 2f, 8f, paint)
        paint.color = 0x40E91E63.toInt()
        canvas.drawCircle(size / 2f, size / 2f, 16f, paint)
        IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    val comboIcon = remember(context) {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        // Outer loop finish halo (pink)
        paint.color = 0x40E91E63.toInt()
        canvas.drawCircle(size / 2f, size / 2f, 16f, paint)
        // Middle finish ring (pink)
        paint.color = 0xFFE91E63.toInt()
        canvas.drawCircle(size / 2f, size / 2f, 12f, paint)
        // Inner start circle (green)
        paint.color = 0xFF4CAF50.toInt()
        canvas.drawCircle(size / 2f, size / 2f, 7f, paint)
        IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView.onDestroy()
            }
        }
    }

    Box(modifier = modifier) {
        when (mapLoadState) {
            MapLoadState.MISSING_CONFIGURATION -> {
                if (BuildConfig.DEBUG) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color(0xFF0F172A))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, androidx.compose.ui.graphics.Color(0xFF334155), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "⚠️ MAPTILER KEY MISSING",
                                    color = androidx.compose.ui.graphics.Color(0xFFEF4444),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Failed to load the MapTiler Outdoor vector style. Please verify that your MAPTILER_API_KEY is valid, not revoked, and has correct permissions.",
                                    color = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    MapUnavailableView(message = "Outdoor map is temporarily unavailable.")
                }
            }
            MapLoadState.NETWORK_UNAVAILABLE -> {
                MapUnavailableView(
                    message = "Network unavailable. Please check your connection.",
                    icon = "📶"
                )
            }
            MapLoadState.STYLE_LOAD_FAILED -> {
                MapUnavailableView(
                    message = "Failed to load the map style. Please check your MapTiler configuration.",
                    icon = "🗺️"
                )
            }
            MapLoadState.LOADING, MapLoadState.READY -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        mapView.apply {
                            addOnDidFailLoadingMapListener { errorMessage ->
                                android.util.Log.e("SummitOutdoorMap", "Map load failed: $errorMessage")
                                if (mapLoadState == MapLoadState.LOADING) {
                                    mapLoadState = if (!isNetworkAvailable(context)) {
                                        MapLoadState.NETWORK_UNAVAILABLE
                                    } else {
                                        MapLoadState.STYLE_LOAD_FAILED
                                    }
                                }
                            }
                            getMapAsync { map ->
                                maplibreMapState = map
                                map.uiSettings.isAttributionEnabled = true
                                map.uiSettings.isLogoEnabled = true
                                
                                map.addOnMoveListener(object : MapboxMap.OnMoveListener {
                                    override fun onMoveBegin(detector: MoveGestureDetector) {
                                        stateManager.handleUserInteraction()
                                    }
                                    override fun onMove(detector: MoveGestureDetector) {}
                                    override fun onMoveEnd(detector: MoveGestureDetector) {}
                                })

                                map.setStyle(styleUrl) { style ->
                                    updateRouteAndLayers(map, style, points, isLiveTracking, activeViewMode, sportType)
                                    mapLoadState = MapLoadState.READY
                                }
                            }
                        }
                    },
                    update = { _ ->
                        val map = maplibreMapState ?: return@AndroidView
                        val safePoints = points.toList()

                        // Style loading logic
                        val currentStyle = map.style
                        if (currentStyle == null || currentStyle.url != styleUrl) {
                            map.setStyle(styleUrl) { loadedStyle ->
                                updateRouteAndLayers(map, loadedStyle, safePoints, isLiveTracking, activeViewMode, sportType)
                                if (activeViewMode == MapViewMode.TERRAIN_3D) {
                                    val currentCamera = map.cameraPosition
                                    val newCamera = CameraPosition.Builder(currentCamera)
                                        .tilt(55.0)
                                        .build()
                                    map.animateCamera(CameraUpdateFactory.newCameraPosition(newCamera), 800)
                                } else {
                                    val currentCamera = map.cameraPosition
                                    if (currentCamera.tilt > 0.0) {
                                        val newCamera = CameraPosition.Builder(currentCamera)
                                            .tilt(0.0)
                                            .build()
                                        map.animateCamera(CameraUpdateFactory.newCameraPosition(newCamera), 800)
                                    }
                                }
                            }
                        } else {
                            updateRouteAndLayers(map, currentStyle, safePoints, isLiveTracking, activeViewMode, sportType)
                        }

                        // Drawing markers and updating camera
                        if (safePoints.isNotEmpty()) {
                            val mapPoints = safePoints.map { LatLng(it.lat, it.lng) }
                            val firstPt = mapPoints.first()
                            val lastPt = mapPoints.last()
                            
                            val hasMovement = safePoints.size >= 2 && hasMeaningfulMovement(safePoints)
                            val isLoop = hasMovement && distanceBetween(firstPt, lastPt) < 25.0

                            if (isLoop && !isLiveTracking) {
                                startMarker?.let { map.removeMarker(it) }
                                startMarker = map.addMarker(
                                    MarkerOptions()
                                        .position(firstPt)
                                        .icon(comboIcon)
                                        .title("Start & Finish")
                                )
                                locationMarker?.let { map.removeMarker(it) }
                                locationMarker = null
                            } else {
                                if (hasMovement) {
                                    startMarker?.let { map.removeMarker(it) }
                                    startMarker = map.addMarker(
                                        MarkerOptions()
                                            .position(firstPt)
                                            .icon(startIcon)
                                            .title("Start Point")
                                    )

                                    locationMarker?.let { map.removeMarker(it) }
                                    locationMarker = map.addMarker(
                                        MarkerOptions()
                                            .position(lastPt)
                                            .icon(if (isLiveTracking) locationIcon else finishIcon)
                                            .title(if (isLiveTracking) "My Location" else "Finish Point")
                                    )
                                } else {
                                    startMarker?.let { map.removeMarker(it) }
                                    startMarker = null
                                    locationMarker?.let { map.removeMarker(it) }
                                    locationMarker = map.addMarker(
                                        MarkerOptions()
                                            .position(lastPt)
                                            .icon(if (isLiveTracking) locationIcon else finishIcon)
                                            .title(if (isLiveTracking) "My Location" else "Finish Point")
                                    )
                                }
                            }

                            // Camera Updates
                            if (isLiveTracking) {
                                if (stateManager.followMode == MapFollowMode.FOLLOWING) {
                                    val currentCamera = map.cameraPosition
                                    val cameraPosition = CameraPosition.Builder()
                                        .target(lastPt)
                                        .zoom(15.5)
                                        .tilt(if (activeViewMode == MapViewMode.TERRAIN_3D) 55.0 else currentCamera.tilt)
                                        .build()
                                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000)
                                }
                            } else {
                                if (hasMovement) {
                                    try {
                                        val boundsBuilder = LatLngBounds.Builder()
                                        mapPoints.forEach { boundsBuilder.include(it) }
                                        val bounds = boundsBuilder.build()
                                        // Padding to ensure route is not cropped or hidden by cards
                                        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 80)
                                        map.animateCamera(cameraUpdate, 1000)
                                    } catch (e: Exception) {
                                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPt, 15.0), 1000)
                                    }
                                } else {
                                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPt, 15.0), 1000)
                                }
                            }
                        } else {
                            val sfCenter = LatLng(37.7749, -122.4194)
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(sfCenter, 12.0), 1000)
                        }
                    }
                )

                if (mapLoadState == MapLoadState.LOADING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color(0x800F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (BuildConfig.DEBUG && MAP_DIAGNOSTICS_ENABLED) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B).copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .border(1.dp, androidx.compose.ui.graphics.Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "MAP DIAGNOSTICS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "MAPTILER_API_KEY configured: TRUE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFF10B981)
                            )
                            Text(
                                text = "Style: MapTiler Outdoor Vector",
                                fontSize = 8.sp,
                                color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                            )
                            Text(
                                text = "Attribution: © MapTiler © OSM",
                                fontSize = 8.sp,
                                color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                if (isLiveTracking && stateManager.followMode == MapFollowMode.FREE_EXPLORE) {
                    FloatingActionButton(
                        onClick = {
                            stateManager.handleRecenter()
                            val lastPt = points.lastOrNull()
                            if (lastPt != null) {
                                maplibreMapState?.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(lastPt.lat, lastPt.lng), 15.5),
                                    800
                                )
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("map_recenter_button"),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Re-center Map",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Compact view mode selector on map overlay
                var showSelector by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showSelector = !showSelector },
                        containerColor = SlateCardSurface.copy(alpha = 0.9f),
                        contentColor = SlateTextPrimary,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("map_layers_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Map Style Layers",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showSelector) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(top = 48.dp)
                                .width(130.dp)
                                .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(12.dp))
                                .testTag("map_layers_selector_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                MapViewMode.values().forEach { mode ->
                                    val isSelected = activeViewMode == mode
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) OrangePrimary.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
                                            .clickable {
                                                activeOnViewModeChange(mode)
                                                showSelector = false
                                            }
                                            .padding(horizontal = 8.dp, vertical = 8.dp)
                                            .testTag("map_layer_option_${mode.name.lowercase()}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (mode) {
                                                MapViewMode.OUTDOOR -> Icons.Default.Terrain
                                                MapViewMode.SATELLITE -> Icons.Default.Satellite
                                                MapViewMode.TERRAIN_3D -> Icons.Default.Terrain
                                            },
                                            contentDescription = mode.name,
                                            tint = if (isSelected) OrangePrimary else SlateTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when (mode) {
                                                MapViewMode.OUTDOOR -> "Outdoor"
                                                MapViewMode.SATELLITE -> "Satellite"
                                                MapViewMode.TERRAIN_3D -> "3D"
                                            },
                                            color = if (isSelected) OrangePrimary else SlateTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun distanceBetween(p1: LatLng, p2: LatLng): Double {
    val results = FloatArray(1)
    try {
        android.location.Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
        return results[0].toDouble()
    } catch (e: Exception) {
        val dx = p1.longitude - p2.longitude
        val dy = p1.latitude - p2.latitude
        return Math.sqrt(dx * dx + dy * dy) * 111000.0
    }
}

private fun hasMeaningfulMovement(points: List<GPSPoint>): Boolean {
    if (points.size < 2) return false
    val first = points.first()
    for (i in 1 until points.size) {
        val pt = points[i]
        val results = FloatArray(1)
        try {
            android.location.Location.distanceBetween(first.lat, first.lng, pt.lat, pt.lng, results)
            if (results[0] > 10.0) return true
        } catch (e: Exception) {
            val dx = first.lng - pt.lng
            val dy = first.lat - pt.lat
            if (Math.sqrt(dx * dx + dy * dy) * 111000.0 > 10.0) return true
        }
    }
    return false
}

private fun updateRouteAndLayers(
    map: MapboxMap,
    style: Style,
    points: List<GPSPoint>,
    isLiveTracking: Boolean,
    viewMode: MapViewMode,
    sportType: String
) {
    try {
        // Apply map sport mode emphasis
        applyStyleForMode(style, mapSportToOutdoorMapMode(sportType))

        val safePoints = points.toList()
        val hasMovement = safePoints.size >= 2 && hasMeaningfulMovement(safePoints)
        
        if (hasMovement) {
            val mapPoints = safePoints.map { LatLng(it.lat, it.lng) }
            val mapboxPoints = mapPoints.map { com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude) }
            val lineString = com.mapbox.geojson.LineString.fromLngLats(mapboxPoints)
            val feature = com.mapbox.geojson.Feature.fromGeometry(lineString)
            val featureCollection = com.mapbox.geojson.FeatureCollection.fromFeature(feature)

            val sourceId = "summit-recorded-route-source"
            var source = style.getSourceAs<com.mapbox.mapboxsdk.style.sources.GeoJsonSource>(sourceId)
            if (source == null) {
                source = com.mapbox.mapboxsdk.style.sources.GeoJsonSource(sourceId, featureCollection)
                style.addSource(source)
            } else {
                source.setGeoJson(featureCollection)
            }

            // Route casing layer: summit-recorded-route-casing
            val casingId = "summit-recorded-route-casing"
            if (style.getLayer(casingId) == null) {
                val casingLayer = LineLayer(casingId, sourceId)
                casingLayer.setProperties(
                    PropertyFactory.lineColor("#1E293B"),
                    PropertyFactory.lineWidth(7f),
                    PropertyFactory.lineOpacity(0.85f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                )
                style.addLayer(casingLayer)
            } else {
                style.getLayer(casingId)?.setProperties(
                    PropertyFactory.lineColor("#1E293B"),
                    PropertyFactory.lineWidth(7f),
                    PropertyFactory.lineOpacity(0.85f)
                )
            }

            // Route core layer: summit-recorded-route
            val coreId = "summit-recorded-route"
            if (style.getLayer(coreId) == null) {
                val coreLayer = LineLayer(coreId, sourceId)
                coreLayer.setProperties(
                    PropertyFactory.lineColor("#FF6D00"),
                    PropertyFactory.lineWidth(4f),
                    PropertyFactory.lineOpacity(1.0f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                )
                style.addLayerAbove(coreLayer, casingId)
            } else {
                style.getLayer(coreId)?.setProperties(
                    PropertyFactory.lineColor("#FF6D00"),
                    PropertyFactory.lineWidth(4f),
                    PropertyFactory.lineOpacity(1.0f)
                )
            }
        } else {
            style.removeLayer("summit-recorded-route")
            style.removeLayer("summit-recorded-route-casing")
            style.removeSource("summit-recorded-route-source")
        }

        // Hillshading overlay for TERRAIN_3D mode
        if (viewMode == MapViewMode.TERRAIN_3D) {
            val hillshadeSourceId = "maptiler-hillshade-source"
            if (style.getSource(hillshadeSourceId) == null) {
                val key = BuildConfig.MAPTILER_API_KEY
                if (!key.isNullOrEmpty() && key != "placeholder_maptiler_key") {
                    val demUrl = "https://api.maptiler.com/tiles/terrain-rgb/tiles.json?key=$key"
                    val demSource = com.mapbox.mapboxsdk.style.sources.RasterDemSource(hillshadeSourceId, demUrl)
                    style.addSource(demSource)

                    val hillshadeLayer = com.mapbox.mapboxsdk.style.layers.HillshadeLayer("summit-hillshade-layer", hillshadeSourceId)
                    hillshadeLayer.setProperties(
                        PropertyFactory.hillshadeShadowColor("#0B0F19"),
                        PropertyFactory.hillshadeHighlightColor("#FFFFFF"),
                        PropertyFactory.hillshadeIlluminationAnchor(Property.HILLSHADE_ILLUMINATION_ANCHOR_MAP),
                        PropertyFactory.hillshadeExaggeration(0.85f)
                    )
                    
                    if (style.getLayer("summit-recorded-route-casing") != null) {
                        style.addLayerBelow(hillshadeLayer, "summit-recorded-route-casing")
                    } else {
                        style.addLayer(hillshadeLayer)
                    }
                }
            }
        } else {
            style.removeLayer("summit-hillshade-layer")
            style.removeSource("maptiler-hillshade-source")
        }
    } catch (e: Exception) {
        android.util.Log.e("SummitOutdoorMap", "Error updating route and layers: ${e.message}", e)
    }
}
