package com.saxonthune.ranktheplanet.ui

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val NotImplementedColor = Color(0xFFD99BB5)

@Composable
fun NotImplementedButton(label: String, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = {},
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = NotImplementedColor,
            contentColor = Color.Black,
        ),
    ) {
        Text(label)
    }
}
