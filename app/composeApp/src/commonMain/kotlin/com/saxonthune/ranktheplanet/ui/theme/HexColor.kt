package com.saxonthune.ranktheplanet.ui.theme

import androidx.compose.ui.graphics.Color

/** Total: malformed input returns [Color.Gray]. Accepts "#RRGGBB", "RRGGBB", "#AARRGGBB", "AARRGGBB". */
fun parseAppearanceColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    val parsed = clean.toLongOrNull(16) ?: return Color.Gray
    return when (clean.length) {
        6 -> Color(parsed or 0xFF000000L)
        8 -> Color(parsed)
        else -> Color.Gray
    }
}
