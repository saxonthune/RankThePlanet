package com.saxonthune.ranktheplanet.util

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSISO8601DateFormatter

actual fun formatShortDate(iso: String): String? = try {
    val parser = NSISO8601DateFormatter()
    val date = parser.dateFromString(iso) ?: return null
    val formatter = NSDateFormatter()
    formatter.dateStyle = NSDateFormatterShortStyle
    formatter.timeStyle = NSDateFormatterNoStyle
    formatter.stringFromDate(date)
} catch (_: Exception) {
    null
}
