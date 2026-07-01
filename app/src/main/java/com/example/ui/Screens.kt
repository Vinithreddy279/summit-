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

                    // Comments Overlay Dialog
                    if (activeCommentsPostId != null) {
                        CommentsDialog(viewModel = viewModel)
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
                .windowInsetsPadding(WindowInsets.safeContent),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Wordmark & Logo Grouping
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                SummitLogo(
                    modifier = Modifier
                        .size(96.dp)
                        .padding(bottom = 12.dp)
                )
                SummitWordmark(fontSize = 32.sp)
            }

            // Middle: Main Greeting & Subtitle inside a Glass Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "Welcome to Summit",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    val items = listOf(
                        "Track every trail." to Icons.Filled.DirectionsRun,
                        "Reach every peak." to Icons.Filled.FilterHdr,
                        "Create unforgettable adventures." to Icons.Filled.Explore
                    )
                    items.forEach { (text, icon) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color(0xFFFF6A00),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            // Footer Button: CTA Continue
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PremiumGradientButton(
                    onClick = {
                        viewModel.setAppFlow(SummitViewModel.AppFlow.MAIN)
                    },
                    text = "Continue →"
                )

                Text(
                    text = "By continuing, you agree to our Premium Explorer Terms.",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1).copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
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
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCardSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, OrangePrimary.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
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

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "VISUAL INTERFACE MODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Theme selection rows
                ThemeOptionRow(
                    label = "System Default",
                    description = "Sync automatically with device theme setting",
                    icon = "💻",
                    selected = themeMode == "system",
                    onClick = { viewModel.setThemeMode("system") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ThemeOptionRow(
                    label = "Midnight Dark",
                    description = "Sleek low-glare dark theme console",
                    icon = "🌌",
                    selected = themeMode == "dark",
                    onClick = { viewModel.setThemeMode("dark") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                ThemeOptionRow(
                    label = "Alpine Light",
                    description = "Radiant high-contrast light theme",
                    icon = "❄️",
                    selected = themeMode == "light",
                    onClick = { viewModel.setThemeMode("light") }
                )

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = SlateCardSurfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

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
                        text = "ACTIVE",
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

@Composable
fun DashboardScreen(viewModel: SummitViewModel) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SystemSettingsDialog(viewModel = viewModel, onDismiss = { showSettings = false })
    }

    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val gears by viewModel.gears.collectAsStateWithLifecycle()

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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("all", "run", "ride", "swim").forEach { sport ->
                        FilterChip(
                            label = sport.replaceFirstChar { it.uppercase() },
                            selected = sportFilter == sport,
                            onClick = { viewModel.setFeedSportFilter(sport) }
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
                items(filteredPosts) { post ->
                    if (compactMode) {
                        CompactFeedPostCard(post = post, onKudosClick = { viewModel.toggleKudos(post.id) }, onCommentClick = { viewModel.openComments(post.id) })
                    } else {
                        RichFeedPostCard(post = post, showStats = showStats, onKudosClick = { viewModel.toggleKudos(post.id) }, onCommentClick = { viewModel.openComments(post.id) })
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
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
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
fun RichFeedPostCard(post: FeedPost, showStats: Boolean, onKudosClick: () -> Unit, onCommentClick: () -> Unit) {
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
                    Text(post.userName, fontWeight = FontWeight.Bold, color = SlateTextPrimary, fontSize = 14.sp)
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
@Composable
fun RecordScreen(viewModel: SummitViewModel) {
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.recordingDurationSeconds.collectAsStateWithLifecycle()
    val distanceKm by viewModel.recordingDistanceKm.collectAsStateWithLifecycle()
    val sportType by viewModel.recordingSportType.collectAsStateWithLifecycle()
    val gears by viewModel.gears.collectAsStateWithLifecycle()
    val selectedGearId by viewModel.recordingGearId.collectAsStateWithLifecycle()
    val trackpoints by viewModel.recordingTrackpoints.collectAsStateWithLifecycle()
    val selectedSimulationRoute by viewModel.selectedSimulationRoute.collectAsStateWithLifecycle()

    var showFinishDialog by remember { mutableStateOf(false) }
    var activityNotes by remember { mutableStateOf("") }

    val activeGearName = gears.find { it.id == selectedGearId }?.name ?: "No Gear Selected"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!isRecording && trackpoints.isEmpty()) {
            // Configuration Setup Layout
            Text(
                text = "Record Activity",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = SlateTextPrimary
            )
            Text(
                text = "Establish your session settings before starting.",
                fontSize = 13.sp,
                color = SlateTextSecondary
            )

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

            // Simulation Route Selection Card (Incredible simulation!)
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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.startRecording() },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_recording_button"),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Start recording")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START RECORDING", fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp)
                }
            }
        } else {
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DISTANCE", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format("%.2f km", distanceKm),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = OrangePrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val avgSpeed = if (durationSeconds > 0) (distanceKm / (durationSeconds / 3600.0)) else 0.0
                            Text("AVG SPEED", fontSize = 10.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format("%.1f km/h", avgSpeed),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Map Drawing Canvas showing real route matching!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SlateCardSurface)
                    .border(1.dp, SlateCardSurfaceVariant, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (trackpoints.isEmpty()) {
                    Text("Acquiring GPS Signal...", color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                } else {
                    // Draw route
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pts = trackpoints
                        val lats = pts.map { it.lat }
                        val lngs = pts.map { it.lng }

                        val minLat = lats.minOrNull() ?: 0.0
                        val maxLat = lats.maxOrNull() ?: 0.0
                        val minLng = lngs.minOrNull() ?: 0.0
                        val maxLng = lngs.maxOrNull() ?: 0.0

                        val rangeLat = maxLat - minLat
                        val rangeLng = maxLng - minLng

                        val padding = 40.dp.toPx()
                        val drawWidth = size.width - (padding * 2)
                        val drawHeight = size.height - (padding * 2)

                        val pathPoints = pts.map { p ->
                            val x = if (rangeLng == 0.0) size.width / 2 else padding + ((p.lng - minLng) / rangeLng * drawWidth).toFloat()
                            // Invert y because canvas (0,0) is top-left
                            val y = if (rangeLat == 0.0) size.height / 2 else padding + ((maxLat - p.lat) / rangeLat * drawHeight).toFloat()
                            Offset(x, y)
                        }

                        // Draw path lines
                        if (pathPoints.size >= 2) {
                            val path = Path().apply {
                                moveTo(pathPoints[0].x, pathPoints[0].y)
                                for (i in 1 until pathPoints.size) {
                                    lineTo(pathPoints[i].x, pathPoints[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = OrangePrimary,
                                style = Stroke(width = 8f, cap = StrokeCap.Round)
                            )
                        }

                        // Start dot (Green)
                        if (pathPoints.isNotEmpty()) {
                            drawCircle(Color.Green, radius = 12f, center = pathPoints.first())
                        }
                        // Live dot (Teal glow)
                        if (pathPoints.size > 1) {
                            drawCircle(NeonTealAccent, radius = 14f, center = pathPoints.last())
                        }
                    }

                    // Floating simulation info
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(SlateDarkBackground.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Route: " + (selectedSimulationRoute ?: "Free Mode"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonTealAccent
                        )
                    }
                }
            }

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
                        onClick = { viewModel.startRecording() },
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

                    Spacer(modifier = Modifier.height(20.dp))

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
                                    viewModel.finishRecording(activityNotes)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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

        Spacer(modifier = Modifier.height(20.dp))

        if (gears.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.DirectionsRun, contentDescription = "No gear", tint = SlateTextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No gear registered yet", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                    Text("Add your running shoes or bicycle to track mileage wear & tear automatically when you save workouts!", color = SlateTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
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
            // Main Listing
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

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
