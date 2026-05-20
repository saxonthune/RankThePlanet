package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.nav.MapMode
import com.saxonthune.ranktheplanet.ui.RtpErrorState
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
import kotlin.math.abs
import kotlin.math.roundToLong

private fun format3dp(value: Double): String {
    val scaled = (value * 1000.0).roundToLong()
    val whole = scaled / 1000
    val frac = (scaled % 1000).let { if (it < 0) -it else it }
    val fracStr = frac.toString().padStart(3, '0')
    return "$whole.$fracStr"
}

private fun formatLatLng(lat: Double, lng: Double): String {
    val ns = if (lat >= 0) "N" else "S"
    val ew = if (lng >= 0) "E" else "W"
    // three decimals is ~100m precision; readable for a peek card
    return "${format3dp(abs(lat))}°$ns, ${format3dp(abs(lng))}°$ew"
}

private fun buildPinsGeoJson(pins: List<PinUi>): String {
    val features = pins.joinToString(",") { pin ->
        """{"type":"Feature","geometry":{"type":"Point","coordinates":[${pin.lng},${pin.lat}]},"properties":{"entryId":"${pin.entryId.value}"}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapOverviewScreen(
    mode: MapMode,
    collections: CollectionRepository,
    entries: EntryRepository,
    locationProvider: LocationProvider,
    onCancelAdd: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFullDetail: (EntryId) -> Unit,
    onViewCollection: (CollectionId) -> Unit,
    onEditReview: () -> Unit,
    onDropPin: () -> Unit,
) {
    val vm = viewModel { MapOverviewViewModel(collections, entries, locationProvider) }
    val state by vm.uiState.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    var searchExpanded by remember { mutableStateOf(false) }

    // Capture surface color here — must not be read inside the MaplibreMap content lambda.
    val pinStroke = MaterialTheme.colorScheme.surface

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

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open the app menu")
                            }
                        },
                        title = {},
                        actions = {
                            SearchBarField(
                                expanded = searchExpanded,
                                onFocus = { searchExpanded = true },
                                onSearch = { vm.search(it) },
                            )
                        },
                    )
                    if (searchExpanded && state.searchResults.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 8.dp,
                        ) {
                            Column {
                                state.searchResults.forEach { result ->
                                    ListItem(
                                        headlineContent = { Text(result.displayName) },
                                        modifier = Modifier.clickable { onDropPin() },
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = mode is MapMode.AddingToCollection,
                        enter = slideInVertically(initialOffsetY = { -it }),
                        exit = slideOutVertically(targetOffsetY = { -it }),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val collectionName =
                                    (mode as? MapMode.AddingToCollection)?.collectionName.orEmpty()
                                Text("Adding to $collectionName")
                                TextButton(onClick = onCancelAdd) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                BottomAppBar(
                    actions = {
                        TextButton(onClick = onOpenCollections) {
                            Icon(Icons.Default.Layers, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Collections")
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = onDropPin) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Drop pin here")
                        }
                    },
                )
            },
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
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
                                color = const(col.color),
                                strokeWidth = const(2.dp),
                                strokeColor = const(pinStroke),
                                onClick = { features ->
                                    val entryId = features.firstOrNull()
                                        ?.properties
                                        ?.get("entryId")
                                        ?.jsonPrimitive
                                        ?.contentOrNull
                                    if (entryId != null) {
                                        vm.selectPin(EntryId(entryId))
                                        ClickResult.Consume
                                    } else {
                                        ClickResult.Pass
                                    }
                                },
                            )
                        }
                    }
                }
                if (state.error != null) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        RtpErrorState(
                            message = state.error!!,
                            onRetry = vm::retry,
                        )
                    }
                }
            }
        }
    }

    if (state.pinSheet !is PinSheet.None) {
        ModalBottomSheet(
            onDismissRequest = { vm.dismissSheet() },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            when (val sheet = state.pinSheet) {
                is PinSheet.Peek -> LocationDetailPeek(
                    peek = sheet,
                    onPickEntry = { vm.openEntryFromPeek(it) },
                )
                is PinSheet.Entry -> EntryDrawerSheet(
                    entry = sheet.entry,
                    onOpenFullDetail = { entryId ->
                        vm.dismissSheet()
                        onOpenFullDetail(entryId)
                    },
                    onViewCollection = { collectionId ->
                        vm.dismissSheet()
                        onViewCollection(collectionId)
                    },
                    onEditReview = {
                        vm.dismissSheet()
                        onEditReview()
                    },
                    onDismiss = { vm.dismissSheet() },
                )
                is PinSheet.None -> {}
            }
        }
    }
}

@Composable
private fun LocationDetailPeek(
    peek: PinSheet.Peek,
    onPickEntry: (EntryId) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp, max = 220.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(peek.locationName, style = MaterialTheme.typography.titleMedium)
            Text(
                formatLatLng(peek.lat, peek.lng),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(peek.entries) { entry ->
                ListItem(
                    headlineContent = { Text(entry.collectionName) },
                    leadingContent = {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(entry.collectionColor, CircleShape)
                        )
                    },
                    supportingContent = {
                        if (entry.visited) {
                            Text("Visited", style = MaterialTheme.typography.bodySmall)
                        } else {
                            AssistChip(
                                onClick = {},
                                label = { Text("Not yet visited") },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                },
                            )
                        }
                    },
                    modifier = Modifier.clickable { onPickEntry(entry.entryId) },
                )
            }
        }
    }
}

@Composable
private fun EntryDrawerSheet(
    entry: EntrySummaryUi,
    onOpenFullDetail: (EntryId) -> Unit,
    onViewCollection: (CollectionId) -> Unit,
    onEditReview: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(entry.locationName, style = MaterialTheme.typography.titleMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(entry.collectionColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(entry.collectionName)
        }
        if (entry.visited) {
            Text(
                "Visited",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AssistChip(
                onClick = {},
                label = { Text("Not yet visited") },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                },
            )
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = { onViewCollection(entry.collectionId) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("View the Collection")
        }
        TextButton(
            onClick = { onEditReview() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Edit the Review")
        }
        Button(
            onClick = { onOpenFullDetail(entry.entryId) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open full detail")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AppMenuDrawer(
    state: MapOverviewUiState,
    onToggleCollection: (CollectionId) -> Unit,
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
                                .background(row.color, CircleShape)
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

// TODO: migrate to DockedSearchBar when material3 DockedSearchBar + SearchBarDefaults.InputField lands in commonMain.
@Composable
private fun SearchBarField(
    expanded: Boolean,
    onFocus: () -> Unit,
    onSearch: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val widthFraction by animateFloatAsState(
        targetValue = if (expanded) 0.8f else 0.4f,
        animationSpec = spring(),
        label = "search-width",
    )

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
            .fillMaxWidth(widthFraction)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused && !expanded) onFocus() },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
    )
}
