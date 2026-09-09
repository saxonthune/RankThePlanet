package com.saxonthune.ranktheplanet.domain

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

object AppearancePalette {
    val swatches: ImmutableList<Appearance> = persistentListOf(
        Appearance(color = "#E2553F", pinStyle = "pin"),
        Appearance(color = "#2E7D32", pinStyle = "pin"),
        Appearance(color = "#1565C0", pinStyle = "pin"),
        Appearance(color = "#6A1B9A", pinStyle = "pin"),
        Appearance(color = "#F9A825", pinStyle = "pin"),
        Appearance(color = "#455A64", pinStyle = "pin"),
    )
    val default: Appearance = swatches.first()
}
