package com.saxonthune.ranktheplanet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalRtpColors = staticCompositionLocalOf<RtpColors> {
    error("No RtpColors provided")
}
private val LocalRtpSpacing = staticCompositionLocalOf { RtpSpacing() }
private val LocalRtpTypography = staticCompositionLocalOf { RtpTypography() }

private fun RtpColors.toMaterialScheme(isDark: Boolean): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        background = background,
        surface = surface,
        onSurface = onSurface,
        onBackground = onSurface,
        error = danger,
    )
}

@Composable
fun RtpTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (dark) RtpDarkColors else RtpLightColors
    CompositionLocalProvider(
        LocalRtpColors provides colors,
        LocalRtpSpacing provides RtpSpacing(),
        LocalRtpTypography provides RtpTypography(),
    ) {
        MaterialTheme(colorScheme = colors.toMaterialScheme(dark)) {
            content()
        }
    }
}

object RtpTheme {
    val colors: RtpColors
        @Composable @ReadOnlyComposable get() = LocalRtpColors.current

    val spacing: RtpSpacing
        @Composable @ReadOnlyComposable get() = LocalRtpSpacing.current

    val typography: RtpTypography
        @Composable @ReadOnlyComposable get() = LocalRtpTypography.current
}
