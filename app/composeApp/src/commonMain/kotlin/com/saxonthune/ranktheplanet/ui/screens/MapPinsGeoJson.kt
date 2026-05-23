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

private fun escapeJsonString(value: String): String {
    val sb = StringBuilder(value.length + 2)
    for (c in value) {
        when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c.code < 0x20) {
                sb.append("\\u")
                sb.append(c.code.toString(16).padStart(4, '0'))
            } else {
                sb.append(c)
            }
        }
    }
    return sb.toString()
}

private fun PinKind.token(): String = when (this) {
    PinKind.Unvisited -> "unvisited"
    PinKind.Reviewed -> "reviewed"
    PinKind.Multi -> "multi"
    PinKind.MultiUnvisited -> "multi-unvisited"
}

internal fun buildPinsGeoJson(pins: List<PinUi>): String {
    val features = pins.joinToString(",") { pin ->
        val name = escapeJsonString(pin.locationName)
        val color = escapeJsonString(pin.colorHex)
        val kind = pin.kind.token()
        """{"type":"Feature","geometry":{"type":"Point","coordinates":[${pin.lng},${pin.lat}]},"properties":{"entryId":"${pin.entryId.value}","name":"$name","color":"$color","kind":"$kind"}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}
