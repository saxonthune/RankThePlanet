package com.saxonthune.ranktheplanet.util

import kotlin.time.TimeSource

object PinTrace {
    private val t0 = TimeSource.Monotonic.markNow()

    fun log(label: String, vararg fields: Pair<String, Any?>) {
        val ms = t0.elapsedNow().inWholeMilliseconds
        val tail = if (fields.isEmpty()) ""
        else " " + fields.joinToString(" ") { (k, v) -> "$k=$v" }
        println("[pin-trace +${ms}ms] $label$tail")
    }
}
