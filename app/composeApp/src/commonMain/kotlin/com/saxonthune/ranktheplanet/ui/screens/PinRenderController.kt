package com.saxonthune.ranktheplanet.ui.screens

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/**
 * Facade over the map's pin feature stream. Any code path — ViewModel events, map
 * lifecycle hooks (`onMapLoadFinished`), debug actions, future error-recovery flows —
 * can push new pins via [setPins] or force a re-push of the existing pin set via
 * [forceRedraw], without needing a Compose context or knowledge of the underlying
 * map source.
 *
 * Downstream observes [frames]; each [PinFrame] carries the current pin set plus a
 * monotonic revision counter. The composable rebuilds GeoJSON per frame, so even a
 * forced redraw (same pins, bumped revision) results in a fresh native `setData`
 * call. This is the recovery hook when the native layer drops features for any
 * reason we cannot reach from Kotlin.
 */
class PinRenderController {
    private val _pins = MutableStateFlow<ImmutableList<PinUi>>(persistentListOf())
    private val _revision = MutableStateFlow(0L)
    private val _selection = MutableStateFlow<Set<String>>(emptySet())

    val frames: Flow<PinFrame> = combine(_pins, _revision, _selection) { p, r, s -> PinFrame(p, r, s) }

    fun setPins(pins: ImmutableList<PinUi>) {
        _pins.value = pins
    }

    /** Location ids whose pins should render in the darkened "selected" variant — the
     *  set is typically size 0 or 1, mirroring the open peek/entry sheet's location. */
    fun setSelection(locationIds: Set<String>) {
        _selection.value = locationIds
    }

    fun forceRedraw() {
        _revision.update { it + 1 }
    }
}

data class PinFrame(
    val pins: ImmutableList<PinUi>,
    val revision: Long,
    val selectedLocationIds: Set<String> = emptySet(),
)
