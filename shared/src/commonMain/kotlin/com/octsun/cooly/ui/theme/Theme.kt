package com.octsun.cooly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Cool blue / mint identity. Bright, simple, "answers visible at a glance".
private val CoolBlue = Color(0xFF0091EA)
private val CoolMint = Color(0xFF00BFA5)
private val CoolBlueDark = Color(0xFF0064B7)

// Risk / AQI accent colors, reused by status bar + pins. Same in both themes —
// safety colors must stay instantly recognizable.
val RiskSafe = Color(0xFF2E7D32)
val RiskCaution = Color(0xFFF9A825)
val RiskDanger = Color(0xFFD32F2F)

val AqiGood = Color(0xFF2E7D32)
val AqiModerate = Color(0xFFF9A825)
val AqiUnhealthySensitive = Color(0xFFEF6C00)
val AqiUnhealthy = Color(0xFFD84315)
val AqiVeryUnhealthy = Color(0xFFC62828)
val AqiHazardous = Color(0xFF7B1FA2)

/** Theme-dependent brand/UI colors (light + dark variants). */
@Immutable
data class CoolyColors(
    val ink: Color,          // primary text
    val sub: Color,          // secondary text
    val brand: Color,        // primary blue accent
    val brandDeep: Color,    // readable blue text on the info tint
    val mint: Color,         // secondary accent
    val infoTint: Color,     // info container background
    val divider: Color,
    val errorBg: Color,
    val errorInk: Color,
    val mapLand: Color,      // CanvasMap background
    val mapGrid: Color,      // CanvasMap grid lines
    val mapLabelBg: Color,   // CanvasMap distance-label chip
    val card: Color,         // elevated card/status surface
    val topBar: Color,       // app top bar
    val isDark: Boolean,
)

val LightCoolyColors = CoolyColors(
    ink = Color(0xFF0B2733),
    sub = Color(0xFF6B7A82),
    brand = CoolBlue,
    brandDeep = Color(0xFF00618F),
    mint = CoolMint,
    infoTint = Color(0xFFE3F6FF),
    divider = Color(0xFFECF3F7),
    errorBg = Color(0xFFFDECEA),
    errorInk = Color(0xFFB71C1C),
    mapLand = Color(0xFFE9F4FB),
    mapGrid = Color(0x220091EA),
    mapLabelBg = Color(0xCCFFFFFF),
    card = Color.White,
    topBar = CoolBlue,
    isDark = false,
)

val DarkCoolyColors = CoolyColors(
    ink = Color(0xFFE4EEF3),
    sub = Color(0xFF95A7B0),
    brand = Color(0xFF4FB7FF),
    brandDeep = Color(0xFF9BD7FF),
    mint = Color(0xFF2FD8C0),
    infoTint = Color(0xFF12303F),
    divider = Color(0xFF223239),
    errorBg = Color(0xFF3A1512),
    errorInk = Color(0xFFFF8A80),
    mapLand = Color(0xFF10222C),
    mapGrid = Color(0x334FB7FF),
    mapLabelBg = Color(0xCC15252D),
    card = Color(0xFF15252D),
    topBar = Color(0xFF0A3A5C),
    isDark = true,
)

val LocalCoolyColors = staticCompositionLocalOf { LightCoolyColors }

/**
 * Theme-aware palette. Reads the current [CoolyColors] from composition, so existing
 * `CoolyPalette.X` call sites keep working in both light and dark themes.
 */
object CoolyPalette {
    val Ink: Color @Composable get() = LocalCoolyColors.current.ink
    val Sub: Color @Composable get() = LocalCoolyColors.current.sub
    val Brand: Color @Composable get() = LocalCoolyColors.current.brand
    val BrandDeep: Color @Composable get() = LocalCoolyColors.current.brandDeep
    val Mint: Color @Composable get() = LocalCoolyColors.current.mint
    val InfoTint: Color @Composable get() = LocalCoolyColors.current.infoTint
    val Divider: Color @Composable get() = LocalCoolyColors.current.divider
    val ErrorBg: Color @Composable get() = LocalCoolyColors.current.errorBg
    val ErrorInk: Color @Composable get() = LocalCoolyColors.current.errorInk
    val MapLand: Color @Composable get() = LocalCoolyColors.current.mapLand
    val MapGrid: Color @Composable get() = LocalCoolyColors.current.mapGrid
    val Card: Color @Composable get() = LocalCoolyColors.current.card
    val TopBar: Color @Composable get() = LocalCoolyColors.current.topBar
}

private val CoolyLightScheme = lightColorScheme(
    primary = CoolBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE9FF),
    onPrimaryContainer = CoolBlueDark,
    secondary = CoolMint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2F5EA),
    onSecondaryContainer = Color(0xFF00514A),
    background = Color(0xFFF5FBFF),
    surface = Color.White,
    onSurface = Color(0xFF0B2733),
)

private val CoolyDarkScheme = darkColorScheme(
    primary = Color(0xFF4FB7FF),
    onPrimary = Color(0xFF002A44),
    primaryContainer = Color(0xFF0A3A5C),
    onPrimaryContainer = Color(0xFFCDE9FF),
    secondary = Color(0xFF2FD8C0),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF00514A),
    onSecondaryContainer = Color(0xFFB2F5EA),
    background = Color(0xFF0B1A21),
    surface = Color(0xFF12242D),
    onSurface = Color(0xFFE4EEF3),
    surfaceContainerLow = Color(0xFF15252D),
    surfaceContainerHigh = Color(0xFF1B303A),
)

@Composable
fun CoolyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(
        LocalCoolyColors provides if (dark) DarkCoolyColors else LightCoolyColors,
    ) {
        MaterialTheme(
            colorScheme = if (dark) CoolyDarkScheme else CoolyLightScheme,
            content = content,
        )
    }
}
