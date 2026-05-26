@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.saxonthune.ranktheplanet.util

import MapLibre.MLNMapView
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIApplication
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.objc.object_getClass

private var tunerCallCounter = 0

actual fun tuneMapForFastTaps(): Boolean {
    val call = ++tunerCallCounter
    val maps = mutableListOf<MLNMapView>()
    UIApplication.sharedApplication.windows.forEach { w ->
        if (w is UIView) collectMapViews(w, maps)
    }
    println("[MapTapTuner #$call] maps=${maps.size}")
    if (maps.isEmpty()) return false
    maps.forEachIndexed { mapIdx, map ->
        val recognizers = map.gestureRecognizers ?: return@forEachIndexed
        val lastLongPressIndex = recognizers.indexOfLast { it is UILongPressGestureRecognizer }
        val lastSingleTapIndex = recognizers.indexOfLast {
            it is UITapGestureRecognizer &&
                it.numberOfTapsRequired.toLong() == 1L &&
                it.numberOfTouchesRequired.toLong() == 1L
        }
        val singleTapCount = recognizers.count {
            it is UITapGestureRecognizer &&
                it.numberOfTapsRequired.toLong() == 1L &&
                it.numberOfTouchesRequired.toLong() == 1L
        }
        println(
            "[MapTapTuner #$call] map[$mapIdx] recognizers=${recognizers.size} " +
                "singleTapCount=$singleTapCount lastSingleTapIdx=$lastSingleTapIndex " +
                "lastLongPressIdx=$lastLongPressIndex"
        )
        recognizers.forEachIndexed { idx, recognizer ->
            if (recognizer !is UIGestureRecognizer) return@forEachIndexed
            val cls = object_getClass(recognizer)?.let { NSStringFromClass(it) } ?: "?"
            val taps = (recognizer as? UITapGestureRecognizer)?.numberOfTapsRequired?.toLong()
            val touches = (recognizer as? UITapGestureRecognizer)?.numberOfTouchesRequired?.toLong()
            val shouldDisable = when {
                recognizer is UITapGestureRecognizer && idx != lastSingleTapIndex -> true
                recognizer is UILongPressGestureRecognizer && idx != lastLongPressIndex -> true
                else -> false
            }
            val action = when {
                !recognizer.isEnabled() -> "already-off"
                shouldDisable -> "DISABLE"
                else -> "keep"
            }
            println(
                "[MapTapTuner #$call]   [$idx] $cls " +
                    "taps=${taps ?: "-"} touches=${touches ?: "-"} " +
                    "enabled=${recognizer.isEnabled()} -> $action"
            )
            if (shouldDisable && recognizer.isEnabled()) recognizer.setEnabled(false)
        }
    }
    return true
}

private fun collectMapViews(view: UIView, out: MutableList<MLNMapView>) {
    if (view is MLNMapView) out.add(view)
    view.subviews.forEach { child ->
        if (child is UIView) collectMapViews(child, out)
    }
}
