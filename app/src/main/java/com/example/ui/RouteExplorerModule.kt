package com.example.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// ============================================================================
// GPX OFFLINE PARSER
// ============================================================================
object GpxParser {
    data class ParsedGpx(
        val name: String,
        val notes: String,
        val activityType: String,
        val points: List<GPSPoint>
    )

    fun parse(inputStream: InputStream, defaultName: String = "Imported Route"): ParsedGpx {
        val points = mutableListOf<GPSPoint>()
        var name = ""
        var notes = ""
        var activityType = "hike"

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var currentTag = ""
        var currentLat: Double? = null
        var currentLon: Double? = null
        var currentEle = 0.0
        var currentHasEle = false
        var currentTimeMs = 0L

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    when (currentTag.lowercase()) {
                        "trkpt" -> {
                            val latAttr = parser.getAttributeValue(null, "lat") ?: parser.getAttributeValue("", "lat")
                            val lonAttr = parser.getAttributeValue(null, "lon") ?: parser.getAttributeValue("", "lon")
                            currentLat = latAttr?.toDoubleOrNull()
                            currentLon = lonAttr?.toDoubleOrNull()
                            currentEle = 0.0
                            currentHasEle = false
                            currentTimeMs = 0L
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        when (currentTag.lowercase()) {
                            "name" -> {
                                if (name.isEmpty()) {
                                    name = text
                                }
                            }
                            "desc" -> {
                                if (notes.isEmpty()) {
                                    notes = text
                                }
                            }
                            "type" -> {
                                val t = text.lowercase()
                                activityType = when {
                                    t.contains("run") -> "run"
                                    t.contains("ride") || t.contains("bike") || t.contains("cycl") -> "ride"
                                    t.contains("walk") -> "walk"
                                    t.contains("hike") -> "hike"
                                    else -> "hike"
                                }
                            }
                            "ele" -> {
                                currentEle = text.toDoubleOrNull() ?: 0.0
                                currentHasEle = text.toDoubleOrNull() != null
                            }
                            "time" -> {
                                try {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                                    currentTimeMs = sdf.parse(text)?.time ?: 0L
                                } catch (e: Exception) {
                                    // Ignore timestamp parsing error
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val endTag = parser.name
                    when (endTag.lowercase()) {
                        "trkpt" -> {
                            if (currentLat != null && currentLon != null) {
                                points.add(
                                    GPSPoint(
                                        lat = currentLat,
                                        lng = currentLon,
                                        elevation = currentEle,
                                        timeMs = currentTimeMs,
                                        speedMps = 0.0,
                                        hasElevation = currentHasEle
                                    )
                                )
                            }
                            currentLat = null
                            currentLon = null
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        if (name.isEmpty()) {
            name = defaultName
        }

        return ParsedGpx(
            name = name,
            notes = notes,
            activityType = activityType,
            points = points
        )
    }
}

// ============================================================================
// GPX OFFLINE EXPORTER
// ============================================================================
object RouteExporter {
    fun exportToGpx(context: Context, route: Route) {
        try {
            val points = JsonHelper.jsonToPoints(route.routePointsJson)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val gpxBuilder = StringBuilder()
            gpxBuilder.append("""<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="SummitApp" xmlns="http://www.topografix.com/GPX/1/1">
  <metadata>
    <name>${escapeXml(route.name)}</name>
    <desc>${escapeXml(route.notes)}</desc>
    <time>${sdf.format(Date(route.dateCreated))}</time>
  </metadata>
  <trk>
    <name>${escapeXml(route.name)}</name>
    <type>${route.activityType.uppercase()}</type>
    <trkseg>
""")

            var currentSimTime = route.dateCreated
            for (p in points) {
                val ptTime = if (p.timeMs > 0) p.timeMs else currentSimTime
                gpxBuilder.append("""      <trkpt lat="${p.lat}" lon="${p.lng}">
        <ele>${p.elevation}</ele>
        <time>${sdf.format(Date(ptTime))}</time>
      </trkpt>
""")
                currentSimTime += 2000L
            }

            gpxBuilder.append("""    </trkseg>
  </trk>
</gpx>""")

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedTitle = route.name.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
            val file = File(exportDir, "route_${sanitizedTitle}.gpx")
            FileOutputStream(file).use {
                it.write(gpxBuilder.toString().toByteArray())
            }

            shareFile(context, file, "application/gpx+xml", "Export GPX Route")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, chooserTitle).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

// ============================================================================
// HELPER FOR READING FILE NAME
// ============================================================================
private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

// ============================================================================
// DEMO ROUTE GENERATORS
// ============================================================================
fun generateDemoRoutePoints(type: String): List<GPSPoint> {
    val points = mutableListOf<GPSPoint>()
    val (baseLat, baseLng) = when (type) {
        "hike" -> Pair(37.7456, -119.5332)   // Yosemite Valley
        "ride" -> Pair(37.7544, -122.4477)   // Twin Peaks
        else -> Pair(37.8085, -122.4762)     // Golden Gate coastal
    }
    val count = 24
    var currentEle = when (type) {
        "hike" -> 1200.0
        "ride" -> 180.0
        else -> 12.0
    }
    for (i in 0 until count) {
        val angle = (2 * Math.PI * i) / count
        val offsetLat = 0.012 * sin(angle)
        val offsetLng = 0.012 * cos(angle) * (1 + 0.25 * sin(angle * 2))
        
        currentEle += if (i < count / 2) 18.0 else -14.0
        points.add(
            GPSPoint(
                lat = baseLat + offsetLat,
                lng = baseLng + offsetLng,
                elevation = currentEle,
                timeMs = System.currentTimeMillis() + i * 15000L,
                speedMps = 0.0
            )
        )
    }
    return points
}

fun insertDemoRoutes(viewModel: SummitViewModel) {
    // 1. Yosemite Half Dome
    val p1 = generateDemoRoutePoints("hike")
    val r1 = Route(
        name = "Half Dome Wilderness Trail",
        activityType = "hike",
        routePointsJson = JsonHelper.pointsToJson(p1),
        distanceKm = 14.8,
        durationSeconds = 19800L, // 5.5 hours
        elevationGainM = 850.0,
        difficulty = "Hard",
        notes = "Stunning hike through Yosemite Valley to Half Dome. Bring plenty of water and gloves for cables.",
        isFavorite = true,
        dateCreated = System.currentTimeMillis() - 86400000L * 3
    )
    viewModel.insertRoute(r1)

    // 2. Twin Peaks Climb
    val p2 = generateDemoRoutePoints("ride")
    val r2 = Route(
        name = "Twin Peaks Crest Ride",
        activityType = "ride",
        routePointsJson = JsonHelper.pointsToJson(p2),
        distanceKm = 6.2,
        durationSeconds = 1320L, // 22 mins
        elevationGainM = 240.0,
        difficulty = "Moderate",
        notes = "Quick, intense cycling climb to Twin Peaks summit. Spectacular 360-degree views of SF.",
        isFavorite = false,
        dateCreated = System.currentTimeMillis() - 86400000L * 2
    )
    viewModel.insertRoute(r2)

    // 3. Golden Gate Coastal
    val p3 = generateDemoRoutePoints("run")
    val r3 = Route(
        name = "Presidio Coastal Run",
        activityType = "run",
        routePointsJson = JsonHelper.pointsToJson(p3),
        distanceKm = 5.5,
        durationSeconds = 1980L, // 33 mins
        elevationGainM = 75.0,
        difficulty = "Easy",
        notes = "Gentle trail running route along the coastal bluffs, overlooking the Golden Gate Bridge.",
        isFavorite = false,
        dateCreated = System.currentTimeMillis() - 86400000L
    )
    viewModel.insertRoute(r3)
}

// ============================================================================
// MAIN ROUTE EXPLORER SCREEN
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteExplorerScreen(viewModel: SummitViewModel) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val useImperial by viewModel.useImperial.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSportFilter by remember { mutableStateOf("all") } // "all", "run", "ride", "hike", "walk"
    var selectedDifficultyFilter by remember { mutableStateOf("all") } // "all", "Easy", "Moderate", "Hard", "Expert"
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("date") } // "date", "name", "distance", "elevation"

    var selectedRouteForDetails by remember { mutableStateOf<Route?>(null) }
    var routeToRename by remember { mutableStateOf<Route?>(null) }
    var newRenameName by remember { mutableStateOf("") }
    var routeToDelete by remember { mutableStateOf<Route?>(null) }

    val context = LocalContext.current

    // GPX Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val fileName = getFileName(context, uri) ?: "imported_route.gpx"
                    val parsed = GpxParser.parse(inputStream, defaultName = fileName.removeSuffix(".gpx"))
                    
                    if (parsed.points.isEmpty()) {
                        viewModel.triggerNotification("Import Failed ❌", "The GPX file contains no valid route coordinates.", "❌")
                        return@use
                    }

                    // Check for duplicate coordinates and name
                    val isDuplicate = routes.any { existing ->
                        existing.name.lowercase() == parsed.name.lowercase() &&
                        JsonHelper.jsonToPoints(existing.routePointsJson).size == parsed.points.size
                    }

                    if (isDuplicate) {
                        viewModel.triggerNotification("Duplicate Detected ⚠️", "An identical route with this name is already imported.", "⚠️")
                    } else {
                        val routePointsJson = JsonHelper.pointsToJson(parsed.points)
                        
                        // Estimate metrics
                        var totalDistM = 0.0
                        var totalElevGain = 0.0
                        for (i in 0 until parsed.points.size - 1) {
                            totalDistM += SegmentMatcher.haversineM(parsed.points[i].latlng, parsed.points[i+1].latlng)
                            val diff = parsed.points[i+1].elevation - parsed.points[i].elevation
                            if (diff > 0.0) totalElevGain += diff
                        }
                        val distanceKm = totalDistM / 1000.0

                        val durationSecs = if (parsed.points.firstOrNull()?.timeMs != null && 
                                               parsed.points.lastOrNull()?.timeMs != null && 
                                               parsed.points.last().timeMs > parsed.points.first().timeMs) {
                            (parsed.points.last().timeMs - parsed.points.first().timeMs) / 1000L
                        } else {
                            val speedKmh = when (parsed.activityType) {
                                "ride" -> 15.0
                                "run" -> 10.0
                                "walk" -> 5.0
                                else -> 4.0 // hike
                            }
                            ((distanceKm / speedKmh) * 3600.0).toLong()
                        }

                        val difficulty = when {
                            distanceKm <= 5.0 && totalElevGain <= 150.0 -> "Easy"
                            distanceKm <= 12.0 && totalElevGain <= 400.0 -> "Moderate"
                            distanceKm <= 22.0 && totalElevGain <= 900.0 -> "Hard"
                            else -> "Expert"
                        }

                        val route = Route(
                            name = parsed.name,
                            activityType = parsed.activityType,
                            routePointsJson = routePointsJson,
                            distanceKm = distanceKm,
                            durationSeconds = durationSecs,
                            elevationGainM = totalElevGain,
                            difficulty = difficulty,
                            notes = parsed.notes,
                            isFavorite = false,
                            dateCreated = System.currentTimeMillis()
                        )
                        viewModel.insertRoute(route)
                        viewModel.triggerNotification("Route Imported 🎉", "Successfully saved '${parsed.name}'!", "🎉")
                    }
                }
            } catch (e: Exception) {
                viewModel.triggerNotification("Import Error ❌", "Invalid or corrupted GPX: ${e.localizedMessage}", "❌")
            }
        }
    }

    // Filter, Sort, Search computations
    val filteredRoutes = remember(routes, searchQuery, selectedSportFilter, selectedDifficultyFilter, showOnlyFavorites, sortBy) {
        routes.filter { route ->
            val matchesSearch = route.name.lowercase().contains(searchQuery.lowercase()) || 
                                route.notes.lowercase().contains(searchQuery.lowercase())
            val matchesSport = selectedSportFilter == "all" || route.activityType.lowercase() == selectedSportFilter.lowercase()
            val matchesDifficulty = selectedDifficultyFilter == "all" || route.difficulty.lowercase() == selectedDifficultyFilter.lowercase()
            val matchesFavorite = !showOnlyFavorites || route.isFavorite
            matchesSearch && matchesSport && matchesDifficulty && matchesFavorite
        }.sortedWith { r1, r2 ->
            when (sortBy) {
                "name" -> r1.name.compareTo(r2.name, ignoreCase = true)
                "distance" -> r2.distanceKm.compareTo(r1.distanceKm) // Descending
                "elevation" -> r2.elevationGainM.compareTo(r1.elevationGainM) // Descending
                else -> r2.dateCreated.compareTo(r1.dateCreated) // Default Newest First
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        // Top Header Row with Import button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Offline Route Explorer",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = SlateTextPrimary
                )
                Text(
                    text = "Import, preview, and organize hiking & cycling routes.",
                    fontSize = 12.sp,
                    color = SlateTextSecondary
                )
            }
            
            IconButton(
                onClick = { filePickerLauncher.launch("application/gpx+xml") },
                modifier = Modifier
                    .size(44.dp)
                    .background(OrangePrimary.copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, OrangePrimary, CircleShape)
                    .testTag("import_gpx_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Import GPX Route",
                    tint = OrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search routes...", color = SlateTextSecondary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Search", tint = SlateTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SlateTextPrimary,
                unfocusedTextColor = SlateTextPrimary,
                focusedBorderColor = OrangePrimary,
                unfocusedBorderColor = SlateCardSurfaceVariant,
                focusedContainerColor = SlateCardSurface,
                unfocusedContainerColor = SlateCardSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("route_search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Sport Filtering & Favorites Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorite toggle
            FilterChip(
                selected = showOnlyFavorites,
                onClick = { showOnlyFavorites = !showOnlyFavorites },
                label = { Text("Favorites ❤️", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0x30E91E63),
                    selectedLabelColor = Color(0xFFEF4444),
                    containerColor = SlateCardSurface,
                    labelColor = SlateTextSecondary
                ),
                border = BorderStroke(1.dp, if (showOnlyFavorites) Color(0xFFEF4444) else SlateCardSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            )

            // All
            FilterChip(
                selected = selectedSportFilter == "all",
                onClick = { selectedSportFilter = "all" },
                label = { Text("All Sports", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangePrimary.copy(alpha = 0.2f),
                    selectedLabelColor = OrangePrimary,
                    containerColor = SlateCardSurface,
                    labelColor = SlateTextSecondary
                ),
                border = BorderStroke(1.dp, if (selectedSportFilter == "all") OrangePrimary else SlateCardSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            )

            // Hike
            FilterChip(
                selected = selectedSportFilter == "hike",
                onClick = { selectedSportFilter = "hike" },
                label = { Text("Hike 🥾", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HikeColor.copy(alpha = 0.2f),
                    selectedLabelColor = HikeColor,
                    containerColor = SlateCardSurface,
                    labelColor = SlateTextSecondary
                ),
                border = BorderStroke(1.dp, if (selectedSportFilter == "hike") HikeColor else SlateCardSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            )

            // Ride
            FilterChip(
                selected = selectedSportFilter == "ride",
                onClick = { selectedSportFilter = "ride" },
                label = { Text("Ride 🚴", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RideColor.copy(alpha = 0.2f),
                    selectedLabelColor = RideColor,
                    containerColor = SlateCardSurface,
                    labelColor = SlateTextSecondary
                ),
                border = BorderStroke(1.dp, if (selectedSportFilter == "ride") RideColor else SlateCardSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            )

            // Run
            FilterChip(
                selected = selectedSportFilter == "run",
                onClick = { selectedSportFilter = "run" },
                label = { Text("Run 🏃", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RunColor.copy(alpha = 0.2f),
                    selectedLabelColor = RunColor,
                    containerColor = SlateCardSurface,
                    labelColor = SlateTextSecondary
                ),
                border = BorderStroke(1.dp, if (selectedSportFilter == "run") RunColor else SlateCardSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            )

            // Walk
            FilterChip(
                selected = selectedSportFilter == "walk",
                onClick = { selectedSportFilter = "walk" },
                label = { Text("Walk 🚶", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WalkColor.copy(alpha = 0.2f),
                    selectedLabelColor = WalkColor,
                    containerColor = SlateCardSurface,
                    labelColor = SlateTextSecondary
                ),
                border = BorderStroke(1.dp, if (selectedSportFilter == "walk") WalkColor else SlateCardSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Sort & Difficulty Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Difficulty filter selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Difficulty: ", fontSize = 11.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                listOf("all", "Easy", "Moderate", "Hard").forEach { diff ->
                    val selected = selectedDifficultyFilter == diff
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) OrangePrimary.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (selected) OrangePrimary else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { selectedDifficultyFilter = diff }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = diff.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (selected) OrangePrimary else SlateTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            // Sort indicator selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sort: ", fontSize = 11.sp, color = SlateTextSecondary)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            sortBy = when (sortBy) {
                                "date" -> "name"
                                "name" -> "distance"
                                "distance" -> "elevation"
                                else -> "date"
                            }
                        }
                        .background(SlateCardSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (sortBy) {
                            "name" -> "NAME"
                            "distance" -> "DISTANCE"
                            "elevation" -> "ELEVATION"
                            else -> "DATE"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Empty State vs Route List
        if (filteredRoutes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = SlateTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (routes.isEmpty()) "No Routes Saved Yet" else "No matching routes found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (routes.isEmpty()) 
                            "Get started by loading default SF & Yosemite trails, or import any GPX route file offline." 
                            else "Try expanding your search query or loosening your sport filters.",
                        fontSize = 12.sp,
                        color = SlateTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    if (routes.isEmpty()) {
                        Button(
                            onClick = { insertDemoRoutes(viewModel) },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("LOAD DEMO TRAILS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredRoutes, key = { it.id }) { route ->
                    var showDropdownMenu by remember { mutableStateOf(false) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(16.dp))
                            .clickable { selectedRouteForDetails = route }
                            .testTag("route_card_${route.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Type, Name, Options dots, Favorite Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (sportIcon, sportColor) = when (route.activityType.lowercase()) {
                                    "run" -> Pair(Icons.Default.DirectionsRun, RunColor)
                                    "ride" -> Pair(Icons.Default.DirectionsBike, RideColor)
                                    "walk" -> Pair(Icons.Default.DirectionsWalk, WalkColor)
                                    else -> Pair(Icons.Default.Terrain, HikeColor) // hike
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(sportColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = sportIcon,
                                        contentDescription = route.activityType,
                                        tint = sportColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = route.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = SlateTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "Created ${SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(route.dateCreated))}",
                                        fontSize = 10.sp,
                                        color = SlateTextSecondary
                                    )
                                }

                                // Favorite Toggle Button
                                IconButton(
                                    onClick = { viewModel.toggleRouteFavorite(route) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (route.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Toggle Favorite",
                                        tint = if (route.isFavorite) Color(0xFFEF4444) else SlateTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Options menu trigger
                                Box {
                                    IconButton(
                                        onClick = { showDropdownMenu = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = SlateTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showDropdownMenu,
                                        onDismissRequest = { showDropdownMenu = false },
                                        modifier = Modifier.background(SlateCardSurface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Rename", color = SlateTextPrimary, fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                showDropdownMenu = false
                                                newRenameName = route.name
                                                routeToRename = route
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Duplicate", color = SlateTextPrimary, fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                showDropdownMenu = false
                                                viewModel.duplicateRoute(route)
                                                viewModel.triggerNotification("Route Duplicated", "Successfully duplicated '${route.name}'!", "🎉")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Export GPX", color = SlateTextPrimary, fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                showDropdownMenu = false
                                                RouteExporter.exportToGpx(context, route)
                                            }
                                        )
                                        HorizontalDivider(color = SlateCardSurfaceVariant)
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = Color(0xFFEF4444), fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) },
                                            onClick = {
                                                showDropdownMenu = false
                                                routeToDelete = route
                                            }
                                        )
                                    }
                                }
                            }

                            // Optional Notes Preview
                            if (route.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = route.notes,
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Grid Metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Distance
                                val distanceStr = if (useImperial) {
                                    String.format(Locale.US, "%.1f mi", route.distanceKm * 0.621371)
                                } else {
                                    String.format(Locale.US, "%.1f km", route.distanceKm)
                                }
                                Column {
                                    Text("DISTANCE", fontSize = 8.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                    Text(distanceStr, fontSize = 13.sp, color = SlateTextPrimary, fontWeight = FontWeight.Black)
                                }

                                // Duration
                                val h = route.durationSeconds / 3600
                                val m = (route.durationSeconds % 3600) / 60
                                val durationStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                                Column {
                                    Text("EST. TIME", fontSize = 8.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                    Text(durationStr, fontSize = 13.sp, color = SlateTextPrimary, fontWeight = FontWeight.Black)
                                }

                                // Elevation Gain
                                val elevUnit = if (useImperial) "ft" else "m"
                                val elevVal = if (useImperial) route.elevationGainM * 3.28084 else route.elevationGainM
                                val elevGainStr = String.format(Locale.US, "+%.0f %s", elevVal, elevUnit)
                                Column {
                                    Text("ELEV GAIN", fontSize = 8.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                    Text(elevGainStr, fontSize = 13.sp, color = SlateTextPrimary, fontWeight = FontWeight.Black)
                                }

                                // Difficulty Badge
                                val diffColor = when (route.difficulty) {
                                    "Easy" -> Color(0xFF4CAF50)
                                    "Moderate" -> Color(0xFFFFB300)
                                    "Hard" -> Color(0xFFEF4444)
                                    else -> Color(0xFF9C27B0) // Expert
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(diffColor.copy(alpha = 0.15f))
                                        .border(1.dp, diffColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = route.difficulty.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = diffColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (routeToRename != null) {
        AlertDialog(
            onDismissRequest = { routeToRename = null },
            title = { Text("Rename Route", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = newRenameName,
                    onValueChange = { newRenameName = it },
                    label = { Text("Route Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SlateTextPrimary,
                        unfocusedTextColor = SlateTextPrimary,
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = SlateCardSurfaceVariant,
                        focusedContainerColor = SlateCardSurface,
                        unfocusedContainerColor = SlateCardSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val route = routeToRename ?: return@TextButton
                        if (newRenameName.isNotBlank()) {
                            viewModel.renameRoute(route, newRenameName.trim())
                            viewModel.triggerNotification("Route Renamed", "Renamed to '${newRenameName}'", "📝")
                        }
                        routeToRename = null
                    }
                ) {
                    Text("SAVE", color = OrangePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { routeToRename = null }) {
                    Text("CANCEL", color = SlateTextSecondary)
                }
            },
            containerColor = SlateCardSurface
        )
    }

    // Delete Confirmation Dialog
    if (routeToDelete != null) {
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            title = { Text("Delete Route?", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Are you sure you want to permanently delete '${routeToDelete?.name}' from your offline library? This action is irreversible.", color = SlateTextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val route = routeToDelete ?: return@TextButton
                        viewModel.deleteRoute(route)
                        viewModel.triggerNotification("Route Deleted", "'${route.name}' removed", "🗑️")
                        routeToDelete = null
                    }
                ) {
                    Text("DELETE", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { routeToDelete = null }) {
                    Text("CANCEL", color = SlateTextSecondary)
                }
            },
            containerColor = SlateCardSurface
        )
    }

    // Detailed Route Dialog
    if (selectedRouteForDetails != null) {
        val route = selectedRouteForDetails!!
        RouteDetailsDialog(
            route = route,
            viewModel = viewModel,
            useImperial = useImperial,
            onDismiss = { selectedRouteForDetails = null }
        )
    }
}

// ============================================================================
// ROUTE DETAILS OVERLAY DIALOG
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailsDialog(
    route: Route,
    viewModel: SummitViewModel,
    useImperial: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var notesText by remember { mutableStateOf(route.notes) }
    var isEditingNotes by remember { mutableStateOf(false) }
    var mapStyleMode by remember { mutableStateOf(MapViewMode.OUTDOOR) }

    val points = remember(route.routePointsJson) {
        JsonHelper.jsonToPoints(route.routePointsJson)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = route.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = route.activityType.uppercase() + " ROUTE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SlateTextPrimary)
                        }
                    },
                    actions = {
                        // Favorite toggle
                        IconButton(onClick = { viewModel.toggleRouteFavorite(route) }) {
                            Icon(
                                imageVector = if (route.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (route.isFavorite) Color(0xFFEF4444) else SlateTextPrimary
                            )
                        }
                        // Export GPX
                        IconButton(onClick = { RouteExporter.exportToGpx(context, route) }) {
                            Icon(Icons.Default.Share, contentDescription = "Export Route", tint = SlateTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SlateDarkBackground,
                        titleContentColor = SlateTextPrimary,
                        actionIconContentColor = SlateTextPrimary
                    )
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlateDarkBackground)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Large OpenStreetMap Component Box (reusing PremiumOSMMapView)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .background(SlateCardSurfaceVariant)
                ) {
                    if (points.isNotEmpty()) {
                        PremiumOSMMapView(
                            points = points,
                            sportType = route.activityType,
                            viewMode = mapStyleMode,
                            onViewModeChange = { mapStyleMode = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No Map Coordinates Available", color = SlateTextSecondary)
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // Title section for stats
                    Text(
                        text = "ROUTE ANALYTICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = SlateTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Stats grid
                    val elevUnit = if (useImperial) "ft" else "m"
                    val elevVal = if (useImperial) route.elevationGainM * 3.28084 else route.elevationGainM
                    val distanceStr = if (useImperial) {
                        String.format(Locale.US, "%.1f mi", route.distanceKm * 0.621371)
                    } else {
                        String.format(Locale.US, "%.1f km", route.distanceKm)
                    }
                    val h = route.durationSeconds / 3600
                    val m = (route.durationSeconds % 3600) / 60
                    val durationStr = if (h > 0) "${h}h ${m}m" else "${m}m"

                    val stats = listOf(
                        Triple("DISTANCE", distanceStr, Icons.Default.Straighten),
                        Triple("EST. DURATION", durationStr, Icons.Default.Timer),
                        Triple("ELEVATION GAIN", String.format(Locale.US, "+%.0f %s", elevVal, elevUnit), Icons.Default.FilterHdr),
                        Triple("DIFFICULTY", route.difficulty, Icons.Default.Speed)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in stats.indices step 2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                stats.getOrNull(i)?.let { stat ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(stat.third, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(stat.first, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                                                if (stat.first == "DIFFICULTY") {
                                                    val diffColor = when (stat.second) {
                                                        "Easy" -> Color(0xFF4CAF50)
                                                        "Moderate" -> Color(0xFFFFB300)
                                                        "Hard" -> Color(0xFFEF4444)
                                                        else -> Color(0xFF9C27B0)
                                                    }
                                                    Text(stat.second, fontSize = 14.sp, fontWeight = FontWeight.Black, color = diffColor)
                                                } else {
                                                    Text(stat.second, fontSize = 14.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                                                }
                                            }
                                        }
                                    }
                                }
                                stats.getOrNull(i + 1)?.let { stat ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(stat.third, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(stat.first, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                                                if (stat.first == "DIFFICULTY") {
                                                    val diffColor = when (stat.second) {
                                                        "Easy" -> Color(0xFF4CAF50)
                                                        "Moderate" -> Color(0xFFFFB300)
                                                        "Hard" -> Color(0xFFEF4444)
                                                        else -> Color(0xFF9C27B0)
                                                    }
                                                    Text(stat.second, fontSize = 14.sp, fontWeight = FontWeight.Black, color = diffColor)
                                                } else {
                                                    Text(stat.second, fontSize = 14.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Notes section (editable!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OFFLINE NOTES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = SlateTextSecondary,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = {
                                if (isEditingNotes) {
                                    // Save
                                    viewModel.updateRoute(route.copy(notes = notesText.trim()))
                                    viewModel.triggerNotification("Notes Saved 📝", "Successfully updated route notes offline.", "📝")
                                }
                                isEditingNotes = !isEditingNotes
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditingNotes) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditingNotes) "Save Notes" else "Edit Notes",
                                tint = OrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isEditingNotes) {
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            placeholder = { Text("Write personal notes about water stops, trail conditions, or difficulty...", color = SlateTextSecondary, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SlateTextPrimary,
                                unfocusedTextColor = SlateTextPrimary,
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = SlateCardSurfaceVariant,
                                focusedContainerColor = SlateCardSurface,
                                unfocusedContainerColor = SlateCardSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp)
                        )
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = if (notesText.isNotBlank()) notesText else "No notes added yet. Tap the edit icon to add private notes about trailheads, gear, and milestones.",
                                fontSize = 12.sp,
                                color = if (notesText.isNotBlank()) SlateTextPrimary else SlateTextSecondary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Date created
                    Text(
                        text = "Saved offline on ${SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.US).format(Date(route.dateCreated))}.",
                        fontSize = 11.sp,
                        color = SlateTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
