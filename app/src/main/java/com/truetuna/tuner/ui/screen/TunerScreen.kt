package com.truetuna.tuner.ui.screen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truetuna.tuner.data.TuningRepository
import com.truetuna.tuner.model.Language
import com.truetuna.tuner.model.TETMode
import com.truetuna.tuner.model.TuningMode
import com.truetuna.tuner.ui.Strings
import com.truetuna.tuner.ui.theme.AmberWarm
import com.truetuna.tuner.ui.theme.CardSurface
import com.truetuna.tuner.ui.theme.NeonGreen
import com.truetuna.tuner.ui.theme.OledBlack
import com.truetuna.tuner.ui.theme.OrangeRedWarn
import com.truetuna.tuner.ui.theme.OrangeBright
import com.truetuna.tuner.ui.theme.OrangeVibrant
import com.truetuna.tuner.ui.theme.SurfaceDark
import com.truetuna.tuner.ui.theme.SurfaceVariant
import com.truetuna.tuner.ui.theme.TextMuted
import com.truetuna.tuner.ui.theme.TextPrimary
import com.truetuna.tuner.ui.theme.TextSecondary
import com.truetuna.tuner.viewmodel.TunerUiState
import com.truetuna.tuner.viewmodel.TunerViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import java.util.Locale

// Always use '.' as decimal separator (musical notation, e.g. "82.4 Hz") regardless of UI language.
private fun fmt(pattern: String, value: Float): String = String.format(Locale.US, pattern, value)

private fun signed(value: Float, decimals: Int = 1): String =
    String.format(Locale.US, "%+.${decimals}f", value)

// ─── Main Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerScreen(viewModel: TunerViewModel) {
    val state by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Haptic feedback when locking in tune
    val wasInTune = remember { mutableStateOf(false) }
    LaunchedEffect(state.pitchResult.isInTune) {
        if (state.pitchResult.isInTune && !wasInTune.value && state.pitchResult.frequency > 0f) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasInTune.value = state.pitchResult.isInTune
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onPermissionGranted() else viewModel.onPermissionDenied()
    }

    LaunchedEffect(state.hasPermission) {
        if (!state.hasPermission && !state.permissionDenied) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (state.hasPermission && !state.isListening) {
            viewModel.startListening()
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    var showTuningPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        if (!state.hasPermission) {
            PermissionScreen(
                state = state,
                onRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top bar ────────────────────────────────────────────────
                TopBar(
                    state = state,
                    onSettingsClick = { showSettings = true },
                    onLanguageToggle = {
                        val next = when (state.language) {
                            Language.RU -> Language.UA
                            Language.UA -> Language.EN
                            Language.EN -> Language.RU
                        }
                        viewModel.setLanguage(next)
                    }
                )

                Spacer(Modifier.height(8.dp))

                // ── Mode switcher ──────────────────────────────────────────
                ModeSwitcher(
                    mode = state.tuningMode,
                    lang = state.language,
                    onModeChange = { viewModel.setTuningMode(it) }
                )

                Spacer(Modifier.height(16.dp))

                // ── Main display ───────────────────────────────────────────
                NoteDisplay(state = state)

                Spacer(Modifier.height(8.dp))

                // ── Gauge ──────────────────────────────────────────────────
                TunerGauge(
                    centsDeviation = state.pitchResult.centsDeviation,
                    isInTune = state.pitchResult.isInTune,
                    isActive = state.pitchResult.frequency > 0f,
                    tetMode = state.tetMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                Spacer(Modifier.height(8.dp))

                // ── Status label ───────────────────────────────────────────
                TuningStatusLabel(state = state)

                Spacer(Modifier.height(12.dp))

                // ── Guitar string panel (guided mode only) ─────────────────
                if (state.tuningMode == TuningMode.GUITAR) {
                    GuitarPanel(
                        state = state,
                        onTuningPickerOpen = { showTuningPicker = true },
                        onStringLock = { viewModel.lockString(it) }
                    )
                }

                // ── TET mode chip (chromatic mode) ─────────────────────────
                if (state.tuningMode == TuningMode.CHROMATIC) {
                    Spacer(Modifier.height(4.dp))
                    TETModeChip(
                        tetMode = state.tetMode,
                        lang = state.language,
                        onToggle = {
                            viewModel.setTETMode(
                                if (state.tetMode == TETMode.TET_12) TETMode.TET_24 else TETMode.TET_12
                            )
                        }
                    )
                }

                Spacer(Modifier.weight(1f))

                // ── Frequency footer ───────────────────────────────────────
                FrequencyFooter(state = state)

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Settings bottom sheet ──────────────────────────────────────────────
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceDark
        ) {
            SettingsSheet(state = state, viewModel = viewModel, onDismiss = { showSettings = false })
        }
    }

    // ── Tuning picker bottom sheet ─────────────────────────────────────────
    if (showTuningPicker) {
        ModalBottomSheet(
            onDismissRequest = { showTuningPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            containerColor = SurfaceDark
        ) {
            TuningPickerSheet(
                state = state,
                onSelect = { tuning ->
                    viewModel.selectTuning(tuning)
                    showTuningPicker = false
                },
                onDismiss = { showTuningPicker = false }
            )
        }
    }
}

// ─── Permission Screen ────────────────────────────────────────────────────────

@Composable
private fun PermissionScreen(state: TunerUiState, onRequest: () -> Unit) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Mic icon drawn with Canvas
            Canvas(modifier = Modifier.size(80.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                drawCircle(
                    color = OrangeVibrant.copy(alpha = 0.15f),
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = OrangeVibrant,
                    radius = size.minDimension / 3.5f,
                    style = Stroke(width = 5f)
                )
                // Microphone body
                drawRoundRect(
                    color = OrangeVibrant,
                    topLeft = Offset(cx - 12.dp.toPx(), cy - 22.dp.toPx()),
                    size = Size(24.dp.toPx(), 32.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                )
                // Stand line
                drawLine(
                    color = OrangeVibrant,
                    start = Offset(cx, cy + 10.dp.toPx()),
                    end = Offset(cx, cy + 20.dp.toPx()),
                    strokeWidth = 3f
                )
                drawLine(
                    color = OrangeVibrant,
                    start = Offset(cx - 8.dp.toPx(), cy + 20.dp.toPx()),
                    end = Offset(cx + 8.dp.toPx(), cy + 20.dp.toPx()),
                    strokeWidth = 3f
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = Strings.permissionTitle(state.language),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (state.permissionDenied)
                    Strings.permissionDeniedMsg(state.language)
                else
                    Strings.permissionBody(state.language),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeVibrant)
            ) {
                Text(
                    if (state.permissionDenied) Strings.retry(state.language)
                    else Strings.permissionButton(state.language),
                    color = OledBlack,
                    fontWeight = FontWeight.Bold
                )
            }
            if (state.permissionDenied) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant)
                ) {
                    Text(
                        Strings.openSettings(state.language),
                        color = OrangeBright,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    state: TunerUiState,
    onSettingsClick: () -> Unit,
    onLanguageToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Language toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariant)
                .clickable { onLanguageToggle() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = state.language.name,
                color = OrangeBright,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // App name
        Text(
            text = "TrueTuna",
            style = MaterialTheme.typography.headlineLarge,
            color = OrangeVibrant,
            fontWeight = FontWeight.Black
        )

        // Settings gear (drawn with Canvas)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceVariant)
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("⚙", fontSize = 20.sp, color = TextSecondary)
        }
    }
}

// ─── Mode Switcher ─────────────────────────────────────────────────────────────

@Composable
private fun ModeSwitcher(
    mode: TuningMode,
    lang: Language,
    onModeChange: (TuningMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TuningMode.values().forEach { m ->
            val selected = mode == m
            val label = if (m == TuningMode.CHROMATIC) Strings.modeChromatic(lang) else Strings.modeGuitar(lang)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) OrangeVibrant else Color.Transparent)
                    .clickable { onModeChange(m) }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) OledBlack else TextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

// ─── Note Display ────────────────────────────────────────────────────────────

@Composable
private fun NoteDisplay(state: TunerUiState) {
    val pitch = state.pitchResult
    val isActive = pitch.frequency > 0f

    val noteColor by animateColorAsState(
        targetValue = when {
            !isActive -> TextMuted
            pitch.isInTune -> NeonGreen
            pitch.isClose -> AmberWarm
            else -> OrangeRedWarn
        },
        animationSpec = tween(300),
        label = "noteColor"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Main note display
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isActive) pitch.noteName else "—",
                fontSize = 96.sp,
                fontWeight = FontWeight.Black,
                color = noteColor,
                lineHeight = 96.sp
            )
            if (isActive) {
                Text(
                    text = pitch.octave.toString(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = noteColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )
                if (pitch.quarterToneBadge.isNotEmpty() && state.tuningMode == TuningMode.CHROMATIC) {
                    Text(
                        text = pitch.quarterToneBadge,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeBright,
                        modifier = Modifier.padding(bottom = 18.dp, start = 6.dp)
                    )
                }
            }
        }

        // Quarter-tone badge in 24-TET mode (chromatic only)
        if (state.tetMode == TETMode.TET_24 && state.tuningMode == TuningMode.CHROMATIC) {
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val onQuarter = isActive && pitch.quarterToneBadge.isNotEmpty()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(OrangeVibrant.copy(alpha = if (onQuarter) 0.30f else 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            !isActive -> "24-TET"
                            onQuarter -> "${pitch.quarterToneName}  ¼"
                            else -> "24-TET"
                        },
                        color = if (onQuarter) OrangeBright else OrangeVibrant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (onQuarter) FontWeight.Bold else FontWeight.Normal
                    )
                }
                if (isActive) {
                    Text(
                        text = "${signed(pitch.centsDeviation, 1)} ct",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// ─── Tuner Gauge ─────────────────────────────────────────────────────────────

@Composable
private fun TunerGauge(
    centsDeviation: Float,
    isInTune: Boolean,
    isActive: Boolean,
    tetMode: TETMode,
    modifier: Modifier = Modifier
) {
    val animatedCents by animateFloatAsState(
        targetValue = if (isActive) centsDeviation.coerceIn(-50f, 50f) else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cents"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isInTune && isActive) 1f else 0f,
        animationSpec = tween(400),
        label = "glow"
    )

    val needleColor = when {
        !isActive -> TextMuted
        isInTune -> NeonGreen
        abs(centsDeviation) <= 15f -> AmberWarm
        else -> OrangeRedWarn
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)

    Canvas(modifier = modifier.padding(horizontal = 16.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.85f  // pivot point near bottom

        val radius = minOf(w / 2f, cy) * 0.92f
        val arcStartAngle = 210f  // degrees, clockwise from +X (screen coordinates)
        val arcSweep = 120f       // total sweep = 120° covering -50..+50 cents
        val arcTopLeft = Offset(cx - radius, cy - radius)
        val arcSize = Size(radius * 2, radius * 2)

        fun angleFor(cents: Float): Double =
            Math.toRadians((arcStartAngle + arcSweep * (cents + 50f) / 100f).toDouble())

        // ── Arc track ───────────────────────────────────────────────────────
        drawArc(
            color = SurfaceVariant,
            startAngle = arcStartAngle,
            sweepAngle = arcSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )

        // ── In-tune glow ────────────────────────────────────────────────────
        if (glowAlpha > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonGreen.copy(alpha = 0.35f * glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
            drawArc(
                color = NeonGreen.copy(alpha = glowAlpha * 0.35f),
                startAngle = arcStartAngle,
                sweepAngle = arcSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // ── Centre "in tune" zone (±2.5 cents) ──────────────────────────────
        val greenSweep = arcSweep * 5f / 100f
        drawArc(
            color = NeonGreen.copy(alpha = 0.85f),
            startAngle = arcStartAngle + arcSweep / 2 - greenSweep / 2,
            sweepAngle = greenSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Butt)
        )

        // ── Major ticks: -50, -25, 0, +25, +50 ──────────────────────────────
        val tickValues = listOf(-50, -25, 0, 25, 50)
        val outerR = radius - 8.dp.toPx()
        tickValues.forEach { cents ->
            val angle = angleFor(cents.toFloat())
            val isCenter = cents == 0
            val isQuarterTone = cents == 25 || cents == -25
            val tickLength = if (isCenter) 20.dp.toPx() else if (isQuarterTone) 14.dp.toPx() else 16.dp.toPx()
            val innerR = outerR - tickLength

            drawLine(
                color = when {
                    isCenter -> NeonGreen
                    isQuarterTone -> OrangeVibrant.copy(alpha = 0.7f)
                    else -> OrangeBright   // ±50 — quarter-tone boundary markers, always visible
                },
                start = Offset(cx + outerR * cos(angle).toFloat(), cy + outerR * sin(angle).toFloat()),
                end = Offset(cx + innerR * cos(angle).toFloat(), cy + innerR * sin(angle).toFloat()),
                strokeWidth = if (isCenter) 3.dp.toPx() else 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Numeric label just inside the tick
            val label = when {
                cents > 0 -> "+$cents"
                else -> cents.toString()
            }
            val layout = textMeasurer.measure(label, labelStyle)
            val labelR = innerR - 12.dp.toPx()
            drawText(
                textLayoutResult = layout,
                color = if (isCenter) NeonGreen else TextSecondary,
                topLeft = Offset(
                    cx + labelR * cos(angle).toFloat() - layout.size.width / 2f,
                    cy + labelR * sin(angle).toFloat() - layout.size.height / 2f
                )
            )
        }

        // ── Minor ticks (every 5 cents; 24-TET also marks ±12.5 / ±37.5) ───
        val minorTicks = mutableListOf<Float>()
        for (c in -45..45 step 5) if (c % 25 != 0) minorTicks.add(c.toFloat())
        val minorLen = 6.dp.toPx()
        minorTicks.forEach { cents ->
            val angle = angleFor(cents)
            drawLine(
                color = TextMuted.copy(alpha = 0.5f),
                start = Offset(cx + outerR * cos(angle).toFloat(), cy + outerR * sin(angle).toFloat()),
                end = Offset(cx + (outerR - minorLen) * cos(angle).toFloat(), cy + (outerR - minorLen) * sin(angle).toFloat()),
                strokeWidth = 1.dp.toPx()
            )
        }
        if (tetMode == TETMode.TET_24) {
            listOf(-37.5f, -12.5f, 12.5f, 37.5f).forEach { cents ->
                val angle = angleFor(cents)
                drawLine(
                    color = OrangeVibrant.copy(alpha = 0.55f),
                    start = Offset(cx + outerR * cos(angle).toFloat(), cy + outerR * sin(angle).toFloat()),
                    end = Offset(cx + (outerR - 10.dp.toPx()) * cos(angle).toFloat(), cy + (outerR - 10.dp.toPx()) * sin(angle).toFloat()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }

        // ── Needle ──────────────────────────────────────────────────────────
        val needleAngle = angleFor(animatedCents)
        val needleLength = radius - 10.dp.toPx()
        val tip = Offset(
            cx + needleLength * cos(needleAngle).toFloat(),
            cy + needleLength * sin(needleAngle).toFloat()
        )

        if (isActive) {
            drawLine(
                color = needleColor.copy(alpha = 0.25f),
                start = Offset(cx, cy),
                end = tip,
                strokeWidth = 8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawLine(
            color = needleColor,
            start = Offset(cx, cy),
            end = tip,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Pivot
        drawCircle(color = needleColor, radius = 8.dp.toPx(), center = Offset(cx, cy))
        drawCircle(color = OledBlack, radius = 4.dp.toPx(), center = Offset(cx, cy))
    }
}

// ─── Tuning Status Label ──────────────────────────────────────────────────────

@Composable
private fun TuningStatusLabel(state: TunerUiState) {
    val pitch = state.pitchResult
    val lang = state.language
    val isActive = pitch.frequency > 0f

    val (statusText, statusColor) = when {
        !isActive -> (if (state.tuningMode == TuningMode.GUITAR) Strings.playString(lang) else Strings.silence(lang)) to TextMuted
        pitch.isInTune -> Strings.inTune(lang) to NeonGreen
        pitch.centsDeviation < 0 -> "${Strings.tooLow(lang)}  ${signed(pitch.centsDeviation, 0)} ct" to
                if (abs(pitch.centsDeviation) > 15f) OrangeRedWarn else AmberWarm
        else -> "${Strings.tooHigh(lang)}  ${signed(pitch.centsDeviation, 0)} ct" to
                if (abs(pitch.centsDeviation) > 15f) OrangeRedWarn else AmberWarm
    }

    val animColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(300),
        label = "statusColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(animColor.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = statusText,
            color = animColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── TET Mode Chip ────────────────────────────────────────────────────────────

@Composable
private fun TETModeChip(
    tetMode: TETMode,
    lang: Language,
    onToggle: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TETMode.values().forEach { mode ->
            val selected = tetMode == mode
            val label = if (mode == TETMode.TET_12) Strings.tetMode12(lang) else Strings.tetMode24(lang)
            FilterChip(
                selected = selected,
                onClick = { if (!selected) onToggle() },
                label = {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OrangeVibrant,
                    selectedLabelColor = OledBlack,
                    containerColor = SurfaceVariant,
                    labelColor = TextSecondary
                )
            )
        }
    }
}

// ─── Guitar Panel ─────────────────────────────────────────────────────────────

@Composable
private fun GuitarPanel(
    state: TunerUiState,
    onTuningPickerOpen: () -> Unit,
    onStringLock: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tuning selector header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Strings.selectTuning(state.language),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(OrangeVibrant.copy(alpha = 0.15f))
                    .clickable { onTuningPickerOpen() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = state.selectedTuning.name,
                    color = OrangeVibrant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Auto label
        if (state.lockedStringIndex == -1) {
            Text(
                text = "● ${Strings.autoDetect(state.language)}",
                color = OrangeBright,
                style = MaterialTheme.typography.labelMedium
            )
        }

        // String pegs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            state.selectedTuning.strings.forEachIndexed { index, guitarString ->
                val isLocked = state.lockedStringIndex == index
                val isActive = state.pitchResult.lockedStringIndex == index || state.lockedStringIndex == index
                val deviation = state.stringCentsDeviations.getOrNull(index)
                val deviationAbs = deviation?.let { abs(it) } ?: Float.MAX_VALUE

                val pegColor = when {
                    isLocked -> OrangeVibrant
                    deviation != null && deviationAbs <= 2.5f -> NeonGreen
                    deviation != null && deviationAbs <= 15f -> AmberWarm
                    deviation != null -> OrangeRedWarn
                    else -> SurfaceVariant
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onStringLock(index) }
                        .padding(4.dp)
                ) {
                    // String number circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(pegColor.copy(alpha = if (isLocked) 1f else 0.15f))
                            .border(
                                width = if (isLocked || isActive) 2.dp else 1.dp,
                                color = pegColor.copy(alpha = if (isLocked) 1f else 0.4f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = guitarString.noteName,
                                color = if (isLocked) OledBlack else pegColor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = guitarString.octave.toString(),
                                color = if (isLocked) OledBlack.copy(alpha = 0.7f) else pegColor.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Deviation indicator
                    if (deviation != null) {
                        Text(
                            text = signed(deviation, 0),
                            color = pegColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Frequency Footer ─────────────────────────────────────────────────────────

@Composable
private fun FrequencyFooter(state: TunerUiState) {
    val pitch = state.pitchResult
    val isActive = pitch.frequency > 0f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Frequency
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isActive) "${fmt("%.1f", pitch.frequency)} Hz" else "— Hz",
                color = if (isActive) OrangeBright else TextMuted,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = Strings.frequencyLabel(state.language),
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(1.dp)
                .background(SurfaceVariant)
        )

        // Cents deviation
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isActive) "${signed(pitch.centsDeviation, 1)} ct" else "— ct",
                color = when {
                    !isActive -> TextMuted
                    pitch.isInTune -> NeonGreen
                    abs(pitch.centsDeviation) <= 15f -> AmberWarm
                    else -> OrangeRedWarn
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = Strings.centsLabel(state.language),
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(1.dp)
                .background(SurfaceVariant)
        )

        // A4 reference
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "A4 = ${state.a4Hz.toInt()} Hz",
                color = TextSecondary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = Strings.referenceShort(state.language),
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ─── Settings Sheet ───────────────────────────────────────────────────────────

@Composable
private fun SettingsSheet(
    state: TunerUiState,
    viewModel: TunerViewModel,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            Strings.settings(state.language),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // ── A4 Reference ────────────────────────────────────────────────────
        Column {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                    Strings.referenceA4(state.language),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "${state.a4Hz.toInt()} Hz",
                    color = OrangeVibrant,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = state.a4Hz,
                onValueChange = { viewModel.setA4Hz(it) },
                valueRange = 415f..466f,
                steps = 50,
                colors = SliderDefaults.colors(
                    thumbColor = OrangeVibrant,
                    activeTrackColor = OrangeVibrant,
                    inactiveTrackColor = SurfaceVariant
                )
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("415 Hz", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                Text("466 Hz", color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }

        // ── Language ────────────────────────────────────────────────────────
        Column {
            Text("${Strings.languageLabel(state.language)} / Мова / Язык", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Language.values().forEach { lang ->
                    val selected = state.language == lang
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setLanguage(lang) },
                        label = { Text(lang.name, style = MaterialTheme.typography.labelLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeVibrant,
                            selectedLabelColor = OledBlack,
                            containerColor = SurfaceVariant,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        // ── TET Mode ────────────────────────────────────────────────────────
        Column {
            Text(Strings.quarterTone(state.language), color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TETMode.values().forEach { mode ->
                    val selected = state.tetMode == mode
                    val label = if (mode == TETMode.TET_12) Strings.tetMode12(state.language) else Strings.tetMode24(state.language)
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setTETMode(mode) },
                        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeVibrant,
                            selectedLabelColor = OledBlack,
                            containerColor = SurfaceVariant,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─── Tuning Picker Sheet ──────────────────────────────────────────────────────

@Composable
private fun TuningPickerSheet(
    state: TunerUiState,
    onSelect: (com.truetuna.tuner.model.GuitarTuning) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            Strings.selectTuning(state.language),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        TuningRepository.categories.forEach { catKey ->
            val tunings = TuningRepository.getByCategory(catKey)
            if (tunings.isEmpty()) return@forEach

            Text(
                text = Strings.categoryLabel(state.language, catKey),
                color = OrangeVibrant,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )

            tunings.forEach { tuning ->
                val isSelected = state.selectedTuning.name == tuning.name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) OrangeVibrant.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onSelect(tuning) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tuning.name,
                        color = if (isSelected) OrangeVibrant else TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    // String notes
                    Text(
                        text = tuning.strings.joinToString(" ") { "${it.noteName}${it.octave}" },
                        color = TextMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
