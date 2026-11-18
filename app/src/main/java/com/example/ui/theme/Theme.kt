package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val HalmaalShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun HalmaalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColorHex: Long = 0xFF0B2B5CL,
    darkAccentColorHex: Long = 0xFF001027L,
    ctaHighlightHex: Long = 0xFFD99B1EL,
    canvasBackgroundHex: Long = 0xFFFAF7F0L,
    content: @Composable () -> Unit
) {
    val dynamicPrimary = Color(primaryColorHex)
    val dynamicSecondary = Color(darkAccentColorHex)
    val dynamicTertiary = Color(ctaHighlightHex)
    val dynamicBg = Color(canvasBackgroundHex)

    val lightScheme = lightColorScheme(
        primary = dynamicPrimary,
        onPrimary = Color.White,
        primaryContainer = dynamicPrimary.copy(alpha = 0.14f),
        onPrimaryContainer = dynamicSecondary,
        secondary = dynamicSecondary,
        onSecondary = Color.White,
        secondaryContainer = SoftCreamSurfaceVariant,
        onSecondaryContainer = NavyInkCharcoal,
        tertiary = dynamicTertiary,
        onTertiary = HalmaalMidnightNavy,
        tertiaryContainer = HalmaalGoldContainer,
        onTertiaryContainer = HalmaalOnGoldContainer,
        error = CrimsonError,
        onError = Color.White,
        errorContainer = CrimsonContainer,
        background = dynamicBg,
        onBackground = NavyInkCharcoal,
        surface = PureWhiteSurface,
        onSurface = NavyInkCharcoal,
        surfaceVariant = SoftCreamSurfaceVariant,
        onSurfaceVariant = SlateInkMuted,
        outline = IvoryGoldOutline
    )

    val darkScheme = darkColorScheme(
        primary = NightGoldPrimary,
        onPrimary = HalmaalMidnightNavy,
        primaryContainer = NightNavyContainer,
        onPrimaryContainer = HalmaalGoldContainer,
        secondary = dynamicPrimary,
        onSecondary = Color.White,
        secondaryContainer = NightSurfaceVariant,
        onSecondaryContainer = NightTextPrimary,
        tertiary = dynamicTertiary,
        onTertiary = HalmaalMidnightNavy,
        tertiaryContainer = Color(0xFF4A3200),
        onTertiaryContainer = HalmaalGoldContainer,
        error = Color(0xFFF87171),
        onError = Color(0xFF450A0A),
        background = NightBackground,
        onBackground = NightTextPrimary,
        surface = NightSurface,
        onSurface = NightTextPrimary,
        surfaceVariant = NightSurfaceVariant,
        onSurfaceVariant = NightTextMuted,
        outline = NightOutline
    )

    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme else lightScheme,
        typography = HalmaalTypography,
        shapes = HalmaalShapes,
        content = content
    )
}
