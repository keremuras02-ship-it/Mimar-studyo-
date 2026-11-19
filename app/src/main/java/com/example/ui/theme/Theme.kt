package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AtelierDarkColorScheme = darkColorScheme(
    primary = BurnishedBrass,
    onPrimary = Color(0xFF1F1105),
    primaryContainer = BurnishedBrassContainer,
    onPrimaryContainer = BurnishedBrassLight,
    secondary = ArchitecturalSage,
    onSecondary = Color(0xFF07211C),
    secondaryContainer = ArchitecturalSageContainer,
    onSecondaryContainer = ArchitecturalSageLight,
    tertiary = BlueprintAmber,
    onTertiary = Color(0xFF241A05),
    background = ObsidianBackground,
    onBackground = WarmTravertine,
    surface = ObsidianSurface,
    onSurface = WarmTravertine,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = MutedLimestone,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineSubtle,
    error = StudioError,
    onError = Color(0xFF2B0808),
    errorContainer = StudioErrorContainer,
    onErrorContainer = Color(0xFFFFDAD6)
)

private val AtelierLightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6DEC9),
    onPrimaryContainer = Color(0xFF3E1C07),
    secondary = Color(0xFF366C61),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2ECE6),
    onSecondaryContainer = Color(0xFF0C2B24),
    tertiary = Color(0xFF8C6422),
    onTertiary = Color.White,
    background = AlabasterBackground,
    onBackground = ObsidianInk,
    surface = AlabasterSurface,
    onSurface = ObsidianInk,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = SlateSecondary,
    outline = Color(0xFFCFC6B8),
    outlineVariant = Color(0xFFE3DBCE)
)

val AtelierShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MimarStudyoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AtelierDarkColorScheme else AtelierLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AtelierShapes,
        content = content
    )
}
