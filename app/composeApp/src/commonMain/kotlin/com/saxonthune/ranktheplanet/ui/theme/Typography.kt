package com.saxonthune.ranktheplanet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import ranktheplanet.composeapp.generated.resources.Res
import ranktheplanet.composeapp.generated.resources.arimo_bold
import ranktheplanet.composeapp.generated.resources.arimo_bold_italic
import ranktheplanet.composeapp.generated.resources.arimo_italic
import ranktheplanet.composeapp.generated.resources.arimo_regular

@Composable
fun arimoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.arimo_regular, FontWeight.Normal, FontStyle.Normal),
    Font(Res.font.arimo_italic, FontWeight.Normal, FontStyle.Italic),
    Font(Res.font.arimo_regular, FontWeight.Medium, FontStyle.Normal),
    Font(Res.font.arimo_italic, FontWeight.Medium, FontStyle.Italic),
    Font(Res.font.arimo_bold, FontWeight.SemiBold, FontStyle.Normal),
    Font(Res.font.arimo_bold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(Res.font.arimo_bold, FontWeight.Bold, FontStyle.Normal),
    Font(Res.font.arimo_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

@Composable
fun arimoTypography(): Typography {
    val base = Typography()
    val family = arimoFontFamily()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}
