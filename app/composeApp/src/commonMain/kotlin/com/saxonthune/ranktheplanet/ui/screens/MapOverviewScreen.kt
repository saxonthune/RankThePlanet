package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.location.LocationProvider
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.nav.MapMode
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import androidx.compose.runtime.key
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

private fun parseHexColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    val r = clean.substring(0, 2).toInt(16)
    val g = clean.substring(2, 4).toInt(16)
    val b = clean.substring(4, 6).toInt(16)
    return Color(red = r, green = g, blue = b)
}

private fun buildPinsGeoJson(pins: List<PinUi>): String {
    val features = pins.joinToString(",") { pin ->
        """{"type":"Feature","geometry":{"type":"Point","coordinates":[${pin.lng},${pin.lat}]},"properties":{"entryId":"${pin.entryId.value}"}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

@Composable
fun MapOverviewScreen(
    mode: MapMode,
    collections: CollectionRepository,
    entries: EntryRepository,
    locationProvider: LocationProvider,
    onCancelAdd: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
    onInspectPin: (EntryId) -> Unit,
    onDropPin: () -> Unit,
) {
    val vm = viewModel { MapOverviewViewModel(collections, entries, locationProvider) }
    val state by vm.uiState.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppMenuDrawer(
                state = state,
                onToggleCollection = { vm.toggleCollection(it) },
                onOpenSettings = onOpenSettings,
            )
        },
    ) {
        val cameraState = rememberCameraState(
            firstPosition = CameraPosition(
                target = Position(longitude = -73.9855, latitude = 40.7580),
                zoom = 12.0,
            )
        )

        Box(Modifier.fillMaxSize()) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                cameraState = cameraState,
                baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                onMapLongClick = { _, _ ->
                    onDropPin()
                    ClickResult.Consume
                },
            ) {
                state.collectionRows.forEach { col ->
                    key(col.id.value) {
                        val colPins = state.pins.filter { it.color == col.color }
                        val geojson = buildPinsGeoJson(colPins)
                        val source = rememberGeoJsonSource(
                            data = GeoJsonData.JsonString(geojson),
                        )
                        CircleLayer(
                            id = "pins-${col.id.value}",
                            source = source,
                            radius = const(8.dp),
                            color = const(parseHexColor(col.color)),
                            strokeWidth = const(2.dp),
                            strokeColor = const(Color.White),
                            onClick = { features ->
                                val entryId = features.firstOrNull()
                                    ?.properties
                                    ?.get("entryId")
                                    ?.jsonPrimitive
                                    ?.contentOrNull
                                if (entryId != null) {
                                    onInspectPin(EntryId(entryId))
                                    ClickResult.Consume
                                } else {
                                    ClickResult.Pass
                                }
                            },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                if (mode is MapMode.AddingToCollection) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Black.copy(alpha = 0.65f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Adding to ${mode.collectionName}", color = Color.White)
                            TextButton(onClick = onCancelAdd) {
                                Text("Cancel", color = Color.White)
                            }
                        }
                    }
                }

                SearchBar(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onSearch = { vm.search(it) },
                    searchResults = state.searchResults,
                    onSearchResultClick = onDropPin,
                )

                Spacer(Modifier.weight(1f))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledIconButton(onClick = onOpenCollections) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Browse Collections",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppMenuDrawer(
    state: MapOverviewUiState,
    onToggleCollection: (com.saxonthune.ranktheplanet.domain.CollectionId) -> Unit,
    onOpenSettings: () -> Unit,
) {
    ModalDrawerSheet {
        Text(
            text = "Menu",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )

        Text(
            text = "Filter",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        var filterQuery by remember { mutableStateOf("") }

        OutlinedTextField(
            value = filterQuery,
            onValueChange = { filterQuery = it },
            placeholder = { Text("Filter…") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        val filtered = state.collectionRows.filter {
            it.name.contains(filterQuery, ignoreCase = true)
        }
        LazyColumn {
            items(filtered, key = { it.id.value }) { row ->
                ListItem(
                    headlineContent = { Text(row.name) },
                    leadingContent = {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(parseHexColor(row.color), CircleShape)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = row.shown,
                            onCheckedChange = { onToggleCollection(row.id) },
                        )
                    },
                )
                HorizontalDivider()
            }
        }

        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Settings") },
            leadingContent = {
                Icon(Icons.Default.Settings, contentDescription = null)
            },
            modifier = Modifier.clickable { onOpenSettings() },
        )
    }
}

@Composable
private fun SearchBar(
    onOpenDrawer: () -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<SearchResultUi>,
    onSearchResultClick: () -> Unit,
) {
    // TODO: Replace with DockedSearchBar + SearchBarDefaults.InputField once that API
    //  is confirmed stable in the bundled CMP material3 version.
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(if (expanded) 0.8f else 0.4f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
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
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused && !expanded) expanded = true },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            )

            if (expanded && searchResults.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                ) {
                    Column {
                        searchResults.forEach { result ->
                            ListItem(
                                headlineContent = { Text(result.displayName) },
                                modifier = Modifier.clickable { onSearchResultClick() },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Open the app menu")
        }
    }
}
