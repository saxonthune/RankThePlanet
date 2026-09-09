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

/**
 * Returns a #RRGGBB hex string for [hex] with its RGB channels multiplied by [factor]
 * (0.0 = black, 1.0 = unchanged). Used to render a "selected" darker variant of
 * Collection appearance colors. Malformed input returns "#555555".
 */
fun darkenHex(hex: String, factor: Float = 0.55f): String {
    val clean = hex.removePrefix("#")
    val parsed = clean.toLongOrNull(16) ?: return "#555555"
    val rgb = when (clean.length) {
        6 -> parsed
        8 -> parsed and 0xFFFFFFL
        else -> return "#555555"
    }
    val r = ((rgb shr 16) and 0xFF).toInt()
    val g = ((rgb shr 8) and 0xFF).toInt()
    val b = (rgb and 0xFF).toInt()
    val dr = (r * factor).toInt().coerceIn(0, 255)
    val dg = (g * factor).toInt().coerceIn(0, 255)
    val db = (b * factor).toInt().coerceIn(0, 255)
    return "#" + dr.toString(16).padStart(2, '0') +
        dg.toString(16).padStart(2, '0') +
        db.toString(16).padStart(2, '0')
}
