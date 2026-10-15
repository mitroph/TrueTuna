package com.truetuna.tuner.ui.screen

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.truetuna.tuner.model.DisplayMode
import com.truetuna.tuner.model.GuitarString
import com.truetuna.tuner.model.GuitarTuning
import com.truetuna.tuner.model.Instrument
import com.truetuna.tuner.model.Language
import com.truetuna.tuner.model.TETMode
import com.truetuna.tuner.model.TuningMode
import com.truetuna.tuner.model.withCapo
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.withFrameNanos
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
    var showMetronomeBar by remember { mutableStateOf(false) }
    var showCapoPicker by remember { mutableStateOf(false) }
    var showCustomTuningDialog by remember { mutableStateOf(false) }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

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
        } else if (isLandscape) {
            // ── Landscape dual-column layout (rack / bench tuner style) ────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopBar(
                    state = state,
                    isMetronomeOpen = showMetronomeBar || state.metronome.isRunning,
                    onToggleMetronome = { showMetronomeBar = !showMetronomeBar },
                    onToggleTone = { viewModel.playReferenceTone() },
                    onToggleDisplayMode = {
                        viewModel.setDisplayMode(
                            if (state.displayMode == DisplayMode.NEEDLE) DisplayMode.STROBE else DisplayMode.NEEDLE
                        )
                    },
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

                AnimatedVisibility(
                    visible = showMetronomeBar || state.metronome.isRunning,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    MetronomeBar(
                        metronome = state.metronome,
                        lang = state.language,
                        onToggle = { viewModel.toggleMetronome() },
                        onBpmChange = { viewModel.setMetronomeBpm(it) },
                        onBeatsChange = { viewModel.setMetronomeBeatsPerBar(it) },
                        onSoundToggle = { viewModel.setMetronomeSound(!state.metronome.isSoundEnabled) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left panel: Note Display + Strobe/Needle Gauge + Status
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        NoteDisplay(state = state)
                        Spacer(Modifier.height(6.dp))
                        if (state.displayMode == DisplayMode.STROBE) {
                            StrobeGauge(
                                centsDeviation = state.pitchResult.centsDeviation,
                                isInTune = state.pitchResult.isInTune,
                                isActive = state.pitchResult.frequency > 0f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )
                        } else {
                            TunerGauge(
                                centsDeviation = state.pitchResult.centsDeviation,
                                isInTune = state.pitchResult.isInTune,
                                isActive = state.pitchResult.frequency > 0f,
                                tetMode = state.tetMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        TuningStatusLabel(state = state)
                    }

                    // Right panel: Controls, String panel, and stats
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ModeSwitcher(
                            mode = state.tuningMode,
                            lang = state.language,
                            onModeChange = { viewModel.setTuningMode(it) }
                        )

                        if (state.tuningMode == TuningMode.GUITAR) {
                            Spacer(Modifier.height(8.dp))
                            GuitarPanel(
                                state = state,
                                onTuningPickerOpen = { showTuningPicker = true },
                                onCapoPickerOpen = { showCapoPicker = true },
                                onPlayTone = { viewModel.playReferenceTone() },
                                onStringLock = { viewModel.lockString(it) }
                            )
                        }

                        if (state.tuningMode == TuningMode.CHROMATIC) {
                            Spacer(Modifier.height(8.dp))
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

                        Spacer(Modifier.height(8.dp))
                        FrequencyFooter(state = state)
                    }
                }
            }
        } else {
            // ── Portrait single-column layout (default) ────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top bar ────────────────────────────────────────────────
                TopBar(
                    state = state,
                    isMetronomeOpen = showMetronomeBar || state.metronome.isRunning,
                    onToggleMetronome = { showMetronomeBar = !showMetronomeBar },
                    onToggleTone = { viewModel.playReferenceTone() },
                    onToggleDisplayMode = {
                        viewModel.setDisplayMode(
                            if (state.displayMode == DisplayMode.NEEDLE) DisplayMode.STROBE else DisplayMode.NEEDLE
                        )
                    },
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

                Spacer(Modifier.height(4.dp))

                // ── Metronome strip (collapsible) ──────────────────────────
                AnimatedVisibility(
                    visible = showMetronomeBar || state.metronome.isRunning,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    MetronomeBar(
                        metronome = state.metronome,
                        lang = state.language,
                        onToggle = { viewModel.toggleMetronome() },
                        onBpmChange = { viewModel.setMetronomeBpm(it) },
                        onBeatsChange = { viewModel.setMetronomeBeatsPerBar(it) },
                        onSoundToggle = { viewModel.setMetronomeSound(!state.metronome.isSoundEnabled) }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Mode switcher ──────────────────────────────────────────
                ModeSwitcher(
                    mode = state.tuningMode,
                    lang = state.language,
                    onModeChange = { viewModel.setTuningMode(it) }
                )

                Spacer(Modifier.height(14.dp))

                // ── Main display ───────────────────────────────────────────
                NoteDisplay(state = state)

                Spacer(Modifier.height(8.dp))

                // ── Gauge (Needle vs Strobe) ────────────────────────────────
                if (state.displayMode == DisplayMode.STROBE) {
                    StrobeGauge(
                        centsDeviation = state.pitchResult.centsDeviation,
                        isInTune = state.pitchResult.isInTune,
                        isActive = state.pitchResult.frequency > 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                } else {
                    TunerGauge(
                        centsDeviation = state.pitchResult.centsDeviation,
                        isInTune = state.pitchResult.isInTune,
                        isActive = state.pitchResult.frequency > 0f,
                        tetMode = state.tetMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Status label ───────────────────────────────────────────
                TuningStatusLabel(state = state)

                Spacer(Modifier.height(12.dp))

                // ── Guitar string panel (guided mode only) ─────────────────
                if (state.tuningMode == TuningMode.GUITAR) {
                    GuitarPanel(
                        state = state,
                        onTuningPickerOpen = { showTuningPicker = true },
                        onCapoPickerOpen = { showCapoPicker = true },
                        onPlayTone = { viewModel.playReferenceTone() },
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

                Spacer(Modifier.height(16.dp))

                // ── Frequency footer ───────────────────────────────────────
                FrequencyFooter(state = state)

                Spacer(Modifier.height(16.dp))
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
                onSelectInstrument = { viewModel.setInstrument(it) },
                onSelect = { tuning ->
                    viewModel.selectTuning(tuning)
                    showTuningPicker = false
                },
                onDeleteCustom = { viewModel.deleteCustomTuning(it) },
                onCreateCustomClick = { showCustomTuningDialog = true },
                onDismiss = { showTuningPicker = false }
            )
        }
    }

    // ── Capo picker bottom sheet ───────────────────────────────────────────
    if (showCapoPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCapoPicker = false },
            containerColor = SurfaceDark
        ) {
            CapoPickerSheet(
                currentFret = state.capoFret,
                lang = state.language,
                onSelectFret = { fret ->
                    viewModel.setCapo(fret)
                    showCapoPicker = false
                }
            )
        }
    }

    // ── Custom tuning creation dialog ──────────────────────────────────────
    if (showCustomTuningDialog) {
        CustomTuningDialog(
            instrument = state.selectedInstrument,
            lang = state.language,
            onDismiss = { showCustomTuningDialog = false },
            onSave = { tuning ->
                viewModel.saveCustomTuning(tuning)
                showCustomTuningDialog = false
            }
        )
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
    isMetronomeOpen: Boolean,
    onToggleMetronome: () -> Unit,
    onToggleTone: () -> Unit,
    onToggleDisplayMode: () -> Unit,
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
        // Left: Language & Display Mode
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant)
                    .clickable { onLanguageToggle() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = state.language.name,
                    color = OrangeBright,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.displayMode == DisplayMode.STROBE) OrangeVibrant.copy(alpha = 0.2f) else SurfaceVariant)
                    .clickable { onToggleDisplayMode() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (state.displayMode == DisplayMode.STROBE) "STROBE" else "NEEDLE",
                    color = if (state.displayMode == DisplayMode.STROBE) OrangeVibrant else TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center: App name
        Text(
            text = "TrueTuna",
            style = MaterialTheme.typography.headlineMedium,
            color = OrangeVibrant,
            fontWeight = FontWeight.Black
        )

        // Right: Tone, Metronome, Settings
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tone generator toggle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (state.isReferenceTonePlaying) OrangeVibrant else SurfaceVariant)
                    .clickable { onToggleTone() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔊",
                    fontSize = 16.sp
                )
            }

            // Metronome toggle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isMetronomeOpen || state.metronome.isRunning) OrangeVibrant else SurfaceVariant)
                    .clickable { onToggleMetronome() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⏱",
                    fontSize = 16.sp
                )
            }

            // Settings gear
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("⚙", fontSize = 18.sp, color = TextSecondary)
            }
        }
    }
}

// ─── Metronome Bar ────────────────────────────────────────────────────────────

@Composable
private fun MetronomeBar(
    metronome: com.truetuna.tuner.model.MetronomeState,
    lang: Language,
    onToggle: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onBeatsChange: (Int) -> Unit,
    onSoundToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Beat dots & Play/Stop
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Beat indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (b in 0 until metronome.beatsPerBar) {
                    val isCurrent = metronome.isRunning && metronome.currentBeat == b
                    val isAccent = b == 0
                    val dotColor = when {
                        isCurrent && isAccent -> NeonGreen
                        isCurrent -> OrangeBright
                        isAccent -> OrangeVibrant.copy(alpha = 0.5f)
                        else -> SurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 18.dp else 14.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                width = if (isAccent) 2.dp else 1.dp,
                                color = if (isCurrent) Color.White else dotColor,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Right side controls: Sound toggle & Start/Stop button
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (metronome.isSoundEnabled) SurfaceVariant else SurfaceVariant.copy(alpha = 0.5f))
                        .clickable { onSoundToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (metronome.isSoundEnabled) "🔊" else "🔇",
                        fontSize = 16.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (metronome.isRunning) OrangeRedWarn else OrangeVibrant)
                        .clickable { onToggle() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (metronome.isRunning) "■ STOP" else "▶ START",
                        color = OledBlack,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Row 2: BPM adjusters & Time signature
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BPM adjuster buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariant)
                        .clickable { onBpmChange(metronome.bpm - 5) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("-5", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariant)
                        .clickable { onBpmChange(metronome.bpm - 1) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("-1", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    text = "${metronome.bpm} BPM",
                    color = OrangeVibrant,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariant)
                        .clickable { onBpmChange(metronome.bpm + 1) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+1", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariant)
                        .clickable { onBpmChange(metronome.bpm + 5) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+5", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Time signature (2, 3, 4 beats)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(2, 3, 4).forEach { beats ->
                    val isSel = metronome.beatsPerBar == beats
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) OrangeVibrant else SurfaceVariant)
                            .clickable { onBeatsChange(beats) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$beats/4",
                            color = if (isSel) OledBlack else TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
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

// ─── Strobe Gauge ─────────────────────────────────────────────────────────────

@Composable
private fun StrobeGauge(
    centsDeviation: Float,
    isInTune: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    var phase by remember { mutableStateOf(0f) }

    LaunchedEffect(isActive, centsDeviation) {
        if (!isActive) return@LaunchedEffect
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now
                val speed = if (abs(centsDeviation) < 0.5f) 0f else centsDeviation * 2.2f
                phase = (phase + speed * dt) % 1f
                if (phase < 0f) phase += 1f
            }
        }
    }

    val barColor by animateColorAsState(
        targetValue = when {
            !isActive -> SurfaceVariant
            isInTune -> NeonGreen
            abs(centsDeviation) <= 15f -> AmberWarm
            else -> OrangeRedWarn
        },
        animationSpec = tween(200), label = "strobeBarColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val numBands = 14
            val bandWidth = w / numBands
            val currentPhase = if (isActive) phase else 0f
            val numTracks = 3
            val trackH = (h - (numTracks + 1) * 8.dp.toPx()) / numTracks

            for (t in 0 until numTracks) {
                val trackY = 8.dp.toPx() + t * (trackH + 8.dp.toPx())
                val mult = 1 shl t
                val trackPhase = (currentPhase * mult) % 1f
                val shift = trackPhase * bandWidth

                for (i in -2..numBands + 2) {
                    val x = i * bandWidth - shift
                    if ((i + t) % 2 == 0) {
                        drawRoundRect(
                            color = if (isInTune) NeonGreen else barColor.copy(alpha = if (t == 1) 0.9f else 0.5f),
                            topLeft = Offset(x, trackY),
                            size = Size(bandWidth * 0.7f, trackH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }

            // Center target reference line
            val cx = w / 2f
            drawLine(
                color = if (isInTune) NeonGreen else OrangeVibrant,
                start = Offset(cx, 0f),
                end = Offset(cx, h),
                strokeWidth = 3.dp.toPx()
            )
        }
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
    onCapoPickerOpen: () -> Unit,
    onPlayTone: () -> Unit,
    onStringLock: (Int) -> Unit
) {
    val effectiveTuning = if (state.capoFret > 0)
        state.selectedTuning.withCapo(state.capoFret, state.a4Hz)
    else state.selectedTuning

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tuning selector & Capo / Tone header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Instrument name + Tuning selector
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTuningPickerOpen() }
                    .padding(4.dp)
            ) {
                Text(
                    text = Strings.instrumentName(state.language, state.selectedInstrument),
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = state.selectedTuning.name,
                    color = OrangeVibrant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Capo button & Reference Tone button
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Capo button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.capoFret > 0) OrangeVibrant.copy(alpha = 0.25f) else SurfaceVariant)
                        .clickable { onCapoPickerOpen() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (state.capoFret > 0) "${Strings.capo(state.language)}: +${state.capoFret}" else Strings.capo(state.language),
                        color = if (state.capoFret > 0) OrangeBright else TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Reference tone for current string
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isReferenceTonePlaying) OrangeVibrant else SurfaceVariant)
                        .clickable { onPlayTone() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (state.isReferenceTonePlaying) "■ Tone" else "▶ Tone",
                        color = if (state.isReferenceTonePlaying) OledBlack else TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Auto / Locked label
        if (state.lockedStringIndex == -1) {
            Text(
                text = "● ${Strings.autoDetect(state.language)}",
                color = OrangeBright,
                style = MaterialTheme.typography.labelMedium
            )
        } else {
            Text(
                text = "🔒 ${Strings.lockedLabel(state.language)}: ${effectiveTuning.strings.getOrNull(state.lockedStringIndex)?.let { "${it.noteName}${it.octave}" } ?: ""}",
                color = OrangeVibrant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // String pegs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            effectiveTuning.strings.forEachIndexed { index, guitarString ->
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
                    // String circle
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

        // ── Display Mode ────────────────────────────────────────────────────
        Column {
            Text(Strings.displayMode(state.language), color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DisplayMode.values().forEach { mode ->
                    val selected = state.displayMode == mode
                    val label = if (mode == DisplayMode.NEEDLE) Strings.modeNeedle(state.language) else Strings.modeStrobe(state.language)
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setDisplayMode(mode) },
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

        // ── In-Tune Chime ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Strings.inTuneChime(state.language), color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = state.inTuneChimeEnabled,
                onCheckedChange = { viewModel.setInTuneChime(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OledBlack,
                    checkedTrackColor = OrangeVibrant,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceVariant
                )
            )
        }

        // ── Harmonic Low-Pass Filter ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Strings.harmonicFilter(state.language), color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = state.lowPassFilterEnabled,
                onCheckedChange = { viewModel.setLowPassFilter(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OledBlack,
                    checkedTrackColor = OrangeVibrant,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceVariant
                )
            )
        }

        // ── Noise Gate ──────────────────────────────────────────────────────
        Column {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                    Strings.noiseGate(state.language),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "${state.noiseGateDbfs.toInt()} dB",
                    color = OrangeBright,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = state.noiseGateDbfs,
                onValueChange = { viewModel.setNoiseGate(it) },
                valueRange = -80f..-20f,
                steps = 60,
                colors = SliderDefaults.colors(
                    thumbColor = OrangeVibrant,
                    activeTrackColor = OrangeVibrant,
                    inactiveTrackColor = SurfaceVariant
                )
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("-80 dB", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                Text("-20 dB", color = TextMuted, style = MaterialTheme.typography.labelSmall)
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
    onSelectInstrument: (Instrument) -> Unit,
    onSelect: (GuitarTuning) -> Unit,
    onDeleteCustom: (GuitarTuning) -> Unit,
    onCreateCustomClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            Strings.selectTuning(state.language),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Instrument Tabs / Scrollable Row
        Text(
            text = Strings.instrumentLabel(state.language),
            color = TextMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Instrument.values().forEach { inst ->
                val isSelected = state.selectedInstrument == inst
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectInstrument(inst) },
                    label = {
                        Text(
                            text = Strings.instrumentName(state.language, inst),
                            style = MaterialTheme.typography.labelMedium
                        )
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

        Spacer(Modifier.height(4.dp))

        // Categories for current instrument
        val categories = TuningRepository.categoriesForInstrument(state.selectedInstrument)
        categories.forEach { catKey ->
            val tunings = TuningRepository.getByCategory(catKey, state.selectedInstrument)
            if (tunings.isNotEmpty()) {
                Text(
                    text = Strings.categoryLabel(state.language, catKey),
                    color = OrangeVibrant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )

                tunings.forEach { tuning ->
                    val isSelected = state.selectedTuning.name == tuning.name && state.selectedTuning.instrument == tuning.instrument
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
                        Text(
                            text = tuning.strings.joinToString(" ") { "${it.noteName}${it.octave}" },
                            color = TextMuted,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Custom Tunings Section
        val customs = TuningRepository.customTunings.filter { it.instrument == state.selectedInstrument }
        Text(
            text = Strings.categoryLabel(state.language, "custom"),
            color = OrangeVibrant,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
        )

        customs.forEach { tuning ->
            val isSelected = state.selectedTuning.name == tuning.name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) OrangeVibrant.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelect(tuning) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = tuning.name,
                        color = if (isSelected) OrangeVibrant else TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = tuning.strings.joinToString(" ") { "${it.noteName}${it.octave}" },
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onDeleteCustom(tuning) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🗑", fontSize = 14.sp)
                }
            }
        }

        // Button to create custom tuning
        Button(
            onClick = onCreateCustomClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant)
        ) {
            Text("+ ${Strings.categoryLabel(state.language, "custom")}", color = OrangeBright, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Capo Picker Sheet ─────────────────────────────────────────────────────────

@Composable
private fun CapoPickerSheet(
    currentFret: Int,
    lang: Language,
    onSelectFret: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = Strings.capo(lang),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (0..9).forEach { fret ->
                val isSelected = currentFret == fret
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectFret(fret) },
                    label = {
                        Text(
                            text = if (fret == 0) Strings.noCapo(lang) else Strings.capoFret(lang, fret),
                            style = MaterialTheme.typography.labelLarge
                        )
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
}

// ─── Custom Tuning Dialog ─────────────────────────────────────────────────────

@Composable
private fun CustomTuningDialog(
    instrument: Instrument,
    lang: Language,
    onDismiss: () -> Unit,
    onSave: (GuitarTuning) -> Unit
) {
    var tuningName by remember { mutableStateOf("") }
    val stringCount = instrument.stringCount

    val notes = remember {
        mutableStateOf(
            List(stringCount) { i ->
                when (instrument) {
                    Instrument.GUITAR_6 -> listOf("E" to 2, "A" to 2, "D" to 3, "G" to 3, "B" to 3, "E" to 4)[i]
                    Instrument.BASS_4 -> listOf("E" to 1, "A" to 1, "D" to 2, "G" to 2)[i]
                    Instrument.BASS_5 -> listOf("B" to 0, "E" to 1, "A" to 1, "D" to 2, "G" to 2)[i]
                    Instrument.VIOLIN -> listOf("G" to 3, "D" to 4, "A" to 4, "E" to 5)[i]
                    Instrument.CELLO -> listOf("C" to 2, "G" to 2, "D" to 3, "A" to 3)[i]
                    else -> listOf("G" to 4, "C" to 4, "E" to 4, "A" to 4)[i]
                }
            }.toMutableList()
        )
    }

    val noteOptions = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Create Custom Tuning", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tuningName,
                    onValueChange = { tuningName = it },
                    label = { Text("Tuning Name (e.g. Open D)", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = OrangeVibrant,
                        unfocusedBorderColor = SurfaceVariant
                    )
                )

                Text("Strings (Low to High):", color = TextSecondary, style = MaterialTheme.typography.labelMedium)

                for (idx in 0 until stringCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Str ${idx + 1}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        // Note cycler
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariant)
                                .clickable {
                                    val current = notes.value[idx].first
                                    val nextIdx = (noteOptions.indexOf(current) + 1) % noteOptions.size
                                    val updated = notes.value.toMutableList()
                                    updated[idx] = noteOptions[nextIdx] to updated[idx].second
                                    notes.value = updated
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(notes.value[idx].first, color = OrangeBright, fontWeight = FontWeight.Bold)
                        }
                        // Octave cycler
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariant)
                                .clickable {
                                    val currentOct = notes.value[idx].second
                                    val nextOct = if (currentOct >= 6) 1 else currentOct + 1
                                    val updated = notes.value.toMutableList()
                                    updated[idx] = updated[idx].first to nextOct
                                    notes.value = updated
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Oct ${notes.value[idx].second}", color = TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (tuningName.isBlank()) "Custom Tuning" else tuningName.trim()
                    val noteIndex = mapOf(
                        "C" to 0, "C#" to 1, "D" to 2, "D#" to 3, "E" to 4, "F" to 5,
                        "F#" to 6, "G" to 7, "G#" to 8, "A" to 9, "A#" to 10, "B" to 11
                    )
                    val gStrings = notes.value.mapIndexed { i, pair ->
                        val semi = (noteIndex[pair.first] ?: 0) + pair.second * 12
                        GuitarString(i + 1, pair.first, pair.second, semi, com.truetuna.tuner.model.semitoneToFrequency(semi, 440f))
                    }
                    onSave(GuitarTuning(finalName, "custom", gStrings, instrument, isCustom = true))
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeVibrant)
            ) {
                Text("Save", color = OledBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant)
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
