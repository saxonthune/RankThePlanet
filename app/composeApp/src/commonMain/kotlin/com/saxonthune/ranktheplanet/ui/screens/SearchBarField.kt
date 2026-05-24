package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction

// TODO: migrate to DockedSearchBar when material3 DockedSearchBar + SearchBarDefaults.InputField lands in commonMain.
@Composable
internal fun SearchBarField(
    expanded: Boolean,
    onFocus: () -> Unit,
    onSearch: (String) -> Unit,
    onQueryChange: (String) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val widthFraction by animateFloatAsState(
        targetValue = if (expanded) 0.8f else 0.4f,
        animationSpec = spring(),
        label = "search-width",
    )

    TextField(
        value = query,
        onValueChange = {
            query = it
            onQueryChange(it)
        },
        placeholder = { Text("Search") },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused && !expanded) onFocus() },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            onSearch(query)
            focusManager.clearFocus()
        }),
    )
}
