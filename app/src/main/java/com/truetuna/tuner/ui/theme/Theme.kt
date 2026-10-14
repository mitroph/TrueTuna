package com.truetuna.tuner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── OLED-optimized dark palette ─────────────────────────────────────────────
val OledBlack        = Color(0xFF0D0D0F)
val SurfaceDark      = Color(0xFF18181C)
val SurfaceVariant   = Color(0xFF222228)
val CardSurface      = Color(0xFF1E1E24)

// Orange brand accent
val OrangeVibrant    = Color(0xFFFF6D00)
val OrangeBright     = Color(0xFFFF8533)
val AmberWarm        = Color(0xFFFFAB40)
val OrangeRedWarn    = Color(0xFFFF3D00)

// In-tune state
val NeonGreen        = Color(0xFF00E676)
val NeonOrange       = Color(0xFFFF9100)

// Text
val TextPrimary      = Color(0xFFF5F5F5)
val TextSecondary    = Color(0xFFAAAAAA)
val TextMuted        = Color(0xFF666672)

private val DarkColorScheme = darkColorScheme(
    primary          = OrangeVibrant,
    onPrimary        = OledBlack,
    secondary        = AmberWarm,
    onSecondary      = OledBlack,
    error            = OrangeRedWarn,
    background       = OledBlack,
    onBackground     = TextPrimary,
    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline          = Color(0xFF3A3A44)
)

@Composable
fun TrueTunaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography   = TrueTunaTypography,
        content      = content
    )
}
