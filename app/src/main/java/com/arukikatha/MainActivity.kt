package com.arukikatha

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arukikatha.domain.ActiveSessionState
import com.arukikatha.domain.ArukikathaPhase
import com.arukikatha.ui.MainViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start the service immediately so the notification appears on app open
        viewModel.initService()

        setContent {
            MaterialTheme {
                val isDarkMode = isSystemInDarkTheme()
                val colors = appColors(isDarkMode)

                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.backgroundBrush)
                    ) {
                        val state by viewModel.sessionState.collectAsStateWithLifecycle()
                        var showLanding by rememberSaveable { mutableStateOf(true) }

                        val context = LocalContext.current
                        val launcher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            if (isGranted) viewModel.start()
                        }

                        LaunchedEffect(Unit) {
                            delay(600)
                            showLanding = false
                        }

                        val startSession = {
                            showLanding = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.start()
                                } else {
                                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.start()
                            }
                        }
                        val stopSession = {
                            viewModel.stop()
                        }

                        AnimatedContent(
                            targetState = showLanding,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                if (!targetState) {
                                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(500)))
                                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(500)))
                                } else {
                                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                                }.using(SizeTransform(clip = false))
                            },
                            label = "landingToWorkoutTransition"
                        ) { landingVisible ->
                            if (landingVisible) {
                                LandingScreen(isDarkMode = isDarkMode)
                            } else {
                                WorkoutScreen(
                                    state = state,
                                    isDarkMode = isDarkMode,
                                    onStart = startSession,
                                    onPause = viewModel::pause,
                                    onResume = viewModel::resume,
                                    onStop = stopSession
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandingScreen(
    isDarkMode: Boolean
) {
    val colors = appColors(isDarkMode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundBrush)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.55f))

        ArukikathaLogo(modifier = Modifier.size(144.dp))
        Spacer(modifier = Modifier.height(26.dp))
        Text(
            text = "Arukikatha",
            color = colors.text,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "3 min brisk. 3 min normal.",
            color = colors.subText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(0.45f))

        Text(
            text = "30 successful minutes",
            color = colors.subText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WorkoutScreen(
    state: ActiveSessionState,
    isDarkMode: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val colors = appColors(isDarkMode)
    val modeColor by animateColorAsState(
        targetValue = modeColor(state.phase),
        animationSpec = tween(durationMillis = 400),
        label = "modeColor"
    )

    val rawPhaseProgress = if (state.currentPhaseDurationMs <= 0) {
        1f
    } else {
        (1f - (state.phaseRemainingMs / state.currentPhaseDurationMs.toFloat())).coerceIn(0f, 1f)
    }
    val phaseProgress = remember { Animatable(rawPhaseProgress) }
    var lastPhase by remember { mutableStateOf(state.phase) }

    LaunchedEffect(state.phase, rawPhaseProgress) {
        if (state.phase != lastPhase) {
            if (phaseProgress.value > 0.1f) {
                phaseProgress.animateTo(1f, animationSpec = tween(durationMillis = 350))
            }
            phaseProgress.snapTo(0f)
            lastPhase = state.phase
        }

        if (rawPhaseProgress < phaseProgress.value - 0.05f) {
            phaseProgress.snapTo(rawPhaseProgress)
        }

        phaseProgress.animateTo(
            targetValue = rawPhaseProgress,
            animationSpec = tween(durationMillis = 150)
        )
    }
    val animatedGoalProgress by animateFloatAsState(
        targetValue = (state.successfulMinutes / 30f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "goalProgress"
    )
    val timerScale by animateFloatAsState(
        targetValue = if (state.isRunning && !state.isPaused) 1.015f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "timerScale"
    )
    val startPauseIcon = if (state.isRunning && !state.isPaused) Icons.Filled.Pause else Icons.Filled.PlayArrow
    val startPauseAction = {
        when {
            !state.isRunning || state.phase == ArukikathaPhase.COMPLETED -> onStart()
            state.isPaused -> onResume()
            else -> onPause()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundBrush)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.phase == ArukikathaPhase.COMPLETED) {
            CelebrationScreen(colors = colors, onDone = onStop)
        } else {
            // Header stays at the top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArukikathaLogo(modifier = Modifier.size(42.dp))
                    Column {
                        Text("Arukikatha", color = colors.text, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                        Text(modeLabel(state.phase), color = modeColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Center content area that expands to fill space
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TimerDial(
                    state = state,
                    textColor = colors.text,
                    subTextColor = colors.subText,
                    surfaceColor = colors.surface,
                    modeColor = modeColor,
                    phaseProgress = phaseProgress.value,
                    timerScale = timerScale,
                    startPauseIcon = startPauseIcon,
                    stopEnabled = state.isRunning,
                    startPauseEnabled = state.phase != ArukikathaPhase.COMPLETED || !state.isRunning,
                    onStartPause = startPauseAction,
                    onStop = onStop
                )

                Spacer(modifier = Modifier.height(76.dp))

                ProgressStatus(
                    state = state,
                    goalProgress = animatedGoalProgress,
                    textColor = colors.text,
                    subTextColor = colors.subText,
                    surfaceColor = colors.surface,
                    accentColor = Color(0xFF4FC3F7)
                )
            }

            // Ensure some padding at the bottom for safety
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CelebrationScreen(
    colors: AppColors,
    onDone: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "celebrationPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(1000)) + scaleIn(tween(1000))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }) {
                    ArukikathaLogo(modifier = Modifier.size(180.dp))
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "Congratulations!",
                    color = colors.text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "You've completed your routine.",
                    color = colors.subText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(48.dp))

                FilledTonalIconButton(
                    onClick = onDone,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xFF43A047),
                        contentColor = Color.White
                    )
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TimerDial(
    state: ActiveSessionState,
    textColor: Color,
    subTextColor: Color,
    surfaceColor: Color,
    modeColor: Color,
    phaseProgress: Float,
    timerScale: Float,
    startPauseIcon: androidx.compose.ui.graphics.vector.ImageVector,
    stopEnabled: Boolean,
    startPauseEnabled: Boolean,
    onStartPause: () -> Unit,
    onStop: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                scaleX = timerScale
                scaleY = timerScale
            }
    ) {
        // This Box provides a proportional aspect ratio so the timer scales with width
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize(0.95f)) {
                val strokeWidthPx = 10.dp.toPx()
                drawCircle(color = surfaceColor, style = Stroke(width = strokeWidthPx))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(modeColor.copy(alpha = 0.20f), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.46f
                    ),
                    radius = size.minDimension * 0.42f
                )
                drawArc(
                    color = modeColor,
                    startAngle = -90f,
                    sweepAngle = phaseProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (state.isPaused) "PAUSED" else "REMAINING",
                    color = subTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = formatTimerText(state.phaseRemainingMs),
                    color = textColor,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    FilledIconButton(
                        onClick = onStartPause,
                        enabled = startPauseEnabled,
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = modeColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = startPauseIcon,
                            contentDescription = "Start or pause",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onStop,
                        enabled = stopEnabled,
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = surfaceColor,
                            contentColor = textColor,
                            disabledContainerColor = surfaceColor.copy(alpha = 0.52f),
                            disabledContentColor = subTextColor.copy(alpha = 0.55f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Stop",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressStatus(
    state: ActiveSessionState,
    goalProgress: Float,
    textColor: Color,
    subTextColor: Color,
    surfaceColor: Color,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = state.lastResetMessage ?: sessionHint(state),
            color = if (state.lastResetMessage == null) textColor else Color(0xFF64B5F6),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        LinearProgressIndicator(
            progress = { goalProgress },
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(8.dp)
                .clip(CircleShape),
            color = accentColor,
            trackColor = surfaceColor
        )
        Text(
            text = "${state.successfulMinutes}/30 successful min",
            color = subTextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ArukikathaLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.075f
        val radius = size.minDimension * 0.34f
        drawCircle(color = Color(0xFFE53935).copy(alpha = 0.92f), radius = radius, center = center)
        drawCircle(color = Color(0xFF43A047), radius = radius * 0.72f, center = center)
        drawArc(
            color = Color(0xFF1E88E5),
            startAngle = -38f,
            sweepAngle = 248f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.36f, size.height * 0.55f),
            end = Offset(size.width * 0.48f, size.height * 0.38f),
            strokeWidth = stroke * 0.72f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.48f, size.height * 0.38f),
            end = Offset(size.width * 0.64f, size.height * 0.58f),
            strokeWidth = stroke * 0.72f,
            cap = StrokeCap.Round
        )
    }
}

private data class AppColors(
    val backgroundBrush: Brush,
    val text: Color,
    val subText: Color,
    val surface: Color
)

private fun appColors(isDarkMode: Boolean): AppColors {
    return if (isDarkMode) {
        AppColors(
            backgroundBrush = Brush.verticalGradient(
                listOf(Color(0xFF111318), Color(0xFF151D22), Color(0xFF0D0F13))
            ),
            text = Color.White,
            subText = Color(0xFFA8B7C8),
            surface = Color(0xFF27303A)
        )
    } else {
        AppColors(
            backgroundBrush = Brush.verticalGradient(
                listOf(Color(0xFFFAFBFC), Color(0xFFEFF5F3), Color(0xFFF8F9FA))
            ),
            text = Color(0xFF171A1D),
            subText = Color(0xFF5E6875),
            surface = Color(0xFFE2E8EC)
        )
    }
}

private fun modeColor(phase: ArukikathaPhase): Color {
    return when (phase) {
        ArukikathaPhase.BRISK -> Color(0xFFE53935)
        ArukikathaPhase.NORMAL -> Color(0xFF43A047)
        ArukikathaPhase.PAUSE_TO_NORMAL,
        ArukikathaPhase.PAUSE_TO_BRISK -> Color(0xFF1E88E5)
        ArukikathaPhase.COMPLETED -> Color(0xFF8E24AA)
    }
}

private fun formatTimerText(ms: Int) = buildAnnotatedString {
    val clamped = ms.coerceAtLeast(0)
    val totalSec = clamped / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val centiseconds = (clamped % 1000) / 10
    append("%02d:%02d".format(minutes, seconds))
    withStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Light)) {
        append(".%02d".format(centiseconds))
    }
}

private fun modeLabel(phase: ArukikathaPhase): String {
    return when (phase) {
        ArukikathaPhase.BRISK -> "Brisk walk"
        ArukikathaPhase.NORMAL -> "Normal walk"
        ArukikathaPhase.PAUSE_TO_NORMAL,
        ArukikathaPhase.PAUSE_TO_BRISK -> "Reset breath"
        ArukikathaPhase.COMPLETED -> "Session complete"
    }
}

private fun sessionHint(state: ActiveSessionState): String {
    val roundIndex = state.completedBriskCount + state.completedNormalCount + 1
    return when {
        state.phase == ArukikathaPhase.COMPLETED -> "Goal complete"
        state.isPaused -> "Round $roundIndex paused"
        else -> "Round $roundIndex"
    }
}
