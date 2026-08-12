package com.warwalking.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// Monochrome CRT palette: reds carry emphasis (headline numbers, the active
// action), greys carry structure (labels, borders, secondary surfaces), black
// is the base. No green/other hues - full red coverage read as too intense
// ("hurts") when tried, so brightness/tone is what creates hierarchy here,
// not a second color family. Every ColorScheme slot is set explicitly;
// leaving any to Material3's defaults reintroduces its baseline purple/teal.

private val CrtBlack = Color(0xFF050505)
private val CrtSurfaceDim = Color(0xFF0A0A0A)
private val CrtSurface = Color(0xFF121212)
private val CrtSurfaceBright = Color(0xFF242424)
private val CrtSurfaceContainerLowest = Color(0xFF000000)
private val CrtSurfaceContainerLow = Color(0xFF0D0D0D)
private val CrtSurfaceContainer = Color(0xFF161616)
private val CrtSurfaceContainerHigh = Color(0xFF202020)
private val CrtSurfaceContainerHighest = Color(0xFF2A2A2A)

private val OutlineGrey = Color(0xFF3A3A3A)
private val MutedGreyText = Color(0xFF9E9E9E)

private val PhosphorRed = Color(0xFFFF3B3B)
private val PhosphorRedContainer = Color(0xFF2A0A0A)
private val DeepRed = Color(0xFFB23A3A)
private val DeepRedContainer = Color(0xFF241414)

val WarWalkingColorScheme = darkColorScheme(
    primary = PhosphorRed,
    onPrimary = Color.Black,
    primaryContainer = PhosphorRedContainer,
    onPrimaryContainer = PhosphorRed,
    inversePrimary = PhosphorRed,

    // A deeper, less saturated red - not another hue - for secondary emphasis
    // (live score readout, selected nav highlight) so it reads as a shade of
    // the same palette rather than a competing accent color.
    secondary = DeepRed,
    onSecondary = Color.Black,
    secondaryContainer = DeepRedContainer,
    onSecondaryContainer = PhosphorRed,

    tertiary = MutedGreyText,
    onTertiary = Color.Black,
    tertiaryContainer = CrtSurfaceContainerHigh,
    onTertiaryContainer = MutedGreyText,

    background = CrtBlack,
    onBackground = PhosphorRed,

    surface = CrtSurface,
    onSurface = PhosphorRed,
    surfaceVariant = CrtSurfaceContainer,
    // Labels/captions are grey, not dim red - keeps big red numbers readable
    // as the thing your eye lands on instead of everything glowing at once.
    onSurfaceVariant = MutedGreyText,
    surfaceTint = PhosphorRed,

    inverseSurface = PhosphorRed,
    inverseOnSurface = Color.Black,

    error = PhosphorRed,
    onError = Color.Black,
    errorContainer = PhosphorRedContainer,
    onErrorContainer = PhosphorRed,

    outline = OutlineGrey,
    outlineVariant = CrtSurfaceContainerHighest,
    scrim = Color.Black,

    surfaceBright = CrtSurfaceBright,
    surfaceDim = CrtSurfaceDim,
    surfaceContainer = CrtSurfaceContainer,
    surfaceContainerHigh = CrtSurfaceContainerHigh,
    surfaceContainerHighest = CrtSurfaceContainerHighest,
    surfaceContainerLow = CrtSurfaceContainerLow,
    surfaceContainerLowest = CrtSurfaceContainerLowest,
)

private val baseTypography = Typography()

val WarWalkingTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = FontFamily.Monospace),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = FontFamily.Monospace),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = FontFamily.Monospace),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = FontFamily.Monospace),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = FontFamily.Monospace),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = FontFamily.Monospace),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = FontFamily.Monospace),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = FontFamily.Monospace),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = FontFamily.Monospace),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = FontFamily.Monospace),
)

@Composable
fun WarWalkingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarWalkingColorScheme,
        typography = WarWalkingTypography,
        content = content,
    )
}
