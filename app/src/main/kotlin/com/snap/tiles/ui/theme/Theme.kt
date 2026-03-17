package com.snap.tiles.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Design system colors from Stitch
val Primary = Color(0xFF6E5E00)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFFBD928)
val OnPrimaryContainer = Color(0xFF6F5E00)
val Secondary = Color(0xFF5F5E5E)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFE2DFDE)
val OnSecondaryContainer = Color(0xFF636262)
val Tertiary = Color(0xFF6E5D00)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFBD928)
val OnTertiaryContainer = Color(0xFF6F5E00)
val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)
val Background = Color(0xFFF9F9F9)
val OnBackground = Color(0xFF1A1C1C)
val Surface = Color(0xFFF9F9F9)
val OnSurface = Color(0xFF1A1C1C)
val SurfaceVariant = Color(0xFFE2E2E2)
val OnSurfaceVariant = Color(0xFF4C4733)
val Outline = Color(0xFF7E7760)
val OutlineVariant = Color(0xFFCFC6AC)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF3F3F3)
val SurfaceContainer = Color(0xFFEEEEEE)
val SurfaceContainerHigh = Color(0xFFE8E8E8)
val SurfaceContainerHighest = Color(0xFFE2E2E2)
val InverseSurface = Color(0xFF2F3131)
val InverseOnSurface = Color(0xFFF1F1F1)
val InversePrimary = Color(0xFFE6C503)
val Success = Color(0xFF2E7D32)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary = InversePrimary,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

@Composable
fun QuickTilesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
