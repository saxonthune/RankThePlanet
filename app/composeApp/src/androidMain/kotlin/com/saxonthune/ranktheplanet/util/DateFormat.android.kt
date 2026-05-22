package com.saxonthune.ranktheplanet.util

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

actual fun formatShortDate(iso: String): String? = try {
    val date = try {
        OffsetDateTime.parse(iso).toLocalDate()
    } catch (_: Exception) {
        LocalDate.parse(iso)
    }
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
} catch (_: Exception) {
    null
}
