package com.waterhardness.note.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CareColors = lightColorScheme(primary = PrimaryBurgundy, onPrimary = SurfaceWhite, secondary = ActionAqua, onSecondary = Ink, background = BackgroundBlush, onBackground = Ink, surface = SurfaceWhite, onSurface = Ink, surfaceVariant = OceanMist, onSurfaceVariant = InkMuted, outline = MineralRose, error = PrimaryBurgundy, onError = SurfaceWhite, primaryContainer = BackgroundBlush, onPrimaryContainer = BurgundyDeep, secondaryContainer = OceanPale, onSecondaryContainer = Ink)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CareColors, typography = CareTypography, content = content)
}
