package com.saxonthune.ranktheplanet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The shared shape + spacing + elevation contract for surfaces that float over the map —
 * the search dropdown, the filter chip, the *Search this area* chip, and any later FAB or
 * attribution overlay. One token set so every map overlay shares a corner radius, an inset
 * from the surface edges, and an elevation instead of each picking its own. See doc02.03.
 *
 * These slots are theme-invariant (a corner radius and an inset do not change light↔dark),
 * so this is a plain top-level token object rather than a CompositionLocal-published set.
 */
object RtpOverlay {
    /** Corner radius for overlay panels — reads as a card over the map, not an edge-to-edge slab. */
    val shape: Shape = RoundedCornerShape(20.dp)

    /** Gap an overlay keeps from the surface edges so it floats rather than bleeding to the bezel. */
    val edgeInset: Dp = 12.dp

    /** Tonal tint that lifts the panel off the map (Material elevation). */
    val tonalElevation: Dp = 3.dp

    /** Drop shadow that separates the panel from the map without veiling it (HIG Depth). */
    val shadowElevation: Dp = 8.dp
}
