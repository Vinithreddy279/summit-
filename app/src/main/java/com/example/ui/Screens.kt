package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.DialogProperties
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.preference.PreferenceManager

@Composable
fun SummitApp(viewModel: SummitViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()

    LaunchedEffect(themeMode, systemDark) {
        ThemeState.isDark = when (themeMode) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }
    }

    val appFlow by viewModel.appFlow.collectAsStateWithLifecycle()

    when (appFlow) {
        SummitViewModel.AppFlow.SPLASH -> SplashScreen(viewModel)
        SummitViewModel.AppFlow.ONBOARDING -> OnboardingScreen(viewModel)
        SummitViewModel.AppFlow.LOGIN -> LoginScreen(viewModel)
        SummitViewModel.AppFlow.MAIN -> {
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
            val activeCommentsPostId by viewModel.activeCommentsPostId.collectAsStateWithLifecycle()

            Scaffold(
                bottomBar = {
                    PremiumBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.setTab(it) }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SlateDarkBackground)
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        SummitViewModel.Tab.DASHBOARD -> DashboardScreen(viewModel)
                        SummitViewModel.Tab.SOCIAL_FEED -> FeedScreen(viewModel)
                        SummitViewModel.Tab.RECORD -> RecordScreen(viewModel)
                        SummitViewModel.Tab.GEAR -> GearScreen(viewModel)
                        SummitViewModel.Tab.SEGMENTS -> SegmentsScreen(viewModel)
                    }

                    if (currentTab == SummitViewModel.Tab.DASHBOARD) {
                        StartActivityFAB(
                            onClick = { viewModel.setTab(SummitViewModel.Tab.RECORD) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 20.dp)
                        )
                    }

                    // Comments Overlay Dialog
                    if (activeCommentsPostId != null) {
                        CommentsDialog(viewModel = viewModel)
                    }

                    // Activity Detail Overlay Dialog
                    val selectedActivity by viewModel.selectedActivity.collectAsStateWithLifecycle()
                    if (selectedActivity != null) {
                        ActivityDetailDialog(
                            activity = selectedActivity!!,
                            viewModel = viewModel,
                            onDismiss = { viewModel.selectActivity(null) }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
@Composable
fun PremiumBottomBar(
    currentTab: SummitViewModel.Tab,
    onTabSelected: (SummitViewModel.Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xEC111827)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x2EFFFFFF),
                    Color(0x05FFFFFF)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Pair(SummitViewModel.Tab.DASHBOARD, "Explore" to Icons.Outlined.Explore),
                Pair(SummitViewModel.Tab.RECORD, "Map" to Icons.Outlined.Map),
                Pair(SummitViewModel.Tab.SEGMENTS, "Trips" to Icons.Outlined.Terrain),
                Pair(SummitViewModel.Tab.SOCIAL_FEED, "Community" to Icons.Outlined.Groups),
                Pair(SummitViewModel.Tab.GEAR, "Profile" to Icons.Outlined.Person)
            )

            val selectedIcons = mapOf(
                SummitViewModel.Tab.DASHBOARD to Icons.Filled.Explore,
                SummitViewModel.Tab.RECORD to Icons.Filled.Map,
                SummitViewModel.Tab.SEGMENTS to Icons.Filled.Terrain,
                SummitViewModel.Tab.SOCIAL_FEED to Icons.Filled.Groups,
                SummitViewModel.Tab.GEAR to Icons.Filled.Person
            )

            val testTags = mapOf(
                SummitViewModel.Tab.DASHBOARD to "nav_dashboard",
                SummitViewModel.Tab.RECORD to "nav_record",
                SummitViewModel.Tab.SEGMENTS to "nav_segments",
                SummitViewModel.Tab.SOCIAL_FEED to "nav_feed",
                SummitViewModel.Tab.GEAR to "nav_gear"
            )

            items.forEach { (tab, details) ->
                val (label, icon) = details
                val isSelected = currentTab == tab
                val testTag = testTags[tab] ?: ""

                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "scale"
                )
                val activeBgColor by animateColorAsState(
                    targetValue = if (isSelected) OrangePrimary.copy(alpha = 0.15f) else Color.Transparent,
                    animationSpec = tween(300),
                    label = "bgColor"
                )
                val activeBorderColor by animateColorAsState(
                    targetValue = if (isSelected) OrangePrimary.copy(alpha = 0.4f) else Color.Transparent,
                    animationSpec = tween(300),
                    label = "borderColor"
                )
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) OrangePrimary else SlateTextSecondary,
                    animationSpec = tween(250),
                    label = "iconColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else SlateTextSecondary,
                    animationSpec = tween(250),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .scale(animatedScale)
                        .clip(RoundedCornerShape(20.dp))
                        .background(activeBgColor)
                        .border(
                            width = 1.dp,
                            color = activeBorderColor,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag(testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) (selectedIcons[tab] ?: icon) else icon,
                            contentDescription = label,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                        ) {
                            Text(
                                text = label,
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Screen 1: Dashboard (Stats & Progress summary)
// ============================================================================
@Composable
fun SummitLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background dark blue premium circle or rounded rect
        val roundedRectPath = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(0f, 0f, w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.28f, h * 0.28f)
                )
            )
        }
        drawPath(
            path = roundedRectPath,
            color = Color(0xFF08111D)
        )

        // Sunrise/sunset gradient circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF9100), Color(0x00FF9100)),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.35f
            )
        )
        drawCircle(
            color = Color(0xFFFFFFE5),
            center = Offset(w * 0.5f, h * 0.45f),
            radius = w * 0.15f
        )

        // Blue summit marker above the peak
        drawCircle(
            color = Color(0xFF2FA7FF),
            center = Offset(w * 0.5f, h * 0.25f),
            radius = w * 0.04f
        )

        // Background mountains (Left)
        val m1 = Path().apply {
            moveTo(w * 0.05f, h * 0.85f)
            lineTo(w * 0.3f, h * 0.52f)
            lineTo(w * 0.55f, h * 0.85f)
            close()
        }
        drawPath(path = m1, color = Color(0xFF151A26))

        // Background mountains (Right)
        val m2 = Path().apply {
            moveTo(w * 0.45f, h * 0.85f)
            lineTo(w * 0.72f, h * 0.56f)
            lineTo(w * 0.95f, h * 0.85f)
            close()
        }
        drawPath(path = m2, color = Color(0xFF12151F))

        // Main majestic peak
        val mainPeak = Path().apply {
            moveTo(w * 0.2f, h * 0.85f)
            lineTo(w * 0.52f, h * 0.38f)
            lineTo(w * 0.84f, h * 0.85f)
            close()
        }
        drawPath(path = mainPeak, color = Color(0xFF111827))

        // Peak ridge highlight
        val ridge = Path().apply {
            moveTo(w * 0.52f, h * 0.38f)
            lineTo(w * 0.52f, h * 0.85f)
        }
        drawPath(
            path = ridge,
            color = Color(0xFFFF6A00),
            style = Stroke(width = (w * 0.012f).coerceAtLeast(1.5f))
        )

        // Orange hiking trail curving to peak
        val trail = Path().apply {
            moveTo(w * 0.28f, h * 0.9f)
            cubicTo(w * 0.45f, h * 0.9f, w * 0.45f, h * 0.76f, w * 0.5f, h * 0.7f)
            cubicTo(w * 0.55f, h * 0.65f, w * 0.62f, h * 0.58f, w * 0.68f, h * 0.52f)
        }
        drawPath(
            path = trail,
            color = Color(0xFFFF6A00),
            style = Stroke(
                width = (w * 0.025f).coerceAtLeast(2.5f),
                cap = StrokeCap.Round
            )
        )

        // Hiker silhouette on foreground hill / peak
        // Rock / hill on right
        val rock = Path().apply {
            moveTo(w * 0.35f, h * 0.85f)
            quadraticTo(w * 0.55f, h * 0.62f, w * 0.68f, h * 0.52f)
            quadraticTo(w * 0.78f, h * 0.68f, w * 0.88f, h * 0.85f)
            close()
        }
        drawPath(path = rock, color = Color(0xFF07090F))

        // Explorer
        val hexX = w * 0.68f
        val hexY = h * 0.44f
        drawCircle(
            color = Color(0xFFFF9100),
            center = Offset(hexX, hexY - w * 0.035f),
            radius = w * 0.02f
        )
        // Body
        val body = Path().apply {
            moveTo(hexX - w * 0.02f, hexY)
            lineTo(hexX + w * 0.02f, hexY)
            lineTo(hexX + w * 0.01f, hexY + w * 0.06f)
            lineTo(hexX - w * 0.01f, hexY + w * 0.06f)
            close()
        }
        drawPath(path = body, color = Color(0xFF07090F))
    }
}

@Composable
fun SummitWordmark(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 28.sp,
    textColor: Color = Color.White
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Summ",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                fontSize = fontSize,
                letterSpacing = (-0.5).sp
            )
        )
        Box(
            modifier = Modifier.align(Alignment.Bottom)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mountain accent above "i" (replaces the dot)
                Canvas(modifier = Modifier.size((fontSize.value * 0.4f).dp)) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.5f, h * 0.1f)
                        lineTo(w * 0.9f, h * 0.9f)
                        lineTo(w * 0.1f, h * 0.9f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFF6A00) // Primary Orange
                    )
                    // Optional tiny snow cap on the mountain
                    val capPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.1f)
                        lineTo(w * 0.65f, h * 0.42f)
                        lineTo(w * 0.35f, h * 0.42f)
                        close()
                    }
                    drawPath(
                        path = capPath,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
                // Body of "i"
                Text(
                    text = "ı", // dotless i so we can put our mountain above it!
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        fontSize = fontSize,
                        letterSpacing = (-0.5).sp
                    )
                )
            }
        }
        Text(
            text = "t",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                fontSize = fontSize,
                letterSpacing = (-0.5).sp
            )
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.6f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x11FFFFFF), // Translucent white for glass feel
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x2EFFFFFF),
                    Color(0x04FFFFFF)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            content()
        }
    }
}

@Composable
fun PremiumGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(27.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF6A00), // Primary Orange
                            Color(0xFFFF9100)  // Gold Highlight / Accent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun SplashScreen(viewModel: SummitViewModel) {
    var animStarted by remember { mutableStateOf(false) }
    val sunYOffset by animateFloatAsState(
        targetValue = if (animStarted) 0f else 150f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "sun_rise"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )
    val fadeAlpha by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = EaseInOut),
        label = "fade_alpha"
    )

    LaunchedEffect(Unit) {
        animStarted = true
        delay(2600)
        viewModel.setAppFlow(SummitViewModel.AppFlow.ONBOARDING)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08111D)),
        contentAlignment = Alignment.Center
    ) {
        // Soft animated ambient/mountain background via Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Mountain ridge silhouette (Background)
            val bgMountain = Path().apply {
                moveTo(0f, h * 0.75f)
                lineTo(w * 0.35f, h * 0.55f)
                lineTo(w * 0.7f, h * 0.8f)
                lineTo(w, h * 0.65f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path = bgMountain, color = Color(0xFF111827).copy(alpha = 0.4f))

            // Soft orange sunset glow radiating upward
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF6A00).copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.8f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .scale(logoScale)
                .alpha(fadeAlpha)
                .drawBehind {
                    // Sunrise Glow Circle moving dynamically
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFC857).copy(alpha = 0.3f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f + sunYOffset - 50f),
                            radius = size.width * 0.4f
                        )
                    )
                }
        ) {
            SummitLogo(
                modifier = Modifier
                    .size(140.dp)
                    .padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            SummitWordmark(fontSize = 42.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "EXPLORE BEYOND LIMITS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6A00),
                letterSpacing = 4.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun OnboardingScreen(viewModel: SummitViewModel) {
    var currentPage by remember { mutableStateOf(0) }
    
    val headings = listOf(
        "Explore Hidden Trails",
        "Track Every Adventure",
        "Reach New Summits"
    )
    val descriptions = listOf(
        "Discover thousands of offline mountain routes curated by a global community of modern explorers.",
        "Record your GPS activity with extreme precision and track hardware-sensor telemetry data seamlessly.",
        "Push your boundaries, log equipment mileage milestones, and reach the pinnacle of your performance."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08111D))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummitWordmark(fontSize = 24.sp)
                TextButton(
                    onClick = { viewModel.setAppFlow(SummitViewModel.AppFlow.LOGIN) }
                ) {
                    Text(
                        text = "SKIP",
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Central Animated Illustration Block (Cinematic gradients & vector art)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Multi-page sliding animation for illustration
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "onboarding_slider"
                ) { page ->
                    OnboardingIllustration(page = page)
                }
            }

            // Info text block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = headings[currentPage],
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = descriptions[currentPage],
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer navigation / buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isSelected = index == currentPage
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(0xFFFF6A00) else Color(0x33FFFFFF)
                                )
                        )
                    }
                }

                // CTA Action Button
                Button(
                    onClick = {
                        if (currentPage < 2) {
                            currentPage++
                        } else {
                            viewModel.setAppFlow(SummitViewModel.AppFlow.LOGIN)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(50.dp).widthIn(min = 130.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF6A00), Color(0xFFFF9100))
                                )
                            )
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentPage == 2) "GET STARTED" else "NEXT →",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingIllustration(page: Int) {
    Canvas(
        modifier = Modifier
            .size(280.dp)
            .padding(16.dp)
    ) {
        val w = size.width
        val h = size.height

        // Outer radial gradient soft glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    when (page) {
                        0 -> Color(0xFF2FA7FF).copy(alpha = 0.15f)
                        1 -> Color(0xFFFF6A00).copy(alpha = 0.15f)
                        else -> Color(0xFFFFC857).copy(alpha = 0.15f)
                    },
                    Color.Transparent
                ),
                center = Offset(w / 2f, h / 2f),
                radius = w * 0.6f
            )
        )

        when (page) {
            0 -> {
                // Screen 1: Mountain landscape & winding pathway
                // Sunset sun
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFF6A00), Color(0xFFFFC857))
                    ),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.22f
                )

                // Background range
                val bgRange = Path().apply {
                    moveTo(0f, h * 0.8f)
                    lineTo(w * 0.35f, h * 0.5f)
                    lineTo(w * 0.7f, h * 0.85f)
                    lineTo(w, h * 0.62f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = bgRange, color = Color(0xFF111827))

                // Foreground peak with custom path
                val forePeak = Path().apply {
                    moveTo(w * 0.2f, h * 0.95f)
                    lineTo(w * 0.6f, h * 0.4f)
                    lineTo(w * 0.95f, h * 0.95f)
                    close()
                }
                drawPath(path = forePeak, color = Color(0xFF07090F))

                // Highlight Ridge on Peak
                val ridgePath = Path().apply {
                    moveTo(w * 0.6f, h * 0.4f)
                    lineTo(w * 0.6f, h * 0.95f)
                }
                drawPath(
                    path = ridgePath,
                    color = Color(0xFFFF6A00),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Winding Orange Trail
                val trailPath = Path().apply {
                    moveTo(0f, h * 0.95f)
                    cubicTo(w * 0.3f, h * 0.92f, w * 0.45f, h * 0.82f, w * 0.48f, h * 0.75f)
                    cubicTo(w * 0.52f, h * 0.68f, w * 0.56f, h * 0.52f, w * 0.6f, h * 0.4f)
                }
                drawPath(
                    path = trailPath,
                    color = Color(0xFFFF6A00),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            1 -> {
                // Screen 2: Tracking Progress & GPS telemetry diagram
                // Glowing radial grid circle
                drawCircle(
                    color = Color(0xFFFF6A00).copy(alpha = 0.05f),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.4f,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFFFF6A00).copy(alpha = 0.12f),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.25f,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Winding sports tracking line (Simulates GPS route)
                val gpsPath = Path().apply {
                    moveTo(w * 0.15f, h * 0.75f)
                    cubicTo(w * 0.35f, h * 0.82f, w * 0.25f, h * 0.42f, w * 0.55f, h * 0.48f)
                    cubicTo(w * 0.75f, h * 0.52f, w * 0.72f, h * 0.22f, w * 0.85f, h * 0.32f)
                }
                drawPath(
                    path = gpsPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFF6A00), Color(0xFF2FA7FF))
                    ),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Pulse markers
                drawCircle(
                    color = Color.White,
                    center = Offset(w * 0.15f, h * 0.75f),
                    radius = 8.dp.toPx()
                )
                drawCircle(
                    color = Color(0xFFFF6A00),
                    center = Offset(w * 0.15f, h * 0.75f),
                    radius = 4.dp.toPx()
                )

                drawCircle(
                    color = Color.White,
                    center = Offset(w * 0.85f, h * 0.32f),
                    radius = 8.dp.toPx()
                )
                drawCircle(
                    color = Color(0xFF2FA7FF),
                    center = Offset(w * 0.85f, h * 0.32f),
                    radius = 4.dp.toPx()
                )

                // Intersecting activity dots / milestones
                drawCircle(
                    color = Color(0xFFFFC857),
                    center = Offset(w * 0.42f, h * 0.52f),
                    radius = 5.dp.toPx()
                )
            }
            else -> {
                // Screen 3: Hiker standing victorious on peak pointing to the sky
                // Huge sun setting
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF08111D), Color(0xFFFF6A00))
                    ),
                    center = Offset(w * 0.5f, h * 0.6f),
                    radius = w * 0.3f
                )

                // Mountain Peak
                val peak = Path().apply {
                    moveTo(w * 0.1f, h * 0.95f)
                    lineTo(w * 0.5f, h * 0.5f)
                    lineTo(w * 0.9f, h * 0.95f)
                    close()
                }
                drawPath(path = peak, color = Color(0xFF07090F))

                // Explorer Silhouette
                val pX = w * 0.5f
                val pY = h * 0.45f

                // Head
                drawCircle(
                    color = Color(0xFFFFC857),
                    center = Offset(pX, pY - w * 0.04f),
                    radius = w * 0.025f
                )

                // Body & Raised Arm vector
                val body = Path().apply {
                    moveTo(pX - w * 0.02f, pY)
                    lineTo(pX + w * 0.02f, pY)
                    lineTo(pX + w * 0.015f, pY + w * 0.08f)
                    lineTo(pX - w * 0.015f, pY + w * 0.08f)
                    close()
                }
                drawPath(path = body, color = Color(0xFF07090F))

                // Star details
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    center = Offset(w * 0.25f, h * 0.22f),
                    radius = 1.5.dp.toPx()
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    center = Offset(w * 0.75f, h * 0.15f),
                    radius = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: SummitViewModel) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08111D))
    ) {
        // Large background vector illustration
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Big warm background sunset glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF6A00).copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.4f),
                    radius = w * 0.8f
                )
            )

            // Dynamic diagonal mountain ranges
            val mountain1 = Path().apply {
                moveTo(-100f, h * 0.5f)
                lineTo(w * 0.4f, h * 0.28f)
                lineTo(w + 100f, h * 0.52f)
                lineTo(w + 100f, h)
                lineTo(-100f, h)
                close()
            }
            drawPath(path = mountain1, color = Color(0xFF111827).copy(alpha = 0.6f))

            val mountain2 = Path().apply {
                moveTo(-100f, h * 0.65f)
                lineTo(w * 0.65f, h * 0.42f)
                lineTo(w + 100f, h * 0.68f)
                lineTo(w + 100f, h)
                lineTo(-100f, h)
                close()
            }
            drawPath(path = mountain2, color = Color(0xFF08111D))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .windowInsetsPadding(WindowInsets.safeContent)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Wordmark & Logo Grouping
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                SummitLogo(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 8.dp)
                )
                SummitWordmark(fontSize = 28.sp)
                Text(
                    text = if (isSignUp) "CREATE OFFLINE EXPLORER PROFILE" else "SECURE ATHLETIC CONSOLE ACCESS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = OrangePrimary,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Middle: Input fields in a Glass Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = if (isSignUp) "Sign Up" else "Log In",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF3B0F11), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE57373), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFFFCDD2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isSignUp) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("Athlete Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedLabelColor = OrangePrimary,
                            unfocusedLabelColor = SlateTextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("signup_name_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Explorer Email") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = SlateCardSurfaceVariant,
                        focusedLabelColor = OrangePrimary,
                        unfocusedLabelColor = SlateTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("login_email_input")
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Console Key (Password)") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = OrangePrimary,
                        unfocusedBorderColor = SlateCardSurfaceVariant,
                        focusedLabelColor = OrangePrimary,
                        unfocusedLabelColor = SlateTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("login_password_input")
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = OrangePrimary,
                                uncheckedColor = SlateTextSecondary
                            ),
                            modifier = Modifier.testTag("remember_me_checkbox")
                        )
                        Text(
                            text = "Remember Me",
                            color = SlateTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Buttons: CTA Submit & Switch View
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumGradientButton(
                    onClick = {
                        if (isSignUp) {
                            viewModel.signUp(
                                email = email,
                                name = name,
                                password = password,
                                rememberMe = rememberMe,
                                onSuccess = {},
                                onError = { errorMessage = it }
                            )
                        } else {
                            viewModel.login(
                                email = email,
                                password = password,
                                rememberMe = rememberMe,
                                onSuccess = {},
                                onError = { errorMessage = it }
                            )
                        }
                    },
                    text = if (isSignUp) "Create Profile" else "Access Console",
                    modifier = Modifier.testTag("submit_button")
                )

                TextButton(
                    onClick = {
                        isSignUp = !isSignUp
                        errorMessage = null
                    }
                ) {
                    Text(
                        text = if (isSignUp) "Already have an offline profile? Log In" else "New to Summit? Create Offline Profile",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Summit keeps all user records strictly offline in a secure, encrypted client-side database.",
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E1).copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun SystemSettingsDialog(
    viewModel: SummitViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val useImperial by viewModel.useImperial.collectAsStateWithLifecycle()
    val autoPauseSetting by viewModel.autoPauseSetting.collectAsStateWithLifecycle()
    val gpsAccuracyMeters by viewModel.gpsAccuracyMeters.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(2.dp, OrangePrimary.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(SlateCardSurface)
                    .padding(20.dp)
            ) {
                // Dialog Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONSOLE SETTINGS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = OrangePrimary,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Visual Interface Mode
                    Column {
                        Text(
                            text = "VISUAL INTERFACE MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ThemeOptionRow(
                            label = "System Default",
                            description = "Sync automatically with device theme setting",
                            icon = "💻",
                            selected = themeMode == "system",
                            onClick = { viewModel.setThemeMode("system") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ThemeOptionRow(
                            label = "Midnight Dark",
                            description = "Sleek low-glare dark theme console",
                            icon = "🌌",
                            selected = themeMode == "dark",
                            onClick = { viewModel.setThemeMode("dark") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ThemeOptionRow(
                            label = "Alpine Light",
                            description = "Radiant high-contrast light theme",
                            icon = "❄️",
                            selected = themeMode == "light",
                            onClick = { viewModel.setThemeMode("light") }
                        )
                    }

                    Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)

                    // Measurement Units Setting
                    Column {
                        Text(
                            text = "MEASUREMENT UNITS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateCardSurfaceVariant.copy(alpha = 0.5f))
                                .clickable { viewModel.setUseImperial(!useImperial) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (useImperial) "Imperial Units (mi, ft, lbs)" else "Metric Units (km, m, kg)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                                Text(
                                    text = "Tap to switch between Metric and Imperial measurement systems",
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary
                                )
                            }
                            Switch(
                                checked = useImperial,
                                onCheckedChange = { viewModel.setUseImperial(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OrangePrimary,
                                    checkedTrackColor = OrangePrimary.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }

                    Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)

                    // Auto Pause Setting
                    Column {
                        Text(
                            text = "RECORDING CONTROLS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateCardSurfaceVariant.copy(alpha = 0.5f))
                                .clickable { viewModel.setAutoPause(!autoPauseSetting) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto Pause",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                                Text(
                                    text = "Automatically pauses recording when you stop moving",
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary
                                )
                            }
                            Switch(
                                checked = autoPauseSetting,
                                onCheckedChange = { viewModel.setAutoPause(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OrangePrimary,
                                    checkedTrackColor = OrangePrimary.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }

                    Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)

                    // GPS Accuracy setting
                    Column {
                        Text(
                            text = "GPS ACCURACY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                5 to "High (5m)",
                                10 to "Medium (10m)",
                                25 to "Low (25m)"
                            ).forEach { (meters, label) ->
                                val selected = gpsAccuracyMeters == meters
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) OrangePrimary.copy(alpha = 0.15f) else SlateCardSurfaceVariant.copy(alpha = 0.3f))
                                        .border(1.dp, if (selected) OrangePrimary else SlateCardSurfaceVariant, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setGpsAccuracyThreshold(meters) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) OrangePrimary else SlateTextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)

                    // Data export options
                    Column {
                        Text(
                            text = "DATA MANAGEMENT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = { ExportUtils.exportCSV(context, activities) },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCardSurfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export Activities History (CSV)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { ExportUtils.backupDatabase(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCardSurfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Backup Local Database (SQLite)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Connection details matching professional theme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONSOLE STATUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextSecondary
                    )
                    Text(
                        text = "SECURE & OFFLINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E676)
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeOptionRow(
    label: String,
    description: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) OrangePrimary.copy(alpha = 0.1f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) OrangePrimary else SlateCardSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = SlateTextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) OrangePrimary else Color.Transparent)
                    .border(2.dp, if (selected) OrangePrimary else SlateTextSecondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

data class TrailItem(
    val name: String,
    val distance: String,
    val elevation: String,
    val difficulty: String,
    val difficultyColor: Color
)

@Composable
fun AchievementBadge(
    modifier: Modifier = Modifier,
    badgeType: String, // "gold", "silver", "bronze", "platinum"
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isUnlocked: Boolean = true
) {
    val metallicGradient = when (badgeType.lowercase()) {
        "gold" -> Brush.linearGradient(listOf(Color(0xFFFFF2A3), Color(0xFFE2A100), Color(0xFFFFFAEC), Color(0xFFB37400), Color(0xFFFFF2A3)))
        "silver" -> Brush.linearGradient(listOf(Color(0xFFF0F0F0), Color(0xFF9E9E9E), Color(0xFFFFFFFF), Color(0xFF616161), Color(0xFFF0F0F0)))
        "bronze" -> Brush.linearGradient(listOf(Color(0xFFE5A988), Color(0xFF8B4726), Color(0xFFFBECE5), Color(0xFF5F2B14), Color(0xFFE5A988)))
        else -> Brush.linearGradient(listOf(Color(0xFFE5EDF6), Color(0xFF8CA0BA), Color(0xFFF8FAFC), Color(0xFF4A5A72), Color(0xFFE5EDF6)))
    }

    val glowColor = when (badgeType.lowercase()) {
        "gold" -> Color(0xFFFFB300).copy(alpha = 0.25f)
        "silver" -> Color(0xFFCFD8DC).copy(alpha = 0.2f)
        "bronze" -> Color(0xFF8D6E63).copy(alpha = 0.2f)
        else -> Color(0xFF80DEEA).copy(alpha = 0.25f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .padding(4.dp)
            .alpha(if (isUnlocked) 1.0f else 0.45f)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = if (isUnlocked) 8.dp else 2.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
                .background(Color(0xEE0F172A), CircleShape)
                .border(width = 2.dp, brush = metallicGradient, shape = CircleShape)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                                center = Offset(size.width * 0.3f, size.height * 0.3f),
                                radius = size.width * 0.5f
                            )
                        )
                    }
            )

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isUnlocked) {
                    when (badgeType.lowercase()) {
                        "gold" -> Color(0xFFFFC857)
                        "silver" -> Color(0xFFE2E8F0)
                        "bronze" -> Color(0xFFD3A48C)
                        else -> Color(0xFFE0F2FE)
                    }
                } else SlateTextSecondary,
                modifier = Modifier.size(28.dp)
            )

            if (!isUnlocked) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0xAA000000), CircleShape)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isUnlocked) SlateTextPrimary else SlateTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            color = SlateTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WeatherWidget(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFC857))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE WEATHER REPORT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFC857),
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chamonix Valley Peak",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = SlateTextPrimary
                )
                Text(
                    text = "Clear sky • Perfect trail traction",
                    fontSize = 12.sp,
                    color = SlateTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Sunny Weather",
                        tint = Color(0xFFFFC857),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "21°C",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = SlateTextPrimary
                    )
                }
                Text(
                    text = "Feels like 20°C",
                    fontSize = 11.sp,
                    color = SlateTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Speed, contentDescription = "Wind", tint = SlateTextSecondary, modifier = Modifier.size(16.dp))
                Column {
                    Text("Wind", fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                    Text("12 km/h", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Black)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Cloud, contentDescription = "Humidity", tint = SlateTextSecondary, modifier = Modifier.size(16.dp))
                Column {
                    Text("Humidity", fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                    Text("45%", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Black)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Thermostat, contentDescription = "UV index", tint = SlateTextSecondary, modifier = Modifier.size(16.dp))
                Column {
                    Text("UV Index", fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                    Text("Very High", fontSize = 12.sp, color = Color(0xFFFFC857), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun RecentTripsCard(
    activities: List<com.example.data.Activity>,
    onActivityClick: (com.example.data.Activity) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT TRIPS & RUNS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = OrangePrimary,
                letterSpacing = 1.5.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(OrangePrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${activities.size} TOTAL",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = OrangePrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (activities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = "No Activities",
                        tint = SlateTextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No recorded trips yet.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "Tap Start Activity or Record to begin your first journey!",
                        fontSize = 11.sp,
                        color = SlateTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                activities.take(3).forEachIndexed { index, activity ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(16.dp))
                            .clickable { onActivityClick(activity) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (activity.sportType == "run") Color(0x20FF5E00) else Color(0x2000A2FF)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (activity.sportType == "run") Icons.Default.DirectionsRun else Icons.Default.DirectionsBike,
                                    contentDescription = activity.sportType,
                                    tint = if (activity.sportType == "run") OrangePrimary else Color(0xFF00A2FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = activity.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SlateTextPrimary
                                )
                                Text(
                                    text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(java.util.Date(activity.timestamp)),
                                    fontSize = 10.sp,
                                    color = SlateTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f km", activity.distanceKm),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = OrangePrimary
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "+%.0fm Elev", activity.elevationGainM),
                                fontSize = 10.sp,
                                color = SlateTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NearbyTrailsSection(modifier: Modifier = Modifier) {
    val trails = listOf(
        TrailItem("Summit Ridge Loop", "7.8 km", "1,240m", "Hard", Color(0xFFEF4444)),
        TrailItem("Golden Hour Crest", "4.2 km", "380m", "Moderate", Color(0xFFF59E0B)),
        TrailItem("Echo Lake Pass", "12.4 km", "850m", "Hard", Color(0xFFEF4444)),
        TrailItem("Valley Vista Loop", "3.5 km", "120m", "Easy", Color(0xFF10B981))
    )

    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEARBY SCENIC TRAILS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = OrangePrimary,
                letterSpacing = 1.5.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(OrangePrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "GPS verified",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = OrangePrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            trails.forEach { trail ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x2010B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terrain,
                                contentDescription = "Trail icon",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = trail.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = "${trail.distance} • ${trail.elevation} gain",
                                fontSize = 11.sp,
                                color = SlateTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(trail.difficultyColor.copy(alpha = 0.15f))
                            .border(1.dp, trail.difficultyColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = trail.difficulty,
                            color = trail.difficultyColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementsSection(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Text(
            text = "ATHLETIC ACHIEVEMENTS & MEDALS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = OrangePrimary,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AchievementBadge(
                modifier = Modifier.weight(1f),
                badgeType = "platinum",
                title = "Apex Predator",
                subtitle = "5000m altitude",
                icon = Icons.Default.Terrain,
                isUnlocked = true
            )
            AchievementBadge(
                modifier = Modifier.weight(1f),
                badgeType = "gold",
                title = "Century Rider",
                subtitle = "100km single trip",
                icon = Icons.Default.DirectionsBike,
                isUnlocked = true
            )
            AchievementBadge(
                modifier = Modifier.weight(1f),
                badgeType = "silver",
                title = "Speed Demon",
                subtitle = "Sub 4-min km pace",
                icon = Icons.Default.DirectionsRun,
                isUnlocked = true
            )
            AchievementBadge(
                modifier = Modifier.weight(1f),
                badgeType = "bronze",
                title = "Early Riser",
                subtitle = "5am daily streak",
                icon = Icons.Default.WbSunny,
                isUnlocked = false
            )
        }
    }
}

@Composable
fun StartActivityFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isMounted = true
    }
    val scale by animateFloatAsState(
        targetValue = if (isMounted) 1.0f else 0.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_scale"
    )

    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = Color.Transparent,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 12.dp),
        modifier = modifier
            .scale(scale)
            .size(56.dp)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = OrangePrimary.copy(alpha = 0.4f),
                spotColor = OrangePrimary.copy(alpha = 0.5f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(OrangePrimary, OrangeSecondary)
                ),
                shape = CircleShape
            )
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.25f), shape = CircleShape)
            .testTag("start_activity_fab")
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Start Activity",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun WeeklyPerformanceMetricsWidget(
    activities: List<com.example.data.Activity>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Heart Rate, 1: Pace Distribution

    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ATHLETIC PERFORMANCE METRICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = OrangePrimary,
                    letterSpacing = 1.5.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(OrangePrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "WEEKLY REPORT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = OrangePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Premium Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateCardSurfaceVariant)
                    .padding(4.dp)
            ) {
                listOf("Heart Rate Zones", "Pace Distribution").forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == index) OrangePrimary else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == index) Color.White else SlateTextSecondary
                        )
                    }
                }
            }
 
             Spacer(modifier = Modifier.height(16.dp))



             Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                HeartRateZonesContent(activities)
            } else {
                PaceDistributionContent(activities)
            }
        }
    }
}

@Composable
fun HeartRateZonesContent(activities: List<com.example.data.Activity>) {
    // Determine zones based on activities or mock values
    val totalSeconds = activities.sumOf { it.durationSeconds }.toDouble()
    
    // Default mock durations if zero workouts
    val warmUpSec = if (totalSeconds > 0) totalSeconds * 0.20 else 2400.0 // 40m
    val fatBurnSec = if (totalSeconds > 0) totalSeconds * 0.40 else 4800.0 // 80m
    val aerobicSec = if (totalSeconds > 0) totalSeconds * 0.30 else 3600.0 // 60m
    val anaerobicSec = if (totalSeconds > 0) totalSeconds * 0.10 else 1200.0 // 20m

    val sum = warmUpSec + fatBurnSec + aerobicSec + anaerobicSec

    val pctZ1 = (warmUpSec / sum).toFloat()
    val pctZ2 = (fatBurnSec / sum).toFloat()
    val pctZ3 = (aerobicSec / sum).toFloat()
    val pctZ4 = (anaerobicSec / sum).toFloat()

    val zones = listOf(
        Triple("Z1 Warm Up", "100-120 BPM", Color(0xFF94A3B8) to pctZ1),
        Triple("Z2 Fat Burn", "121-140 BPM", Color(0xFF10B981) to pctZ2),
        Triple("Z3 Aerobic/Cardio", "141-160 BPM", Color(0xFFF59E0B) to pctZ3),
        Triple("Z4 Anaerobic/Peak", "161-190+ BPM", Color(0xFFEF4444) to pctZ4)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Intensity distribution based on heart rate modeling:",
            fontSize = 12.sp,
            color = SlateTextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Stacked progress bar using Canvas for high precision
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            val width = size.width
            val height = size.height

            var currentX = 0f
            zones.forEach { (_, _, colorAndPct) ->
                val (color, pct) = colorAndPct
                val segmentWidth = width * pct
                drawRect(
                    color = color,
                    topLeft = Offset(currentX, 0f),
                    size = androidx.compose.ui.geometry.Size(segmentWidth, height)
                )
                currentX += segmentWidth
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend with metrics
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            zones.forEach { (name, range, colorAndPct) ->
                val (color, pct) = colorAndPct
                val segmentMin = ((sum * pct) / 60.0).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Column {
                            Text(
                                text = name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = range,
                                fontSize = 10.sp,
                                color = SlateTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.0f%%", pct * 100),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = "${segmentMin} min",
                            fontSize = 10.sp,
                            color = SlateTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaceDistributionContent(activities: List<com.example.data.Activity>) {
    val totalSeconds = activities.sumOf { it.durationSeconds }
    val avgPaceText = if (activities.isEmpty()) "5:20/km" else {
        val totalDistance = activities.sumOf { it.distanceKm }
        if (totalDistance > 0) {
            val totalMin = totalSeconds / 60.0
            val minPerKm = totalMin / totalDistance
            val mins = minPerKm.toInt()
            val secs = ((minPerKm - mins) * 60).toInt()
            String.format(java.util.Locale.US, "%d:%02d/km", mins, secs)
        } else "5:20/km"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Weekly pace density curve:",
                    fontSize = 12.sp,
                    color = SlateTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "High density in moderate aerobic pace",
                    fontSize = 10.sp,
                    color = OrangeSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Avg Pace",
                    fontSize = 10.sp,
                    color = SlateTextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = avgPaceText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = OrangePrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom drawn Canvas area curve for pace distribution
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(SlateDarkBackground.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(8.dp))
                .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Grid lines
                val gridColor = SlateCardSurfaceVariant
                drawLine(gridColor, Offset(0f, height * 0.25f), Offset(width, height * 0.25f), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, height * 0.5f), Offset(width, height * 0.5f), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, height * 0.75f), Offset(width, height * 0.75f), strokeWidth = 1f)

                // Spline points for pace distribution: coordinates
                val points = listOf(
                    Offset(0f, height),
                    Offset(width * 0.15f, height * 0.9f),
                    Offset(width * 0.3f, height * 0.5f),
                    Offset(width * 0.45f, height * 0.15f), // Mode / peak density
                    Offset(width * 0.6f, height * 0.35f),
                    Offset(width * 0.75f, height * 0.7f),
                    Offset(width * 0.9f, height * 0.95f),
                    Offset(width, height)
                )

                val path = Path()
                path.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val pPrev = points[i - 1]
                    val pCurr = points[i]
                    val controlX = (pPrev.x + pCurr.x) / 2
                    path.cubicTo(controlX, pPrev.y, controlX, pCurr.y, pCurr.x, pCurr.y)
                }
                path.lineTo(width, height)
                path.close()

                // Draw Area under curve with dynamic gradient
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(OrangePrimary.copy(alpha = 0.4f), Color.Transparent)
                    )
                )

                // Draw Curve stroke
                val strokePath = Path()
                strokePath.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val pPrev = points[i - 1]
                    val pCurr = points[i]
                    val controlX = (pPrev.x + pCurr.x) / 2
                    strokePath.cubicTo(controlX, pPrev.y, controlX, pCurr.y, pCurr.x, pCurr.y)
                }
                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(OrangePrimary, OrangeSecondary)
                    ),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Peak Dot Indicator
                val peakX = width * 0.45f
                val peakY = height * 0.15f
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(peakX, peakY)
                )
                drawCircle(
                    color = OrangePrimary,
                    radius = 3.dp.toPx(),
                    center = Offset(peakX, peakY)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Axis Legend Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Fast (3:30)", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
            Text("Aero (5:15)", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
            Text("Slow (7:00+)", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardScreen(viewModel: SummitViewModel) {
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val gears by viewModel.gears.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }
    var showMonthlySummary by remember { mutableStateOf(false) }

    if (showSettings) {
        SystemSettingsDialog(viewModel = viewModel, onDismiss = { showSettings = false })
    }

    if (showMonthlySummary) {
        MonthlySummaryDialog(activities = activities, onDismiss = { showMonthlySummary = false })
    }

    val totalDistance = activities.sumOf { it.distanceKm }
    val totalSeconds = activities.sumOf { it.durationSeconds }
    val totalElev = activities.sumOf { it.elevationGainM }
    val activityCount = activities.size

    val runsCount = activities.count { it.sportType == "run" }
    val ridesCount = activities.count { it.sportType == "ride" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            // Summit Athlete Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummitLogo(modifier = Modifier.size(44.dp))
                    Column {
                        Text(
                            text = "SUMMIT",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = SlateTextPrimary,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(OrangePrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ATHLETIC CONSOLE ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                
                // Quick-Access Settings Toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showMonthlySummary = true },
                        modifier = Modifier
                            .testTag("monthly_summary_button")
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SlateCardSurface)
                            .border(1.dp, OrangePrimary.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Monthly Summary",
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SlateCardSurface)
                            .border(1.dp, OrangePrimary.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Console Settings",
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SlateCardSurface)
                            .border(1.dp, OrangeSecondary, CircleShape)
                            .clickable { showSettings = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "S",
                            color = OrangePrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            WeatherWidget(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            RecentTripsCard(
                activities = activities,
                onActivityClick = { viewModel.selectActivity(it) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            WeeklyPerformanceMetricsWidget(activities = activities, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            NearbyTrailsSection(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            AchievementsSection(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // S-Class Leveling Athlete Status Panel (Premium Glassmorphism Widget)
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                    // Panel Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ATHLETE PERFORMANCE PANEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangePrimary,
                            letterSpacing = 1.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(OrangePrimary.copy(alpha = 0.15f))
                                .border(1.dp, OrangePrimary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ELITE CLASS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = OrangePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Athlete level & title summary row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Level Indicator based on logged distance metrics
                        val calculatedLevel = 1 + (totalDistance / 10).toInt()
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SlateDarkBackground)
                                .border(2.dp, OrangePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("LVL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                                Text("$calculatedLevel", fontSize = 18.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
                            }
                        }

                        // Athlete profile identifiers
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SUMMIT PERFORMER",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = "CLASS: MULTISPORT PERFORMER • TITLES: MOUNTAIN REIGN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Endurance & Frequency status bars
                    val hpProgress = minOf(1.0f, (totalDistance / 50.0f).toFloat())
                    val mpProgress = minOf(1.0f, (activityCount / 10.0f).toFloat())

                    // Endurance progress (Weekly Distance Goal)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ENDURANCE [WEEKLY DISTANCE GOAL]", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Text(
                                text = String.format(Locale.US, "%.1f / 50.0 km (%.0f%%)", totalDistance, hpProgress * 100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Progress bar styled with sporty orange gradients
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(SlateDarkBackground)
                                .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(5.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(hpProgress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(OrangeSecondary, OrangePrimary)
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Frequency progress (Workout completed streak rate)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("FREQUENCY [WEEKLY ACTIVE TARGET]", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Text(
                                text = String.format(Locale.US, "%d / 10 Activities (%.0f%%)", activityCount, mpProgress * 100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Progress bar styled with sporty gold/orange gradients
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(SlateDarkBackground)
                                .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(5.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(mpProgress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(OrangePrimary, OrangeTertiary)
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Core Athletic Attribute Analytics (derived dynamically from fitness data)
                    Text(
                        text = "ATHLETIC ATTRIBUTES ANALYSIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = OrangePrimary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val strVal = 10 + runsCount * 5 + ridesCount * 3
                        val vitVal = 10 + (totalElev / 50).toInt()
                        val agiVal = 12 + runsCount * 4
                        val senVal = 8 + gears.size * 5
                        val intVal = 10 + activityCount * 2

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "PWR (POWER):      $strVal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                            Text(text = "END (ENDURANCE):  $vitVal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                            Text(text = "SPD (SPEED):      $agiVal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "FRC (FREQUENCY):  $senVal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                            Text(text = "EFF (EFFICIENCY): $intVal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                            Text(text = "CLASS RANK:       ELITE", fontSize = 12.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom overall metrics quick summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RUNS/RIDES", fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(activityCount.toString(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ACTIVE TIME", fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(formatElapsedTimeShort(totalSeconds), fontSize = 16.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ELEV GAIN", fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(String.format(Locale.US, "%.0fm", totalElev), fontSize = 16.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
                        }
                    }
                }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Sport distribution
            Text("SPORT ANALYSIS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SportCard(
                    sport = "Rides",
                    count = ridesCount,
                    icon = Icons.Filled.DirectionsBike,
                    color = RideColor,
                    modifier = Modifier.weight(1f)
                )
                SportCard(
                    sport = "Runs/Walks",
                    count = runsCount + activities.count { it.sportType == "walk" },
                    icon = Icons.Filled.DirectionsRun,
                    color = RunColor,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Equipment Watch / Active Gear Section matching HTML
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE GEAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(OrangePrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${minOf(2, gears.size)} Active",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrangePrimary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (gears.isEmpty()) {
                        Text(
                            text = "No active gear registered. Go to Gear tab to register shoes or bikes!",
                            color = SlateTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } else {
                        gears.take(2).forEachIndexed { index, gear ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            val progress = (gear.currentMileageKm / gear.maxMileageKm).toFloat()
                            val progressColor = OrangePrimary
                            
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = gear.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SlateTextPrimary
                                        )
                                        Text(
                                            text = "${gear.brand} • ${if (gear.type == "shoes") "Road Running" else "Cycling"}",
                                            fontSize = 11.sp,
                                            color = SlateTextSecondary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format("%.0f / %.0f km", gear.currentMileageKm, gear.maxMileageKm),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateTextPrimary
                                        )
                                        Text(
                                            text = String.format("%.0f%% Lifespan", progress * 100),
                                            fontSize = 11.sp,
                                            color = OrangeSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = minOf(1.0f, progress),
                                    color = progressColor,
                                    trackColor = SlateCardSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = SlateTextSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
        Text(label, fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SportCard(sport: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = sport, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(count.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
            Text(sport, fontSize = 12.sp, color = SlateTextSecondary)
        }
    }
}

// ============================================================================
// Screen 2: Customizable Social Feed (Posts, Kudos, Filterable, Compact view)
// ============================================================================
@Composable
fun FeedScreen(viewModel: SummitViewModel) {
    val posts by viewModel.feedPosts.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val sportFilter by viewModel.feedSportFilter.collectAsStateWithLifecycle()
    val authorFilter by viewModel.feedAuthorFilter.collectAsStateWithLifecycle()
    val compactMode by viewModel.feedCompactMode.collectAsStateWithLifecycle()
    val showStats by viewModel.feedShowStats.collectAsStateWithLifecycle()

    var showPostDialog by remember { mutableStateOf(false) }
    var customPostTitle by remember { mutableStateOf("") }
    var customPostContent by remember { mutableStateOf("") }

    // Filter logic
    val filteredPosts = posts.filter { post ->
        val matchesSport = if (sportFilter == "all") true else post.sportType?.lowercase() == sportFilter.lowercase()
        val matchesAuthor = when (authorFilter) {
            "me" -> post.userName == "You"
            "friends" -> post.userName != "You"
            else -> true
        }
        matchesSport && matchesAuthor
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Feed Controls TopBar (Customizer Panel!)
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    SlateCardSurfaceVariant,
                    RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Athletic Feed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = SlateTextPrimary
                    )
                    IconButton(
                        onClick = { showPostDialog = true },
                        modifier = Modifier
                            .testTag("add_post_button")
                            .size(36.dp)
                            .background(OrangePrimary, CircleShape)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add custom post", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sport Filters Horizontal Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sportFilters = listOf(
                        "all" to "All",
                        "run" to "Running",
                        "ride" to "Cycling"
                    )
                    sportFilters.forEach { (key, label) ->
                        FilterChip(
                            label = label,
                            selected = sportFilter == key,
                            onClick = { viewModel.setFeedSportFilter(key) },
                            modifier = Modifier.testTag("filter_chip_$key")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle controls (Stats, Compact mode, Author filters)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Author Filters
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            label = "All Authors",
                            selected = authorFilter == "all",
                            onClick = { viewModel.setFeedAuthorFilter("all") }
                        )
                        FilterChip(
                            label = "Me Only",
                            selected = authorFilter == "me",
                            onClick = { viewModel.setFeedAuthorFilter("me") }
                        )
                    }

                    // Toggles icon row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Toggle Stats
                        IconButton(
                            onClick = { viewModel.toggleFeedShowStats() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (showStats) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle statistics visibility",
                                tint = if (showStats) OrangePrimary else SlateTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        // Toggle Compact mode
                        IconButton(
                            onClick = { viewModel.toggleFeedCompactMode() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (compactMode) Icons.Filled.List else Icons.Filled.Grid3x3,
                                contentDescription = "Toggle feed display mode",
                                tint = if (compactMode) OrangePrimary else SlateTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Posts list
        if (filteredPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.RssFeed, contentDescription = "No feed activity", tint = SlateTextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Feed is empty for current filters.", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                    Text("Record an activity or post a custom thought!", color = SlateTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredPosts, key = { it.id }) { post ->
                    AnimatedCardContainer(key = post.id) {
                        if (compactMode) {
                            CompactFeedPostCard(post = post, onKudosClick = { viewModel.toggleKudos(post.id) }, onCommentClick = { viewModel.openComments(post.id) })
                        } else {
                            RichFeedPostCard(
                                post = post,
                                showStats = showStats,
                                onKudosClick = { viewModel.toggleKudos(post.id) },
                                onCommentClick = { viewModel.openComments(post.id) },
                                onViewRouteClick = {
                                    val matchedAct = activities.find { it.id == post.activityId }
                                    if (matchedAct != null) {
                                        viewModel.selectActivity(matchedAct)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // New Custom Post Dialog
    if (showPostDialog) {
        Dialog(onDismissRequest = { showPostDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Share a Post", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customPostTitle,
                        onValueChange = { customPostTitle = it },
                        label = { Text("Title / Headline") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customPostContent,
                        onValueChange = { customPostContent = it },
                        label = { Text("What's on your mind?") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showPostDialog = false }) {
                            Text("Cancel", color = SlateTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (customPostTitle.trim().isNotEmpty() && customPostContent.trim().isNotEmpty()) {
                                    viewModel.insertCustomPost(customPostTitle, customPostContent)
                                    customPostTitle = ""
                                    customPostContent = ""
                                    showPostDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Text("Post", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) OrangePrimary else SlateCardSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else SlateTextSecondary
        )
    }
}

@Composable
fun AnimatedKudosButton(post: FeedPost, onClick: () -> Unit) {
    var isClicked by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isClicked) 1.5f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "KudosScale",
        finishedListener = {
            isClicked = false
        }
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                isClicked = true
                onClick()
            }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (post.isKudosedByMe) "🧡" else "🤍",
            fontSize = 16.sp,
            modifier = Modifier.scale(scale)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${post.kudosCount}",
            fontSize = 13.sp,
            color = if (post.isKudosedByMe) Color(0xFF00E5FF) else SlateTextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AnimatedKudosIconButton(post: FeedPost, onClick: () -> Unit) {
    var isClicked by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isClicked) 1.5f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "KudosIconScale",
        finishedListener = {
            isClicked = false
        }
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                isClicked = true
                onClick()
            }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ThumbUp,
            contentDescription = "Give kudos",
            tint = if (post.isKudosedByMe) OrangePrimary else SlateTextSecondary,
            modifier = Modifier
                .size(16.dp)
                .scale(scale)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${post.kudosCount}",
            fontSize = 12.sp,
            color = SlateTextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RichFeedPostCard(
    post: FeedPost,
    showStats: Boolean,
    onKudosClick: () -> Unit,
    onCommentClick: () -> Unit,
    onViewRouteClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Get avatar color based on name
                val avatarBgColor = when (post.userName) {
                    "You" -> Color(0xFFD0BCFF)
                    "Jordan Miller" -> Color(0xFFEFB8C8)
                    else -> Color(0xFFC5CAE9)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(avatarBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.userName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(post.userName, fontWeight = FontWeight.Bold, color = SlateTextPrimary, fontSize = 14.sp)
                        if (post.privacy != "Public") {
                            Spacer(modifier = Modifier.width(6.dp))
                            val privIcon = when (post.privacy) {
                                "Private" -> "🔒"
                                "Friends Only" -> "👥"
                                else -> "🔓"
                            }
                            Box(
                                modifier = Modifier
                                    .background(SlateCardSurfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$privIcon ${post.privacy}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextSecondary
                                )
                            }
                        }
                    }
                    Text(
                        text = "${formatDate(post.timestamp)} • Boulder, CO",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main body card block in #F3EDF7 matching Jordan Miller style
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (post.distanceKm != null && post.distanceKm > 0) {
                        // Display athletic metrics prominently
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", post.distanceKm),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SlateTextPrimary,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = "km",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateTextPrimary,
                                    modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                                )
                            }
                            Text(
                                text = post.title.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6750A4),
                                letterSpacing = 1.5.sp,
                                textAlign = TextAlign.Center
                            )

                            if (post.distanceKm != null && post.distanceKm >= 15.0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .background(Color(0x10FFD700), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0x30FFD700), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Milestone Badge",
                                        tint = Color(0xFFFFC857),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "EARNED APEX PREDATOR GOLD MEDAL!",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFFC857)
                                    )
                                }
                            }
                            
                            if (post.content.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = post.content,
                                    fontSize = 13.sp,
                                    color = SlateTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Bottom metrics row (Pace, Elev, Time) styled with italic and bold
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PACE", fontSize = 9.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
                                    val paceSec = if (post.distanceKm > 0) ((post.durationSeconds ?: 0) / post.distanceKm).toInt() else 0
                                    val minutes = paceSec / 60
                                    val seconds = paceSec % 60
                                    Text(
                                        text = String.format(Locale.US, "%d'%02d\"", minutes, seconds),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateTextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ELEV", fontSize = 9.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = String.format(Locale.US, "%.0fm", post.elevationGainM ?: 0.0),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateTextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("TIME", fontSize = 9.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = formatElapsedTimeShort(post.durationSeconds ?: 0L),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateTextPrimary
                                    )
                                }
                            }
                            if (post.activityId != null && onViewRouteClick != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onViewRouteClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary.copy(alpha = 0.12f), contentColor = OrangePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("VIEW INTERACTIVE MAP", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                    } else {
                        // Text-only post
                        Text(
                            text = post.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = post.content,
                            fontSize = 13.sp,
                            color = SlateTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Kudos / Comment actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedKudosButton(post = post, onClick = onKudosClick)

                    Spacer(modifier = Modifier.width(16.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCommentClick)
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💬",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.commentsCount}",
                            fontSize = 13.sp,
                            color = Color(0xFF49454F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Achievement badge
                if (post.distanceKm != null && post.distanceKm > 10.0) {
                    Text(
                        text = "New PB on Peak Sprint",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangePrimary,
                        letterSpacing = (-0.2).sp
                    )
                }
            }
        }
    }
}

@Composable
fun CompactFeedPostCard(post: FeedPost, onKudosClick: () -> Unit, onCommentClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (post.userName == "You") OrangePrimary else SlateCardSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(post.userName.take(1), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(post.userName, fontWeight = FontWeight.Bold, color = SlateTextPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(formatDate(post.timestamp), fontSize = 10.sp, color = SlateTextSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.title,
                    fontWeight = FontWeight.Black,
                    color = SlateTextPrimary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (post.distanceKm != null) {
                    Text(
                        text = String.format("%.1f km | %s", post.distanceKm, formatElapsedTimeShort(post.durationSeconds ?: 0L)),
                        fontSize = 12.sp,
                        color = OrangePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedKudosIconButton(post = post, onClick = onKudosClick)

                IconButton(onClick = onCommentClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Open comments",
                        tint = SlateTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text("${post.commentsCount}", fontSize = 12.sp, color = SlateTextSecondary)
            }
        }
    }
}

// ============================================================================
// Screen 3: GPS Fitness Active Recording (Simulated / Real tracking with route drawing Canvas)
// ============================================================================
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecordScreen(viewModel: SummitViewModel) {
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isPaused by TrackingService.isPaused.collectAsStateWithLifecycle()
    val isAutoPaused by TrackingService.isAutoPaused.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.recordingDurationSeconds.collectAsStateWithLifecycle()
    val distanceKm by viewModel.recordingDistanceKm.collectAsStateWithLifecycle()
    val sportType by viewModel.recordingSportType.collectAsStateWithLifecycle()
    val gears by viewModel.gears.collectAsStateWithLifecycle()
    val selectedGearId by viewModel.recordingGearId.collectAsStateWithLifecycle()
    val trackpoints by viewModel.recordingTrackpoints.collectAsStateWithLifecycle()
    val selectedSimulationRoute by viewModel.selectedSimulationRoute.collectAsStateWithLifecycle()

    val currentSpeedKmh by TrackingService.currentSpeedKmh.collectAsStateWithLifecycle()
    val avgSpeedKmh by TrackingService.avgSpeedKmh.collectAsStateWithLifecycle()
    val currentPaceString by TrackingService.currentPaceString.collectAsStateWithLifecycle()
    val avgPaceString by TrackingService.avgPaceString.collectAsStateWithLifecycle()
    val gpsAccuracyMeters by TrackingService.gpsAccuracyMeters.collectAsStateWithLifecycle()
    val caloriesBurned by TrackingService.caloriesBurned.collectAsStateWithLifecycle()
    val autoPauseSetting by TrackingService.autoPauseSetting.collectAsStateWithLifecycle()

    var showFinishDialog by remember { mutableStateOf(false) }
    var activityNotes by remember { mutableStateOf("") }
    var selectedPrivacy by remember { mutableStateOf("Public") }

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val activeGearName = gears.find { it.id == selectedGearId }?.name ?: "No Gear Selected"

    val context = LocalContext.current
    var gpsFixState by remember { mutableStateOf<android.location.Location?>(null) }
    var isCheckingGps by remember { mutableStateOf(false) }

    DisposableEffect(selectedSimulationRoute, permissionState.allPermissionsGranted, isRecording) {
        var callback: com.google.android.gms.location.LocationCallback? = null
        val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        
        if (!isRecording && (selectedSimulationRoute == "None" || selectedSimulationRoute == null)) {
            if (permissionState.allPermissionsGranted) {
                isCheckingGps = true
                
                callback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val lastLoc = result.lastLocation
                        if (lastLoc != null) {
                            gpsFixState = lastLoc
                            isCheckingGps = false
                        }
                    }
                }
                
                try {
                    fusedClient.lastLocation.addOnSuccessListener { loc ->
                        if (loc != null && (System.currentTimeMillis() - loc.time) < 60000) {
                            gpsFixState = loc
                            isCheckingGps = false
                        }
                    }
                    
                    val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        2000L
                    ).build()
                    
                    fusedClient.requestLocationUpdates(
                        locationRequest,
                        callback,
                        android.os.Looper.getMainLooper()
                    )
                } catch (e: SecurityException) {
                    isCheckingGps = false
                }
            } else {
                gpsFixState = null
                isCheckingGps = false
            }
        } else {
            gpsFixState = null
            isCheckingGps = false
        }
        
        onDispose {
            callback?.let {
                try {
                    fusedClient.removeLocationUpdates(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (!isRecording && trackpoints.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 100.dp)
            ) {
                Text(
                    text = "Record Activity",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = SlateTextPrimary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Establish your session settings before starting.",
                        fontSize = 13.sp,
                        color = SlateTextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Status indicator
                    val isDemo = selectedSimulationRoute != "None" && selectedSimulationRoute != null
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (isDemo) Color(0xFFFFA726).copy(alpha = 0.15f) else Color(0xFF66BB6A).copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isDemo) Color(0xFFFFA726) else Color(0xFF66BB6A), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDemo) "Demo Mode" else "Live GPS",
                            color = if (isDemo) Color(0xFFFFA726) else Color(0xFF66BB6A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sport Selection Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Activity Type", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SportSelectorButton(
                                label = "Run",
                                icon = Icons.Filled.DirectionsRun,
                                selected = sportType == "run",
                                color = RunColor,
                                onClick = { viewModel.setRecordingSportType("run") }
                            )
                            SportSelectorButton(
                                label = "Ride",
                                icon = Icons.Filled.DirectionsBike,
                                selected = sportType == "ride",
                                color = RideColor,
                                onClick = { viewModel.setRecordingSportType("ride") }
                            )
                            SportSelectorButton(
                                label = "Walk",
                                icon = Icons.Filled.DirectionsWalk,
                                selected = sportType == "walk",
                                color = WalkColor,
                                onClick = { viewModel.setRecordingSportType("walk") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gear Selection Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Gear to Wear", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (gears.isEmpty()) {
                            Text("No active gear registered. Tap Gear screen to register gear first.", color = SlateTextSecondary, fontSize = 12.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                gears.filter { !it.isRetired }.forEach { gear ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedGearId == gear.id) OrangePrimary.copy(alpha = 0.2f) else SlateCardSurfaceVariant)
                                            .clickable { viewModel.setRecordingGear(if (selectedGearId == gear.id) null else gear.id) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (gear.type == "shoes") Icons.Filled.DirectionsRun else Icons.Filled.DirectionsBike,
                                                contentDescription = "Gear icon",
                                                tint = if (selectedGearId == gear.id) OrangePrimary else SlateTextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(gear.name, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                        }
                                        if (selectedGearId == gear.id) {
                                            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = OrangePrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-Pause Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Pause Stationary", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                            Text("Automatically pause tracking when you stop moving.", fontSize = 11.sp, color = SlateTextSecondary)
                        }
                        Switch(
                            checked = autoPauseSetting,
                            onCheckedChange = { TrackingService.autoPauseSetting.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OrangePrimary,
                                checkedTrackColor = OrangePrimary.copy(alpha = 0.4f),
                                uncheckedThumbColor = SlateTextSecondary,
                                uncheckedTrackColor = SlateCardSurfaceVariant
                            )
                        )
                    }
                }

                // GPS Simulation Route (Demo Mode) Card - Only visible in Debug builds
                if (com.example.BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("GPS Simulation Route (Demo Mode)", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                            Text("Great for testing segment matching directly in the web emulator!", fontSize = 11.sp, color = SlateTextSecondary)
                            Spacer(modifier = Modifier.height(12.dp))

                            val simulationOptions = listOf(
                                "None",
                                "Golden Gate Bridge Crossing",
                                "Twin Peaks Hill Climb",
                                "Presidio Loop Cycle",
                                "Hawk Hill Peak Climb"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                simulationOptions.forEach { route ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selectedSimulationRoute == route) NeonTealAccent.copy(alpha = 0.2f) else SlateCardSurfaceVariant)
                                            .clickable {
                                                viewModel.setSimulationRoute(route)
                                                if (route.contains("Cycle") || route.contains("Hill Peak")) {
                                                    viewModel.setRecordingSportType("ride")
                                                } else if (route != "None") {
                                                    viewModel.setRecordingSportType("run")
                                                }
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(route, fontWeight = FontWeight.Bold, color = SlateTextPrimary, fontSize = 13.sp)
                                        if (selectedSimulationRoute == route) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = NeonTealAccent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedSimulationRoute == "None" && !permissionState.allPermissionsGranted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RunColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, RunColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Location Warning",
                                tint = RunColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Location permission is required for Real Live GPS tracking. Tap START below to grant permission.",
                                fontSize = 12.sp,
                                color = SlateTextPrimary
                            )
                        }
                    }
                }
            }

            // Fixed bottom bar with Orange Primary button, visible on every screen size
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, SlateDarkBackground.copy(alpha = 0.95f), SlateDarkBackground),
                            startY = 0f,
                            endY = 40f
                        )
                    )
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                val isDemo = selectedSimulationRoute != "None" && selectedSimulationRoute != null
                val needsGpsFix = !isDemo && permissionState.allPermissionsGranted
                val hasGpsFix = gpsFixState != null

                Button(
                    onClick = {
                        if (selectedSimulationRoute == "None" && !permissionState.allPermissionsGranted) {
                            permissionState.launchMultiplePermissionRequest()
                        } else {
                            viewModel.startRecording()
                        }
                    },
                    enabled = sportType.isNotEmpty() && (!needsGpsFix || hasGpsFix),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangePrimary,
                        disabledContainerColor = OrangePrimary.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_recording_button"),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (needsGpsFix && !hasGpsFix) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Waiting for GPS...", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                        } else {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Start recording", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (!permissionState.allPermissionsGranted && selectedSimulationRoute == "None") "GRANT PERMISSION & START" else "START RECORDING",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            // Live Recording Screen Dashboard
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) Color.Red else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRecording) "RECORDING LIVE" else "PAUSED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) Color.Red else Color.Gray
                            )
                        }
                        Text(
                            text = sportType.uppercase() + " | " + activeGearName,
                            fontSize = 11.sp,
                            color = SlateTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Timer / Clock
                    Text("ELAPSED TIME", fontSize = 11.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatElapsedTime(durationSeconds),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = SlateTextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("DISTANCE", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.US, "%.2f km", distanceKm),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = OrangePrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("PACE", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (currentPaceString.isNotBlank()) currentPaceString else if (avgPaceString.isNotBlank()) avgPaceString else "--:--/km",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("SPEED", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.US, "%.1f km/h", currentSpeedKmh),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                        }
                    }
                }
            }

            val isDemo = selectedSimulationRoute != "None" && selectedSimulationRoute != null
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDemo) Color(0xFFFFA726).copy(alpha = 0.15f) else Color(0xFF66BB6A).copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(
                        1.dp, 
                        if (isDemo) Color(0xFFFFA726).copy(alpha = 0.4f) else Color(0xFF66BB6A).copy(alpha = 0.4f), 
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isDemo) Color(0xFFFFA726) else Color(0xFF66BB6A), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDemo) "🟠 DEMO MODE" else "🟢 LIVE GPS",
                            color = if (isDemo) Color(0xFFFFA726) else Color(0xFF66BB6A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    if (isDemo) {
                        Text(
                            text = selectedSimulationRoute ?: "",
                            color = SlateTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (trackpoints.isEmpty()) {
                        Text(
                            text = "Waiting for GPS...",
                            color = Color(0xFFFFA726),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = String.format(Locale.US, "Accuracy: %.1fm", gpsAccuracyMeters),
                            color = SlateTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actual Real OpenStreetMap Live Tracker with Polyline and Custom Overlays
            OSMMapView(
                points = trackpoints,
                isLiveTracking = isRecording,
                selectedSimulationRoute = selectedSimulationRoute,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SlateCardSurface)
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action row buttons (Play/Pause, Finish, Discard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isRecording) {
                    Button(
                        onClick = { viewModel.stopRecording() },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCardSurfaceVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = SlateTextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PAUSE", color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.resumeRecording() },
                        colors = ButtonDefaults.buttonColors(containerColor = RunColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Resume", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESUME", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = { showFinishDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp)
                        .testTag("finish_recording_button"),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, contentDescription = "Finish", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("FINISH WORKOUT", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    // Finish Notes Dialog
    if (showFinishDialog) {
        Dialog(onDismissRequest = { showFinishDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Finish Workout", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                    Text("Add a custom description to post onto your social feed!", fontSize = 12.sp, color = SlateTextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = activityNotes,
                        onValueChange = { activityNotes = it },
                        label = { Text("How did it feel?") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        placeholder = { Text("Felt great! Pushed the pace near the summit...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Privacy Settings", fontSize = 12.sp, fontWeight = FontWeight.Black, color = SlateTextSecondary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            Triple("Public", "🔓 Public", "Everyone"),
                            Triple("Friends Only", "👥 Friends", "Mutuals"),
                            Triple("Private", "🔒 Private", "Only you")
                        )
                        options.forEach { (value, label, desc) ->
                            val isSelected = selectedPrivacy == value
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) OrangePrimary.copy(alpha = 0.15f) else SlateCardSurfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = if (isSelected) OrangePrimary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPrivacy = value }
                                    .testTag("privacy_option_$value")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = 10.dp, horizontal = 4.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) OrangePrimary else SlateTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 8.sp,
                                        color = SlateTextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.discardRecording()
                                showFinishDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Discard Session")
                        }

                        Row {
                            TextButton(onClick = { showFinishDialog = false }) {
                                Text("Cancel", color = SlateTextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.finishRecording(activityNotes, selectedPrivacy)
                                    activityNotes = ""
                                    showFinishDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                            ) {
                                Text("Save & Share", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SportSelectorButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(alpha = 0.2f) else SlateCardSurfaceVariant)
            .border(
                1.dp,
                if (selected) color else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = if (selected) color else SlateTextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, color = if (selected) color else SlateTextSecondary, fontSize = 13.sp)
        }
    }
}

// ============================================================================
// Screen 4: Gear Tracking Manager (Shoes, Bikes, Wear bars)
// ============================================================================
@Composable
fun GearScreen(viewModel: SummitViewModel) {
    val gears by viewModel.gears.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var gearName by remember { mutableStateOf("") }
    var gearBrand by remember { mutableStateOf("") }
    var gearType by remember { mutableStateOf("shoes") } // "shoes" or "bike"
    var gearLimit by remember { mutableStateOf("") }
    var alertPercent by remember { mutableStateOf(85) } // default 85%
    var gearNotes by remember { mutableStateOf("") }

    var expandedGearId by remember { mutableStateOf<Int?>(null) }

    var activeSubTab by remember { mutableStateOf("profile") } // default to "profile"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Switcher Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SlateCardSurfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "profile" to "PROFILE & STATS",
                "gear" to "GEAR CLOSET"
            ).forEach { (tabId, tabName) ->
                val selected = activeSubTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) OrangePrimary else Color.Transparent)
                        .clickable { activeSubTab = tabId }
                        .padding(vertical = 10.dp)
                        .testTag("gear_screen_subtab_$tabId"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (selected) Color.White else SlateTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        if (activeSubTab == "profile") {
            ProfileSubTabScreen(viewModel)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gear Closet",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "Monitor total mileage on your sports gear.",
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("add_gear_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = "Add gear", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ADD GEAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            AchievementsSection(modifier = Modifier.fillMaxWidth())
        }

        item {
            Text(
                text = "ACTIVE ATHLETE GEAR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = OrangePrimary,
                letterSpacing = 1.5.sp
            )
        }

        if (gears.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.DirectionsRun, contentDescription = "No gear", tint = SlateTextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No gear registered yet", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        Text("Add your running shoes or bicycle to track mileage wear & tear automatically when you save workouts!", color = SlateTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(gears) { gear ->
                    val progress = (gear.currentMileageKm / gear.maxMileageKm).toFloat()
                    val thresholdFraction = gear.alertThresholdPercent / 100.0f
                    val isAlertTriggered = progress >= thresholdFraction
                    val progressColor = when {
                        isAlertTriggered -> Color(0xFFB3261E) // red alert
                        progress >= (thresholdFraction - 0.15f) -> Color(0xFFFF4E00) // orange near limit
                        else -> Color(0xFF6750A4) // sleek purple
                    }
                    val isExpanded = expandedGearId == gear.id

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedGearId = if (isExpanded) null else gear.id }
                            .border(
                                width = if (isAlertTriggered) 2.dp else 1.dp,
                                color = if (isAlertTriggered) Color(0xFFB3261E).copy(alpha = 0.8f) else SlateCardSurfaceVariant,
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Main content
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (gear.type == "shoes") RunColor.copy(alpha = 0.12f)
                                                else RideColor.copy(alpha = 0.12f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (gear.type == "shoes") Icons.Filled.DirectionsRun else Icons.Filled.DirectionsBike,
                                            contentDescription = "Gear type",
                                            tint = if (gear.type == "shoes") RunColor else RideColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = gear.name,
                                                fontWeight = FontWeight.Bold,
                                                color = SlateTextPrimary,
                                                fontSize = 16.sp
                                            )
                                            if (gear.isRetired) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("RETIRED", color = SlateTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${gear.brand} • Added ${SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(gear.dateAdded))}",
                                            fontSize = 12.sp,
                                            color = SlateTextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.retireGear(gear) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (gear.isRetired) Icons.Filled.SettingsBackupRestore else Icons.Filled.Block,
                                            contentDescription = "Retire gear",
                                            tint = SlateTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteGear(gear) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete gear",
                                            tint = Color(0xFFB3261E).copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text("TOTAL MILEAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary, letterSpacing = 0.5.sp)
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f", gear.currentMileageKm),
                                            fontWeight = FontWeight.Black,
                                            color = SlateTextPrimary,
                                            fontSize = 20.sp
                                        )
                                        Text(
                                            text = String.format(Locale.US, " / %.0f km", gear.maxMileageKm),
                                            color = SlateTextSecondary,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("LIFESPAN USED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary, letterSpacing = 0.5.sp)
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", minOf(100f, progress * 100)),
                                        fontSize = 16.sp,
                                        color = progressColor,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = minOf(1.0f, progress),
                                color = progressColor,
                                trackColor = SlateCardSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )

                            // replacement alert text
                            if (isAlertTriggered && !gear.isRetired) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFDE8E8), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFF8B4B4), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚠️", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ALERT: Reached replacement threshold (${gear.alertThresholdPercent}%). Consider changing this gear soon for optimal safety and performance.",
                                            color = Color(0xFF9B1C1C),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else if (progress >= (thresholdFraction - 0.15f) && !gear.isRetired) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFFCD34D), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⚡", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "CAUTION: Approaching replacement alert threshold of ${gear.alertThresholdPercent}%. Current mileage is ${String.format(Locale.US, "%.0f%%", progress * 100)}.",
                                            color = Color(0xFF92400E),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // EXPANDED REGION
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(14.dp))

                                // Interactive alert slider
                                Text(
                                    text = "ADJUST REPLACEMENT ALERT THRESHOLD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextSecondary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Alert when ${gear.alertThresholdPercent}% of limit is used",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SlateTextPrimary
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.0f km", gear.maxMileageKm * gear.alertThresholdPercent / 100.0),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = progressColor
                                    )
                                }
                                Slider(
                                    value = gear.alertThresholdPercent.toFloat(),
                                    onValueChange = { newVal ->
                                        coroutineScope.launch {
                                            viewModel.repository.updateGear(gear.copy(alertThresholdPercent = newVal.toInt()))
                                        }
                                    },
                                    valueRange = 50f..100f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(
                                        thumbColor = progressColor,
                                        activeTrackColor = progressColor,
                                        inactiveTrackColor = SlateCardSurfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Display notes if present
                                if (gear.notes.isNotEmpty()) {
                                    Text(
                                        text = "GEAR DETAILS & NOTES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateTextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF7F2FA), RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = gear.notes,
                                            fontSize = 12.sp,
                                            color = SlateTextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                // Gear Statistics Section
                                val gearActivities = activities.filter { it.gearId == gear.id }
                                Text(
                                    text = "ACTIVITY-BASED USAGE METRICS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextSecondary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF9F9FA), RoundedCornerShape(16.dp))
                                        .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(16.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("ACTIVITIES", fontSize = 8.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${gearActivities.size}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("AVG DISTANCE", fontSize = 8.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                        val avg = if (gearActivities.isEmpty()) 0.0 else gearActivities.sumOf { it.distanceKm } / gearActivities.size
                                        Text(String.format(Locale.US, "%.1f km", avg), fontSize = 16.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("ELEV CLIMBED", fontSize = 8.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                        Text(String.format(Locale.US, "%.0fm", gearActivities.sumOf { it.elevationGainM }), fontSize = 16.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Linked activities service log
                                Text(
                                    text = "WORKOUT SERVICE HISTORY LOG",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextSecondary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                if (gearActivities.isEmpty()) {
                                    Text(
                                        text = "No activities recorded on this gear yet. Go to Record tab to start tracking!",
                                        fontSize = 12.sp,
                                        color = SlateTextSecondary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        gearActivities.forEach { act ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFF3EDF7).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (act.sportType == "run") "🏃" else "🚴",
                                                        fontSize = 18.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = act.title,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SlateTextPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(act.timestamp)),
                                                            fontSize = 11.sp,
                                                            color = SlateTextSecondary
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = String.format(Locale.US, "+%.1f km", act.distanceKm),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = SlateTextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Hint to tap to expand
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tap to view statistics & service log",
                                        fontSize = 11.sp,
                                        color = SlateTextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.ExpandMore,
                                        contentDescription = "Expand",
                                        tint = SlateTextSecondary,
                                        modifier = Modifier.size(16.dp)
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

    // Add Gear Dialog
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Register New Gear", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = gearName,
                        onValueChange = { gearName = it },
                        label = { Text("Gear Name (e.g. Pegasus 40)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = gearBrand,
                        onValueChange = { gearBrand = it },
                        label = { Text("Brand (e.g. Nike)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Gear Type", fontWeight = FontWeight.Bold, color = SlateTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { gearType = "shoes" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (gearType == "shoes") RunColor else SlateCardSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.DirectionsRun, contentDescription = "Shoes")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shoes")
                        }
                        Button(
                            onClick = { gearType = "bike" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (gearType == "bike") RideColor else SlateCardSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.DirectionsBike, contentDescription = "Bike")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bike")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = gearLimit,
                        onValueChange = { gearLimit = it },
                        label = { Text("Limit Threshold (Km)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        placeholder = { Text("e.g. 500") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Replacement Alert Threshold", fontWeight = FontWeight.Bold, color = SlateTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(75, 80, 85, 90, 95).forEach { pct ->
                            val isSelected = alertPercent == pct
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) OrangePrimary else Color(0xFFF1EFF4))
                                    .clickable { alertPercent = pct }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$pct%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else SlateTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = gearNotes,
                        onValueChange = { gearNotes = it },
                        label = { Text("Notes / Descriptions (Optional)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        placeholder = { Text("e.g. weight, model year...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel", color = SlateTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val limitVal = gearLimit.toDoubleOrNull() ?: 0.0
                                if (gearName.trim().isNotEmpty() && gearBrand.trim().isNotEmpty() && limitVal > 0) {
                                    viewModel.addGear(gearName, gearBrand, gearType, limitVal, alertPercent, gearNotes)
                                    gearName = ""
                                    gearBrand = ""
                                    gearLimit = ""
                                    gearNotes = ""
                                    alertPercent = 85
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Screen 5: Segment Explorer & Leaderboards
// ============================================================================
@Composable
fun SegmentsScreen(viewModel: SummitViewModel) {
    val segments by viewModel.segments.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val useImperial by viewModel.useImperial.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf("history") } // "history" or "segments"
    var selectedSegmentId by remember { mutableStateOf<String?>(null) }
    val selectedSegment = segments.find { it.id == selectedSegmentId }

    val segmentEffortsState = selectedSegmentId?.let { sid ->
        viewModel.repository.getEffortsForSegment(sid).collectAsStateWithLifecycle(initialValue = emptyList())
    }
    val segmentEfforts = segmentEffortsState?.value ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (selectedSegmentId == null) {
            // Main sub-tabs selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateCardSurfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "history" to "ACTIVITY HISTORY",
                    "segments" to "PREDEFINED SEGMENTS"
                ).forEach { (tabId, tabName) ->
                    val selected = activeSubTab == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) OrangePrimary else Color.Transparent)
                            .clickable { activeSubTab = tabId }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (selected) Color.White else SlateTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            if (activeSubTab == "history") {
                // Activity History layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Activity History",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = "A records registry of all your secure offline workouts.",
                            fontSize = 13.sp,
                            color = SlateTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = SlateTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No workouts recorded yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = "Your workouts will sync and display here.",
                                fontSize = 12.sp,
                                color = SlateTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(activities) { activity ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.selectActivity(activity) }
                                    .testTag("activity_item_card_${activity.id}")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (activity.sportType == "run") RunColor.copy(alpha = 0.2f)
                                                        else RideColor.copy(alpha = 0.2f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (activity.sportType == "run") Icons.Default.DirectionsRun else Icons.Default.DirectionsBike,
                                                    contentDescription = null,
                                                    tint = if (activity.sportType == "run") RunColor else RideColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = activity.title,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = SlateTextPrimary
                                            )
                                        }

                                        Text(
                                            text = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(activity.timestamp)),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateTextSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Distance
                                        Column {
                                            Text("DISTANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                                            val distStr = if (useImperial) {
                                                String.format(Locale.US, "%.2f mi", activity.distanceKm * 0.621371)
                                            } else {
                                                String.format(Locale.US, "%.2f km", activity.distanceKm)
                                            }
                                            Text(distStr, fontSize = 15.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
                                        }

                                        // Duration
                                        Column {
                                            Text("DURATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                                            Text(formatElapsedTimeShort(activity.durationSeconds), fontSize = 15.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                                        }

                                        // Pace / Speed
                                        Column {
                                            Text(if (activity.sportType == "run") "PACE" else "SPEED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                                            val paceStr = if (activity.sportType == "run") {
                                                val factor = if (useImperial) 0.621371 else 1.0
                                                val effectiveDist = activity.distanceKm * factor
                                                if (effectiveDist > 0) {
                                                    val paceSeconds = (activity.durationSeconds / effectiveDist).toLong()
                                                    val m = paceSeconds / 60
                                                    val s = paceSeconds % 60
                                                    String.format(Locale.US, "%d:%02d /%s", m, s, if (useImperial) "mi" else "km")
                                                } else "0:00"
                                            } else {
                                                val speed = if (useImperial) activity.avgSpeedKmh * 0.621371 else activity.avgSpeedKmh
                                                String.format(Locale.US, "%.1f %s", speed, if (useImperial) "mph" else "km/h")
                                            }
                                            Text(paceStr, fontSize = 15.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                                        }

                                        // Calories
                                        Column {
                                            Text("CALORIES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                                            val cals = (activity.distanceKm * if (activity.sportType == "run") 65 else 45).toInt()
                                            Text("$cals kcal", fontSize = 15.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Main Listing of Predefined Segments
                Text(
                    text = "Predefined Segments",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = SlateTextPrimary
                )
                Text(
                    text = "Race yourself or other athletes on specific stretches of paths.",
                    fontSize = 13.sp,
                    color = SlateTextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(segments) { seg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(14.dp))
                                .clickable { selectedSegmentId = seg.id }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (seg.sportType == "run") RunColor.copy(alpha = 0.2f) else RideColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (seg.sportType == "run") Icons.Filled.DirectionsRun else Icons.Filled.DirectionsBike,
                                                contentDescription = "Sport icon",
                                                tint = if (seg.sportType == "run") RunColor else RideColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = seg.sportType.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (seg.sportType == "run") RunColor else RideColor,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(seg.name, fontWeight = FontWeight.Bold, color = SlateTextPrimary, fontSize = 16.sp)
                                    Text(String.format("Length: %.0f meters", seg.lengthM), fontSize = 12.sp, color = SlateTextSecondary)
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = "View detail", tint = SlateTextSecondary)
                            }
                        }
                    }
                }
            }
        } else {
            // Segment Detail and Leaderboard
            if (selectedSegment != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedSegmentId = null }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SlateTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Segment Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (selectedSegment.sportType == "run") Icons.Filled.DirectionsRun else Icons.Filled.DirectionsBike,
                                contentDescription = "Sport Type",
                                tint = OrangePrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(selectedSegment.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = String.format("Length: %.1f km", selectedSegment.lengthM / 1000.0),
                            fontSize = 14.sp,
                            color = SlateTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("GPS Bounds: Start (%.4f, %.4f) ➡️ End (%.4f, %.4f)", selectedSegment.startLat, selectedSegment.startLng, selectedSegment.endLat, selectedSegment.endLng),
                            fontSize = 11.sp,
                            color = SlateTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "PUBLIC LEADERBOARD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextSecondary,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Combined Seed + Real matched efforts
                val leaderboardEntries = remember(segmentEfforts) {
                    val list = mutableListOf<LeaderboardEntry>()
                    // Add some fun static seed athletes
                    list.add(LeaderboardEntry("Elena Rostova", 1, if (selectedSegment.sportType == "run") selectedSegment.lengthM / 4.8 else selectedSegment.lengthM / 12.0, false))
                    list.add(LeaderboardEntry("Markus Vance", 2, if (selectedSegment.sportType == "run") selectedSegment.lengthM / 4.2 else selectedSegment.lengthM / 10.5, false))

                    // Map real efforts
                    segmentEfforts.forEach { eff ->
                        list.add(LeaderboardEntry("You", 0, eff.elapsedTimeSeconds, eff.isPr))
                    }

                    // Sort by time
                    list.sortBy { it.elapsedSeconds }
                    list.mapIndexed { idx, entry -> entry.copy(rank = idx + 1) }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(leaderboardEntries) { entry ->
                        val isUser = entry.name == "You"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isUser) OrangePrimary.copy(alpha = 0.15f) else SlateCardSurface)
                                .border(
                                    1.dp,
                                    if (isUser) OrangePrimary else SlateCardSurfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (entry.rank == 1) PRColor else SlateCardSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = entry.rank.toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.rank == 1) Color.Black else SlateTextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(entry.name, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                        if (entry.isPr) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(PRColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("PR 👑", color = PRColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    val avgSpeedKmh = (selectedSegment.lengthM / entry.elapsedSeconds) * 3.6
                                    Text(String.format("Average Speed: %.1f km/h", avgSpeedKmh), fontSize = 11.sp, color = SlateTextSecondary)
                                }
                            }
                            Text(
                                text = formatElapsedTimeDouble(entry.elapsedSeconds),
                                fontWeight = FontWeight.Black,
                                color = if (isUser) OrangePrimary else SlateTextPrimary,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

data class LeaderboardEntry(
    val name: String,
    val rank: Int,
    val elapsedSeconds: Double,
    val isPr: Boolean
)

// ============================================================================
// Overlay: Comments Dialog
// ============================================================================
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CommentsDialog(viewModel: SummitViewModel) {
    val activeCommentsPostId by viewModel.activeCommentsPostId.collectAsStateWithLifecycle()
    val comments by viewModel.activeComments.collectAsStateWithLifecycle()
    val newCommentText by viewModel.newCommentText.collectAsStateWithLifecycle()

    if (activeCommentsPostId != null) {
        Dialog(onDismissRequest = { viewModel.closeComments() }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Post Comments", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                        IconButton(onClick = { viewModel.closeComments() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = SlateTextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Comments List
                    Box(modifier = Modifier.weight(1f)) {
                        if (comments.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No comments yet. Write a kudos thought!", color = SlateTextSecondary, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(comments) { comment ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (comment.userName == "You") OrangePrimary else SlateCardSurfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(comment.userName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(comment.userName, fontWeight = FontWeight.Bold, color = SlateTextPrimary, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(formatDate(comment.timestamp), fontSize = 10.sp, color = SlateTextSecondary)
                                            }
                                            Text(comment.commentText, color = SlateTextSecondary, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { viewModel.setNewCommentText(it) },
                            placeholder = { Text("Write a comment...", fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = SlateCardSurfaceVariant,
                                focusedTextColor = SlateTextPrimary,
                                unfocusedTextColor = SlateTextPrimary
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.postComment() },
                            modifier = Modifier
                                .testTag("post_comment_button")
                                .size(44.dp)
                                .background(OrangePrimary, CircleShape)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Helpers & Formatters
// ============================================================================
fun formatElapsedTime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

fun formatElapsedTimeShort(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    return if (hrs > 0) {
        "${hrs}h ${mins}m"
    } else {
        "${mins}m"
    }
}

fun formatElapsedTimeDouble(seconds: Double): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return String.format("%02d:%02d", mins, secs)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun MonthlySummaryDialog(
    activities: List<Activity>,
    onDismiss: () -> Unit
) {
    val now = java.util.Calendar.getInstance()
    val currentMonth = now.get(java.util.Calendar.MONTH)
    val currentYear = now.get(java.util.Calendar.YEAR)

    val monthName = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US).format(now.time).uppercase()

    val currentMonthActivities = activities.filter { activity ->
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = activity.timestamp
        cal.get(java.util.Calendar.MONTH) == currentMonth && cal.get(java.util.Calendar.YEAR) == currentYear
    }

    val totalDistKm = currentMonthActivities.sumOf { it.distanceKm }
    val totalElevM = currentMonthActivities.sumOf { it.elevationGainM }
    val totalSecs = currentMonthActivities.sumOf { it.durationSeconds }
    val activityCount = currentMonthActivities.size

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, OrangePrimary, RoundedCornerShape(24.dp))
                .padding(4.dp)
                .testTag("monthly_summary_dialog")
        ) {
            Column(
                modifier = Modifier
                    .background(SlateCardSurface)
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MONTHLY SUMMARY",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = OrangePrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = monthName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Distance card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCardSurfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Distance",
                                tint = OrangePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("DISTANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Text(
                                text = String.format("%.1f km", totalDistKm),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                        }
                    }

                    // Elevation Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCardSurfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terrain,
                                contentDescription = "Elevation",
                                tint = OrangeSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ELEVATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Text(
                                text = String.format("%.0f m", totalElevM),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time & Count Info
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCardSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ACTIVE TIME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Text(formatElapsedTimeShort(totalSecs), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ACTIVITIES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Text("$activityCount recorded", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CONTRIBUTING ACTIVITIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // List of contributors
                Box(modifier = Modifier.heightIn(max = 140.dp)) {
                    if (currentMonthActivities.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            Text("No activities logged yet in $monthName.", color = SlateTextSecondary, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(currentMonthActivities) { act ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SlateCardSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (act.sportType == "run") "🏃" else "🚴",
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.widthIn(max = 130.dp)) {
                                            Text(
                                                text = act.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SlateTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(java.util.Date(act.timestamp)),
                                                fontSize = 10.sp,
                                                color = SlateTextSecondary
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format("%.2f km", act.distanceKm),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SlateTextPrimary
                                        )
                                        Text(
                                            text = String.format("+%.0fm", act.elevationGainM),
                                            fontSize = 10.sp,
                                            color = SlateTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AnimatedCardContainer(
    key: Any,
    content: @Composable () -> Unit
) {
    var visible by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) + slideInVertically(
            initialOffsetY = { 60 },
            animationSpec = androidx.compose.animation.core.tween(500, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
        )
    ) {
        content()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OSMMapView(
    points: List<com.example.data.GPSPoint>,
    isLiveTracking: Boolean,
    selectedSimulationRoute: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var autoFollow by remember { mutableStateOf(true) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        org.osmdroid.config.Configuration.getInstance().load(
            context,
            android.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )
        org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

                    // Add rotation gesture overlay
                    val rotationGestureOverlay = RotationGestureOverlay(this).apply { isEnabled = true }
                    overlays.add(rotationGestureOverlay)

                    // Add compass overlay
                    val compassOverlay = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), this).apply {
                        enableCompass()
                    }
                    overlays.add(compassOverlay)

                    // Turn off auto-follow when user manually drags/scrolls the map
                    setOnTouchListener { v, event ->
                        if (event.action == android.view.MotionEvent.ACTION_MOVE) {
                            autoFollow = false
                        }
                        v.performClick()
                        false
                    }
                    
                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapViewRef = mapView
                
                // Clear existing dynamically added overlays (keeping rotation/compass overlays)
                val nonSystemOverlays = mapView.overlays.filter { 
                    it !is RotationGestureOverlay && it !is CompassOverlay 
                }
                mapView.overlays.removeAll(nonSystemOverlays)

                if (points.isNotEmpty()) {
                    // Draw Polyline (Summit Orange Route)
                    val polyline = Polyline(mapView).apply {
                        color = android.graphics.Color.parseColor("#FF5722") // Summit Orange Primary
                        width = 8f
                        setPoints(points.map { GeoPoint(it.lat, it.lng) })
                    }
                    mapView.overlays.add(polyline)

                    // Start Marker (Green Dot with translucent aura)
                    val startPt = points.first()
                    val startMarker = Marker(mapView).apply {
                        position = GeoPoint(startPt.lat, startPt.lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Start Location"
                        
                        val size = 48
                        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                        paint.color = 0xFF4CAF50.toInt() // Green
                        canvas.drawCircle(size / 2f, size / 2f, 8f, paint)
                        paint.color = 0x404CAF50.toInt() // Green Aura
                        canvas.drawCircle(size / 2f, size / 2f, 16f, paint)
                        icon = BitmapDrawable(context.resources, bitmap)
                    }
                    mapView.overlays.add(startMarker)

                    // Current / End Marker
                    val lastPt = points.last()
                    val lastMarker = Marker(mapView).apply {
                        position = GeoPoint(lastPt.lat, lastPt.lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = if (isLiveTracking) "Current Position" else "Finish Location"
                        
                        val size = 48
                        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                        
                        if (isLiveTracking) {
                            // Blue current location marker with translucent aura
                            paint.color = 0xFF007AFF.toInt() // modern blue
                            canvas.drawCircle(size / 2f, size / 2f, 10f, paint)
                            paint.color = 0x40007AFF.toInt() // Aura
                            canvas.drawCircle(size / 2f, size / 2f, 20f, paint)
                        } else {
                            // Red marker
                            paint.color = 0xFFE91E63.toInt()
                            canvas.drawCircle(size / 2f, size / 2f, 8f, paint)
                            paint.color = 0x40E91E63.toInt()
                            canvas.drawCircle(size / 2f, size / 2f, 16f, paint)
                        }
                        icon = BitmapDrawable(context.resources, bitmap)
                    }
                    mapView.overlays.add(lastMarker)

                    // Trigger Camera Animations / Fitting
                    if (isLiveTracking) {
                        if (autoFollow) {
                            val gp = GeoPoint(lastPt.lat, lastPt.lng)
                            mapView.controller.animateTo(gp)
                            if (mapView.zoomLevelDouble < 12.0) {
                                mapView.controller.setZoom(17.5)
                            }
                        }
                    } else {
                        // Zoom to fit the route bounds
                        val lats = points.map { it.lat }
                        val lngs = points.map { it.lng }
                        val minLat = lats.minOrNull() ?: 0.0
                        val maxLat = lats.maxOrNull() ?: 0.0
                        val minLng = lngs.minOrNull() ?: 0.0
                        val maxLng = lngs.maxOrNull() ?: 0.0
                        
                        val boundingBox = org.osmdroid.util.BoundingBox(maxLat, maxLng, minLat, minLng)
                        mapView.post {
                            try {
                                mapView.zoomToBoundingBox(boundingBox, true, 40)
                            } catch (e: Exception) {
                                val center = GeoPoint((maxLat + minLat) / 2.0, (maxLng + minLng) / 2.0)
                                mapView.controller.setCenter(center)
                                mapView.controller.setZoom(14.0)
                            }
                        }
                    }
                } else {
                    // Fallback/Default Center when no points yet (e.g. San Francisco)
                    val sfCenter = GeoPoint(37.7749, -122.4194)
                    mapView.controller.setCenter(sfCenter)
                    mapView.controller.setZoom(13.0)
                }
                mapView.invalidate()
            }
        )

        // Custom Overlay Controls: Zoom In, Zoom Out, Re-center
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(SlateDarkBackground.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { 
                    mapViewRef?.let {
                        val currentZoom = it.zoomLevelDouble
                        it.controller.setZoom(currentZoom + 1.0)
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = OrangePrimary)
            }
            IconButton(
                onClick = { 
                    mapViewRef?.let {
                        val currentZoom = it.zoomLevelDouble
                        it.controller.setZoom(currentZoom - 1.0)
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = OrangePrimary)
            }
            IconButton(
                onClick = { 
                    autoFollow = true
                    if (points.isNotEmpty()) {
                        val lastPt = points.last()
                        mapViewRef?.controller?.animateTo(GeoPoint(lastPt.lat, lastPt.lng))
                        mapViewRef?.controller?.setZoom(17.5)
                    } else {
                        mapViewRef?.controller?.animateTo(GeoPoint(37.7749, -122.4194))
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (autoFollow) Icons.Default.MyLocation else Icons.Default.LocationSearching, 
                    contentDescription = "Re-center", 
                    tint = if (autoFollow) NeonTealAccent else OrangePrimary
                )
            }
        }

        // Floating info badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color(0xBA1F2937), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                "🗺️ INTERACTIVE OPENSTREETMAP",
                fontSize = 9.sp,
                color = OrangePrimary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun InteractiveMapComponent(
    points: List<com.example.data.GPSPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .background(SlateCardSurfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No GPS route data recorded.", color = SlateTextSecondary, fontSize = 12.sp)
        }
        return
    }

    OSMMapView(
        points = points,
        isLiveTracking = false,
        selectedSimulationRoute = "Route Preview",
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SlateDarkBackground)
            .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(20.dp))
    )
}

data class ActivitySplit(
    val splitNum: Int,
    val distance: Double,
    val timeSeconds: Long,
    val avgSpeedKmhOrMph: Double,
    val paceString: String
)

fun calculateActivitySplits(points: List<com.example.data.GPSPoint>, useImperial: Boolean): List<ActivitySplit> {
    if (points.size < 2) return emptyList()
    
    val targetSegmentMeters = if (useImperial) 1609.34 else 1000.0
    val unitLabel = if (useImperial) "mi" else "km"
    val splits = mutableListOf<ActivitySplit>()
    
    var currentSplitDistanceM = 0.0
    var currentSplitTimeMs = 0L
    var splitIndex = 1
    var lastPoint = points.first()
    
    for (i in 1 until points.size) {
        val currentPoint = points[i]
        val segmentDistM = SegmentMatcher.haversineM(
            Pair(lastPoint.lat, lastPoint.lng),
            Pair(currentPoint.lat, currentPoint.lng)
        )
        val segmentTimeMs = currentPoint.timeMs - lastPoint.timeMs
        
        currentSplitDistanceM += segmentDistM
        if (segmentTimeMs > 0) {
            currentSplitTimeMs += segmentTimeMs
        }
        
        if (currentSplitDistanceM >= targetSegmentMeters) {
            val splitDist = if (useImperial) currentSplitDistanceM / 1609.34 else currentSplitDistanceM / 1000.0
            val splitTimeSec = currentSplitTimeMs / 1000
            
            val avgSpeed = if (splitTimeSec > 0) {
                val distKm = currentSplitDistanceM / 1000.0
                val hours = splitTimeSec / 3600.0
                val speedKmh = distKm / hours
                if (useImperial) speedKmh * 0.621371 else speedKmh
            } else {
                0.0
            }
            
            val paceMin = if (splitDist > 0 && splitTimeSec > 0) {
                (splitTimeSec / 60.0) / splitDist
            } else {
                0.0
            }
            val paceMinPart = paceMin.toInt()
            val paceSecPart = ((paceMin - paceMinPart) * 60).toInt()
            val paceStr = String.format(Locale.US, "%d:%02d/%s", paceMinPart, paceSecPart, unitLabel)
            
            splits.add(
                ActivitySplit(
                    splitNum = splitIndex++,
                    distance = splitDist,
                    timeSeconds = splitTimeSec,
                    avgSpeedKmhOrMph = avgSpeed,
                    paceString = paceStr
                )
            )
            
            currentSplitDistanceM = 0.0
            currentSplitTimeMs = 0L
        }
        
        lastPoint = currentPoint
    }
    
    if (currentSplitDistanceM > 50.0) {
        val splitDist = if (useImperial) currentSplitDistanceM / 1609.34 else currentSplitDistanceM / 1000.0
        val splitTimeSec = currentSplitTimeMs / 1000
        
        val avgSpeed = if (splitTimeSec > 0) {
            val distKm = currentSplitDistanceM / 1000.0
            val hours = splitTimeSec / 3600.0
            val speedKmh = distKm / hours
            if (useImperial) speedKmh * 0.621371 else speedKmh
        } else {
            0.0
        }
        
        val paceMin = if (splitDist > 0 && splitTimeSec > 0) {
            (splitTimeSec / 60.0) / splitDist
        } else {
            0.0
        }
        val paceMinPart = paceMin.toInt()
        val paceSecPart = ((paceMin - paceMinPart) * 60).toInt()
        val paceStr = String.format(Locale.US, "%d:%02d/%s", paceMinPart, paceSecPart, unitLabel)
        
        splits.add(
            ActivitySplit(
                splitNum = splitIndex,
                distance = splitDist,
                timeSeconds = splitTimeSec,
                avgSpeedKmhOrMph = avgSpeed,
                paceString = paceStr
            )
        )
    }
    
    if (splits.isEmpty() && points.size >= 2) {
        val totalDistM = points.zipWithNext { a, b ->
            SegmentMatcher.haversineM(Pair(a.lat, a.lng), Pair(b.lat, b.lng))
        }.sum()
        val totalTimeMs = maxOf(0L, points.last().timeMs - points.first().timeMs)
        val totalTimeSec = totalTimeMs / 1000
        val splitDist = if (useImperial) totalDistM / 1609.34 else totalDistM / 1000.0
        
        val avgSpeed = if (totalTimeSec > 0) {
            val distKm = totalDistM / 1000.0
            val hours = totalTimeSec / 3600.0
            val speedKmh = distKm / hours
            if (useImperial) speedKmh * 0.621371 else speedKmh
        } else {
            0.0
        }
        
        val paceMin = if (splitDist > 0 && totalTimeSec > 0) {
            (totalTimeSec / 60.0) / splitDist
        } else {
            0.0
        }
        val paceMinPart = paceMin.toInt()
        val paceSecPart = ((paceMin - paceMinPart) * 60).toInt()
        val paceStr = String.format(Locale.US, "%d:%02d/%s", paceMinPart, paceSecPart, unitLabel)
        
        splits.add(
            ActivitySplit(
                splitNum = 1,
                distance = splitDist,
                timeSeconds = totalTimeSec,
                avgSpeedKmhOrMph = avgSpeed,
                paceString = paceStr
            )
        )
    }
    
    return splits
}

@Composable
fun InteractiveCanvasChart(
    title: String,
    xValues: List<Double>,
    yValues: List<Double>,
    xLabelFormatter: (Double) -> String,
    yLabelFormatter: (Double) -> String,
    lineColor: Color = OrangePrimary,
    modifier: Modifier = Modifier
) {
    if (xValues.isEmpty() || yValues.isEmpty() || xValues.size != yValues.size) {
        Box(
            modifier = modifier
                .background(SlateCardSurfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Insufficient data for chart.", color = SlateTextSecondary, fontSize = 12.sp)
        }
        return
    }

    var activeIndex by remember { mutableStateOf<Int?>(null) }

    val minX = xValues.minOrNull() ?: 0.0
    val maxX = xValues.maxOrNull() ?: 1.0
    val minY = yValues.minOrNull() ?: 0.0
    val maxY = yValues.maxOrNull() ?: 1.0

    val rangeX = if (maxX - minX == 0.0) 1.0 else maxX - minX
    val rangeY = if (maxY - minY == 0.0) 1.0 else maxY - minY

    val paddingLeft = 50.dp
    val paddingRight = 16.dp
    val paddingTop = 24.dp
    val paddingBottom = 32.dp

    Column(
        modifier = modifier
            .background(SlateCardSurface, RoundedCornerShape(16.dp))
            .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
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
                color = SlateTextPrimary,
                letterSpacing = 0.5.sp
            )
            
            if (activeIndex != null && activeIndex!! in xValues.indices) {
                val xVal = xValues[activeIndex!!]
                val yVal = yValues[activeIndex!!]
                Text(
                    text = "${yLabelFormatter(yVal)} @ ${xLabelFormatter(xVal)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = lineColor
                )
            } else {
                Text(
                    text = "Drag to inspect",
                    fontSize = 10.sp,
                    color = SlateTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            val pLeft = with(density) { paddingLeft.toPx() }
            val pRight = with(density) { paddingRight.toPx() }
            val pTop = with(density) { paddingTop.toPx() }
            val pBottom = with(density) { paddingBottom.toPx() }

            val chartWidth = widthPx - pLeft - pRight
            val chartHeight = heightPx - pTop - pBottom

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val xInChart = offset.x - pLeft
                                if (xInChart in 0f..chartWidth) {
                                    val pct = xInChart / chartWidth
                                    val approxIndex = (pct * (xValues.size - 1))
                                        .roundToInt()
                                        .coerceIn(0, xValues.size - 1)
                                    activeIndex = approxIndex
                                }
                            },
                            onDrag = { change, _ ->
                                val xInChart = change.position.x - pLeft
                                if (xInChart in 0f..chartWidth) {
                                    val pct = xInChart / chartWidth
                                    val approxIndex = (pct * (xValues.size - 1))
                                        .roundToInt()
                                        .coerceIn(0, xValues.size - 1)
                                    activeIndex = approxIndex
                                }
                            },
                            onDragEnd = {
                                activeIndex = null
                            },
                            onDragCancel = {
                                activeIndex = null
                            }
                        )
                    }
            ) {
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#718096")
                    textSize = with(density) { 10.sp.toPx() }
                    isAntiAlias = true
                }

                for (j in 0..2) {
                    val yPct = j / 2f
                    val yPos = pTop + chartHeight * (1f - yPct)
                    val yVal = minY + rangeY * yPct
                    
                    drawLine(
                        color = SlateCardSurfaceVariant,
                        start = Offset(pLeft, yPos),
                        end = Offset(widthPx - pRight, yPos),
                        strokeWidth = 1f
                    )
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        yLabelFormatter(yVal),
                        10f,
                        yPos + 8f,
                        textPaint
                    )
                }

                val path = Path()
                val fillPath = Path()

                xValues.forEachIndexed { idx, x ->
                    val y = yValues[idx]
                    val xPct = (x - minX) / rangeX
                    val yPct = (y - minY) / rangeY
                    val px = pLeft + chartWidth * xPct.toFloat()
                    val py = pTop + chartHeight * (1f - yPct.toFloat())

                    if (idx == 0) {
                        path.moveTo(px, py)
                        fillPath.moveTo(px, chartHeight + pTop)
                        fillPath.lineTo(px, py)
                    } else {
                        path.lineTo(px, py)
                        fillPath.lineTo(px, py)
                    }
                    
                    if (idx == xValues.size - 1) {
                        fillPath.lineTo(px, chartHeight + pTop)
                        fillPath.close()
                    }
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                        startY = pTop,
                        endY = pTop + chartHeight
                    )
                )

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 4f,
                        join = StrokeJoin.Round,
                        cap = StrokeCap.Round
                    )
                )

                drawContext.canvas.nativeCanvas.drawText(
                    xLabelFormatter(minX),
                    pLeft,
                    heightPx - 10f,
                    textPaint
                )
                
                val endLabel = xLabelFormatter(maxX)
                val endLabelWidth = textPaint.measureText(endLabel)
                drawContext.canvas.nativeCanvas.drawText(
                    endLabel,
                    widthPx - pRight - endLabelWidth,
                    heightPx - 10f,
                    textPaint
                )

                activeIndex?.let { activeIdx ->
                    if (activeIdx in xValues.indices) {
                        val activeX = xValues[activeIdx]
                        val activeY = yValues[activeIdx]
                        val xPct = (activeX - minX) / rangeX
                        val yPct = (activeY - minY) / rangeY
                        val px = pLeft + chartWidth * xPct.toFloat()
                        val py = pTop + chartHeight * (1f - yPct.toFloat())

                        drawLine(
                            color = SlateTextSecondary.copy(alpha = 0.5f),
                            start = Offset(px, pTop),
                            end = Offset(px, pTop + chartHeight),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        drawCircle(
                            color = lineColor,
                            radius = 12f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumOSMMapView(
    points: List<com.example.data.GPSPoint>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

                    val rotationGestureOverlay = RotationGestureOverlay(this).apply { isEnabled = true }
                    overlays.add(rotationGestureOverlay)

                    val compassOverlay = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), this).apply {
                        enableCompass()
                    }
                    overlays.add(compassOverlay)

                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapViewRef = mapView
                val nonSystemOverlays = mapView.overlays.filter { 
                    it !is RotationGestureOverlay && it !is CompassOverlay 
                }
                mapView.overlays.removeAll(nonSystemOverlays)

                if (points.isNotEmpty()) {
                    val polyline = Polyline(mapView).apply {
                        color = android.graphics.Color.parseColor("#FF6A00")
                        width = 8f
                        setPoints(points.map { GeoPoint(it.lat, it.lng) })
                    }
                    mapView.overlays.add(polyline)

                    val startPt = points.first()
                    val startMarker = Marker(mapView).apply {
                        position = GeoPoint(startPt.lat, startPt.lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Start Location"
                        val size = 48
                        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                        paint.color = 0xFF4CAF50.toInt()
                        canvas.drawCircle(size / 2f, size / 2f, 8f, paint)
                        paint.color = 0x404CAF50.toInt()
                        canvas.drawCircle(size / 2f, size / 2f, 16f, paint)
                        icon = BitmapDrawable(context.resources, bitmap)
                    }
                    mapView.overlays.add(startMarker)

                    val lastPt = points.last()
                    val lastMarker = Marker(mapView).apply {
                        position = GeoPoint(lastPt.lat, lastPt.lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Finish Location"
                        val size = 48
                        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                        paint.color = 0xFFE91E63.toInt()
                        canvas.drawCircle(size / 2f, size / 2f, 8f, paint)
                        paint.color = 0x40E91E63.toInt()
                        canvas.drawCircle(size / 2f, size / 2f, 16f, paint)
                        icon = BitmapDrawable(context.resources, bitmap)
                    }
                    mapView.overlays.add(lastMarker)

                    val lats = points.map { it.lat }
                    val lngs = points.map { it.lng }
                    val minLat = lats.minOrNull() ?: 0.0
                    val maxLat = lats.maxOrNull() ?: 0.0
                    val minLng = lngs.minOrNull() ?: 0.0
                    val maxLng = lngs.maxOrNull() ?: 0.0
                    val boundingBox = org.osmdroid.util.BoundingBox(maxLat, maxLng, minLat, minLng)
                    mapView.post {
                        try {
                            mapView.zoomToBoundingBox(boundingBox, true, 40)
                        } catch (e: Exception) {
                            val center = GeoPoint((maxLat + minLat) / 2.0, (maxLng + minLng) / 2.0)
                            mapView.controller.setCenter(center)
                            mapView.controller.setZoom(14.0)
                        }
                    }
                }
                mapView.invalidate()
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    val mapView = mapViewRef ?: return@FloatingActionButton
                    if (points.isNotEmpty()) {
                        val lats = points.map { it.lat }
                        val lngs = points.map { it.lng }
                        val minLat = lats.minOrNull() ?: 0.0
                        val maxLat = lats.maxOrNull() ?: 0.0
                        val minLng = lngs.minOrNull() ?: 0.0
                        val maxLng = lngs.maxOrNull() ?: 0.0
                        val boundingBox = org.osmdroid.util.BoundingBox(maxLat, maxLng, minLat, minLng)
                        try {
                            mapView.zoomToBoundingBox(boundingBox, true, 40)
                        } catch (e: Exception) {
                            val center = GeoPoint((maxLat + minLat) / 2.0, (maxLng + minLng) / 2.0)
                            mapView.controller.setCenter(center)
                            mapView.controller.setZoom(14.0)
                        }
                    }
                },
                containerColor = SlateCardSurface,
                contentColor = OrangePrimary,
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Re-center Map",
                    modifier = Modifier.size(20.dp)
                )
            }

            FloatingActionButton(
                onClick = {
                    mapViewRef?.controller?.zoomIn()
                },
                containerColor = SlateCardSurface,
                contentColor = SlateTextPrimary,
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    modifier = Modifier.size(20.dp)
                )
            }

            FloatingActionButton(
                onClick = {
                    mapViewRef?.controller?.zoomOut()
                },
                containerColor = SlateCardSurface,
                contentColor = SlateTextPrimary,
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ActivityDetailDialog(
    activity: com.example.data.Activity,
    viewModel: SummitViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val useImperial by viewModel.useImperial.collectAsStateWithLifecycle()
    val points = remember(activity.routePointsJson) {
        try {
            JsonHelper.jsonToPoints(activity.routePointsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val efforts by viewModel.repository.getEffortsForActivity(activity.id).collectAsState(emptyList())
    
    var editTitle by remember(activity.id) { mutableStateOf(activity.title) }
    var editNotes by remember(activity.id) { mutableStateOf(activity.notes) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val estCalories = remember(activity.distanceKm, activity.sportType, activity.durationSeconds) {
        when (activity.sportType.lowercase()) {
            "run" -> (activity.distanceKm * 65).toInt()
            "ride" -> (activity.distanceKm * 32).toInt()
            "hike" -> (activity.distanceKm * 55).toInt()
            "walk" -> (activity.distanceKm * 48).toInt()
            "swim" -> ((activity.durationSeconds / 3600.0) * 600).toInt()
            else -> (activity.distanceKm * 50).toInt()
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Workout?", color = SlateTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this workout? This action cannot be undone.", color = SlateTextSecondary) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    onClick = {
                        viewModel.deleteActivity(activity)
                        showDeleteConfirmation = false
                        onDismiss()
                    }
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel", color = SlateTextSecondary)
                }
            },
            containerColor = SlateCardSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(16.dp)
                .border(1.5.dp, OrangePrimary, RoundedCornerShape(24.dp))
                .testTag("activity_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activity.sportType == "run") Color(0x20FF5E00) else Color(0x2000A2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (activity.sportType.lowercase()) {
                                    "run" -> Icons.Default.DirectionsRun
                                    "ride" -> Icons.Default.DirectionsBike
                                    "hike" -> Icons.Default.Terrain
                                    "walk" -> Icons.Default.DirectionsWalk
                                    else -> Icons.Default.FitnessCenter
                                },
                                contentDescription = null,
                                tint = if (activity.sportType == "run") OrangePrimary else Color(0xFF00A2FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = activity.sportType.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date(activity.timestamp)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = "Started at " + SimpleDateFormat("h:mm a", Locale.US).format(Date(activity.timestamp)),
                                fontSize = 11.sp,
                                color = SlateTextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Favorite Heart
                        IconButton(
                            onClick = { 
                                viewModel.updateActivity(activity.copy(isFavorite = !activity.isFavorite))
                            },
                            modifier = Modifier.size(36.dp).testTag("favorite_button")
                        ) {
                            Icon(
                                imageVector = if (activity.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (activity.isFavorite) Color(0xFFE91E63) else SlateTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(6.dp))

                        // Close Button
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = SlateTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = SlateCardSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Quick Stats summary in Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val distVal = if (useImperial) activity.distanceKm * 0.621371 else activity.distanceKm
                    Column {
                        Text("DISTANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                        Text(String.format(Locale.US, "%.2f %s", distVal, if (useImperial) "mi" else "km"), fontSize = 18.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                    }
                    Column {
                        Text("DURATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                        Text(formatElapsedTimeShort(activity.durationSeconds), fontSize = 18.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                    }
                    Column {
                        Text("CALORIES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                        Text("$estCalories kcal", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Map Section
                    Text("ROUTE MAP", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SlateTextSecondary, letterSpacing = 1.sp)
                    PremiumOSMMapView(
                        points = points,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, SlateCardSurfaceVariant, RoundedCornerShape(16.dp))
                    )

                    // Editable Title & Notes Section
                    Text("WORKOUT DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SlateTextSecondary, letterSpacing = 1.sp)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCardSurfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { 
                                    editTitle = it
                                    viewModel.updateActivity(activity.copy(title = it))
                                },
                                label = { Text("Workout Title", color = SlateTextSecondary, fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = SlateCardSurfaceVariant,
                                    focusedTextColor = SlateTextPrimary,
                                    unfocusedTextColor = SlateTextPrimary,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.fillMaxWidth().testTag("edit_title_input")
                            )

                            OutlinedTextField(
                                value = editNotes,
                                onValueChange = { 
                                    editNotes = it
                                    viewModel.updateActivity(activity.copy(notes = it))
                                },
                                label = { Text("Notes & Description", color = SlateTextSecondary, fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = SlateCardSurfaceVariant,
                                    focusedTextColor = SlateTextPrimary,
                                    unfocusedTextColor = SlateTextPrimary,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("edit_notes_input")
                            )
                        }
                    }

                    // Detailed Statistics Grid
                    Text("PERFORMANCE STATISTICS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SlateTextSecondary, letterSpacing = 1.sp)
                    val avgPaceMinPerKm = if (activity.distanceKm > 0) (activity.durationSeconds / 60.0) / activity.distanceKm else 0.0
                    val avgPaceStr = if (useImperial) {
                        val paceMinPerMi = avgPaceMinPerKm / 0.621371
                        val minPart = paceMinPerMi.toInt()
                        val secPart = ((paceMinPerMi - minPart) * 60).toInt()
                        String.format(Locale.US, "%d:%02d/mi", minPart, secPart)
                    } else {
                        val minPart = avgPaceMinPerKm.toInt()
                        val secPart = ((avgPaceMinPerKm - minPart) * 60).toInt()
                        String.format(Locale.US, "%d:%02d/km", minPart, secPart)
                    }
                    val speedUnit = if (useImperial) "mph" else "km/h"
                    val avgSpeedVal = if (useImperial) activity.avgSpeedKmh * 0.621371 else activity.avgSpeedKmh
                    val maxSpeedVal = if (useImperial) activity.maxSpeedKmh * 0.621371 else activity.maxSpeedKmh
                    val elevUnit = if (useImperial) "ft" else "m"

                    val elevationStats = remember(points) {
                        var gain = 0.0
                        var loss = 0.0
                        for (i in 1 until points.size) {
                            val diff = points[i].elevation - points[i-1].elevation
                            if (diff > 0) {
                                gain += diff
                            } else {
                                loss += -diff
                            }
                        }
                        Pair(gain, loss)
                    }
                    val finalElevationGain = if (elevationStats.first > 0.0) elevationStats.first else activity.elevationGainM
                    val finalElevationLoss = elevationStats.second

                    val statCards = remember(activity, points, useImperial, finalElevationGain, finalElevationLoss) {
                        val list = mutableListOf<Pair<String, String>>()
                        
                        list.add(Pair("AVERAGE PACE", avgPaceStr))
                        list.add(Pair("AVERAGE SPEED", String.format(Locale.US, "%.1f %s", avgSpeedVal, speedUnit)))
                        list.add(Pair("MAXIMUM SPEED", String.format(Locale.US, "%.1f %s", maxSpeedVal, speedUnit)))
                        list.add(Pair("MOVING TIME", formatElapsedTimeShort(activity.durationSeconds)))
                        list.add(Pair("ELAPSED TIME", formatElapsedTimeShort((activity.durationSeconds * 1.05).toLong())))
                        
                        if (points.isNotEmpty()) {
                            list.add(Pair("GPS ACCURACY", "High (3m)"))
                        }
                        
                        if (finalElevationGain > 0.0) {
                            val elevGainVal = if (useImperial) finalElevationGain * 3.28084 else finalElevationGain
                            list.add(Pair("ELEVATION GAIN", String.format(Locale.US, "+%.0f %s", elevGainVal, elevUnit)))
                        }
                        
                        if (finalElevationLoss > 0.0) {
                            val elevLossVal = if (useImperial) finalElevationLoss * 3.28084 else finalElevationLoss
                            list.add(Pair("ELEVATION LOSS", String.format(Locale.US, "-%.0f %s", elevLossVal, elevUnit)))
                        }
                        
                        list
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in statCards.indices step 2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateCardSurfaceVariant),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(statCards[i].first, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                                        Text(statCards[i].second, fontSize = 16.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                                    }
                                }
                                
                                if (i + 1 < statCards.size) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SlateCardSurfaceVariant),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(statCards[i + 1].first, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                                            Text(statCards[i + 1].second, fontSize = 16.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Splits Section
                    val splits = remember(points, useImperial) {
                        calculateActivitySplits(points, useImperial)
                    }
                    if (splits.isNotEmpty()) {
                        Text("1 ${if (useImperial) "MILE" else "KM"} SPLITS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SlateTextSecondary, letterSpacing = 1.sp)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCardSurfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("SPLIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary, modifier = Modifier.weight(1f))
                                    Text("DISTANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                    Text("TIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                    Text("PACE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                    Text("AVG SPEED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                }
                                Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                splits.forEach { split ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(OrangePrimary.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "${split.splitNum}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OrangePrimary
                                            )
                                        }
                                        Text(
                                            text = String.format(Locale.US, "%.2f %s", split.distance, if (useImperial) "mi" else "km"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SlateTextPrimary,
                                            modifier = Modifier.weight(1.5f),
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = formatElapsedTimeShort(split.timeSeconds),
                                            fontSize = 11.sp,
                                            color = SlateTextPrimary,
                                            modifier = Modifier.weight(1.5f),
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = split.paceString,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OrangeSecondary,
                                            modifier = Modifier.weight(1.5f),
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f %s", split.avgSpeedKmhOrMph, if (useImperial) "mph" else "km/h"),
                                            fontSize = 11.sp,
                                            color = SlateTextPrimary,
                                            modifier = Modifier.weight(1.5f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Performance Graphs
                    if (points.size >= 2) {
                        Text("PERFORMANCE ANALYSIS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SlateTextSecondary, letterSpacing = 1.sp)
                        val elapsedTimes = points.map { ((it.timeMs - points.first().timeMs) / 1000.0) }
                        val speeds = points.map {
                            if (useImperial) it.speedMps * 2.23694 else it.speedMps * 3.6
                        }
                        val paces = points.map {
                            if (it.speedMps > 0.3) {
                                val pace = if (useImperial) (1609.34 / it.speedMps) / 60.0 else (1000.0 / it.speedMps) / 60.0
                                pace.coerceAtMost(30.0)
                            } else {
                                30.0
                            }
                        }
                        val cumulativeDistances = remember(points, useImperial) {
                            var totalDist = 0.0
                            val list = mutableListOf<Double>()
                            list.add(0.0)
                            for (i in 1 until points.size) {
                                val d = com.example.data.SegmentMatcher.haversineM(
                                    Pair(points[i-1].lat, points[i-1].lng),
                                    Pair(points[i].lat, points[i].lng)
                                )
                                totalDist += if (useImperial) d / 1609.34 else d / 1000.0
                                list.add(totalDist)
                            }
                            list
                        }
                        val elevations = points.map {
                            if (useImperial) it.elevation * 3.28084 else it.elevation
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Pace vs Time
                            InteractiveCanvasChart(
                                title = "Pace vs Time",
                                xValues = elapsedTimes,
                                yValues = paces,
                                xLabelFormatter = { xVal ->
                                    val totalSec = xVal.toInt()
                                    String.format(Locale.US, "%d:%02d", totalSec / 60, totalSec % 60)
                                },
                                yLabelFormatter = { yVal ->
                                    val minPart = yVal.toInt()
                                    val secPart = ((yVal - minPart) * 60).toInt()
                                    String.format(Locale.US, "%d:%02d/%s", minPart, secPart, if (useImperial) "mi" else "km")
                                },
                                lineColor = OrangePrimary,
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                            )

                            // Speed vs Time
                            InteractiveCanvasChart(
                                title = "Speed vs Time",
                                xValues = elapsedTimes,
                                yValues = speeds,
                                xLabelFormatter = { xVal ->
                                    val totalSec = xVal.toInt()
                                    String.format(Locale.US, "%d:%02d", totalSec / 60, totalSec % 60)
                                },
                                yLabelFormatter = { yVal ->
                                    String.format(Locale.US, "%.1f %s", yVal, if (useImperial) "mph" else "km/h")
                                },
                                lineColor = NeonTealAccent,
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                            )

                            // Elevation vs Distance
                            val hasElevation = elevations.any { it > 0.0 }
                            if (hasElevation) {
                                InteractiveCanvasChart(
                                    title = "Elevation vs Distance",
                                    xValues = cumulativeDistances,
                                    yValues = elevations,
                                    xLabelFormatter = { xVal ->
                                        String.format(Locale.US, "%.2f %s", xVal, if (useImperial) "mi" else "km")
                                    },
                                    yLabelFormatter = { yVal ->
                                        String.format(Locale.US, "%.0f %s", yVal, if (useImperial) "ft" else "m")
                                    },
                                    lineColor = OrangeTertiary,
                                    modifier = Modifier.fillMaxWidth().height(220.dp)
                                )
                            }
                        }
                    }

                    // Export & Share Section
                    Text("EXPORT & SHARE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SlateTextSecondary, letterSpacing = 1.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { ExportUtils.exportGPX(context, activity) },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCardSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("export_gpx_button")
                        ) {
                            Text("GPX", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { ExportUtils.exportSingleCSV(context, activity) },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCardSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("export_csv_button")
                        ) {
                            Text("CSV", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { ExportUtils.exportSummaryImage(context, activity) },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f).height(44.dp).testTag("share_image_button")
                        ) {
                            Text("Share Card", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Delete Button
                    Button(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1CEF4444)),
                        border = BorderStroke(1.dp, Color(0x66EF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_activity_button")
                    ) {
                        Text("Delete Activity", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Done Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("DONE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ProfileSubTabScreen(viewModel: SummitViewModel) {
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val useImperial by viewModel.useImperial.collectAsStateWithLifecycle()

    // Edit Profile State
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editAvatar by remember { mutableStateOf("avatar_you") }
    var editHeight by remember { mutableStateOf("") }
    var editWeight by remember { mutableStateOf("") }
    var editBirthday by remember { mutableStateOf("") }
    var editGender by remember { mutableStateOf("") }

    // Change Password State
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    // Delete Account State
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Profile header card
        item {
            val user = loggedInUser
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Circle
                        val emoji = when (user?.avatar) {
                            "avatar_shoes" -> "🏃‍♂️"
                            "avatar_bike" -> "🚴‍♀️"
                            "avatar_fire" -> "🔥"
                            "avatar_trophy" -> "🏆"
                            "avatar_mountain" -> "🏔️"
                            else -> "⚡"
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(OrangePrimary.copy(alpha = 0.15f))
                                .border(2.dp, OrangePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 32.sp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.name ?: "Summit Athlete",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = user?.email ?: "offline@summit.io",
                                fontSize = 12.sp,
                                color = SlateTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(OrangePrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SUMMIT ATHLETE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OrangePrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Profile Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (user != null) {
                                    editName = user.name
                                    editAvatar = user.avatar
                                    editHeight = user.heightCm.toString()
                                    editWeight = user.weightKg.toString()
                                    editBirthday = user.birthday
                                    editGender = user.gender
                                }
                                showEditProfileDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCardSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("edit_profile_button")
                        ) {
                            Text("EDIT PROFILE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        }

                        Button(
                            onClick = { showChangePasswordDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCardSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("change_password_button")
                        ) {
                            Text("PASSWORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x15FF5E00)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("logout_button")
                        ) {
                            Text("LOGOUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                        }

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x15FF3B30)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("delete_account_button")
                        ) {
                            Text("DELETE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3B30))
                        }
                    }
                }
            }
        }

        // Athlete Details card
        item {
            val user = loggedInUser
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ATHLETE BIOMETRICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = OrangePrimary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Height
                        Column(modifier = Modifier.weight(1f)) {
                            Text("HEIGHT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            val hCm = user?.heightCm ?: 175.0
                            val heightStr = if (useImperial) {
                                val totalInches = (hCm / 2.54).toInt()
                                val ft = totalInches / 12
                                val inch = totalInches % 12
                                "$ft'$inch\""
                            } else {
                                "${hCm.toInt()} cm"
                            }
                            Text(heightStr, fontSize = 16.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                        }

                        // Weight
                        Column(modifier = Modifier.weight(1f)) {
                            Text("WEIGHT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            val wKg = user?.weightKg ?: 70.0
                            val weightStr = if (useImperial) {
                                String.format(Locale.US, "%.0f lbs", wKg * 2.20462)
                            } else {
                                "${wKg.toInt()} kg"
                            }
                            Text(weightStr, fontSize = 16.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Birthday
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BIRTHDAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            val bday = user?.birthday ?: "1990-01-01"
                            val formattedBday = try {
                                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(bday)
                                SimpleDateFormat("MMM dd, yyyy", Locale.US).format(parsed!!)
                            } catch (e: Exception) {
                                bday
                            }
                            Text(formattedBday, fontSize = 16.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                        }

                        // Gender
                        Column(modifier = Modifier.weight(1f)) {
                            Text("GENDER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Text(user?.gender ?: "Other", fontSize = 16.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                        }
                    }
                }
            }
        }

        // Workout Statistics card
        item {
            val totalWorkouts = activities.size
            val totalDistanceKm = activities.sumOf { it.distanceKm }
            val totalDurationSec = activities.sumOf { it.durationSeconds }
            val totalCalories = activities.sumOf { (it.distanceKm * if (it.sportType == "run") 65 else 45).toInt() }
            val longestWorkoutKm = activities.maxOfOrNull { it.distanceKm } ?: 0.0

            // Fastest pace
            val fastestPaceStr = remember(activities, useImperial) {
                val runActivities = activities.filter { it.sportType == "run" && it.distanceKm > 0 }
                val rideActivities = activities.filter { it.sportType == "ride" && it.distanceKm > 0 }
                
                val bestRunPace = runActivities.map { act ->
                    val factor = if (useImperial) 0.621371 else 1.0
                    val effectiveDist = act.distanceKm * factor
                    act.durationSeconds / effectiveDist
                }.minOrNull()

                val bestRideSpeed = rideActivities.map { act ->
                    if (useImperial) act.avgSpeedKmh * 0.621371 else act.avgSpeedKmh
                }.maxOrNull()

                val runText = if (bestRunPace != null) {
                    val m = (bestRunPace / 60).toLong()
                    val s = (bestRunPace % 60).toLong()
                    String.format(Locale.US, "%d:%02d /%s", m, s, if (useImperial) "mi" else "km")
                } else null

                val rideText = if (bestRideSpeed != null) {
                    String.format(Locale.US, "%.1f %s", bestRideSpeed, if (useImperial) "mph" else "km/h")
                } else null

                when {
                    runText != null && rideText != null -> "🏃 $runText | 🚴 $rideText"
                    runText != null -> "🏃 $runText"
                    rideText != null -> "🚴 $rideText"
                    else -> "N/A"
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ATHLETIC SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = OrangePrimary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total workouts
                        Column(modifier = Modifier.weight(1f)) {
                            Text("WORKOUTS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Text("$totalWorkouts", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                        }

                        // Total Distance
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DISTANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            val distStr = if (useImperial) {
                                String.format(Locale.US, "%.1f mi", totalDistanceKm * 0.621371)
                            } else {
                                String.format(Locale.US, "%.1f km", totalDistanceKm)
                            }
                            Text(distStr, fontSize = 20.sp, fontWeight = FontWeight.Black, color = OrangePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Duration
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DURATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            val hrs = totalDurationSec / 3600
                            val mins = (totalDurationSec % 3600) / 60
                            Text(
                                text = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                        }

                        // Calories
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CALORIES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Text("$totalCalories kcal", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Longest workout
                        Column(modifier = Modifier.weight(1f)) {
                            Text("LONGEST WORKOUT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            val maxDistStr = if (useImperial) {
                                String.format(Locale.US, "%.2f mi", longestWorkoutKm * 0.621371)
                            } else {
                                String.format(Locale.US, "%.2f km", longestWorkoutKm)
                            }
                            Text(maxDistStr, fontSize = 16.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                        }

                        // Fastest pace
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FASTEST PACE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                            Text(fastestPaceStr, fontSize = 14.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                        }
                    }
                }
            }
        }

        // Achievements Header Section (keeping existing AchievementsSection)
        item {
            AchievementsSection(modifier = Modifier.fillMaxWidth())
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        Dialog(onDismissRequest = { showEditProfileDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Edit Athlete Profile", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Profile Avatar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "avatar_you" to "⚡",
                            "avatar_shoes" to "🏃‍♂️",
                            "avatar_bike" to "🚴‍♀️",
                            "avatar_fire" to "🔥",
                            "avatar_trophy" to "🏆",
                            "avatar_mountain" to "🏔️"
                        ).forEach { (avId, avEmoji) ->
                            val selected = editAvatar == avId
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) OrangePrimary else SlateCardSurfaceVariant)
                                    .clickable { editAvatar = avId },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(avEmoji, fontSize = 20.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editHeight,
                        onValueChange = { editHeight = it },
                        label = { Text("Height (cm)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editWeight,
                        onValueChange = { editWeight = it },
                        label = { Text("Weight (kg)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editBirthday,
                        onValueChange = { editBirthday = it },
                        label = { Text("Birthday (YYYY-MM-DD)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Gender", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Male", "Female", "Other").forEach { gen ->
                            val selected = editGender == gen
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) OrangePrimary else SlateCardSurfaceVariant)
                                    .clickable { editGender = gen }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = gen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else SlateTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditProfileDialog = false }) {
                            Text("Cancel", color = SlateTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val hVal = editHeight.toDoubleOrNull() ?: 175.0
                                val wVal = editWeight.toDoubleOrNull() ?: 70.0
                                viewModel.editProfile(
                                    name = editName,
                                    avatar = editAvatar,
                                    heightCm = hVal,
                                    weightKg = wVal,
                                    birthday = editBirthday,
                                    gender = editGender,
                                    onSuccess = { showEditProfileDialog = false },
                                    onError = {}
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        Dialog(onDismissRequest = { showChangePasswordDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Change Password", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SlateTextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    var passError by remember { mutableStateOf<String?>(null) }

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = SlateCardSurfaceVariant,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (passError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(passError!!, color = Color.Red, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showChangePasswordDialog = false }) {
                            Text("Cancel", color = SlateTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (newPassword != confirmNewPassword) {
                                    passError = "New passwords do not match."
                                } else {
                                    viewModel.changePassword(
                                        oldPass = currentPassword,
                                        newPass = newPassword,
                                        onSuccess = {
                                            currentPassword = ""
                                            newPassword = ""
                                            confirmNewPassword = ""
                                            passError = null
                                            showChangePasswordDialog = false
                                        },
                                        onError = { passError = it }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Text("Change", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Delete Account Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Account permanently?", fontWeight = FontWeight.Black, color = SlateTextPrimary) },
            text = { Text("All your recorded offline workouts, athlete bio-metrics, and settings will be permanently erased. This cannot be undone.", color = SlateTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(
                            onSuccess = { showDeleteConfirmDialog = false },
                            onError = {}
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) {
                    Text("Delete Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = SlateTextSecondary)
                }
            },
            containerColor = SlateCardSurface
        )
    }
}
