package com.saxonthune.ranktheplanet.util

/** Format an ISO-8601 date/timestamp string as a short locale-aware date (e.g. "5/22/2026" en-US). Returns null when the input is not parseable. */
expect fun formatShortDate(iso: String): String?
