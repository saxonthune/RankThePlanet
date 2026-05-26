@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.saxonthune.ranktheplanet.util

import MapLibre.MLNMapView
import kotlin.time.TimeSource
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIApplication
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerDelegateProtocol
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.objc.object_getClass

private var tunerCallCounter = 0

actual fun tuneMapForFastTaps(): Boolean {
    val call = ++tunerCallCounter
    val maps = findMapViews()
    println("[MapTapTuner #$call] maps=${maps.size}")
    if (maps.isEmpty()) return false
    maps.forEachIndexed { mapIdx, map ->
        dumpRecognizers("MapTapTuner #$call", mapIdx, map, mutate = true)
    }
    return true
}

private val probeTargets = mutableListOf<MapTapProbeTarget>()
private val probeRecognizers = mutableListOf<UILongPressGestureRecognizer>()

actual fun installMapTapProbe(): Boolean {
    val maps = findMapViews()
    if (maps.isEmpty()) return false
    maps.forEachIndexed { mapIdx, map ->
        if (probeTargets.any { it.map === map }) return@forEachIndexed
        val target = MapTapProbeTarget(map, mapIdx)
        val r = UILongPressGestureRecognizer(target, NSSelectorFromString("handleGesture:"))
        r.minimumPressDuration = 0.0
        r.cancelsTouchesInView = false
        r.delaysTouchesBegan = false
        r.delaysTouchesEnded = false
        r.delegate = target
        probeTargets.add(target)
        probeRecognizers.add(r)
        map.addGestureRecognizer(r)
        println("[MapTapProbe] installed on map[$mapIdx]")
    }
    return true
}

@ExportObjCClass
private class MapTapProbeTarget(
    val map: MLNMapView,
    val mapIdx: Int,
) : NSObject(), UIGestureRecognizerDelegateProtocol {
    private var touchDownMark: TimeSource.Monotonic.ValueTimeMark? = null

    @ObjCAction
    fun handleGesture(sender: UIGestureRecognizer) {
        when (sender.state) {
            UIGestureRecognizerStateBegan -> {
                touchDownMark = TimeSource.Monotonic.markNow()
                println("[MapTapProbe] touch-down map[$mapIdx]")
                dumpRecognizers("MapTapProbe@down", mapIdx, map, mutate = false)
            }
            UIGestureRecognizerStateEnded -> {
                val mark = touchDownMark
                val elapsedMs = mark?.elapsedNow()?.inWholeMilliseconds ?: -1
                println("[MapTapProbe] touch-up   map[$mapIdx] elapsedSinceDown=${elapsedMs}ms")
                touchDownMark = null
            }
            else -> {}
        }
    }

    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWithGestureRecognizer: UIGestureRecognizer,
    ): Boolean = true
}

private fun findMapViews(): List<MLNMapView> {
    val maps = mutableListOf<MLNMapView>()
    UIApplication.sharedApplication.windows.forEach { w ->
        if (w is UIView) collectMapViews(w, maps)
    }
    return maps
}

private fun collectMapViews(view: UIView, out: MutableList<MLNMapView>) {
    if (view is MLNMapView) out.add(view)
    view.subviews.forEach { child ->
        if (child is UIView) collectMapViews(child, out)
    }
}

private fun dumpRecognizers(tag: String, mapIdx: Int, map: MLNMapView, mutate: Boolean) {
    val snapshot = (map.gestureRecognizers ?: return).toList()
    val lastLongPressIndex = snapshot.indexOfLast { it is UILongPressGestureRecognizer }
    val lastSingleTapIndex = snapshot.indexOfLast {
        it is UITapGestureRecognizer &&
            it.numberOfTapsRequired.toLong() == 1L &&
            it.numberOfTouchesRequired.toLong() == 1L
    }
    val singleTapCount = snapshot.count {
        it is UITapGestureRecognizer &&
            it.numberOfTapsRequired.toLong() == 1L &&
            it.numberOfTouchesRequired.toLong() == 1L
    }
    println(
        "[$tag] map[$mapIdx] recognizers=${snapshot.size} " +
            "singleTapCount=$singleTapCount lastSingleTapIdx=$lastSingleTapIndex " +
            "lastLongPressIdx=$lastLongPressIndex"
    )
    val toRemove = mutableListOf<UIGestureRecognizer>()
    snapshot.forEachIndexed { idx, recognizer ->
        if (recognizer !is UIGestureRecognizer) return@forEachIndexed
        val cls = object_getClass(recognizer)?.let { NSStringFromClass(it) } ?: "?"
        val taps = (recognizer as? UITapGestureRecognizer)?.numberOfTapsRequired?.toLong()
        val touches = (recognizer as? UITapGestureRecognizer)?.numberOfTouchesRequired?.toLong()
        val shouldRemove = mutate && when {
            recognizer is UITapGestureRecognizer && idx != lastSingleTapIndex -> true
            recognizer is UILongPressGestureRecognizer && idx != lastLongPressIndex -> true
            else -> false
        }
        val action = if (shouldRemove) "REMOVE" else "keep"
        println(
            "[$tag]   [$idx] $cls " +
                "taps=${taps ?: "-"} touches=${touches ?: "-"} " +
                "enabled=${recognizer.isEnabled()} -> $action"
        )
        if (shouldRemove) toRemove.add(recognizer)
    }
    toRemove.forEach { map.removeGestureRecognizer(it) }
}
