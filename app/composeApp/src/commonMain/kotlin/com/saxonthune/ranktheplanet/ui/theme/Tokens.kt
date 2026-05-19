package com.saxonthune.ranktheplanet.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class RtpColors(
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val accent: Color,
    val danger: Color,
)

@Immutable
data class RtpSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
)

@Immutable
data class RtpTypography(
    val screenTitle: TextStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    val body: TextStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    val label: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    val caption: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
)

val RtpLightColors = RtpColors(
    background = Palette.slate50,
    surface = Palette.white,
    onSurface = Palette.slate900,
    accent = Palette.teal600,
    danger = Palette.red600,
)

val RtpDarkColors = RtpColors(
    background = Palette.slate950,
    surface = Palette.slate900,
    onSurface = Palette.slate100,
    accent = Palette.teal400,
    danger = Palette.red400,
)
