@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.saxonthune.ranktheplanet.util

import MapLibre.MLNMapView
import platform.UIKit.UIApplication
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView

actual fun tuneMapForFastTaps(): Boolean {
    val maps = mutableListOf<MLNMapView>()
    UIApplication.sharedApplication.windows.forEach { w ->
        if (w is UIView) collectMapViews(w, maps)
    }
    if (maps.isEmpty()) return false
    maps.forEach { map ->
        val snapshot = (map.gestureRecognizers ?: return@forEach).toList()
        // maplibre-compose adds its recognizers last; keep its tap and long-press, remove the rest.
        // Disabling is not enough — MLN re-enables its own recognizers after style load, so a
        // disabled recognizer is back in the arbitration cascade by first tap. Removal sticks.
        val lastLongPressIndex = snapshot.indexOfLast { it is UILongPressGestureRecognizer }
        val lastSingleTapIndex = snapshot.indexOfLast {
            it is UITapGestureRecognizer &&
                it.numberOfTapsRequired.toLong() == 1L &&
                it.numberOfTouchesRequired.toLong() == 1L
        }
        val toRemove = mutableListOf<UIGestureRecognizer>()
        snapshot.forEachIndexed { idx, recognizer ->
            if (recognizer !is UIGestureRecognizer) return@forEachIndexed
            val shouldRemove = when {
                recognizer is UITapGestureRecognizer && idx != lastSingleTapIndex -> true
                recognizer is UILongPressGestureRecognizer && idx != lastLongPressIndex -> true
                else -> false
            }
            if (shouldRemove) toRemove.add(recognizer)
        }
        toRemove.forEach { map.removeGestureRecognizer(it) }
    }
    return true
}

private fun collectMapViews(view: UIView, out: MutableList<MLNMapView>) {
    if (view is MLNMapView) out.add(view)
    view.subviews.forEach { child ->
        if (child is UIView) collectMapViews(child, out)
    }
}
