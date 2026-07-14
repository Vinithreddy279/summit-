package com.example.ui

import android.content.Context
import android.net.Uri
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetTargetHikeScreen(viewModel: SummitViewModel) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var distanceStr by remember { mutableStateOf("") }
    var elevationGainStr by remember { mutableStateOf("") }
    var durationHoursStr by remember { mutableStateOf("") }
    var durationMinsStr by remember { mutableStateOf("") }

    // Validation Errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var distanceError by remember { mutableStateOf<String?>(null) }
    var elevationError by remember { mutableStateOf<String?>(null) }
    var durationError by remember { mutableStateOf<String?>(null) }

    // GPX Picker Launcher
    val gpxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val rawName = getFileName(context, uri) ?: "Imported Route"
                    val displayName = rawName.removeSuffix(".gpx")
                    val parsed = GpxParser.parse(inputStream, defaultName = displayName)

                    if (parsed.points.isEmpty()) {
                        viewModel.triggerNotification("Empty GPX ❌", "The GPX file does not contain any valid track coordinates.", "❌")
                        return@use
                    }

                    // Calculate metrics using SegmentMatcher.haversineM and TargetHikeLogic
                    var totalDistM = 0.0
                    for (i in 0 until parsed.points.size - 1) {
                        totalDistM += SegmentMatcher.haversineM(parsed.points[i].latlng, parsed.points[i+1].latlng)
                    }

                    val totalElevGain = TargetHikeLogic.calculateElevationGain(parsed.points)
                    val maxElevation = TargetHikeLogic.calculateMaxElevation(parsed.points)
                    val minElevation = TargetHikeLogic.calculateMinElevation(parsed.points)

                    val durationMinutes = if (parsed.points.firstOrNull()?.timeMs != null &&
                        parsed.points.lastOrNull()?.timeMs != null &&
                        parsed.points.last().timeMs > parsed.points.first().timeMs
                    ) {
                        ((parsed.points.last().timeMs - parsed.points.first().timeMs) / 1000L / 60L).toInt()
                    } else {
                        TargetHikeLogic.estimateDurationMinutes(totalDistM, totalElevGain)
                    }

                    val hasElevationData = parsed.points.any { it.hasElevation }

                    val targetHike = TargetHike(
                        name = parsed.name.ifBlank { displayName },
                        distanceMeters = totalDistM,
                        elevationGainMeters = totalElevGain,
                        estimatedDurationMinutes = durationMinutes,
                        maxElevationMeters = maxElevation,
                        minElevationMeters = minElevation,
                        gpxPath = null, // Set to null as temporary Android URIs are not durable
                        status = "ACTIVE",
                        hasElevationData = hasElevationData
                    )

                    viewModel.setTargetToReview(targetHike)
                    viewModel.triggerNotification("GPX Parsed 🚀", "Successfully parsed route metrics!", "🚀")
                }
            } catch (e: Exception) {
                viewModel.triggerNotification("Import Error ❌", "Could not parse GPX: ${e.localizedMessage}", "❌")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Set Your Target",
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setShowSetTargetScreen(false) },
                        modifier = Modifier.testTag("cancel_target_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = OrangePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDarkBackground
                )
            )
        },
        containerColor = SlateDarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "What hike do you want to prepare for?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateTextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            // Mode A: Import GPX Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { gpxLauncher.launch("*/*") }
                    .testTag("import_gpx_button"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Import GPX icon",
                            tint = OrangePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "IMPORT GPX ROUTE FILE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = OrangePrimary,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "Upload a GPS route file to automatically analyze distance, elevation, and climb demands.",
                        fontSize = 12.sp,
                        color = SlateTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Elegant "OR" Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = SlateTextSecondary.copy(alpha = 0.2f)
                )
                Text(
                    text = "OR ENTER MANUALLY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    letterSpacing = 1.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = SlateTextSecondary.copy(alpha = 0.2f)
                )
            }

            // Mode B: Manual Entry Fields
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                border = BorderStroke(1.dp, SlateTextSecondary.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hike Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        label = { Text("Hike Name") },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hike_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateTextSecondary.copy(alpha = 0.3f),
                            focusedLabelColor = OrangePrimary,
                            unfocusedLabelColor = SlateTextSecondary
                        )
                    )

                    // Distance Input
                    OutlinedTextField(
                        value = distanceStr,
                        onValueChange = {
                            distanceStr = it
                            distanceError = null
                        },
                        label = { Text("Distance (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = distanceError != null,
                        supportingText = distanceError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("distance_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateTextSecondary.copy(alpha = 0.3f),
                            focusedLabelColor = OrangePrimary,
                            unfocusedLabelColor = SlateTextSecondary
                        )
                    )

                    // Elevation Gain Input
                    OutlinedTextField(
                        value = elevationGainStr,
                        onValueChange = {
                            elevationGainStr = it
                            elevationError = null
                        },
                        label = { Text("Elevation Gain (meters)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = elevationError != null,
                        supportingText = elevationError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("elevation_gain_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateTextSecondary.copy(alpha = 0.3f),
                            focusedLabelColor = OrangePrimary,
                            unfocusedLabelColor = SlateTextSecondary
                        )
                    )

                    // Duration Row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Estimated Duration",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = durationHoursStr,
                                onValueChange = {
                                    durationHoursStr = it
                                    durationError = null
                                },
                                label = { Text("Hours") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("duration_hours_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = SlateTextSecondary.copy(alpha = 0.3f),
                                    focusedLabelColor = OrangePrimary,
                                    unfocusedLabelColor = SlateTextSecondary
                                )
                            )

                            OutlinedTextField(
                                value = durationMinsStr,
                                onValueChange = {
                                    durationMinsStr = it
                                    durationError = null
                                },
                                label = { Text("Minutes") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("duration_minutes_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = SlateTextSecondary.copy(alpha = 0.3f),
                                    focusedLabelColor = OrangePrimary,
                                    unfocusedLabelColor = SlateTextSecondary
                                )
                            )
                        }
                        if (durationError != null) {
                            Text(
                                text = durationError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Review Manual Target button
            Button(
                onClick = {
                    var hasError = false

                    val cleanedName = name.trim()
                    if (cleanedName.isEmpty()) {
                        nameError = "Hike name is required"
                        hasError = true
                    }

                    val dist = distanceStr.toDoubleOrNull()
                    if (dist == null || dist <= 0.0 || dist.isNaN() || dist.isInfinite()) {
                        distanceError = "Enter a valid distance greater than 0"
                        hasError = true
                    }

                    val elev = elevationGainStr.toDoubleOrNull()
                    if (elev == null || elev < 0.0 || elev.isNaN() || elev.isInfinite()) {
                        elevationError = "Enter an elevation gain of 0 or greater"
                        hasError = true
                    }

                    val hours = durationHoursStr.toIntOrNull() ?: 0
                    val mins = durationMinsStr.toIntOrNull() ?: 0
                    val totalDurationMins = (hours * 60) + mins
                    if (totalDurationMins <= 0) {
                        durationError = "Total duration must be greater than 0 minutes"
                        hasError = true
                    }

                    if (!hasError) {
                        val manualTarget = TargetHike(
                            name = cleanedName,
                            distanceMeters = dist!! * 1000.0,
                            elevationGainMeters = elev!!,
                            estimatedDurationMinutes = totalDurationMins,
                            maxElevationMeters = null, // unknown
                            minElevationMeters = null, // unknown
                            gpxPath = null,
                            status = "ACTIVE",
                            hasElevationData = true
                        )
                        viewModel.setTargetToReview(manualTarget)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("review_target_button")
            ) {
                Text(
                    text = "REVIEW TARGET",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetReviewScreen(viewModel: SummitViewModel) {
    val target = viewModel.targetToReview.collectAsStateWithLifecycle().value ?: return

    val distanceKm = target.distanceMeters / 1000.0
    val elevationGain = target.elevationGainMeters
    val durationMins = target.estimatedDurationMinutes
    val hours = durationMins / 60
    val mins = durationMins % 60

    val maxElevation = target.maxElevationMeters
    val highestPointStr = if (maxElevation == null) "Unavailable" else "${maxElevation.roundToInt()} m"

    val demand = TargetHikeLogic.classifyDemand(
        target.distanceMeters,
        target.elevationGainMeters,
        target.estimatedDurationMinutes,
        target.hasElevationData
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Target Route Analysis",
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setTargetToReview(null) },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = OrangePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDarkBackground
                )
            )
        },
        containerColor = SlateDarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Main Info Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = target.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = SlateTextPrimary
                    )

                    HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.1f))

                    // 3-Column Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Distance Column
                        Column {
                            Text("DISTANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(String.format("%.1f km", distanceKm), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        }

                        // Elev Gain Column
                        Column {
                            Text("ELEVATION GAIN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            val elevText = if (target.hasElevationData) {
                                String.format("%,d m", elevationGain.roundToInt())
                            } else {
                                "Unavailable"
                            }
                            Text(elevText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                        }

                        // Estimated Duration Column
                        Column {
                            Text("V1 DURATION ESTIMATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            val durationText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                            Text(durationText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        }
                    }

                    HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.1f))

                    // Highest point row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terrain,
                                contentDescription = "Highest Point",
                                tint = SlateTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "HIGHEST POINT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlateTextSecondary
                            )
                        }
                        Text(
                            text = highestPointStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                    }
                }
            }

            // Route Demand Card
            Text(
                text = "ROUTE DEMAND CLASSIFICATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                border = BorderStroke(1.dp, SlateTextSecondary.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DemandRow(
                        title = "DISTANCE DEMAND",
                        level = demand.distanceDemand,
                        description = "Based on total trail length of ${String.format("%.1f km", distanceKm)}."
                    )

                    HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.05f))

                    DemandRow(
                        title = "CLIMBING DEMAND",
                        level = demand.climbingDemand,
                        description = if (target.hasElevationData) {
                            "Based on total vertical climb of ${String.format("%,d m", elevationGain.roundToInt())}."
                        } else {
                            "Elevation data is not available for this route."
                        }
                    )

                    HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.05f))

                    DemandRow(
                        title = "ENDURANCE DEMAND",
                        level = demand.enduranceDemand,
                        description = "Based on estimated total exertion duration."
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.setTargetToReview(null) },
                    border = BorderStroke(1.dp, SlateTextSecondary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "CANCEL",
                        fontWeight = FontWeight.Bold,
                        color = SlateTextSecondary
                    )
                }

                Button(
                    onClick = {
                        viewModel.saveActiveTarget(target)
                        viewModel.setShowSetTargetScreen(false)
                        viewModel.setTargetToReview(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                        .testTag("set_as_my_target_button")
                ) {
                    Text(
                        text = "SET AS MY TARGET",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun DemandRow(
    title: String,
    level: RouteDemandLevel,
    description: String
) {
    val levelColor = when (level) {
        RouteDemandLevel.LOW -> Color(0xFF10B981) // Green
        RouteDemandLevel.MODERATE -> Color(0xFFFFC857) // Yellow-Gold
        RouteDemandLevel.HIGH -> Color(0xFFFF6A00) // Orange
        RouteDemandLevel.VERY_HIGH -> Color(0xFFEF4444) // Red
        RouteDemandLevel.UNKNOWN -> Color(0xFF94A3B8) // Slate Gray
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextSecondary
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(levelColor.copy(alpha = 0.15f))
                    .border(1.dp, levelColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = level.name,
                    color = levelColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Text(
            text = description,
            fontSize = 11.sp,
            color = SlateTextSecondary.copy(alpha = 0.8f)
        )
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadinessScreen(viewModel: SummitViewModel) {
    val activeTarget by viewModel.activeTarget.collectAsStateWithLifecycle()
    val readinessResult by viewModel.readinessResult.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Summit Readiness",
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setShowReadinessScreen(false) },
                        modifier = Modifier.testTag("readiness_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = OrangePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDarkBackground,
                    titleContentColor = SlateTextPrimary
                )
            )
        },
        containerColor = SlateDarkBackground
    ) { innerPadding ->
        val target = activeTarget
        val rr = readinessResult

        if (target == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.widthIn(max = 400.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = "No Target",
                        tint = SlateTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "No Active Target",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "You haven't set a target hike yet. Go to the dashboard, select a hike, and make it your active target to see your readiness analysis.",
                        fontSize = 14.sp,
                        color = SlateTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = {
                            viewModel.setShowReadinessScreen(false)
                            viewModel.setShowSetTargetScreen(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("set_target_from_readiness_button")
                    ) {
                        Text("Set Target Hike", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        } else if (rr == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("readiness_scroll_content"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Score Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = target.name.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangePrimary,
                            letterSpacing = 1.sp
                        )

                        // Circular Gauge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            val levelColor = getReadinessColor(rr.readinessLevel)

                            CircularProgressIndicator(
                                progress = { rr.overallScore / 100f },
                                modifier = Modifier.fillMaxSize(),
                                color = levelColor,
                                strokeWidth = 12.dp,
                                trackColor = levelColor.copy(alpha = 0.1f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${rr.overallScore}%",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SlateTextPrimary
                                )
                                Text(
                                    text = getReadinessLabel(rr.readinessLevel),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = levelColor
                                )
                            }
                        }

                        // Short description of state
                        Text(
                            text = if (rr.evidence.historyActivityCount == 0) {
                                "No hikes completed in the last 90 days. Your training baseline is empty."
                            } else {
                                "Based on ${rr.evidence.historyActivityCount} hikes completed in the last 90 days."
                            },
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Main Limiter Box (Highly visual & high contrast)
                val limiterColor = when (rr.mainLimiter) {
                    ReadinessDimension.DISTANCE -> Color(0xFFFFC857)
                    ReadinessDimension.ELEVATION -> Color(0xFFFF6A00)
                    ReadinessDimension.ENDURANCE -> Color(0xFFEF4444)
                    ReadinessDimension.RECENT_LOAD -> Color(0xFF5CC2F2)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .border(BorderStroke(1.5.dp, limiterColor.copy(alpha = 0.6f)), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Limiter Icon",
                                tint = limiterColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "MAIN LIMITER: " + when (rr.mainLimiter) {
                                    ReadinessDimension.DISTANCE -> "DISTANCE CAPACITY"
                                    ReadinessDimension.ELEVATION -> "CLIMBING ENDURANCE"
                                    ReadinessDimension.ENDURANCE -> "DURATION ENDURANCE"
                                    ReadinessDimension.RECENT_LOAD -> "RECENT CONSISTENCY"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = limiterColor,
                                letterSpacing = 0.5.sp
                            )
                        }

                        val formatMinutes: (Double) -> String = { mins ->
                            val hrs = mins.toInt() / 60
                            val m = mins.toInt() % 60
                            if (hrs > 0) "${hrs}h ${m}m" else "${m}m"
                        }

                        // Dynamic explanation based on the limiter scores and capacities
                        val explanationText = when (rr.mainLimiter) {
                            ReadinessDimension.DISTANCE -> {
                                val top3AvgDist = rr.evidence.recentDistanceCapacityMeters / 1000.0
                                val targetDist = target.distanceMeters / 1000.0
                                String.format(
                                    "Your recent distance capacity is approximately %.1f km compared with the %.1f km target distance.",
                                    top3AvgDist,
                                    targetDist
                                )
                            }
                            ReadinessDimension.ELEVATION -> {
                                val top3AvgElev = rr.evidence.recentElevationCapacityMeters ?: 0.0
                                val targetElev = target.elevationGainMeters
                                if (rr.evidence.validRecentElevationActivityCount == 0 && targetElev > 0.0) {
                                    "No valid recent elevation history is available yet."
                                } else {
                                    String.format(
                                        "Your target requires approximately %,.0f m of elevation gain. Your recent climbing capacity is approximately %,.0f m.",
                                        targetElev,
                                        top3AvgElev
                                    )
                                }
                            }
                            ReadinessDimension.ENDURANCE -> {
                                val top3AvgDur = rr.evidence.recentEnduranceCapacityMinutes
                                val targetDur = target.estimatedDurationMinutes.toDouble()
                                val targetStr = formatMinutes(targetDur)
                                val capacityStr = formatMinutes(top3AvgDur)
                                "Your estimated target duration is $targetStr. Your recent endurance capacity is approximately $capacityStr."
                            }
                            ReadinessDimension.RECENT_LOAD -> {
                                "You were active in ${rr.evidence.recentActiveWeeks} of the last 4 hiking weeks."
                            }
                        }

                        Text(
                            text = explanationText,
                            fontSize = 14.sp,
                            color = SlateTextPrimary
                        )

                        HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "MAIN FOCUS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextSecondary
                        )

                        Text(
                            text = when (rr.mainLimiter) {
                                ReadinessDimension.DISTANCE -> "Building distance capacity is your main focus."
                                ReadinessDimension.ELEVATION -> "Climbing endurance is your main focus."
                                ReadinessDimension.ENDURANCE -> "Duration endurance is your main focus."
                                ReadinessDimension.RECENT_LOAD -> "Recent consistency is your main focus."
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateTextPrimary
                        )
                    }
                }

                // 4 Dimensions Breakdown
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "DETAILED METRIC ANALYSIS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = SlateTextSecondary,
                            letterSpacing = 0.5.sp
                        )

                        // 1. Distance Capacity Row
                        ReadinessDimensionRow(
                            title = "Distance Capacity",
                            score = rr.distanceScore,
                            capacityText = String.format("Peak Avg: %.1f km", rr.evidence.recentDistanceCapacityMeters / 1000.0),
                            targetText = String.format("Target: %.1f km", target.distanceMeters / 1000.0)
                        )

                        // 2. Climbing Endurance Row (Conditional on target having elevation data)
                        if (target.hasElevationData) {
                            rr.elevationScore?.let { elevScore ->
                                ReadinessDimensionRow(
                                    title = "Climbing Endurance",
                                    score = elevScore,
                                    capacityText = if (rr.evidence.validRecentElevationActivityCount == 0 && target.elevationGainMeters > 0.0) {
                                        "Peak Avg: N/A"
                                    } else {
                                        String.format("Peak Avg: %,d m", (rr.evidence.recentElevationCapacityMeters ?: 0.0).toInt())
                                    },
                                    targetText = String.format("Target: %,d m", target.elevationGainMeters.toInt())
                                )
                            }
                        }

                        // 3. Duration Endurance Row
                        ReadinessDimensionRow(
                            title = "Duration Endurance",
                            score = rr.enduranceScore,
                            capacityText = String.format("Peak Avg: %.0f min", rr.evidence.recentEnduranceCapacityMinutes),
                            targetText = String.format("Target: %d min", target.estimatedDurationMinutes)
                        )

                        HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.1f))

                        // 4. Recent Consistency Row (Customized visualization for 4 weekly buckets)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Consistency",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                                Text(
                                    text = "${rr.recentLoadScore}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = getScoreColor(rr.recentLoadScore)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val activeWeeks = rr.evidence.activeWeeks
                                for (i in 0..3) {
                                    val isActive = activeWeeks.getOrElse(i) { false }
                                    val label = "W${i + 1}"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isActive) Color(0xFF10B981).copy(alpha = 0.15f)
                                                else SlateDarkBackground
                                            )
                                            .border(
                                                1.dp,
                                                if (isActive) Color(0xFF10B981).copy(alpha = 0.5f)
                                                else SlateTextSecondary.copy(alpha = 0.15f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isActive) Color(0xFF10B981) else SlateTextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isActive) "ACTIVE" else "IDLE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isActive) Color(0xFF10B981) else SlateTextSecondary.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.setShowReadinessScreen(false)
                        viewModel.setShowProgressionScreen(true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("view_progression_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "VIEW PROGRESSION",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReadinessDimensionRow(
    title: String,
    score: Int,
    capacityText: String,
    targetText: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )
            Text(
                text = "$score%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = getScoreColor(score)
            )
        }

        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = getScoreColor(score),
            trackColor = getScoreColor(score).copy(alpha = 0.1f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = capacityText,
                fontSize = 11.sp,
                color = SlateTextSecondary
            )
            Text(
                text = targetText,
                fontSize = 11.sp,
                color = SlateTextSecondary
            )
        }
    }
}

fun getReadinessLevelForScore(score: Int): ReadinessLevel {
    return com.example.data.ReadinessEngine.getLevelForScore(score)
}

fun getReadinessColor(level: ReadinessLevel): Color {
    return when (level) {
        ReadinessLevel.NOT_READY -> Color(0xFFEF4444)
        ReadinessLevel.BUILDING -> Color(0xFFFF6A00)
        ReadinessLevel.MODERATE -> Color(0xFFFFC857)
        ReadinessLevel.READY -> Color(0xFF10B981)
        ReadinessLevel.HIGHLY_READY -> Color(0xFF059669)
    }
}

fun getReadinessLabel(level: ReadinessLevel): String {
    return when (level) {
        ReadinessLevel.NOT_READY -> "NOT READY"
        ReadinessLevel.BUILDING -> "BUILDING"
        ReadinessLevel.MODERATE -> "MODERATE"
        ReadinessLevel.READY -> "READY"
        ReadinessLevel.HIGHLY_READY -> "HIGHLY READY"
    }
}

fun getScoreColor(score: Int): Color {
    return getReadinessColor(getReadinessLevelForScore(score))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionScreen(viewModel: SummitViewModel) {
    val activeTarget by viewModel.activeTarget.collectAsStateWithLifecycle()
    val readinessResult by viewModel.readinessResult.collectAsStateWithLifecycle()
    val progressionPlan by viewModel.progressionPlan.collectAsStateWithLifecycle()
    val activeProgressionPlan by viewModel.activeProgressionPlan.collectAsStateWithLifecycle()
    val activeProgressionSteps by viewModel.activeProgressionSteps.collectAsStateWithLifecycle()
    val activeAdaptationProposal by viewModel.activeAdaptationProposal.collectAsStateWithLifecycle()
    val readinessHistory by viewModel.readinessHistory.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Your Progression",
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setShowProgressionScreen(false) },
                        modifier = Modifier.testTag("progression_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = OrangePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDarkBackground,
                    titleContentColor = SlateTextPrimary
                )
            )
        },
        containerColor = SlateDarkBackground
    ) { innerPadding ->
        val target = activeTarget
        val rr = readinessResult
        val plan = progressionPlan
        val activePlan = activeProgressionPlan

        if (target == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.widthIn(max = 400.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = "No Target",
                        tint = SlateTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "No Active Target",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "You haven't set a target hike yet. Go to the dashboard, select a hike, and make it your active target to see your preparation progression.",
                        fontSize = 14.sp,
                        color = SlateTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = {
                            viewModel.setShowProgressionScreen(false)
                            viewModel.setShowSetTargetScreen(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("set_target_from_progression_button")
                    ) {
                        Text("Set Target Hike", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        } else if (rr == null || (plan == null && activePlan == null)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (activePlan != null) {
            // PERSISTED ACTIVE PROGRESSION JOURNEY
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Persistent Journey Active Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .testTag("active_journey_header_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ACTIVE PREPARATION JOURNEY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OrangePrimary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = target.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(OrangePrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = OrangePrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("STARTING CAPACITY", fontSize = 10.sp, color = SlateTextSecondary)
                                Text("${activePlan.startingReadinessScore}% Readiness", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = getReadinessColor(getReadinessLevelForScore(activePlan.startingReadinessScore)))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("LIMITING DEMAND", fontSize = 10.sp, color = SlateTextSecondary)
                                Text(
                                    text = activePlan.mainLimiter.uppercase(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                            }
                        }
                    }
                }

                val proposal = activeAdaptationProposal
                if (proposal != null && proposal.state == com.example.data.ProgressionAdaptationState.UPDATE_AVAILABLE) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .testTag("adaptation_proposal_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                        border = BorderStroke(1.5.dp, OrangePrimary)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Plan Update Available",
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "PROGRESSION ADAPTATION PROPOSED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = OrangePrimary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Your training capacity has changed! Proposing adaptive progression update for remaining pending steps.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateTextPrimary
                                    )
                                }
                            }

                            HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.08f))

                            Text(
                                text = "Compare Material Changes:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )

                            // Render changes
                            proposal.changes.forEach { change ->
                                val stepNumber = change.stepNumber
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SlateTextSecondary.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "WEEK $stepNumber (PENDING)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SlateTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Distance comparison
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Distance:", fontSize = 12.sp, color = SlateTextSecondary)
                                        val oldDist = change.oldDistanceMeters ?: 0.0
                                        val newDist = change.newDistanceMeters ?: 0.0
                                        val distDiff = newDist - oldDist
                                        val color = if (distDiff >= 0.0) Color(0xFF10B981) else Color(0xFFEF4444)
                                        val sign = if (distDiff >= 0.0) "+" else ""
                                        Text(
                                            text = String.format("%.1f km -> %.1f km (%s%.1f km)", 
                                                oldDist / 1000.0, 
                                                newDist / 1000.0, 
                                                sign, distDiff / 1000.0),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }

                                    // Elevation comparison
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Elevation Gain:", fontSize = 12.sp, color = SlateTextSecondary)
                                        val oldElev = change.oldElevationGainMeters ?: 0.0
                                        val newElev = change.newElevationGainMeters ?: 0.0
                                        val eleDiff = newElev - oldElev
                                        val color = if (eleDiff >= 0.0) Color(0xFF10B981) else Color(0xFFEF4444)
                                        val sign = if (eleDiff >= 0.0) "+" else ""
                                        Text(
                                            text = String.format("%d m -> %d m (%s%d m)", 
                                                oldElev.toInt(), 
                                                newElev.toInt(), 
                                                sign, eleDiff.toInt()),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }

                                    // Duration comparison
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Est. Duration:", fontSize = 12.sp, color = SlateTextSecondary)
                                        val oldDur = change.oldDurationMinutes ?: 0
                                        val newDur = change.newDurationMinutes ?: 0
                                        val durDiff = newDur - oldDur
                                        val color = if (durDiff >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                        val sign = if (durDiff >= 0) "+" else ""
                                        Text(
                                            text = "${oldDur}m -> ${newDur}m ($sign${durDiff}m)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }

                                    // Focus dimension comparison
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Focus Dimension:", fontSize = 12.sp, color = SlateTextSecondary)
                                        Text(
                                            text = "${change.oldFocusDimension ?: "NONE"} -> ${change.newFocusDimension ?: "NONE"}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateTextPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.acceptPlanUpdate(proposal) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("accept_adaptation_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("UPDATE MY PLAN", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Render the steps
                activeProgressionSteps.forEachIndexed { index, step ->
                    val isCurrentStep = index == activePlan.currentStepIndex
                    val isCompleted = step.status == "COMPLETED"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .testTag("progression_step_${step.stepNumber}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentStep) {
                                OrangePrimary.copy(alpha = 0.05f)
                            } else {
                                SlateCardSurface
                            }
                        ),
                        border = if (isCurrentStep) {
                            BorderStroke(1.5.dp, OrangePrimary)
                        } else if (isCompleted) {
                            BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                        } else {
                            null
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (step.type == "TARGET") "TARGET" else "WEEK ${step.stepNumber}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isCurrentStep) OrangePrimary else SlateTextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = step.title.uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateTextPrimary
                                    )
                                }

                                // Status indicator badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isCompleted -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                isCurrentStep -> OrangePrimary.copy(alpha = 0.15f)
                                                else -> SlateTextSecondary.copy(alpha = 0.1f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isCompleted -> "COMPLETED"
                                            isCurrentStep -> "CURRENT STEP"
                                            else -> "PENDING"
                                        },
                                        color = when {
                                            isCompleted -> Color(0xFF10B981)
                                            isCurrentStep -> OrangePrimary
                                            else -> SlateTextSecondary
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.08f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Distance Column
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = "Distance",
                                            tint = if (step.focusDimension == "DISTANCE") OrangePrimary else SlateTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("Distance", fontSize = 11.sp, color = SlateTextSecondary)
                                    }
                                    val distVal = step.targetDistanceMeters
                                    Text(
                                        text = if (distVal != null) String.format("%.1f km", distVal / 1000.0) else "—",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (step.focusDimension == "DISTANCE") OrangePrimary else SlateTextPrimary
                                    )
                                }

                                // Elevation Column
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Terrain,
                                            contentDescription = "Elevation",
                                            tint = if (step.focusDimension == "ELEVATION") OrangePrimary else SlateTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("Elevation", fontSize = 11.sp, color = SlateTextSecondary)
                                    }
                                    val elevVal = step.targetElevationGainMeters
                                    Text(
                                        text = if (elevVal != null) String.format("%,d m", elevVal.toInt()) else "Unavailable",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (step.focusDimension == "ELEVATION") OrangePrimary else SlateTextPrimary
                                    )
                                }

                                // Duration Column
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = "Duration",
                                            tint = if (step.focusDimension == "ENDURANCE") OrangePrimary else SlateTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("Est. Duration", fontSize = 11.sp, color = SlateTextSecondary)
                                    }
                                    val durVal = step.targetDurationMinutes
                                    val hours = (durVal ?: 0) / 60
                                    val mins = (durVal ?: 0) % 60
                                    val durValText = if (durVal != null) {
                                        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                                    } else "—"
                                    Text(
                                        text = durValText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (step.focusDimension == "ENDURANCE") OrangePrimary else SlateTextPrimary
                                    )
                                }
                            }

                            if (isCurrentStep) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.launchTrackerForStep(activePlan.id, step.id)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("launch_tracker_for_step_${step.stepNumber}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Start Workout",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("LAUNCH TRACKER FOR STEP", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                ReadinessHistoryChart(readinessHistory)
            }
        } else if (plan != null && plan.state == ProgressionPlanState.TARGET_READY) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .testTag("target_ready_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "TARGET PREPARATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangePrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "HIGHLY READY",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = target.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = "Your recorded hiking history closely matches the demands of this target.",
                            fontSize = 14.sp,
                            color = SlateTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Target distance", fontSize = 11.sp, color = SlateTextSecondary)
                                Text(
                                    text = String.format("%.1f km", target.distanceMeters / 1000.0),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Target elevation gain", fontSize = 11.sp, color = SlateTextSecondary)
                                val elevText = if (target.hasElevationData) String.format("%,d m", target.elevationGainMeters.toInt()) else "Unavailable"
                                Text(
                                    text = elevText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Estimated duration", fontSize = 11.sp, color = SlateTextSecondary)
                                val hours = target.estimatedDurationMinutes / 60
                                val mins = target.estimatedDurationMinutes % 60
                                val durText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                                Text(
                                    text = durText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.setShowProgressionScreen(false)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("view_target_ready_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("VIEW TARGET", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else if (plan != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Limited history warning card
                if (plan.isLimitedHistory) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .testTag("limited_history_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Limited History Warning",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (rr.evidence.historyActivityCount == 0) {
                                    "Summit has no recorded hiking history yet. This progression starts conservatively and will improve as you record hikes."
                                } else {
                                    "This progression is based on limited recent hiking history."
                                },
                                fontSize = 13.sp,
                                color = SlateTextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Hero Plan Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "CALCULATED PROGRESSION PLAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangePrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = plan.targetName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CURRENT CAPACITY", fontSize = 10.sp, color = SlateTextSecondary)
                                Text("${plan.startingReadinessScore}% Readiness", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = getReadinessColor(getReadinessLevelForScore(plan.startingReadinessScore)))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TARGET DEMAND", fontSize = 10.sp, color = SlateTextSecondary)
                                Text(
                                    text = when (plan.mainLimiter) {
                                        ReadinessDimension.DISTANCE -> "DISTANCE CAPACITY"
                                        ReadinessDimension.ELEVATION -> "CLIMBING ENDURANCE"
                                        ReadinessDimension.ENDURANCE -> "DURATION ENDURANCE"
                                        ReadinessDimension.RECENT_LOAD -> "RECENT CONSISTENCY"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                            }
                        }

                        HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.08f))

                        Button(
                            onClick = {
                                viewModel.startProgression(plan)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_progression_journey_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("START PROGRESSION JOURNEY", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Steps list
                plan.steps.forEachIndexed { index, step ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .testTag("progression_step_${step.stepNumber}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (step.type == ProgressionStepType.TARGET) {
                                OrangePrimary.copy(alpha = 0.08f)
                            } else {
                                SlateCardSurface
                            }
                        ),
                        border = if (step.type == ProgressionStepType.TARGET) {
                            BorderStroke(1.5.dp, OrangePrimary)
                        } else {
                            null
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = when (step.type) {
                                            ProgressionStepType.BUILD -> "WEEK ${step.stepNumber}"
                                            ProgressionStepType.RECOVERY -> "WEEK ${step.stepNumber}"
                                            ProgressionStepType.TARGET -> "TARGET"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (step.type == ProgressionStepType.TARGET) OrangePrimary else SlateTextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = step.title.uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateTextPrimary
                                    )
                                }

                                // Beautiful badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (step.type) {
                                                ProgressionStepType.BUILD -> Color(0xFF3B82F6).copy(alpha = 0.1f)
                                                ProgressionStepType.RECOVERY -> Color(0xFF10B981).copy(alpha = 0.1f)
                                                ProgressionStepType.TARGET -> OrangePrimary.copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when (step.type) {
                                            ProgressionStepType.BUILD -> "BUILD"
                                            ProgressionStepType.RECOVERY -> "RECOVERY"
                                            ProgressionStepType.TARGET -> "GOAL"
                                        },
                                        color = when (step.type) {
                                            ProgressionStepType.BUILD -> Color(0xFF3B82F6)
                                            ProgressionStepType.RECOVERY -> Color(0xFF10B981)
                                            ProgressionStepType.TARGET -> OrangePrimary
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.08f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Distance Column
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = "Distance",
                                            tint = if (step.focusDimension == ReadinessDimension.DISTANCE) OrangePrimary else SlateTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("Distance", fontSize = 11.sp, color = SlateTextSecondary)
                                    }
                                    val distVal = step.targetDistanceMeters
                                    Text(
                                        text = if (distVal != null) String.format("%.1f km", distVal / 1000.0) else "—",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (step.focusDimension == ReadinessDimension.DISTANCE) OrangePrimary else SlateTextPrimary
                                    )
                                }

                                // Elevation Column
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Terrain,
                                            contentDescription = "Elevation",
                                            tint = if (step.focusDimension == ReadinessDimension.ELEVATION) OrangePrimary else SlateTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("Elevation", fontSize = 11.sp, color = SlateTextSecondary)
                                    }
                                    val elevVal = step.targetElevationGainMeters
                                    Text(
                                        text = if (elevVal != null) String.format("%,d m", elevVal.toInt()) else "Unavailable",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (step.focusDimension == ReadinessDimension.ELEVATION) OrangePrimary else SlateTextPrimary
                                    )
                                }

                                // Duration Column
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = "Duration",
                                            tint = if (step.focusDimension == ReadinessDimension.ENDURANCE) OrangePrimary else SlateTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("Est. Duration", fontSize = 11.sp, color = SlateTextSecondary)
                                    }
                                    val durVal = step.targetDurationMinutes
                                    val hours = (durVal ?: 0) / 60
                                    val mins = (durVal ?: 0) % 60
                                    val durValText = if (durVal != null) {
                                        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                                    } else "—"
                                    Text(
                                        text = durValText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (step.focusDimension == ReadinessDimension.ENDURANCE) OrangePrimary else SlateTextPrimary
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

@Composable
fun ReadinessHistoryChart(history: List<ReadinessHistoryEntity>) {
    val sortedHistory = remember(history) { history.sortedBy { it.recordedAt } }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("readiness_history_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCardSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "READINESS TRACKING TIMELINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = OrangePrimary,
                letterSpacing = 1.sp
            )
            
            if (sortedHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No snapshots recorded yet. Complete step workouts to track your readiness timeline.",
                        color = SlateTextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                val dateFormat = remember { java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()) }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = 16.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val pointsCount = sortedHistory.size
                    
                    // Draw Y-axis grid lines (0, 50, 100)
                    val yLines = listOf(0f, 0.5f, 1f)
                    yLines.forEach { ratio ->
                        val y = height * (1f - ratio)
                        drawLine(
                            color = SlateTextSecondary.copy(alpha = 0.1f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1f
                        )
                    }
                    
                    if (pointsCount > 0) {
                        val xStep = if (pointsCount > 1) width / (pointsCount - 1) else width
                        val path = androidx.compose.ui.graphics.Path()
                        
                        val screenCoordinates = sortedHistory.mapIndexed { idx, entity ->
                            val x = if (pointsCount > 1) idx * xStep else width / 2f
                            val scoreRatio = entity.overallScore / 100f
                            val y = height * (1f - scoreRatio)
                            androidx.compose.ui.geometry.Offset(x, y)
                        }
                        
                        // Draw connection lines
                        screenCoordinates.forEachIndexed { idx, offset ->
                            if (idx == 0) {
                                path.moveTo(offset.x, offset.y)
                            } else {
                                path.lineTo(offset.x, offset.y)
                            }
                        }
                        
                        drawPath(
                            path = path,
                            color = OrangePrimary,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                        
                        // Draw point circles
                        sortedHistory.forEachIndexed { idx, entity ->
                            val coord = screenCoordinates[idx]
                            val color = getScoreColor(entity.overallScore)
                            
                            drawCircle(
                                color = color,
                                radius = 6.dp.toPx(),
                                center = coord
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = coord
                            )
                        }
                    }
                }
                
                // Legends & list of timeline events below
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recorded Snapshots",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    
                    sortedHistory.asReversed().forEach { entity ->
                        val dateStr = dateFormat.format(java.util.Date(entity.recordedAt))
                        val labelText = when (entity.reason) {
                            "BASELINE" -> "Initial Baseline Snapshot"
                            "ACTIVITY_IMPACT" -> "Step Activity Completed"
                            "TARGET_COMPLETED" -> "Target preparation complete! 🏆"
                            else -> entity.reason
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateTextSecondary.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(getScoreColor(entity.overallScore))
                                )
                                Column {
                                    Text(
                                        text = labelText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateTextPrimary
                                    )
                                    Text(
                                        text = "$dateStr • Main Limiter: ${entity.mainLimiter}",
                                        fontSize = 11.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(OrangePrimary.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${entity.overallScore} Score",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangePrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpactReviewScreen(viewModel: SummitViewModel) {
    val impact by viewModel.transientActivityReadinessImpact.collectAsStateWithLifecycle()
    val matchResult by viewModel.transientMatchResult.collectAsStateWithLifecycle()
    val activity by viewModel.transientCompletedActivity.collectAsStateWithLifecycle()
    val step by viewModel.transientStep.collectAsStateWithLifecycle()
    val activePlan by viewModel.activeProgressionPlan.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Segment Preparation Report",
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDarkBackground,
                    titleContentColor = SlateTextPrimary
                )
            )
        },
        containerColor = SlateDarkBackground,
        bottomBar = {
            val p = activePlan
            val s = step
            val a = activity
            if (p != null && s != null && a != null) {
                Surface(
                    color = SlateCardSurface,
                    border = BorderStroke(1.dp, SlateCardSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.keepCurrentStep()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("keep_current_step_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SlateTextPrimary
                            ),
                            border = BorderStroke(1.dp, SlateTextSecondary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("KEEP CURRENT STEP", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.completeStep(p.id, s.id, a.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("complete_step_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("COMPLETE STEP", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val imp = impact
            val mr = matchResult
            val act = activity
            val st = step

            if (imp == null || mr == null || act == null || st == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else {
                // MATCH STATUS CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .testTag("match_status_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (mr) {
                            ProgressionStepMatchResult.MATCHED -> Color(0xFF10B981).copy(alpha = 0.08f)
                            ProgressionStepMatchResult.PARTIALLY_MATCHED -> Color(0xFFF59E0B).copy(alpha = 0.08f)
                            ProgressionStepMatchResult.NOT_MATCHED -> Color(0xFFEF4444).copy(alpha = 0.08f)
                        }
                    ),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = when (mr) {
                            ProgressionStepMatchResult.MATCHED -> Color(0xFF10B981)
                            ProgressionStepMatchResult.PARTIALLY_MATCHED -> Color(0xFFF59E0B)
                            ProgressionStepMatchResult.NOT_MATCHED -> Color(0xFFEF4444)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AUTOMATIC RECOMMENDATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = when (mr) {
                                ProgressionStepMatchResult.MATCHED -> Color(0xFF10B981)
                                ProgressionStepMatchResult.PARTIALLY_MATCHED -> Color(0xFFF59E0B)
                                ProgressionStepMatchResult.NOT_MATCHED -> Color(0xFFEF4444)
                            },
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = when (mr) {
                                ProgressionStepMatchResult.MATCHED -> "MATCHED"
                                ProgressionStepMatchResult.PARTIALLY_MATCHED -> "PARTIALLY MATCHED"
                                ProgressionStepMatchResult.NOT_MATCHED -> "NOT MATCHED"
                            },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = when (mr) {
                                ProgressionStepMatchResult.MATCHED -> Color(0xFF10B981)
                                ProgressionStepMatchResult.PARTIALLY_MATCHED -> Color(0xFFF59E0B)
                                ProgressionStepMatchResult.NOT_MATCHED -> Color(0xFFEF4444)
                            }
                        )

                        Text(
                            text = when (mr) {
                                ProgressionStepMatchResult.MATCHED -> "Great job! This workout successfully satisfied all verified targets for this step."
                                ProgressionStepMatchResult.PARTIALLY_MATCHED -> "You met some of the targets for this step, but not all of them."
                                ProgressionStepMatchResult.NOT_MATCHED -> "This workout did not satisfy any of the target parameters for this step."
                            },
                            fontSize = 13.sp,
                            color = SlateTextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                // COMPARISON CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .testTag("demand_vs_performance_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "DEMAND VS. PERFORMANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangePrimary,
                            letterSpacing = 1.sp
                        )

                        // Distance Comparison
                        if (st.targetDistanceMeters != null && st.targetDistanceMeters > 0) {
                            ComparisonRow(
                                title = "Distance",
                                targetText = String.format("%.1f km", st.targetDistanceMeters / 1000.0),
                                performanceText = String.format("%.1f km", act.distanceKm),
                                ratio = (act.distanceKm * 1000.0) / st.targetDistanceMeters
                            )
                        }

                        // Elevation Comparison
                        if (st.targetElevationGainMeters != null && st.targetElevationGainMeters > 0) {
                            val performanceEleText = if (act.hasElevationData) String.format("%,d m", act.elevationGainM.toInt()) else "Unavailable"
                            ComparisonRow(
                                title = "Elevation Climb",
                                targetText = String.format("%,d m", st.targetElevationGainMeters.toInt()),
                                performanceText = performanceEleText,
                                ratio = if (act.hasElevationData) act.elevationGainM / st.targetElevationGainMeters else null
                            )
                        }

                        // Duration Comparison
                        if (st.targetDurationMinutes != null && st.targetDurationMinutes > 0) {
                            ComparisonRow(
                                title = "Est. Duration",
                                targetText = "${st.targetDurationMinutes}m",
                                performanceText = String.format("%.1f m", act.durationSeconds / 60.0),
                                ratio = (act.durationSeconds / 60.0) / st.targetDurationMinutes
                            )
                        }
                    }
                }

                // DELTA ANALYSIS CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .testTag("delta_analysis_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "READINESS SCORE IMPACT (DELTA ANALYSIS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangePrimary,
                            letterSpacing = 1.sp
                        )

                        // Overall Score Comparison Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Overall Readiness Score", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${imp.overallBefore}%", fontSize = 14.sp, color = SlateTextSecondary)
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "to",
                                    tint = SlateTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                val diff = imp.overallAfter - imp.overallBefore
                                val diffText = if (diff >= 0) "+$diff%" else "$diff%"
                                val diffColor = if (diff >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                Text(
                                    text = "${imp.overallAfter}% ($diffText)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = diffColor
                                )
                            }
                        }

                        HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.08f))

                        // Individual Metrics Comparison
                        DeltaRow(label = "Distance Capacity", before = imp.distanceBefore, after = imp.distanceAfter)
                        if (imp.elevationBefore != null && imp.elevationAfter != null) {
                            DeltaRow(label = "Climbing Endurance", before = imp.elevationBefore, after = imp.elevationAfter)
                        }
                        DeltaRow(label = "Duration Endurance", before = imp.enduranceBefore, after = imp.enduranceAfter)
                        DeltaRow(label = "Recent Consistency", before = imp.recentLoadBefore, after = imp.recentLoadAfter)

                        HorizontalDivider(color = SlateTextSecondary.copy(alpha = 0.08f))

                        // Limiter Comparison
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Main Limiter", fontSize = 13.sp, color = SlateTextSecondary)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(imp.mainLimiterBefore.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "to",
                                    tint = SlateTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(imp.mainLimiterAfter.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonRow(
    title: String,
    targetText: String,
    performanceText: String,
    ratio: Double?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Target: $targetText", fontSize = 12.sp, color = SlateTextSecondary)
                Text("Performance: $performanceText", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
            }
        }

        if (ratio != null) {
            val progress = ratio.coerceIn(0.0, 1.0).toFloat()
            val hasMet = ratio >= 0.80
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (hasMet) Color(0xFF10B981) else Color(0xFFF59E0B),
                    trackColor = SlateCardSurfaceVariant
                )
                Text(
                    text = String.format("%.0f%%", ratio * 100.0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasMet) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
            }
        } else {
            Text("Elevation data unavailable for completed activity; target cannot be verified.", fontSize = 11.sp, color = SlateTextSecondary)
        }
    }
}

@Composable
fun DeltaRow(
    label: String,
    before: Int,
    after: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = SlateTextSecondary)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$before%", fontSize = 12.sp, color = SlateTextSecondary)
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "to",
                tint = SlateTextSecondary,
                modifier = Modifier.size(12.dp)
            )
            val diff = after - before
            val diffText = if (diff >= 0) "+$diff%" else "$diff%"
            val diffColor = if (diff >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
            Text(
                text = "$after% ($diffText)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = diffColor
            )
        }
    }
}
