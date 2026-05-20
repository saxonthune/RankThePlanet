package com.saxonthune.ranktheplanet.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class HexColorTest {

    @Test
    fun hashPrefixedRrggbb() {
        val result = parseAppearanceColor("#0D9488")
        assertEquals(Color(0xFF0D9488L), result)
    }

    @Test
    fun unprefixedRrggbb() {
        val result = parseAppearanceColor("0D9488")
        assertEquals(Color(0xFF0D9488L), result)
    }

    @Test
    fun aarrggbb() {
        val result = parseAppearanceColor("FF0D9488")
        assertEquals(Color(0xFF0D9488L), result)
    }

    @Test
    fun bogusReturnGray() {
        assertEquals(Color.Gray, parseAppearanceColor("bogus"))
    }

    @Test
    fun emptyReturnGray() {
        assertEquals(Color.Gray, parseAppearanceColor(""))
    }
}
