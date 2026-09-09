package com.saxonthune.ranktheplanet.ui.screens

import kotlin.math.abs
import kotlin.math.roundToLong

private fun format3dp(value: Double): String {
    val scaled = (value * 1000.0).roundToLong()
    val whole = scaled / 1000
    val frac = (scaled % 1000).let { if (it < 0) -it else it }
    val fracStr = frac.toString().padStart(3, '0')
    return "$whole.$fracStr"
}

// three decimals is ~100m precision; readable for a peek card
internal fun formatLatLng(lat: Double, lng: Double): String {
    val ns = if (lat >= 0) "N" else "S"
    val ew = if (lng >= 0) "E" else "W"
    return "${format3dp(abs(lat))}°$ns, ${format3dp(abs(lng))}°$ew"
}
