package com.saxonthune.ranktheplanet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saxonthune.ranktheplanet.data.CollectionRepository
import com.saxonthune.ranktheplanet.data.EntryRepository
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.data.location.LocationProviderRegistry
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.EntryId
import com.saxonthune.ranktheplanet.nav.MapMode
import com.saxonthune.ranktheplanet.ui.RtpErrorState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.any
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.OrnamentOptions
import org.jetbrains.compose.resources.painterResource
import ranktheplanet.composeapp.generated.resources.Res
import ranktheplanet.composeapp.generated.resources.pin_body
import ranktheplanet.composeapp.generated.resources.pin_mark_dot
import ranktheplanet.composeapp.generated.resources.pin_mark_plus
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import kotlin.math.abs
import kotlin.math.roundToLong

private fun format3dp(value: Double): String {
    val scaled = (value * 1000.0).roundToLong()
    val whole = scaled / 1000
    val frac = (scaled % 1000).let { if (it < 0) -it else it }
    val fracStr = frac.toString().padStart(3, '0')
    return "$whole.$fracStr"
}

internal fun formatLatLng(lat: Double, lng: Double): String {
    val ns = if (lat >= 0) "N" else "S"
    val ew = if (lng >= 0) "E" else "W"
    // three decimals is ~100m precision; readable for a peek card
    return "${format3dp(abs(lat))}°$ns, ${format3dp(abs(lng))}°$ew"
}

private fun escapeJsonString(value: String): String {
    val sb = StringBuilder(value.length + 2)
    for (c in value) {
        when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c.code < 0x20) {
                sb.append("\\u")
                sb.append(c.code.toString(16).padStart(4, '0'))
            } else {
                sb.append(c)
            }
        }
    }
    return sb.toString()
}


private fun PinKind.token(): String = when (this) {
    PinKind.Unvisited -> "unvisited"
    PinKind.Reviewed -> "reviewed"
    PinKind.Multi -> "multi"
    PinKind.MultiUnvisited -> "multi-unvisited"
}

private fun buildPinsGeoJson(pins: List<PinUi>): String {
    val features = pins.joinToString(",") { pin ->
        val name = escapeJsonString(pin.locationName)
        val color = escapeJsonString(pin.colorHex)
        val kind = pin.kind.token()
        """{"type":"Feature","geometry":{"type":"Point","coordinates":[${pin.lng},${pin.lat}]},"properties":{"entryId":"${pin.entryId.value}","name":"$name","color":"$color","kind":"$kind"}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MapOverviewScreen(
    mode: MapMode,
    collections: CollectionRepository,
    entries: EntryRepository,
    templates: TemplateRepository,
    providerRegistry: LocationProviderRegistry,
    onCancelAdd: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
    onViewCollection: (CollectionId) -> Unit,
    onEditReview: (EntryId) -> Unit,
    onPickCollectionForDraft: (CollectionId) -> Unit,
    onNewCollectionForDraft: () -> Unit,
    onGoToReview: (EntryId) -> Unit,
    onPendingReviewDismissed: () -> Unit,
) {
    val vm = viewModel { MapOverviewViewModel(collections, entries, providerRegistry, templates) }
    val state by vm.uiState.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    var searchExpanded by remember { mutableStateOf(false) }

    // Capture surface color here — must not be read inside the MaplibreMap content lambda.
    val pinStroke = MaterialTheme.colorScheme.surface

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = -73.9855, latitude = 40.7580),
            zoom = 12.0,
        )
    )

    val snackbarHostState = remember { SnackbarHostState() }

    val pending = state.pendingReview
    if (pending is PendingReviewPrompt.Pending) {
        LaunchedEffect(pending.entryId) {
            val result = snackbarHostState.showSnackbar(
                message = "Review ${pending.locationName}?",
                actionLabel = "Review",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> onGoToReview(pending.entryId)
                SnackbarResult.Dismissed -> Unit
            }
            vm.clearPendingReview()
            onPendingReviewDismissed()
        }
    }

    BackHandler(enabled = state.draft is LocationDraftSheet.Open || state.pinSheet !is PinSheet.None) {
        val draft = state.draft
        if (draft is LocationDraftSheet.Open) {
            if (draft.phase == DraftPhase.AddToCollection) vm.backToDraft()
            else vm.dismissDraft()
        } else vm.dismissSheet()
    }

    val scaffoldContent: @Composable () -> Unit = {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        navigationIcon = {
                            when (mode) {
                                is MapMode.Browse -> IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open the app menu")
                                }
                                is MapMode.AddingToCollection -> IconButton(onClick = onCancelAdd) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel adding")
                                }
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
                                        modifier = Modifier.clickable {
                                            vm.startDraft(
                                                lat = result.lat,
                                                lng = result.lng,
                                                displayName = result.displayName,
                                            )
                                        },
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                when (mode) {
                    is MapMode.Browse -> BottomAppBar(
                        actions = {
                            TextButton(onClick = onOpenCollections) {
                                Icon(Icons.Default.Layers, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Collections")
                            }
                        },
                    )
                    is MapMode.AddingToCollection -> Surface(
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
                            Text("Adding to ${mode.collectionName}")
                        }
                    }
                }
            },
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                MaplibreMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraState = cameraState,
                    baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/bright"),
                    options = MapOptions(ornamentOptions = OrnamentOptions.OnlyLogo),
                    onMapLongClick = { position, _ ->
                        vm.startDraft(lat = position.latitude, lng = position.longitude)
                        ClickResult.Consume
                    },
                ) {
                    val geojson = buildPinsGeoJson(state.pins)
                    val source = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(geojson),
                    )
                    val bodyImage = image(painterResource(Res.drawable.pin_body), drawAsSdf = true)
                    val dotImage = image(painterResource(Res.drawable.pin_mark_dot), drawAsSdf = true)
                    val plusImage = image(painterResource(Res.drawable.pin_mark_plus), drawAsSdf = true)
                    val grey = Color(0xFF9AA0A6)
                    val offWhite = Color(0xFFF5F0E8)
                    val kindExpr = feature["kind"].asString()
                    val colorExpr = feature["color"].convertToColor(const(grey))

                    // Layer 1: colored outline halo for unvisited (slightly larger body, behind grey body).
                    SymbolLayer(
                        id = "pins-halo",
                        source = source,
                        filter = kindExpr eq const("unvisited"),
                        iconImage = bodyImage,
                        iconColor = colorExpr,
                        iconSize = const(1.15f),
                        iconAllowOverlap = const(true),
                        iconAnchor = const(SymbolAnchor.Bottom),
                    )
                    // Layer 2: teardrop body — all pins. Color depends on kind.
                    SymbolLayer(
                        id = "pins-body",
                        source = source,
                        iconImage = bodyImage,
                        iconColor = switch(
                            kindExpr,
                            case("reviewed", colorExpr),
                            case("multi", const(offWhite)),
                            fallback = const(grey),
                        ),
                        iconSize = const(1.0f),
                        iconAllowOverlap = const(true),
                        iconAnchor = const(SymbolAnchor.Bottom),
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
                    // Layer 3: centered mark (dot/plus) for reviewed and multi kinds.
                    SymbolLayer(
                        id = "pins-mark",
                        source = source,
                        filter = any(
                            kindExpr eq const("reviewed"),
                            kindExpr eq const("multi"),
                            kindExpr eq const("multi-unvisited"),
                        ),
                        iconImage = switch(
                            kindExpr,
                            case("reviewed", dotImage),
                            case("multi", plusImage),
                            case("multi-unvisited", plusImage),
                            fallback = dotImage,
                        ),
                        iconColor = const(Color.Black),
                        iconSize = const(1.0f),
                        iconAllowOverlap = const(true),
                        iconAnchor = const(SymbolAnchor.Bottom),
                    )
                    // Labels for all pins.
                    SymbolLayer(
                        id = "pin-labels",
                        source = source,
                        minZoom = 12f,
                        textField = format(span(feature["name"].asString())),
                        textSize = const(12.sp),
                        textOffset = offset(0f.em, 1.2f.em),
                        textAnchor = const(SymbolAnchor.Top),
                        textOptional = const(true),
                        iconAllowOverlap = const(true),
                        textHaloColor = const(pinStroke),
                        textHaloWidth = const(1.dp),
                    )
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

    if (mode is MapMode.Browse) {
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
            scaffoldContent()
        }
    } else {
        scaffoldContent()
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
                    onTapLocation = { vm.peekLocationFromEntry() },
                    onViewCollection = { collectionId ->
                        vm.dismissSheet()
                        onViewCollection(collectionId)
                    },
                    onEditReview = { entryId ->
                        vm.dismissSheet()
                        onEditReview(entryId)
                    },
                )
                is PinSheet.None -> {}
            }
        }
    }

    val currentDraft = state.draft
    if (currentDraft is LocationDraftSheet.Open) {
        val draftSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { vm.dismissDraft() },
            sheetState = draftSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            when (currentDraft.phase) {
                DraftPhase.Draft -> LocationDraftSheet(
                    draft = currentDraft,
                    mode = mode,
                    nearbyCandidates = state.nearbyCandidates,
                    isResolvingNearby = state.isResolvingNearby,
                    onDismiss = { vm.dismissDraft() },
                    onCancelAdd = {
                        vm.dismissDraft()
                        onCancelAdd()
                    },
                    onAddToCollection = { vm.openAddToCollection() },
                    onFindNearby = { vm.findNearby() },
                    onAdoptCandidate = { vm.adoptCandidate(it) },
                    onKeepCoordinates = { vm.keepCoordinates() },
                )
                DraftPhase.AddToCollection -> AddLocationToCollectionSheet(
                    draft = currentDraft,
                    collections = state.collectionPicks,
                    preSelectedCollectionId = (mode as? MapMode.AddingToCollection)?.collectionId,
                    onBack = { vm.backToDraft() },
                    onNewCollection = {
                        vm.dismissDraft()
                        onNewCollectionForDraft()
                    },
                    onPickCollection = { collectionId ->
                        vm.pickCollectionForDraft(collectionId)
                    },
                )
            }
        }
    }
}
